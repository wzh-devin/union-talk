"""PyMilvus 同步客户端类型适配器。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/30 10:43
"""

from typing import Any

from pymilvus import AnnSearchRequest, MilvusClient, RRFRanker

from union_talk_agent.infrastructure.milvus.milvus_client_contract import (
    MilvusCollectionClient,
    MilvusResourceWriterClient,
    MilvusSearchClient,
)
from union_talk_agent.knowledge.application.ports.embedding_gateway import HybridEmbedding


class PyMilvusClientAdapter(
    MilvusCollectionClient,
    MilvusSearchClient,
    MilvusResourceWriterClient,
):
    """将 PyMilvus 动态返回值收敛到 Agent 内部稳定契约。"""

    def __init__(self, client: MilvusClient) -> None:
        """
        初始化 PyMilvusClientAdapter

        :param client: 外部系统客户端
        :return: 无返回值
        """

        self._client: Any = client

    def has_collection(self, collection_name: str) -> bool:
        """
        判断 Collection 是否存在

        :param collection_name: Milvus Collection 名称
        :return: Collection 是否存在
        """

        return bool(self._client.has_collection(collection_name))

    def load_collection(self, collection_name: str) -> None:
        """
        加载 Collection

        :param collection_name: Milvus Collection 名称
        :return: 无返回值
        """

        self._client.load_collection(collection_name)

    def create_collection(
        self,
        *,
        collection_name: str,
        schema: object,
        index_params: object,
        num_partitions: int,
        consistency_level: str,
    ) -> None:
        """
        创建 Collection

        :param collection_name: Milvus Collection 名称
        :param schema: Milvus Collection Schema
        :param index_params: Milvus 索引参数
        :param num_partitions: Milvus Partition 数量
        :param consistency_level: Milvus 一致性级别
        :return: 无返回值
        """

        self._client.create_collection(
            collection_name=collection_name,
            schema=schema,
            index_params=index_params,
            num_partitions=num_partitions,
            consistency_level=consistency_level,
        )

    def search(
        self,
        *,
        collection_name: str,
        data: list[list[float]],
        anns_field: str,
        filter_expression: str,
        limit: int,
        output_fields: list[str],
        search_params: dict[str, object],
    ) -> list[list[dict[str, object]]]:
        """
        执行向量检索

        :param collection_name: Milvus Collection 名称
        :param data: 待写入或检索的数据
        :param anns_field: Milvus 向量字段名称
        :param filter_expression: Milvus 标量过滤表达式
        :param limit: 查询或召回数量上限
        :param output_fields: Milvus 返回字段列表
        :param search_params: Milvus 向量检索参数
        :return: 按查询向量分组的 Milvus 命中结果
        """

        return self._client.search(
            collection_name=collection_name,
            data=data,
            anns_field=anns_field,
            filter=filter_expression,
            limit=limit,
            output_fields=output_fields,
            search_params=search_params,
        )

    def upsert(
        self,
        collection_name: str,
        data: list[dict[str, object]],
    ) -> dict[str, object]:
        """
        写入或覆盖向量实体

        :param collection_name: Milvus Collection 名称
        :param data: 待写入或检索的数据
        :return: PyMilvus Upsert 执行结果
        """

        return self._client.upsert(collection_name, data)

    def hybrid_search(
        self,
        *,
        collection_name: str,
        embedding: HybridEmbedding,
        filter_expression: str,
        limit: int,
        output_fields: list[str],
    ) -> list[list[dict[str, object]]]:
        """
        使用 Milvus RRF 合并 Dense 与 Sparse 候选

        :param collection_name: Milvus Collection 名称
        :param embedding: 查询联合向量
        :param filter_expression: Milvus 标量过滤表达式
        :param limit: 混合召回数量上限
        :param output_fields: Milvus 返回字段列表
        :return: RRF 合并后的候选结果
        """

        candidate_limit = max(limit * 3, 30)
        request_list = [
            AnnSearchRequest(
                data=[embedding.dense],
                anns_field="dense_embedding",
                param={"metric_type": "COSINE", "params": {"ef": max(limit * 4, 64)}},
                limit=candidate_limit,
                expr=filter_expression,
            ),
            AnnSearchRequest(
                data=[embedding.sparse],
                anns_field="sparse_embedding",
                param={"metric_type": "IP", "params": {}},
                limit=candidate_limit,
                expr=filter_expression,
            ),
        ]
        return self._client.hybrid_search(
            collection_name=collection_name,
            reqs=request_list,
            ranker=RRFRanker(k=60),
            limit=limit,
            output_fields=output_fields,
        )

    def delete(
        self,
        collection_name: str,
        *,
        filter_expression: str,
    ) -> dict[str, int]:
        """
        按表达式删除向量实体

        :param collection_name: Milvus Collection 名称
        :param filter_expression: Milvus 标量过滤表达式
        :return: PyMilvus 删除数量结果
        """

        return self._client.delete(
            collection_name,
            filter=filter_expression,
        )

    def close(self) -> None:
        """
        关闭客户端

        :return: 无返回值
        """

        self._client.close()
