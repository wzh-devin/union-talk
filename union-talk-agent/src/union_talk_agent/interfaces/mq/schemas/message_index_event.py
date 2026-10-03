"""Message Service 消息索引事件契约。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/08/12 23:57
"""

from typing import Literal

from pydantic import BaseModel, ConfigDict, Field
from pydantic.alias_generators import to_camel

from union_talk_agent.interfaces.mq.schemas.event_datetime import EventDatetime
from union_talk_agent.knowledge.application.message_indexing_service import (
    IndexMessageCommand,
)


class SenderUserSnapshot(BaseModel):
    """消息发送者用户快照。"""

    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)

    user_id: int | None = None
    username: str = ""


class AgentCitationSnapshot(BaseModel):
    """Agent 回答引用快照。"""

    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)

    source_type: str = ""
    message_id: int | None = Field(default=None, gt=0)
    asset_file_id: int | None = Field(default=None, gt=0)
    resource_version: int | None = Field(default=None, gt=0)


class MessageIndexEvent(BaseModel):
    """消息创建、撤回和删除的统一索引事件。"""

    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)

    event_id: str = Field(min_length=1, max_length=64)
    event_type: Literal["MESSAGE_CREATED", "MESSAGE_RECALLED", "MESSAGE_DELETED"] = (
        "MESSAGE_CREATED"
    )
    schema_version: int = 1
    message_id: int = Field(gt=0)
    conversation_id: int = Field(gt=0)
    sender_id: int = Field(default=0, ge=0)
    sender_user: SenderUserSnapshot | None = None
    message_type: str = Field(default="TEXT", alias="type")
    content: str = ""
    citation_list: list[AgentCitationSnapshot] = Field(
        default_factory=lambda: list[AgentCitationSnapshot]()
    )
    recalled: bool = False
    source_revision: int = Field(default=1, gt=0)
    created_at: EventDatetime

    def to_command(self) -> IndexMessageCommand:
        """
        转换为消息索引命令

        :return: 消息索引命令
        """

        source_asset_version_map = {
            citation.asset_file_id: citation.resource_version
            for citation in self.citation_list
            if citation.source_type == "RESOURCE_CHUNK"
            and citation.asset_file_id is not None
            and citation.resource_version is not None
        }
        return IndexMessageCommand(
            event_id=self.event_id,
            event_type=self.event_type,
            message_id=self.message_id,
            conversation_id=self.conversation_id,
            sender_id=self.sender_id,
            sender_display_name=(self.sender_user.username if self.sender_user else ""),
            message_type=self.message_type,
            content=self.content,
            occurred_at=self.created_at,
            recalled=self.recalled,
            source_revision=self.source_revision,
            source_asset_version_map=source_asset_version_map,
            source_message_id_list=sorted(
                {
                    citation.message_id
                    for citation in self.citation_list
                    if citation.source_type == "MESSAGE_SEGMENT" and citation.message_id is not None
                }
            ),
        )
