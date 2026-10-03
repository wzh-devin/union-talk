"""聊天模型端口。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/29 18:50
"""

from collections.abc import AsyncIterator
from typing import Protocol

from union_talk_agent.agent_config.domain.model_config import RuntimeModelConfig
from union_talk_agent.agent_run.domain.chat_model import (
    ChatMessage,
    ChatModelEvent,
    ChatToolDefinition,
)


class ChatModel(Protocol):
    """支持 SSE 增量输出的聊天模型。"""

    def stream(
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
        流式生成回答

        :param model_config: 模型连接和重试配置
        :param message_list: 模型输入消息列表
        :param max_output_tokens: 最大输出 Token 数
        :param temperature: 模型温度
        :param thinking_enabled: 是否启用模型 Thinking
        :param thinking_effort: 模型 Thinking 强度
        :param tool_list: 当前回合允许模型选择的工具列表
        :yield: 模型 Provider 判别联合事件
        """

        ...

    async def test_connection(self, model_config: RuntimeModelConfig) -> None:
        """
        测试模型连接

        :param model_config: 模型连接配置
        :return: 无返回值
        """

        ...
