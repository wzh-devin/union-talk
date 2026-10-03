"""Pi 风格 Agent 实时事件与物化快照。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/08/12 21:45
"""

from __future__ import annotations

from datetime import UTC, datetime
from typing import cast

from pydantic import BaseModel, ConfigDict, Field, field_serializer
from pydantic.alias_generators import to_camel

from union_talk_agent.agent_run.domain.enums import (
    AgentContentBlockStatus,
    AgentContentBlockType,
    AgentMessageStatus,
    AgentRealtimeEventType,
    AgentRunStage,
    AgentRunStatus,
    AgentToolExecutionStatus,
)


class RealtimeModel(BaseModel):
    """统一使用 camelCase 输出的实时模型。"""

    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)


class ContentBlockSnapshot(RealtimeModel):
    """单条 Assistant Message 的可见内容块快照。"""

    content_index: int
    block_type: AgentContentBlockType
    status: AgentContentBlockStatus = AgentContentBlockStatus.STREAMING
    content: str = ""
    tool_call_id: str | None = None
    tool_name: str | None = None


class MessageSnapshot(RealtimeModel):
    """一个 Turn 内持续更新的 Assistant Message 快照。"""

    turn_no: int
    message_key: str
    role: str = "assistant"
    status: AgentMessageStatus = AgentMessageStatus.STREAMING
    model_id: str = ""
    content_block_list: list[ContentBlockSnapshot] = Field(
        default_factory=lambda: list[ContentBlockSnapshot]()
    )
    stop_reason: str | None = None
    input_tokens: int = 0
    output_tokens: int = 0


class ToolExecutionSnapshot(RealtimeModel):
    """一次真实工具执行的实时快照。"""

    tool_call_id: str
    turn_no: int
    tool_name: str
    display_name: str
    status: AgentToolExecutionStatus = AgentToolExecutionStatus.RUNNING
    arguments_summary: dict[str, object] = Field(default_factory=dict)
    result_summary: dict[str, object] = Field(default_factory=dict)
    error_code: str | None = None
    error_message: str | None = None
    duration_ms: int | None = None


class AgentRunEvent(RealtimeModel):
    """Redis Stream 和 SSE 共用的 V2 Agent Loop 事件信封。"""

    schema_version: int = 2
    event_id: str = ""
    run_id: int
    conversation_id: int
    trigger_message_id: int
    requester_user_id: int
    sequence: int = 0
    event_type: AgentRealtimeEventType = Field(alias="type")
    turn_no: int | None = None
    message_key: str | None = None
    payload: dict[str, object] = Field(default_factory=dict)
    status: AgentRunStatus
    stage: AgentRunStage
    occurred_at: datetime = Field(default_factory=lambda: datetime.now(UTC))

    @field_serializer(
        "run_id",
        "conversation_id",
        "trigger_message_id",
        "requester_user_id",
        when_used="json",
    )
    def serialize_identifier(self, value: int) -> str:
        """
        避免浏览器解析 Snowflake ID 时丢失精度

        :param value: 待序列化的业务 ID
        :return: 可安全传输的字符串标识
        """

        return str(value)

    @classmethod
    def create(
        cls,
        *,
        event_type: AgentRealtimeEventType,
        run_id: int,
        conversation_id: int,
        trigger_message_id: int,
        requester_user_id: int,
        status: AgentRunStatus,
        stage: AgentRunStage,
        turn_no: int | None = None,
        message_key: str | None = None,
        payload: dict[str, object] | None = None,
    ) -> AgentRunEvent:
        """
        创建等待 Redis 分配单调序号的 V2 事件

        :param event_type: Agent Loop 事件类型
        :param run_id: Agent Run ID
        :param conversation_id: 会话 ID
        :param trigger_message_id: 触发消息 ID
        :param requester_user_id: 请求用户 ID
        :param status: Agent Run 状态
        :param stage: Agent Run 阶段
        :param turn_no: 当前 Turn 序号
        :param message_key: 当前 partial message 稳定键
        :param payload: 事件类型对应的增量载荷
        :return: 尚未分配 sequence 的实时事件
        """

        return cls(
            type=event_type,
            run_id=run_id,
            conversation_id=conversation_id,
            trigger_message_id=trigger_message_id,
            requester_user_id=requester_user_id,
            status=status,
            stage=stage,
            turn_no=turn_no,
            message_key=message_key,
            payload=payload or {},
        )


