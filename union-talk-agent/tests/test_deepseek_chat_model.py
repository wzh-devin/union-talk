"""DeepSeek Provider V2 流式事件测试。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/08/12 23:58
"""

import json
from collections.abc import AsyncIterator
from typing import cast

import httpx
import pytest

from union_talk_agent.agent_config.domain.enums import ProviderType
from union_talk_agent.agent_config.domain.model_config import RuntimeModelConfig
from union_talk_agent.agent_run.domain.chat_model import (
    ChatMessage,
    ChatToolCall,
    ChatToolDefinition,
)
from union_talk_agent.agent_run.domain.enums import ProviderEventType
from union_talk_agent.agent_run.domain.exceptions import RetryableAgentError
from union_talk_agent.infrastructure.model.deepseek_chat_model import DeepSeekChatModel


def model_config(max_retries: int = 1) -> RuntimeModelConfig:
    """
    构造 DeepSeek 运行配置

    :param max_retries: 模型请求最大重试次数
    :return: 测试模型配置
    """

    return RuntimeModelConfig(
        credential_id=1,
        credential_version=1,
        credential_owner_user_id=2,
        provider=ProviderType.DEEPSEEK,
        api_base="https://deepseek.test/v1",
        api_key="secret",
        model_id="deepseek-chat",
        timeout_ms=5000,
        max_retries=max_retries,
    )


def sse_payload(delta: dict[str, object], finish_reason: str | None = None) -> bytes:
    """
    构造 DeepSeek SSE 帧

    :param delta: Choice Delta
    :param finish_reason: 停止原因
    :return: 编码后的 SSE 内容
    """

    payload = {"choices": [{"delta": delta, "finish_reason": finish_reason}]}
    return f"data: {json.dumps(payload)}\n\ndata: [DONE]\n\n".encode()


async def test_retry_happens_before_first_content_delta() -> None:
    """
    首个内容 Delta 前的 503 会重试

    :return: 无返回值
    """

    request_count = 0

    def handler(_: httpx.Request) -> httpx.Response:
        """
        首次返回 503，第二次返回正文

        :param _: 未使用的请求
        :return: 测试响应
        """

        nonlocal request_count
        request_count += 1
        if request_count == 1:
            return httpx.Response(503)
        return httpx.Response(200, content=sse_payload({"content": "成功"}))

    model = DeepSeekChatModel(httpx.AsyncClient(transport=httpx.MockTransport(handler)))
    event_list = [
        event
        async for event in model.stream(
            model_config(),
            [ChatMessage(role="user", content="问题")],
            100,
            0.2,
            False,
            "medium",
        )
    ]

    assert [event.event_type for event in event_list if event.delta] == [
        ProviderEventType.TEXT_DELTA
    ]
    assert next(event.delta for event in event_list if event.delta) == "成功"
    assert request_count == 2
    await model.close()


async def test_thinking_tool_call_event_order_and_request_rules() -> None:
    """
    Thinking 与工具调用产生完整边界事件且请求排除不兼容字段

    :return: 无返回值
    """

    def handler(request: httpx.Request) -> httpx.Response:
        """
        校验 Thinking 请求并返回 reasoning 和工具调用

        :param request: DeepSeek 请求
        :return: 多帧 SSE 响应
        """

        request_payload: dict[str, object] = json.loads(request.content)
        assert request_payload["thinking"] == {"type": "enabled"}
        assert request_payload["reasoning_effort"] == "high"
        assert "temperature" not in request_payload
        assert "tool_choice" not in request_payload
        reasoning = {
            "choices": [
                {
                    "delta": {"reasoning_content": "需要检索"},
                    "finish_reason": None,
                }
            ]
        }
        tool_call = {
            "choices": [
                {
                    "delta": {
                        "tool_calls": [
                            {
                                "index": 0,
                                "id": "call-1",
                                "function": {
                                    "name": "search_conversation_resources",
                                    "arguments": '{"query":"RAG"}',
                                },
                            }
                        ]
                    },
                    "finish_reason": "tool_calls",
                }
            ]
        }
        content = (
            f"data: {json.dumps(reasoning)}\n\ndata: {json.dumps(tool_call)}\n\ndata: [DONE]\n\n"
        ).encode()
        return httpx.Response(200, content=content)

    model = DeepSeekChatModel(httpx.AsyncClient(transport=httpx.MockTransport(handler)))
    event_list = [
        event
        async for event in model.stream(
            model_config(),
            [ChatMessage(role="user", content="问题")],
            100,
            0.2,
            True,
            "high",
            [
                ChatToolDefinition(
                    name="search_conversation_resources",
                    description="搜索会话资源",
                    parameters={"type": "object"},
                )
            ],
        )
    ]

    assert [event.event_type for event in event_list] == [
        ProviderEventType.START,
        ProviderEventType.THINKING_START,
        ProviderEventType.THINKING_DELTA,
        ProviderEventType.THINKING_END,
        ProviderEventType.TOOL_CALL_START,
        ProviderEventType.TOOL_CALL_DELTA,
        ProviderEventType.TOOL_CALL_END,
        ProviderEventType.DONE,
    ]
    tool_event = next(
        event for event in event_list if event.event_type is ProviderEventType.TOOL_CALL_DELTA
    )
    assert tool_event.tool_call_delta is not None
    assert tool_event.tool_call_delta.call_id == "call-1"
    await model.close()


