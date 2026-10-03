"""Agent Run 不可变命令。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/29 18:46
"""

from dataclasses import dataclass, field

from union_talk_agent.agent_run.domain.agent_citation import AgentCitation
from union_talk_agent.agent_run.domain.constants import (
    MAX_ANSWER_LENGTH,
    MAX_CITATION_COUNT,
    MAX_MODEL_ID_LENGTH,
)
from union_talk_agent.agent_run.domain.exceptions import AgentDomainError, AgentErrorCode


@dataclass(frozen=True, slots=True)
class StartAgentRunCommand:
    """启动 Agent Run 命令。"""

    event_id: str
    trigger_message_id: int
    conversation_id: int
    agent_id: int
    requester_user_id: int
    requester_display_name: str
    message_type: str


@dataclass(frozen=True, slots=True)
class CreateAgentReplyCommand:
    """创建正式 Agent 回复命令。"""

    run_id: int
    conversation_id: int
    trigger_message_id: int
    reply_to_user_id: int
    agent_id: int
    model_id: str
    content: str
    citation_list: list[AgentCitation] = field(default_factory=lambda: list[AgentCitation]())

    def __post_init__(self) -> None:
        """
        校验写入 Message Service 前的领域边界

        :return: 无返回值
        """

        if (
            min(
                self.run_id,
                self.conversation_id,
                self.trigger_message_id,
                self.reply_to_user_id,
                self.agent_id,
            )
            <= 0
        ):
            raise AgentDomainError(
                AgentErrorCode.REQUEST_INVALID,
                "Agent回复关联ID非法",
            )
        if not 1 <= len(self.model_id) <= MAX_MODEL_ID_LENGTH:
            raise AgentDomainError(
                AgentErrorCode.REQUEST_INVALID,
                "Agent回复模型标识长度非法",
            )
        if not self.content.strip() or len(self.content) > MAX_ANSWER_LENGTH:
            raise AgentDomainError(
                AgentErrorCode.REQUEST_INVALID,
                "Agent回复正文长度非法",
            )
        if len(self.citation_list) > MAX_CITATION_COUNT:
            raise AgentDomainError(
                AgentErrorCode.REQUEST_INVALID,
                "Agent回复引用数量超限",
            )


@dataclass(frozen=True, slots=True)
class CancelAgentRunCommand:
    """取消 Agent Run 命令。"""

    run_id: int
    requested_by: int
