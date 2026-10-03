"""Pi 事件语义驱动的会话 Agent Loop。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/08/12 23:18
"""

import json
from datetime import datetime
from typing import Any, cast

from langgraph.graph import END, START, StateGraph
from pydantic import BaseModel, ConfigDict

from union_talk_agent.agent_config.domain.agent_config import RuntimeAgentConfig
from union_talk_agent.agent_run.application.ports.chat_model import ChatModel
from union_talk_agent.agent_run.application.ports.conversation_reader import ConversationReader
from union_talk_agent.agent_run.application.ports.knowledge_retriever import (
    KnowledgeRetriever,
    MessageSearchQuery,
    ResourceSearchQuery,
)
from union_talk_agent.agent_run.application.ports.message_reply_writer import MessageReplyWriter
from union_talk_agent.agent_run.application.ports.run_lifecycle import RunLifecycle
from union_talk_agent.agent_run.application.prompt_assembler import PromptAssembler
from union_talk_agent.agent_run.domain.agent_citation import AgentCitation
from union_talk_agent.agent_run.domain.agent_run import AgentRun
from union_talk_agent.agent_run.domain.agent_runtime import (
    AgentRunContentBlock,
    AgentRunMessage,
    AgentRunTurn,
)
from union_talk_agent.agent_run.domain.agent_step import AgentStep
from union_talk_agent.agent_run.domain.chat_model import (
    ChatMessage,
    ChatModelEvent,
    ChatToolCall,
    ChatToolDefinition,
)
from union_talk_agent.agent_run.domain.commands import CreateAgentReplyCommand
from union_talk_agent.agent_run.domain.constants import MAX_ANSWER_LENGTH, MAX_CITATION_COUNT
from union_talk_agent.agent_run.domain.conversation_context import ConversationContext
from union_talk_agent.agent_run.domain.enums import (
    AgentContentBlockType,
    AgentMessageStatus,
    AgentRunStage,
    AgentStepType,
    ProviderEventType,
    RetrievalToolResultStatus,
    TraceVisibility,
)
from union_talk_agent.agent_run.domain.exceptions import (
    AgentDomainError,
    AgentErrorCode,
    AgentRunCancelledError,
    NonRetryableAgentError,
)
from union_talk_agent.knowledge.domain.enums import ResourceStatus, SourceType
from union_talk_agent.knowledge.domain.retrieval_evidence import RetrievalEvidence

_SEARCH_MESSAGES_TOOL_NAME = "search_conversation_messages"
_SEARCH_RESOURCES_TOOL_NAME = "search_conversation_resources"
_EXPAND_RESOURCE_TOOL_NAME = "expand_resource_context"
_MAX_MODEL_TURN_COUNT = 8
_MAX_TOOL_RESULT_CHARACTER_COUNT = 18_000
_MAX_SINGLE_TOOL_RESULT_CHARACTER_COUNT = 4_000
_AGENT_LOOP_INSTRUCTION = """
你正在执行一个会话 Agent Loop。请遵守：
- 当前上下文足够时直接回答，不要为了展示过程而调用工具。
- 问题依赖历史成员、时间或以前讨论时，使用消息检索工具。
- 历史消息只证明曾经讨论过，不能证明文件当前仍存在。
- 问题依赖当前上传文件、附件、目录或资源时，必须使用资源检索工具；命中小块后可扩展上下文。
- 用户询问“当前文档”“现在的文件”“已有资源”时，只能根据资源检索结果判断文件范围。
- 不能从历史消息推断当前文件。
- 工具返回 INDEXING 时明确告知资源正在解析，不得假装已经读取。
- 工具调用后进入下一 Turn 重新评估，最终 Text 只输出面向用户的答案。
""".strip()


class AnswerGraphState(BaseModel):
    """LangGraph 节点间传递的 Pi Agent Loop 状态。"""

    model_config = ConfigDict(arbitrary_types_allowed=True)

    run: AgentRun
    runtime_config: RuntimeAgentConfig
    context: ConversationContext
    message_list: list[ChatMessage]
    pending_tool_call_list: list[ChatToolCall]
    completed_tool_name_list: list[str]
    evidence_list: list[RetrievalEvidence]
    answer_text: str
    citation_list: list[AgentCitation]
    answer_message_id: int
    next_sequence_no: int
    model_turn_count: int
    turn_no_offset: int
    current_turn: AgentRunTurn | None
    current_message: AgentRunMessage | None
    required_resource_status: RetrievalToolResultStatus | None


