"""MVP 可靠链路固定回答模型。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/29 18:58
"""

from collections.abc import AsyncIterator

from union_talk_agent.agent_config.domain.model_config import RuntimeModelConfig
from union_talk_agent.agent_run.domain.chat_model import (
    ChatMessage,
    ChatModelEvent,
    ChatToolDefinition,
)
from union_talk_agent.agent_run.domain.enums import ProviderEventType


class FixedAnswerChatModel:
    """不访问外部模型，用于端到端链路联调。"""

    def __init__(self, answer_text: str) -> None:
        """
        初始化 FixedAnswerChatModel

        :param answer_text: 完整回答文本
        :return: 无返回值
        """

        self._answer_text = answer_text

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
        输出单个固定文本增量

        :param model_config: 运行时模型配置
        :param message_list: 模型消息列表
        :param max_output_tokens: 最大输出 Token 数
        :param temperature: 模型采样温度
        :param thinking_enabled: 是否启用 Thinking
        :param thinking_effort: Thinking 强度
        :param tool_list: 当前回合允许模型选择的工具列表
        :yield: Provider 判别联合事件
        """

        _ignored_arguments = (
            model_config,
            message_list,
            max_output_tokens,
            temperature,
            thinking_enabled,
            thinking_effort,
            tool_list,
        )
        yield ChatModelEvent(event_type=ProviderEventType.START)
        yield ChatModelEvent(event_type=ProviderEventType.TEXT_START, content_index=0)
        yield ChatModelEvent(
            event_type=ProviderEventType.TEXT_DELTA,
            content_index=0,
            delta=self._answer_text,
        )
        yield ChatModelEvent(event_type=ProviderEventType.TEXT_END, content_index=0)
        yield ChatModelEvent(event_type=ProviderEventType.DONE, finish_reason="stop")

    @staticmethod
    async def test_connection(model_config: RuntimeModelConfig) -> None:
        """
        固定回答不需要连接测试

        :param model_config: 运行时模型配置
        :return: 无返回值
        """

        _ignored_model_config = model_config
