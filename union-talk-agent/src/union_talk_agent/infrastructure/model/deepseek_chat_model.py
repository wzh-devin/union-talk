"""DeepSeek OpenAI 兼容 SSE Provider 事件适配器。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/08/12 22:02
"""

import json
from collections.abc import AsyncIterator
from dataclasses import dataclass, field
from typing import ClassVar, cast

import httpx

from union_talk_agent.agent_config.domain.model_config import RuntimeModelConfig
from union_talk_agent.agent_run.domain.chat_model import (
    ChatMessage,
    ChatModelEvent,
    ChatToolCallDelta,
    ChatToolDefinition,
)
from union_talk_agent.agent_run.domain.enums import ProviderEventType
from union_talk_agent.agent_run.domain.exceptions import (
    AgentErrorCode,
    NonRetryableAgentError,
    RetryableAgentError,
)


@dataclass(frozen=True, slots=True)
class _DeepSeekChunk:
    """单个 DeepSeek SSE Choice 的内部解析结果。"""

    content_delta: str = ""
    reasoning_delta: str = ""
    finish_reason: str | None = None
    tool_call_delta_list: tuple[ChatToolCallDelta, ...] = field(default_factory=tuple)
    input_tokens: int = 0
    output_tokens: int = 0


class DeepSeekChatModel:
    """把 DeepSeek 原生增量转换为稳定的 Provider 判别联合事件。"""

    _RETRYABLE_STATUS_CODE_SET: ClassVar[frozenset[int]] = frozenset(
        {408, 409, 425, 429, 500, 502, 503, 504}
    )

    def __init__(self, http_client: httpx.AsyncClient) -> None:
        """
        初始化 DeepSeek Provider 适配器

        :param http_client: 异步 HTTP 客户端
        :return: 无返回值
        """

        self._http_client = http_client

    async def stream(
        self,
        model_config: RuntimeModelConfig,
        message_list: list[ChatMessage],
        max_output_tokens: int,
        temperature: float,
        thinking_enabled: bool,
        thinking_effort: str,
        tool_list: list[ChatToolDefinition] | None = None,
    ) -> AsyncIterator[ChatModelEvent]:
        """
        流式生成 Provider 事件且只在首个内容事件之前重试

        :param model_config: 运行时模型配置
        :param message_list: 模型消息列表
        :param max_output_tokens: 最大输出 Token 数
        :param temperature: 模型采样温度
        :param thinking_enabled: 是否启用 Thinking
        :param thinking_effort: Thinking 强度
        :param tool_list: 当前回合可用工具目录
        :yield: Provider 判别联合事件
        """

        request_body: dict[str, object] = {
            "model": model_config.model_id,
            "messages": [self._serialize_message(message) for message in message_list],
            "max_tokens": max_output_tokens,
            "stream": True,
            "stream_options": {"include_usage": True},
        }
        if thinking_enabled:
            request_body["thinking"] = {"type": "enabled"}
            request_body["reasoning_effort"] = thinking_effort
        else:
            request_body["temperature"] = temperature
        if tool_list:
            request_body["tools"] = [
                {
                    "type": "function",
                    "function": {
                        "name": tool.name,
                        "description": tool.description,
                        "parameters": tool.parameters,
                    },
                }
                for tool in tool_list
            ]
            if not thinking_enabled:
                request_body["tool_choice"] = "auto"

        emitted_content = False
        for attempt in range(model_config.max_retries + 1):
            try:
                yield ChatModelEvent(event_type=ProviderEventType.START)
                async for event in self._translate_stream(model_config, request_body):
                    if event.event_type in {
                        ProviderEventType.TEXT_DELTA,
                        ProviderEventType.THINKING_DELTA,
                        ProviderEventType.TOOL_CALL_DELTA,
                    }:
                        emitted_content = True
                    yield event
                return
            except RetryableAgentError as error:
                if emitted_content or attempt >= model_config.max_retries:
                    yield self._error_event(error)
                    raise
            except NonRetryableAgentError as error:
                yield self._error_event(error)
                raise
        raise RetryableAgentError(
            AgentErrorCode.MODEL_REQUEST_FAILED,
            "DeepSeek流式请求重试次数耗尽",
        )

    @staticmethod
    def _error_event(error: RetryableAgentError | NonRetryableAgentError) -> ChatModelEvent:
        """
        把最终 Provider 异常转换为可观测的 error 事件

        :param error: 已停止重试的 Provider 异常
        :return: 包含稳定错误码和安全消息的 Provider 事件
        """

        return ChatModelEvent(
            event_type=ProviderEventType.ERROR,
            error_code=error.code.value,
            error_message=str(error),
        )

    async def test_connection(self, model_config: RuntimeModelConfig) -> None:
        """
        使用最小非流式请求测试 Provider 配置

        :param model_config: 运行时模型配置
        :return: 无返回值
        """

        try:
            response = await self._http_client.post(
                self._completion_url(model_config.api_base),
                headers=self._headers(model_config.api_key),
                json={
                    "model": model_config.model_id,
                    "messages": [{"role": "user", "content": "Reply OK"}],
                    "max_tokens": 1,
                    "stream": False,
                },
                timeout=model_config.timeout_ms / 1000,
            )
        except httpx.TimeoutException as error:
            raise RetryableAgentError(
                AgentErrorCode.MODEL_REQUEST_FAILED,
                "DeepSeek连接测试超时",
            ) from error
        except httpx.HTTPError as error:
            raise RetryableAgentError(
                AgentErrorCode.MODEL_REQUEST_FAILED,
                "DeepSeek连接测试网络失败",
            ) from error
        self._raise_for_status(response)

    async def close(self) -> None:
        """
        关闭 HTTP 连接池

        :return: 无返回值
        """

        await self._http_client.aclose()

    async def _translate_stream(
        self,
        model_config: RuntimeModelConfig,
        request_body: dict[str, object],
    ) -> AsyncIterator[ChatModelEvent]:
        """
        把 DeepSeek Chunk 转换为有明确边界的 Provider 事件

        :param model_config: 运行时模型配置
        :param request_body: DeepSeek 请求体
        :yield: start 之后的内容、完成或错误事件
        """

        thinking_started = False
        text_started = False
        tool_index_set: set[int] = set()
        last_finish_reason: str | None = None
        input_tokens = 0
        output_tokens = 0
        async for chunk in self._request_stream(model_config, request_body):
            if chunk.reasoning_delta:
                if not thinking_started:
                    thinking_started = True
                    yield ChatModelEvent(
                        event_type=ProviderEventType.THINKING_START,
                        content_index=0,
                    )
                yield ChatModelEvent(
                    event_type=ProviderEventType.THINKING_DELTA,
                    content_index=0,
                    delta=chunk.reasoning_delta,
                )
            if chunk.content_delta:
                if thinking_started:
                    thinking_started = False
                    yield ChatModelEvent(
                        event_type=ProviderEventType.THINKING_END,
                        content_index=0,
                    )
                if not text_started:
                    text_started = True
                    yield ChatModelEvent(
                        event_type=ProviderEventType.TEXT_START,
                        content_index=1,
                    )
                yield ChatModelEvent(
                    event_type=ProviderEventType.TEXT_DELTA,
                    content_index=1,
                    delta=chunk.content_delta,
                )
            for tool_delta in chunk.tool_call_delta_list:
                if thinking_started:
                    thinking_started = False
                    yield ChatModelEvent(
                        event_type=ProviderEventType.THINKING_END,
                        content_index=0,
                    )
                content_index = 2 + tool_delta.index
                if tool_delta.index not in tool_index_set:
                    tool_index_set.add(tool_delta.index)
                    yield ChatModelEvent(
                        event_type=ProviderEventType.TOOL_CALL_START,
                        content_index=content_index,
                    )
                yield ChatModelEvent(
                    event_type=ProviderEventType.TOOL_CALL_DELTA,
                    content_index=content_index,
                    tool_call_delta=tool_delta,
                )
            if chunk.finish_reason is not None:
                last_finish_reason = chunk.finish_reason
            input_tokens = max(input_tokens, chunk.input_tokens)
            output_tokens = max(output_tokens, chunk.output_tokens)

        if thinking_started:
            yield ChatModelEvent(
                event_type=ProviderEventType.THINKING_END,
                content_index=0,
            )
        if text_started:
            yield ChatModelEvent(
                event_type=ProviderEventType.TEXT_END,
                content_index=1,
            )
        for tool_index in sorted(tool_index_set):
            yield ChatModelEvent(
                event_type=ProviderEventType.TOOL_CALL_END,
                content_index=2 + tool_index,
                tool_call_delta=ChatToolCallDelta(index=tool_index),
            )
        yield ChatModelEvent(
            event_type=ProviderEventType.DONE,
            finish_reason=last_finish_reason,
            input_tokens=input_tokens,
            output_tokens=output_tokens,
        )

    async def _request_stream(
        self,
        model_config: RuntimeModelConfig,
        request_body: dict[str, object],
    ) -> AsyncIterator[_DeepSeekChunk]:
        """
        请求并读取 DeepSeek SSE Chunk

        :param model_config: 运行时模型配置
        :param request_body: DeepSeek 请求体
        :yield: 已解析的 DeepSeek Chunk
        """

        try:
            async with self._http_client.stream(
                "POST",
                self._completion_url(model_config.api_base),
                headers=self._headers(model_config.api_key),
                json=request_body,
                timeout=model_config.timeout_ms / 1000,
            ) as response:
                self._raise_for_status(response)
                async for line in response.aiter_lines():
                    if not line.startswith("data:"):
                        continue
                    data = line[5:].strip()
                    if data == "[DONE]":
                        return
                    if data:
                        yield self._parse_chunk(data)
        except (RetryableAgentError, NonRetryableAgentError):
            raise
        except httpx.TimeoutException as error:
            raise RetryableAgentError(
                AgentErrorCode.MODEL_REQUEST_FAILED,
                "DeepSeek流式请求超时",
            ) from error
        except httpx.HTTPError as error:
            raise RetryableAgentError(
                AgentErrorCode.MODEL_REQUEST_FAILED,
                "DeepSeek流式请求网络失败",
            ) from error

    @staticmethod
    def _parse_chunk(data: str) -> _DeepSeekChunk:
        """
        解析单个 DeepSeek SSE 数据帧

        :param data: SSE data 字段
        :return: Provider 内部 Chunk
        """

        try:
            decoded_payload: object = json.loads(data)
            if not isinstance(decoded_payload, dict):
                raise TypeError("DeepSeek响应不是JSON对象")
            payload = cast(dict[str, object], decoded_payload)
            raw_choice_list = payload.get("choices")
            choice_list = (
                cast(list[object], raw_choice_list) if isinstance(raw_choice_list, list) else []
            )
            raw_usage = payload.get("usage")
            usage = cast(dict[str, object], raw_usage) if isinstance(raw_usage, dict) else {}
            if not choice_list:
                return _DeepSeekChunk(
                    input_tokens=DeepSeekChatModel._integer(usage.get("prompt_tokens")),
                    output_tokens=DeepSeekChatModel._integer(usage.get("completion_tokens")),
                )
            raw_choice = choice_list[0]
            if not isinstance(raw_choice, dict):
                raise TypeError("DeepSeek Choice不是JSON对象")
            choice = cast(dict[str, object], raw_choice)
            raw_delta_payload = choice.get("delta")
            delta_payload = (
                cast(dict[str, object], raw_delta_payload)
                if isinstance(raw_delta_payload, dict)
                else {}
            )
            raw_tool_call_list = delta_payload.get("tool_calls")
            tool_call_list = (
                cast(list[object], raw_tool_call_list)
                if isinstance(raw_tool_call_list, list)
                else []
            )
            return _DeepSeekChunk(
                content_delta=str(delta_payload.get("content") or ""),
                reasoning_delta=str(delta_payload.get("reasoning_content") or ""),
                finish_reason=DeepSeekChatModel._optional_string(choice.get("finish_reason")),
                tool_call_delta_list=tuple(
                    DeepSeekChatModel._parse_tool_call(tool_call, index)
                    for index, tool_call in enumerate(tool_call_list)
                ),
                input_tokens=DeepSeekChatModel._integer(usage.get("prompt_tokens")),
                output_tokens=DeepSeekChatModel._integer(usage.get("completion_tokens")),
            )
        except (KeyError, IndexError, TypeError, ValueError, json.JSONDecodeError) as error:
            raise NonRetryableAgentError(
                AgentErrorCode.MODEL_REQUEST_FAILED,
                "DeepSeek流式响应格式非法",
            ) from error

    @staticmethod
    def _parse_tool_call(value: object, fallback_index: int) -> ChatToolCallDelta:
        """
        解析单个工具调用增量

        :param value: DeepSeek 工具调用 JSON 值
        :param fallback_index: 缺少 index 时使用的顺序号
        :return: 类型安全的工具调用增量
        """

        if not isinstance(value, dict):
            raise TypeError("DeepSeek工具调用不是JSON对象")
        tool_call = cast(dict[str, object], value)
        raw_function = tool_call.get("function")
        function = cast(dict[str, object], raw_function) if isinstance(raw_function, dict) else {}
        return ChatToolCallDelta(
            index=DeepSeekChatModel._integer(tool_call.get("index"), fallback_index),
            call_id=str(tool_call.get("id") or ""),
            name=str(function.get("name") or ""),
            arguments_delta=str(function.get("arguments") or ""),
        )

    @staticmethod
    def _integer(value: object, default: int = 0) -> int:
        """
        读取 Provider JSON 中的整数

        :param value: 待解析 JSON 值
        :param default: 非整数时使用的默认值
        :return: 类型安全的整数
        """

        return value if isinstance(value, int) else default

    @staticmethod
    def _optional_string(value: object) -> str | None:
        """
        读取 Provider JSON 中的可选字符串

        :param value: 待解析 JSON 值
        :return: 字符串或空值
        """

        return value if isinstance(value, str) else None

    @staticmethod
    def _serialize_message(message: ChatMessage) -> dict[str, object]:
        """
        将领域消息转换为 DeepSeek 请求消息

        :param message: 领域聊天消息
        :return: DeepSeek 请求消息
        """

        payload: dict[str, object] = {"role": message.role, "content": message.content}
        if message.reasoning_content:
            payload["reasoning_content"] = message.reasoning_content
        if message.tool_call_list:
            payload["tool_calls"] = [
                {
                    "id": tool_call.call_id,
                    "type": "function",
                    "function": {
                        "name": tool_call.name,
                        "arguments": tool_call.arguments,
                    },
                }
                for tool_call in message.tool_call_list
            ]
        if message.tool_call_id:
            payload["tool_call_id"] = message.tool_call_id
        if message.name:
            payload["name"] = message.name
        return payload

    @classmethod
    def _raise_for_status(cls, response: httpx.Response) -> None:
        """
        将模型 HTTP 错误转换为明确异常

        :param response: 模型 HTTP 响应
        :return: 无返回值
        """

        if response.is_success:
            return
        message = f"DeepSeek请求失败, httpStatus={response.status_code}"
        if response.status_code in cls._RETRYABLE_STATUS_CODE_SET:
            raise RetryableAgentError(AgentErrorCode.MODEL_REQUEST_FAILED, message)
        raise NonRetryableAgentError(AgentErrorCode.MODEL_REQUEST_FAILED, message)

    @staticmethod
    def _completion_url(api_base: str) -> str:
        """
        构建模型补全接口地址

        :param api_base: 模型 API 基础地址
        :return: 模型补全接口地址
        """

        return f"{api_base.rstrip('/')}/chat/completions"

    @staticmethod
    def _headers(api_key: str) -> dict[str, str]:
        """
        构建模型请求认证头

        :param api_key: 模型 API 密钥
        :return: 模型请求头
        """

        return {
            "Authorization": f"Bearer {api_key}",
            "Content-Type": "application/json",
            "Accept": "text/event-stream",
        }