class AnswerGraph:
    """以 partial assistant message 和真实工具执行驱动多 Turn 回答。"""

    def __init__(
        self,
        *,
        conversation_reader: ConversationReader,
        knowledge_retriever: KnowledgeRetriever,
        chat_model: ChatModel,
        message_reply_writer: MessageReplyWriter,
        prompt_assembler: PromptAssembler,
        lifecycle_service: RunLifecycle,
        fixed_answer_enabled: bool,
    ) -> None:
        """
        初始化 Pi 风格回答图

        :param conversation_reader: 会话上下文读取端口
        :param knowledge_retriever: 分类知识检索端口
        :param chat_model: Provider 事件流端口
        :param message_reply_writer: 正式回答写回端口
        :param prompt_assembler: 模型上下文组装器
        :param lifecycle_service: Turn、Message、Block 和工具生命周期
        :param fixed_answer_enabled: 是否启用固定回答联调模式
        :return: 无返回值
        """

        self._conversation_reader = conversation_reader
        self._knowledge_retriever = knowledge_retriever
        self._chat_model = chat_model
        self._message_reply_writer = message_reply_writer
        self._prompt_assembler = prompt_assembler
        self._lifecycle_service = lifecycle_service
        self._fixed_answer_enabled = fixed_answer_enabled

        builder = StateGraph(AnswerGraphState)
        builder.add_node("load_context", self._load_context)
        builder.add_node("model_turn", self._model_turn)
        builder.add_node("retrieval_policy_guard", self._retrieval_policy_guard)
        builder.add_node("execute_tools", self._execute_tools)
        builder.add_node("persist_reply", self._persist_reply)
        builder.add_edge(START, "load_context")
        builder.add_edge("load_context", "model_turn")
        builder.add_edge("model_turn", "retrieval_policy_guard")
        builder.add_conditional_edges(
            "retrieval_policy_guard",
            self._route_after_policy_guard,
            {"tools": "execute_tools", "final": "persist_reply"},
        )
        builder.add_conditional_edges(
            "execute_tools",
            self._route_after_tool_execution,
            {"model": "model_turn", "final": "persist_reply"},
        )
        builder.add_edge("persist_reply", END)
        self._compiled: Any = builder.compile()

    @property
    def compiled(self) -> Any:
        """
        返回 LangGraph 编译图

        :return: 已编译的动态 Agent 图
        """

        return self._compiled

    async def execute(
        self,
        run: AgentRun,
        runtime_config: RuntimeAgentConfig,
        initial_turn_no: int = 0,
    ) -> int:
        """
        执行 Agent Loop 并返回正式回答消息 ID

        :param run: 当前 Agent Run
        :param runtime_config: 本次运行的不可变配置快照
        :param initial_turn_no: 对账恢复后已经使用的最大 Turn 序号
        :return: 正式回答消息 ID
        """

        result: dict[str, object] = await self._compiled.ainvoke(
            {
                "run": run,
                "runtime_config": runtime_config,
                "context": ConversationContext(
                    conversation_id=run.conversation_id,
                    trigger_message_id=run.trigger_message_id,
                    question="",
                ),
                "message_list": [],
                "pending_tool_call_list": [],
                "completed_tool_name_list": [],
                "evidence_list": [],
                "answer_text": "",
                "citation_list": [],
                "answer_message_id": 0,
                "next_sequence_no": 1,
                "model_turn_count": 0,
                "turn_no_offset": initial_turn_no,
                "current_turn": None,
                "current_message": None,
                "required_resource_status": None,
            },
            config={"configurable": {"thread_id": str(run.run_id)}},
        )
        answer_message_id = result.get("answer_message_id")
        if not isinstance(answer_message_id, int) or answer_message_id <= 0:
            raise NonRetryableAgentError(
                AgentErrorCode.MESSAGE_REPLY_FAILED,
                "回答工作流未返回正式消息ID",
            )
        return answer_message_id

    async def _load_context(self, state: AnswerGraphState) -> dict[str, object]:
        """
        读取当前问题、近期消息和引用资源快照

        :param state: 当前回答图状态
        :return: 上下文和初始模型消息更新
        """

        run = state.run
        config = state.runtime_config.conversation
        step = await self._lifecycle_service.start_step(
            run,
            sequence_no=state.next_sequence_no,
            stage=AgentRunStage.PREPARING,
            step_type=AgentStepType.CONTEXT_LOAD,
            step_code="load_context",
            display_name="加载会话上下文",
            visibility=TraceVisibility.INTERNAL,
        )
        try:
            if self._fixed_answer_enabled:
                context = ConversationContext(
                    conversation_id=run.conversation_id,
                    trigger_message_id=run.trigger_message_id,
                    question="",
                )
            else:
                recent_message_limit = max(20, min(config.recent_message_tokens // 32, 200))
                context = await self._conversation_reader.get_agent_context(
                    run.conversation_id,
                    run.trigger_message_id,
                    recent_message_limit,
                )
                invalid_message_id_set = await self._knowledge_retriever.list_invalid_message_ids(
                    run.conversation_id,
                    [message.message_id for message in context.recent_message_list],
                )
                if invalid_message_id_set:
                    context = ConversationContext(
                        conversation_id=context.conversation_id,
                        trigger_message_id=context.trigger_message_id,
                        question=context.question,
                        recent_message_list=[
                            message
                            for message in context.recent_message_list
                            if message.message_id not in invalid_message_id_set
                        ],
                        receiver_user_id_list=context.receiver_user_id_list,
                        quoted_message_id=context.quoted_message_id,
                        referenced_resource_id_list=context.referenced_resource_id_list,
                        referenced_asset_file_id_list=context.referenced_asset_file_id_list,
                    )
            message_list = self._prompt_assembler.assemble(config, context, [])
            if message_list:
                first_message = message_list[0]
                message_list[0] = ChatMessage(
                    role=first_message.role,
                    content=f"{first_message.content}\n\n{_AGENT_LOOP_INSTRUCTION}",
                )
            await self._lifecycle_service.complete_step(
                step,
                output_summary={
                    "recentMessageCount": len(context.recent_message_list),
                    "quotedMessagePresent": context.quoted_message_id is not None,
                    "referencedResourceCount": len(context.referenced_resource_id_list),
                    "referencedAssetCount": len(context.referenced_asset_file_id_list),
                },
            )
            return {
                "context": context,
                "message_list": message_list,
                "next_sequence_no": state.next_sequence_no + 1,
            }
        except Exception as error:
            await self._record_step_failure(step, error)
            raise

    async def _model_turn(self, state: AnswerGraphState) -> dict[str, object]:
        """
        执行一次模型 Turn 并实时转发每个 Provider 内容事件

        :param state: 当前回答图状态
        :return: partial message、工具选择或最终回答更新
        """

        if state.model_turn_count >= _MAX_MODEL_TURN_COUNT:
            raise NonRetryableAgentError(
                AgentErrorCode.MODEL_REQUEST_FAILED,
                "Agent执行轮次超过系统上限",
            )
        run = state.run
        turn_no = state.turn_no_offset + state.model_turn_count + 1
        step = await self._lifecycle_service.start_step(
            run,
            sequence_no=state.next_sequence_no,
            stage=AgentRunStage.GENERATING,
            step_type=AgentStepType.MODEL_GENERATION,
            step_code="model_turn",
            display_name="模型生成",
            visibility=TraceVisibility.INTERNAL,
        )
        turn, message = await self._lifecycle_service.start_turn(
            run,
            turn_no,
            state.runtime_config.model.model_id,
        )
        content_block_map: dict[int, AgentRunContentBlock] = {}
        text_part_list: list[str] = []
        thinking_part_list: list[str] = []
        tool_call_part_map: dict[int, dict[str, str]] = {}
        stop_reason: str | None = None
        input_tokens = 0
        output_tokens = 0
        try:
            async for model_event in self._chat_model.stream(
                state.runtime_config.model,
                state.message_list,
                state.runtime_config.conversation.max_output_tokens,
                state.runtime_config.conversation.temperature,
                state.runtime_config.conversation.thinking_enabled,
                state.runtime_config.conversation.thinking_effort,
                self._build_tool_definition_list(state.runtime_config),
            ):
                tool_call_id, tool_name = self._accumulate_provider_event(
                    run,
                    turn_no,
                    model_event,
                    text_part_list,
                    thinking_part_list,
                    tool_call_part_map,
                )
                await self._apply_content_block_event(
                    run,
                    turn,
                    message,
                    model_event,
                    content_block_map,
                    tool_call_id,
                    tool_name,
                )
                await self._lifecycle_service.publish_model_event(
                    run,
                    turn,
                    message,
                    model_event,
                    tool_call_id,
                    tool_name,
                )
                if model_event.event_type is ProviderEventType.DONE:
                    stop_reason = model_event.finish_reason
                    input_tokens = model_event.input_tokens
                    output_tokens = model_event.output_tokens

            answer_text = "".join(text_part_list).strip()
            if len(answer_text) > MAX_ANSWER_LENGTH:
                raise NonRetryableAgentError(
                    AgentErrorCode.MODEL_REQUEST_FAILED,
                    "模型回答长度超过系统上限",
                )
            tool_call_list = [
                ChatToolCall(
                    call_id=part["call_id"] or f"call-{run.run_id}-{turn_no}-{index}",
                    name=part["name"],
                    arguments=part["arguments"] or "{}",
                )
                for index, part in sorted(tool_call_part_map.items())
            ]
            if not answer_text and not tool_call_list:
                raise NonRetryableAgentError(
                    AgentErrorCode.MODEL_REQUEST_FAILED,
                    "模型既未返回回答也未调用工具",
                )
            await self._lifecycle_service.complete_turn(
                run,
                turn,
                message,
                stop_reason,
                input_tokens,
                output_tokens,
            )
            await self._lifecycle_service.complete_step(
                step,
                output_summary={
                    "stopReason": stop_reason,
                    "toolCount": len(tool_call_list),
                    "answerCharacters": len(answer_text),
                },
            )
            assistant_message = ChatMessage(
                role="assistant",
                content=answer_text,
                reasoning_content="".join(thinking_part_list),
                tool_call_list=tuple(tool_call_list),
            )
            return {
                "message_list": [*state.message_list, assistant_message],
                "pending_tool_call_list": tool_call_list,
                "answer_text": answer_text,
                "next_sequence_no": state.next_sequence_no + 1,
                "model_turn_count": state.model_turn_count + 1,
                "current_turn": turn,
                "current_message": message,
            }
        except Exception as error:
            turn.fail(self._error_code(error), str(error))
            message.status = AgentMessageStatus.FAILED
            message.error_code = self._error_code(error)
            message.error_message = str(error)[:500]
            message.finished_at = turn.finished_at
            await self._record_step_failure(step, error)
            raise

    async def _retrieval_policy_guard(self, state: AnswerGraphState) -> dict[str, object]:
        """
        在模型决策后强制补齐明确引用消息或资源所需检索

        :param state: 当前回答图状态
        :return: 可能补充强制工具调用的状态更新
        """

        if state.pending_tool_call_list or self._fixed_answer_enabled:
            return {}
        completed_tool_name_set = set(state.completed_tool_name_list)
        context = state.context
        forced_tool_call: ChatToolCall | None = None
        if (
            context.referenced_resource_id_list or context.referenced_asset_file_id_list
        ) and _SEARCH_RESOURCES_TOOL_NAME not in completed_tool_name_set:
            resource_id_list = [
                *context.referenced_resource_id_list,
                *context.referenced_asset_file_id_list,
            ]
            forced_tool_call = ChatToolCall(
                call_id=(
                    f"policy-resource-{state.run.run_id}-"
                    f"{state.turn_no_offset + state.model_turn_count}"
                ),
                name=_SEARCH_RESOURCES_TOOL_NAME,
                arguments=json.dumps(
                    {"query": context.question, "resourceIds": resource_id_list},
                    ensure_ascii=False,
                ),
            )
        elif (
            context.quoted_message_id is not None
            and _SEARCH_MESSAGES_TOOL_NAME not in completed_tool_name_set
        ):
            forced_tool_call = ChatToolCall(
                call_id=(
                    f"policy-message-{state.run.run_id}-"
                    f"{state.turn_no_offset + state.model_turn_count}"
                ),
                name=_SEARCH_MESSAGES_TOOL_NAME,
                arguments=json.dumps({"query": context.question}, ensure_ascii=False),
            )
        if forced_tool_call is None:
            citation_list = self._build_citations(state.evidence_list)
            await self._lifecycle_service.publish_citations(
                state.run,
                [self._citation_to_dict(citation) for citation in citation_list],
            )
            return {"citation_list": citation_list}
        message_list = list(state.message_list)
        if message_list and message_list[-1].role == "assistant":
            message_list[-1] = ChatMessage(
                role="assistant",
                content="",
                reasoning_content=message_list[-1].reasoning_content,
                tool_call_list=(forced_tool_call,),
            )
        return {
            "message_list": message_list,
            "pending_tool_call_list": [forced_tool_call],
            "answer_text": "",
        }

    @staticmethod
    def _route_after_policy_guard(state: AnswerGraphState) -> str:
        """
        按策略守卫结果路由到工具执行或正式写回

        :param state: 当前回答图状态
        :return: 下一节点路由名称
        """

        return "tools" if state.pending_tool_call_list else "final"

    @staticmethod
    def _route_after_tool_execution(state: AnswerGraphState) -> str:
        """
        在指定资源不可用时直接返回可信状态提示

        :param state: 当前回答图状态
        :return: 下一节点路由名称
        """

        blocked_status_set = {
            RetrievalToolResultStatus.EMPTY,
            RetrievalToolResultStatus.INDEXING,
            RetrievalToolResultStatus.FAILED,
        }
        return "final" if state.required_resource_status in blocked_status_set else "model"

    async def _execute_tools(self, state: AnswerGraphState) -> dict[str, object]:
        """
        执行本 Turn 的全部工具调用并把观察结果写回模型上下文

        :param state: 当前回答图状态
        :return: 工具结果消息、证据和完成工具列表更新
        """

        if state.current_turn is None or state.current_message is None:
            raise NonRetryableAgentError(
                AgentErrorCode.RUN_STATE_INVALID,
                "工具调用缺少所属Turn或Message",
            )
        message_list = list(state.message_list)
        evidence_list = list(state.evidence_list)
        completed_tool_name_list = list(state.completed_tool_name_list)
        required_resource_status = state.required_resource_status
        next_sequence_no = state.next_sequence_no
        for tool_call in state.pending_tool_call_list:
            display_name = self._tool_display_name(tool_call.name)
            step = await self._lifecycle_service.start_step(
                state.run,
                sequence_no=next_sequence_no,
                stage=AgentRunStage.TOOL_RUNNING,
                step_type=AgentStepType.TOOL_CALL,
                step_code=f"tool.{tool_call.name}"[:64],
                display_name=display_name,
            )
            next_sequence_no += 1
            arguments = self._parse_tool_arguments(tool_call)
            execution = await self._lifecycle_service.start_tool_execution(
                state.run,
                state.current_turn,
                state.current_message,
                step,
                tool_call,
                display_name,
                self._redact_tool_arguments(arguments),
            )
            try:
                (
                    new_evidence_list,
                    output_summary,
                    tool_result,
                    tool_status,
                ) = await self._execute_tool_call(state, tool_call, arguments)
                if self._is_explicit_resource_search(tool_call, arguments):
                    required_resource_status = tool_status
                evidence_list = self._merge_evidence(evidence_list, new_evidence_list)
                completed_tool_name_list.append(tool_call.name)
                await self._lifecycle_service.complete_step(
                    step,
                    input_summary=self._redact_tool_arguments(arguments),
                    output_summary=output_summary,
                )
                await self._lifecycle_service.complete_tool_execution(
                    state.run,
                    execution,
                    output_summary,
                )
            except AgentRunCancelledError as error:
                await self._record_step_failure(step, error)
                await self._lifecycle_service.fail_tool_execution(
                    state.run,
                    execution,
                    self._error_code(error),
                    str(error),
                )
                raise
            except Exception as error:
                await self._record_step_failure(step, error)
                await self._lifecycle_service.fail_tool_execution(
                    state.run,
                    execution,
                    self._error_code(error),
                    str(error),
                )
                tool_result = json.dumps(
                    {
                        "ok": False,
                        "error": self._error_code(error),
                        "message": "工具调用失败，请根据当前信息决定重试或继续回答。",
                    },
                    ensure_ascii=False,
                )
                if self._is_explicit_resource_search(tool_call, arguments):
                    required_resource_status = RetrievalToolResultStatus.FAILED
            message_list.append(
                ChatMessage(
                    role="tool",
                    content=tool_result,
                    tool_call_id=tool_call.call_id,
                    name=tool_call.name,
                )
            )
        answer_text = self._required_resource_status_answer(required_resource_status)
        if answer_text:
            await self._lifecycle_service.publish_citations(state.run, [])
            await self._publish_policy_answer(state, answer_text)
        return {
            "message_list": message_list,
            "pending_tool_call_list": [],
            "completed_tool_name_list": completed_tool_name_list,
            "evidence_list": evidence_list,
            "next_sequence_no": next_sequence_no,
            "required_resource_status": required_resource_status,
            "answer_text": answer_text,
            "citation_list": [] if answer_text else state.citation_list,
        }

    async def _publish_policy_answer(
        self,
        state: AnswerGraphState,
        answer_text: str,
    ) -> None:
        """
        将策略层确定性回答记录为可恢复的 assistant message

        :param state: 当前回答图状态
        :param answer_text: 面向用户的资源状态回答
        :return: 无返回值
        """

        turn_no = state.turn_no_offset + state.model_turn_count + 1
        turn, message = await self._lifecycle_service.start_turn(
            state.run,
            turn_no,
            "retrieval-policy-guard",
        )
        block_map: dict[int, AgentRunContentBlock] = {}
        event_list = [
            ChatModelEvent(
                event_type=ProviderEventType.TEXT_START,
                content_index=0,
            ),
            ChatModelEvent(
                event_type=ProviderEventType.TEXT_DELTA,
                content_index=0,
                delta=answer_text,
            ),
            ChatModelEvent(
                event_type=ProviderEventType.TEXT_END,
                content_index=0,
            ),
        ]
        for event in event_list:
            await self._apply_content_block_event(
                state.run,
                turn,
                message,
                event,
                block_map,
                None,
                None,
            )
            await self._lifecycle_service.publish_model_event(
                state.run,
                turn,
                message,
                event,
            )
        await self._lifecycle_service.complete_turn(
            state.run,
            turn,
            message,
            "policy_guard",
            0,
            0,
        )

    async def _execute_tool_call(
        self,
        state: AnswerGraphState,
        tool_call: ChatToolCall,
        arguments: dict[str, object],
    ) -> tuple[
        list[RetrievalEvidence],
        dict[str, object],
        str,
        RetrievalToolResultStatus,
    ]:
        """
        分派三个稳定只读检索工具

        :param state: 当前回答图状态
        :param tool_call: 模型工具调用
        :param arguments: 已校验 JSON 参数
        :return: 新证据、结果摘要、模型观察文本和工具状态
        """

        query = self._required_query(arguments)
        config = state.runtime_config.conversation
        if tool_call.name == _SEARCH_MESSAGES_TOOL_NAME:
            evidence_list = await self._knowledge_retriever.search_messages(
                MessageSearchQuery(
                    conversation_id=state.run.conversation_id,
                    query=query,
                    top_k=config.message_top_k if config.is_history_enabled else 0,
                    participant_user_id_list=self._integer_list(arguments.get("participantIds")),
                    started_at=self._optional_datetime(arguments.get("startedAt")),
                    ended_at=self._optional_datetime(arguments.get("endedAt")),
                )
            )
        elif tool_call.name == _SEARCH_RESOURCES_TOOL_NAME:
            evidence_list = await self._knowledge_retriever.search_resources(
                ResourceSearchQuery(
                    conversation_id=state.run.conversation_id,
                    query=query,
                    top_k=config.resource_top_k if config.is_resource_enabled else 0,
                    resource_id_list=self._integer_list(arguments.get("resourceIds")),
                    folder_id=self._optional_integer(arguments.get("folderId")),
                    mime_type_list=self._string_list(arguments.get("mimeTypes")),
                )
            )
        elif tool_call.name == _EXPAND_RESOURCE_TOOL_NAME:
            chunk_id_list = self._integer_list(arguments.get("chunkIds"))
            token_budget = self._optional_integer(arguments.get("tokenBudget")) or 4000
            evidence_list = await self._knowledge_retriever.expand_resource_context(
                state.run.conversation_id,
                chunk_id_list,
                token_budget,
            )
        else:
            raise NonRetryableAgentError(
                AgentErrorCode.MODEL_REQUEST_FAILED,
                f"模型请求了未注册工具: {tool_call.name}",
            )
        status = (
            RetrievalToolResultStatus.READY if evidence_list else RetrievalToolResultStatus.EMPTY
        )
        if (
            tool_call.name == _SEARCH_RESOURCES_TOOL_NAME
            and not evidence_list
            and self._integer_list(arguments.get("resourceIds"))
        ):
            resource_status = await self._knowledge_retriever.get_resource_status(
                state.run.conversation_id,
                self._integer_list(arguments.get("resourceIds")),
            )
            if resource_status is ResourceStatus.INDEXING or resource_status is None:
                status = RetrievalToolResultStatus.INDEXING
            elif resource_status is ResourceStatus.FAILED:
                status = RetrievalToolResultStatus.FAILED
        result_payload = self._build_tool_result_payload(evidence_list, status)
        output_summary: dict[str, object] = {
            "status": status.value,
            "resultCount": len(evidence_list),
            "evidenceKeyList": [evidence.evidence_key for evidence in evidence_list],
        }
        return (
            evidence_list,
            output_summary,
            json.dumps(result_payload, ensure_ascii=False),
            status,
        )

    async def _persist_reply(self, state: AnswerGraphState) -> dict[str, object]:
        """
        将最终 Text Block 写回 Message Service

        :param state: 当前回答图状态
        :return: 正式回答消息 ID 更新
        """

        step = await self._lifecycle_service.start_step(
            state.run,
            sequence_no=state.next_sequence_no,
            stage=AgentRunStage.FINALIZING,
            step_type=AgentStepType.MESSAGE_PERSIST,
            step_code="persist_reply",
            display_name="保存正式回复",
            visibility=TraceVisibility.INTERNAL,
        )
        try:
            result = await self._message_reply_writer.create_agent_reply(
                CreateAgentReplyCommand(
                    run_id=state.run.run_id,
                    conversation_id=state.run.conversation_id,
                    trigger_message_id=state.run.trigger_message_id,
                    reply_to_user_id=state.run.requester_user_id,
                    agent_id=state.run.agent_id,
                    model_id=state.runtime_config.model.model_id,
                    content=state.answer_text,
                    citation_list=state.citation_list,
                )
            )
            await self._lifecycle_service.complete_step(
                step,
                output_summary={
                    "answerMessageId": str(result.answer_message_id),
                    "created": result.is_created,
                },
            )
            return {
                "answer_message_id": result.answer_message_id,
                "next_sequence_no": state.next_sequence_no + 1,
            }
        except Exception as error:
            await self._record_step_failure(step, error)
            raise

    async def _apply_content_block_event(
        self,
        run: AgentRun,
        turn: AgentRunTurn,
        message: AgentRunMessage,
        event: ChatModelEvent,
        block_map: dict[int, AgentRunContentBlock],
        tool_call_id: str | None,
        tool_name: str | None,
    ) -> None:
        """
        在内存累积 Provider 内容并只在 End 边界落库

        :param run: 当前 Agent Run
        :param turn: 当前 Agent Turn
        :param message: 当前 partial assistant message
        :param event: Provider 内容事件
        :param block_map: 当前 Turn 内容块映射
        :param tool_call_id: 工具调用 ID
        :param tool_name: 工具名称
        :return: 无返回值
        """

        if event.content_index is None:
            return
        block_type_map = {
            ProviderEventType.TEXT_START: AgentContentBlockType.TEXT,
            ProviderEventType.TEXT_DELTA: AgentContentBlockType.TEXT,
            ProviderEventType.TEXT_END: AgentContentBlockType.TEXT,
            ProviderEventType.THINKING_START: AgentContentBlockType.THINKING,
            ProviderEventType.THINKING_DELTA: AgentContentBlockType.THINKING,
            ProviderEventType.THINKING_END: AgentContentBlockType.THINKING,
            ProviderEventType.TOOL_CALL_START: AgentContentBlockType.TOOL_CALL,
            ProviderEventType.TOOL_CALL_DELTA: AgentContentBlockType.TOOL_CALL,
            ProviderEventType.TOOL_CALL_END: AgentContentBlockType.TOOL_CALL,
        }
        block_type = block_type_map.get(event.event_type)
        if block_type is None:
            return
        block = block_map.get(event.content_index)
        if block is None:
            block = self._lifecycle_service.create_content_block(
                run,
                turn,
                message,
                event.content_index,
                block_type,
                tool_call_id,
                tool_name,
            )
            block_map[event.content_index] = block
        if event.delta:
            block.append(event.delta)
        if event.event_type is ProviderEventType.TOOL_CALL_DELTA and event.tool_call_delta:
            block.append(event.tool_call_delta.arguments_delta)
            block.tool_call_id = tool_call_id
            block.tool_name = tool_name
        if event.event_type in {
            ProviderEventType.TEXT_END,
            ProviderEventType.THINKING_END,
            ProviderEventType.TOOL_CALL_END,
        }:
            block.complete()
            await self._lifecycle_service.persist_content_block(block)

    @staticmethod
    def _accumulate_provider_event(
        run: AgentRun,
        turn_no: int,
        event: ChatModelEvent,
        text_part_list: list[str],
        thinking_part_list: list[str],
        tool_call_part_map: dict[int, dict[str, str]],
    ) -> tuple[str | None, str | None]:
        """
        累积 Provider Delta 以便构造下一 Turn 完整消息

        :param run: 当前 Agent Run
        :param turn_no: 当前 Turn 序号
        :param event: Provider 事件
        :param text_part_list: Text 增量列表
        :param thinking_part_list: Thinking 增量列表
        :param tool_call_part_map: 工具调用分片映射
        :return: 当前工具调用 ID 和名称
        """

        if event.event_type is ProviderEventType.TEXT_DELTA:
            text_part_list.append(event.delta)
        if event.event_type is ProviderEventType.THINKING_DELTA:
            thinking_part_list.append(event.delta)
        tool_delta = event.tool_call_delta
        if event.event_type is not ProviderEventType.TOOL_CALL_DELTA or tool_delta is None:
            return None, None
        part = tool_call_part_map.setdefault(
            tool_delta.index,
            {"call_id": "", "name": "", "arguments": ""},
        )
        if tool_delta.call_id:
            part["call_id"] = tool_delta.call_id
        part["name"] += tool_delta.name
        part["arguments"] += tool_delta.arguments_delta
        return (
            part["call_id"] or f"call-{run.run_id}-{turn_no}-{tool_delta.index}",
            part["name"] or None,
        )

    def _build_tool_definition_list(
        self,
        runtime_config: RuntimeAgentConfig,
    ) -> list[ChatToolDefinition]:
        """
        按 Agent 定义生成三个稳定只读工具

        :param runtime_config: 当前 Run 配置快照
        :return: 本轮模型可选工具目录
        """

        config = runtime_config.conversation
        if self._fixed_answer_enabled:
            return []
        tool_list: list[ChatToolDefinition] = []
        if config.is_history_enabled:
            tool_list.append(
                ChatToolDefinition(
                    name=_SEARCH_MESSAGES_TOOL_NAME,
                    description=(
                        "搜索当前会话历史消息，可按参与者和时间范围过滤。"
                        "结果只表示历史讨论，不表示相关文件当前仍存在。"
                    ),
                    parameters={
                        "type": "object",
                        "properties": {
                            "query": {"type": "string"},
                            "participantIds": {"type": "array", "items": {"type": "integer"}},
                            "startedAt": {"type": "string", "format": "date-time"},
                            "endedAt": {"type": "string", "format": "date-time"},
                        },
                        "required": ["query"],
                        "additionalProperties": False,
                    },
                )
            )
        if config.is_resource_enabled:
            tool_list.extend(
                [
                    ChatToolDefinition(
                        name=_SEARCH_RESOURCES_TOOL_NAME,
                        description="搜索当前会话已上传且索引就绪的资源文件。",
                        parameters={
                            "type": "object",
                            "properties": {
                                "query": {"type": "string"},
                                "resourceIds": {
                                    "type": "array",
                                    "items": {"type": "integer"},
                                },
                                "folderId": {"type": "integer"},
                                "mimeTypes": {"type": "array", "items": {"type": "string"}},
                            },
                            "required": ["query"],
                            "additionalProperties": False,
                        },
                    ),
                    ChatToolDefinition(
                        name=_EXPAND_RESOURCE_TOOL_NAME,
                        description="根据已命中的资源块加载 Parent、前后相邻块和标题路径。",
                        parameters={
                            "type": "object",
                            "properties": {
                                "query": {"type": "string"},
                                "chunkIds": {"type": "array", "items": {"type": "integer"}},
                                "tokenBudget": {"type": "integer", "minimum": 256},
                            },
                            "required": ["query", "chunkIds"],
                            "additionalProperties": False,
                        },
                    ),
                ]
            )
        return tool_list

    @staticmethod
    def _tool_display_name(tool_name: str) -> str:
        """
        返回 Tool Registry 中的成员可读名称

        :param tool_name: 稳定工具名称
        :return: 成员可读名称
        """

        display_name_map = {
            _SEARCH_MESSAGES_TOOL_NAME: "检索会话消息",
            _SEARCH_RESOURCES_TOOL_NAME: "检索会话资源",
            _EXPAND_RESOURCE_TOOL_NAME: "展开资源上下文",
        }
        return display_name_map.get(tool_name, tool_name or "未知工具")

    @staticmethod
    def _parse_tool_arguments(tool_call: ChatToolCall) -> dict[str, object]:
        """
        解析并校验工具调用 JSON 对象

        :param tool_call: 模型工具调用
        :return: 工具参数字典
        """

        try:
            parsed_arguments: object = json.loads(tool_call.arguments or "{}")
        except json.JSONDecodeError as error:
            raise NonRetryableAgentError(
                AgentErrorCode.MODEL_REQUEST_FAILED,
                "工具参数不是合法JSON",
            ) from error
        if not isinstance(parsed_arguments, dict):
            raise NonRetryableAgentError(
                AgentErrorCode.MODEL_REQUEST_FAILED,
                "工具参数必须是JSON对象",
            )
        return cast(dict[str, object], parsed_arguments)

    @staticmethod
    def _required_query(arguments: dict[str, object]) -> str:
        """
        读取工具必需的 query 参数

        :param arguments: 工具参数字典
        :return: 非空检索问题
        """

        raw_query = arguments.get("query")
        query = raw_query.strip() if isinstance(raw_query, str) else ""
        if not query:
            raise NonRetryableAgentError(
                AgentErrorCode.MODEL_REQUEST_FAILED,
                "检索工具缺少query参数",
            )
        return query

    @staticmethod
    def _build_tool_result_payload(
        evidence_list: list[RetrievalEvidence],
        status: RetrievalToolResultStatus,
    ) -> dict[str, object]:
        """
        构建带 Evidence Key 且受字符预算约束的工具结果

        :param evidence_list: 工具检索证据
        :param status: 检索工具结果状态
        :return: 可安全加入模型上下文的工具结果
        """

        result_item_list: list[dict[str, str]] = []
        is_truncated = False
        for evidence in evidence_list:
            next_item = {
                "evidenceKey": evidence.evidence_key,
                "sourceType": str(evidence.source_type),
                "sourceId": str(evidence.source_id),
                "headingPath": evidence.heading_path,
                "content": evidence.text_content[:_MAX_SINGLE_TOOL_RESULT_CHARACTER_COUNT],
            }
            candidate_payload = {
                "ok": True,
                "status": status.value,
                "results": [*result_item_list, next_item],
            }
            if (
                len(json.dumps(candidate_payload, ensure_ascii=False))
                > _MAX_TOOL_RESULT_CHARACTER_COUNT
            ):
                is_truncated = True
                break
            result_item_list.append(next_item)
        return {
            "ok": status
            not in {
                RetrievalToolResultStatus.INDEXING,
                RetrievalToolResultStatus.FAILED,
            },
            "status": status.value,
            "resultCount": len(evidence_list),
            "returnedCount": len(result_item_list),
            "truncated": is_truncated,
            "results": result_item_list,
        }

    @staticmethod
    def _is_explicit_resource_search(
        tool_call: ChatToolCall,
        arguments: dict[str, object],
    ) -> bool:
        """
        判断工具调用是否明确指定会话资源

        :param tool_call: 当前工具调用
        :param arguments: 已解析工具参数
        :return: 是否属于必须基于指定资源回答的检索
        """

        return tool_call.name == _SEARCH_RESOURCES_TOOL_NAME and bool(
            AnswerGraph._integer_list(arguments.get("resourceIds"))
        )

    @staticmethod
    def _required_resource_status_answer(
        status: RetrievalToolResultStatus | None,
    ) -> str:
        """
        为不可用的指定资源生成可信且稳定的最终提示

        :param status: 指定资源检索状态
        :return: 阻断回答文案；无需阻断时返回空字符串
        """

        if status is None or status is RetrievalToolResultStatus.READY:
            return ""
        answer_map: dict[RetrievalToolResultStatus, str] = {
            RetrievalToolResultStatus.INDEXING: (
                "你引用的资源正在解析和建立索引，暂时无法读取。请稍后再试。"
            ),
            RetrievalToolResultStatus.FAILED: (
                "你引用的资源解析失败，当前无法读取。请重新上传或联系管理员处理。"
            ),
            RetrievalToolResultStatus.EMPTY: (
                "没有在你引用的资源中检索到可用于回答的内容，我不会假装已经读取该资源。"
            ),
        }
        return answer_map.get(status, "")

    @staticmethod
    def _redact_tool_arguments(arguments: dict[str, object]) -> dict[str, object]:
        """
        生成不包含原始长 Query 的成员可见参数摘要

        :param arguments: 原始工具参数
        :return: 脱敏工具参数摘要
        """

        summary = dict(arguments)
        raw_query = summary.get("query")
        if isinstance(raw_query, str) and len(raw_query) > 200:
            summary["query"] = f"{raw_query[:200]}…"
        return summary

    @staticmethod
    def _integer_list(value: object) -> list[int]:
        """
        从工具参数读取整数列表

        :param value: 工具参数值
        :return: 过滤非法元素后的整数列表
        """

        if not isinstance(value, list):
            return []
        object_list = cast(list[object], value)
        return [item for item in object_list if isinstance(item, int) and item > 0]

    @staticmethod
    def _string_list(value: object) -> list[str]:
        """
        从工具参数读取字符串列表

        :param value: 工具参数值
        :return: 过滤空值后的字符串列表
        """

        if not isinstance(value, list):
            return []
        object_list = cast(list[object], value)
        return [item for item in object_list if isinstance(item, str) and item]

    @staticmethod
    def _optional_integer(value: object) -> int | None:
        """
        从工具参数读取可选正整数

        :param value: 工具参数值
        :return: 正整数或空值
        """

        return value if isinstance(value, int) and value > 0 else None

    @staticmethod
    def _optional_datetime(value: object) -> datetime | None:
        """
        从工具参数读取 ISO 时间

        :param value: 工具参数值
        :return: 可解析时间或空值
        """

        if not isinstance(value, str) or not value:
            return None
        try:
            return datetime.fromisoformat(value.replace("Z", "+00:00"))
        except ValueError:
            return None

    @staticmethod
    def _merge_evidence(
        current_list: list[RetrievalEvidence],
        next_list: list[RetrievalEvidence],
    ) -> list[RetrievalEvidence]:
        """
        合并多轮工具证据并按来源去重

        :param current_list: 已有证据列表
        :param next_list: 新工具证据列表
        :return: 去重后的合并证据列表
        """

        merged_list = list(current_list)
        evidence_key_set = {
            (str(evidence.source_type), evidence.source_id) for evidence in current_list
        }
        for evidence in next_list:
            key = (str(evidence.source_type), evidence.source_id)
            if key in evidence_key_set:
                continue
            evidence_key_set.add(key)
            merged_list.append(evidence)
        return merged_list

    async def _record_step_failure(self, step: AgentStep, error: Exception) -> None:
        """
        记录不包含完整上下文的业务步骤失败

        :param step: 当前业务步骤
        :param error: 步骤异常
        :return: 无返回值
        """

        await self._lifecycle_service.fail_step(step, self._error_code(error), str(error))

    @staticmethod
    def _error_code(error: Exception) -> str:
        """
        将异常转换为稳定 Agent 错误码

        :param error: 待转换异常
        :return: 稳定错误码
        """

        if isinstance(error, AgentDomainError):
            return str(error.code)
        return str(AgentErrorCode.INFRASTRUCTURE_UNAVAILABLE)

    @staticmethod
    def _build_citations(evidence_list: list[RetrievalEvidence]) -> list[AgentCitation]:
        """
        根据模型实际获得的检索证据构建正式引用

        :param evidence_list: 多轮检索证据
        :return: 正式回答引用列表
        """

        citation_list: list[AgentCitation] = []
        for index, evidence in enumerate(evidence_list[:MAX_CITATION_COUNT], start=1):
            citation_list.append(
                AgentCitation(
                    citation_key=str(index),
                    source_type=str(evidence.source_type),
                    message_id=(
                        evidence.message_id
                        if evidence.source_type is SourceType.MESSAGE_SEGMENT
                        else None
                    ),
                    asset_file_id=evidence.asset_file_id,
                    resource_version=evidence.resource_version,
                    chunk_id=(
                        evidence.source_id
                        if evidence.source_type is SourceType.RESOURCE_CHUNK
                        else None
                    ),
                    page_from=evidence.page_from,
                    page_to=evidence.page_to,
                    heading_path=evidence.heading_path,
                )
            )
        return citation_list

    @staticmethod
    def _citation_to_dict(citation: AgentCitation) -> dict[str, object]:
        """
        将正式引用转换为实时协议字典

        :param citation: Agent 回答引用
        :return: 浏览器安全的引用字典
        """

        return {
            "citationKey": citation.citation_key,
            "sourceType": citation.source_type,
            "messageId": str(citation.message_id) if citation.message_id is not None else None,
            "assetFileId": (
                str(citation.asset_file_id) if citation.asset_file_id is not None else None
            ),
            "resourceVersion": citation.resource_version,
            "chunkId": str(citation.chunk_id) if citation.chunk_id is not None else None,
            "pageFrom": citation.page_from,
            "pageTo": citation.page_to,
            "headingPath": citation.heading_path,
        }


def create_development_graph() -> Any:
    """
    使用生产容器构造 LangGraph Studio 图

    :return: 供 LangGraph Studio 使用的编译图
    """

    from union_talk_agent.bootstrap.container import build_container
    from union_talk_agent.settings import load_settings

    return build_container(load_settings()).answer_graph.compiled
