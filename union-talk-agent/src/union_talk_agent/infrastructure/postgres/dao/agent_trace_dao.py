"""Agent 结构化执行轨迹 DAO。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/29 18:52
"""

from datetime import UTC, datetime

import sqlalchemy as sa
from sqlalchemy.ext.asyncio import AsyncSession

from union_talk_agent.agent_run.domain.agent_runtime import (
    AgentOpenRuntime,
    AgentRunContentBlock,
    AgentRunMessage,
    AgentRunTurn,
    AgentToolExecution,
)
from union_talk_agent.agent_run.domain.agent_step import AgentStep
from union_talk_agent.agent_run.domain.enums import (
    AgentMessageStatus,
    AgentTurnStatus,
)
from union_talk_agent.infrastructure.postgres.column_expression import table_column
from union_talk_agent.infrastructure.postgres.tables import (
    agent_run_content_block_table,
    agent_run_message_table,
    agent_run_step_table,
    agent_run_turn_table,
    agent_tool_call_table,
)


class AgentTraceDao:
    """运行步骤新增、更新和查询 DAO。"""

    def __init__(self, session: AsyncSession) -> None:
        """
        初始化 AgentTraceDao

        :param session: 异步数据库会话
        :return: 无返回值
        """

        self._session = session

    async def insert_step(self, agent_step: AgentStep) -> None:
        """
        新增运行步骤

        :param agent_step: 待记录步骤
        :return: 无返回值
        """

        now = datetime.now(UTC)
        await self._session.execute(
            sa.insert(agent_run_step_table).values(
                id=agent_step.step_id,
                run_id=agent_step.run_id,
                sequence_no=agent_step.sequence_no,
                step_type=str(agent_step.step_type),
                step_code=agent_step.step_code,
                display_name=agent_step.display_name,
                status=str(agent_step.status),
                visibility=str(agent_step.visibility),
                started_at=agent_step.started_at,
                finished_at=agent_step.finished_at,
                created_at=now,
                updated_at=now,
            )
        )

    async def update_step(
        self,
        agent_step: AgentStep,
        input_summary: dict[str, object] | None,
        output_summary: dict[str, object] | None,
    ) -> None:
        """
        更新运行步骤

        :param agent_step: 已变化步骤
        :param input_summary: 脱敏输入摘要
        :param output_summary: 脱敏输出摘要
        :return: 无返回值
        """

        duration_ms = None
        if agent_step.started_at is not None and agent_step.finished_at is not None:
            duration_ms = int(
                (agent_step.finished_at - agent_step.started_at).total_seconds() * 1000
            )
        await self._session.execute(
            sa.update(agent_run_step_table)
            .where(table_column(agent_run_step_table, "id") == agent_step.step_id)
            .values(
                status=str(agent_step.status),
                input_summary_json=input_summary,
                output_summary_json=output_summary,
                error_code=agent_step.error_code,
                error_message=agent_step.error_message,
                started_at=agent_step.started_at,
                finished_at=agent_step.finished_at,
                duration_ms=duration_ms,
                updated_at=datetime.now(UTC),
            )
        )

    async def list_by_run_id(self, run_id: int) -> list[dict[str, object]]:
        """
        查询 Run 的成员可见轨迹

        :param run_id: Agent Run ID
        :return: 已脱敏步骤字典列表
        """

        result = await self._session.execute(
            sa.select(agent_run_step_table)
            .where(table_column(agent_run_step_table, "run_id") == run_id)
            .where(table_column(agent_run_step_table, "visibility").in_(["MEMBER", "ADMIN"]))
            .order_by(agent_run_step_table.c["sequence_no"])
        )
        return [
            {
                "stepId": str(row["id"]),
                "sequence": int(row["sequence_no"]),
                "stepType": str(row["step_type"]),
                "stepCode": str(row["step_code"]),
                "displayName": str(row["display_name"]),
                "status": str(row["status"]),
                "visibility": str(row["visibility"]),
                "inputSummary": row["input_summary_json"] or {},
                "outputSummary": row["output_summary_json"] or {},
                "errorCode": row["error_code"],
                "errorMessage": row["error_message"],
                "startedAt": row["started_at"].isoformat() if row["started_at"] else None,
                "finishedAt": row["finished_at"].isoformat() if row["finished_at"] else None,
                "durationMs": row["duration_ms"],
            }
            for row in result.mappings().all()
        ]

    async def insert_turn(self, turn: AgentRunTurn, message: AgentRunMessage) -> None:
        """
        新增 Turn 和对应 partial assistant message

        :param turn: Agent Turn
        :param message: partial assistant message
        :return: 无返回值
        """

        now = datetime.now(UTC)
        await self._session.execute(
            sa.insert(agent_run_turn_table).values(
                id=turn.turn_id,
                run_id=turn.run_id,
                turn_no=turn.turn_no,
                status=str(turn.status),
                started_at=turn.started_at,
                created_at=now,
                updated_at=now,
            )
        )
        await self._session.execute(
            sa.insert(agent_run_message_table).values(
                id=message.message_id,
                run_id=message.run_id,
                turn_id=message.turn_id,
                turn_no=message.turn_no,
                message_key=message.message_key,
                role=message.role,
                status=str(message.status),
                model_id=message.model_id,
                started_at=message.started_at,
                created_at=now,
                updated_at=now,
            )
        )

    async def update_turn(self, turn: AgentRunTurn, message: AgentRunMessage) -> None:
        """
        更新 Turn 和运行时消息终态

        :param turn: 已完成或失败的 Agent Turn
        :param message: 已完成或失败的运行时消息
        :return: 无返回值
        """

        now = datetime.now(UTC)
        duration_ms = (
            int((turn.finished_at - turn.started_at).total_seconds() * 1000)
            if turn.finished_at is not None
            else None
        )
        await self._session.execute(
            sa.update(agent_run_turn_table)
            .where(table_column(agent_run_turn_table, "id") == turn.turn_id)
            .values(
                status=str(turn.status),
                stop_reason=turn.stop_reason,
                error_code=turn.error_code,
                error_message=turn.error_message,
                finished_at=turn.finished_at,
                duration_ms=duration_ms,
                updated_at=now,
            )
        )
        await self._session.execute(
            sa.update(agent_run_message_table)
            .where(table_column(agent_run_message_table, "id") == message.message_id)
            .values(
                status=str(message.status),
                stop_reason=message.stop_reason,
                input_tokens=message.input_tokens,
                output_tokens=message.output_tokens,
                error_code=message.error_code,
                error_message=message.error_message,
                finished_at=message.finished_at,
                updated_at=now,
            )
        )

    async def insert_content_block(self, block: AgentRunContentBlock) -> None:
        """
        写入已经结束的完整内容块

        :param block: 已结束内容块
        :return: 无返回值
        """

        now = datetime.now(UTC)
        await self._session.execute(
            sa.insert(agent_run_content_block_table).values(
                id=block.block_id,
                run_id=block.run_id,
                turn_id=block.turn_id,
                message_id=block.message_id,
                content_index=block.content_index,
                block_type=str(block.block_type),
                status=str(block.status),
                visibility=str(block.visibility),
                content_text=block.content,
                tool_call_id=block.tool_call_id,
                tool_name=block.tool_name,
                started_at=block.started_at,
                finished_at=block.finished_at,
                created_at=now,
                updated_at=now,
            )
        )

    async def insert_tool_execution(self, execution: AgentToolExecution) -> None:
        """
        新增运行中的工具执行轨迹

        :param execution: 运行中的工具执行
        :return: 无返回值
        """

        now = datetime.now(UTC)
        await self._session.execute(
            sa.insert(agent_tool_call_table).values(
                id=execution.tool_execution_id,
                run_id=execution.run_id,
                step_id=execution.step_id,
                turn_id=execution.turn_id,
                message_id=execution.message_id,
                tool_call_key=execution.tool_call_id,
                tool_name=execution.tool_name,
                display_name=execution.display_name,
                status=str(execution.status),
                visibility=str(execution.visibility),
                arguments_redacted_json=execution.arguments_summary,
                attempt=1,
                started_at=execution.started_at,
                created_at=now,
                updated_at=now,
            )
        )

    async def update_tool_execution(self, execution: AgentToolExecution) -> None:
        """
        更新工具执行终态和结果摘要

        :param execution: 已完成或失败的工具执行
        :return: 无返回值
        """

        duration_ms = (
            int((execution.finished_at - execution.started_at).total_seconds() * 1000)
            if execution.finished_at is not None
            else None
        )
        await self._session.execute(
            sa.update(agent_tool_call_table)
            .where(table_column(agent_tool_call_table, "id") == execution.tool_execution_id)
            .values(
                status=str(execution.status),
                result_summary_json=execution.result_summary,
                error_code=execution.error_code,
                error_message=execution.error_message,
                finished_at=execution.finished_at,
                duration_ms=duration_ms,
                updated_at=datetime.now(UTC),
            )
        )

    async def get_latest_turn_no(self, run_id: int) -> int:
        """
        查询 Run 已创建的最大 Turn 序号

        :param run_id: Agent Run ID
        :return: 最大 Turn 序号，不存在时返回零
        """

        result = await self._session.execute(
            sa.select(sa.func.max(agent_run_turn_table.c["turn_no"])).where(
                table_column(agent_run_turn_table, "run_id") == run_id
            )
        )
        return int(result.scalar_one_or_none() or 0)

    async def list_open_runtime(self, run_id: int) -> list[AgentOpenRuntime]:
        """
        查询租约过期后仍未关闭的 Turn 和 Message

        :param run_id: Agent Run ID
        :return: 未关闭运行时列表
        """

        result = await self._session.execute(
            sa.select(
                agent_run_turn_table.c["id"].label("turn_id"),
                agent_run_turn_table.c["turn_no"].label("turn_no"),
                agent_run_turn_table.c["status"].label("turn_status"),
                agent_run_turn_table.c["stop_reason"].label("turn_stop_reason"),
                agent_run_turn_table.c["started_at"].label("turn_started_at"),
                agent_run_message_table.c["id"].label("message_id"),
                agent_run_message_table.c["turn_id"].label("message_turn_id"),
                agent_run_message_table.c["turn_no"].label("message_turn_no"),
                agent_run_message_table.c["message_key"].label("message_key"),
                agent_run_message_table.c["role"].label("message_role"),
                agent_run_message_table.c["model_id"].label("model_id"),
                agent_run_message_table.c["status"].label("message_status"),
                agent_run_message_table.c["stop_reason"].label("message_stop_reason"),
                agent_run_message_table.c["input_tokens"].label("input_tokens"),
                agent_run_message_table.c["output_tokens"].label("output_tokens"),
                agent_run_message_table.c["started_at"].label("message_started_at"),
            )
            .join(
                agent_run_message_table,
                table_column(agent_run_message_table, "turn_id")
                == table_column(agent_run_turn_table, "id"),
            )
            .where(table_column(agent_run_turn_table, "run_id") == run_id)
            .where(
                sa.or_(
                    table_column(agent_run_turn_table, "status") == str(AgentTurnStatus.RUNNING),
                    table_column(agent_run_message_table, "status")
                    == str(AgentMessageStatus.STREAMING),
                )
            )
            .order_by(agent_run_turn_table.c["turn_no"])
        )
        runtime_list: list[AgentOpenRuntime] = []
        for row in result.mappings().all():
            message_id = int(row["message_id"])
            block_result = await self._session.execute(
                sa.select(agent_run_content_block_table.c["content_index"]).where(
                    table_column(agent_run_content_block_table, "message_id") == message_id
                )
            )
            runtime_list.append(
                AgentOpenRuntime(
                    turn=AgentRunTurn(
                        turn_id=int(row["turn_id"]),
                        run_id=run_id,
                        turn_no=int(row["turn_no"]),
                        status=AgentTurnStatus(str(row["turn_status"])),
                        stop_reason=row["turn_stop_reason"],
                        started_at=row["turn_started_at"],
                    ),
                    message=AgentRunMessage(
                        message_id=message_id,
                        run_id=run_id,
                        turn_id=int(row["message_turn_id"]),
                        turn_no=int(row["message_turn_no"]),
                        message_key=str(row["message_key"]),
                        role=str(row["message_role"]),
                        model_id=str(row["model_id"]),
                        status=AgentMessageStatus(str(row["message_status"])),
                        stop_reason=row["message_stop_reason"],
                        input_tokens=int(row["input_tokens"] or 0),
                        output_tokens=int(row["output_tokens"] or 0),
                        started_at=row["message_started_at"],
                    ),
                    persisted_content_index_set={
                        int(content_index) for content_index in block_result.scalars().all()
                    },
                )
            )
        return runtime_list

    async def interrupt_runtime(
        self,
        runtime: AgentOpenRuntime,
        partial_block_list: list[AgentRunContentBlock],
    ) -> None:
        """
        关闭旧运行时并保存 Redis 中最后的 partial block

        :param runtime: 待关闭运行时
        :param partial_block_list: 尚未落库的 partial block 列表
        :return: 无返回值
        """

        runtime.turn.interrupt()
        runtime.message.interrupt()
        await self.update_turn(runtime.turn, runtime.message)
        for block in partial_block_list:
            await self.insert_content_block(block)
        now = datetime.now(UTC)
        await self._session.execute(
            sa.update(agent_tool_call_table)
            .where(table_column(agent_tool_call_table, "turn_id") == runtime.turn.turn_id)
            .where(table_column(agent_tool_call_table, "status") == "RUNNING")
            .values(
                status="INTERRUPTED",
                error_code="WORKER_LEASE_EXPIRED",
                error_message="执行 Worker 租约已过期",
                finished_at=now,
                updated_at=now,
            )
        )

    async def get_runtime_trace(self, run_id: int) -> dict[str, object]:
        """
        查询按 Turn 聚合的成员可见运行时轨迹

        :param run_id: Agent Run ID
        :return: V2 运行时轨迹聚合
        """

        turn_result = await self._session.execute(
            sa.select(agent_run_turn_table)
            .where(table_column(agent_run_turn_table, "run_id") == run_id)
            .order_by(agent_run_turn_table.c["turn_no"])
        )
        message_result = await self._session.execute(
            sa.select(agent_run_message_table)
            .where(table_column(agent_run_message_table, "run_id") == run_id)
            .order_by(agent_run_message_table.c["turn_no"])
        )
        block_result = await self._session.execute(
            sa.select(agent_run_content_block_table)
            .where(table_column(agent_run_content_block_table, "run_id") == run_id)
            .where(
                table_column(agent_run_content_block_table, "visibility").in_(["MEMBER", "ADMIN"])
            )
            .order_by(agent_run_content_block_table.c["content_index"])
        )
        tool_result = await self._session.execute(
            sa.select(agent_tool_call_table)
            .where(table_column(agent_tool_call_table, "run_id") == run_id)
            .where(table_column(agent_tool_call_table, "visibility").in_(["MEMBER", "ADMIN"]))
            .order_by(agent_tool_call_table.c["created_at"])
        )
        message_map = {int(row["id"]): dict(row) for row in message_result.mappings().all()}
        block_map: dict[int, list[dict[str, object]]] = {}
        for row in block_result.mappings().all():
            block_map.setdefault(int(row["message_id"]), []).append(
                {
                    "contentIndex": int(row["content_index"]),
                    "blockType": str(row["block_type"]),
                    "status": str(row["status"]),
                    "content": str(row["content_text"] or ""),
                    "toolCallId": row["tool_call_id"],
                    "toolName": row["tool_name"],
                }
            )
        tool_map: dict[int, list[dict[str, object]]] = {}
        for row in tool_result.mappings().all():
            tool_map.setdefault(int(row["turn_id"]), []).append(
                {
                    "toolCallId": str(row["tool_call_key"]),
                    "toolName": str(row["tool_name"]),
                    "displayName": str(row["display_name"]),
                    "status": str(row["status"]),
                    "argumentsSummary": row["arguments_redacted_json"] or {},
                    "resultSummary": row["result_summary_json"] or {},
                    "durationMs": row["duration_ms"],
                    "errorCode": row["error_code"],
                    "errorMessage": row["error_message"],
                }
            )
        turn_list: list[dict[str, object]] = []
        for turn_row in turn_result.mappings().all():
            turn_id = int(turn_row["id"])
            turn_message_list = []
            for message_id, message_row in message_map.items():
                if int(message_row["turn_id"]) != turn_id:
                    continue
                turn_message_list.append(
                    {
                        "messageKey": str(message_row["message_key"]),
                        "role": str(message_row["role"]),
                        "status": str(message_row["status"]),
                        "modelId": str(message_row["model_id"]),
                        "stopReason": message_row["stop_reason"],
                        "inputTokens": int(message_row["input_tokens"] or 0),
                        "outputTokens": int(message_row["output_tokens"] or 0),
                        "contentBlockList": block_map.get(message_id, []),
                    }
                )
            turn_list.append(
                {
                    "turnNo": int(turn_row["turn_no"]),
                    "status": str(turn_row["status"]),
                    "stopReason": turn_row["stop_reason"],
                    "durationMs": turn_row["duration_ms"],
                    "messageList": turn_message_list,
                    "toolCallList": tool_map.get(turn_id, []),
                }
            )
        return {"turnList": turn_list}
