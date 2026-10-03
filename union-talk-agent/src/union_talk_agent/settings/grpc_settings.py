"""gRPC 客户端配置。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/29 18:46
"""

from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class GrpcSettings(BaseSettings):
    """Message Service gRPC 客户端配置。"""

    model_config = SettingsConfigDict(
        env_prefix="AGENT_GRPC_",
        env_file=".env",
        extra="ignore",
    )

    message_target: str = Field(default="127.0.0.1:19091", description="Message gRPC 地址")
    file_target: str = Field(default="127.0.0.1:19006", description="File gRPC 地址")
    deadline_seconds: float = Field(default=5.0, gt=0, le=60, description="单次调用截止秒数")
