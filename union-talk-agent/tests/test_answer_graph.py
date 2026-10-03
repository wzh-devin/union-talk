"""Pi 风格 LangGraph 回答主流程测试。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/08/12 23:50
"""

from collections.abc import AsyncIterator, Callable
from typing import cast

from tests.conftest import build_conversation_config
from union_talk_agent.agent_config.domain.agent_config import RuntimeAgentConfig
from union_talk_agent.agent_config.domain.enums import ProviderType
from union_talk_agent.agent_config.domain.model_config import RuntimeModelConfig
from union_talk_agent.agent_run.application.ports.knowledge_retriever import (
    MessageSearchQuery,
    ResourceSearchQuery,
)
from union_talk_agent.agent_run.application.ports.run_lifecycle import RunLifecycle
from union_talk_agent.agent_run.application.prompt_assembler import PromptAssembler
from union_talk_agent.agent_run.domain.agent_citation import AgentCitation
from union_talk_agent.agent_run.domain.agent_run import AgentRun
from union_talk_agent.agent_run.domain.agent_runtime import (
    AgentRunContentBlock,
    AgentRunMessage,
    AgentRunTurn,
    AgentToolExecution,
)
from union_talk_agent.agent_run.domain.agent_step import AgentStep
from union_talk_agent.agent_run.domain.chat_model import (
    AgentReplyResult,
    ChatMessage,
    ChatModelEvent,
    ChatToolCall,
    ChatToolCallDelta,
    ChatToolDefinition,
)
from union_talk_agent.agent_run.domain.commands import CreateAgentReplyCommand
from union_talk_agent.agent_run.domain.conversation_context import ConversationContext
from union_talk_agent.agent_run.domain.enums import (
    AgentContentBlockType,
    AgentRunStage,
    AgentStepType,
    ProviderEventType,
    TraceVisibility,
)
from union_talk_agent.agent_run.graph.answer_graph import AnswerGraph
from union_talk_agent.infrastructure.model.fixed_answer_chat_model import FixedAnswerChatModel
from union_talk_agent.knowledge.domain.enums import ResourceStatus, SourceType
from union_talk_agent.knowledge.domain.retrieval_evidence import RetrievalEvidence


def test_message_citation_uses_chat_message_id_instead_of_segment_id() -> None:
    """
    消息引用必须输出正式聊天消息 ID 供后续血缘继承

    :return: 无返回值
    """

    build_citations = cast(
        Callable[[list[RetrievalEvidence]], list[AgentCitation]],
        AnswerGraph.__dict__["_build_citations"],
    )
    citation_list = build_citations(
        [
            RetrievalEvidence(
                source_type=SourceType.MESSAGE_SEGMENT,
                source_id=101,
                message_id=202,
                text_content="历史回答",
                score=0.9,
            )
        ]
    )

    assert citation_list[0].message_id == 202


class FakeConversationReader:
    """返回固定当前问题。"""

    async def get_agent_context(
        self,
        conversation_id: int,
        trigger_message_id: int,
        recent_message_limit: int,
    ) -> ConversationContext:
        """
        构造紧凑上下文

        :param conversation_id: 会话 ID
        :param trigger_message_id: 触发消息 ID
        :param recent_message_limit: 最近消息数量上限
        :return: 包含固定问题的测试会话上下文
        """

        assert recent_message_limit > 0
        return ConversationContext(
            conversation_id=conversation_id,
            trigger_message_id=trigger_message_id,
            question="项目结论是什么？",
        )


