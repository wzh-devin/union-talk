"""RAG 召回证据模型。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/29 18:46
"""

from dataclasses import dataclass

from union_talk_agent.knowledge.domain.enums import SourceType


@dataclass(frozen=True, slots=True)
class VectorCandidate:
    """Milvus 返回的会话内候选。"""

    source_type: SourceType
    source_id: int
    source_version: int
    score: float


@dataclass(frozen=True, slots=True)
class RetrievalEvidence:
    """回表校验后的回答证据。"""

    source_type: SourceType
    source_id: int
    text_content: str
    score: float
    message_id: int | None = None
    rerank_score: float | None = None
    asset_file_id: int | None = None
    resource_version: int | None = None
    page_from: int | None = None
    page_to: int | None = None
    heading_path: str = ""
    parent_chunk_id: int | None = None
    chunk_no: int | None = None
    evidence_key: str = ""
