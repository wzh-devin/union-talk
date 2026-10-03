"""扩展消息片段发送者元数据。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/08/12 23:54
"""

import sqlalchemy as sa

from alembic import op

revision = "20260812_0005"
down_revision = "20260812_0004"
branch_labels = None
depends_on = None


def upgrade() -> None:
    """
    增加消息发送者字段与普通检索索引

    :return: 无返回值
    """

    bind = op.get_bind()
    inspector = sa.inspect(bind)
    columns = {
        column["name"]
        for column in inspector.get_columns("ut_agent_message_segment", schema="agent")
    }
    indexes = {
        index["name"]
        for index in inspector.get_indexes("ut_agent_message_segment", schema="agent")
    }
    if "sender_id" not in columns:
        op.add_column(
            "ut_agent_message_segment",
            sa.Column("sender_id", sa.BigInteger, comment="消息发送者ID"),
            schema="agent",
        )
    if "sender_display_name" not in columns:
        op.add_column(
            "ut_agent_message_segment",
            sa.Column("sender_display_name", sa.String(128), comment="发送者展示名称"),
            schema="agent",
        )
    if "idx_agent_message_segment_sender_time" not in indexes:
        op.create_index(
            "idx_agent_message_segment_sender_time",
            "ut_agent_message_segment",
            ["conversation_id", "sender_id", "start_at"],
            unique=False,
            schema="agent",
        )


def downgrade() -> None:
    """
    删除消息发送者检索字段

    :return: 无返回值
    """

    op.drop_index(
        "idx_agent_message_segment_sender_time",
        table_name="ut_agent_message_segment",
        schema="agent",
    )
    op.drop_column("ut_agent_message_segment", "sender_display_name", schema="agent")
    op.drop_column("ut_agent_message_segment", "sender_id", schema="agent")
