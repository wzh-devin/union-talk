"""Agent Run 状态、轨迹与实时事件协同。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/29 19:00
"""

from union_talk_agent.agent_run.application.ports.agent_unit_of_work import (
    AgentUnitOfWork,
    AgentUnitOfWorkFactory,
)
from union_talk_agent.agent_run.application.ports.id_generator import IdGenerator
from union_talk_agent.agent_run.application.ports.run_cancellation_store import (
    RunCancellationStore,
)
from union_talk_agent.agent_run.application.ports.run_event_publisher import RunEventPublisher
from union_talk_agent.agent_run.application.ports.run_lifecycle import RunLifecycle
from union_talk_agent.agent_run.domain.agent_run import AgentRun
from union_talk_agent.agent_run.domain.agent_runtime import (
    AgentOpenRuntime,
    AgentRunContentBlock,
    AgentRunMessage,
    AgentRunTurn,
    AgentToolExecution,
)
from union_talk_agent.agent_run.domain.agent_step import AgentStep
from union_talk_agent.agent_run.domain.chat_model import ChatModelEvent, ChatToolCall
from union_talk_agent.agent_run.domain.enums import (
    AgentContentBlockType,
    AgentRealtimeEventType,
    AgentRunStage,
    AgentRunStatus,
    AgentStepType,
    ProviderEventType,
    TraceVisibility,
)
from union_talk_agent.agent_run.domain.exceptions import (
    AgentDomainError,
    AgentErrorCode,
    AgentRunCancelledError,
)
from union_talk_agent.agent_run.realtime.run_event import AgentRunEvent, AgentRunSnapshot


