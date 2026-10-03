"""创建 Agent 控制面、运行态和 RAG 元数据表。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/29 18:54
"""

from alembic import op
from union_talk_agent.infrastructure.postgres.tables import metadata

revision = "20260729_0001"
down_revision = None
branch_labels = None
depends_on = None


def upgrade() -> None:
    """
    创建 schema、表、字段注释和普通索引

    :return: 无返回值
    """

    op.execute("CREATE SCHEMA IF NOT EXISTS agent")
    metadata.create_all(bind=op.get_bind(), checkfirst=True)


def downgrade() -> None:
    """
    删除本版本创建的 Agent 表

    :return: 无返回值
    """

    metadata.drop_all(bind=op.get_bind(), checkfirst=True)
