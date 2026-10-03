"""BGE-M3 内部 Embedding API 客户端。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/08/12 22:37
"""

import httpx

from union_talk_agent.agent_run.domain.exceptions import (
    AgentErrorCode,
    RetryableAgentError,
)
from union_talk_agent.knowledge.application.ports.embedding_gateway import HybridEmbedding


class OpenAiEmbeddingClient:
    """调用独立 embedding-api 生成 BGE-M3 编码和重排分数。"""

    def __init__(
        self,
        http_client: httpx.AsyncClient,
        api_base: str,
        api_key: str,
        model: str,
        dimension: int,
        timeout_seconds: int,
    ) -> None:
        """
        初始化内部 Embedding 客户端

        :param http_client: 异步 HTTP 客户端
        :param api_base: 内部 Embedding API 地址
        :param api_key: 内部调用凭证
        :param model: Embedding 模型标识
        :param dimension: Dense 向量维度
        :param timeout_seconds: 请求超时秒数
        :return: 无返回值
        """

        self._http_client = http_client
        self._api_base = api_base
        self._api_key = api_key
        self._model = model
        self._dimension = dimension
        self._timeout_seconds = timeout_seconds

    async def embed(self, text_list: list[str]) -> list[list[float]]:
        """
        生成与输入顺序一致的 Dense 向量列表

        :param text_list: 待生成向量的文本列表
        :return: 与输入顺序一致的 Dense 向量列表
        """

        return [item.dense for item in await self.encode_hybrid(text_list)]

    async def encode_hybrid(self, text_list: list[str]) -> list[HybridEmbedding]:
        """
        生成 BGE-M3 Dense 与 Sparse 联合向量

        :param text_list: 待生成向量的文本列表
        :return: 与输入顺序一致的联合向量列表
        """

        if not text_list:
            return []
        try:
            response = await self._http_client.post(
                f"{self._api_base.rstrip('/')}/internal/v1/embeddings",
                headers=self._headers(),
                json={"model": self._model, "input": text_list},
                timeout=self._timeout_seconds,
            )
            response.raise_for_status()
            payload = response.json()
            embedding_list = [
                HybridEmbedding(
                    dense=[float(value) for value in item["dense"]],
                    sparse={int(key): float(value) for key, value in item["sparse"].items()},
                )
                for item in payload["data"]
            ]
        except (httpx.HTTPError, KeyError, TypeError, ValueError) as error:
            raise RetryableAgentError(
                AgentErrorCode.INFRASTRUCTURE_UNAVAILABLE,
                "Embedding服务请求失败",
            ) from error
        if len(embedding_list) != len(text_list) or any(
            len(item.dense) != self._dimension for item in embedding_list
        ):
            raise RetryableAgentError(
                AgentErrorCode.INFRASTRUCTURE_UNAVAILABLE,
                "Embedding服务返回的向量数量或维度非法",
            )
        return embedding_list

    async def rerank(self, query: str, document_list: list[str]) -> list[float]:
        """
        对回表后的候选正文执行 BGE-M3 重排

        :param query: 检索问题
        :param document_list: 候选正文列表
        :return: 与候选顺序一致的重排分数
        """

        if not document_list:
            return []
        try:
            response = await self._http_client.post(
                f"{self._api_base.rstrip('/')}/internal/v1/rerank",
                headers=self._headers(),
                json={"model": self._model, "query": query, "documents": document_list},
                timeout=self._timeout_seconds,
            )
            response.raise_for_status()
            payload = response.json()
            score_list = [float(value) for value in payload["scores"]]
        except (httpx.HTTPError, KeyError, TypeError, ValueError) as error:
            raise RetryableAgentError(
                AgentErrorCode.INFRASTRUCTURE_UNAVAILABLE,
                "Rerank服务请求失败",
            ) from error
        if len(score_list) != len(document_list):
            raise RetryableAgentError(
                AgentErrorCode.INFRASTRUCTURE_UNAVAILABLE,
                "Rerank服务返回数量非法",
            )
        return score_list

    def _headers(self) -> dict[str, str]:
        """
        构建内部模型服务请求头

        :return: 内部 API 请求头
        """

        headers = {"Content-Type": "application/json"}
        if self._api_key:
            headers["Authorization"] = f"Bearer {self._api_key}"
        return headers
