"""创建资产生命周期投影。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/08/13 17:34
"""

from alembic import op
from union_talk_agent.infrastructure.postgres.tables import agent_asset_lifecycle_table

revision = "20260813_0006"
down_revision = "20260812_0005"
branch_labels = None
depends_on = None


def upgrade() -> None:
    """
    创建资产生命周期投影表与普通索引

    :return: 无返回值
    """

    agent_asset_lifecycle_table.create(bind=op.get_bind(), checkfirst=True)


def downgrade() -> None:
    """
    删除资产生命周期投影表

    :return: 无返回值
    """

    agent_asset_lifecycle_table.drop(bind=op.get_bind(), checkfirst=True)
