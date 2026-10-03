"""Agent Run 取消用例。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/29 19:24
"""

from union_talk_agent.agent_run.application.ports.agent_unit_of_work import (
    AgentUnitOfWorkFactory,
)
from union_talk_agent.agent_run.application.run_lifecycle_service import RunLifecycleService
from union_talk_agent.agent_run.domain.agent_run import AgentRun
from union_talk_agent.agent_run.domain.enums import AgentRunStage, AgentRunStatus
from union_talk_agent.agent_run.domain.exceptions import AgentDomainError, AgentErrorCode


class RunCancellationService:
    """校验发起者或管理员权限并取消 Run。"""

    def __init__(
        self,
        unit_of_work_factory: AgentUnitOfWorkFactory,
        lifecycle_service: RunLifecycleService,
    ) -> None:
        """
        初始化 RunCancellationService

        :param unit_of_work_factory: Agent 工作单元工厂
        :param lifecycle_service: Agent Run 生命周期服务
        :return: 无返回值
        """

        self._unit_of_work_factory = unit_of_work_factory
        self._lifecycle_service = lifecycle_service

    async def cancel(
        self,
        run_id: int,
        requested_by: int,
        requester_can_manage_agent_run: bool,
    ) -> AgentRun:
        """
        取消指定 Run 并返回最新状态

        :param run_id: Agent Run ID
        :param requested_by: 发起操作的用户 ID
        :param requester_can_manage_agent_run: 发起用户是否可以管理其他成员的Agent Run
        :return: 取消后的最新 Agent Run
        """

        async with self._unit_of_work_factory() as unit_of_work:
            run = await unit_of_work.agent_runs.get_by_id(run_id)
        if run is None:
            raise AgentDomainError(AgentErrorCode.RUN_NOT_FOUND, "Agent Run不存在")
        if run.requester_user_id != requested_by and not requester_can_manage_agent_run:
            raise AgentDomainError(
                AgentErrorCode.PERMISSION_DENIED,
                "只有发起者或群主、管理员可以取消Agent Run",
            )
        if run.status is AgentRunStatus.CANCELLED:
            return run
        if run.is_terminal:
            raise AgentDomainError(
                AgentErrorCode.RUN_STATE_INVALID,
                "已完成的Agent Run不能取消",
            )
        if run.stage is AgentRunStage.FINALIZING:
            raise AgentDomainError(
                AgentErrorCode.RUN_STATE_INVALID,
                "Agent Run正在保存正式回复，不能取消",
            )
        return await self._lifecycle_service.cancel(run_id, requested_by)
