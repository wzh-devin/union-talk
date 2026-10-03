"""FastAPI 应用工厂。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/29 19:08
"""

import logging
from collections.abc import AsyncGenerator
from contextlib import asynccontextmanager

from fastapi import FastAPI, Request, status
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from starlette.exceptions import HTTPException as StarletteHttpException

from union_talk_agent.agent_run.domain.exceptions import AgentDomainError, AgentErrorCode
from union_talk_agent.bootstrap.container import ApplicationContainer
from union_talk_agent.interfaces.http.routers.agent_config_router import (
    create_agent_control_plane_router,
)
from union_talk_agent.interfaces.http.routers.agent_run_router import (
    create_agent_run_router,
)
from union_talk_agent.interfaces.http.schemas.api_result import (
    AgentApiErrorCode,
    api_failure,
)

logger = logging.getLogger(__name__)


def create_app(container: ApplicationContainer) -> FastAPI:
    """
    创建绑定显式依赖容器的 FastAPI 应用

    :param container: 应用依赖容器
    :return: 完成依赖装配的 FastAPI 应用
    """

    @asynccontextmanager
    async def lifespan(_: FastAPI) -> AsyncGenerator[None]:
        """
        管理 Agent HTTP 应用生命周期

        :param _: 未使用的位置值
        :yield: 应用生命周期控制权
        """

        try:
            container.message_client.start()
            if container.milvus_manager is not None:
                await container.milvus_manager.ensure_collection()
            await container.service_registry.start()
            yield
        finally:
            await container.close()

    app = FastAPI(
        title="Union Talk Agent",
        version="0.1.0",
        lifespan=lifespan,
    )
    app.include_router(
        create_agent_control_plane_router(
            container.control_plane_service,
            container.message_client,
        )
    )
    app.include_router(
        create_agent_run_router(
            container.run_query_service,
            container.event_publisher,
            container.run_cancellation_service,
            container.message_client,
        )
    )

    @app.exception_handler(AgentDomainError)
    async def handle_agent_error(
        _: Request,
        error: AgentDomainError,
    ) -> JSONResponse:
        """
        将 Agent 业务异常转换为 HTTP 响应

        :param _: 未使用的位置值
        :param error: 已捕获的异常
        :return: 包含业务错误码和脱敏消息的 JSON 响应
        """

        status_code = _status_for_error(error.code)
        return _api_error_response(
            status_code,
            int(AgentApiErrorCode[error.code.name]),
            str(error),
        )

    @app.exception_handler(StarletteHttpException)
    async def handle_http_error(
        _: Request,
        error: StarletteHttpException,
    ) -> JSONResponse:
        """
        将 HTTP 异常转换为统一失败响应

        :param _: 当前 HTTP 请求
        :param error: 已捕获的 HTTP 异常
        :return: Java ApiResult 兼容失败响应
        """

        return _api_error_response(
            error.status_code,
            _error_code_for_http_status(error.status_code),
            error.detail,
        )

    @app.exception_handler(RequestValidationError)
    async def handle_validation_error(
        _: Request,
        error: RequestValidationError,
    ) -> JSONResponse:
        """
        将 FastAPI 参数校验异常转换为统一失败响应

        :param _: 当前 HTTP 请求
        :param error: 已捕获的参数校验异常
        :return: Java ApiResult 兼容失败响应
        """

        logger.warning("Agent请求参数校验失败, errorCount=%s", len(error.errors()))
        return _api_error_response(
            status.HTTP_400_BAD_REQUEST,
            int(AgentApiErrorCode.PARAM_ERROR),
            "请求参数错误",
        )

    @app.exception_handler(Exception)
    async def handle_unexpected_error(
        _: Request,
        error: Exception,
    ) -> JSONResponse:
        """
        将未知异常转换为不泄露内部信息的统一失败响应

        :param _: 当前 HTTP 请求
        :param error: 未分类异常
        :return: Java ApiResult 兼容失败响应
        """

        logger.exception("Agent API出现未分类异常", exc_info=error)
        return _api_error_response(
            status.HTTP_500_INTERNAL_SERVER_ERROR,
            int(AgentApiErrorCode.SYSTEM_ERROR),
            "系统未知异常",
        )

    @app.get("/health/live")
    async def live() -> dict[str, str]:
        """
        返回进程存活状态

        :return: 表示进程存活的状态字典
        """

        return {"status": "UP"}

    @app.get("/health/ready")
    async def ready() -> JSONResponse:
        """
        返回服务就绪状态

        :return: 包含 PostgreSQL、Redis、Milvus 与 Nacos 状态的就绪响应
        """

        postgres_ready = await container.database.check()
        milvus_ready = (
            await container.milvus_manager.check() if container.milvus_manager is not None else True
        )
        redis_ready = (
            await container.event_publisher.check() if container.settings.redis.enabled else True
        )
        nacos_ready = container.service_registry.is_ready
        is_ready = postgres_ready and redis_ready and milvus_ready and nacos_ready
        return JSONResponse(
            status_code=(status.HTTP_200_OK if is_ready else status.HTTP_503_SERVICE_UNAVAILABLE),
            content={
                "status": "UP" if is_ready else "DOWN",
                "postgres": postgres_ready,
                "redis": redis_ready,
                "realtimeEnabled": container.settings.redis.enabled,
                "milvus": milvus_ready,
                "nacos": nacos_ready,
            },
        )

    return app


