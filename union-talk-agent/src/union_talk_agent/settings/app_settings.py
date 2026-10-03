"""Agent 进程总配置。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/29 18:46
"""

from dataclasses import dataclass
from functools import lru_cache

from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict

from union_talk_agent.settings.grpc_settings import GrpcSettings
from union_talk_agent.settings.milvus_settings import EmbeddingSettings, MilvusSettings
from union_talk_agent.settings.model_settings import ModelSettings
from union_talk_agent.settings.nacos_settings import NacosSettings
from union_talk_agent.settings.ocr_settings import OcrSettings
from union_talk_agent.settings.postgres_settings import PostgresSettings
from union_talk_agent.settings.rabbitmq_settings import RabbitMqSettings
from union_talk_agent.settings.redis_settings import RedisSettings


class AppSettings(BaseSettings):
    """Agent 应用、入口和功能开关配置。"""

    model_config = SettingsConfigDict(env_prefix="AGENT_", env_file=".env", extra="ignore")

    environment: str = Field(default="local", description="运行环境")
    log_level: str = Field(default="INFO", description="日志级别")
    api_host: str = Field(default="0.0.0.0", description="API 监听地址")
    api_port: int = Field(default=18080, ge=1, le=65535, description="API 监听端口")
    fixed_answer_enabled: bool = Field(default=False, description="是否启用联调固定回答模型")
    fixed_answer_text: str = Field(
        default="[联调模式] Agent 固定回答。",
        min_length=1,
        max_length=4000,
        description="仅用于联调的固定回答正文",
    )
    answer_worker_enabled: bool = Field(default=True, description="是否启用回答 Worker")
    index_worker_enabled: bool = Field(default=False, description="是否启用索引 Worker")
    reconciliation_worker_enabled: bool = Field(default=True, description="是否启用对账 Worker")
    id_worker_id: int = Field(default=1, ge=0, le=31, description="雪花算法工作节点")
    id_datacenter_id: int = Field(default=1, ge=0, le=31, description="雪花算法数据中心")
    api_key_master_key: str = Field(default="", description="API Key AEAD 主密钥")
    reconciliation_interval_seconds: int = Field(
        default=30,
        ge=5,
        le=3600,
        description="对账扫描间隔秒数",
    )
    run_lease_seconds: int = Field(
        default=90,
        ge=30,
        le=3600,
        description="回答Worker执行租约秒数",
    )
    run_heartbeat_seconds: int = Field(
        default=20,
        ge=5,
        le=300,
        description="回答Worker续租心跳间隔秒数",
    )


@dataclass(frozen=True, slots=True)
class Settings:
    """Agent 全部进程配置集合。"""

    app: AppSettings
    postgres: PostgresSettings
    rabbitmq: RabbitMqSettings
    redis: RedisSettings
    milvus: MilvusSettings
    embedding: EmbeddingSettings
    grpc: GrpcSettings
    model: ModelSettings
    nacos: NacosSettings
    ocr: OcrSettings


@lru_cache(maxsize=1)
def load_settings() -> Settings:
    """
    加载并缓存进程配置

    :return: Agent 全部进程配置
    """

    return Settings(
        app=AppSettings(),
        postgres=PostgresSettings(),
        rabbitmq=RabbitMqSettings(),
        redis=RedisSettings(),
        milvus=MilvusSettings(),
        embedding=EmbeddingSettings(),
        grpc=GrpcSettings(),
        model=ModelSettings(),
        nacos=NacosSettings(),
        ocr=OcrSettings(),
    )
