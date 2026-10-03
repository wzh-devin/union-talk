"""会话Agent权限规则测试。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/08/07 16:54
"""

import pytest
from fastapi import HTTPException

from union_talk_agent.access_control.domain.conversation_permission import (
    ConversationPermission,
)
from union_talk_agent.access_control.domain.enums import ConversationRole, ConversationType
from union_talk_agent.interfaces.http.request_principal import (
    RequestPrincipal,
    require_conversation_agent_manager,
)


class StaticConversationPermissionReader:
    """返回固定会话权限的测试读取器。"""

    def __init__(self, permission: ConversationPermission) -> None:
        """
        初始化固定权限读取器

        :param permission: 待返回的会话权限
        :return: 无返回值
        """

        self._permission = permission

    async def get_conversation_permission(
        self,
        conversation_id: int,
        user_id: int,
    ) -> ConversationPermission:
        """
        返回固定的会话权限

        :param conversation_id: 会话 ID
        :param user_id: 当前用户 ID
        :return: 固定会话权限
        """

        return self._permission


def test_private_member_can_manage_agent() -> None:
    """
    验证有效私聊成员可以管理会话Agent配置

    :return: 无返回值
    """

    permission = ConversationPermission(
        conversation_id=1,
        user_id=2,
        is_member=True,
        role=ConversationRole.MEMBER,
        conversation_type=ConversationType.PRIVATE,
        group_id=None,
    )

    assert permission.can_manage_agent_config is True
    assert permission.can_manage_agent_run is False


def test_group_permission_keeps_manager_boundary() -> None:
    """
    验证群主和管理员可管理而普通群成员不可管理

    :return: 无返回值
    """

    owner_permission = ConversationPermission(
        1, 2, True, ConversationRole.OWNER, ConversationType.GROUP, 10
    )
    admin_permission = ConversationPermission(
        1, 3, True, ConversationRole.ADMIN, ConversationType.GROUP, 10
    )
    member_permission = ConversationPermission(
        1, 4, True, ConversationRole.MEMBER, ConversationType.GROUP, 10
    )

    assert owner_permission.can_manage_agent_config is True
    assert owner_permission.can_manage_agent_run is True
    assert admin_permission.can_manage_agent_config is True
    assert admin_permission.can_manage_agent_run is True
    assert member_permission.can_manage_agent_config is False
    assert member_permission.can_manage_agent_run is False


def test_non_member_cannot_manage_agent() -> None:
    """
    验证非会话成员不能管理Agent配置

    :return: 无返回值
    """

    permission = ConversationPermission(
        conversation_id=1,
        user_id=2,
        is_member=False,
        role=ConversationRole.MEMBER,
        conversation_type=ConversationType.PRIVATE,
        group_id=None,
    )

    assert permission.can_manage_agent_config is False
    assert permission.can_manage_agent_run is False


async def test_private_member_passes_agent_config_route_permission() -> None:
    """
    验证私聊成员通过Agent配置接口权限校验

    :return: 无返回值
    """

    permission = ConversationPermission(
        1, 2, True, ConversationRole.MEMBER, ConversationType.PRIVATE, None
    )
    reader = StaticConversationPermissionReader(permission)

    actual_permission = await require_conversation_agent_manager(
        reader,
        RequestPrincipal(user_id=2),
        1,
    )

    assert actual_permission is permission


async def test_group_member_fails_agent_config_route_permission() -> None:
    """
    验证普通群成员无法通过Agent配置接口权限校验

    :return: 无返回值
    """

    permission = ConversationPermission(
        1, 2, True, ConversationRole.MEMBER, ConversationType.GROUP, 10
    )
    reader = StaticConversationPermissionReader(permission)

    with pytest.raises(HTTPException) as error_info:
        await require_conversation_agent_manager(
            reader,
            RequestPrincipal(user_id=2),
            1,
        )

    assert error_info.value.status_code == 403
