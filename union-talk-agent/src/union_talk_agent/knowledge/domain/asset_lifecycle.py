"""资产生命周期领域模型。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/08/13 17:32
"""

from dataclasses import dataclass
from datetime import datetime

from union_talk_agent.knowledge.domain.enums import AssetLifecycleStatus


@dataclass(frozen=True, slots=True)
class AssetLifecycle:
    """File Service 资产最新生命周期事实。"""

    asset_file_id: int
    conversation_id: int
    resource_version: int
    status: AssetLifecycleStatus
    event_id: str
    occurred_at: datetime

    def accepts_content(self, resource_version: int, occurred_at: datetime) -> bool:
        """
        判断内容事件能否覆盖当前生命周期事实

        :param resource_version: 内容事件资源版本
        :param occurred_at: 内容事件发生时间
        :return: 内容事件是否仍然有效
        """

        if resource_version > self.resource_version:
            return True
        if resource_version < self.resource_version:
            return False
        return self.status is AssetLifecycleStatus.ACTIVE and occurred_at >= self.occurred_at

    def accepts_deletion(self, resource_version: int, occurred_at: datetime) -> bool:
        """
        判断删除事件能否覆盖当前生命周期事实

        :param resource_version: 删除事件资源版本
        :param occurred_at: 删除事件发生时间
        :return: 删除事件是否仍然有效
        """

        return resource_version > self.resource_version or (
            resource_version == self.resource_version and occurred_at >= self.occurred_at
        )