class FakeKnowledgeRetriever:
    """记录分类检索并返回资源证据。"""

    def __init__(
        self,
        *,
        resource_status: ResourceStatus = ResourceStatus.READY,
        resource_result_enabled: bool = True,
    ) -> None:
        """
        初始化检索记录

        :param resource_status: 空召回时返回的资源索引状态
        :param resource_result_enabled: 是否返回固定资源证据
        :return: 无返回值
        """

        self.resource_query: ResourceSearchQuery | None = None
        self.resource_status = resource_status
        self.resource_result_enabled = resource_result_enabled

    async def search_messages(
        self,
        search_query: MessageSearchQuery,
    ) -> list[RetrievalEvidence]:
        """
        返回空消息证据

        :param search_query: 消息检索条件
        :return: 空列表
        """

        _ignored_query = search_query
        return []

    async def list_invalid_message_ids(
        self,
        conversation_id: int,
        message_id_list: list[int],
    ) -> set[int]:
        """
        返回空失效消息集合

        :param conversation_id: 会话 ID
        :param message_id_list: 待检查消息 ID 列表
        :return: 空消息 ID 集合
        """

        _ignored_arguments = (conversation_id, message_id_list)
        return set()

    async def search_resources(
        self,
        search_query: ResourceSearchQuery,
    ) -> list[RetrievalEvidence]:
        """
        返回单条资源证据

        :param search_query: 资源检索条件
        :return: 通过回表校验的资源证据
        """

        self.resource_query = search_query
        if not self.resource_result_enabled:
            return []
        return [
            RetrievalEvidence(
                source_type=SourceType.RESOURCE_CHUNK,
                source_id=88,
                text_content="项目已经完成MVP。",
                score=0.9,
                asset_file_id=77,
                resource_version=1,
                evidence_key="R1",
            )
        ]

    async def expand_resource_context(
        self,
        conversation_id: int,
        chunk_id_list: list[int],
        token_budget: int,
    ) -> list[RetrievalEvidence]:
        """
        返回空扩展结果

        :param conversation_id: 会话 ID
        :param chunk_id_list: Chunk ID 列表
        :param token_budget: Token 预算
        :return: 空列表
        """

        _ignored_arguments = (conversation_id, chunk_id_list, token_budget)
        return []

    async def get_resource_status(
        self,
        conversation_id: int,
        resource_id_list: list[int],
    ) -> ResourceStatus | None:
        """
        返回测试资源的当前索引状态

        :param conversation_id: 会话 ID
        :param resource_id_list: 资源文件 ID 列表
        :return: 资源已就绪状态
        """

        _ignored_arguments = (conversation_id, resource_id_list)
        return self.resource_status


class FakeReplyWriter:
    """记录正式消息写回命令。"""

    def __init__(self) -> None:
        """
        初始化命令记录

        :return: 无返回值
        """

        self.command: CreateAgentReplyCommand | None = None

    async def create_agent_reply(
        self,
        command: CreateAgentReplyCommand,
    ) -> AgentReplyResult:
        """
        返回固定正式消息 ID

        :param command: Agent 回复命令
        :return: 固定消息写回结果
        """

        self.command = command
        return AgentReplyResult(answer_message_id=999, is_created=True)


class ToolChoosingChatModel:
    """首轮请求资源检索，第二轮返回最终回答。"""

    def __init__(self, tool_arguments: str = '{"query":"项目当前结论"}') -> None:
        """
        初始化模型 Turn 计数

        :param tool_arguments: 首轮资源检索工具参数
        :return: 无返回值
        """

        self.turn_count = 0
        self.second_turn_message_list: list[ChatMessage] = []
        self.tool_arguments = tool_arguments

    async def stream(
        self,
        model_config: RuntimeModelConfig,
        message_list: list[ChatMessage],
        max_output_tokens: int,
        temperature: float,
        thinking_enabled: bool,
        thinking_effort: str,
        tool_list: list[ChatToolDefinition] | None = None,
    ) -> AsyncIterator[ChatModelEvent]:
        """
        输出包含 Thinking 和工具调用的 Provider 事件

        :param model_config: 模型配置
        :param message_list: 当前消息上下文
        :param max_output_tokens: 最大输出 Token 数
        :param temperature: 模型温度
        :param thinking_enabled: 是否启用 Thinking
        :param thinking_effort: Thinking 强度
        :param tool_list: 当前工具目录
        :yield: Pi 风格 Provider 事件
        """

        _ignored_arguments = (
            model_config,
            max_output_tokens,
            temperature,
            thinking_enabled,
            thinking_effort,
        )
        self.turn_count += 1
        yield ChatModelEvent(event_type=ProviderEventType.START)
        if self.turn_count == 1:
            assert tool_list
            assert any(tool.name == "search_conversation_resources" for tool in tool_list)
            yield ChatModelEvent(
                event_type=ProviderEventType.THINKING_START,
                content_index=0,
            )
            yield ChatModelEvent(
                event_type=ProviderEventType.THINKING_DELTA,
                content_index=0,
                delta="需要检索会话资源",
            )
            yield ChatModelEvent(
                event_type=ProviderEventType.THINKING_END,
                content_index=0,
            )
            yield ChatModelEvent(
                event_type=ProviderEventType.TOOL_CALL_START,
                content_index=1,
            )
            yield ChatModelEvent(
                event_type=ProviderEventType.TOOL_CALL_DELTA,
                content_index=1,
                tool_call_delta=ChatToolCallDelta(
                    index=0,
                    call_id="call-search-1",
                    name="search_conversation_resources",
                    arguments_delta=self.tool_arguments,
                ),
            )
            yield ChatModelEvent(
                event_type=ProviderEventType.TOOL_CALL_END,
                content_index=1,
            )
            yield ChatModelEvent(
                event_type=ProviderEventType.DONE,
                finish_reason="tool_calls",
            )
            return
        self.second_turn_message_list = list(message_list)
        yield ChatModelEvent(event_type=ProviderEventType.TEXT_START, content_index=0)
        yield ChatModelEvent(
            event_type=ProviderEventType.TEXT_DELTA,
            content_index=0,
            delta="项目已经完成MVP。",
        )
        yield ChatModelEvent(event_type=ProviderEventType.TEXT_END, content_index=0)
        yield ChatModelEvent(event_type=ProviderEventType.DONE, finish_reason="stop")

    async def test_connection(self, model_config: RuntimeModelConfig) -> None:
        """
        跳过测试模型连接

        :param model_config: 模型配置
        :return: 无返回值
        """

        _ignored_model_config = model_config