class AgentRunSnapshot(RealtimeModel):
    """可在 SSE 重连时直接恢复的 V2 Agent Loop 投影。"""

    schema_version: int = 2
    run_id: int
    conversation_id: int
    trigger_message_id: int
    requester_user_id: int
    card_key: str
    status: AgentRunStatus
    stage: AgentRunStage
    current_turn_no: int | None = None
    message_list: list[MessageSnapshot] = Field(default_factory=lambda: list[MessageSnapshot]())
    tool_execution_list: list[ToolExecutionSnapshot] = Field(
        default_factory=lambda: list[ToolExecutionSnapshot]()
    )
    citation_list: list[dict[str, object]] = Field(
        default_factory=lambda: list[dict[str, object]]()
    )
    last_sequence: int = 0
    answer_message_id: str | None = None
    error_code: str | None = None
    error_message: str | None = None
    updated_at: datetime = Field(default_factory=lambda: datetime.now(UTC))

    @field_serializer(
        "run_id",
        "conversation_id",
        "trigger_message_id",
        "requester_user_id",
        when_used="json",
    )
    def serialize_identifier(self, value: int) -> str:
        """
        保持 Snapshot 中 Snowflake ID 的浏览器安全格式

        :param value: 待序列化的业务 ID
        :return: 字符串业务 ID
        """

        return str(value)

    @classmethod
    def from_event(cls, event: AgentRunEvent) -> AgentRunSnapshot:
        """
        使用首个 Agent 事件初始化快照

        :param event: 同一 Run 的首个事件
        :return: 已应用首个事件的 Snapshot
        """

        return cls(
            run_id=event.run_id,
            conversation_id=event.conversation_id,
            trigger_message_id=event.trigger_message_id,
            requester_user_id=event.requester_user_id,
            card_key=f"agent-run:{event.run_id}",
            status=event.status,
            stage=event.stage,
        ).apply(event)

    def apply(self, event: AgentRunEvent) -> AgentRunSnapshot:
        """
        按复合业务键把增量事件归并到当前 Snapshot

        :param event: 同一 Run 的下一条单调事件
        :return: 已归并事件的 Snapshot 副本
        """

        if event.sequence <= self.last_sequence or not self.accepts(event):
            return self
        message_list = [message.model_copy(deep=True) for message in self.message_list]
        tool_execution_list = [tool.model_copy(deep=True) for tool in self.tool_execution_list]
        citation_list = list(self.citation_list)
        answer_message_id = self.answer_message_id
        error_code = self.error_code
        error_message = self.error_message
        current_turn_no = event.turn_no if event.turn_no is not None else self.current_turn_no

        if event.event_type is AgentRealtimeEventType.MESSAGE_START:
            self._apply_message_start(message_list, event)
        elif event.event_type is AgentRealtimeEventType.MESSAGE_UPDATE:
            self._apply_message_update(message_list, event)
        elif event.event_type is AgentRealtimeEventType.MESSAGE_END:
            self._apply_message_end(message_list, event)
        elif event.event_type in {
            AgentRealtimeEventType.TOOL_EXECUTION_START,
            AgentRealtimeEventType.TOOL_EXECUTION_UPDATE,
            AgentRealtimeEventType.TOOL_EXECUTION_END,
        }:
            self._apply_tool_execution(tool_execution_list, event)
        elif event.event_type is AgentRealtimeEventType.AGENT_END:
            raw_answer_message_id = event.payload.get("answerMessageId")
            answer_message_id = (
                str(raw_answer_message_id) if raw_answer_message_id is not None else None
            )
            raw_error_code = event.payload.get("errorCode")
            raw_error_message = event.payload.get("errorMessage")
            error_code = str(raw_error_code) if raw_error_code is not None else None
            error_message = str(raw_error_message) if raw_error_message is not None else None
        raw_citation_list = event.payload.get("citationList")
        if isinstance(raw_citation_list, list):
            item_list = cast(list[object], raw_citation_list)
            citation_list = [
                cast(dict[str, object], item) for item in item_list if isinstance(item, dict)
            ]

        return self.model_copy(
            update={
                "status": event.status,
                "stage": event.stage,
                "current_turn_no": current_turn_no,
                "message_list": message_list,
                "tool_execution_list": tool_execution_list,
                "citation_list": citation_list,
                "last_sequence": event.sequence,
                "answer_message_id": answer_message_id,
                "error_code": error_code,
                "error_message": error_message,
                "updated_at": event.occurred_at,
            }
        )

    def accepts(self, event: AgentRunEvent) -> bool:
        """
        阻止终态 Snapshot 被迟到的运行中事件回退

        :param event: 待判断的实时事件
        :return: 是否允许事件更新当前 Snapshot
        """

        terminal_status_set = {
            AgentRunStatus.SUCCEEDED,
            AgentRunStatus.FAILED,
            AgentRunStatus.CANCELLED,
        }
        return not (self.status in terminal_status_set and event.status not in terminal_status_set)

    @staticmethod
    def _apply_message_start(
        message_list: list[MessageSnapshot],
        event: AgentRunEvent,
    ) -> None:
        """
        创建本 Turn 的 partial assistant message

        :param message_list: 当前 Snapshot 消息列表
        :param event: message_start 事件
        :return: 无返回值
        """

        if event.message_key is None or event.turn_no is None:
            return
        if any(message.message_key == event.message_key for message in message_list):
            return
        message_list.append(
            MessageSnapshot(
                turn_no=event.turn_no,
                message_key=event.message_key,
                role=str(event.payload.get("role") or "assistant"),
                model_id=str(event.payload.get("modelId") or ""),
            )
        )

    @classmethod
    def _apply_message_update(
        cls,
        message_list: list[MessageSnapshot],
        event: AgentRunEvent,
    ) -> None:
        """
        把 Provider 内容增量写入指定 Message 和 Content Block

        :param message_list: 当前 Snapshot 消息列表
        :param event: message_update 事件
        :return: 无返回值
        """

        message = cls._find_message(message_list, event.message_key)
        raw_assistant_event = event.payload.get("assistantMessageEvent")
        if message is None or not isinstance(raw_assistant_event, dict):
            return
        assistant_event = cast(dict[str, object], raw_assistant_event)
        raw_type = assistant_event.get("type")
        raw_content_index = assistant_event.get("contentIndex")
        if not isinstance(raw_type, str) or not isinstance(raw_content_index, int):
            return
        block_type_map = {
            "text_start": AgentContentBlockType.TEXT,
            "text_delta": AgentContentBlockType.TEXT,
            "text_end": AgentContentBlockType.TEXT,
            "thinking_start": AgentContentBlockType.THINKING,
            "thinking_delta": AgentContentBlockType.THINKING,
            "thinking_end": AgentContentBlockType.THINKING,
            "toolcall_start": AgentContentBlockType.TOOL_CALL,
            "toolcall_delta": AgentContentBlockType.TOOL_CALL,
            "toolcall_end": AgentContentBlockType.TOOL_CALL,
        }
        block_type = block_type_map.get(raw_type)
        if block_type is None:
            return
        block = next(
            (
                item
                for item in message.content_block_list
                if item.content_index == raw_content_index
            ),
            None,
        )
        if block is None:
            block = ContentBlockSnapshot(
                content_index=raw_content_index,
                block_type=block_type,
                tool_call_id=cls._optional_string(assistant_event.get("toolCallId")),
                tool_name=cls._optional_string(assistant_event.get("toolName")),
            )
            message.content_block_list.append(block)
        raw_delta = assistant_event.get("delta")
        if isinstance(raw_delta, str):
            block.content = f"{block.content}{raw_delta}"
        if raw_type.endswith("_end"):
            block.status = AgentContentBlockStatus.COMPLETED

    @classmethod
    def _apply_message_end(
        cls,
        message_list: list[MessageSnapshot],
        event: AgentRunEvent,
    ) -> None:
        """
        标记 partial assistant message 为完成或失败

        :param message_list: 当前 Snapshot 消息列表
        :param event: message_end 事件
        :return: 无返回值
        """

        message = cls._find_message(message_list, event.message_key)
        if message is None:
            return
        raw_status = event.payload.get("status")
        if isinstance(raw_status, str):
            message.status = AgentMessageStatus(raw_status)
        if message.status is AgentMessageStatus.INTERRUPTED:
            for block in message.content_block_list:
                if block.status is AgentContentBlockStatus.STREAMING:
                    block.status = AgentContentBlockStatus.INTERRUPTED
        message.stop_reason = cls._optional_string(event.payload.get("stopReason"))
        raw_usage = event.payload.get("usage")
        if isinstance(raw_usage, dict):
            usage = cast(dict[str, object], raw_usage)
            input_tokens = usage.get("inputTokens")
            output_tokens = usage.get("outputTokens")
            message.input_tokens = input_tokens if isinstance(input_tokens, int) else 0
            message.output_tokens = output_tokens if isinstance(output_tokens, int) else 0

    @staticmethod
    def _apply_tool_execution(
        tool_execution_list: list[ToolExecutionSnapshot],
        event: AgentRunEvent,
    ) -> None:
        """
        归并工具执行开始、进度和结束事件

        :param tool_execution_list: 当前工具执行快照列表
        :param event: 工具执行事件
        :return: 无返回值
        """

        raw_tool_call_id = event.payload.get("toolCallId")
        if not isinstance(raw_tool_call_id, str) or event.turn_no is None:
            return
        tool = next(
            (item for item in tool_execution_list if item.tool_call_id == raw_tool_call_id),
            None,
        )
        if tool is None:
            tool = ToolExecutionSnapshot(
                tool_call_id=raw_tool_call_id,
                turn_no=event.turn_no,
                tool_name=str(event.payload.get("toolName") or ""),
                display_name=str(event.payload.get("displayName") or ""),
            )
            tool_execution_list.append(tool)
        raw_status = event.payload.get("status")
        if isinstance(raw_status, str):
            tool.status = AgentToolExecutionStatus(raw_status)
        raw_arguments = event.payload.get("argumentsSummary")
        if isinstance(raw_arguments, dict):
            tool.arguments_summary = raw_arguments
        raw_result = event.payload.get("resultSummary")
        if isinstance(raw_result, dict):
            tool.result_summary = raw_result
        raw_error_code = event.payload.get("errorCode")
        raw_error_message = event.payload.get("errorMessage")
        tool.error_code = str(raw_error_code) if raw_error_code is not None else None
        tool.error_message = str(raw_error_message) if raw_error_message is not None else None
        raw_duration_ms = event.payload.get("durationMs")
        tool.duration_ms = int(raw_duration_ms) if isinstance(raw_duration_ms, int) else None

    @staticmethod
    def _find_message(
        message_list: list[MessageSnapshot],
        message_key: str | None,
    ) -> MessageSnapshot | None:
        """
        按稳定键查找 Snapshot 中的 partial message

        :param message_list: 当前 Snapshot 消息列表
        :param message_key: partial message 稳定键
        :return: 匹配消息，不存在时返回空
        """

        if message_key is None:
            return None
        return next((item for item in message_list if item.message_key == message_key), None)

    @staticmethod
    def _optional_string(value: object) -> str | None:
        """
        把可选载荷值转换为字符串

        :param value: 待转换载荷值
        :return: 字符串值，空值保持为空
        """

        return str(value) if value is not None else None


class StoredRunEvent(RealtimeModel):
    """Redis Stream 游标和业务事件组合。"""

    stream_id: str
    event: AgentRunEvent
