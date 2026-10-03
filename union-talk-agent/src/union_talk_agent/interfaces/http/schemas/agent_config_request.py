"""Agent 控制面请求模型。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/08/07 18:55
"""

from pydantic import BaseModel, ConfigDict, Field, HttpUrl, SecretStr
from pydantic.alias_generators import to_camel

from union_talk_agent.agent_config.domain.constants import (
    DEFAULT_MESSAGE_TOP_K,
    DEFAULT_RESOURCE_TOP_K,
    MAX_API_BASE_LENGTH,
    MAX_CONTEXT_TOKENS,
    MAX_MODEL_ID_LENGTH,
    MAX_OUTPUT_TOKENS,
    MAX_SYSTEM_PROMPT_LENGTH,
    MIN_CONTEXT_TOKENS,
    MIN_OUTPUT_TOKENS,
)


class AgentDefinitionRequest(BaseModel):
    """新增或版本化更新 Agent 定义的请求。"""

    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)

    agent_version: int = Field(default=0, ge=0)
    binding_version: int = Field(default=0, ge=0)
    model_id: str = Field(default="deepseek-chat", min_length=1, max_length=MAX_MODEL_ID_LENGTH)
    timeout_ms: int = Field(default=60_000, ge=1000, le=600_000)
    max_retries: int = Field(default=2, ge=0, le=5)
    display_name: str = Field(default="AI 助手", min_length=1, max_length=128)
    system_prompt: str = Field(default="", max_length=MAX_SYSTEM_PROMPT_LENGTH)
    enabled: bool = True
    history_enabled: bool = True
    resource_enabled: bool = True
    max_context_tokens: int = Field(
        default=32_000,
        ge=MIN_CONTEXT_TOKENS,
        le=MAX_CONTEXT_TOKENS,
    )
    max_output_tokens: int = Field(
        default=4096,
        ge=MIN_OUTPUT_TOKENS,
        le=MAX_OUTPUT_TOKENS,
    )
    recent_message_tokens: int = Field(default=6000, ge=0, le=32_000)
    message_top_k: int = Field(default=DEFAULT_MESSAGE_TOP_K, ge=0, le=100)
    resource_top_k: int = Field(default=DEFAULT_RESOURCE_TOP_K, ge=0, le=100)
    temperature: float = Field(default=0.2, ge=0, le=2)
    thinking_enabled: bool = False
    thinking_effort: str = Field(default="medium", pattern="^(low|medium|high)$")


class AgentCredentialRequest(BaseModel):
    """测试并替换会话当前 Provider 凭证的请求。"""

    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)

    binding_version: int = Field(ge=1)
    api_base: HttpUrl = Field(max_length=MAX_API_BASE_LENGTH)
    api_key: SecretStr = Field(min_length=1, max_length=2048)
