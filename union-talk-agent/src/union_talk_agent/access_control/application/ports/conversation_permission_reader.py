"""会话Agent权限读取端口。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/31 13:45
"""

from typing import Protocol

from union_talk_agent.access_control.domain.conversation_permission import (
    ConversationPermission,
)


class ConversationPermissionReader(Protocol):
    """从 Message Service 读取当前会话权限。"""

    async def get_conversation_permission(
        self,
        conversation_id: int,
        user_id: int,
    ) -> ConversationPermission:
        """
        查询用户当前的会话成员和角色信息

        :param conversation_id: 会话 ID
        :param user_id: 当前登录用户 ID
        :return: Message Service 返回的会话权限
        """

        raise NotImplementedError
