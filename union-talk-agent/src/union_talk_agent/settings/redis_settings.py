"""Redis 实时面配置。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/29 18:46
"""

from pydantic import Field, SecretStr
from pydantic_settings import BaseSettings, SettingsConfigDict


class RedisSettings(BaseSettings):
    """Redis Snapshot、Stream 与 Pub/Sub 配置。"""

    model_config = SettingsConfigDict(
        env_prefix="AGENT_REDIS_",
        env_file=".env",
        extra="ignore",
    )

    url: str = Field(default="redis://127.0.0.1:6379/0", description="Redis 连接地址")
    username: str = Field(default="", description="Redis ACL 用户名")
    password: SecretStr = Field(default=SecretStr(""), description="Redis 访问密码")
    enabled: bool = Field(default=True, description="是否启用实时事件")
    run_ttl_seconds: int = Field(default=86400, ge=60, description="Run 实时数据过期秒数")
    stream_max_length: int = Field(default=2000, ge=100, description="单 Run Stream 最大事件数")
