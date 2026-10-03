"""共享 Milvus Collection Schema 管理。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/29 19:04
"""

import asyncio

from pymilvus import DataType, MilvusClient

from union_talk_agent.infrastructure.milvus.milvus_client_contract import (
    MilvusCollectionClient,
)


class MilvusCollectionManager:
    """创建以 conversation_id 为 Partition Key 的共享 Collection。"""

    _PARTITION_COUNT = 64

    def __init__(
        self,
        client: MilvusCollectionClient,
        collection_name: str,
        vector_dimension: int,
    ) -> None:
        """
        初始化 MilvusCollectionManager

        :param client: 外部系统客户端
        :param collection_name: Milvus Collection 名称
        :param vector_dimension: 模型向量维度
        :return: 无返回值
        """

        self._client = client
        self._collection_name = collection_name
        self._vector_dimension = vector_dimension

    async def ensure_collection(self) -> None:
        """
        幂等创建 Collection、HNSW 索引并加载

        :return: 无返回值
        """

        await asyncio.to_thread(self._ensure_collection_sync)

    async def check(self) -> bool:
        """
        检查 Collection 是否存在

        :return: 判断结果
        """

        return bool(
            await asyncio.to_thread(
                self._client.has_collection,
                self._collection_name,
            )
        )

    async def close(self) -> None:
        """
        关闭 Milvus 客户端

        :return: 无返回值
        """

        await asyncio.to_thread(self._client.close)

    def _ensure_collection_sync(self) -> None:
        """
        同步检查并创建 Milvus Collection

        :return: 无返回值
        """

        if self._client.has_collection(self._collection_name):
            self._client.load_collection(self._collection_name)
            return
        schema = MilvusClient.create_schema(
            auto_id=False,
            enable_dynamic_field=False,
        )
        schema.add_field(
            "vector_pk",
            DataType.VARCHAR,
            is_primary=True,
            max_length=96,
        )
        schema.add_field(
            "conversation_id",
            DataType.INT64,
            is_partition_key=True,
        )
        schema.add_field("source_type", DataType.INT8)
        schema.add_field("source_id", DataType.INT64)
        schema.add_field("source_version", DataType.INT32)
        schema.add_field("chunk_kind", DataType.INT8)
        schema.add_field("mime_group", DataType.INT8)
        schema.add_field("asset_file_id", DataType.INT64)
        schema.add_field("message_id", DataType.INT64)
        schema.add_field("sender_id", DataType.INT64)
        schema.add_field("occurred_at_ms", DataType.INT64)
        schema.add_field("folder_id", DataType.INT64)
        schema.add_field("mime_type", DataType.VARCHAR, max_length=256)
        schema.add_field("token_count", DataType.INT32)
        schema.add_field(
            "dense_embedding",
            DataType.FLOAT_VECTOR,
            dim=self._vector_dimension,
        )
        schema.add_field("sparse_embedding", DataType.SPARSE_FLOAT_VECTOR)
        index_params = MilvusClient.prepare_index_params()
        index_params.add_index(
            field_name="dense_embedding",
            index_name="idx_dense_embedding_hnsw",
            index_type="HNSW",
            metric_type="COSINE",
            params={"M": 32, "efConstruction": 200},
        )
        index_params.add_index(
            field_name="sparse_embedding",
            index_name="idx_sparse_embedding_inverted",
            index_type="SPARSE_INVERTED_INDEX",
            metric_type="IP",
            params={"inverted_index_algo": "DAAT_MAXSCORE"},
        )
        self._client.create_collection(
            collection_name=self._collection_name,
            schema=schema,
            index_params=index_params,
            num_partitions=self._PARTITION_COUNT,
            consistency_level="Bounded",
        )
        self._client.load_collection(self._collection_name)
