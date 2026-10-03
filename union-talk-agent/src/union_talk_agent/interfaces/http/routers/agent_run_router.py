"""Agent Run、Snapshot、轨迹与独立 SSE 路由。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/29 19:06
"""

from collections.abc import AsyncIterator
from datetime import datetime
from typing import Annotated

import orjson
from fastapi import APIRouter, Depends, Header, Query
from fastapi.responses import StreamingResponse

from union_talk_agent.access_control.application.ports.conversation_permission_reader import (
    ConversationPermissionReader,
)
from union_talk_agent.agent_run.application.ports.run_event_publisher import (
    RunEventPublisher,
    RunEventReader,
    RunEventReadError,
)
from union_talk_agent.agent_run.application.ports.run_stream_query import RunStreamQuery
from union_talk_agent.agent_run.application.run_cancellation_service import (
    RunCancellationService,
)
from union_talk_agent.agent_run.application.run_query_service import RunQueryService
from union_talk_agent.agent_run.domain.constants import (
    DEFAULT_AGENT_RUN_PAGE_SIZE,
    MAX_AGENT_RUN_PAGE_SIZE,
)
from union_talk_agent.agent_run.domain.enums import (
    AgentRealtimeEventType,
    AgentRunStatus,
)
from union_talk_agent.interfaces.http.request_principal import (
    RequestPrincipal,
    require_authenticated_user,
    require_conversation_member,
)
from union_talk_agent.interfaces.http.schemas.agent_run_page_response import (
    AgentRunPageResponse,
)
from union_talk_agent.interfaces.http.schemas.agent_run_response import AgentRunResponse
from union_talk_agent.interfaces.http.schemas.api_result import ApiResult, api_success


def create_agent_run_router(
    query_service: RunQueryService,
    event_publisher: RunEventPublisher,
    cancellation_service: RunCancellationService,
    permission_reader: ConversationPermissionReader,
) -> APIRouter:
    """
    创建显式注入查询和实时端口的 Run 路由

    :param query_service: Agent Run 查询服务
    :param event_publisher: Agent 实时事件发布端口
    :param cancellation_service: Agent Run 取消服务
    :param permission_reader: 会话权限读取端口
    :return: Agent Run Router
    """

    router = APIRouter(prefix="/api/v1", tags=["agent-run"])

    @router.get(
        "/conversations/{conversation_id}/active-runs",
        response_model=ApiResult[list[AgentRunResponse]],
    )
    async def list_active_runs(
        conversation_id: int,
        principal: Annotated[RequestPrincipal, Depends(require_authenticated_user)],
    ) -> ApiResult[list[AgentRunResponse]]:
        """
        查询当前会话活动 Agent Run

        :param conversation_id: 会话 ID
        :param principal: 当前请求身份
        :return: 当前会话活动 Run 列表
        """

        await require_conversation_member(
            permission_reader,
            principal,
            conversation_id,
        )
        run_list = await query_service.list_active_runs(conversation_id)
        return api_success([AgentRunResponse.from_domain(run) for run in run_list])

    @router.get(
        "/conversations/{conversation_id}/runs",
        response_model=ApiResult[AgentRunPageResponse],
    )
    async def list_runs(
        conversation_id: int,
        principal: Annotated[RequestPrincipal, Depends(require_authenticated_user)],
        page_size: Annotated[
            int,
            Query(alias="pageSize", ge=1, le=MAX_AGENT_RUN_PAGE_SIZE),
        ] = DEFAULT_AGENT_RUN_PAGE_SIZE,
        cursor_value: Annotated[
            datetime | None,
            Query(alias="cursorValue"),
        ] = None,
        cursor_id: Annotated[int | None, Query(alias="cursorId")] = None,
    ) -> ApiResult[AgentRunPageResponse]:
        """
        按双游标查询当前会话 Agent Run

        :param conversation_id: 会话 ID
        :param principal: 当前请求身份
        :param page_size: 当前页展示数量
        :param cursor_value: 上一页末项排队时间
        :param cursor_id: 上一页末项 Run ID
        :return: 与 Java CursorPageResult 同构的 Agent Run 游标页
        """

        await require_conversation_member(
            permission_reader,
            principal,
            conversation_id,
        )
        page = await query_service.list_runs(
            conversation_id,
            page_size,
            cursor_value,
            cursor_id,
        )
        return api_success(
            AgentRunPageResponse(
                list=[AgentRunResponse.from_domain(run) for run in page.run_list],
                has_next=page.has_next,
                next_cursor_value=page.next_cursor_value,
                next_cursor_id=(
                    str(page.next_cursor_id) if page.next_cursor_id is not None else None
                ),
            )
        )

    @router.get("/runs/{run_id}", response_model=ApiResult[AgentRunResponse])
    async def get_run(
        run_id: int,
        principal: Annotated[RequestPrincipal, Depends(require_authenticated_user)],
    ) -> ApiResult[AgentRunResponse]:
        """
        查询指定 Agent Run

        :param run_id: Agent Run ID
        :param principal: 当前请求身份
        :return: Agent Run；不存在时返回空
        """

        run = await query_service.get_run(run_id)
        await require_conversation_member(
            permission_reader,
            principal,
            run.conversation_id,
        )
        return api_success(AgentRunResponse.from_domain(run))

    @router.get(
        "/runs/{run_id}/snapshot",
        response_model=ApiResult[dict[str, object]],
    )
    async def get_snapshot(
        run_id: int,
        principal: Annotated[RequestPrincipal, Depends(require_authenticated_user)],
    ) -> ApiResult[dict[str, object]]:
        """
        查询 Agent Run 卡片快照

        :param run_id: Agent Run ID
        :param principal: 当前请求身份
        :return: Agent 卡片快照；不可用时返回空
        """

        run = await query_service.get_run(run_id)
        await require_conversation_member(
            permission_reader,
            principal,
            run.conversation_id,
        )
        return api_success(await query_service.get_snapshot(run_id))

    @router.get(
        "/runs/{run_id}/trace",
        response_model=ApiResult[dict[str, object]],
    )
    async def get_trace(
        run_id: int,
        principal: Annotated[RequestPrincipal, Depends(require_authenticated_user)],
    ) -> ApiResult[dict[str, object]]:
        """
        查询 Agent Run 执行轨迹

        :param run_id: Agent Run ID
        :param principal: 当前请求身份
        :return: 成员可见的执行轨迹列表
        """

        run = await query_service.get_run(run_id)
        await require_conversation_member(
            permission_reader,
            principal,
            run.conversation_id,
        )
        return api_success(
            {
                "runId": str(run_id),
                **await query_service.get_runtime_trace(run_id),
            }
        )

    @router.get("/runs/{run_id}/events")
    async def stream_events(
        run_id: int,
        principal: Annotated[RequestPrincipal, Depends(require_authenticated_user)],
        last_event_id: Annotated[str | None, Header(alias="Last-Event-ID")] = None,
    ) -> StreamingResponse:
        """
        创建 Agent Run SSE 响应

        :param run_id: Agent Run ID
        :param principal: 当前请求身份
        :param last_event_id: 客户端最后接收的 SSE 事件 ID
        :return: 按 `text/event-stream` 输出的流式响应
        """

        run = await query_service.get_run(run_id)
        await require_conversation_member(
            permission_reader,
            principal,
            run.conversation_id,
        )
        return StreamingResponse(
            stream_run_events(
                run_id,
                query_service,
                event_publisher,
                last_event_id,
            ),
            media_type="text/event-stream",
            headers={
                "Cache-Control": "no-cache",
                "X-Accel-Buffering": "no",
            },
        )

    @router.post(
        "/runs/{run_id}/cancel",
        response_model=ApiResult[AgentRunResponse],
    )
    async def cancel_run(
        run_id: int,
        principal: Annotated[RequestPrincipal, Depends(require_authenticated_user)],
    ) -> ApiResult[AgentRunResponse]:
        """
        取消指定 Agent Run

        :param run_id: Agent Run ID
        :param principal: 当前请求身份
        :return: 取消后的 Agent Run 响应
        """

        run = await query_service.get_run(run_id)
        permission = await require_conversation_member(
            permission_reader,
            principal,
            run.conversation_id,
        )
        cancelled_run = await cancellation_service.cancel(
            run_id,
            principal.user_id,
            permission.can_manage_agent_run,
        )
        return api_success(AgentRunResponse.from_domain(cancelled_run))

    return router


