"""Agent 控制面安全响应模型。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/08/07 18:55
"""

from pydantic import BaseModel, ConfigDict
from pydantic.alias_generators import to_camel

from union_talk_agent.agent_config.domain.agent_config import AgentContextView


class AgentDefinitionResponse(BaseModel):
    """Agent 定义和当前版本响应。"""

    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)

    agent_id: str
    agent_version: int
    binding_version: int
    display_name: str
    model_id: str
    timeout_ms: int
    max_retries: int
    system_prompt: str
    enabled: bool
    history_enabled: bool
    resource_enabled: bool
    max_context_tokens: int
    max_output_tokens: int
    recent_message_tokens: int
    message_top_k: int
    resource_top_k: int
    temperature: float
    thinking_enabled: bool
    thinking_effort: str


class AgentCredentialResponse(BaseModel):
    """不包含 API Key 明文和密文的凭证摘要。"""

    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)

    credential_id: str
    credential_version: int
    owner_user_id: str
    provider: str
    api_base: str
    key_fingerprint: str
    status: str
    connection_test_status: str
    connection_test_error: str | None


class AgentContextResponse(BaseModel):
    """包含服务端能力判断的会话 Agent 上下文响应。"""

    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)

    conversation_id: str
    conversation_type: str
    agent_state: str
    can_view: bool
    can_invoke: bool
    can_manage_definition: bool
    can_manage_current_credential: bool
    can_replace_credential: bool
    agent: AgentDefinitionResponse | None
    credential: AgentCredentialResponse | None

    @classmethod
    def from_view(cls, view: AgentContextView) -> "AgentContextResponse":
        """
        从领域上下文构造安全响应

        :param view: 会话 Agent 上下文
        :return: 不暴露密钥的 Agent 上下文响应
        """

        control_plane = view.control_plane
        if control_plane is None:
            agent = None
            credential = None
        else:
            definition = control_plane.definition
            definition_version = control_plane.definition_version
            binding = control_plane.binding
            agent = AgentDefinitionResponse(
                agent_id=str(definition.agent_id),
                agent_version=definition_version.version,
                binding_version=binding.binding_version,
                display_name=definition.display_name,
                model_id=definition_version.model_id,
                timeout_ms=definition_version.timeout_ms,
                max_retries=definition_version.max_retries,
                system_prompt=(
                    definition_version.system_prompt if view.can_manage_definition else ""
                ),
                enabled=str(definition.status) == "ACTIVE",
                history_enabled=definition_version.is_history_enabled,
                resource_enabled=definition_version.is_resource_enabled,
                max_context_tokens=definition_version.max_context_tokens,
                max_output_tokens=definition_version.max_output_tokens,
                recent_message_tokens=definition_version.recent_message_tokens,
                message_top_k=definition_version.message_top_k,
                resource_top_k=definition_version.resource_top_k,
                temperature=definition_version.temperature,
                thinking_enabled=definition_version.thinking_enabled,
                thinking_effort=definition_version.thinking_effort,
            )
            if control_plane.credential is None or control_plane.credential_version is None:
                credential = None
            else:
                credential_record = control_plane.credential
                credential_version = control_plane.credential_version
                credential = AgentCredentialResponse(
                    credential_id=str(credential_record.credential_id),
                    credential_version=credential_version.version,
                    owner_user_id=str(credential_record.owner_user_id),
                    provider=str(credential_record.provider),
                    api_base=credential_version.api_base,
                    key_fingerprint=credential_version.key_fingerprint,
                    status=str(credential_record.status),
                    connection_test_status=str(credential_version.connection_test_status),
                    connection_test_error=credential_version.connection_test_error,
                )
        return cls(
            conversation_id=str(view.conversation_id),
            conversation_type=view.conversation_type,
            agent_state=str(view.state),
            can_view=view.can_view,
            can_invoke=view.can_invoke,
            can_manage_definition=view.can_manage_definition,
            can_manage_current_credential=view.can_manage_current_credential,
            can_replace_credential=view.can_replace_credential,
            agent=agent,
            credential=credential,
        )
