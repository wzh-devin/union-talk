"""跨服务事件时间类型。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/08/14 02:30
"""

from datetime import UTC, datetime
from typing import Annotated
from zoneinfo import ZoneInfo

from pydantic import AfterValidator

from union_talk_agent.interfaces.mq.constants import LEGACY_EVENT_TIMEZONE_NAME


def to_utc_event_datetime(value: datetime) -> datetime:
    """
    将跨服务事件时间转换为 UTC 时间点

    :param value: 已由 Pydantic 解析的事件时间
    :return: 带 UTC 时区的事件时间
    """

    if value.tzinfo is None or value.utcoffset() is None:
        value = value.replace(tzinfo=ZoneInfo(LEGACY_EVENT_TIMEZONE_NAME))
    return value.astimezone(UTC)


EventDatetime = Annotated[datetime, AfterValidator(to_utc_event_datetime)]
