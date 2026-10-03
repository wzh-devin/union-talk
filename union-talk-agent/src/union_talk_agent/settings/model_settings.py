"""模型 Provider 默认配置。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/29 18:46
"""

from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class ModelSettings(BaseSettings):
    """未配置会话模型时使用的 DeepSeek 默认值。"""

    model_config = SettingsConfigDict(
        env_prefix="AGENT_MODEL_",
        env_file=".env",
        extra="ignore",
    )

    deepseek_api_base: str = Field(
        default="https://api.deepseek.com",
        description="DeepSeek 默认 API 地址",
    )
    deepseek_model_id: str = Field(default="deepseek-chat", description="DeepSeek 默认模型")
    timeout_seconds: float = Field(default=60.0, gt=0, le=600, description="模型请求超时秒数")
    max_retries: int = Field(default=2, ge=0, le=5, description="首 Token 前最大重试次数")
