"""会话 Agent 控制面上下文授权测试。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/08/07 19:05
"""

from union_talk_agent.access_control.domain.conversation_permission import (
    ConversationPermission,
)
from union_talk_agent.access_control.domain.enums import ConversationRole, ConversationType
from union_talk_agent.agent_config.application.agent_config_service import (
    build_agent_context,
)
from union_talk_agent.agent_config.domain.agent_config import (
    AgentControlPlaneView,
    AgentDefinition,
    AgentDefinitionVersion,
    ConversationAgentBinding,
    ProviderCredential,
    ProviderCredentialVersion,
)
from union_talk_agent.agent_config.domain.enums import (
    AgentBindingStatus,
    AgentContextState,
    AgentDefinitionStatus,
    AgentOwnerType,
    ConnectionTestStatus,
    CredentialStatus,
    CredentialUsageScopeType,
    ProviderType,
)
from union_talk_agent.interfaces.http.schemas.agent_config_response import (
    AgentContextResponse,
)


def build_control_plane_view(credential_owner_user_id: int) -> AgentControlPlaneView:
    """
    构造凭证可用的会话 Agent 控制面聚合

    :param credential_owner_user_id: 当前凭证所有者用户 ID
    :return: 可直接构造前端上下文的控制面聚合
    """

    return AgentControlPlaneView(
        binding=ConversationAgentBinding(
            binding_id=101,
            conversation_id=1,
            agent_id=201,
            agent_version=3,
            credential_id=301,
            credential_version=2,
            status=AgentBindingStatus.ACTIVE,
            binding_version=5,
            updated_by=credential_owner_user_id,
        ),
        definition=AgentDefinition(
            agent_id=201,
            owner_type=AgentOwnerType.CONVERSATION,
            owner_id=1,
            display_name="AI 助手",
            status=AgentDefinitionStatus.ACTIVE,
            latest_version=3,
        ),
        definition_version=AgentDefinitionVersion(
            agent_id=201,
            version=3,
            model_id="deepseek-chat",
            timeout_ms=60_000,
            max_retries=2,
            system_prompt="仅授权管理者可见的系统指令",
            is_history_enabled=True,
            is_resource_enabled=True,
            max_context_tokens=32_000,
            max_output_tokens=4096,
            recent_message_tokens=6000,
            message_top_k=20,
            resource_top_k=30,
            temperature=0.2,
            thinking_enabled=True,
            thinking_effort="medium",
        ),
        credential=ProviderCredential(
            credential_id=301,
            owner_user_id=credential_owner_user_id,
            usage_scope_type=CredentialUsageScopeType.CONVERSATION,
            usage_scope_id=1,
            provider=ProviderType.DEEPSEEK,
            status=CredentialStatus.ACTIVE,
            latest_version=2,
        ),
        credential_version=ProviderCredentialVersion(
            credential_id=301,
            version=2,
            api_base="https://api.deepseek.com",
            api_key_ciphertext=b"ciphertext",
            api_key_nonce=b"nonce",
            key_fingerprint="sk-****1234",
            connection_test_status=ConnectionTestStatus.SUCCEEDED,
            connection_test_error=None,
            tested_at=None,
        ),
    )


def build_private_permission(user_id: int, is_member: bool = True) -> ConversationPermission:
    """
    构造私聊成员权限事实

    :param user_id: 当前用户 ID
    :param is_member: 是否为有效会话成员
    :return: 私聊会话权限事实
    """

    return ConversationPermission(
        conversation_id=1,
        user_id=user_id,
        is_member=is_member,
        role=ConversationRole.MEMBER,
        conversation_type=ConversationType.PRIVATE,
        group_id=None,
    )


def build_group_member_permission(user_id: int) -> ConversationPermission:
    """
    构造群聊普通成员权限事实

    :param user_id: 当前用户 ID
    :return: 群聊普通成员权限事实
    """

    return ConversationPermission(
        conversation_id=1,
        user_id=user_id,
        is_member=True,
        role=ConversationRole.MEMBER,
        conversation_type=ConversationType.GROUP,
        group_id=100,
    )


def test_private_members_share_agent_but_only_sponsor_manages_saved_key() -> None:
    """
    验证私聊双方共享 Agent 且只有凭证赞助者可管理已保存 Key

    :return: 无返回值
    """

    view = build_control_plane_view(credential_owner_user_id=10)

    sponsor_context = build_agent_context(
        1,
        build_private_permission(user_id=10),
        view,
    )
    peer_context = build_agent_context(
        1,
        build_private_permission(user_id=20),
        view,
    )

    assert sponsor_context.state is AgentContextState.READY
    assert sponsor_context.can_invoke is True
    assert sponsor_context.can_manage_definition is True
    assert sponsor_context.can_manage_current_credential is True
    assert sponsor_context.can_replace_credential is True
    assert peer_context.state is AgentContextState.READY
    assert peer_context.can_invoke is True
    assert peer_context.can_manage_definition is True
    assert peer_context.can_manage_current_credential is False
    assert peer_context.can_replace_credential is True


def test_removed_private_member_receives_no_agent_capability() -> None:
    """
    验证退出私聊会话的旧凭证所有者不会继续获得管理能力

    :return: 无返回值
    """

    context = build_agent_context(
        1,
        build_private_permission(user_id=10, is_member=False),
        build_control_plane_view(credential_owner_user_id=10),
    )

    assert context.can_view is False
    assert context.can_invoke is False
    assert context.can_manage_definition is False
    assert context.can_manage_current_credential is False
    assert context.can_replace_credential is False


def test_group_member_response_masks_system_prompt() -> None:
    """
    验证群聊普通成员的上下文响应不会暴露系统指令

    :return: 无返回值
    """

    context = build_agent_context(
        1,
        build_group_member_permission(user_id=20),
        build_control_plane_view(credential_owner_user_id=10),
    )

    response = AgentContextResponse.from_view(context)

    assert context.can_invoke is True
    assert context.can_manage_definition is False
    assert response.agent is not None
    assert response.agent.system_prompt == ""


def test_private_member_response_keeps_shared_system_prompt() -> None:
    """
    验证私聊成员可读取双方共同维护的系统指令

    :return: 无返回值
    """

    context = build_agent_context(
        1,
        build_private_permission(user_id=20),
        build_control_plane_view(credential_owner_user_id=10),
    )

    response = AgentContextResponse.from_view(context)

    assert context.can_manage_definition is True
    assert response.agent is not None
    assert response.agent.system_prompt == "仅授权管理者可见的系统指令"
