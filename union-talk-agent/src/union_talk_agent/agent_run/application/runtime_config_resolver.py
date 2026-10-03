"""回答执行期版本快照加载。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/08/07 19:10
"""

from union_talk_agent.agent_config.domain.agent_config import (
    ConversationAgentConfig,
    RuntimeAgentConfig,
)
from union_talk_agent.agent_config.domain.enums import (
    AgentBindingStatus,
    ConnectionTestStatus,
    CredentialStatus,
    ProviderType,
)
from union_talk_agent.agent_config.domain.model_config import RuntimeModelConfig
from union_talk_agent.agent_run.application.ports.agent_unit_of_work import AgentUnitOfWorkFactory
from union_talk_agent.agent_run.domain.agent_run import AgentRun
from union_talk_agent.agent_run.domain.constants import (
    DEFAULT_FIXED_MODEL_ID,
    DEFAULT_MAX_CONTEXT_TOKENS,
    DEFAULT_MAX_OUTPUT_TOKENS,
    DEFAULT_RECENT_MESSAGE_TOKENS,
)
from union_talk_agent.agent_run.domain.exceptions import (
    AgentErrorCode,
    NonRetryableAgentError,
)
from union_talk_agent.infrastructure.crypto.api_key_cipher import ApiKeyCipher


class RuntimeConfigLoader:
    """只按 Run 固化的版本标识加载不可变运行配置。"""

    def __init__(
        self,
        unit_of_work_factory: AgentUnitOfWorkFactory,
        api_key_cipher: ApiKeyCipher,
        fixed_answer_enabled: bool,
        default_api_base: str,
        default_model_id: str,
        default_timeout_seconds: float,
        default_max_retries: int,
    ) -> None:
        """
        初始化 RuntimeConfigLoader

        :param unit_of_work_factory: Agent 工作单元工厂
        :param api_key_cipher: API Key 加解密组件
        :param fixed_answer_enabled: 是否启用固定回答模式
        :param default_api_base: 默认模型 API 基础地址
        :param default_model_id: 默认模型标识
        :param default_timeout_seconds: 默认请求超时秒数
        :param default_max_retries: 默认最大重试次数
        :return: 无返回值
        """

        self._unit_of_work_factory = unit_of_work_factory
        self._api_key_cipher = api_key_cipher
        self._fixed_answer_enabled = fixed_answer_enabled
        self._default_api_base = default_api_base
        self._default_model_id = default_model_id
        self._default_timeout_ms = int(default_timeout_seconds * 1000)
        self._default_max_retries = default_max_retries

    async def load(self, run: AgentRun) -> RuntimeAgentConfig:
        """
        严格按 Run 创建时固化的版本加载并解密运行配置

        :param run: 已包含完整控制面版本快照的 Agent Run
        :return: 本次 Run 的不可变运行配置
        """

        if self._fixed_answer_enabled:
            return self._fixed_runtime_config(run)
        async with self._unit_of_work_factory() as unit_of_work:
            view = await unit_of_work.agent_control_plane.get_versioned_view(
                binding_id=run.binding_id,
                binding_version=run.binding_version,
                agent_id=run.agent_id,
                agent_version=run.agent_version,
                credential_id=run.credential_id,
                credential_version=run.credential_version,
            )
        if view is None or view.credential is None or view.credential_version is None:
            raise NonRetryableAgentError(
                AgentErrorCode.CONFIG_NOT_FOUND,
                "Agent Run对应的配置快照不存在",
            )
        if view.binding.status is AgentBindingStatus.ARCHIVED:
            raise NonRetryableAgentError(
                AgentErrorCode.CONFIG_DISABLED,
                "Agent Run对应的会话绑定已归档",
            )
        if view.credential.status not in {
            CredentialStatus.ACTIVE,
            CredentialStatus.DISABLED,
        }:
            raise NonRetryableAgentError(
                AgentErrorCode.CONFIG_DISABLED,
                "Agent Run对应的Provider凭证已撤销",
            )
        if view.credential_version.connection_test_status is not ConnectionTestStatus.SUCCEEDED:
            raise NonRetryableAgentError(
                AgentErrorCode.CONFIG_DISABLED,
                "Agent Run对应的Provider凭证未通过连接测试",
            )
        definition = view.definition_version
        return RuntimeAgentConfig(
            conversation=ConversationAgentConfig(
                agent_id=view.definition.agent_id,
                agent_version=definition.version,
                display_name=view.definition.display_name,
                system_prompt=definition.system_prompt,
                is_history_enabled=definition.is_history_enabled,
                is_resource_enabled=definition.is_resource_enabled,
                max_context_tokens=definition.max_context_tokens,
                max_output_tokens=definition.max_output_tokens,
                recent_message_tokens=definition.recent_message_tokens,
                message_top_k=definition.message_top_k,
                resource_top_k=definition.resource_top_k,
                temperature=definition.temperature,
                thinking_enabled=definition.thinking_enabled,
                thinking_effort=definition.thinking_effort,
            ),
            model=RuntimeModelConfig(
                credential_id=view.credential.credential_id,
                credential_version=view.credential_version.version,
                credential_owner_user_id=view.credential.owner_user_id,
                provider=view.credential.provider,
                api_base=view.credential_version.api_base,
                api_key=self._api_key_cipher.decrypt(
                    view.credential_version.api_key_ciphertext,
                    view.credential_version.api_key_nonce,
                ),
                model_id=definition.model_id,
                timeout_ms=definition.timeout_ms,
                max_retries=definition.max_retries,
            ),
        )

    def _fixed_runtime_config(self, run: AgentRun) -> RuntimeAgentConfig:
        """
        构建仅用于本地链路测试的固定回答运行配置

        :param run: 已创建的 Agent Run
        :return: 固定回答运行配置
        """

        return RuntimeAgentConfig(
            conversation=ConversationAgentConfig(
                agent_id=run.agent_id,
                agent_version=run.agent_version,
                display_name="AI 助手",
                system_prompt="",
                is_history_enabled=False,
                is_resource_enabled=False,
                max_context_tokens=DEFAULT_MAX_CONTEXT_TOKENS,
                max_output_tokens=DEFAULT_MAX_OUTPUT_TOKENS,
                recent_message_tokens=DEFAULT_RECENT_MESSAGE_TOKENS,
                message_top_k=0,
                resource_top_k=0,
                temperature=0.2,
                thinking_enabled=False,
                thinking_effort="medium",
            ),
            model=RuntimeModelConfig(
                credential_id=run.credential_id,
                credential_version=run.credential_version,
                credential_owner_user_id=run.credential_owner_user_id,
                provider=ProviderType.DEEPSEEK,
                api_base=self._default_api_base,
                api_key="",
                model_id=DEFAULT_FIXED_MODEL_ID,
                timeout_ms=self._default_timeout_ms,
                max_retries=self._default_max_retries,
            ),
        )
