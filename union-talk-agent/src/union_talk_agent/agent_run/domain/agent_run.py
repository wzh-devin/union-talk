"""Agent Run 聚合。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/29 18:46
"""

from dataclasses import dataclass
from datetime import UTC, datetime

from union_talk_agent.agent_run.domain.enums import AgentRunStage, AgentRunStatus
from union_talk_agent.agent_run.domain.exceptions import AgentDomainError, AgentErrorCode


@dataclass(slots=True)
class AgentRun:
    """一次 `@AI` 回答运行。"""

    run_id: int
    conversation_id: int
    trigger_message_id: int
    requester_user_id: int
    binding_id: int
    binding_version: int
    agent_id: int
    agent_version: int
    credential_id: int
    credential_version: int
    credential_owner_user_id: int
    model_id: str
    status: AgentRunStatus = AgentRunStatus.QUEUED
    stage: AgentRunStage = AgentRunStage.PREPARING
    answer_message_id: int | None = None
    last_event_sequence: int = 0
    error_code: str | None = None
    error_message: str | None = None
    queued_at: datetime | None = None
    started_at: datetime | None = None
    completed_at: datetime | None = None
    cancel_requested_at: datetime | None = None
    cancel_requested_by: int | None = None

    @classmethod
    def create(
        cls,
        *,
        run_id: int,
        conversation_id: int,
        trigger_message_id: int,
        requester_user_id: int,
        binding_id: int,
        binding_version: int,
        agent_id: int,
        agent_version: int,
        credential_id: int,
        credential_version: int,
        credential_owner_user_id: int,
        model_id: str,
    ) -> "AgentRun":
        """
        创建待执行 Run

        :param run_id: Agent Run ID
        :param conversation_id: 会话 ID
        :param trigger_message_id: 触发消息 ID
        :param requester_user_id: Agent 请求用户 ID
        :param binding_id: 会话 Agent 绑定 ID
        :param binding_version: 会话 Agent 绑定版本
        :param agent_id: 稳定 Agent ID
        :param agent_version: Agent 定义版本
        :param credential_id: Provider 凭证 ID
        :param credential_version: Provider 凭证版本
        :param credential_owner_user_id: Provider 凭证所有者用户 ID
        :param model_id: 模型标识
        :return: 待执行 Agent Run
        """

        return cls(
            run_id=run_id,
            conversation_id=conversation_id,
            trigger_message_id=trigger_message_id,
            requester_user_id=requester_user_id,
            binding_id=binding_id,
            binding_version=binding_version,
            agent_id=agent_id,
            agent_version=agent_version,
            credential_id=credential_id,
            credential_version=credential_version,
            credential_owner_user_id=credential_owner_user_id,
            model_id=model_id,
            queued_at=datetime.now(UTC),
        )

    def start(self) -> None:
        """
        将 Run 转换为运行中

        :return: 无返回值
        """

        if self.status not in {AgentRunStatus.QUEUED, AgentRunStatus.RUNNING}:
            raise AgentDomainError(
                AgentErrorCode.RUN_STATE_INVALID,
                f"当前状态不允许启动: {self.status}",
            )
        self.status = AgentRunStatus.RUNNING
        self.stage = AgentRunStage.PREPARING
        self.started_at = self.started_at or datetime.now(UTC)

    def change_stage(self, stage: AgentRunStage) -> None:
        """
        更新运行阶段

        :param stage: 新运行阶段
        :return: 无返回值
        """

        if self.status is not AgentRunStatus.RUNNING:
            raise AgentDomainError(AgentErrorCode.RUN_STATE_INVALID, "只有运行中 Run 可以切换阶段")
        self.stage = stage

    def succeed(self, answer_message_id: int) -> None:
        """
        标记 Run 成功

        :param answer_message_id: 正式回复消息 ID
        :return: 无返回值
        """

        if self.status is not AgentRunStatus.RUNNING:
            raise AgentDomainError(AgentErrorCode.RUN_STATE_INVALID, "只有运行中 Run 可以完成")
        self.status = AgentRunStatus.SUCCEEDED
        self.stage = AgentRunStage.FINALIZING
        self.answer_message_id = answer_message_id
        self.completed_at = datetime.now(UTC)

    def fail(self, error_code: str, error_message: str) -> None:
        """
        标记 Run 失败

        :param error_code: 脱敏错误码
        :param error_message: 脱敏错误摘要
        :return: 无返回值
        """

        if self.status in {
            AgentRunStatus.SUCCEEDED,
            AgentRunStatus.CANCELLED,
        }:
            raise AgentDomainError(AgentErrorCode.RUN_STATE_INVALID, "终态 Run 不允许标记失败")
        self.status = AgentRunStatus.FAILED
        self.error_code = error_code
        self.error_message = error_message
        self.completed_at = datetime.now(UTC)

    def requeue(self, error_code: str, error_message: str) -> None:
        """
        释放本次执行并等待 RabbitMQ 重试

        :param error_code: 最近一次可重试错误码
        :param error_message: 最近一次脱敏错误摘要
        :return: 无返回值
        """

        if self.is_terminal:
            raise AgentDomainError(AgentErrorCode.RUN_STATE_INVALID, "终态 Run 不允许重新排队")
        self.status = AgentRunStatus.QUEUED
        self.stage = AgentRunStage.PREPARING
        self.error_code = error_code
        self.error_message = error_message
        self.completed_at = None

    def cancel(self, requested_by: int) -> None:
        """
        标记 Run 已取消

        :param requested_by: 发起取消的用户 ID
        :return: 无返回值
        """

        if self.status is AgentRunStatus.SUCCEEDED:
            raise AgentDomainError(AgentErrorCode.RUN_STATE_INVALID, "成功 Run 不允许取消")
        now = datetime.now(UTC)
        self.cancel_requested_at = now
        self.cancel_requested_by = requested_by
        self.status = AgentRunStatus.CANCELLED
        self.completed_at = now

    @property
    def is_terminal(self) -> bool:
        """
        判断是否已经进入终态

        :return: 是否为成功、失败或取消
        """

        return self.status in {
            AgentRunStatus.SUCCEEDED,
            AgentRunStatus.FAILED,
            AgentRunStatus.CANCELLED,
        }