async def stream_run_events(
    run_id: int,
    query_service: RunStreamQuery,
    event_publisher: RunEventReader,
    last_event_id: str | None,
) -> AsyncIterator[str]:
    """
    持续读取并输出 Agent Run 事件

    :param run_id: Agent Run ID
    :param query_service: Agent Run 查询服务
    :param event_publisher: Agent 实时事件发布端口
    :param last_event_id: 客户端最后接收的 SSE 事件 ID
    :yield: 符合 SSE 协议的数据帧
    """

    cursor = last_event_id or "0-0"
    snapshot = await query_service.get_snapshot(run_id)
    raw_sequence = snapshot.get("lastSequence", 0)
    minimum_sequence = int(raw_sequence) if isinstance(raw_sequence, int | str) else 0
    yield _sse_data("snapshot", snapshot)
    if _snapshot_is_terminal(snapshot):
        return
    while True:
        try:
            stored_event_list = await event_publisher.read_after(
                run_id,
                cursor,
                15_000,
            )
        except RunEventReadError:
            yield _sse_data(
                "realtime-unavailable",
                {"runId": str(run_id), "message": "实时事件暂不可用"},
            )
            return
        if not stored_event_list:
            yield ": heartbeat\n\n"
            continue
        for stored_event in stored_event_list:
            cursor = stored_event.stream_id
            event = stored_event.event
            if event.sequence <= minimum_sequence:
                continue
            yield _sse_data(
                "agent-event",
                event.model_dump(by_alias=True, mode="json"),
                cursor,
            )
            if event.event_type is AgentRealtimeEventType.AGENT_END:
                return


def _sse_data(
    event_name: str,
    payload: dict[str, object],
    event_id: str | None = None,
) -> str:
    """
    编码单条 SSE 数据帧

    :param event_name: 事件名称
    :param payload: 事件或接口载荷
    :param event_id: 事件 ID
    :return: 符合 SSE 协议的数据帧
    """

    line_list: list[str] = []
    if event_id is not None:
        line_list.append(f"id: {event_id}")
    line_list.append(f"event: {event_name}")
    line_list.append(f"data: {orjson.dumps(payload).decode()}")
    return "\n".join(line_list) + "\n\n"


def _snapshot_is_terminal(snapshot: dict[str, object]) -> bool:
    """
    判断卡片快照是否已经终态

    :param snapshot: Agent 卡片快照
    :return: 快照是否处于终态
    """

    status_value = snapshot.get("status")
    return status_value in {
        str(AgentRunStatus.SUCCEEDED),
        str(AgentRunStatus.FAILED),
        str(AgentRunStatus.CANCELLED),
    }
