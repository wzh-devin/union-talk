"""模型运行凭证配置。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/08/07 18:25
"""

from dataclasses import dataclass

from union_talk_agent.agent_config.domain.enums import ProviderType


@dataclass(frozen=True, slots=True)
class RuntimeModelConfig:
    """单次 Run 使用的解密后模型凭证快照。"""

    credential_id: int
    credential_version: int
    credential_owner_user_id: int
    provider: ProviderType
    api_base: str
    api_key: str
    model_id: str
    timeout_ms: int
    max_retries: int