def _api_error_response(
    status_code: int,
    err_code: int,
    err_msg: str,
) -> JSONResponse:
    """
    构造统一失败 JSON 响应

    :param status_code: HTTP 状态码
    :param err_code: 接口错误码
    :param err_msg: 脱敏错误信息
    :return: Java ApiResult 兼容 JSON 响应
    """

    result = api_failure(err_code, err_msg)
    return JSONResponse(
        status_code=status_code,
        content=result.model_dump(by_alias=True, mode="json"),
    )


def _error_code_for_http_status(status_code: int) -> int:
    """
    将 HTTP 状态映射为统一接口错误码

    :param status_code: HTTP 状态码
    :return: 对应接口错误码
    """

    error_code_map = {
        status.HTTP_400_BAD_REQUEST: AgentApiErrorCode.PARAM_ERROR,
        status.HTTP_401_UNAUTHORIZED: AgentApiErrorCode.UNAUTHORIZED,
        status.HTTP_403_FORBIDDEN: AgentApiErrorCode.PERMISSION_DENIED,
        status.HTTP_404_NOT_FOUND: AgentApiErrorCode.NOT_FOUND,
    }
    return int(error_code_map.get(status_code, AgentApiErrorCode.BUSINESS_ERROR))


def _status_for_error(error_code: AgentErrorCode) -> int:
    """
    将 Agent 错误码映射为 HTTP 状态码

    :param error_code: 错误码
    :return: 对应的 HTTP 状态码
    """

    if error_code in {AgentErrorCode.RUN_NOT_FOUND, AgentErrorCode.CONFIG_NOT_FOUND}:
        return status.HTTP_404_NOT_FOUND
    if error_code is AgentErrorCode.CONFIG_VERSION_CONFLICT:
        return status.HTTP_409_CONFLICT
    if error_code is AgentErrorCode.PERMISSION_DENIED:
        return status.HTTP_403_FORBIDDEN
    if error_code in {
        AgentErrorCode.INFRASTRUCTURE_UNAVAILABLE,
        AgentErrorCode.CONTEXT_UNAVAILABLE,
    }:
        return status.HTTP_503_SERVICE_UNAVAILABLE
    if error_code in {
        AgentErrorCode.MODEL_REQUEST_FAILED,
        AgentErrorCode.MESSAGE_REPLY_FAILED,
    }:
        return status.HTTP_502_BAD_GATEWAY
    return status.HTTP_400_BAD_REQUEST