class RecordingLifecycle(RunLifecycle):
    """不访问数据库的 V2 生命周期记录器。"""

    def __init__(self) -> None:
        """
        初始化生命周期记录

        :return: 无返回值
        """

        self.next_id = 100
        self.stage_list: list[AgentRunStage] = []
        self.provider_event_list: list[ChatModelEvent] = []
        self.persisted_block_list: list[AgentRunContentBlock] = []
        self.tool_execution_list: list[AgentToolExecution] = []
        self.citation_count = 0

    async def recover_interrupted_runtime(self, run: AgentRun) -> int:
        """
        测试生命周期不存在待恢复运行时

        :param run: Agent Run
        :return: 固定返回零
        """

        _ignored_run = run
        return 0

    def _id(self) -> int:
        """
        返回下一个测试 ID

        :return: 单调测试 ID
        """

        self.next_id += 1
        return self.next_id

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
        创建测试步骤

        :param run: Agent Run
        :param sequence_no: 步骤序号
        :param stage: 运行阶段
        :param step_type: 步骤类型
        :param step_code: 步骤代码
        :param display_name: 展示名称
        :param visibility: 可见性
        :return: 运行中的步骤
        """

        run.change_stage(stage)
        self.stage_list.append(stage)
        step = AgentStep(
            step_id=self._id(),
            run_id=run.run_id,
            sequence_no=sequence_no,
            step_type=step_type,
            step_code=step_code,
            display_name=display_name,
            visibility=visibility,
        )
        step.start()
        return step

    async def complete_step(
        self,
        step: AgentStep,
        *,
        input_summary: dict[str, object] | None = None,
        output_summary: dict[str, object] | None = None,
    ) -> None:
        """
        完成测试步骤

        :param step: Agent 步骤
        :param input_summary: 输入摘要
        :param output_summary: 输出摘要
        :return: 无返回值
        """

        _ignored_summary = (input_summary, output_summary)
        step.complete()

    async def fail_step(self, step: AgentStep, error_code: str, error_message: str) -> None:
        """
        标记测试步骤失败

        :param step: Agent 步骤
        :param error_code: 错误码
        :param error_message: 错误摘要
        :return: 无返回值
        """

        step.fail(error_code, error_message)

    async def start_turn(
        self,
        run: AgentRun,
        turn_no: int,
        model_id: str,
    ) -> tuple[AgentRunTurn, AgentRunMessage]:
        """
        创建测试 Turn 和 Message

        :param run: Agent Run
        :param turn_no: Turn 序号
        :param model_id: 模型标识
        :return: 测试 Turn 和 Message
        """

        turn = AgentRunTurn(self._id(), run.run_id, turn_no)
        return turn, AgentRunMessage(
            self._id(),
            run.run_id,
            turn.turn_id,
            turn_no,
            f"assistant:{turn_no}",
            "assistant",
            model_id,
        )

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
        记录 Provider 事件

        :param run: Agent Run
        :param turn: Agent Turn
        :param message: Agent Message
        :param model_event: Provider 事件
        :param tool_call_id: 工具调用 ID
        :param tool_name: 工具名称
        :return: 无返回值
        """

        _ignored_arguments = (run, turn, message, tool_call_id, tool_name)
        self.provider_event_list.append(model_event)

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
        创建测试 Content Block

        :param run: Agent Run
        :param turn: Agent Turn
        :param message: Agent Message
        :param content_index: 内容序号
        :param block_type: 内容类型
        :param tool_call_id: 工具调用 ID
        :param tool_name: 工具名称
        :return: 流式 Content Block
        """

        return AgentRunContentBlock(
            self._id(),
            run.run_id,
            turn.turn_id,
            message.message_id,
            content_index,
            block_type,
            TraceVisibility.MEMBER,
            tool_call_id=tool_call_id,
            tool_name=tool_name,
        )

    async def persist_content_block(self, block: AgentRunContentBlock) -> None:
        """
        记录已完成 Block

        :param block: Content Block
        :return: 无返回值
        """

        self.persisted_block_list.append(block)

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
        完成测试 Turn

        :param run: Agent Run
        :param turn: Agent Turn
        :param message: Agent Message
        :param stop_reason: 停止原因
        :param input_tokens: 输入 Token
        :param output_tokens: 输出 Token
        :return: 无返回值
        """

        _ignored_run = run
        turn.complete(stop_reason)
        message.complete(stop_reason, input_tokens, output_tokens)

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
        创建测试工具执行

        :param run: Agent Run
        :param turn: Agent Turn
        :param message: Agent Message
        :param step: Agent 步骤
        :param tool_call: 工具调用
        :param display_name: 展示名称
        :param arguments_summary: 参数摘要
        :return: 运行中的工具执行
        """

        execution = AgentToolExecution(
            self._id(),
            run.run_id,
            turn.turn_id,
            turn.turn_no,
            message.message_id,
            step.step_id,
            tool_call.call_id,
            tool_call.name,
            display_name,
            TraceVisibility.MEMBER,
            arguments_summary=arguments_summary,
        )
        self.tool_execution_list.append(execution)
        return execution

    async def complete_tool_execution(
        self,
        run: AgentRun,
        execution: AgentToolExecution,
        result_summary: dict[str, object],
    ) -> None:
        """
        完成测试工具执行

        :param run: Agent Run
        :param execution: 工具执行
        :param result_summary: 结果摘要
        :return: 无返回值
        """

        _ignored_run = run
        execution.succeed(result_summary)

    async def fail_tool_execution(
        self,
        run: AgentRun,
        execution: AgentToolExecution,
        error_code: str,
        error_message: str,
    ) -> None:
        """
        标记测试工具失败

        :param run: Agent Run
        :param execution: 工具执行
        :param error_code: 错误码
        :param error_message: 错误摘要
        :return: 无返回值
        """

        _ignored_run = run
        execution.fail(error_code, error_message)

    async def publish_citations(
        self,
        run: AgentRun,
        citation_list: list[dict[str, object]],
    ) -> None:
        """
        记录引用数量

        :param run: Agent Run
        :param citation_list: 引用列表
        :return: 无返回值
        """

        _ignored_run = run
        self.citation_count = len(citation_list)


def build_run(run_id: int) -> AgentRun:
    """
    构造运行中的 Agent Run

    :param run_id: Run ID
    :return: 运行中的 Agent Run
    """

    run = AgentRun.create(
        run_id=run_id,
        conversation_id=202,
        trigger_message_id=3,
        requester_user_id=4,
        binding_id=102,
        binding_version=1,
        agent_id=103,
        agent_version=1,
        credential_id=104,
        credential_version=1,
        credential_owner_user_id=1,
        model_id="deepseek-chat",
    )
    run.start()
    return run


def build_runtime_config() -> RuntimeAgentConfig:
    """
    构造运行时配置

    :return: 测试运行时配置
    """

    return RuntimeAgentConfig(
        conversation=build_conversation_config(),
        model=RuntimeModelConfig(
            credential_id=104,
            credential_version=1,
            credential_owner_user_id=1,
            provider=ProviderType.DEEPSEEK,
            api_base="https://example.test",
            api_key="",
            model_id="deepseek-chat",
            timeout_ms=1000,
            max_retries=0,
        ),
    )


async def test_plain_question_streams_without_retrieval() -> None:
    """
    普通问题直接流式回答且不触发检索

    :return: 无返回值
    """

    retriever = FakeKnowledgeRetriever()
    lifecycle = RecordingLifecycle()
    reply_writer = FakeReplyWriter()
    graph = AnswerGraph(
        conversation_reader=FakeConversationReader(),
        knowledge_retriever=retriever,
        chat_model=FixedAnswerChatModel("这是最终回答"),
        message_reply_writer=reply_writer,
        prompt_assembler=PromptAssembler(),
        lifecycle_service=lifecycle,
        fixed_answer_enabled=False,
    )

    answer_message_id = await graph.execute(build_run(1), build_runtime_config())

    assert answer_message_id == 999
    assert retriever.resource_query is None
    assert [event.event_type for event in lifecycle.provider_event_list] == [
        ProviderEventType.START,
        ProviderEventType.TEXT_START,
        ProviderEventType.TEXT_DELTA,
        ProviderEventType.TEXT_END,
        ProviderEventType.DONE,
    ]
    assert lifecycle.persisted_block_list[0].content == "这是最终回答"
    assert reply_writer.command is not None
    assert reply_writer.command.content == "这是最终回答"


async def test_tool_turn_persists_thinking_and_returns_tool_result() -> None:
    """
    工具 Turn 持久化 Thinking 并在下一 Turn 回传完整消息

    :return: 无返回值
    """

    retriever = FakeKnowledgeRetriever()
    model = ToolChoosingChatModel()
    lifecycle = RecordingLifecycle()
    reply_writer = FakeReplyWriter()
    graph = AnswerGraph(
        conversation_reader=FakeConversationReader(),
        knowledge_retriever=retriever,
        chat_model=model,
        message_reply_writer=reply_writer,
        prompt_assembler=PromptAssembler(),
        lifecycle_service=lifecycle,
        fixed_answer_enabled=False,
    )

    answer_message_id = await graph.execute(build_run(2), build_runtime_config())

    assert answer_message_id == 999
    assert retriever.resource_query is not None
    assert retriever.resource_query.conversation_id == 202
    assert lifecycle.citation_count == 1
    assert [block.block_type for block in lifecycle.persisted_block_list] == [
        AgentContentBlockType.THINKING,
        AgentContentBlockType.TOOL_CALL,
        AgentContentBlockType.TEXT,
    ]
    assert lifecycle.persisted_block_list[0].content == "需要检索会话资源"
    assert lifecycle.tool_execution_list[0].result_summary["status"] == "READY"
    assistant_message = next(
        message
        for message in model.second_turn_message_list
        if message.role == "assistant" and message.tool_call_list
    )
    assert assistant_message.reasoning_content == "需要检索会话资源"
    assert any(message.role == "tool" for message in model.second_turn_message_list)
    assert reply_writer.command is not None
    assert reply_writer.command.content == "项目已经完成MVP。"


async def test_explicit_indexing_resource_blocks_ungrounded_model_answer() -> None:
    """
    指定资源仍在索引时直接返回可信提示且不进入下一模型 Turn

    :return: 无返回值
    """

    retriever = FakeKnowledgeRetriever(
        resource_status=ResourceStatus.INDEXING,
        resource_result_enabled=False,
    )
    model = ToolChoosingChatModel(
        '{"query":"总结文件","resourceIds":[77]}',
    )
    lifecycle = RecordingLifecycle()
    reply_writer = FakeReplyWriter()
    graph = AnswerGraph(
        conversation_reader=FakeConversationReader(),
        knowledge_retriever=retriever,
        chat_model=model,
        message_reply_writer=reply_writer,
        prompt_assembler=PromptAssembler(),
        lifecycle_service=lifecycle,
        fixed_answer_enabled=False,
    )

    answer_message_id = await graph.execute(build_run(3), build_runtime_config())

    assert answer_message_id == 999
    assert model.turn_count == 1
    assert lifecycle.tool_execution_list[0].result_summary["status"] == "INDEXING"
    assert lifecycle.persisted_block_list[-1].block_type is AgentContentBlockType.TEXT
    assert lifecycle.persisted_block_list[-1].content == (
        "你引用的资源正在解析和建立索引，暂时无法读取。请稍后再试。"
    )
    assert [event.event_type for event in lifecycle.provider_event_list[-3:]] == [
        ProviderEventType.TEXT_START,
        ProviderEventType.TEXT_DELTA,
        ProviderEventType.TEXT_END,
    ]
    assert reply_writer.command is not None
    assert reply_writer.command.content == (
        "你引用的资源正在解析和建立索引，暂时无法读取。请稍后再试。"
    )
