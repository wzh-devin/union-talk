"""会话消息检索片段领域模型。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/08/12 23:48
"""

from dataclasses import dataclass, field
from datetime import datetime

from union_talk_agent.knowledge.domain.enums import VectorStatus


@dataclass(slots=True)
class MessageSegment:
    """可独立失效和重建的单消息检索片段。"""

    segment_id: int
    conversation_id: int
    message_id: int
    sender_id: int
    sender_display_name: str
    text_content: str
    occurred_at: datetime
    source_revision: int
    embedding_model_version: str
    source_asset_version_map: dict[int, int] = field(default_factory=lambda: dict[int, int]())
    vector_status: VectorStatus = VectorStatus.PENDING

    @property
    def source_asset_file_id_list(self) -> list[int]:
        """
        返回消息片段引用的资产文件 ID 列表

        :return: 已排序且去重的资产文件 ID 列表
        """

        return sorted(self.source_asset_version_map)

    def references_asset(self, asset_file_id: int) -> bool:
        """
        判断消息片段是否引用指定资产

        :param asset_file_id: 待判断资产文件 ID
        :return: 是否存在该资产血缘
        """

        return asset_file_id in self.source_asset_version_map

    @property
    def token_count(self) -> int:
        """
        估算当前片段的 Token 数

        :return: 至少为一的估算 Token 数
        """

        return max((len(self.text_content) + 3) // 4, 1)
