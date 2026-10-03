"""创建 Pi 风格 Agent 运行时结构并升级 Thinking 配置。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/08/12 22:16
"""

import sqlalchemy as sa

from alembic import op
from union_talk_agent.infrastructure.postgres.tables import (
    agent_run_content_block_table,
    agent_run_message_table,
    agent_run_turn_table,
)

revision = "20260812_0004"
down_revision = "20260807_0003"
branch_labels = None
depends_on = None


def upgrade() -> None:
    """
    一次性升级 Agent Runtime V2

    :return: 无返回值
    """

    bind = op.get_bind()
    agent_run_turn_table.create(bind=bind, checkfirst=True)
    agent_run_message_table.create(bind=bind, checkfirst=True)
    agent_run_content_block_table.create(bind=bind, checkfirst=True)
    inspector = sa.inspect(bind)
    definition_columns = {
        column["name"]
        for column in inspector.get_columns(
            "ut_agent_definition_version", schema="agent"
        )
    }
    tool_call_columns = {
        column["name"]
        for column in inspector.get_columns("ut_agent_tool_call", schema="agent")
    }
    if "thinking_enabled" not in definition_columns:
        op.add_column(
            "ut_agent_definition_version",
            sa.Column("thinking_enabled", sa.Boolean, comment="是否启用模型Thinking"),
            schema="agent",
        )
    if "thinking_effort" not in definition_columns:
        op.add_column(
            "ut_agent_definition_version",
            sa.Column("thinking_effort", sa.String(16), comment="模型Thinking强度"),
            schema="agent",
        )
    if "turn_id" not in tool_call_columns:
        op.add_column(
            "ut_agent_tool_call",
            sa.Column("turn_id", sa.BigInteger, comment="Agent Turn ID"),
            schema="agent",
        )
    if "message_id" not in tool_call_columns:
        op.add_column(
            "ut_agent_tool_call",
            sa.Column("message_id", sa.BigInteger, comment="运行时消息ID"),
            schema="agent",
        )


def downgrade() -> None:
    """
    删除 Agent Runtime V2 新增结构

    :return: 无返回值
    """

    bind = op.get_bind()
    agent_run_content_block_table.drop(bind=bind, checkfirst=True)
    agent_run_message_table.drop(bind=bind, checkfirst=True)
    agent_run_turn_table.drop(bind=bind, checkfirst=True)
    op.drop_column("ut_agent_tool_call", "message_id", schema="agent")
    op.drop_column("ut_agent_tool_call", "turn_id", schema="agent")
    op.drop_column("ut_agent_definition_version", "thinking_effort", schema="agent")
    op.drop_column("ut_agent_definition_version", "thinking_enabled", schema="agent")
