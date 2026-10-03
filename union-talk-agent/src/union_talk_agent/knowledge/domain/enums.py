"""知识索引枚举。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/29 18:46
"""

from enum import StrEnum


class SourceType(StrEnum):
    """向量来源类型。"""

    MESSAGE_SEGMENT = "MESSAGE_SEGMENT"
    RESOURCE_CHUNK = "RESOURCE_CHUNK"


class ChunkKind(StrEnum):
    """资源块类型。"""

    NOT_APPLICABLE = "NOT_APPLICABLE"
    CHILD = "CHILD"
    TABLE = "TABLE"
    PARENT = "PARENT"


class MimeGroup(StrEnum):
    """MIME 粗分类。"""

    UNKNOWN = "UNKNOWN"
    PDF = "PDF"
    DOCX = "DOCX"
    TEXT = "TEXT"
    MARKDOWN = "MARKDOWN"
    HTML = "HTML"
    JSON = "JSON"


class ResourceStatus(StrEnum):
    """资源索引状态。"""

    PENDING = "PENDING"
    INDEXING = "INDEXING"
    READY = "READY"
    FAILED = "FAILED"
    DELETED = "DELETED"
    UNSUPPORTED = "UNSUPPORTED"
    OCR_REQUIRED = "OCR_REQUIRED"


class AssetLifecycleStatus(StrEnum):
    """File Service 资产生命周期投影状态。"""

    ACTIVE = "ACTIVE"
    DELETED = "DELETED"


class VectorStatus(StrEnum):
    """向量写入状态。"""

    PENDING = "PENDING"
    READY = "READY"
    FAILED = "FAILED"
    DELETED = "DELETED"
    NOT_APPLICABLE = "NOT_APPLICABLE"


class IngestJobStage(StrEnum):
    """资源入库任务阶段。"""

    DOWNLOADING = "DOWNLOADING"
    PARSING = "PARSING"
    CHUNKING = "CHUNKING"
    EMBEDDING = "EMBEDDING"
    VECTOR_WRITING = "VECTOR_WRITING"
    FINALIZING = "FINALIZING"


class IngestJobStatus(StrEnum):
    """资源入库任务状态。"""

    RUNNING = "RUNNING"
    SUCCEEDED = "SUCCEEDED"
    FAILED = "FAILED"
    CANCELLED = "CANCELLED"
