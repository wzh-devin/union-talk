"""Embedding API 启动入口测试。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/08/13 11:18
"""

from typing import cast

import pytest
import uvicorn
from fastapi import FastAPI

from union_talk_agent.bootstrap import embedding_api


class _ServerStub:
    """记录 Uvicorn 异步启动调用。"""

    def __init__(self) -> None:
        """
        初始化未启动的服务桩

        :return: 无返回值
        """

        self.serve_called = False

    async def serve(self) -> None:
        """
        记录异步服务启动调用

        :return: 无返回值
        """

        self.serve_called = True


@pytest.mark.asyncio
async def test_run_uses_async_uvicorn_server(monkeypatch: pytest.MonkeyPatch) -> None:
    """
    验证 Embedding API 直接等待 Uvicorn 异步服务

    :param monkeypatch: Pytest 属性替换工具
    :return: 无返回值
    """

    server = _ServerStub()

    def create_server(_config: uvicorn.Config) -> _ServerStub:
        """
        返回可观察的 Uvicorn 服务桩

        :param _config: Uvicorn 服务配置
        :return: Uvicorn 服务桩
        """

        return server

    def create_config(_app: FastAPI, **_kwargs: object) -> uvicorn.Config:
        """
        返回无需真实网络配置的 Uvicorn 配置桩

        :param _app: FastAPI 测试应用
        :param _kwargs: Uvicorn 配置参数
        :return: Uvicorn 配置桩
        """

        return cast(uvicorn.Config, object())

    def create_test_app() -> FastAPI:
        """
        创建不加载本地模型的测试应用

        :return: FastAPI 测试应用
        """

        return FastAPI()

    monkeypatch.setattr(embedding_api.uvicorn, "Server", create_server)
    monkeypatch.setattr(embedding_api.uvicorn, "Config", create_config)
    monkeypatch.setattr(embedding_api, "create_app", create_test_app)

    await embedding_api.run()

    assert server.serve_called is True
