"""Inbox 状态枚举。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/29 18:46
"""

from enum import StrEnum


class InboxStatus(StrEnum):
    """事件消费状态。"""

    PROCESSING = "PROCESSING"
    SUCCEEDED = "SUCCEEDED"
    FAILED = "FAILED"
