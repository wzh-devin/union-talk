"""Inbox 终态写入服务。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/30 10:51
"""

from union_talk_agent.agent_run.application.ports.agent_unit_of_work import (
    AgentUnitOfWorkFactory,
)
from union_talk_agent.event_inbox.domain.inbox_event import InboxEvent


class InboxLifecycleService:
    """在事务锁保护下提交 Inbox 成功或失败状态。"""

    def __init__(self, unit_of_work_factory: AgentUnitOfWorkFactory) -> None:
        """
        初始化 InboxLifecycleService

        :param unit_of_work_factory: Agent 工作单元工厂
        :return: 无返回值
        """

        self._unit_of_work_factory = unit_of_work_factory

    async def mark_failed(self, event_id: str, error_message: str) -> None:
        """
        提交 Inbox 失败状态

        :param event_id: 事件 ID
        :param error_message: 脱敏错误摘要
        :return: 无返回值
        """

        async with self._unit_of_work_factory() as unit_of_work:
            await unit_of_work.business_locks.lock("agent-event", event_id)
            inbox = await unit_of_work.inbox_events.get_by_event_id(event_id)
            if inbox is not None:
                inbox.fail(error_message)
                await unit_of_work.inbox_events.update(inbox)
            await unit_of_work.commit()

    async def mark_succeeded(self, event_id: str) -> None:
        """
        提交 Inbox 成功状态

        :param event_id: 事件 ID
        :return: 无返回值
        """

        async with self._unit_of_work_factory() as unit_of_work:
            await unit_of_work.business_locks.lock("agent-event", event_id)
            inbox = await unit_of_work.inbox_events.get_by_event_id(event_id)
            if inbox is not None:
                inbox.succeed()
                await unit_of_work.inbox_events.update(inbox)
            await unit_of_work.commit()

    async def record_rejected(
        self,
        event_id: str,
        event_type: str,
        aggregate_type: str,
        aggregate_id: str,
        error_message: str,
    ) -> None:
        """
        持久化无需重试事件的终态失败审计

        :param event_id: 事件 ID
        :param event_type: 事件类型
        :param aggregate_type: 事件聚合类型
        :param aggregate_id: 事件聚合标识
        :param error_message: 脱敏错误摘要
        :return: 无返回值
        """

        async with self._unit_of_work_factory() as unit_of_work:
            await unit_of_work.business_locks.lock("agent-event", event_id)
            inbox = await unit_of_work.inbox_events.get_by_event_id(event_id)
            if inbox is None:
                inbox = InboxEvent.receive(
                    event_id,
                    event_type,
                    aggregate_type,
                    aggregate_id,
                )
                inbox.fail(error_message)
                await unit_of_work.inbox_events.insert(inbox)
            else:
                inbox.fail(error_message)
                await unit_of_work.inbox_events.update(inbox)
            await unit_of_work.commit()
