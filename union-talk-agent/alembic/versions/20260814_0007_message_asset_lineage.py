"""创建消息片段资产血缘。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/08/14 00:00
"""

from alembic import op
from union_talk_agent.infrastructure.postgres.tables import (
    agent_message_segment_asset_table,
)

revision = "20260814_0007"
down_revision = "20260813_0006"
branch_labels = None
depends_on = None


def upgrade() -> None:
    """
    创建消息资产血缘表

    :return: 无返回值
    """

    agent_message_segment_asset_table.create(bind=op.get_bind(), checkfirst=True)


def downgrade() -> None:
    """
    删除消息资产血缘表

    :return: 无返回值
    """

    agent_message_segment_asset_table.drop(bind=op.get_bind(), checkfirst=True)
