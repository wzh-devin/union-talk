"""Milvus 资源元数据编码测试。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/29 19:22
"""

from union_talk_agent.infrastructure.milvus.milvus_client_contract import (
    MilvusResourceWriterClient,
)
from union_talk_agent.infrastructure.milvus.resource_vector_index import (
    MilvusResourceVectorIndex,
)
from union_talk_agent.knowledge.application.ports.embedding_gateway import HybridEmbedding
from union_talk_agent.knowledge.domain.enums import ChunkKind, MimeGroup
from union_talk_agent.knowledge.domain.indexed_resource import ResourceDescriptor
from union_talk_agent.knowledge.domain.resource_chunk import ResourceChunk


class RecordingMilvusClient(MilvusResourceWriterClient):
    """记录 Milvus 删除和 Upsert 参数。"""

    def __init__(self) -> None:
        """
        初始化 RecordingMilvusClient

        :return: 无返回值
        """

        self.collection_name = ""
        self.delete_filter = ""
        self.entity_list: list[dict[str, object]] = []

    def delete(
        self,
        collection_name: str,
        *,
        filter_expression: str,
    ) -> dict[str, int]:
        """
        记录精确资源版本过滤条件

        :param collection_name: Milvus Collection 名称
        :param filter_expression: Milvus 标量过滤表达式
        :return: 固定删除数量结果
        """

        self.delete_filter = filter_expression
        self.collection_name = collection_name
        return {"delete_count": 1}

    def upsert(
        self,
        collection_name: str,
        data: list[dict[str, object]],
    ) -> dict[str, object]:
        """
        记录待写入实体

        :param collection_name: Milvus Collection 名称
        :param data: 待写入或检索的数据
        :return: 按实体数量生成的 Upsert 结果
        """

        self.collection_name = collection_name
        self.entity_list = data
        return {"upsert_count": len(data)}


async def test_vector_entity_contains_conversation_partition_key() -> None:
    """
    每个向量实体必须携带 conversation_id 和数字编码元数据

    :return: 无返回值
    """

    client = RecordingMilvusClient()
    writer = MilvusResourceVectorIndex(
        client,
        "union_talk_rag_v2",
    )
    descriptor = ResourceDescriptor(
        asset_file_id=10,
        conversation_id=20,
        resource_version=3,
        file_name="guide.md",
        mime_type="text/markdown",
        sha256="abc",
    )
    chunk = ResourceChunk(
        chunk_id=30,
        resource_id=40,
        conversation_id=20,
        parent_chunk_id=29,
        chunk_kind=ChunkKind.CHILD,
        chunk_no=1,
        text_content="正文",
        token_count=2,
        mime_group=MimeGroup.MARKDOWN,
    )
    await writer.replace_resource_vectors(
        descriptor,
        [chunk],
        [HybridEmbedding(dense=[0.1, 0.2], sparse={1: 0.8})],
    )

    assert "asset_file_id == 10" in client.delete_filter
    assert "source_version == 3" in client.delete_filter
    assert client.entity_list[0]["conversation_id"] == 20
    assert client.entity_list[0]["source_type"] == 2
    assert client.entity_list[0]["chunk_kind"] == 1
    assert client.entity_list[0]["dense_embedding"] == [0.1, 0.2]
    assert client.entity_list[0]["sparse_embedding"] == {1: 0.8}


async def test_delete_resource_vectors_removes_all_asset_versions() -> None:
    """
    删除资产时必须使用仅包含资产 ID 的全版本过滤条件

    :return: 无返回值
    """

    client = RecordingMilvusClient()
    writer = MilvusResourceVectorIndex(client, "union_talk_rag_v2")

    await writer.delete_resource_vectors(10)

    assert client.collection_name == "union_talk_rag_v2"
    assert client.delete_filter == "asset_file_id == 10"


async def test_delete_resource_vectors_removes_derived_message_vectors() -> None:
    """
    删除资产时必须同时删除引用该资产的消息向量

    :return: 无返回值
    """

    client = RecordingMilvusClient()
    writer = MilvusResourceVectorIndex(client, "union_talk_rag_v2")

    await writer.delete_message_vector_list(20, [30, 31])

    assert client.delete_filter == "conversation_id == 20 and message_id in [30,31]"
