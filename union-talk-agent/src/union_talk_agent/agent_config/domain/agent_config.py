"""Agent 定义、凭证和会话绑定领域模型。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/08/07 18:25
"""

from dataclasses import dataclass
from datetime import datetime

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


@dataclass(frozen=True, slots=True)
class AgentDefinition:
    """稳定的 Agent 定义身份。"""

    agent_id: int
    owner_type: AgentOwnerType
    owner_id: int
    display_name: str
    status: AgentDefinitionStatus
    latest_version: int


@dataclass(frozen=True, slots=True)
class AgentDefinitionVersion:
    """不可变的 Agent 定义版本。"""

    agent_id: int
    version: int
    model_id: str
    timeout_ms: int
    max_retries: int
    system_prompt: str
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
class ProviderCredential:
    """稳定的 Provider 凭证身份和所有权。"""

    credential_id: int
    owner_user_id: int
    usage_scope_type: CredentialUsageScopeType
    usage_scope_id: int
    provider: ProviderType
    status: CredentialStatus
    latest_version: int


@dataclass(frozen=True, slots=True)
class ProviderCredentialVersion:
    """不可变的 Provider 凭证密文版本。"""

    credential_id: int
    version: int
    api_base: str
    api_key_ciphertext: bytes
    api_key_nonce: bytes
    key_fingerprint: str
    connection_test_status: ConnectionTestStatus
    connection_test_error: str | None
    tested_at: datetime | None


@dataclass(frozen=True, slots=True)
class ConversationAgentBinding:
    """会话当前采用的 Agent 和凭证版本。"""

    binding_id: int
    conversation_id: int
    agent_id: int
    agent_version: int
    credential_id: int | None
    credential_version: int | None
    status: AgentBindingStatus
    binding_version: int
    updated_by: int


@dataclass(frozen=True, slots=True)
class AgentControlPlaneView:
    """不包含明文 API Key 的控制面聚合视图。"""

    binding: ConversationAgentBinding
    definition: AgentDefinition
    definition_version: AgentDefinitionVersion
    credential: ProviderCredential | None
    credential_version: ProviderCredentialVersion | None


@dataclass(frozen=True, slots=True)
class AgentContextView:
    """包含服务端授权能力的会话 Agent 上下文。"""

    conversation_id: int
    conversation_type: str
    state: AgentContextState
    control_plane: AgentControlPlaneView | None
    can_view: bool
    can_invoke: bool
    can_manage_definition: bool
    can_manage_current_credential: bool
    can_replace_credential: bool


@dataclass(frozen=True, slots=True)
class ConversationAgentConfig:
    """单次 Run 使用的不可变 Agent 定义快照。"""

    agent_id: int
    agent_version: int
    display_name: str
    system_prompt: str
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
class RuntimeAgentConfig:
    """回答执行期固定的 Agent 定义和模型凭证快照。"""

    conversation: ConversationAgentConfig
    model: RuntimeModelConfig
