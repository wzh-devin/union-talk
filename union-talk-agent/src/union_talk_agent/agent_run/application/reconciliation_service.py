"""卡死或丢失 MQ 投递的 Agent Run 对账。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/29 19:08
"""

import logging

from sqlalchemy.exc import SQLAlchemyError

from union_talk_agent.agent_run.application.answer_run_service import AnswerRunService
from union_talk_agent.agent_run.application.ports.agent_unit_of_work import (
    AgentUnitOfWorkFactory,
)
from union_talk_agent.agent_run.domain.exceptions import AgentDomainError, RetryableAgentError

logger = logging.getLogger(__name__)


class ReconciliationService:
    """扫描 QUEUED 和租约过期 Run，并复用同一执行入口修复。"""

    def __init__(
        self,
        unit_of_work_factory: AgentUnitOfWorkFactory,
        answer_run_service: AnswerRunService,
    ) -> None:
        """
        初始化 ReconciliationService

        :param unit_of_work_factory: Agent 工作单元工厂
        :param answer_run_service: Agent 回答执行服务
        :return: 无返回值
        """

        self._unit_of_work_factory = unit_of_work_factory
        self._answer_run_service = answer_run_service

    async def execute_batch(self, batch_size: int = 50) -> int:
        """
        执行一批对账并返回实际认领数量

        :param batch_size: 单批处理数量上限
        :return: 本批实际认领并重新投递的 Run 数量
        """

        async with self._unit_of_work_factory() as unit_of_work:
            run_list = await unit_of_work.agent_runs.list_reconcilable(batch_size)
            await unit_of_work.commit()
        executed_count = 0
        for run in run_list:
            try:
                if await self._answer_run_service.execute(run.run_id):
                    executed_count += 1
            except RetryableAgentError as error:
                await self._answer_run_service.requeue(
                    run.run_id,
                    str(error.code),
                    str(error),
                )
                logger.warning(
                    "Agent Run对账遇到可重试异常, runId=%s, errorCode=%s",
                    run.run_id,
                    str(error.code),
                )
            except AgentDomainError as error:
                await self._answer_run_service.fail(
                    run.run_id,
                    str(error.code),
                    str(error),
                )
            except SQLAlchemyError:
                logger.exception("Agent Run对账出现未分类异常, runId=%s", run.run_id)
        return executed_count
