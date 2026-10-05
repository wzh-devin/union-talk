"""Agent 统一多进程入口测试。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/10/05 00:00
"""

from types import SimpleNamespace
from typing import cast

from union_talk_agent.bootstrap.multi_process import build_process_specs
from union_talk_agent.settings import Settings


def test_build_process_specs_starts_all_enabled_processes() -> None:
    """
    验证全部开关打开时统一入口包含五个 Agent 进程

    :return: 无返回值
    """

    settings = cast(
        Settings,
        SimpleNamespace(
            app=SimpleNamespace(
                answer_worker_enabled=True,
                index_worker_enabled=True,
                reconciliation_worker_enabled=True,
            )
        ),
    )

    process_spec_list = build_process_specs(settings)

    assert [process_spec.command for process_spec in process_spec_list] == [
        "agent-api",
        "embedding-api",
        "answer-worker",
        "index-worker",
        "reconciliation-worker",
    ]


def test_build_process_specs_skips_disabled_workers() -> None:
    """
    验证关闭 Worker 时不会启动对应子进程

    :return: 无返回值
    """

    settings = cast(
        Settings,
        SimpleNamespace(
            app=SimpleNamespace(
                answer_worker_enabled=False,
                index_worker_enabled=False,
                reconciliation_worker_enabled=True,
            )
        ),
    )

    process_spec_list = build_process_specs(settings)

    assert [process_spec.command for process_spec in process_spec_list] == [
        "agent-api",
        "embedding-api",
        "reconciliation-worker",
    ]
