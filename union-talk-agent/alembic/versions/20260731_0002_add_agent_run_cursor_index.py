"""增加 Agent Run 游标分页索引。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/31 15:28
"""

from alembic import op

revision = "20260731_0002"
down_revision = "20260729_0001"
branch_labels = None
depends_on = None


def upgrade() -> None:
    """
    创建会话 Run 双游标普通索引

    :return: 无返回值
    """

    op.create_index(
        "idx_agent_run_conversation_queued_id",
        "ut_agent_run",
        ["conversation_id", "queued_at", "id"],
        unique=False,
        schema="agent",
        if_not_exists=True,
    )


def downgrade() -> None:
    """
    删除会话 Run 双游标普通索引

    :return: 无返回值
    """

    op.drop_index(
        "idx_agent_run_conversation_queued_id",
        table_name="ut_agent_run",
        schema="agent",
        if_exists=True,
    )
