"""Agent Run DAO。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/29 18:52
"""

from datetime import UTC, datetime, timedelta

import sqlalchemy as sa
from sqlalchemy.engine import RowMapping
from sqlalchemy.ext.asyncio import AsyncSession

from union_talk_agent.agent_run.domain.agent_run import AgentRun
from union_talk_agent.agent_run.domain.enums import AgentRunStage, AgentRunStatus
from union_talk_agent.agent_run.domain.exceptions import AgentDomainError, AgentErrorCode
from union_talk_agent.infrastructure.postgres.column_expression import table_column
from union_talk_agent.infrastructure.postgres.query_cardinality import select_one_or_none
from union_talk_agent.infrastructure.postgres.tables import agent_run_table


class AgentRunDao:
    """Agent Run 查询、状态迁移和租约认领 DAO。"""

    def __init__(self, session: AsyncSession) -> None:
        """
        初始化 AgentRunDao

        :param session: 异步数据库会话
        :return: 无返回值
        """

        self._session = session

    async def get_by_id(self, run_id: int) -> AgentRun | None:
        """
        按 Run ID 查询

        :param run_id: Agent Run ID
        :return: Agent Run，不存在时返回空
        """

        row = await select_one_or_none(
            self._session,
            sa.select(agent_run_table)
            .where(table_column(agent_run_table, "id") == run_id)
            .order_by(agent_run_table.c["created_at"]),
            "同一Agent Run ID存在重复记录",
        )
        return self._to_domain(row) if row is not None else None

    async def get_by_trigger_message_id(self, trigger_message_id: int) -> AgentRun | None:
        """
        按触发消息查询 Run

        :param trigger_message_id: 触发消息 ID
        :return: 已存在 Run，不存在时返回空
        """

        row = await select_one_or_none(
            self._session,
            sa.select(agent_run_table)
            .where(table_column(agent_run_table, "trigger_message_id") == trigger_message_id)
            .order_by(agent_run_table.c["created_at"]),
            "同一触发消息存在重复Agent Run",
        )
        return self._to_domain(row) if row is not None else None

    async def insert(self, agent_run: AgentRun) -> None:
        """
        新增 Run

        :param agent_run: 待执行 Run
        :return: 无返回值
        """

        now = datetime.now(UTC)
        await self._session.execute(
            sa.insert(agent_run_table).values(
                id=agent_run.run_id,
                conversation_id=agent_run.conversation_id,
                trigger_message_id=agent_run.trigger_message_id,
                requester_user_id=agent_run.requester_user_id,
                binding_id=agent_run.binding_id,
                binding_version=agent_run.binding_version,
                agent_id=agent_run.agent_id,
                agent_version=agent_run.agent_version,
                credential_id=agent_run.credential_id,
                credential_version=agent_run.credential_version,
                credential_owner_user_id=agent_run.credential_owner_user_id,
                status=str(agent_run.status),
                stage=str(agent_run.stage),
                answer_message_id=agent_run.answer_message_id,
                model_id=agent_run.model_id,
                last_event_sequence=agent_run.last_event_sequence,
                trace_schema_version=2,
                error_code=agent_run.error_code,
                error_message=agent_run.error_message,
                queued_at=agent_run.queued_at,
                started_at=agent_run.started_at,
                completed_at=agent_run.completed_at,
                cancel_requested_at=agent_run.cancel_requested_at,
                cancel_requested_by=agent_run.cancel_requested_by,
                created_at=now,
                updated_at=now,
            )
        )

    async def update(self, agent_run: AgentRun) -> None:
        """
        更新 Run 状态

        :param agent_run: 已变化 Run
        :return: 无返回值
        """

        value_map: dict[str, object] = {
            "status": str(agent_run.status),
            "stage": str(agent_run.stage),
            "answer_message_id": agent_run.answer_message_id,
            "model_id": agent_run.model_id,
            "last_event_sequence": agent_run.last_event_sequence,
            "error_code": agent_run.error_code,
            "error_message": agent_run.error_message,
            "queued_at": agent_run.queued_at,
            "started_at": agent_run.started_at,
            "completed_at": agent_run.completed_at,
            "cancel_requested_at": agent_run.cancel_requested_at,
            "cancel_requested_by": agent_run.cancel_requested_by,
            "updated_at": datetime.now(UTC),
        }
        if agent_run.status is not AgentRunStatus.RUNNING:
            value_map["worker_id"] = None
            value_map["lease_expires_at"] = None
        await self._session.execute(
            sa.update(agent_run_table)
            .where(table_column(agent_run_table, "id") == agent_run.run_id)
            .values(**value_map)
        )

    async def next_event_sequence(self, run_id: int) -> int:
        """
        原子递增实时事件序号

        :param run_id: Agent Run ID
        :return: 新事件序号
        """

        result = await self._session.execute(
            sa.update(agent_run_table)
            .where(table_column(agent_run_table, "id") == run_id)
            .values(
                last_event_sequence=sa.func.coalesce(
                    agent_run_table.c["last_event_sequence"],
                    0,
                )
                + 1,
                updated_at=datetime.now(UTC),
            )
            .returning(agent_run_table.c["last_event_sequence"])
        )
        sequence = result.scalar_one_or_none()
        if sequence is None:
            raise AgentDomainError(AgentErrorCode.RUN_NOT_FOUND, "Agent Run 不存在")
        return int(sequence)

    async def claim_execution(
        self,
        run_id: int,
        worker_id: str,
        lease_seconds: int,
    ) -> AgentRun | None:
        """
        原子认领待执行或租约过期的 Run

        :param run_id: Agent Run ID
        :param worker_id: 当前 Worker 唯一标识
        :param lease_seconds: 租约时长秒数
        :return: 认领成功后的 Run；有效租约被占用时返回空
        """

        now = datetime.now(UTC)
        result = await self._session.execute(
            sa.update(agent_run_table)
            .where(table_column(agent_run_table, "id") == run_id)
            .where(
                sa.or_(
                    table_column(agent_run_table, "status") == str(AgentRunStatus.QUEUED),
                    sa.and_(
                        table_column(agent_run_table, "status") == str(AgentRunStatus.RUNNING),
                        sa.or_(
                            table_column(agent_run_table, "lease_expires_at").is_(None),
                            table_column(agent_run_table, "lease_expires_at") <= now,
                        ),
                    ),
                )
            )
            .values(
                status=str(AgentRunStatus.RUNNING),
                stage=str(AgentRunStage.PREPARING),
                worker_id=worker_id,
                lease_expires_at=now + timedelta(seconds=lease_seconds),
                heartbeat_at=now,
                started_at=sa.func.coalesce(agent_run_table.c["started_at"], now),
                updated_at=now,
            )
            .returning(*agent_run_table.c)
        )
        row = result.mappings().first()
        return self._to_domain(row) if row is not None else None

    async def renew_execution_lease(
        self,
        run_id: int,
        worker_id: str,
        lease_seconds: int,
    ) -> bool:
        """
        续租当前 Worker 已持有的 Run

        :param run_id: Agent Run ID
        :param worker_id: 当前 Worker 唯一标识
        :param lease_seconds: 新租约时长秒数
        :return: 是否续租成功
        """

        now = datetime.now(UTC)
        result = await self._session.execute(
            sa.update(agent_run_table)
            .where(table_column(agent_run_table, "id") == run_id)
            .where(table_column(agent_run_table, "worker_id") == worker_id)
            .where(table_column(agent_run_table, "status") == str(AgentRunStatus.RUNNING))
            .values(
                lease_expires_at=now + timedelta(seconds=lease_seconds),
                heartbeat_at=now,
                updated_at=now,
            )
            .returning(agent_run_table.c["id"])
        )
        return result.scalar_one_or_none() is not None

    async def list_active_by_conversation_id(self, conversation_id: int) -> list[AgentRun]:
        """
        查询会话活动 Run

        :param conversation_id: 会话 ID
        :return: 排队中和运行中的 Run 列表
        """

        result = await self._session.execute(
            sa.select(agent_run_table)
            .where(table_column(agent_run_table, "conversation_id") == conversation_id)
            .where(
                table_column(agent_run_table, "status").in_(
                    [str(AgentRunStatus.QUEUED), str(AgentRunStatus.RUNNING)]
                )
            )
            .order_by(agent_run_table.c["created_at"])
        )
        return [self._to_domain(row) for row in result.mappings().all()]

    async def list_by_conversation_id(
        self,
        conversation_id: int,
        page_size: int,
        cursor_value: datetime | None,
        cursor_id: int | None,
    ) -> list[AgentRun]:
        """
        按双游标查询会话 Agent Run

        :param conversation_id: 会话 ID
        :param page_size: 当前页展示数量
        :param cursor_value: 上一页末项排队时间
        :param cursor_id: 上一页末项 Run ID
        :return: 多查一条且按排队时间和 Run ID 倒序排列的 Agent Run 列表
        """

        statement = sa.select(agent_run_table).where(
            table_column(agent_run_table, "conversation_id") == conversation_id
        )
        if cursor_value is not None and cursor_id is not None:
            statement = statement.where(
                sa.or_(
                    table_column(agent_run_table, "queued_at") < cursor_value,
                    sa.and_(
                        table_column(agent_run_table, "queued_at") == cursor_value,
                        table_column(agent_run_table, "id") < cursor_id,
                    ),
                )
            )
        result = await self._session.execute(
            statement.order_by(
                agent_run_table.c["queued_at"].desc().nullslast(),
                agent_run_table.c["id"].desc(),
            ).limit(page_size + 1)
        )
        return [self._to_domain(row) for row in result.mappings().all()]

    async def list_reconcilable(self, batch_size: int) -> list[AgentRun]:
        """
        认领卡死 Run

        :param batch_size: 单批最大数量
        :return: 需要幂等修复的 Run 列表
        """

        now = datetime.now(UTC)
        result = await self._session.execute(
            sa.select(agent_run_table)
            .where(
                sa.or_(
                    table_column(agent_run_table, "status") == str(AgentRunStatus.QUEUED),
                    sa.and_(
                        table_column(agent_run_table, "status") == str(AgentRunStatus.RUNNING),
                        sa.or_(
                            table_column(agent_run_table, "lease_expires_at").is_(None),
                            table_column(agent_run_table, "lease_expires_at") <= now,
                        ),
                    ),
                )
            )
            .order_by(agent_run_table.c["created_at"])
            .limit(batch_size)
            .with_for_update(skip_locked=True)
        )
        return [self._to_domain(row) for row in result.mappings().all()]

    @staticmethod
    def _to_domain(row: RowMapping) -> AgentRun:
        """
        将数据库行转换为领域对象

        :param row: 数据库查询行
        :return: 转换后的领域对象
        """

        return AgentRun(
            run_id=int(row["id"]),
            conversation_id=int(row["conversation_id"]),
            trigger_message_id=int(row["trigger_message_id"]),
            requester_user_id=int(row["requester_user_id"]),
            binding_id=int(row["binding_id"]),
            binding_version=int(row["binding_version"]),
            agent_id=int(row["agent_id"]),
            agent_version=int(row["agent_version"]),
            credential_id=int(row["credential_id"]),
            credential_version=int(row["credential_version"]),
            credential_owner_user_id=int(row["credential_owner_user_id"]),
            model_id=str(row["model_id"]),
            status=AgentRunStatus(str(row["status"])),
            stage=AgentRunStage(str(row["stage"])),
            answer_message_id=(
                int(row["answer_message_id"]) if row["answer_message_id"] is not None else None
            ),
            last_event_sequence=int(row["last_event_sequence"] or 0),
            error_code=str(row["error_code"]) if row["error_code"] is not None else None,
            error_message=(str(row["error_message"]) if row["error_message"] is not None else None),
            queued_at=row["queued_at"],
            started_at=row["started_at"],
            completed_at=row["completed_at"],
            cancel_requested_at=row["cancel_requested_at"],
            cancel_requested_by=(
                int(row["cancel_requested_by"]) if row["cancel_requested_by"] is not None else None
            ),
        )
