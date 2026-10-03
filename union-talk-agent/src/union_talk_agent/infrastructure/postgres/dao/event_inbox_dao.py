"""Inbox 幂等记录 DAO。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/29 18:52
"""

from datetime import UTC, datetime

import sqlalchemy as sa
from sqlalchemy.ext.asyncio import AsyncSession

from union_talk_agent.event_inbox.domain.enums import InboxStatus
from union_talk_agent.event_inbox.domain.inbox_event import InboxEvent
from union_talk_agent.infrastructure.postgres.column_expression import table_column
from union_talk_agent.infrastructure.postgres.query_cardinality import select_one_or_none
from union_talk_agent.infrastructure.postgres.tables import agent_event_inbox_table


class EventInboxDao:
    """收敛 Inbox 查询和状态写入。"""

    def __init__(self, session: AsyncSession) -> None:
        """
        初始化 EventInboxDao

        :param session: 异步数据库会话
        :return: 无返回值
        """

        self._session = session

    async def get_by_event_id(self, event_id: str) -> InboxEvent | None:
        """
        按事件 ID 查询 Inbox

        :param event_id: RabbitMQ 事件 ID
        :return: Inbox 记录，不存在时返回空
        """

        row = await select_one_or_none(
            self._session,
            sa.select(agent_event_inbox_table)
            .where(table_column(agent_event_inbox_table, "event_id") == event_id)
            .order_by(agent_event_inbox_table.c["received_at"]),
            "同一Inbox事件ID存在重复记录",
        )
        if row is None:
            return None
        return InboxEvent(
            event_id=str(row["event_id"]),
            event_type=str(row["event_type"]),
            aggregate_type=str(row["aggregate_type"]),
            aggregate_id=str(row["aggregate_id"]),
            status=InboxStatus(str(row["status"])),
            attempt=int(row["attempt"]),
            last_error=str(row["last_error"]) if row["last_error"] is not None else None,
            received_at=row["received_at"],
            processed_at=row["processed_at"],
        )

    async def insert(self, inbox_event: InboxEvent) -> None:
        """
        新增 Inbox

        :param inbox_event: 首次接收的 Inbox 记录
        :return: 无返回值
        """

        now = datetime.now(UTC)
        await self._session.execute(
            sa.insert(agent_event_inbox_table).values(
                event_id=inbox_event.event_id,
                event_type=inbox_event.event_type,
                aggregate_type=inbox_event.aggregate_type,
                aggregate_id=inbox_event.aggregate_id,
                status=str(inbox_event.status),
                attempt=inbox_event.attempt,
                last_error=inbox_event.last_error,
                received_at=inbox_event.received_at or now,
                processed_at=inbox_event.processed_at,
                updated_at=now,
            )
        )

    async def update(self, inbox_event: InboxEvent) -> None:
        """
        更新 Inbox

        :param inbox_event: 已变化的 Inbox 记录
        :return: 无返回值
        """

        await self._session.execute(
            sa.update(agent_event_inbox_table)
            .where(table_column(agent_event_inbox_table, "event_id") == inbox_event.event_id)
            .values(
                status=str(inbox_event.status),
                attempt=inbox_event.attempt,
                last_error=inbox_event.last_error,
                processed_at=inbox_event.processed_at,
                updated_at=datetime.now(UTC),
            )
        )
