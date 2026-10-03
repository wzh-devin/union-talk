"""BGE-M3 内部 Embedding 与 Rerank HTTP 接口。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/08/12 22:37
"""

from fastapi import APIRouter
from pydantic import BaseModel, ConfigDict, Field
from pydantic.alias_generators import to_camel

from union_talk_agent.infrastructure.embedding.bge_m3_model import BgeM3Model


class EmbeddingRequest(BaseModel):
    """内部联合向量请求。"""

    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)

    model: str
    input: list[str] = Field(min_length=1, max_length=128)


class RerankRequest(BaseModel):
    """内部候选重排请求。"""

    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)

    model: str
    query: str = Field(min_length=1)
    documents: list[str] = Field(min_length=1, max_length=256)


def create_embedding_router(model: BgeM3Model) -> APIRouter:
    """
    创建只由 embedding-api 挂载的内部模型路由

    :param model: 常驻 BGE-M3 模型
    :return: 内部 Embedding Router
    """

    router = APIRouter(prefix="/internal/v1", tags=["embedding-internal"])

    @router.post("/embeddings")
    async def embeddings(request: EmbeddingRequest) -> dict[str, object]:
        """
        返回 Dense 与 Sparse 联合向量

        :param request: 批量向量请求
        :return: 联合向量响应
        """

        embedding_list = await model.encode(request.input)
        return {
            "model": request.model,
            "data": [
                {
                    "index": index,
                    "dense": item.dense,
                    "sparse": {str(key): value for key, value in item.sparse.items()},
                }
                for index, item in enumerate(embedding_list)
            ],
        }

    @router.post("/rerank")
    async def rerank(request: RerankRequest) -> dict[str, object]:
        """
        返回候选正文的 BGE-M3 混合重排分数

        :param request: 重排请求
        :return: 与候选顺序一致的分数响应
        """

        return {
            "model": request.model,
            "scores": await model.rerank(request.query, request.documents),
        }

    @router.get("/health")
    async def health() -> dict[str, str]:
        """
        返回内部模型进程健康状态

        :return: 健康状态
        """

        await model.load()
        return {"status": "UP"}

    return router
