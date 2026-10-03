"""Milvus 与 Embedding 配置。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/29 18:46
"""

from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class MilvusSettings(BaseSettings):
    """Milvus Collection 与系统级 Embedding 配置。"""

    model_config = SettingsConfigDict(
        env_prefix="AGENT_MILVUS_",
        env_file=".env",
        extra="ignore",
    )

    uri: str = Field(default="http://127.0.0.1:19530", description="Milvus 连接地址")
    token: str = Field(default="", description="Milvus 连接令牌")
    enabled: bool = Field(default=False, description="是否启用向量检索")
    collection_name: str = Field(default="union_talk_rag_v2", description="共享 Collection 名称")


class EmbeddingSettings(BaseSettings):
    """系统级 BGE-M3 Embedding 服务配置。"""

    model_config = SettingsConfigDict(
        env_prefix="AGENT_EMBEDDING_",
        env_file=".env",
        extra="ignore",
    )

    api_base: str = Field(default="http://127.0.0.1:13008", description="Embedding API 地址")
    api_key: str = Field(default="", description="Embedding API Key")
    model: str = Field(default="BAAI/bge-m3", description="Embedding 模型标识")
    dimension: int = Field(default=1024, ge=1, description="向量维度")
    timeout_seconds: int = Field(default=30, ge=1, le=300, description="Embedding 超时秒数")
    batch_size: int = Field(default=16, ge=1, le=256, description="模型批处理大小")
    batch_wait_milliseconds: int = Field(
        default=10,
        ge=0,
        le=100,
        description="并发请求微批等待毫秒数",
    )
    queue_capacity: int = Field(default=1024, ge=1, le=10000, description="微批请求队列容量")
    host: str = Field(default="0.0.0.0", description="Embedding API 监听地址")
    port: int = Field(default=13008, ge=1, le=65535, description="Embedding API 监听端口")
