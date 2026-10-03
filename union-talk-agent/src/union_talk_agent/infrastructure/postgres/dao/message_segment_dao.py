"""会话消息检索片段 DAO。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/08/12 23:52
"""

from datetime import UTC, datetime

import sqlalchemy as sa
from sqlalchemy.engine import RowMapping
from sqlalchemy.ext.asyncio import AsyncSession

from union_talk_agent.infrastructure.postgres.column_expression import table_column
from union_talk_agent.infrastructure.postgres.query_cardinality import select_one_or_none
from union_talk_agent.infrastructure.postgres.tables import (
    agent_message_segment_asset_table,
    agent_message_segment_table,
)
from union_talk_agent.knowledge.domain.enums import VectorStatus
from union_talk_agent.knowledge.domain.message_segment import MessageSegment


class MessageSegmentDao:
    """维护单消息检索片段及其向量状态。"""

    def __init__(self, session: AsyncSession) -> None:
        """
        初始化消息片段 DAO

        :param session: 异步数据库会话
        :return: 无返回值
        """

        self._session = session

    async def get_by_message_id(self, message_id: int) -> MessageSegment | None:
        """
        按消息 ID 查询当前片段

        :param message_id: 消息 ID
        :return: 消息片段，不存在时返回空
        """

        row = await select_one_or_none(
            self._session,
            sa.select(agent_message_segment_table)
            .where(table_column(agent_message_segment_table, "start_message_id") == message_id)
            .where(table_column(agent_message_segment_table, "end_message_id") == message_id)
            .order_by(agent_message_segment_table.c["created_at"]),
            "同一消息存在重复检索片段",
        )
        if row is None:
            return None
        lineage_result = await self._session.execute(
            sa.select(
                agent_message_segment_asset_table.c["asset_file_id"],
                agent_message_segment_asset_table.c["resource_version"],
            ).where(table_column(agent_message_segment_asset_table, "segment_id") == int(row["id"]))
        )
        return self._to_domain(
            row,
            {
                int(lineage_row["asset_file_id"]): int(lineage_row["resource_version"])
                for lineage_row in lineage_result.mappings().all()
            },
        )

    async def save_pending(self, segment: MessageSegment) -> None:
        """
        新增或覆盖待向量化消息片段

        :param segment: 消息检索片段
        :return: 无返回值
        """

        now = datetime.now(UTC)
        existing = await self.get_by_message_id(segment.message_id)
        value_map: dict[str, object] = {
            "conversation_id": segment.conversation_id,
            "start_message_id": segment.message_id,
            "end_message_id": segment.message_id,
            "sender_id": segment.sender_id,
            "sender_display_name": segment.sender_display_name,
            "start_at": segment.occurred_at,
            "end_at": segment.occurred_at,
            "message_count": 1,
            "segment_text": segment.text_content,
            "summary_text": None,
            "summary_json": {},
            "token_count": segment.token_count,
            "milvus_pk": f"message:{segment.segment_id}",
            "embedding_model_version": segment.embedding_model_version,
            "vector_status": str(VectorStatus.PENDING),
            "status": "INDEXING",
            "source_revision": segment.source_revision,
            "updated_at": now,
        }
        if existing is None:
            await self._session.execute(
                sa.insert(agent_message_segment_table).values(
                    id=segment.segment_id,
                    created_at=now,
                    **value_map,
                )
            )
        else:
            await self._session.execute(
                sa.update(agent_message_segment_table)
                .where(table_column(agent_message_segment_table, "id") == existing.segment_id)
                .values(**value_map)
            )
        await self._session.execute(
            sa.delete(agent_message_segment_asset_table).where(
                table_column(agent_message_segment_asset_table, "segment_id") == segment.segment_id
            )
        )
        if segment.source_asset_version_map:
            await self._session.execute(
                sa.insert(agent_message_segment_asset_table),
                [
                    {
                        "segment_id": segment.segment_id,
                        "conversation_id": segment.conversation_id,
                        "message_id": segment.message_id,
                        "asset_file_id": asset_file_id,
                        "resource_version": resource_version,
                        "created_at": now,
                    }
                    for asset_file_id, resource_version in sorted(
                        segment.source_asset_version_map.items()
                    )
                ],
            )

    async def mark_ready(self, segment: MessageSegment) -> None:
        """
        将消息片段标记为可检索

        :param segment: 已成功写入向量的消息片段
        :return: 无返回值
        """

        await self._session.execute(
            sa.update(agent_message_segment_table)
            .where(table_column(agent_message_segment_table, "id") == segment.segment_id)
            .values(
                vector_status=str(VectorStatus.READY),
                status="READY",
                updated_at=datetime.now(UTC),
            )
        )

    async def mark_failed(self, segment_id: int) -> None:
        """
        将消息片段标记为向量化失败

        :param segment_id: 消息片段 ID
        :return: 无返回值
        """

        await self._session.execute(
            sa.update(agent_message_segment_table)
            .where(table_column(agent_message_segment_table, "id") == segment_id)
            .values(
                vector_status=str(VectorStatus.FAILED),
                status="FAILED",
                updated_at=datetime.now(UTC),
            )
        )

    async def mark_deleted(self, segment_id: int) -> None:
        """
        将消息片段标记为不可检索

        :param segment_id: 消息片段 ID
        :return: 无返回值
        """

        await self._session.execute(
            sa.update(agent_message_segment_table)
            .where(table_column(agent_message_segment_table, "id") == segment_id)
            .values(
                vector_status=str(VectorStatus.DELETED),
                status="DELETED",
                updated_at=datetime.now(UTC),
            )
        )

    async def invalidate_by_message_id(self, message_id: int) -> None:
        """
        使被撤回或删除消息的检索片段失效

        :param message_id: 消息 ID
        :return: 无返回值
        """

        await self._session.execute(
            sa.update(agent_message_segment_table)
            .where(table_column(agent_message_segment_table, "start_message_id") == message_id)
            .where(table_column(agent_message_segment_table, "end_message_id") == message_id)
            .values(
                vector_status=str(VectorStatus.DELETED),
                status="DELETED",
                updated_at=datetime.now(UTC),
            )
        )

    async def invalidate_by_asset_file_id(self, asset_file_id: int) -> list[int]:
        """
        使引用指定资产的消息片段失效

        :param asset_file_id: 已删除资产文件 ID
        :return: 已失效的消息 ID 列表
        """

        result = await self._session.execute(
            sa.select(agent_message_segment_asset_table.c["message_id"])
            .where(
                table_column(agent_message_segment_asset_table, "asset_file_id") == asset_file_id
            )
            .distinct()
        )
        message_id_list = [int(row[0]) for row in result.all()]
        if not message_id_list:
            return []
        await self._session.execute(
            sa.update(agent_message_segment_table)
            .where(
                table_column(agent_message_segment_table, "start_message_id").in_(message_id_list)
            )
            .where(table_column(agent_message_segment_table, "end_message_id").in_(message_id_list))
            .values(
                vector_status=str(VectorStatus.DELETED),
                status="DELETED",
                updated_at=datetime.now(UTC),
            )
        )
        return message_id_list

    async def get_asset_version_map_by_message_id_list(
        self,
        message_id_list: list[int],
    ) -> dict[int, int]:
        """
        查询来源消息已经继承的资产血缘

        :param message_id_list: 来源消息 ID 列表
        :return: 资产文件 ID 与资源版本映射
        """

        if not message_id_list:
            return {}
        result = await self._session.execute(
            sa.select(
                agent_message_segment_asset_table.c["asset_file_id"],
                agent_message_segment_asset_table.c["resource_version"],
            ).where(
                table_column(agent_message_segment_asset_table, "message_id").in_(message_id_list)
            )
        )
        return {
            int(row["asset_file_id"]): int(row["resource_version"])
            for row in result.mappings().all()
        }

    @staticmethod
    def _to_domain(
        row: RowMapping,
        source_asset_version_map: dict[int, int],
    ) -> MessageSegment:
        """
        将数据库行转换为领域对象

        :param row: 数据库查询行
        :param source_asset_version_map: 资产文件 ID 与资源版本映射
        :return: 消息检索片段
        """

        return MessageSegment(
            segment_id=int(row["id"]),
            conversation_id=int(row["conversation_id"]),
            message_id=int(row["start_message_id"]),
            sender_id=int(row["sender_id"] or 0),
            sender_display_name=str(row["sender_display_name"] or ""),
            text_content=str(row["segment_text"] or ""),
            occurred_at=row["start_at"],
            source_revision=int(row["source_revision"] or 1),
            embedding_model_version=str(row["embedding_model_version"] or ""),
            source_asset_version_map=source_asset_version_map,
            vector_status=VectorStatus(str(row["vector_status"])),
        )