async def test_reasoning_content_is_returned_with_tool_call_next_turn() -> None:
    """
    Thinking 工具 Turn 在下一请求完整回传 reasoning_content

    :return: 无返回值
    """

    def handler(request: httpx.Request) -> httpx.Response:
        """
        校验 Assistant 工具消息包含 reasoning_content

        :param request: DeepSeek 请求
        :return: 最终正文 SSE
        """

        request_payload: dict[str, object] = json.loads(request.content)
        message_list = request_payload["messages"]
        assert isinstance(message_list, list)
        typed_message_list = cast(list[object], message_list)
        assistant_message = typed_message_list[1]
        assert isinstance(assistant_message, dict)
        typed_assistant_message = cast(dict[str, object], assistant_message)
        assert typed_assistant_message["reasoning_content"] == "完整推理"
        return httpx.Response(200, content=sse_payload({"content": "完成"}, "stop"))

    model = DeepSeekChatModel(httpx.AsyncClient(transport=httpx.MockTransport(handler)))
    event_list = [
        event
        async for event in model.stream(
            model_config(),
            [
                ChatMessage(role="user", content="问题"),
                ChatMessage(
                    role="assistant",
                    content="",
                    reasoning_content="完整推理",
                    tool_call_list=(ChatToolCall("call-1", "search_conversation_resources", "{}"),),
                ),
                ChatMessage(role="tool", content="结果", tool_call_id="call-1"),
            ],
            100,
            0.2,
            True,
            "medium",
        )
    ]

    assert any(event.delta == "完成" for event in event_list)
    await model.close()


class FailingAfterTokenStream(httpx.AsyncByteStream):
    """输出首个 Text Delta 后模拟网络断流。"""

    async def __aiter__(self) -> AsyncIterator[bytes]:
        """
        迭代一个正文帧后抛出读取错误

        :yield: DeepSeek SSE 字节帧
        """

        yield sse_payload({"content": "部分"}).split(b"data: [DONE]")[0]
        raise httpx.ReadError("connection lost")

    async def aclose(self) -> None:
        """
        关闭测试流

        :return: 无返回值
        """


async def test_no_retry_after_first_content_delta() -> None:
    """
    首个内容 Delta 后断流不得从头重试

    :return: 无返回值
    """

    request_count = 0

    def handler(_: httpx.Request) -> httpx.Response:
        """
        返回首帧后断流响应

        :param _: 未使用的请求
        :return: 中断流响应
        """

        nonlocal request_count
        request_count += 1
        return httpx.Response(200, stream=FailingAfterTokenStream())

    model = DeepSeekChatModel(httpx.AsyncClient(transport=httpx.MockTransport(handler)))
    delta_list: list[str] = []
    event_type_list: list[ProviderEventType] = []
    with pytest.raises(RetryableAgentError):
        async for event in model.stream(
            model_config(max_retries=3),
            [ChatMessage(role="user", content="问题")],
            100,
            0.2,
            False,
            "medium",
        ):
            event_type_list.append(event.event_type)
            if event.event_type is ProviderEventType.TEXT_DELTA:
                delta_list.append(event.delta)

    assert delta_list == ["部分"]
    assert event_type_list[-1] is ProviderEventType.ERROR
    assert request_count == 1
    await model.close()
