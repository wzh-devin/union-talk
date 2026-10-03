"""PostgreSQL 连接配置。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/29 18:46
"""

from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class PostgresSettings(BaseSettings):
    """PostgreSQL 连接池配置。"""

    model_config = SettingsConfigDict(
        env_prefix="AGENT_POSTGRES_",
        env_file=".env",
        extra="ignore",
    )

    dsn: str = Field(
        default="postgresql+asyncpg://postgres:postgres@127.0.0.1:5432/union_talk",
        description="PostgreSQL 异步连接地址",
    )
    pool_size: int = Field(default=10, ge=1, le=100, description="连接池常驻连接数")
    max_overflow: int = Field(default=20, ge=0, le=200, description="连接池额外连接数")
    pool_timeout_seconds: int = Field(default=10, ge=1, le=120, description="获取连接超时秒数")
