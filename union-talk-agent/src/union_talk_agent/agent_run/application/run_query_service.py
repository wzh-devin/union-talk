"""Agent Run 查询用例。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/29 19:05
"""

from datetime import datetime

from union_talk_agent.agent_run.application.ports.agent_unit_of_work import (
    AgentUnitOfWorkFactory,
)
from union_talk_agent.agent_run.application.ports.run_event_publisher import RunEventPublisher
from union_talk_agent.agent_run.application.run_cursor_page import AgentRunCursorPage
from union_talk_agent.agent_run.domain.agent_run import AgentRun
from union_talk_agent.agent_run.domain.exceptions import AgentDomainError, AgentErrorCode


class RunQueryService:
    """查询 Run、活动卡片、Snapshot 与脱敏轨迹。"""

    def __init__(
        self,
        unit_of_work_factory: AgentUnitOfWorkFactory,
        event_publisher: RunEventPublisher,
    ) -> None:
        """
        初始化 RunQueryService

        :param unit_of_work_factory: Agent 工作单元工厂
        :param event_publisher: Agent 实时事件发布端口
        :return: 无返回值
        """

        self._unit_of_work_factory = unit_of_work_factory
        self._event_publisher = event_publisher

    async def get_run(self, run_id: int) -> AgentRun:
        """
        按 ID 获取 Run

        :param run_id: Agent Run ID
        :return: Agent Run；不存在时返回空
        """

        async with self._unit_of_work_factory() as unit_of_work:
            run = await unit_of_work.agent_runs.get_by_id(run_id)
        if run is None:
            raise AgentDomainError(AgentErrorCode.RUN_NOT_FOUND, "Agent Run不存在")
        return run

    async def list_active_runs(self, conversation_id: int) -> list[AgentRun]:
        """
        查询会话中仍需展示临时卡片的 Run

        :param conversation_id: 会话 ID
        :return: 当前会话活动 Run 列表
        """

        async with self._unit_of_work_factory() as unit_of_work:
            return await unit_of_work.agent_runs.list_active_by_conversation_id(conversation_id)

    async def list_runs(
        self,
        conversation_id: int,
        page_size: int,
        cursor_value: datetime | None,
        cursor_id: int | None,
    ) -> AgentRunCursorPage:
        """
        按双游标查询会话 Agent Run

        :param conversation_id: 会话 ID
        :param page_size: 当前页展示数量
        :param cursor_value: 上一页末项排队时间
        :param cursor_id: 上一页末项 Run ID
        :return: 包含下一页游标的 Agent Run 分页结果
        """

        async with self._unit_of_work_factory() as unit_of_work:
            candidate_list = await unit_of_work.agent_runs.list_by_conversation_id(
                conversation_id,
                page_size,
                cursor_value,
                cursor_id,
            )
        return AgentRunCursorPage.from_candidate_list(candidate_list, page_size)

    async def get_snapshot(self, run_id: int) -> dict[str, object]:
        """
        优先读取 Redis Snapshot，过期时回退 PostgreSQL 终态

        :param run_id: Agent Run ID
        :return: Agent 卡片快照；不可用时返回空
        """

        snapshot = await self._event_publisher.get_snapshot(run_id)
        if snapshot is not None:
            return snapshot
        run = await self.get_run(run_id)
        return {
            "schemaVersion": 2,
            "runId": str(run.run_id),
            "conversationId": str(run.conversation_id),
            "triggerMessageId": str(run.trigger_message_id),
            "requesterUserId": str(run.requester_user_id),
            "cardKey": f"agent-run:{run.run_id}",
            "status": str(run.status),
            "stage": str(run.stage),
            "currentTurnNo": None,
            "messageList": [],
            "toolExecutionList": [],
            "lastSequence": run.last_event_sequence,
            "answerMessageId": (
                str(run.answer_message_id) if run.answer_message_id is not None else None
            ),
            "citationList": [],
            "errorCode": run.error_code,
            "errorMessage": run.error_message,
        }

    async def get_trace(self, run_id: int) -> list[dict[str, object]]:
        """
        查询成员可见的结构化执行轨迹

        :param run_id: Agent Run ID
        :return: 成员可见的执行轨迹列表
        """

        await self.get_run(run_id)
        async with self._unit_of_work_factory() as unit_of_work:
            return await unit_of_work.agent_traces.list_by_run_id(run_id)

    async def get_runtime_trace(self, run_id: int) -> dict[str, object]:
        """
        查询按 Turn 聚合的 V2 执行轨迹

        :param run_id: Agent Run ID
        :return: Turn、Message、Content Block 和工具执行聚合
        """

        await self.get_run(run_id)
        async with self._unit_of_work_factory() as unit_of_work:
            runtime_trace = await unit_of_work.agent_traces.get_runtime_trace(run_id)
            runtime_trace["stepList"] = await unit_of_work.agent_traces.list_by_run_id(run_id)
            return runtime_trace
