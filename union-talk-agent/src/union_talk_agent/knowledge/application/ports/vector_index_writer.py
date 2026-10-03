"""Milvus 资源向量写入边界。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/29 19:15
"""

from typing import Protocol

from union_talk_agent.knowledge.application.ports.embedding_gateway import HybridEmbedding
from union_talk_agent.knowledge.domain.indexed_resource import ResourceDescriptor
from union_talk_agent.knowledge.domain.message_segment import MessageSegment
from union_talk_agent.knowledge.domain.resource_chunk import ResourceChunk


class VectorIndexWriter(Protocol):
    """幂等写入或删除单个资源版本的向量。"""

    async def replace_resource_vectors(
        self,
        descriptor: ResourceDescriptor,
        chunk_list: list[ResourceChunk],
        vector_list: list[HybridEmbedding],
    ) -> None:
        """
        删除同版本旧实体并写入新实体

        :param descriptor: 资源描述信息
        :param chunk_list: 资源切块列表
        :param vector_list: 资源切块 Dense 与 Sparse 联合向量列表
        :return: 无返回值
        """

        ...

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

        ...

    async def delete_resource_vectors(self, asset_file_id: int) -> None:
        """
        删除指定资产的全部资源版本向量

        :param asset_file_id: 资产文件 ID
        :return: 无返回值
        """

        ...

    async def delete_message_vectors(self, conversation_id: int, message_id: int) -> None:
        """
        删除指定会话消息的全部向量版本

        :param conversation_id: 会话 ID
        :param message_id: 消息 ID
        :return: 无返回值
        """

        ...

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

        ...
