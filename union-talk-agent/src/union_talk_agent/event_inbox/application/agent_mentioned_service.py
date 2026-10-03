"""AGENT_MENTIONED 事件可靠消费编排。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/29 19:02
"""

from dataclasses import dataclass

from union_talk_agent.agent_config.domain.enums import (
    AgentBindingStatus,
    ConnectionTestStatus,
    CredentialStatus,
)
from union_talk_agent.agent_run.application.answer_run_service import AnswerRunService
from union_talk_agent.agent_run.application.ports.agent_unit_of_work import AgentUnitOfWorkFactory
from union_talk_agent.agent_run.application.ports.id_generator import IdGenerator
from union_talk_agent.agent_run.domain.agent_run import AgentRun
from union_talk_agent.agent_run.domain.commands import StartAgentRunCommand
from union_talk_agent.agent_run.domain.constants import DEFAULT_FIXED_MODEL_ID
from union_talk_agent.agent_run.domain.exceptions import (
    AgentDomainError,
    AgentErrorCode,
    AgentRunCancelledError,
    NonRetryableAgentError,
    RetryableAgentError,
)
from union_talk_agent.event_inbox.application.inbox_lifecycle_service import (
    InboxLifecycleService,
)
from union_talk_agent.event_inbox.domain.enums import InboxStatus
from union_talk_agent.event_inbox.domain.inbox_event import InboxEvent


@dataclass(frozen=True, slots=True)
class PreparedAgentRun:
    """Inbox 准备阶段的幂等结果。"""

    run_id: int
    attempt: int
    already_processed: bool


