"""可信网关身份和会话权限校验。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/29 19:05
"""

from dataclasses import dataclass

from fastapi import Header, HTTPException, status

from union_talk_agent.access_control.application.ports.conversation_permission_reader import (
    ConversationPermissionReader,
)
from union_talk_agent.access_control.domain.conversation_permission import (
    ConversationPermission,
)


@dataclass(frozen=True, slots=True)
class RequestPrincipal:
    """Java Gateway 已校验的当前用户身份。"""

    user_id: int


def require_authenticated_user(
    x_user_id: str = Header(alias="X-User-Id"),
) -> RequestPrincipal:
    """
    读取 Java Gateway 覆盖写入的当前用户 ID

    :param x_user_id: Gateway 校验登录后写入的用户 ID
    :return: 已解析的当前请求身份
    """

    try:
        user_id = int(x_user_id)
    except (ValueError, TypeError) as error:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="无效的登录身份",
        ) from error
    if user_id <= 0:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="无效的登录身份",
        )
    return RequestPrincipal(user_id=user_id)


async def require_conversation_member(
    permission_reader: ConversationPermissionReader,
    principal: RequestPrincipal,
    conversation_id: int,
) -> ConversationPermission:
    """
    通过 Message Service 校验当前会话成员身份

    :param permission_reader: 会话权限读取端口
    :param principal: 当前请求身份
    :param conversation_id: 待访问的会话 ID
    :return: 已校验的当前会话权限
    """

    permission = await permission_reader.get_conversation_permission(
        conversation_id,
        principal.user_id,
    )
    if not permission.is_member:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="无权访问该会话Agent资源",
        )
    return permission


async def require_conversation_agent_manager(
    permission_reader: ConversationPermissionReader,
    principal: RequestPrincipal,
    conversation_id: int,
) -> ConversationPermission:
    """
    通过 Message Service 校验会话Agent管理权限

    :param permission_reader: 会话权限读取端口
    :param principal: 当前请求身份
    :param conversation_id: 待管理的会话 ID
    :return: 已校验的当前会话Agent管理权限
    """

    permission = await require_conversation_member(
        permission_reader,
        principal,
        conversation_id,
    )
    if not permission.can_manage_agent_config:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="当前用户无权管理会话Agent",
        )
    return permission
