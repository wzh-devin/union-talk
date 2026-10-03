"""Agent Run 回答图生命周期端口。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/30 10:43
"""

from abc import ABC, abstractmethod

from union_talk_agent.agent_run.domain.agent_run import AgentRun
from union_talk_agent.agent_run.domain.agent_runtime import (
    AgentRunContentBlock,
    AgentRunMessage,
    AgentRunTurn,
    AgentToolExecution,
)
from union_talk_agent.agent_run.domain.agent_step import AgentStep
from union_talk_agent.agent_run.domain.chat_model import ChatModelEvent, ChatToolCall
from union_talk_agent.agent_run.domain.enums import (
    AgentContentBlockType,
    AgentRunStage,
    AgentStepType,
    TraceVisibility,
)


class RunLifecycle(ABC):
    """回答图记录阶段、步骤和实时内容所需的最小能力。"""

    @abstractmethod
    async def recover_interrupted_runtime(self, run: AgentRun) -> int:
        """
        关闭租约过期的旧运行时并返回已使用的最大 Turn 序号

        :param run: 已重新认领的 Agent Run
        :return: 已使用的最大 Turn 序号
        """

        raise NotImplementedError

    @abstractmethod
    async def start_step(
        self,
        run: AgentRun,
        *,
        sequence_no: int,
        stage: AgentRunStage,
        step_type: AgentStepType,
        step_code: str,
        display_name: str,
        visibility: TraceVisibility = TraceVisibility.MEMBER,
    ) -> AgentStep:
        """
        切换阶段并创建步骤

        :param run: 当前 Agent Run
        :param sequence_no: 执行步骤顺序号
        :param stage: Agent Run 执行阶段
        :param step_type: 执行步骤类型
        :param step_code: 执行步骤代码
        :param display_name: 步骤展示名称
        :param visibility: 步骤可见范围
        :return: 已进入运行中的 Agent 执行步骤
        """

        raise NotImplementedError

    @abstractmethod
    async def complete_step(
        self,
        step: AgentStep,
        *,
        input_summary: dict[str, object] | None = None,
        output_summary: dict[str, object] | None = None,
    ) -> None:
        """
        完成步骤

        :param step: Agent 执行步骤
        :param input_summary: 脱敏输入摘要
        :param output_summary: 脱敏输出摘要
        :return: 无返回值
        """

        raise NotImplementedError

    @abstractmethod
    async def fail_step(
        self,
        step: AgentStep,
        error_code: str,
        error_message: str,
    ) -> None:
        """
        记录步骤失败

        :param step: Agent 执行步骤
        :param error_code: 错误码
        :param error_message: 脱敏错误摘要
        :return: 无返回值
        """

        raise NotImplementedError

    @abstractmethod
    async def start_turn(
        self,
        run: AgentRun,
        turn_no: int,
        model_id: str,
    ) -> tuple[AgentRunTurn, AgentRunMessage]:
        """
        创建 Turn 和 partial assistant message

        :param run: 当前 Agent Run
        :param turn_no: Turn 顺序号
        :param model_id: 模型标识
        :return: 已持久化的 Turn 和运行时消息
        """

        raise NotImplementedError

    @abstractmethod
    async def publish_model_event(
        self,
        run: AgentRun,
        turn: AgentRunTurn,
        message: AgentRunMessage,
        model_event: ChatModelEvent,
        tool_call_id: str | None = None,
        tool_name: str | None = None,
    ) -> None:
        """
        把 Provider 事件立即投影为 message_update

        :param run: 当前 Agent Run
        :param turn: 当前 Agent Turn
        :param message: 当前 partial assistant message
        :param model_event: Provider 判别联合事件
        :param tool_call_id: 当前工具调用 ID
        :param tool_name: 当前工具名称
        :return: 无返回值
        """

        raise NotImplementedError

    @abstractmethod
    async def persist_content_block(self, block: AgentRunContentBlock) -> None:
        """
        在内容边界结束时写入完整 Block

        :param block: 已完成内容块
        :return: 无返回值
        """

        raise NotImplementedError

    @abstractmethod
    def create_content_block(
        self,
        run: AgentRun,
        turn: AgentRunTurn,
        message: AgentRunMessage,
        content_index: int,
        block_type: AgentContentBlockType,
        tool_call_id: str | None = None,
        tool_name: str | None = None,
    ) -> AgentRunContentBlock:
        """
        创建仅在内存累积的运行时内容块

        :param run: 当前 Agent Run
        :param turn: 当前 Agent Turn
        :param message: 当前运行时消息
        :param content_index: 内容块顺序号
        :param block_type: 内容块类型
        :param tool_call_id: 模型工具调用 ID
        :param tool_name: 模型工具名称
        :return: 尚未持久化的内容块
        """

        raise NotImplementedError

    @abstractmethod
    async def complete_turn(
        self,
        run: AgentRun,
        turn: AgentRunTurn,
        message: AgentRunMessage,
        stop_reason: str | None,
        input_tokens: int,
        output_tokens: int,
    ) -> None:
        """
        完成 Turn、Message 并发布边界事件

        :param run: 当前 Agent Run
        :param turn: 当前 Agent Turn
        :param message: 当前运行时消息
        :param stop_reason: Provider 停止原因
        :param input_tokens: 输入 Token 数量
        :param output_tokens: 输出 Token 数量
        :return: 无返回值
        """

        raise NotImplementedError

    @abstractmethod
    async def start_tool_execution(
        self,
        run: AgentRun,
        turn: AgentRunTurn,
        message: AgentRunMessage,
        step: AgentStep,
        tool_call: ChatToolCall,
        display_name: str,
        arguments_summary: dict[str, object],
    ) -> AgentToolExecution:
        """
        持久化并发布工具执行开始事件

        :param run: 当前 Agent Run
        :param turn: 工具所属 Turn
        :param message: 工具所属运行时消息
        :param step: 对应业务步骤
        :param tool_call: 模型工具调用
        :param display_name: 工具显示名称
        :param arguments_summary: 脱敏参数摘要
        :return: 运行中的工具执行
        """

        raise NotImplementedError

    @abstractmethod
    async def complete_tool_execution(
        self,
        run: AgentRun,
        execution: AgentToolExecution,
        result_summary: dict[str, object],
    ) -> None:
        """
        完成工具执行并发布结果摘要

        :param run: 当前 Agent Run
        :param execution: 当前工具执行
        :param result_summary: 成员可见结果摘要
        :return: 无返回值
        """

        raise NotImplementedError

    @abstractmethod
    async def fail_tool_execution(
        self,
        run: AgentRun,
        execution: AgentToolExecution,
        error_code: str,
        error_message: str,
    ) -> None:
        """
        标记工具执行失败并发布错误摘要

        :param run: 当前 Agent Run
        :param execution: 当前工具执行
        :param error_code: 稳定错误码
        :param error_message: 脱敏错误摘要
        :return: 无返回值
        """

        raise NotImplementedError

    @abstractmethod
    async def publish_citations(
        self,
        run: AgentRun,
        citation_list: list[dict[str, object]],
    ) -> None:
        """
        发布引用列表

        :param run: 当前 Agent Run
        :param citation_list: Agent 回答引用列表
        :return: 无返回值
        """

        raise NotImplementedError
