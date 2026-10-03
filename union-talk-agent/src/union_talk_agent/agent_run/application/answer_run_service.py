"""单个 Agent Run 的租约执行编排。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/29 19:02
"""

import asyncio
import logging

from sqlalchemy.exc import SQLAlchemyError

from union_talk_agent.agent_run.application.ports.agent_unit_of_work import (
    AgentUnitOfWorkFactory,
)
from union_talk_agent.agent_run.application.run_lifecycle_service import RunLifecycleService
from union_talk_agent.agent_run.application.runtime_config_resolver import RuntimeConfigLoader
from union_talk_agent.agent_run.domain.enums import AgentRunStatus
from union_talk_agent.agent_run.domain.exceptions import (
    AgentDomainError,
    AgentErrorCode,
)
from union_talk_agent.agent_run.graph.answer_graph import AnswerGraph

logger = logging.getLogger(__name__)


class AnswerRunService:
    """认领、续租并执行回答图。"""

    def __init__(
        self,
        *,
        unit_of_work_factory: AgentUnitOfWorkFactory,
        runtime_config_loader: RuntimeConfigLoader,
        answer_graph: AnswerGraph,
        lifecycle_service: RunLifecycleService,
        worker_id: str,
        lease_seconds: int,
        heartbeat_seconds: int,
    ) -> None:
        """
        初始化 AnswerRunService

        :param unit_of_work_factory: Agent 工作单元工厂
        :param runtime_config_loader: Agent 运行配置加载器
        :param answer_graph: Agent 回答图
        :param lifecycle_service: Agent Run 生命周期服务
        :param worker_id: 当前 Worker 标识
        :param lease_seconds: 执行租约有效秒数
        :param heartbeat_seconds: 执行租约心跳间隔秒数
        :return: 无返回值
        """

        self._unit_of_work_factory = unit_of_work_factory
        self._runtime_config_loader = runtime_config_loader
        self._answer_graph = answer_graph
        self._lifecycle_service = lifecycle_service
        self._worker_id = worker_id
        self._lease_seconds = lease_seconds
        self._heartbeat_seconds = heartbeat_seconds

    async def execute(self, run_id: int) -> bool:
        """
        尝试认领并执行 Run

        :param run_id: Agent Run ID
        :return: 当前调用是否实际认领了 Run；有效租约已被其他 Worker 持有时返回假
        """

        async with self._unit_of_work_factory() as unit_of_work:
            claimed_run = await unit_of_work.agent_runs.claim_execution(
                run_id,
                self._worker_id,
                self._lease_seconds,
            )
            await unit_of_work.commit()
        if claimed_run is None:
            return False

        await self._lifecycle_service.publish_started(claimed_run)
        initial_turn_no = await self._lifecycle_service.recover_interrupted_runtime(claimed_run)
        stop_heartbeat = asyncio.Event()
        heartbeat_task = asyncio.create_task(
            self._heartbeat_loop(claimed_run.run_id, stop_heartbeat),
            name=f"agent-run-heartbeat-{claimed_run.run_id}",
        )
        try:
            runtime_config = await self._runtime_config_loader.load(claimed_run)
            answer_message_id = await self._answer_graph.execute(
                claimed_run,
                runtime_config,
                initial_turn_no,
            )
            await self._lifecycle_service.succeed(claimed_run, answer_message_id)
            return True
        finally:
            stop_heartbeat.set()
            await heartbeat_task

    async def requeue(
        self,
        run_id: int,
        error_code: str,
        error_message: str,
    ) -> None:
        """
        释放租约并将可重试 Run 放回 QUEUED

        :param run_id: Agent Run ID
        :param error_code: 错误码
        :param error_message: 脱敏错误摘要
        :return: 无返回值
        """

        async with self._unit_of_work_factory() as unit_of_work:
            await unit_of_work.business_locks.lock("agent-run", str(run_id))
            run = await unit_of_work.agent_runs.get_by_id(run_id)
            if run is None:
                raise AgentDomainError(AgentErrorCode.RUN_NOT_FOUND, "Agent Run不存在")
            if run.status in {AgentRunStatus.QUEUED, AgentRunStatus.RUNNING}:
                run.requeue(error_code, error_message[:500])
                await unit_of_work.agent_runs.update(run)
            await unit_of_work.commit()

    async def fail(
        self,
        run_id: int,
        error_code: str,
        error_message: str,
    ) -> None:
        """
        将不可重试或耗尽重试的 Run 标记为失败

        :param run_id: Agent Run ID
        :param error_code: 错误码
        :param error_message: 脱敏错误摘要
        :return: 无返回值
        """

        async with self._unit_of_work_factory() as unit_of_work:
            run = await unit_of_work.agent_runs.get_by_id(run_id)
        if run is None:
            raise AgentDomainError(AgentErrorCode.RUN_NOT_FOUND, "Agent Run不存在")
        if run.is_terminal:
            return
        await self._lifecycle_service.fail(run, error_code, error_message)

    async def _heartbeat_loop(self, run_id: int, stop_event: asyncio.Event) -> None:
        """
        定期续租当前 Agent Run 执行租约

        :param run_id: Agent Run ID
        :param stop_event: 进程协作式停止事件
        :return: 无返回值
        """

        while not stop_event.is_set():
            try:
                await asyncio.wait_for(
                    stop_event.wait(),
                    timeout=self._heartbeat_seconds,
                )
                return
            except TimeoutError:
                pass
            try:
                async with self._unit_of_work_factory() as unit_of_work:
                    renewed = await unit_of_work.agent_runs.renew_execution_lease(
                        run_id,
                        self._worker_id,
                        self._lease_seconds,
                    )
                    await unit_of_work.commit()
                if not renewed:
                    logger.warning("Agent Run执行租约续租失败, runId=%s", run_id)
                    return
            except (AgentDomainError, SQLAlchemyError):
                logger.warning("Agent Run心跳异常, runId=%s", run_id, exc_info=True)
