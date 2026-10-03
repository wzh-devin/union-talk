"""会话上下文读取端口。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/29 18:50
"""

from typing import Protocol

from union_talk_agent.agent_run.domain.conversation_context import ConversationContext


class ConversationReader(Protocol):
    """从 Message Service 读取 Agent 会话上下文。"""

    async def get_agent_context(
        self,
        conversation_id: int,
        trigger_message_id: int,
        recent_message_limit: int,
    ) -> ConversationContext:
        """
        读取当前问题与近期消息

        :param conversation_id: 会话 ID
        :param trigger_message_id: 触发消息 ID
        :param recent_message_limit: 最大近期消息数量
        :return: 紧凑会话上下文
        """

        ...
