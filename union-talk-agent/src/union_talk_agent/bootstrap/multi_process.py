"""Agent 生产多进程统一入口。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/10/05 00:00
"""

from __future__ import annotations

import logging
import shutil
import signal
import subprocess
import time
from collections.abc import Sequence
from dataclasses import dataclass
from threading import Event
from types import FrameType

from union_talk_agent.settings import Settings, load_settings

logger = logging.getLogger(__name__)

_SHUTDOWN_TIMEOUT_SECONDS = 15


@dataclass(frozen=True, slots=True)
class ProcessSpec:
    """描述一个由统一入口托管的 Agent 子进程。"""

    name: str
    command: str


def build_process_specs(settings: Settings) -> tuple[ProcessSpec, ...]:
    """
    按 Agent 配置生成需要启动的进程列表

    :param settings: Agent 全部运行配置
    :return: 需要由生产入口托管的进程规格
    """

    process_spec_list = [
        ProcessSpec(name="agent-api", command="agent-api"),
        ProcessSpec(name="embedding-api", command="embedding-api"),
    ]
    if settings.app.answer_worker_enabled:
        process_spec_list.append(ProcessSpec(name="answer-worker", command="answer-worker"))
    if settings.app.index_worker_enabled:
        process_spec_list.append(ProcessSpec(name="index-worker", command="index-worker"))
    if settings.app.reconciliation_worker_enabled:
        process_spec_list.append(
            ProcessSpec(name="reconciliation-worker", command="reconciliation-worker")
        )
    return tuple(process_spec_list)


def _start_processes(
    process_spec_list: Sequence[ProcessSpec],
) -> list[tuple[ProcessSpec, subprocess.Popen[bytes]]]:
    """
    启动并记录全部 Agent 子进程

    :param process_spec_list: 待启动的进程规格
    :return: 进程规格和子进程句柄
    :raises RuntimeError: 找不到已安装的进程入口
    """

    process_list: list[tuple[ProcessSpec, subprocess.Popen[bytes]]] = []
    try:
        for process_spec in process_spec_list:
            executable = shutil.which(process_spec.command)
            if executable is None:
                raise RuntimeError(f"找不到 Agent 进程入口: {process_spec.command}")
            process = subprocess.Popen([executable], stdin=subprocess.DEVNULL)
            process_list.append((process_spec, process))
            logger.info("Agent 子进程已启动: %s, pid=%s", process_spec.name, process.pid)
    except Exception:
        _stop_processes(process_list)
        raise
    return process_list


def _stop_processes(
    process_list: Sequence[tuple[ProcessSpec, subprocess.Popen[bytes]]],
) -> None:
    """
    向全部子进程发送停止信号并在超时后强制回收

    :param process_list: 需要停止的子进程
    :return: 无返回值
    """

    for _process_spec, process in process_list:
        if process.poll() is None:
            try:
                process.terminate()
            except OSError:
                pass

    deadline = time.monotonic() + _SHUTDOWN_TIMEOUT_SECONDS
    while time.monotonic() < deadline:
        if all(process.poll() is not None for _process_spec, process in process_list):
            break
        time.sleep(0.1)

    for process_spec, process in process_list:
        if process.poll() is None:
            logger.warning(
                "Agent 子进程未及时退出，强制终止: %s, pid=%s", process_spec.name, process.pid
            )
            try:
                process.kill()
            except OSError:
                pass

    for _process_spec, process in process_list:
        try:
            process.wait()
        except OSError:
            pass


def _wait_for_processes(
    process_list: Sequence[tuple[ProcessSpec, subprocess.Popen[bytes]]],
    stop_event: Event,
) -> int:
    """
    监控子进程并在异常退出时返回失败状态

    :param process_list: 正在运行的子进程
    :param stop_event: 容器停止事件
    :return: 0 表示收到正常停止请求，1 表示子进程异常退出
    """

    while not stop_event.wait(0.5):
        for process_spec, process in process_list:
            exit_code = process.poll()
            if exit_code is not None:
                logger.error("Agent 子进程已退出: %s, exit_code=%s", process_spec.name, exit_code)
                return 1
    return 0


def main() -> int:
    """
    启动并托管 Agent API、Embedding 和 Worker 进程

    :return: Docker 容器退出状态码
    """

    settings = load_settings()
    logging.basicConfig(
        level=settings.app.log_level.upper(),
        format="%(asctime)s %(levelname)s %(name)s %(message)s",
    )
    process_spec_list = build_process_specs(settings)
    stop_event = Event()

    def request_stop(_signal_number: int, _frame: FrameType | None) -> None:
        """
        将容器停止信号转换为统一退出事件

        :param _signal_number: 收到的系统信号编号
        :param _frame: 当前 Python 栈帧
        :return: 无返回值
        """

        stop_event.set()

    previous_handlers = {
        signal_number: signal.getsignal(signal_number)
        for signal_number in (signal.SIGINT, signal.SIGTERM)
    }
    for signal_number in previous_handlers:
        signal.signal(signal_number, request_stop)

    process_list: list[tuple[ProcessSpec, subprocess.Popen[bytes]]] = []
    exit_code = 1
    try:
        logger.info(
            "启动 Agent 统一进程入口: %s",
            ", ".join(process_spec.name for process_spec in process_spec_list),
        )
        process_list = _start_processes(process_spec_list)
        exit_code = _wait_for_processes(process_list, stop_event)
    except KeyboardInterrupt:
        stop_event.set()
        exit_code = 0
    except Exception:
        logger.exception("Agent 统一进程入口启动失败")
    finally:
        _stop_processes(process_list)
        for signal_number, handler in previous_handlers.items():
            signal.signal(signal_number, handler)
    return exit_code


if __name__ == "__main__":
    raise SystemExit(main())
