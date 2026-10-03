"""Agent API 进程入口。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/29 19:08
"""

import asyncio

import uvicorn

from union_talk_agent.bootstrap.container import build_container
from union_talk_agent.bootstrap.logging_config import configure_logging
from union_talk_agent.interfaces.http.app import create_app
from union_talk_agent.settings import load_settings


async def run() -> None:
    """
    构造并异步运行 FastAPI 服务

    直接等待 ``Server.serve``，避免 IDE 对 ``asyncio.run`` 的调试补丁与
    Uvicorn ``Server.run`` 传入的 ``loop_factory`` 参数发生冲突。

    :return: 无返回值
    """

    settings = load_settings()
    configure_logging(settings.app.log_level)
    container = build_container(settings)
    app = create_app(container)
    server = uvicorn.Server(
        uvicorn.Config(
            app,
            host=settings.app.api_host,
            port=settings.app.api_port,
            log_level=settings.app.log_level.lower(),
        )
    )
    try:
        await server.serve()
    except KeyboardInterrupt:
        return


def main() -> None:
    """
    启动 Agent API 进程

    :return: 无返回值
    """

    asyncio.run(run())
