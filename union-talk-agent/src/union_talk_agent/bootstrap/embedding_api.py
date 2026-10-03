"""Embedding API 进程启动入口。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/08/12 22:37
"""

import asyncio
from collections.abc import AsyncGenerator
from contextlib import asynccontextmanager

import uvicorn
from fastapi import FastAPI

from union_talk_agent.infrastructure.embedding.bge_m3_model import BgeM3Model
from union_talk_agent.interfaces.http.routers.embedding_router import create_embedding_router
from union_talk_agent.settings import load_settings


def create_app() -> FastAPI:
    """
    创建独立 Embedding API 应用

    :return: 仅暴露内部模型接口的 FastAPI 应用
    """

    settings = load_settings()
    model = BgeM3Model(
        settings.embedding.model,
        batch_size=settings.embedding.batch_size,
        batch_wait_milliseconds=settings.embedding.batch_wait_milliseconds,
        queue_capacity=settings.embedding.queue_capacity,
    )

    @asynccontextmanager
    async def lifespan(_app: FastAPI) -> AsyncGenerator[None]:
        """
        在应用生命周期内维护 BGE-M3 模型与微批任务

        :param _app: FastAPI 应用实例
        :yield: 应用运行控制权
        """

        await model.start()
        try:
            yield
        finally:
            await model.close()

    app = FastAPI(title="Union Talk Embedding API", version="2.0.0", lifespan=lifespan)
    app.include_router(create_embedding_router(model))
    return app


async def run() -> None:
    """
    构造并异步运行 Embedding API 服务

    直接等待 ``Server.serve``，避免 PyCharm asyncio 调试补丁与 Uvicorn
    ``Server.run`` 的 ``loop_factory`` 参数发生冲突。

    :return: 无返回值
    """

    settings = load_settings()
    server = uvicorn.Server(
        uvicorn.Config(
            create_app(),
            host=settings.embedding.host,
            port=settings.embedding.port,
            log_level=settings.app.log_level.lower(),
        )
    )
    try:
        await server.serve()
    except KeyboardInterrupt:
        return


def main() -> None:
    """
    启动 Embedding API 进程

    :return: 无返回值
    """

    asyncio.run(run())


if __name__ == "__main__":
    main()
