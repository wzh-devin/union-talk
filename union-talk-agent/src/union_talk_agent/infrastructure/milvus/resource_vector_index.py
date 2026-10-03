"""Milvus 资源切块向量写入。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/29 19:17
"""

import asyncio

from union_talk_agent.infrastructure.milvus.milvus_client_contract import (
    MilvusResourceWriterClient,
)
from union_talk_agent.knowledge.application.ports.embedding_gateway import HybridEmbedding
from union_talk_agent.knowledge.domain.constants import (
    MILVUS_CHUNK_KIND_CODE_MAP,
    MILVUS_MIME_GROUP_CODE_MAP,
    MILVUS_SOURCE_TYPE_CODE_MAP,
)
from union_talk_agent.knowledge.domain.enums import SourceType
from union_talk_agent.knowledge.domain.indexed_resource import ResourceDescriptor
from union_talk_agent.knowledge.domain.message_segment import MessageSegment
from union_talk_agent.knowledge.domain.resource_chunk import ResourceChunk


class MilvusResourceVectorIndex:
    """以资源版本为幂等边界替换 Milvus 实体。"""

    def __init__(
        self,
        client: MilvusResourceWriterClient,
        collection_name: str,
    ) -> None:
        """
        初始化 MilvusResourceVectorIndex

        :param client: 外部系统客户端
        :param collection_name: Milvus Collection 名称
        :return: 无返回值
        """

        self._client = client
        self._collection_name = collection_name

    async def replace_resource_vectors(
        self,
        descriptor: ResourceDescriptor,
        chunk_list: list[ResourceChunk],
        vector_list: list[HybridEmbedding],
    ) -> None:
        """
        删除同一资产版本的旧向量后批量 Upsert

        :param descriptor: 资源描述信息
        :param chunk_list: 资源切块列表
        :param vector_list: 资源切块 Dense 与 Sparse 联合向量列表
        :return: 无返回值
        """

        if len(chunk_list) != len(vector_list):
            raise ValueError("资源切块和向量数量不一致")
        await asyncio.to_thread(
            self._client.delete,
            self._collection_name,
            filter_expression=(
                f"asset_file_id == {descriptor.asset_file_id} "
                f"and source_version == {descriptor.resource_version}"
            ),
        )
        if not chunk_list:
            return
        entity_list: list[dict[str, object]] = [
            {
                "vector_pk": f"resource:{chunk.chunk_id}",
                "conversation_id": descriptor.conversation_id,
                "source_type": MILVUS_SOURCE_TYPE_CODE_MAP[str(SourceType.RESOURCE_CHUNK)],
                "source_id": chunk.chunk_id,
                "source_version": descriptor.resource_version,
                "chunk_kind": MILVUS_CHUNK_KIND_CODE_MAP[str(chunk.chunk_kind)],
                "mime_group": MILVUS_MIME_GROUP_CODE_MAP[str(chunk.mime_group)],
                "asset_file_id": descriptor.asset_file_id,
                "message_id": 0,
                "sender_id": 0,
                "occurred_at_ms": 0,
                "folder_id": descriptor.folder_id or 0,
                "mime_type": descriptor.mime_type,
                "token_count": chunk.token_count,
                "dense_embedding": vector.dense,
                "sparse_embedding": vector.sparse,
            }
            for chunk, vector in zip(chunk_list, vector_list, strict=True)
        ]
        await asyncio.to_thread(
            self._client.upsert,
            self._collection_name,
            entity_list,
        )

    async def replace_message_vector(
        self,
        segment: MessageSegment,
        vector: HybridEmbedding,
    ) -> None:
        """
        覆盖指定消息的当前向量实体

        :param segment: 消息检索片段
        :param vector: 消息 Dense 与 Sparse 联合向量
        :return: 无返回值
        """

        await self.delete_message_vectors(segment.conversation_id, segment.message_id)
        await asyncio.to_thread(
            self._client.upsert,
            self._collection_name,
            [
                {
                    "vector_pk": f"message:{segment.segment_id}",
                    "conversation_id": segment.conversation_id,
                    "source_type": MILVUS_SOURCE_TYPE_CODE_MAP[str(SourceType.MESSAGE_SEGMENT)],
                    "source_id": segment.segment_id,
                    "source_version": segment.source_revision,
                    "chunk_kind": MILVUS_CHUNK_KIND_CODE_MAP["NOT_APPLICABLE"],
                    "mime_group": MILVUS_MIME_GROUP_CODE_MAP["UNKNOWN"],
                    "asset_file_id": 0,
                    "message_id": segment.message_id,
                    "sender_id": segment.sender_id,
                    "occurred_at_ms": int(segment.occurred_at.timestamp() * 1000),
                    "folder_id": 0,
                    "mime_type": "",
                    "token_count": segment.token_count,
                    "dense_embedding": vector.dense,
                    "sparse_embedding": vector.sparse,
                }
            ],
        )

    async def delete_resource_vectors(self, asset_file_id: int) -> None:
        """
        删除指定资产的全部资源版本向量

        :param asset_file_id: 资产文件 ID
        :return: 无返回值
        """

        await asyncio.to_thread(
            self._client.delete,
            self._collection_name,
            filter_expression=f"asset_file_id == {asset_file_id}",
        )

    async def delete_message_vectors(self, conversation_id: int, message_id: int) -> None:
        """
        删除指定会话消息的全部向量版本

        :param conversation_id: 会话 ID
        :param message_id: 消息 ID
        :return: 无返回值
        """

        await asyncio.to_thread(
            self._client.delete,
            self._collection_name,
            filter_expression=(
                f"conversation_id == {conversation_id} and message_id == {message_id}"
            ),
        )

    async def delete_message_vector_list(
        self,
        conversation_id: int,
        message_id_list: list[int],
    ) -> None:
        """
        批量删除指定会话消息的全部向量版本

        :param conversation_id: 会话 ID
        :param message_id_list: 消息 ID 列表
        :return: 无返回值
        """

        if not message_id_list:
            return
        message_id_text = ",".join(str(message_id) for message_id in sorted(set(message_id_list)))
        await asyncio.to_thread(
            self._client.delete,
            self._collection_name,
            filter_expression=(
                f"conversation_id == {conversation_id} and message_id in [{message_id_text}]"
            ),
        )
