"""Nacos 服务注册配置。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/31 14:05
"""

from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class NacosSettings(BaseSettings):
    """Agent API 的 Nacos 服务注册配置。"""

    model_config = SettingsConfigDict(
        env_prefix="AGENT_NACOS_",
        env_file=".env",
        extra="ignore",
    )

    enabled: bool = Field(default=True, description="是否注册 Nacos 服务实例")
    server_addresses: str = Field(
        default="127.0.0.1:8848",
        min_length=3,
        description="Nacos 服务端地址",
    )
    namespace_id: str = Field(default="union-talk", description="Nacos 命名空间 ID")
    group_name: str = Field(default="DEFAULT_GROUP", min_length=1, description="服务分组")
    service_name: str = Field(
        default="union-talk-agent",
        min_length=1,
        description="服务名称",
    )
    cluster_name: str = Field(default="DEFAULT", min_length=1, description="集群名称")
    instance_ip: str = Field(
        default="127.0.0.1",
        min_length=1,
        description="Gateway 可访问的实例 IP",
    )
    username: str = Field(default="nacos", description="Nacos 用户名")
    password: str = Field(default="nacos", description="Nacos 密码")
    weight: float = Field(default=1.0, gt=0, le=100, description="实例权重")
    grpc_timeout_ms: int = Field(
        default=5000,
        ge=1000,
        le=30000,
        description="Nacos gRPC 超时毫秒数",
    )
