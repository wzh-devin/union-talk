"""Embedding 边界。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/29 19:15
"""

from typing import Protocol

from pydantic import BaseModel, Field


class HybridEmbedding(BaseModel):
    """BGE-M3 Dense 与 Sparse 联合编码结果。"""

    dense: list[float]
    sparse: dict[int, float] = Field(default_factory=lambda: dict[int, float]())


class EmbeddingGateway(Protocol):
    """批量文本向量化能力。"""

    async def embed(self, text_list: list[str]) -> list[list[float]]:
        """
        生成与输入顺序一致的向量

        :param text_list: 待生成向量的文本列表
        :return: 与输入文本顺序一致的向量列表
        """

        ...

    async def encode_hybrid(self, text_list: list[str]) -> list[HybridEmbedding]:
        """
        生成 Dense 与 Sparse 联合向量

        :param text_list: 待编码文本列表
        :return: 与输入顺序一致的联合向量列表
        """

        ...

    async def rerank(self, query: str, document_list: list[str]) -> list[float]:
        """
        使用 BGE-M3 多向量能力重排候选文本

        :param query: 检索问题
        :param document_list: 待重排候选正文列表
        :return: 与候选顺序一致的相关性分数
        """

        ...
