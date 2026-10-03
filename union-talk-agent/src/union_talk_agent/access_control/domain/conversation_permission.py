"""当前会话Agent权限。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/31 13:45
"""

from dataclasses import dataclass

from union_talk_agent.access_control.domain.enums import ConversationRole, ConversationType


@dataclass(frozen=True, slots=True)
class ConversationPermission:
    """Message Service 返回的当前会话权限。"""

    conversation_id: int
    user_id: int
    is_member: bool
    role: ConversationRole | None
    conversation_type: ConversationType | None
    group_id: int | None

    @property
    def can_manage_agent_config(self) -> bool:
        """
        判断当前用户是否可以管理会话Agent配置

        :return: 是否具有私聊成员、群主或管理员权限
        """

        if not self.is_member or self.conversation_type is None:
            return False
        if self.conversation_type is ConversationType.PRIVATE:
            return True
        return self.role in {ConversationRole.OWNER, ConversationRole.ADMIN}

    @property
    def can_replace_agent_credential(self) -> bool:
        """
        判断当前用户是否可以显式替换会话 Agent 凭证

        :return: 私聊成员、群主或管理员是否可以提交新凭证
        """

        return self.can_manage_agent_config

    @property
    def can_manage_agent_run(self) -> bool:
        """
        判断当前用户是否可以管理其他成员发起的Agent Run

        :return: 是否具有群主或管理员权限
        """

        return self.is_member and self.role in {
            ConversationRole.OWNER,
            ConversationRole.ADMIN,
        }
