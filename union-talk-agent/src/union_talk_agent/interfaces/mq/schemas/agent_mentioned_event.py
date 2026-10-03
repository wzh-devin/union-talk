"""Message Service AGENT_MENTIONED 事件契约。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/29 19:07
"""

from typing import Literal

from pydantic import BaseModel, ConfigDict, Field
from pydantic.alias_generators import to_camel

from union_talk_agent.agent_run.domain.commands import StartAgentRunCommand
from union_talk_agent.interfaces.mq.schemas.event_datetime import EventDatetime


class AgentMentionedEvent(BaseModel):
    """与当前 Java `AgentMentionedEvent` 保持字段兼容。"""

    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)

    event_id: str = Field(min_length=1, max_length=64)
    event_type: Literal["AGENT_MENTIONED"]
    schema_version: Literal[3]
    trigger_message_id: int = Field(gt=0)
    conversation_id: int = Field(gt=0)
    agent_id: int = Field(gt=0)
    requester_user_id: int = Field(gt=0)
    requester_display_name: str = Field(default="", max_length=128)
    message_type: Literal["TEXT"]
    occurred_at: EventDatetime

    def to_command(self) -> StartAgentRunCommand:
        """
        转换为不依赖 MQ 的应用命令

        :return: Agent 提及应用命令
        """

        return StartAgentRunCommand(
            event_id=self.event_id,
            trigger_message_id=self.trigger_message_id,
            conversation_id=self.conversation_id,
            agent_id=self.agent_id,
            requester_user_id=self.requester_user_id,
            requester_display_name=self.requester_display_name,
            message_type=self.message_type,
        )
