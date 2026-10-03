"""资源切块模型。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/29 18:46
"""

from dataclasses import dataclass, field

from union_talk_agent.knowledge.domain.enums import ChunkKind, MimeGroup


@dataclass(frozen=True, slots=True)
class ParsedPage:
    """解析完成的单页文本与页码。"""

    page_number: int
    text_content: str


@dataclass(frozen=True, slots=True)
class ParsedDocument:
    """解析完成的文本和定位信息。"""

    text_content: str
    mime_group: MimeGroup
    title: str = ""
    page_list: tuple[ParsedPage, ...] = ()


@dataclass(frozen=True, slots=True)
class ResourceChunk:
    """准备持久化和向量化的资源块。"""

    chunk_id: int
    resource_id: int
    conversation_id: int
    parent_chunk_id: int | None
    chunk_kind: ChunkKind
    chunk_no: int
    text_content: str
    token_count: int
    mime_group: MimeGroup
    heading_path: str = ""
    page_from: int | None = None
    page_to: int | None = None
    metadata_map: dict[str, str | int | float | bool | None] = field(
        default_factory=lambda: dict[str, str | int | float | bool | None]()
    )
