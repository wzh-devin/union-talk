"""Milvus Collection 与检索适配器。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/29 19:04
"""

from union_talk_agent.infrastructure.milvus.collection_manager import (
    MilvusCollectionManager,
)
from union_talk_agent.infrastructure.milvus.knowledge_retriever import (
    DisabledKnowledgeRetriever,
    MilvusKnowledgeRetriever,
)
from union_talk_agent.infrastructure.milvus.resource_vector_index import (
    MilvusResourceVectorIndex,
)

__all__ = [
    "DisabledKnowledgeRetriever",
    "MilvusCollectionManager",
    "MilvusKnowledgeRetriever",
    "MilvusResourceVectorIndex",
]
