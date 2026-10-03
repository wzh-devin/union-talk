"""Agent 控制面枚举。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/08/07 18:25
"""

from enum import StrEnum


class ProviderType(StrEnum):
    """模型 Provider。"""

    DEEPSEEK = "DEEPSEEK"


class AgentOwnerType(StrEnum):
    """Agent 定义所有者类型。"""

    CONVERSATION = "CONVERSATION"
    GROUP = "GROUP"
    PLATFORM = "PLATFORM"


class CredentialUsageScopeType(StrEnum):
    """Provider 凭证允许使用的作用域类型。"""

    CONVERSATION = "CONVERSATION"
    GROUP = "GROUP"
    PLATFORM = "PLATFORM"


class AgentDefinitionStatus(StrEnum):
    """Agent 定义状态。"""

    ACTIVE = "ACTIVE"
    DISABLED = "DISABLED"
    ARCHIVED = "ARCHIVED"


class CredentialStatus(StrEnum):
    """Provider 凭证状态。"""

    ACTIVE = "ACTIVE"
    DISABLED = "DISABLED"
    REVOKED = "REVOKED"


class AgentBindingStatus(StrEnum):
    """会话 Agent 绑定状态。"""

    ACTIVE = "ACTIVE"
    DISABLED = "DISABLED"
    CREDENTIAL_UNAVAILABLE = "CREDENTIAL_UNAVAILABLE"
    ARCHIVED = "ARCHIVED"


class AgentContextState(StrEnum):
    """前端可消费的会话 Agent 状态。"""

    NOT_CONFIGURED = "NOT_CONFIGURED"
    READY = "READY"
    DISABLED = "DISABLED"
    CREDENTIAL_UNAVAILABLE = "CREDENTIAL_UNAVAILABLE"


class ConnectionTestStatus(StrEnum):
    """模型连接测试状态。"""

    UNTESTED = "UNTESTED"
    SUCCEEDED = "SUCCEEDED"
    FAILED = "FAILED"