class AgentMentionedService:
    """将至少一次投递收敛为一个 Run 和一条正式回复。"""

    def __init__(
        self,
        *,
        unit_of_work_factory: AgentUnitOfWorkFactory,
        id_generator: IdGenerator,
        answer_run_service: AnswerRunService,
        inbox_lifecycle_service: InboxLifecycleService,
        fixed_answer_enabled: bool,
        max_delivery_attempts: int,
    ) -> None:
        """
        初始化 AgentMentionedService

        :param unit_of_work_factory: Agent 工作单元工厂
        :param id_generator: 业务 ID 生成器
        :param answer_run_service: Agent 回答执行服务
        :param inbox_lifecycle_service: Inbox 状态流转服务
        :param fixed_answer_enabled: 是否启用固定回答模式
        :param max_delivery_attempts: 消息最大投递次数
        :return: 无返回值
        """

        self._unit_of_work_factory = unit_of_work_factory
        self._id_generator = id_generator
        self._answer_run_service = answer_run_service
        self._inbox_lifecycle_service = inbox_lifecycle_service
        self._fixed_answer_enabled = fixed_answer_enabled
        self._max_delivery_attempts = max_delivery_attempts

    async def handle(self, command: StartAgentRunCommand) -> int:
        """
        处理一次 Agent 提及事件

        可重试异常会释放 Run 租约并重新抛出，由 RabbitMQ 执行重投；
        不可重试异常会形成失败卡片并正常结束消费。

        :param command: 待执行的应用命令
        :return: 创建或复用的 Agent Run ID
        """

        try:
            prepared = await self._prepare(command)
        except NonRetryableAgentError as error:
            await self._inbox_lifecycle_service.record_rejected(
                command.event_id,
                "AGENT_MENTIONED",
                "MESSAGE",
                str(command.trigger_message_id),
                str(error),
            )
            raise
        if prepared.already_processed:
            return prepared.run_id
        try:
            await self._answer_run_service.execute(prepared.run_id)
        except AgentRunCancelledError:
            pass
        except RetryableAgentError as error:
            if prepared.attempt < self._max_delivery_attempts:
                await self._answer_run_service.requeue(
                    prepared.run_id,
                    str(error.code),
                    str(error),
                )
                await self._inbox_lifecycle_service.mark_failed(command.event_id, str(error))
                raise
            await self._answer_run_service.fail(
                prepared.run_id,
                str(error.code),
                str(error),
            )
        except NonRetryableAgentError as error:
            await self._answer_run_service.fail(
                prepared.run_id,
                str(error.code),
                str(error),
            )
        except AgentDomainError as error:
            await self._answer_run_service.fail(
                prepared.run_id,
                str(error.code),
                str(error),
            )
        except Exception as error:
            retryable_error = RetryableAgentError(
                AgentErrorCode.INFRASTRUCTURE_UNAVAILABLE,
                "Agent执行发生基础设施异常",
            )
            if prepared.attempt < self._max_delivery_attempts:
                await self._answer_run_service.requeue(
                    prepared.run_id,
                    str(retryable_error.code),
                    str(retryable_error),
                )
                await self._inbox_lifecycle_service.mark_failed(
                    command.event_id,
                    str(retryable_error),
                )
                raise retryable_error from error
            await self._answer_run_service.fail(
                prepared.run_id,
                str(retryable_error.code),
                str(retryable_error),
            )
        await self._inbox_lifecycle_service.mark_succeeded(command.event_id)
        return prepared.run_id

    async def _prepare(self, command: StartAgentRunCommand) -> PreparedAgentRun:
        """
        幂等创建或加载待执行 Agent Run

        :param command: 待执行的应用命令
        :return: 待执行 Agent Run；重复事件已完成时返回空
        """

        async with self._unit_of_work_factory() as unit_of_work:
            await unit_of_work.business_locks.lock("agent-event", command.event_id)
            inbox = await unit_of_work.inbox_events.get_by_event_id(command.event_id)
            if inbox is not None and inbox.status is InboxStatus.SUCCEEDED:
                existing_run = await unit_of_work.agent_runs.get_by_trigger_message_id(
                    command.trigger_message_id
                )
                if existing_run is None:
                    raise AgentDomainError(
                        AgentErrorCode.RUN_NOT_FOUND,
                        "Inbox已完成但Agent Run不存在",
                    )
                await unit_of_work.commit()
                return PreparedAgentRun(
                    run_id=existing_run.run_id,
                    attempt=inbox.attempt,
                    already_processed=True,
                )
            if inbox is None:
                inbox = InboxEvent.receive(
                    command.event_id,
                    "AGENT_MENTIONED",
                    "MESSAGE",
                    str(command.trigger_message_id),
                )
                await unit_of_work.inbox_events.insert(inbox)
            else:
                inbox.retry()
                await unit_of_work.inbox_events.update(inbox)

            await unit_of_work.business_locks.lock(
                "agent-trigger-message",
                str(command.trigger_message_id),
            )
            run = await unit_of_work.agent_runs.get_by_trigger_message_id(
                command.trigger_message_id
            )
            if run is None:
                run_id = self._id_generator.next_id()
                control_plane = await unit_of_work.agent_control_plane.get_view_by_agent(
                    command.conversation_id,
                    command.agent_id,
                )
                if control_plane is None and not self._fixed_answer_enabled:
                    raise NonRetryableAgentError(
                        AgentErrorCode.CONFIG_NOT_FOUND,
                        "被提及的Agent不属于当前会话",
                    )
                if control_plane is not None:
                    if control_plane.binding.status is not AgentBindingStatus.ACTIVE:
                        raise NonRetryableAgentError(
                            AgentErrorCode.CONFIG_DISABLED,
                            "当前会话Agent不可用",
                        )
                    if (
                        control_plane.credential is None
                        or control_plane.credential_version is None
                        or control_plane.credential.status is not CredentialStatus.ACTIVE
                        or control_plane.credential_version.connection_test_status
                        is not ConnectionTestStatus.SUCCEEDED
                    ):
                        raise NonRetryableAgentError(
                            AgentErrorCode.CONFIG_NOT_FOUND,
                            "当前会话Agent凭证不可用",
                        )
                    binding_id = control_plane.binding.binding_id
                    binding_version = control_plane.binding.binding_version
                    agent_id = control_plane.definition.agent_id
                    agent_version = control_plane.definition_version.version
                    credential_id = control_plane.credential.credential_id
                    credential_version = control_plane.credential_version.version
                    credential_owner_user_id = control_plane.credential.owner_user_id
                    model_id = control_plane.definition_version.model_id
                else:
                    binding_id = command.agent_id
                    binding_version = 1
                    agent_id = command.agent_id
                    agent_version = 1
                    credential_id = command.agent_id
                    credential_version = 1
                    credential_owner_user_id = command.requester_user_id
                    model_id = DEFAULT_FIXED_MODEL_ID
                run = AgentRun.create(
                    run_id=run_id,
                    conversation_id=command.conversation_id,
                    trigger_message_id=command.trigger_message_id,
                    requester_user_id=command.requester_user_id,
                    binding_id=binding_id,
                    binding_version=binding_version,
                    agent_id=agent_id,
                    agent_version=agent_version,
                    credential_id=credential_id,
                    credential_version=credential_version,
                    credential_owner_user_id=credential_owner_user_id,
                    model_id=model_id,
                )
                await unit_of_work.agent_runs.insert(run)
            await unit_of_work.commit()
            return PreparedAgentRun(
                run_id=run.run_id,
                attempt=inbox.attempt,
                already_processed=False,
            )
