"""资源索引领域模型。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/29 19:15
"""

from dataclasses import dataclass

from union_talk_agent.knowledge.domain.enums import ResourceStatus


@dataclass(frozen=True, slots=True)
class ResourceDescriptor:
    """File Service 返回的资源元数据快照。"""

    asset_file_id: int
    conversation_id: int
    resource_version: int
    file_name: str
    mime_type: str
    sha256: str
    etag: str = ""
    folder_id: int | None = None
    path_text: str = ""


@dataclass(frozen=True, slots=True)
class ResourceContent:
    """资源描述和通过临时下载地址读取的原始内容。"""

    descriptor: ResourceDescriptor
    content: bytes


@dataclass(slots=True)
class IndexedResource:
    """PostgreSQL 中可重建的资源索引版本。"""

    resource_id: int
    descriptor: ResourceDescriptor
    status: ResourceStatus
    parser_version: str
    chunker_version: str
    embedding_model_version: str
    chunk_count: int = 0
    token_count: int = 0
    error_code: str | None = None
    error_message: str | None = None
