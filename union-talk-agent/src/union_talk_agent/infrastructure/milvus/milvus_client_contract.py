"""按职责隔离 PyMilvus 不完整类型标注的同步客户端契约。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/29 19:10
"""

from abc import ABC, abstractmethod

from union_talk_agent.knowledge.application.ports.embedding_gateway import HybridEmbedding


class MilvusCollectionClient(ABC):
    """Collection 生命周期所需能力。"""

    @abstractmethod
    def has_collection(self, collection_name: str) -> bool:
        """
        判断 Collection 是否存在

        :param collection_name: Milvus Collection 名称
        :return: Collection 是否存在
        """

        raise NotImplementedError

    @abstractmethod
    def load_collection(self, collection_name: str) -> None:
        """
        加载 Collection

        :param collection_name: Milvus Collection 名称
        :return: 无返回值
        """

        raise NotImplementedError

    @abstractmethod
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

        raise NotImplementedError

    @abstractmethod
    def close(self) -> None:
        """
        关闭客户端

        :return: 无返回值
        """

        raise NotImplementedError


class MilvusSearchClient(ABC):
    """向量召回所需能力。"""

    @abstractmethod
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

        raise NotImplementedError

    @abstractmethod
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
        执行 Dense 与 Sparse RRF 混合检索

        :param collection_name: Milvus Collection 名称
        :param embedding: 查询联合向量
        :param filter_expression: Milvus 标量过滤表达式
        :param limit: 混合召回数量上限
        :param output_fields: Milvus 返回字段列表
        :return: RRF 合并后的候选结果
        """

        raise NotImplementedError


class MilvusResourceWriterClient(ABC):
    """资源向量替换所需能力。"""

    @abstractmethod
    def upsert(
        self,
        collection_name: str,
        data: list[dict[str, object]],
    ) -> dict[str, object]:
        """
        写入或覆盖向量实体

        :param collection_name: Milvus Collection 名称
        :param data: 待写入或检索的数据
        :return: Milvus Upsert 执行结果
        """

        raise NotImplementedError

    @abstractmethod
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
        :return: Milvus 删除数量结果
        """

        raise NotImplementedError
