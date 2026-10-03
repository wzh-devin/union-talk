"""PostgreSQL 无约束建模契约测试。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/29 19:21
"""

from sqlalchemy.dialects import postgresql
from sqlalchemy.schema import CreateTable

from union_talk_agent.infrastructure.postgres.tables import metadata


def test_agent_tables_only_have_normal_indexes_and_comments() -> None:
    """
    所有业务表字段可空、带注释且没有数据库约束

    :return: 无返回值
    """

    expected_control_plane_table_set = {
        "agent.ut_agent_definition",
        "agent.ut_agent_definition_version",
        "agent.ut_agent_provider_credential",
        "agent.ut_agent_provider_credential_version",
        "agent.ut_agent_conversation_binding",
    }
    assert len(metadata.tables) == 22
    assert {
        "agent.ut_agent_run_turn",
        "agent.ut_agent_run_message",
        "agent.ut_agent_run_content_block",
    }.issubset(metadata.tables)
    assert expected_control_plane_table_set.issubset(metadata.tables)
    assert "agent.ut_agent_asset_lifecycle" in metadata.tables
    assert "agent.ut_agent_message_segment_asset" in metadata.tables
    for table in metadata.tables.values():
        assert table.comment
        assert table.indexes
        assert all(not index.unique for index in table.indexes)
        for column in table.columns:
            assert column.comment
            assert column.nullable
            assert not column.primary_key
            assert not column.unique
        ddl = str(CreateTable(table).compile(dialect=postgresql.dialect())).upper()
        assert "PRIMARY KEY" not in ddl
        assert "NOT NULL" not in ddl
        assert "FOREIGN KEY" not in ddl
        assert " UNIQUE " not in ddl
        assert " CHECK " not in ddl
