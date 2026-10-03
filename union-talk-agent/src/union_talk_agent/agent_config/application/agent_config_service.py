"""会话 Agent 控制面应用服务。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/08/07 18:45
"""

from dataclasses import dataclass
from datetime import UTC, datetime

from union_talk_agent.access_control.domain.conversation_permission import (
    ConversationPermission,
)
from union_talk_agent.agent_config.domain.agent_config import (
    AgentContextView,
    AgentControlPlaneView,
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
from union_talk_agent.agent_config.domain.model_config import RuntimeModelConfig
from union_talk_agent.agent_run.application.ports.agent_unit_of_work import (
    AgentDefinitionVersionWrite,
    AgentDefinitionWrite,
    AgentUnitOfWork,
    AgentUnitOfWorkFactory,
    ConversationAgentBindingWrite,
    ProviderCredentialVersionWrite,
    ProviderCredentialWrite,
)
from union_talk_agent.agent_run.application.ports.chat_model import ChatModel
from union_talk_agent.agent_run.application.ports.id_generator import IdGenerator
from union_talk_agent.agent_run.domain.exceptions import AgentDomainError, AgentErrorCode
from union_talk_agent.infrastructure.crypto.api_key_cipher import ApiKeyCipher


@dataclass(frozen=True, slots=True)
class SaveAgentDefinitionCommand:
    """保存 Agent 定义版本所需字段。"""

    conversation_id: int
    updated_by: int
    agent_version: int
    binding_version: int
    display_name: str
    model_id: str
    timeout_ms: int
    max_retries: int
    system_prompt: str
    is_enabled: bool
    is_history_enabled: bool
    is_resource_enabled: bool
    max_context_tokens: int
    max_output_tokens: int
    recent_message_tokens: int
    message_top_k: int
    resource_top_k: int
    temperature: float
    thinking_enabled: bool
    thinking_effort: str


@dataclass(frozen=True, slots=True)
class ReplaceAgentCredentialCommand:
    """测试并替换会话当前 Provider 凭证所需字段。"""

    conversation_id: int
    updated_by: int
    binding_version: int
    api_base: str
    api_key: str


def build_agent_context(
    conversation_id: int,
    permission: ConversationPermission,
    view: AgentControlPlaneView | None,
) -> AgentContextView:
    """
    根据实时权限和控制面聚合构造前端上下文

    :param conversation_id: 会话 ID
    :param permission: Message Service 返回的会话权限事实
    :param view: 当前 Agent 控制面聚合
    :return: 会话 Agent 上下文
    """

    conversation_type = (
        str(permission.conversation_type) if permission.conversation_type is not None else ""
    )
    if view is None:
        state = AgentContextState.NOT_CONFIGURED
        can_invoke = False
        can_manage_current_credential = False
    else:
        state_map = {
            AgentBindingStatus.ACTIVE: AgentContextState.READY,
            AgentBindingStatus.DISABLED: AgentContextState.DISABLED,
            AgentBindingStatus.CREDENTIAL_UNAVAILABLE: (AgentContextState.CREDENTIAL_UNAVAILABLE),
            AgentBindingStatus.ARCHIVED: AgentContextState.DISABLED,
        }
        state = state_map[view.binding.status]
        can_invoke = state is AgentContextState.READY
        can_manage_current_credential = (
            permission.is_member
            and view.credential is not None
            and view.credential.owner_user_id == permission.user_id
        )
    return AgentContextView(
        conversation_id=conversation_id,
        conversation_type=conversation_type,
        state=state,
        control_plane=view,
        can_view=permission.is_member,
        can_invoke=permission.is_member and can_invoke,
        can_manage_definition=permission.can_manage_agent_config,
        can_manage_current_credential=can_manage_current_credential,
        can_replace_credential=permission.can_replace_agent_credential,
    )


class AgentControlPlaneService:
    """管理 Agent 定义、凭证所有权和会话绑定。"""

    def __init__(
        self,
        *,
        unit_of_work_factory: AgentUnitOfWorkFactory,
        id_generator: IdGenerator,
        api_key_cipher: ApiKeyCipher,
        deepseek_chat_model: ChatModel,
    ) -> None:
        """
        初始化 AgentControlPlaneService

        :param unit_of_work_factory: Agent 工作单元工厂
        :param id_generator: 业务 ID 生成器
        :param api_key_cipher: API Key 加解密组件
        :param deepseek_chat_model: DeepSeek 聊天模型客户端
        :return: 无返回值
        """

        self._unit_of_work_factory = unit_of_work_factory
        self._id_generator = id_generator
        self._api_key_cipher = api_key_cipher
        self._deepseek_chat_model = deepseek_chat_model

    async def get_context(
        self,
        conversation_id: int,
        permission: ConversationPermission,
    ) -> AgentContextView:
        """
        查询包含服务端能力判断的会话 Agent 上下文

        :param conversation_id: 会话 ID
        :param permission: Message Service 返回的会话权限事实
        :return: 会话 Agent 上下文
        """

        async with self._unit_of_work_factory() as unit_of_work:
            view = await unit_of_work.agent_control_plane.get_view(conversation_id)
        return build_agent_context(conversation_id, permission, view)

    async def save_definition(
        self,
        command: SaveAgentDefinitionCommand,
        permission: ConversationPermission,
    ) -> AgentContextView:
        """
        新增或版本化更新 Agent 定义且不改变凭证所有权

        :param command: Agent 定义保存命令
        :param permission: Message Service 返回的会话权限事实
        :return: 保存后的会话 Agent 上下文
        """

        if not permission.can_manage_agent_config:
            raise AgentDomainError(AgentErrorCode.PERMISSION_DENIED, "当前用户无权管理会话Agent")
        now = datetime.now(UTC)
        async with self._unit_of_work_factory() as unit_of_work:
            await unit_of_work.business_locks.lock(
                "agent-conversation-binding",
                str(command.conversation_id),
            )
            existing = await unit_of_work.agent_control_plane.get_view(command.conversation_id)
            if existing is None:
                if command.agent_version != 0 or command.binding_version != 0:
                    raise self._version_conflict()
                agent_id = self._id_generator.next_id()
                agent_version = 1
                binding_id = self._id_generator.next_id()
                binding_version = 1
                await unit_of_work.agent_control_plane.insert_definition(
                    AgentDefinitionWrite(
                        agent_id=agent_id,
                        owner_type=str(AgentOwnerType.CONVERSATION),
                        owner_id=command.conversation_id,
                        display_name=command.display_name,
                        status=str(
                            AgentDefinitionStatus.ACTIVE
                            if command.is_enabled
                            else AgentDefinitionStatus.DISABLED
                        ),
                        latest_version=agent_version,
                        updated_by=command.updated_by,
                        now=now,
                    )
                )
                await self._insert_definition_version(
                    unit_of_work,
                    command,
                    agent_id,
                    agent_version,
                    now,
                )
                await unit_of_work.agent_control_plane.insert_binding(
                    ConversationAgentBindingWrite(
                        binding_id=binding_id,
                        conversation_id=command.conversation_id,
                        agent_id=agent_id,
                        agent_version=agent_version,
                        credential_id=None,
                        credential_version=None,
                        status=str(
                            AgentBindingStatus.CREDENTIAL_UNAVAILABLE
                            if command.is_enabled
                            else AgentBindingStatus.DISABLED
                        ),
                        binding_version=binding_version,
                        updated_by=command.updated_by,
                        now=now,
                    )
                )
            else:
                if (
                    existing.definition_version.version != command.agent_version
                    or existing.binding.binding_version != command.binding_version
                ):
                    raise self._version_conflict()
                agent_id = existing.definition.agent_id
                agent_version = existing.definition.latest_version + 1
                binding_version = existing.binding.binding_version + 1
                await unit_of_work.agent_control_plane.update_definition(
                    AgentDefinitionWrite(
                        agent_id=agent_id,
                        owner_type=str(existing.definition.owner_type),
                        owner_id=existing.definition.owner_id,
                        display_name=command.display_name,
                        status=str(
                            AgentDefinitionStatus.ACTIVE
                            if command.is_enabled
                            else AgentDefinitionStatus.DISABLED
                        ),
                        latest_version=agent_version,
                        updated_by=command.updated_by,
                        now=now,
                    )
                )
                await self._insert_definition_version(
                    unit_of_work,
                    command,
                    agent_id,
                    agent_version,
                    now,
                )
                has_active_credential = (
                    existing.credential is not None
                    and existing.credential.status is CredentialStatus.ACTIVE
                    and existing.credential_version is not None
                    and existing.credential_version.connection_test_status
                    is ConnectionTestStatus.SUCCEEDED
                )
                await unit_of_work.agent_control_plane.update_binding(
                    ConversationAgentBindingWrite(
                        binding_id=existing.binding.binding_id,
                        conversation_id=command.conversation_id,
                        agent_id=agent_id,
                        agent_version=agent_version,
                        credential_id=existing.binding.credential_id,
                        credential_version=existing.binding.credential_version,
                        status=str(
                            AgentBindingStatus.DISABLED
                            if not command.is_enabled
                            else (
                                AgentBindingStatus.ACTIVE
                                if has_active_credential
                                else AgentBindingStatus.CREDENTIAL_UNAVAILABLE
                            )
                        ),
                        binding_version=binding_version,
                        updated_by=command.updated_by,
                        now=now,
                    )
                )
            await unit_of_work.commit()
        return await self.get_context(command.conversation_id, permission)

    async def replace_credential(
        self,
        command: ReplaceAgentCredentialCommand,
        permission: ConversationPermission,
    ) -> AgentContextView:
        """
        测试完整 API Key 并在成功后替换当前会话凭证赞助者

        :param command: Provider 凭证替换命令
        :param permission: Message Service 返回的会话权限事实
        :return: 替换后的会话 Agent 上下文
        """

        if not permission.can_replace_agent_credential:
            raise AgentDomainError(
                AgentErrorCode.PERMISSION_DENIED,
                "当前用户无权替换会话Agent凭证",
            )
        async with self._unit_of_work_factory() as unit_of_work:
            existing = await unit_of_work.agent_control_plane.get_view(command.conversation_id)
        if existing is None:
            raise AgentDomainError(AgentErrorCode.CONFIG_NOT_FOUND, "请先保存Agent定义")
        runtime_model = RuntimeModelConfig(
            credential_id=0,
            credential_version=0,
            credential_owner_user_id=command.updated_by,
            provider=ProviderType.DEEPSEEK,
            api_base=command.api_base.rstrip("/"),
            api_key=command.api_key,
            model_id=existing.definition_version.model_id,
            timeout_ms=existing.definition_version.timeout_ms,
            max_retries=existing.definition_version.max_retries,
        )
        await self._deepseek_chat_model.test_connection(runtime_model)

        encrypted_key = self._api_key_cipher.encrypt(command.api_key)
        now = datetime.now(UTC)
        async with self._unit_of_work_factory() as unit_of_work:
            await unit_of_work.business_locks.lock(
                "agent-conversation-binding",
                str(command.conversation_id),
            )
            locked_view = await unit_of_work.agent_control_plane.get_view(command.conversation_id)
            if locked_view is None:
                raise AgentDomainError(AgentErrorCode.CONFIG_NOT_FOUND, "当前会话Agent定义不存在")
            if locked_view.binding.binding_version != command.binding_version:
                raise self._version_conflict()

            current_credential = locked_view.credential
            if (
                current_credential is not None
                and current_credential.owner_user_id == command.updated_by
            ):
                credential_id = current_credential.credential_id
                credential_version = current_credential.latest_version + 1
                await unit_of_work.agent_control_plane.update_credential(
                    ProviderCredentialWrite(
                        credential_id=credential_id,
                        owner_user_id=command.updated_by,
                        usage_scope_type=str(CredentialUsageScopeType.CONVERSATION),
                        usage_scope_id=command.conversation_id,
                        provider=str(ProviderType.DEEPSEEK),
                        status=str(CredentialStatus.ACTIVE),
                        latest_version=credential_version,
                        updated_by=command.updated_by,
                        now=now,
                    )
                )
            else:
                if current_credential is not None:
                    await unit_of_work.agent_control_plane.update_credential(
                        ProviderCredentialWrite(
                            credential_id=current_credential.credential_id,
                            owner_user_id=current_credential.owner_user_id,
                            usage_scope_type=str(current_credential.usage_scope_type),
                            usage_scope_id=current_credential.usage_scope_id,
                            provider=str(current_credential.provider),
                            status=str(CredentialStatus.DISABLED),
                            latest_version=current_credential.latest_version,
                            updated_by=command.updated_by,
                            now=now,
                        )
                    )
                credential_id = self._id_generator.next_id()
                credential_version = 1
                await unit_of_work.agent_control_plane.insert_credential(
                    ProviderCredentialWrite(
                        credential_id=credential_id,
                        owner_user_id=command.updated_by,
                        usage_scope_type=str(CredentialUsageScopeType.CONVERSATION),
                        usage_scope_id=command.conversation_id,
                        provider=str(ProviderType.DEEPSEEK),
                        status=str(CredentialStatus.ACTIVE),
                        latest_version=credential_version,
                        updated_by=command.updated_by,
                        now=now,
                    )
                )
            await unit_of_work.agent_control_plane.insert_credential_version(
                ProviderCredentialVersionWrite(
                    record_id=self._id_generator.next_id(),
                    credential_id=credential_id,
                    version=credential_version,
                    api_base=command.api_base.rstrip("/"),
                    api_key_ciphertext=encrypted_key.ciphertext,
                    api_key_nonce=encrypted_key.nonce,
                    key_fingerprint=encrypted_key.fingerprint,
                    connection_test_status=str(ConnectionTestStatus.SUCCEEDED),
                    connection_test_error=None,
                    tested_at=now,
                    created_by=command.updated_by,
                    now=now,
                )
            )
            await unit_of_work.agent_control_plane.update_binding(
                ConversationAgentBindingWrite(
                    binding_id=locked_view.binding.binding_id,
                    conversation_id=command.conversation_id,
                    agent_id=locked_view.binding.agent_id,
                    agent_version=locked_view.binding.agent_version,
                    credential_id=credential_id,
                    credential_version=credential_version,
                    status=str(
                        AgentBindingStatus.ACTIVE
                        if locked_view.definition.status is AgentDefinitionStatus.ACTIVE
                        else AgentBindingStatus.DISABLED
                    ),
                    binding_version=locked_view.binding.binding_version + 1,
                    updated_by=command.updated_by,
                    now=now,
                )
            )
            await unit_of_work.commit()
        return await self.get_context(command.conversation_id, permission)

    async def test_current_credential(
        self,
        conversation_id: int,
        permission: ConversationPermission,
    ) -> None:
        """
        使用当前已保存凭证执行最小模型请求

        :param conversation_id: 会话 ID
        :param permission: Message Service 返回的会话权限事实
        :return: 无返回值
        """

        async with self._unit_of_work_factory() as unit_of_work:
            view = await unit_of_work.agent_control_plane.get_view(conversation_id)
        if view is None or view.credential is None or view.credential_version is None:
            raise AgentDomainError(AgentErrorCode.CONFIG_NOT_FOUND, "当前会话尚未配置Agent凭证")
        if not self._can_manage_current_credential(permission, view):
            raise AgentDomainError(
                AgentErrorCode.PERMISSION_DENIED,
                "只有当前凭证所有者可以测试凭证",
            )
        runtime_model = self._to_runtime_model(view)
        try:
            await self._deepseek_chat_model.test_connection(runtime_model)
        except Exception as error:
            await self._save_test_result(
                view.credential.credential_id,
                view.credential_version.version,
                ConnectionTestStatus.FAILED,
                str(error)[:500],
            )
            raise
        await self._save_test_result(
            view.credential.credential_id,
            view.credential_version.version,
            ConnectionTestStatus.SUCCEEDED,
            None,
        )

    async def revoke_current_credential(
        self,
        conversation_id: int,
        permission: ConversationPermission,
    ) -> AgentContextView:
        """
        撤销当前会话绑定的 Provider 凭证

        :param conversation_id: 会话 ID
        :param permission: Message Service 返回的会话权限事实
        :return: 撤销后的会话 Agent 上下文
        """

        now = datetime.now(UTC)
        async with self._unit_of_work_factory() as unit_of_work:
            await unit_of_work.business_locks.lock(
                "agent-conversation-binding",
                str(conversation_id),
            )
            view = await unit_of_work.agent_control_plane.get_view(conversation_id)
            if view is None or view.credential is None:
                raise AgentDomainError(
                    AgentErrorCode.CONFIG_NOT_FOUND,
                    "当前会话尚未配置Agent凭证",
                )
            if not self._can_manage_current_credential(permission, view):
                raise AgentDomainError(
                    AgentErrorCode.PERMISSION_DENIED,
                    "只有当前凭证所有者可以撤销凭证",
                )
            await unit_of_work.agent_control_plane.update_credential(
                ProviderCredentialWrite(
                    credential_id=view.credential.credential_id,
                    owner_user_id=view.credential.owner_user_id,
                    usage_scope_type=str(view.credential.usage_scope_type),
                    usage_scope_id=view.credential.usage_scope_id,
                    provider=str(view.credential.provider),
                    status=str(CredentialStatus.REVOKED),
                    latest_version=view.credential.latest_version,
                    updated_by=permission.user_id,
                    now=now,
                )
            )
            await unit_of_work.agent_control_plane.update_binding(
                ConversationAgentBindingWrite(
                    binding_id=view.binding.binding_id,
                    conversation_id=conversation_id,
                    agent_id=view.binding.agent_id,
                    agent_version=view.binding.agent_version,
                    credential_id=None,
                    credential_version=None,
                    status=str(AgentBindingStatus.CREDENTIAL_UNAVAILABLE),
                    binding_version=view.binding.binding_version + 1,
                    updated_by=permission.user_id,
                    now=now,
                )
            )
            await unit_of_work.commit()
        return await self.get_context(conversation_id, permission)

    async def _insert_definition_version(
        self,
        unit_of_work: AgentUnitOfWork,
        command: SaveAgentDefinitionCommand,
        agent_id: int,
        agent_version: int,
        now: datetime,
    ) -> None:
        """
        新增完整 Agent 定义版本

        :param unit_of_work: 当前事务工作单元
        :param command: Agent 定义保存命令
        :param agent_id: Agent 定义 ID
        :param agent_version: 新 Agent 定义版本
        :param now: 统一写入时间
        :return: 无返回值
        """

        await unit_of_work.agent_control_plane.insert_definition_version(
            AgentDefinitionVersionWrite(
                record_id=self._id_generator.next_id(),
                agent_id=agent_id,
                version=agent_version,
                model_id=command.model_id,
                timeout_ms=command.timeout_ms,
                max_retries=command.max_retries,
                system_prompt=command.system_prompt,
                is_history_enabled=command.is_history_enabled,
                is_resource_enabled=command.is_resource_enabled,
                max_context_tokens=command.max_context_tokens,
                max_output_tokens=command.max_output_tokens,
                recent_message_tokens=command.recent_message_tokens,
                message_top_k=command.message_top_k,
                resource_top_k=command.resource_top_k,
                temperature=command.temperature,
                thinking_enabled=command.thinking_enabled,
                thinking_effort=command.thinking_effort,
                created_by=command.updated_by,
                now=now,
            )
        )

    async def _save_test_result(
        self,
        credential_id: int,
        credential_version: int,
        status: ConnectionTestStatus,
        error_message: str | None,
    ) -> None:
        """
        保存指定凭证版本的连接测试结果

        :param credential_id: Provider 凭证 ID
        :param credential_version: Provider 凭证版本
        :param status: 连接测试状态
        :param error_message: 脱敏错误摘要
        :return: 无返回值
        """

        async with self._unit_of_work_factory() as unit_of_work:
            await unit_of_work.agent_control_plane.update_connection_test(
                credential_id,
                credential_version,
                str(status),
                error_message,
            )
            await unit_of_work.commit()

    def _to_runtime_model(self, view: AgentControlPlaneView) -> RuntimeModelConfig:
        """
        将控制面聚合转换为解密后的运行模型配置

        :param view: 完整 Agent 控制面聚合
        :return: 解密后的运行模型配置
        """

        credential = view.credential
        credential_version = view.credential_version
        if credential is None or credential_version is None:
            raise AgentDomainError(AgentErrorCode.CONFIG_NOT_FOUND, "Provider凭证不存在")
        return RuntimeModelConfig(
            credential_id=credential.credential_id,
            credential_version=credential_version.version,
            credential_owner_user_id=credential.owner_user_id,
            provider=credential.provider,
            api_base=credential_version.api_base,
            api_key=self._api_key_cipher.decrypt(
                credential_version.api_key_ciphertext,
                credential_version.api_key_nonce,
            ),
            model_id=view.definition_version.model_id,
            timeout_ms=view.definition_version.timeout_ms,
            max_retries=view.definition_version.max_retries,
        )

    @staticmethod
    def _can_manage_current_credential(
        permission: ConversationPermission,
        view: AgentControlPlaneView,
    ) -> bool:
        """
        判断当前用户是否为当前凭证所有者

        :param permission: Message Service 返回的会话权限事实
        :param view: 当前 Agent 控制面聚合
        :return: 当前用户是否可以管理已绑定凭证
        """

        return (
            permission.is_member
            and view.credential is not None
            and view.credential.owner_user_id == permission.user_id
        )

    @staticmethod
    def _version_conflict() -> AgentDomainError:
        """
        构造控制面乐观锁冲突异常

        :return: Agent 控制面版本冲突异常
        """

        return AgentDomainError(
            AgentErrorCode.CONFIG_VERSION_CONFLICT,
            "Agent配置版本冲突，请刷新后重试",
        )
