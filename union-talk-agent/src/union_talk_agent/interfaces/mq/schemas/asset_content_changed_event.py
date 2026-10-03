"""File Service 资产内容变更事件契约。

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
from union_talk_agent.knowledge.application.resource_ingestion_service import (
    IndexResourceCommand,
)


class AssetContentChangedEvent(BaseModel):
    """资产内容已经可供索引的稳定事件。"""

    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)

    event_id: str = Field(min_length=1, max_length=64)
    event_type: Literal["ASSET_CONTENT_CHANGED"]
    schema_version: Literal[1]
    asset_file_id: int = Field(gt=0)
    conversation_id: int = Field(gt=0)
    resource_version: int = Field(gt=0)
    occurred_at: EventDatetime

    def to_command(self) -> IndexResourceCommand:
        """
        转换为资源入库命令

        :return: 资源入库命令
        """

        return IndexResourceCommand(
            event_id=self.event_id,
            asset_file_id=self.asset_file_id,
            conversation_id=self.conversation_id,
            resource_version=self.resource_version,
            occurred_at=self.occurred_at,
        )
