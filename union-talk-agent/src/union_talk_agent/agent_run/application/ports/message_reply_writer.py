"""正式 Agent 回复写回端口。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/29 18:50
"""

from typing import Protocol

from union_talk_agent.agent_run.domain.chat_model import AgentReplyResult
from union_talk_agent.agent_run.domain.commands import CreateAgentReplyCommand


class MessageReplyWriter(Protocol):
    """通过 Message Service 创建正式回复。"""

    async def create_agent_reply(
        self,
        command: CreateAgentReplyCommand,
    ) -> AgentReplyResult:
        """
        幂等创建正式 Agent 回复

        :param command: Agent 回复写回命令
        :return: 正式消息 ID 与创建标记
        """

        ...
