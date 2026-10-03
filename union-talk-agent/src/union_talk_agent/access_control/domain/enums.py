"""Agent访问控制枚举。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/31 13:45
"""

from enum import StrEnum


class ConversationRole(StrEnum):
    """会话成员角色。"""

    OWNER = "OWNER"
    ADMIN = "ADMIN"
    MEMBER = "MEMBER"


class ConversationType(StrEnum):
    """会话类型。"""

    PRIVATE = "PRIVATE"
    GROUP = "GROUP"
