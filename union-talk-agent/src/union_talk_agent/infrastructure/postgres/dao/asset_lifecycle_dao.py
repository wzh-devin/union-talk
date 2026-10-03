"""资产生命周期投影 DAO。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/08/13 17:34
"""

from datetime import UTC, datetime

import sqlalchemy as sa
from sqlalchemy.ext.asyncio import AsyncSession

from union_talk_agent.infrastructure.postgres.column_expression import table_column
from union_talk_agent.infrastructure.postgres.query_cardinality import select_one_or_none
from union_talk_agent.infrastructure.postgres.tables import agent_asset_lifecycle_table
from union_talk_agent.knowledge.domain.asset_lifecycle import AssetLifecycle
from union_talk_agent.knowledge.domain.enums import AssetLifecycleStatus


class AssetLifecycleDao:
    """读写 File Service 资产生命周期投影。"""

    def __init__(self, session: AsyncSession) -> None:
        """
        初始化 AssetLifecycleDao

        :param session: 异步数据库会话
        :return: 无返回值
        """

        self._session = session

    async def get_by_asset_file_id(self, asset_file_id: int) -> AssetLifecycle | None:
        """
        查询资产最新生命周期事实

        :param asset_file_id: 资产文件 ID
        :return: 资产生命周期事实，不存在时返回空
        """

        row = await select_one_or_none(
            self._session,
            sa.select(agent_asset_lifecycle_table)
            .where(table_column(agent_asset_lifecycle_table, "asset_file_id") == asset_file_id)
            .order_by(agent_asset_lifecycle_table.c["created_at"]),
            "同一资产存在重复生命周期投影",
        )
        if row is None:
            return None
        return AssetLifecycle(
            asset_file_id=int(row["asset_file_id"]),
            conversation_id=int(row["conversation_id"]),
            resource_version=int(row["resource_version"]),
            status=AssetLifecycleStatus(str(row["status"])),
            event_id=str(row["event_id"]),
            occurred_at=row["occurred_at"],
        )

    async def save(self, lifecycle: AssetLifecycle) -> None:
        """
        新增或更新资产生命周期事实

        :param lifecycle: 资产生命周期事实
        :return: 无返回值
        """

        now = datetime.now(UTC)
        existing = await self.get_by_asset_file_id(lifecycle.asset_file_id)
        value_map = {
            "conversation_id": lifecycle.conversation_id,
            "resource_version": lifecycle.resource_version,
            "status": str(lifecycle.status),
            "event_id": lifecycle.event_id,
            "occurred_at": lifecycle.occurred_at,
            "updated_at": now,
        }
        if existing is None:
            await self._session.execute(
                sa.insert(agent_asset_lifecycle_table).values(
                    asset_file_id=lifecycle.asset_file_id,
                    created_at=now,
                    **value_map,
                )
            )
            return
        await self._session.execute(
            sa.update(agent_asset_lifecycle_table)
            .where(
                table_column(agent_asset_lifecycle_table, "asset_file_id")
                == lifecycle.asset_file_id
            )
            .values(**value_map)
        )
