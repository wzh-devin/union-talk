"""SQLAlchemy Core 动态列的静态类型边界。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/30 11:00
"""

from typing import Any, cast

import sqlalchemy as sa
from sqlalchemy.sql.elements import ColumnElement


def table_column(table: sa.Table, column_name: str) -> ColumnElement[Any]:
    """
    返回可被 IDE 正确识别运算符重载的表列

    :param table: SQLAlchemy Table
    :param column_name: 数据库列名称
    :return: 支持 SQLAlchemy 运算符重载的列表达式
    """

    return cast(ColumnElement[Any], table.c[column_name])