class RunLifecycleService(RunLifecycle):
    """保证 PostgreSQL 状态和 Redis 卡片事件使用同一 Run 语义。"""

    def __init__(
        self,
        unit_of_work_factory: AgentUnitOfWorkFactory,
        id_generator: IdGenerator,
        event_publisher: RunEventPublisher,
        cancellation_store: RunCancellationStore,
    ) -> None:
        """
        初始化 RunLifecycleService

        :param unit_of_work_factory: Agent 工作单元工厂
        :param id_generator: 业务 ID 生成器
        :param event_publisher: Agent 实时事件发布端口
        :param cancellation_store: Agent Run 取消标记存储
        :return: 无返回值
        """

        self._unit_of_work_factory = unit_of_work_factory
        self._id_generator = id_generator
        self._event_publisher = event_publisher
        self._cancellation_store = cancellation_store

    async def publish_started(self, run: AgentRun) -> None:
        """
        发布单张卡片的首个运行事件

        :param run: 当前 Agent Run
        :return: 无返回值
        """

        await self._publish_and_record(
            run,
            AgentRunEvent.create(
                event_type=AgentRealtimeEventType.AGENT_START,
                run_id=run.run_id,
                conversation_id=run.conversation_id,
                trigger_message_id=run.trigger_message_id,
                requester_user_id=run.requester_user_id,
                status=run.status,
                stage=run.stage,
                payload={"cardKey": f"agent-run:{run.run_id}"},
            ),
        )

    async def recover_interrupted_runtime(self, run: AgentRun) -> int:
        """
        固化租约过期 Worker 的最后快照并关闭旧运行时

        :param run: 已由当前 Worker 重新认领的 Agent Run
        :return: 已使用的最大 Turn 序号
        """

        raw_snapshot = await self._event_publisher.get_snapshot(run.run_id)
        snapshot = (
            AgentRunSnapshot.model_validate(raw_snapshot) if raw_snapshot is not None else None
        )
        async with self._unit_of_work_factory() as unit_of_work:
            runtime_list = await unit_of_work.agent_traces.list_open_runtime(run.run_id)
            latest_turn_no = await unit_of_work.agent_traces.get_latest_turn_no(run.run_id)
            for runtime in runtime_list:
                block_list = self._restore_partial_block_list(runtime, snapshot)
                await unit_of_work.agent_traces.interrupt_runtime(runtime, block_list)
            await unit_of_work.commit()
        for runtime in runtime_list:
            await self._publish_interrupted_runtime(run, runtime.turn, runtime.message)
        return latest_turn_no

    async def start_step(
        self,
        run: AgentRun,
        *,
        sequence_no: int,
        stage: AgentRunStage,
        step_type: AgentStepType,
        step_code: str,
        display_name: str,
        visibility: TraceVisibility = TraceVisibility.MEMBER,
    ) -> AgentStep:
        """
        切换阶段并创建成员可见步骤

        :param run: 当前 Agent Run
        :param sequence_no: 执行步骤顺序号
        :param stage: Agent Run 执行阶段
        :param step_type: 执行步骤类型
        :param step_code: 执行步骤代码
        :param display_name: 步骤展示名称
        :param visibility: 步骤可见范围
        :return: 已进入运行中的成员可见步骤
        """

        await self._ensure_not_cancelled(run)
        step = AgentStep(
            step_id=self._id_generator.next_id(),
            run_id=run.run_id,
            sequence_no=sequence_no,
            step_type=step_type,
            step_code=step_code,
            display_name=display_name,
            visibility=visibility,
        )
        step.start()
        async with self._unit_of_work_factory() as unit_of_work:
            current = await self._get_locked_non_cancelled_run(unit_of_work, run.run_id)
            if current.is_terminal:
                raise AgentDomainError(
                    AgentErrorCode.RUN_STATE_INVALID,
                    "终态Agent Run不能继续执行",
                )
            current.change_stage(stage)
            await unit_of_work.agent_runs.update(current)
            await unit_of_work.agent_traces.insert_step(step)
            await unit_of_work.commit()
        run.status = current.status
        run.stage = current.stage
        return step

    async def complete_step(
        self,
        step: AgentStep,
        *,
        input_summary: dict[str, object] | None = None,
        output_summary: dict[str, object] | None = None,
    ) -> None:
        """
        完成步骤并写入脱敏摘要

        :param step: Agent 执行步骤
        :param input_summary: 脱敏输入摘要
        :param output_summary: 脱敏输出摘要
        :return: 无返回值
        """

        step.complete()
        async with self._unit_of_work_factory() as unit_of_work:
            await unit_of_work.agent_traces.update_step(
                step,
                input_summary,
                output_summary,
            )
            await unit_of_work.commit()

    async def fail_step(
        self,
        step: AgentStep,
        error_code: str,
        error_message: str,
    ) -> None:
        """
        记录步骤失败，错误信息限制为脱敏摘要

        :param step: Agent 执行步骤
        :param error_code: 错误码
        :param error_message: 脱敏错误摘要
        :return: 无返回值
        """

        step.fail(error_code, error_message[:500])
        async with self._unit_of_work_factory() as unit_of_work:
            await unit_of_work.agent_traces.update_step(step, None, None)
            await unit_of_work.commit()

    async def start_turn(
        self,
        run: AgentRun,
        turn_no: int,
        model_id: str,
    ) -> tuple[AgentRunTurn, AgentRunMessage]:
        """
        创建 Turn 和持续更新的 partial assistant message

        :param run: 当前 Agent Run
        :param turn_no: Turn 顺序号
        :param model_id: 模型标识
        :return: 已持久化的 Turn 和运行时消息
        """

        await self._ensure_not_cancelled(run)
        turn = AgentRunTurn(
            turn_id=self._id_generator.next_id(),
            run_id=run.run_id,
            turn_no=turn_no,
        )
        message = AgentRunMessage(
            message_id=self._id_generator.next_id(),
            run_id=run.run_id,
            turn_id=turn.turn_id,
            turn_no=turn_no,
            message_key=f"assistant:{turn_no}",
            role="assistant",
            model_id=model_id,
        )
        async with self._unit_of_work_factory() as unit_of_work:
            await unit_of_work.agent_traces.insert_turn(turn, message)
            await unit_of_work.commit()
        await self._publish_and_record(
            run,
            AgentRunEvent.create(
                event_type=AgentRealtimeEventType.TURN_START,
                run_id=run.run_id,
                conversation_id=run.conversation_id,
                trigger_message_id=run.trigger_message_id,
                requester_user_id=run.requester_user_id,
                status=run.status,
                stage=run.stage,
                turn_no=turn_no,
                payload={"turnId": str(turn.turn_id)},
            ),
        )
        await self._publish_and_record(
            run,
            AgentRunEvent.create(
                event_type=AgentRealtimeEventType.MESSAGE_START,
                run_id=run.run_id,
                conversation_id=run.conversation_id,
                trigger_message_id=run.trigger_message_id,
                requester_user_id=run.requester_user_id,
                status=run.status,
                stage=run.stage,
                turn_no=turn_no,
                message_key=message.message_key,
                payload={"role": message.role, "modelId": model_id},
            ),
        )
        return turn, message

    async def publish_model_event(
        self,
        run: AgentRun,
        turn: AgentRunTurn,
        message: AgentRunMessage,
        model_event: ChatModelEvent,
        tool_call_id: str | None = None,
        tool_name: str | None = None,
    ) -> None:
        """
        将 Provider 内容事件立即发布为 message_update

        :param run: 当前 Agent Run
        :param turn: 当前 Agent Turn
        :param message: 当前 partial assistant message
        :param model_event: Provider 内容事件
        :param tool_call_id: 当前工具调用 ID
        :param tool_name: 当前工具名称
        :return: 无返回值
        """

        if model_event.event_type in {
            ProviderEventType.START,
            ProviderEventType.DONE,
            ProviderEventType.ERROR,
        }:
            return
        if await self._cancellation_store.is_cancel_requested(run.run_id):
            raise AgentRunCancelledError(AgentErrorCode.RUN_CANCELLED, "Agent Run已取消")
        await self._publish_and_record(
            run,
            AgentRunEvent.create(
                event_type=AgentRealtimeEventType.MESSAGE_UPDATE,
                run_id=run.run_id,
                conversation_id=run.conversation_id,
                trigger_message_id=run.trigger_message_id,
                requester_user_id=run.requester_user_id,
                status=run.status,
                stage=run.stage,
                turn_no=turn.turn_no,
                message_key=message.message_key,
                payload={
                    "assistantMessageEvent": {
                        "type": str(model_event.event_type),
                        "contentIndex": model_event.content_index,
                        "delta": model_event.delta,
                        "toolCallId": tool_call_id,
                        "toolName": tool_name,
                    }
                },
            ),
        )

    async def persist_content_block(self, block: AgentRunContentBlock) -> None:
        """
        在内容边界结束时持久化完整 Block

        :param block: 已完成内容块
        :return: 无返回值
        """

        async with self._unit_of_work_factory() as unit_of_work:
            await unit_of_work.agent_traces.insert_content_block(block)
            await unit_of_work.commit()

    def create_content_block(
        self,
        run: AgentRun,
        turn: AgentRunTurn,
        message: AgentRunMessage,
        content_index: int,
        block_type: AgentContentBlockType,
        tool_call_id: str | None = None,
        tool_name: str | None = None,
    ) -> AgentRunContentBlock:
        """
        创建仅在内存累积、结束时才落库的内容块

        :param run: 当前 Agent Run
        :param turn: 当前 Agent Turn
        :param message: 当前运行时消息
        :param content_index: 内容块顺序号
        :param block_type: 内容块类型
        :param tool_call_id: 模型工具调用 ID
        :param tool_name: 模型工具名称
        :return: 尚未持久化的内容块
        """

        return AgentRunContentBlock(
            block_id=self._id_generator.next_id(),
            run_id=run.run_id,
            turn_id=turn.turn_id,
            message_id=message.message_id,
            content_index=content_index,
            block_type=block_type,
            visibility=TraceVisibility.MEMBER,
            tool_call_id=tool_call_id,
            tool_name=tool_name,
        )

    async def complete_turn(
        self,
        run: AgentRun,
        turn: AgentRunTurn,
        message: AgentRunMessage,
        stop_reason: str | None,
        input_tokens: int,
        output_tokens: int,
    ) -> None:
        """
        完成 Turn、Message 并发布 message_end 和 turn_end

        :param run: 当前 Agent Run
        :param turn: 当前 Agent Turn
        :param message: 当前运行时消息
        :param stop_reason: Provider 停止原因
        :param input_tokens: 输入 Token 数量
        :param output_tokens: 输出 Token 数量
        :return: 无返回值
        """

        turn.complete(stop_reason)
        message.complete(stop_reason, input_tokens, output_tokens)
        async with self._unit_of_work_factory() as unit_of_work:
            await unit_of_work.agent_traces.update_turn(turn, message)
            await unit_of_work.commit()
        await self._publish_and_record(
            run,
            AgentRunEvent.create(
                event_type=AgentRealtimeEventType.MESSAGE_END,
                run_id=run.run_id,
                conversation_id=run.conversation_id,
                trigger_message_id=run.trigger_message_id,
                requester_user_id=run.requester_user_id,
                status=run.status,
                stage=run.stage,
                turn_no=turn.turn_no,
                message_key=message.message_key,
                payload={
                    "status": str(message.status),
                    "stopReason": stop_reason,
                    "usage": {
                        "inputTokens": input_tokens,
                        "outputTokens": output_tokens,
                    },
                },
            ),
        )
        await self._publish_and_record(
            run,
            AgentRunEvent.create(
                event_type=AgentRealtimeEventType.TURN_END,
                run_id=run.run_id,
                conversation_id=run.conversation_id,
                trigger_message_id=run.trigger_message_id,
                requester_user_id=run.requester_user_id,
                status=run.status,
                stage=run.stage,
                turn_no=turn.turn_no,
                payload={"status": str(turn.status), "stopReason": stop_reason},
            ),
        )

    async def start_tool_execution(
        self,
        run: AgentRun,
        turn: AgentRunTurn,
        message: AgentRunMessage,
        step: AgentStep,
        tool_call: ChatToolCall,
        display_name: str,
        arguments_summary: dict[str, object],
    ) -> AgentToolExecution:
        """
        持久化并发布真实工具执行开始事件

        :param run: 当前 Agent Run
        :param turn: 工具所属 Turn
        :param message: 工具所属运行时消息
        :param step: 对应业务步骤
        :param tool_call: 模型工具调用
        :param display_name: 工具显示名称
        :param arguments_summary: 脱敏参数摘要
        :return: 运行中的工具执行
        """

        execution = AgentToolExecution(
            tool_execution_id=self._id_generator.next_id(),
            run_id=run.run_id,
            turn_id=turn.turn_id,
            turn_no=turn.turn_no,
            message_id=message.message_id,
            step_id=step.step_id,
            tool_call_id=tool_call.call_id,
            tool_name=tool_call.name,
            display_name=display_name,
            visibility=TraceVisibility.MEMBER,
            arguments_summary=arguments_summary,
        )
        async with self._unit_of_work_factory() as unit_of_work:
            await unit_of_work.agent_traces.insert_tool_execution(execution)
            await unit_of_work.commit()
        await self._publish_tool_event(
            run,
            turn,
            execution,
            AgentRealtimeEventType.TOOL_EXECUTION_START,
        )
        return execution

    async def complete_tool_execution(
        self,
        run: AgentRun,
        execution: AgentToolExecution,
        result_summary: dict[str, object],
    ) -> None:
        """
        完成工具执行并发布结果摘要

        :param run: 当前 Agent Run
        :param execution: 当前工具执行
        :param result_summary: 成员可见结果摘要
        :return: 无返回值
        """

        execution.succeed(result_summary)
        async with self._unit_of_work_factory() as unit_of_work:
            await unit_of_work.agent_traces.update_tool_execution(execution)
            await unit_of_work.commit()
        await self._publish_tool_event(
            run,
            AgentRunTurn(
                turn_id=execution.turn_id,
                run_id=run.run_id,
                turn_no=execution.turn_no,
            ),
            execution,
            AgentRealtimeEventType.TOOL_EXECUTION_END,
        )

    async def fail_tool_execution(
        self,
        run: AgentRun,
        execution: AgentToolExecution,
        error_code: str,
        error_message: str,
    ) -> None:
        """
        标记工具执行失败并发布错误摘要

        :param run: 当前 Agent Run
        :param execution: 当前工具执行
        :param error_code: 稳定错误码
        :param error_message: 脱敏错误摘要
        :return: 无返回值
        """

        execution.fail(error_code, error_message)
        async with self._unit_of_work_factory() as unit_of_work:
            await unit_of_work.agent_traces.update_tool_execution(execution)
            await unit_of_work.commit()
        await self._publish_tool_event(
            run,
            AgentRunTurn(
                turn_id=execution.turn_id,
                run_id=run.run_id,
                turn_no=execution.turn_no,
            ),
            execution,
            AgentRealtimeEventType.TOOL_EXECUTION_END,
        )

    async def publish_citations(
        self,
        run: AgentRun,
        citation_list: list[dict[str, object]],
    ) -> None:
        """
        发布引用更新事件

        :param run: 当前 Agent Run
        :param citation_list: Agent 回答引用列表
        :return: 无返回值
        """

        if not citation_list:
            return
        await self._event_publisher.publish(
            AgentRunEvent.create(
                event_type=AgentRealtimeEventType.MESSAGE_UPDATE,
                run_id=run.run_id,
                conversation_id=run.conversation_id,
                trigger_message_id=run.trigger_message_id,
                requester_user_id=run.requester_user_id,
                status=run.status,
                stage=run.stage,
                payload={"citationList": citation_list},
            )
        )

    async def succeed(self, run: AgentRun, answer_message_id: int) -> None:
        """
        提交成功终态并通知卡片替换为正式消息

        :param run: 当前 Agent Run
        :param answer_message_id: 正式回答消息 ID
        :return: 无返回值
        """

        async with self._unit_of_work_factory() as unit_of_work:
            current = await self._get_locked_non_cancelled_run(unit_of_work, run.run_id)
            if current.status is AgentRunStatus.SUCCEEDED:
                if current.answer_message_id != answer_message_id:
                    raise AgentDomainError(
                        AgentErrorCode.RUN_STATE_INVALID,
                        "Agent Run正式回复ID不一致",
                    )
            elif current.is_terminal:
                raise AgentDomainError(
                    AgentErrorCode.RUN_STATE_INVALID,
                    "终态Agent Run不能标记成功",
                )
            else:
                current.succeed(answer_message_id)
                await unit_of_work.agent_runs.update(current)
            await unit_of_work.commit()
        self._synchronize_run(run, current)
        await self._publish_and_record(
            run,
            AgentRunEvent.create(
                event_type=AgentRealtimeEventType.AGENT_END,
                run_id=run.run_id,
                conversation_id=run.conversation_id,
                trigger_message_id=run.trigger_message_id,
                requester_user_id=run.requester_user_id,
                status=run.status,
                stage=run.stage,
                payload={"answerMessageId": str(answer_message_id)},
            ),
        )

    async def cancel(self, run_id: int, requested_by: int) -> AgentRun:
        """
        提交取消终态并通知正在生成的 Worker

        :param run_id: Agent Run ID
        :param requested_by: 发起操作的用户 ID
        :return: 取消后的最新 Agent Run
        """

        async with self._unit_of_work_factory() as unit_of_work:
            await unit_of_work.business_locks.lock("agent-run", str(run_id))
            current = await unit_of_work.agent_runs.get_by_id(run_id)
            if current is None:
                raise AgentDomainError(
                    AgentErrorCode.RUN_NOT_FOUND,
                    "Agent Run不存在",
                )
            if current.status is AgentRunStatus.CANCELLED:
                await unit_of_work.commit()
                return current
            if current.is_terminal:
                raise AgentDomainError(
                    AgentErrorCode.RUN_STATE_INVALID,
                    "已完成的Agent Run不能取消",
                )
            if current.stage is AgentRunStage.FINALIZING:
                raise AgentDomainError(
                    AgentErrorCode.RUN_STATE_INVALID,
                    "Agent Run正在保存正式回复，不能取消",
                )
            current.cancel(requested_by)
            await unit_of_work.agent_runs.update(current)
            await unit_of_work.commit()
        await self._cancellation_store.request_cancel(current.run_id)
        await self._publish_and_record(
            current,
            AgentRunEvent.create(
                event_type=AgentRealtimeEventType.AGENT_END,
                run_id=current.run_id,
                conversation_id=current.conversation_id,
                trigger_message_id=current.trigger_message_id,
                requester_user_id=current.requester_user_id,
                status=current.status,
                stage=current.stage,
                payload={"errorCode": str(AgentErrorCode.RUN_CANCELLED)},
            ),
        )
        return current

    async def fail(self, run: AgentRun, error_code: str, error_message: str) -> None:
        """
        提交失败终态并发布用户可理解的卡片状态

        :param run: 当前 Agent Run
        :param error_code: 错误码
        :param error_message: 脱敏错误摘要
        :return: 无返回值
        """

        async with self._unit_of_work_factory() as unit_of_work:
            await unit_of_work.business_locks.lock("agent-run", str(run.run_id))
            current = await unit_of_work.agent_runs.get_by_id(run.run_id)
            if current is None:
                raise AgentDomainError(
                    AgentErrorCode.RUN_NOT_FOUND,
                    "Agent Run不存在",
                )
            if current.is_terminal:
                await unit_of_work.commit()
                return
            current.fail(error_code, error_message[:500])
            await unit_of_work.agent_runs.update(current)
            await unit_of_work.commit()
        self._synchronize_run(run, current)
        await self._publish_and_record(
            run,
            AgentRunEvent.create(
                event_type=AgentRealtimeEventType.AGENT_END,
                run_id=run.run_id,
                conversation_id=run.conversation_id,
                trigger_message_id=run.trigger_message_id,
                requester_user_id=run.requester_user_id,
                status=run.status,
                stage=run.stage,
                payload={
                    "errorCode": error_code,
                    "errorMessage": error_message[:500],
                },
            ),
        )

    async def _publish_and_record(self, run: AgentRun, event: AgentRunEvent) -> None:
        """
        发布实时事件并记录最新事件序号

        :param run: 当前 Agent Run
        :param event: 待处理的 Agent 实时事件
        :return: 无返回值
        """

        published_event = await self._event_publisher.publish(event)
        if published_event.sequence <= run.last_event_sequence:
            return
        run.last_event_sequence = published_event.sequence
        async with self._unit_of_work_factory() as unit_of_work:
            await unit_of_work.agent_runs.update(run)
            await unit_of_work.commit()

    def _restore_partial_block_list(
        self,
        runtime: AgentOpenRuntime,
        snapshot: AgentRunSnapshot | None,
    ) -> list[AgentRunContentBlock]:
        """
        从 Redis Snapshot 恢复尚未写入 PostgreSQL 的内容块

        :param runtime: 当前未关闭运行时
        :param snapshot: Redis 中最后物化快照
        :return: 待作为 INTERRUPTED 写入的内容块列表
        """

        if snapshot is None:
            return []
        message_snapshot = next(
            (
                message
                for message in snapshot.message_list
                if message.message_key == runtime.message.message_key
            ),
            None,
        )
        if message_snapshot is None:
            return []
        block_list: list[AgentRunContentBlock] = []
        for block_snapshot in message_snapshot.content_block_list:
            if (
                block_snapshot.content_index in runtime.persisted_content_index_set
                or not block_snapshot.content
            ):
                continue
            block = AgentRunContentBlock(
                block_id=self._id_generator.next_id(),
                run_id=runtime.turn.run_id,
                turn_id=runtime.turn.turn_id,
                message_id=runtime.message.message_id,
                content_index=block_snapshot.content_index,
                block_type=block_snapshot.block_type,
                visibility=TraceVisibility.MEMBER,
                content=block_snapshot.content,
                tool_call_id=block_snapshot.tool_call_id,
                tool_name=block_snapshot.tool_name,
            )
            block.interrupt()
            block_list.append(block)
        return block_list

    async def _publish_interrupted_runtime(
        self,
        run: AgentRun,
        turn: AgentRunTurn,
        message: AgentRunMessage,
    ) -> None:
        """
        发布旧 Turn 和 partial message 的中断边界事件

        :param run: 当前 Agent Run
        :param turn: 已中断 Turn
        :param message: 已中断运行时消息
        :return: 无返回值
        """

        await self._publish_and_record(
            run,
            AgentRunEvent.create(
                event_type=AgentRealtimeEventType.MESSAGE_END,
                run_id=run.run_id,
                conversation_id=run.conversation_id,
                trigger_message_id=run.trigger_message_id,
                requester_user_id=run.requester_user_id,
                status=run.status,
                stage=run.stage,
                turn_no=turn.turn_no,
                message_key=message.message_key,
                payload={
                    "status": str(message.status),
                    "stopReason": message.stop_reason,
                },
            ),
        )
        await self._publish_and_record(
            run,
            AgentRunEvent.create(
                event_type=AgentRealtimeEventType.TURN_END,
                run_id=run.run_id,
                conversation_id=run.conversation_id,
                trigger_message_id=run.trigger_message_id,
                requester_user_id=run.requester_user_id,
                status=run.status,
                stage=run.stage,
                turn_no=turn.turn_no,
                payload={"status": str(turn.status), "stopReason": turn.stop_reason},
            ),
        )

    async def _publish_tool_event(
        self,
        run: AgentRun,
        turn: AgentRunTurn,
        execution: AgentToolExecution,
        event_type: AgentRealtimeEventType,
    ) -> None:
        """
        发布工具执行生命周期事件

        :param run: 当前 Agent Run
        :param turn: 工具所属 Turn
        :param execution: 当前工具执行
        :param event_type: 工具执行事件类型
        :return: 无返回值
        """

        duration_ms = (
            int((execution.finished_at - execution.started_at).total_seconds() * 1000)
            if execution.finished_at is not None
            else None
        )
        await self._publish_and_record(
            run,
            AgentRunEvent.create(
                event_type=event_type,
                run_id=run.run_id,
                conversation_id=run.conversation_id,
                trigger_message_id=run.trigger_message_id,
                requester_user_id=run.requester_user_id,
                status=run.status,
                stage=run.stage,
                turn_no=turn.turn_no,
                payload={
                    "toolCallId": execution.tool_call_id,
                    "toolName": execution.tool_name,
                    "displayName": execution.display_name,
                    "status": str(execution.status),
                    "argumentsSummary": execution.arguments_summary,
                    "resultSummary": execution.result_summary,
                    "errorCode": execution.error_code,
                    "errorMessage": execution.error_message,
                    "durationMs": duration_ms,
                },
            ),
        )

    async def _ensure_not_cancelled(self, run: AgentRun) -> None:
        """
        校验 Agent Run 尚未请求取消

        :param run: 当前 Agent Run
        :return: 无返回值
        """

        if await self._cancellation_store.is_cancel_requested(run.run_id):
            raise AgentRunCancelledError(
                AgentErrorCode.RUN_CANCELLED,
                "Agent Run已取消",
            )
        async with self._unit_of_work_factory() as unit_of_work:
            current = await unit_of_work.agent_runs.get_by_id(run.run_id)
        if current is not None and current.status is AgentRunStatus.CANCELLED:
            raise AgentRunCancelledError(
                AgentErrorCode.RUN_CANCELLED,
                "Agent Run已取消",
            )

    @staticmethod
    async def _get_locked_non_cancelled_run(
        unit_of_work: AgentUnitOfWork,
        run_id: int,
    ) -> AgentRun:
        """
        在事务锁内读取尚未取消的 Run

        :param unit_of_work: 当前 Agent 工作单元
        :param run_id: Agent Run ID
        :return: 已加锁且尚未取消的 Agent Run
        """

        await unit_of_work.business_locks.lock("agent-run", str(run_id))
        current = await unit_of_work.agent_runs.get_by_id(run_id)
        if current is None:
            raise AgentDomainError(
                AgentErrorCode.RUN_NOT_FOUND,
                "Agent Run不存在",
            )
        if current.status is AgentRunStatus.CANCELLED:
            raise AgentRunCancelledError(
                AgentErrorCode.RUN_CANCELLED,
                "Agent Run已取消",
            )
        return current

    @staticmethod
    def _synchronize_run(target: AgentRun, source: AgentRun) -> None:
        """
        把事务内确认的运行状态同步到当前工作流对象

        :param target: 待同步的 Agent Run
        :param source: 源领域对象
        :return: 无返回值
        """

        target.status = source.status
        target.stage = source.stage
        target.answer_message_id = source.answer_message_id
        target.last_event_sequence = source.last_event_sequence
        target.error_code = source.error_code
        target.error_message = source.error_message
        target.completed_at = source.completed_at
        target.cancel_requested_at = source.cancel_requested_at
        target.cancel_requested_by = source.cancel_requested_by
