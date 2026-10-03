"""RabbitMQ 连接配置。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/29 18:46
"""

from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class RabbitMqSettings(BaseSettings):
    """RabbitMQ 消费配置。"""

    model_config = SettingsConfigDict(
        env_prefix="AGENT_RABBITMQ_",
        env_file=".env",
        extra="ignore",
    )

    url: str = Field(default="amqp://guest:guest@127.0.0.1/", description="RabbitMQ 连接地址")
    prefetch_count: int = Field(default=8, ge=1, le=1000, description="单连接预取数量")
    consumer_concurrency: int = Field(default=4, ge=1, le=64, description="消费者并发数量")
    reconnect_interval_seconds: int = Field(default=5, ge=1, le=60, description="重连间隔秒数")
    retry_delay_seconds: int = Field(default=5, ge=1, le=300, description="索引失败重试等待秒数")
    max_delivery_attempts: int = Field(default=5, ge=1, le=20, description="事件最大处理次数")
