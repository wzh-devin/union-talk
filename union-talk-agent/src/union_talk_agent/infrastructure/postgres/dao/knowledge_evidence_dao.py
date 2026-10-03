"""RAG 候选回表校验 DAO。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/29 18:52
"""

import sqlalchemy as sa
from sqlalchemy.ext.asyncio import AsyncSession

from union_talk_agent.infrastructure.postgres.column_expression import table_column
from union_talk_agent.infrastructure.postgres.tables import (
    agent_message_segment_table,
    agent_resource_chunk_table,
    agent_resource_table,
)
from union_talk_agent.knowledge.domain.enums import ResourceStatus, SourceType
from union_talk_agent.knowledge.domain.retrieval_evidence import (
    RetrievalEvidence,
    VectorCandidate,
)


class KnowledgeEvidenceDao:
    """校验候选会话、版本和 READY 状态并加载正文。"""

    def __init__(self, session: AsyncSession) -> None:
        """
        初始化 KnowledgeEvidenceDao

        :param session: 异步数据库会话
        :return: 无返回值
        """

        self._session = session

    async def load_ready_evidence(
        self,
        conversation_id: int,
        candidate_list: list[VectorCandidate],
    ) -> list[RetrievalEvidence]:
        """
        加载当前会话可用证据

        :param conversation_id: 会话 ID
        :param candidate_list: Milvus 候选列表
        :return: 回表校验通过的证据
        """

        score_map = {
            (
                candidate.source_type,
                candidate.source_id,
                candidate.source_version,
            ): candidate.score
            for candidate in candidate_list
        }
        message_id_list = [
            candidate.source_id
            for candidate in candidate_list
            if candidate.source_type is SourceType.MESSAGE_SEGMENT
        ]
        chunk_id_list = [
            candidate.source_id
            for candidate in candidate_list
            if candidate.source_type is SourceType.RESOURCE_CHUNK
        ]
        evidence_list: list[RetrievalEvidence] = []
        if message_id_list:
            result = await self._session.execute(
                sa.select(agent_message_segment_table)
                .where(table_column(agent_message_segment_table, "id").in_(message_id_list))
                .where(
                    table_column(agent_message_segment_table, "conversation_id") == conversation_id
                )
                .where(table_column(agent_message_segment_table, "status") == "READY")
            )
            evidence_list.extend(
                RetrievalEvidence(
                    source_type=SourceType.MESSAGE_SEGMENT,
                    source_id=int(row["id"]),
                    message_id=int(row["start_message_id"]),
                    text_content=str(row["segment_text"]),
                    score=score_map[
                        (
                            SourceType.MESSAGE_SEGMENT,
                            int(row["id"]),
                            int(row["source_revision"]),
                        )
                    ],
                )
                for row in result.mappings().all()
                if (
                    SourceType.MESSAGE_SEGMENT,
                    int(row["id"]),
                    int(row["source_revision"]),
                )
                in score_map
            )
        if chunk_id_list:
            result = await self._session.execute(
                sa.select(
                    agent_resource_chunk_table.c["id"].label("chunk_id"),
                    agent_resource_chunk_table.c["text_content"],
                    agent_resource_chunk_table.c["page_from"],
                    agent_resource_chunk_table.c["page_to"],
                    agent_resource_chunk_table.c["heading_path"],
                    agent_resource_chunk_table.c["parent_chunk_id"],
                    agent_resource_chunk_table.c["chunk_no"],
                    agent_resource_table.c["asset_file_id"],
                    agent_resource_table.c["resource_version"],
                )
                .join(
                    agent_resource_table,
                    table_column(agent_resource_table, "id")
                    == table_column(agent_resource_chunk_table, "resource_id"),
                )
                .where(table_column(agent_resource_chunk_table, "id").in_(chunk_id_list))
                .where(
                    table_column(agent_resource_chunk_table, "conversation_id") == conversation_id
                )
                .where(table_column(agent_resource_chunk_table, "vector_status") == "READY")
                .where(table_column(agent_resource_table, "status") == "READY")
                .where(table_column(agent_resource_table, "is_current").is_(True))
            )
            evidence_list.extend(
                RetrievalEvidence(
                    source_type=SourceType.RESOURCE_CHUNK,
                    source_id=int(row["chunk_id"]),
                    text_content=str(row["text_content"]),
                    score=score_map[
                        (
                            SourceType.RESOURCE_CHUNK,
                            int(row["chunk_id"]),
                            int(row["resource_version"]),
                        )
                    ],
                    asset_file_id=int(row["asset_file_id"]),
                    resource_version=int(row["resource_version"]),
                    page_from=int(row["page_from"]) if row["page_from"] is not None else None,
                    page_to=int(row["page_to"]) if row["page_to"] is not None else None,
                    heading_path=str(row["heading_path"] or ""),
                    parent_chunk_id=(
                        int(row["parent_chunk_id"]) if row["parent_chunk_id"] is not None else None
                    ),
                    chunk_no=int(row["chunk_no"]),
                )
                for row in result.mappings().all()
                if (
                    SourceType.RESOURCE_CHUNK,
                    int(row["chunk_id"]),
                    int(row["resource_version"]),
                )
                in score_map
            )
        return sorted(evidence_list, key=lambda evidence: evidence.score, reverse=True)

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

        if not message_id_list:
            return set()
        result = await self._session.execute(
            sa.select(agent_message_segment_table.c["start_message_id"])
            .where(table_column(agent_message_segment_table, "conversation_id") == conversation_id)
            .where(
                table_column(agent_message_segment_table, "start_message_id").in_(message_id_list)
            )
            .where(table_column(agent_message_segment_table, "status") == "DELETED")
        )
        return {int(row[0]) for row in result.all()}

    async def expand_resource_context(
        self,
        conversation_id: int,
        chunk_id_list: list[int],
    ) -> list[RetrievalEvidence]:
        """
        加载命中块的 Parent、前后相邻块和标题路径

        :param conversation_id: 会话 ID
        :param chunk_id_list: 命中资源块 ID 列表
        :return: 已按资源和块顺序排列的扩展证据
        """

        hit_result = await self._session.execute(
            sa.select(agent_resource_chunk_table)
            .join(
                agent_resource_table,
                table_column(agent_resource_table, "id")
                == table_column(agent_resource_chunk_table, "resource_id"),
            )
            .where(table_column(agent_resource_chunk_table, "id").in_(chunk_id_list))
            .where(table_column(agent_resource_chunk_table, "conversation_id") == conversation_id)
            .where(table_column(agent_resource_chunk_table, "vector_status") == "READY")
            .where(table_column(agent_resource_table, "status") == "READY")
            .where(table_column(agent_resource_table, "is_current").is_(True))
        )
        hit_row_list = hit_result.mappings().all()
        if not hit_row_list:
            return []
        resource_chunk_no_map: dict[int, set[int]] = {}
        parent_id_set: set[int] = set()
        for row in hit_row_list:
            resource_id = int(row["resource_id"])
            chunk_no = int(row["chunk_no"])
            resource_chunk_no_map.setdefault(resource_id, set()).update(
                {max(chunk_no - 1, 0), chunk_no, chunk_no + 1}
            )
            if row["parent_chunk_id"] is not None:
                parent_id_set.add(int(row["parent_chunk_id"]))
        filter_expression = table_column(agent_resource_chunk_table, "id").in_(parent_id_set)
        for resource_id, chunk_no_set in resource_chunk_no_map.items():
            filter_expression = sa.or_(
                filter_expression,
                sa.and_(
                    table_column(agent_resource_chunk_table, "resource_id") == resource_id,
                    table_column(agent_resource_chunk_table, "chunk_no").in_(chunk_no_set),
                ),
            )
        result = await self._session.execute(
            sa.select(
                agent_resource_chunk_table.c["id"].label("chunk_id"),
                agent_resource_chunk_table.c["resource_id"],
                agent_resource_chunk_table.c["parent_chunk_id"],
                agent_resource_chunk_table.c["chunk_no"],
                agent_resource_chunk_table.c["text_content"],
                agent_resource_chunk_table.c["page_from"],
                agent_resource_chunk_table.c["page_to"],
                agent_resource_chunk_table.c["heading_path"],
                agent_resource_table.c["asset_file_id"],
                agent_resource_table.c["resource_version"],
            )
            .join(
                agent_resource_table,
                table_column(agent_resource_table, "id")
                == table_column(agent_resource_chunk_table, "resource_id"),
            )
            .where(filter_expression)
            .where(table_column(agent_resource_chunk_table, "conversation_id") == conversation_id)
            .where(table_column(agent_resource_table, "status") == "READY")
            .where(table_column(agent_resource_table, "is_current").is_(True))
            .order_by(
                agent_resource_chunk_table.c["resource_id"],
                agent_resource_chunk_table.c["chunk_no"],
            )
        )
        return [
            RetrievalEvidence(
                source_type=SourceType.RESOURCE_CHUNK,
                source_id=int(row["chunk_id"]),
                text_content=str(row["text_content"]),
                score=1.0,
                asset_file_id=int(row["asset_file_id"]),
                resource_version=int(row["resource_version"]),
                page_from=int(row["page_from"]) if row["page_from"] is not None else None,
                page_to=int(row["page_to"]) if row["page_to"] is not None else None,
                heading_path=str(row["heading_path"] or ""),
                parent_chunk_id=(
                    int(row["parent_chunk_id"]) if row["parent_chunk_id"] is not None else None
                ),
                chunk_no=int(row["chunk_no"]),
                evidence_key=f"X{index + 1}",
            )
            for index, row in enumerate(result.mappings().all())
        ]

    async def get_resource_status(
        self,
        conversation_id: int,
        resource_id_list: list[int],
    ) -> ResourceStatus | None:
        """
        查询明确引用资源的当前索引状态

        :param conversation_id: 会话 ID
        :param resource_id_list: 资源文件 ID 列表
        :return: 聚合后的当前资源状态；无记录时返回空
        """

        if not resource_id_list:
            return None
        result = await self._session.execute(
            sa.select(
                agent_resource_table.c["asset_file_id"],
                agent_resource_table.c["status"],
            )
            .where(table_column(agent_resource_table, "conversation_id") == conversation_id)
            .where(table_column(agent_resource_table, "asset_file_id").in_(resource_id_list))
            .where(table_column(agent_resource_table, "is_current").is_(True))
        )
        row_list = result.mappings().all()
        found_resource_id_set = {int(row["asset_file_id"]) for row in row_list}
        if found_resource_id_set != set(resource_id_list):
            return ResourceStatus.INDEXING
        status_set = {ResourceStatus(str(row["status"])) for row in row_list}
        if ResourceStatus.INDEXING in status_set or ResourceStatus.PENDING in status_set:
            return ResourceStatus.INDEXING
        if ResourceStatus.FAILED in status_set:
            return ResourceStatus.FAILED
        if ResourceStatus.READY in status_set:
            return ResourceStatus.READY
        return next(iter(status_set), None)
