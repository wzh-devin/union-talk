"""Milvus 混合候选召回、PostgreSQL 校验与 BGE-M3 重排。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/08/12 23:04
"""

import asyncio
from dataclasses import replace
from typing import cast

from union_talk_agent.agent_run.application.ports.agent_unit_of_work import (
    AgentUnitOfWorkFactory,
)
from union_talk_agent.agent_run.application.ports.knowledge_retriever import (
    MessageSearchQuery,
    ResourceSearchQuery,
)
from union_talk_agent.infrastructure.embedding.openai_embedding_client import (
    OpenAiEmbeddingClient,
)
from union_talk_agent.infrastructure.milvus.milvus_client_contract import MilvusSearchClient
from union_talk_agent.knowledge.domain.constants import MILVUS_SOURCE_TYPE_CODE_MAP
from union_talk_agent.knowledge.domain.enums import ResourceStatus, SourceType
from union_talk_agent.knowledge.domain.retrieval_evidence import (
    RetrievalEvidence,
    VectorCandidate,
)


class MilvusKnowledgeRetriever:
    """执行 BGE-M3 Dense/Sparse、RRF、回表、重排和来源配额。"""

    def __init__(
        self,
        *,
        client: MilvusSearchClient,
        collection_name: str,
        embedding_client: OpenAiEmbeddingClient,
        unit_of_work_factory: AgentUnitOfWorkFactory,
    ) -> None:
        """
        初始化 Milvus 混合检索器

        :param client: Milvus 检索客户端
        :param collection_name: Milvus Collection 名称
        :param embedding_client: BGE-M3 内部服务客户端
        :param unit_of_work_factory: Agent 工作单元工厂
        :return: 无返回值
        """

        self._client = client
        self._collection_name = collection_name
        self._embedding_client = embedding_client
        self._unit_of_work_factory = unit_of_work_factory

    async def search_messages(self, search_query: MessageSearchQuery) -> list[RetrievalEvidence]:
        """
        混合检索并重排会话历史消息

        :param search_query: 历史消息检索条件
        :return: 当前会话可用的消息证据
        """

        return await self._search(
            search_query.conversation_id,
            search_query.query,
            SourceType.MESSAGE_SEGMENT,
            search_query.top_k,
            [
                *(
                    [
                        "sender_id in ["
                        + ",".join(str(value) for value in search_query.participant_user_id_list)
                        + "]"
                    ]
                    if search_query.participant_user_id_list
                    else []
                ),
                *(
                    [f"occurred_at_ms >= {int(search_query.started_at.timestamp() * 1000)}"]
                    if search_query.started_at is not None
                    else []
                ),
                *(
                    [f"occurred_at_ms <= {int(search_query.ended_at.timestamp() * 1000)}"]
                    if search_query.ended_at is not None
                    else []
                ),
            ],
        )

    async def list_invalid_message_ids(
        self,
        conversation_id: int,
        message_id_list: list[int],
    ) -> set[int]:
        """
        查询不得再次进入模型上下文的消息 ID

        :param conversation_id: 会话 ID
        :param message_id_list: 待检查消息 ID 列表
        :return: 已失效消息 ID 集合
        """

        async with self._unit_of_work_factory() as unit_of_work:
            return await unit_of_work.knowledge_evidence.list_invalid_message_ids(
                conversation_id,
                message_id_list,
            )

    async def search_resources(
        self,
        search_query: ResourceSearchQuery,
    ) -> list[RetrievalEvidence]:
        """
        混合检索并重排会话资源块

        :param search_query: 资源检索条件
        :return: 当前会话可用的资源证据
        """

        extra_filter_list: list[str] = []
        if search_query.resource_id_list:
            resource_id_text = ",".join(str(value) for value in search_query.resource_id_list)
            extra_filter_list.append(f"asset_file_id in [{resource_id_text}]")
        if search_query.folder_id is not None:
            extra_filter_list.append(f"folder_id == {search_query.folder_id}")
        if search_query.mime_type_list:
            mime_text = ",".join(
                f'"{self._escape_filter_string(value)}"' for value in search_query.mime_type_list
            )
            extra_filter_list.append(f"mime_type in [{mime_text}]")
        return await self._search(
            search_query.conversation_id,
            search_query.query,
            SourceType.RESOURCE_CHUNK,
            search_query.top_k,
            extra_filter_list,
        )

    async def expand_resource_context(
        self,
        conversation_id: int,
        chunk_id_list: list[int],
        token_budget: int,
    ) -> list[RetrievalEvidence]:
        """
        回表加载 Parent 与相邻块并按 Token 预算裁剪

        :param conversation_id: 会话 ID
        :param chunk_id_list: 命中资源块 ID 列表
        :param token_budget: 扩展 Token 预算
        :return: 扩展并裁剪后的证据列表
        """

        if not chunk_id_list or token_budget <= 0:
            return []
        async with self._unit_of_work_factory() as unit_of_work:
            evidence_list = await unit_of_work.knowledge_evidence.expand_resource_context(
                conversation_id,
                chunk_id_list,
            )
        result_list: list[RetrievalEvidence] = []
        used_tokens = 0
        for evidence in evidence_list:
            evidence_tokens = max(len(evidence.text_content) // 4, 1)
            if used_tokens + evidence_tokens > token_budget:
                break
            result_list.append(evidence)
            used_tokens += evidence_tokens
        return result_list

    async def get_resource_status(
        self,
        conversation_id: int,
        resource_id_list: list[int],
    ) -> ResourceStatus | None:
        """
        回表查询明确引用资源的当前索引状态

        :param conversation_id: 会话 ID
        :param resource_id_list: 资源文件 ID 列表
        :return: 聚合后的当前资源状态；无记录时返回空
        """

        async with self._unit_of_work_factory() as unit_of_work:
            return await unit_of_work.knowledge_evidence.get_resource_status(
                conversation_id,
                resource_id_list,
            )

    async def _search(
        self,
        conversation_id: int,
        question: str,
        source_type: SourceType,
        top_k: int,
        extra_filter_list: list[str],
    ) -> list[RetrievalEvidence]:
        """
        执行单来源混合召回和重排

        :param conversation_id: 会话 ID
        :param question: 检索问题
        :param source_type: 检索来源类型
        :param top_k: 最终返回数量
        :param extra_filter_list: 额外 Milvus 标量过滤条件
        :return: 重排后的证据列表
        """

        if not question.strip() or top_k <= 0:
            return []
        embedding = (await self._embedding_client.encode_hybrid([question]))[0]
        source_code = MILVUS_SOURCE_TYPE_CODE_MAP[str(source_type)]
        filter_list = [
            f"conversation_id == {conversation_id}",
            f"source_type == {source_code}",
            *extra_filter_list,
        ]
        result = await asyncio.to_thread(
            self._client.hybrid_search,
            collection_name=self._collection_name,
            embedding=embedding,
            filter_expression=" and ".join(filter_list),
            limit=max(top_k * 3, 30),
            output_fields=["source_type", "source_id", "source_version"],
        )
        candidate_list = self._to_candidate_list(result, source_type)
        async with self._unit_of_work_factory() as unit_of_work:
            evidence_list = await unit_of_work.knowledge_evidence.load_ready_evidence(
                conversation_id,
                candidate_list,
            )
        score_list = await self._embedding_client.rerank(
            question,
            [evidence.text_content for evidence in evidence_list],
        )
        ranked_list = sorted(
            [
                replace(
                    evidence,
                    rerank_score=score,
                    evidence_key=f"E{index + 1}",
                )
                for index, (evidence, score) in enumerate(
                    zip(evidence_list, score_list, strict=True)
                )
            ],
            key=lambda item: item.rerank_score or 0,
            reverse=True,
        )
        return self._mmr_source_quota(ranked_list, top_k)

    @staticmethod
    def _escape_filter_string(value: str) -> str:
        """
        转义 Milvus 字符串过滤值

        :param value: 原始字符串值
        :return: 可安全嵌入过滤表达式的字符串
        """

        return value.replace("\\", "\\\\").replace('"', '\\"')

    @staticmethod
    def _to_candidate_list(
        result: list[list[dict[str, object]]],
        source_type: SourceType,
    ) -> list[VectorCandidate]:
        """
        把 Milvus RRF 命中转换为回表候选

        :param result: Milvus 混合检索结果
        :param source_type: 当前检索来源类型
        :return: 向量候选列表
        """

        if not result:
            return []
        candidate_list: list[VectorCandidate] = []
        for hit in result[0]:
            raw_entity = hit.get("entity", {})
            if not isinstance(raw_entity, dict):
                continue
            entity = cast(dict[str, object], raw_entity)
            source_id = entity.get("source_id")
            source_version = entity.get("source_version")
            raw_score = hit.get("distance", 0)
            if not isinstance(source_id, int) or not isinstance(source_version, int):
                continue
            candidate_list.append(
                VectorCandidate(
                    source_type=source_type,
                    source_id=source_id,
                    source_version=source_version,
                    score=float(raw_score) if isinstance(raw_score, int | float) else 0.0,
                )
            )
        return candidate_list

    @staticmethod
    def _mmr_source_quota(
        evidence_list: list[RetrievalEvidence],
        top_k: int,
    ) -> list[RetrievalEvidence]:
        """
        按来源 ID 去重并限制单资源占用最终结果

        :param evidence_list: 已按重排分数排序的候选
        :param top_k: 最终返回数量
        :return: 来源多样化后的证据
        """

        result_list: list[RetrievalEvidence] = []
        source_count_map: dict[int, int] = {}
        seen_text_set: set[str] = set()
        single_source_limit = max(top_k // 2, 2)
        for evidence in evidence_list:
            compact_text = " ".join(evidence.text_content.split())
            if compact_text in seen_text_set:
                continue
            quota_key = evidence.asset_file_id or evidence.source_id
            if source_count_map.get(quota_key, 0) >= single_source_limit:
                continue
            seen_text_set.add(compact_text)
            source_count_map[quota_key] = source_count_map.get(quota_key, 0) + 1
            result_list.append(evidence)
            if len(result_list) >= top_k:
                break
        return result_list


class DisabledKnowledgeRetriever:
    """Milvus 未启用时保持三类检索端口稳定。"""

    @staticmethod
    async def search_messages(search_query: MessageSearchQuery) -> list[RetrievalEvidence]:
        """
        返回空消息证据

        :param search_query: 历史消息检索条件
        :return: 空证据列表
        """

        _ignored_search_query = search_query
        return []

    @staticmethod
    async def list_invalid_message_ids(
        conversation_id: int,
        message_id_list: list[int],
    ) -> set[int]:
        """
        返回未启用检索时的空失效消息集合

        :param conversation_id: 会话 ID
        :param message_id_list: 待检查消息 ID 列表
        :return: 空消息 ID 集合
        """

        _ignored_arguments = (conversation_id, message_id_list)
        return set()

    @staticmethod
    async def search_resources(
        search_query: ResourceSearchQuery,
    ) -> list[RetrievalEvidence]:
        """
        返回空资源证据

        :param search_query: 资源检索条件
        :return: 空证据列表
        """

        _ignored_search_query = search_query
        return []

    @staticmethod
    async def expand_resource_context(
        conversation_id: int,
        chunk_id_list: list[int],
        token_budget: int,
    ) -> list[RetrievalEvidence]:
        """
        返回空扩展上下文

        :param conversation_id: 会话 ID
        :param chunk_id_list: 命中 Chunk ID 列表
        :param token_budget: 扩展 Token 预算
        :return: 空证据列表
        """

        _ignored_arguments = (conversation_id, chunk_id_list, token_budget)
        return []

    @staticmethod
    async def get_resource_status(
        conversation_id: int,
        resource_id_list: list[int],
    ) -> ResourceStatus | None:
        """
        返回未启用检索时的资源状态

        :param conversation_id: 会话 ID
        :param resource_id_list: 资源文件 ID 列表
        :return: 空状态
        """

        _ignored_arguments = (conversation_id, resource_id_list)
        return None
