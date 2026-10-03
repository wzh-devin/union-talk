"""Agent PostgreSQL Core Table。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/29 18:49
"""

from union_talk_agent.infrastructure.postgres.tables.agent_tables import (
    agent_asset_lifecycle_table,
    agent_conversation_binding_table,
    agent_definition_table,
    agent_definition_version_table,
    agent_event_inbox_table,
    agent_graph_checkpoint_table,
    agent_graph_write_table,
    agent_ingest_job_table,
    agent_message_segment_asset_table,
    agent_message_segment_table,
    agent_provider_credential_table,
    agent_provider_credential_version_table,
    agent_resource_chunk_table,
    agent_resource_table,
    agent_run_citation_table,
    agent_run_content_block_table,
    agent_run_message_table,
    agent_run_step_table,
    agent_run_table,
    agent_run_turn_table,
    agent_tool_call_table,
    agent_vector_collection_table,
    metadata,
)

__all__ = [
    "agent_asset_lifecycle_table",
    "agent_conversation_binding_table",
    "agent_definition_table",
    "agent_definition_version_table",
    "agent_event_inbox_table",
    "agent_graph_checkpoint_table",
    "agent_graph_write_table",
    "agent_ingest_job_table",
    "agent_message_segment_asset_table",
    "agent_message_segment_table",
    "agent_provider_credential_table",
    "agent_provider_credential_version_table",
    "agent_resource_chunk_table",
    "agent_resource_table",
    "agent_run_citation_table",
    "agent_run_content_block_table",
    "agent_run_message_table",
    "agent_run_step_table",
    "agent_run_table",
    "agent_run_turn_table",
    "agent_tool_call_table",
    "agent_vector_collection_table",
    "metadata",
]
