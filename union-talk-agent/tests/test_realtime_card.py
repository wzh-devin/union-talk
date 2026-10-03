"""Pi 风格 Agent Run V2 实时快照测试。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/08/13 00:06
"""

from union_talk_agent.agent_run.domain.enums import (
    AgentContentBlockStatus,
    AgentMessageStatus,
    AgentRealtimeEventType,
    AgentRunStage,
    AgentRunStatus,
)
from union_talk_agent.agent_run.realtime.run_event import AgentRunEvent, AgentRunSnapshot


def event(
    event_type: AgentRealtimeEventType,
    sequence: int,
    *,
    turn_no: int | None = None,
    message_key: str | None = None,
    payload: dict[str, object] | None = None,
    status: AgentRunStatus = AgentRunStatus.RUNNING,
) -> AgentRunEvent:
    """
    构造同一 Run 的 V2 事件

    :param event_type: Agent Loop 事件类型
    :param sequence: 单调序号
    :param turn_no: Turn 序号
    :param message_key: partial message 稳定键
    :param payload: 事件载荷
    :param status: Run 状态
    :return: 已编号测试事件
    """

    return AgentRunEvent.create(
        event_type=event_type,
        run_id=11,
        conversation_id=22,
        trigger_message_id=33,
        requester_user_id=44,
        status=status,
        stage=AgentRunStage.GENERATING,
        turn_no=turn_no,
        message_key=message_key,
        payload=payload,
    ).model_copy(update={"sequence": sequence, "event_id": f"11:{sequence}"})


def test_snapshot_materializes_thinking_text_and_tool_execution() -> None:
    """
    V2 Snapshot 以复合键归并 Thinking、Text 和工具执行

    :return: 无返回值
    """

    snapshot = AgentRunSnapshot.from_event(event(AgentRealtimeEventType.AGENT_START, 1))
    snapshot = snapshot.apply(
        event(
            AgentRealtimeEventType.MESSAGE_START,
            2,
            turn_no=1,
            message_key="assistant:1",
            payload={"role": "assistant", "modelId": "deepseek-chat"},
        )
    )
    snapshot = snapshot.apply(
        event(
            AgentRealtimeEventType.MESSAGE_UPDATE,
            3,
            turn_no=1,
            message_key="assistant:1",
            payload={
                "assistantMessageEvent": {
                    "type": "thinking_delta",
                    "contentIndex": 0,
                    "delta": "正在选择检索工具",
                }
            },
        )
    )
    snapshot = snapshot.apply(
        event(
            AgentRealtimeEventType.TOOL_EXECUTION_START,
            4,
            turn_no=1,
            payload={
                "toolCallId": "call-1",
                "toolName": "search_conversation_resources",
                "displayName": "检索会话资源",
                "status": "RUNNING",
            },
        )
    )
    duplicate = snapshot.apply(
        event(
            AgentRealtimeEventType.MESSAGE_UPDATE,
            3,
            turn_no=1,
            message_key="assistant:1",
            payload={
                "assistantMessageEvent": {
                    "type": "thinking_delta",
                    "contentIndex": 0,
                    "delta": "重复",
                }
            },
        )
    )

    assert duplicate.last_sequence == 4
    assert duplicate.message_list[0].content_block_list[0].content == "正在选择检索工具"
    assert duplicate.tool_execution_list[0].display_name == "检索会话资源"


def test_terminal_snapshot_ignores_late_running_event() -> None:
    """
    Agent End 后忽略迟到的运行中事件

    :return: 无返回值
    """

    snapshot = AgentRunSnapshot.from_event(event(AgentRealtimeEventType.AGENT_START, 1))
    terminal = snapshot.apply(
        event(
            AgentRealtimeEventType.AGENT_END,
            2,
            payload={"answerMessageId": "999"},
            status=AgentRunStatus.SUCCEEDED,
        )
    )
    unchanged = terminal.apply(event(AgentRealtimeEventType.TURN_START, 3, turn_no=2))

    assert unchanged.status is AgentRunStatus.SUCCEEDED
    assert unchanged.answer_message_id == "999"
    assert unchanged.last_sequence == 2


def test_interrupted_message_closes_all_streaming_blocks() -> None:
    """
    Worker 中断边界事件会关闭 Snapshot 中遗留的流式 Block

    :return: 无返回值
    """

    snapshot = AgentRunSnapshot.from_event(event(AgentRealtimeEventType.AGENT_START, 1))
    snapshot = snapshot.apply(
        event(
            AgentRealtimeEventType.MESSAGE_START,
            2,
            turn_no=1,
            message_key="assistant:1",
            payload={"role": "assistant", "modelId": "deepseek-chat"},
        )
    )
    snapshot = snapshot.apply(
        event(
            AgentRealtimeEventType.MESSAGE_UPDATE,
            3,
            turn_no=1,
            message_key="assistant:1",
            payload={
                "assistantMessageEvent": {
                    "type": "text_delta",
                    "contentIndex": 0,
                    "delta": "未完成正文",
                }
            },
        )
    )
    snapshot = snapshot.apply(
        event(
            AgentRealtimeEventType.MESSAGE_END,
            4,
            turn_no=1,
            message_key="assistant:1",
            payload={"status": "INTERRUPTED", "stopReason": "worker_lease_expired"},
        )
    )

    message = snapshot.message_list[0]
    assert message.status is AgentMessageStatus.INTERRUPTED
    assert message.content_block_list[0].status is AgentContentBlockStatus.INTERRUPTED


def test_realtime_json_serializes_snowflake_ids_as_strings() -> None:
    """
    浏览器协议中的 Snowflake ID 使用字符串

    :return: 无返回值
    """

    large_id = 9_223_372_036_854_775_000
    realtime_event = AgentRunEvent.create(
        event_type=AgentRealtimeEventType.AGENT_END,
        run_id=large_id,
        conversation_id=large_id - 1,
        trigger_message_id=large_id - 2,
        requester_user_id=large_id - 3,
        status=AgentRunStatus.SUCCEEDED,
        stage=AgentRunStage.FINALIZING,
        payload={"answerMessageId": str(large_id - 4)},
    )

    payload = realtime_event.model_dump(by_alias=True, mode="json")

    assert payload["schemaVersion"] == 2
    assert payload["runId"] == str(large_id)
    assert payload["type"] == "agent_end"
    assert payload["payload"]["answerMessageId"] == str(large_id - 4)
