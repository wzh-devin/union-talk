"""Message Service gRPC 防腐层。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/29 18:57
"""

from collections.abc import Callable
from typing import cast

import grpc
from grpc import aio

from union_talk_agent.access_control.domain.conversation_permission import (
    ConversationPermission,
)
from union_talk_agent.access_control.domain.enums import ConversationRole, ConversationType
from union_talk_agent.agent_run.domain.chat_model import AgentReplyResult
from union_talk_agent.agent_run.domain.commands import CreateAgentReplyCommand
from union_talk_agent.agent_run.domain.constants import AGENT_REPLY_IDEMPOTENCY_KEY_PREFIX
from union_talk_agent.agent_run.domain.conversation_context import (
    ConversationContext,
    ConversationMessage,
)
from union_talk_agent.agent_run.domain.exceptions import (
    AgentErrorCode,
    NonRetryableAgentError,
    RetryableAgentError,
)
from union_talk_agent.infrastructure.grpc.generated import (
    message_agent_pb2,
    message_agent_pb2_grpc,
)

GrpcChannelFactory = Callable[[str], aio.Channel]


class MessageGrpcClient:
    """实现会话读取和正式回复写回两个 Message Service 端口。"""

    def __init__(
        self,
        target: str,
        deadline_seconds: float,
        channel_factory: GrpcChannelFactory = aio.insecure_channel,
    ) -> None:
        """
        初始化 MessageGrpcClient

        :param target: Message Service gRPC 地址
        :param deadline_seconds: gRPC 调用超时秒数
        :param channel_factory: 异步 gRPC Channel 工厂
        :return: 无返回值
        """

        self._target = target
        self._deadline_seconds = deadline_seconds
        self._channel_factory = channel_factory
        self._channel: aio.Channel | None = None
        self._stub: message_agent_pb2_grpc.MessageGrpcServiceStub | None = None

    def start(self) -> None:
        """
        在当前进程事件循环中初始化 gRPC Channel

        :return: 无返回值
        """

        self._get_stub()

    async def create_agent_reply(
        self,
        command: CreateAgentReplyCommand,
    ) -> AgentReplyResult:
        """
        通过 Message Service 幂等创建正式 Agent 回复

        :param command: 待执行的应用命令
        :return: Message 服务返回的正式回答写回结果
        """

        citation_list = [
            message_agent_pb2.AgentCitation(
                citation_key=citation.citation_key,
                source_type=citation.source_type,
                message_id=self._optional_id(citation.message_id),
                asset_file_id=self._optional_id(citation.asset_file_id),
                resource_version=citation.resource_version or 0,
                chunk_id=self._optional_id(citation.chunk_id),
                page_from=citation.page_from or 0,
                page_to=citation.page_to or 0,
                heading_path=citation.heading_path,
            )
            for citation in command.citation_list
        ]
        request = message_agent_pb2.CreateAgentReplyRequest(
            run_id=str(command.run_id),
            conversation_id=str(command.conversation_id),
            trigger_message_id=str(command.trigger_message_id),
            agent_id=str(command.agent_id),
            model_id=command.model_id,
            content=command.content,
            citations=citation_list,
            idempotency_key=f"{AGENT_REPLY_IDEMPOTENCY_KEY_PREFIX}{command.run_id}",
            reply_to_user_id=str(command.reply_to_user_id),
        )
        try:
            stub = self._get_stub()
            response = cast(
                message_agent_pb2.CreateAgentReplyResponse,
                await stub.createAgentReply(
                    request,
                    timeout=self._deadline_seconds,
                ),
            )
        except aio.AioRpcError as error:
            raise self._map_rpc_error(
                error,
                AgentErrorCode.MESSAGE_REPLY_FAILED,
                "Message Service 创建Agent回复失败",
            ) from error
        if not response.br.success:
            raise NonRetryableAgentError(
                AgentErrorCode.MESSAGE_REPLY_FAILED,
                f"Message Service拒绝Agent回复, code={response.br.code}",
            )
        try:
            answer_message_id = int(response.answer_message_id)
        except ValueError as error:
            raise RetryableAgentError(
                AgentErrorCode.MESSAGE_REPLY_FAILED,
                "Message Service返回的消息ID非法",
            ) from error
        return AgentReplyResult(
            answer_message_id=answer_message_id,
            is_created=bool(response.created),
        )

    async def get_agent_context(
        self,
        conversation_id: int,
        trigger_message_id: int,
        recent_message_limit: int,
    ) -> ConversationContext:
        """
        调用待补齐的 Message Service 上下文读取契约

        :param conversation_id: 会话 ID
        :param trigger_message_id: 触发消息 ID
        :param recent_message_limit: 最近消息数量上限
        :return: Message 服务返回的紧凑会话上下文
        """

        request = message_agent_pb2.GetAgentConversationContextRequest(
            conversation_id=str(conversation_id),
            trigger_message_id=str(trigger_message_id),
            recent_message_limit=recent_message_limit,
        )
        try:
            stub = self._get_stub()
            response = cast(
                message_agent_pb2.GetAgentConversationContextResponse,
                await stub.getAgentConversationContext(
                    request,
                    timeout=self._deadline_seconds,
                ),
            )
        except aio.AioRpcError as error:
            raise self._map_rpc_error(
                error,
                AgentErrorCode.CONTEXT_UNAVAILABLE,
                "Message Service会话上下文不可用",
            ) from error
        if not response.br.success:
            raise NonRetryableAgentError(
                AgentErrorCode.CONTEXT_UNAVAILABLE,
                f"Message Service拒绝读取会话上下文, code={response.br.code}",
            )
        return ConversationContext(
            conversation_id=int(response.conversation_id),
            trigger_message_id=int(response.trigger_message_id),
            question=str(response.question),
            recent_message_list=[
                ConversationMessage(
                    message_id=int(message.message_id),
                    sender_type=str(message.sender_type),
                    sender_display_name=str(message.sender_display_name),
                    content=str(message.content),
                    created_at_ms=int(message.created_at_ms),
                )
                for message in response.recent_messages
            ],
            receiver_user_id_list=[
                int(receiver_user_id) for receiver_user_id in response.receiver_user_ids
            ],
            quoted_message_id=(
                int(response.quoted_message_id) if response.quoted_message_id else None
            ),
            referenced_resource_id_list=[
                int(resource_id) for resource_id in response.referenced_resource_ids
            ],
            referenced_asset_file_id_list=[
                int(asset_file_id) for asset_file_id in response.referenced_asset_file_ids
            ],
        )

    async def get_conversation_permission(
        self,
        conversation_id: int,
        user_id: int,
    ) -> ConversationPermission:
        """
        查询当前用户的实时会话成员和角色信息

        :param conversation_id: 会话 ID
        :param user_id: 当前登录用户 ID
        :return: Message Service 返回的会话权限
        """

        request = message_agent_pb2.GetAgentConversationPermissionRequest(
            conversation_id=str(conversation_id),
            user_id=str(user_id),
        )
        try:
            stub = self._get_stub()
            response = cast(
                message_agent_pb2.GetAgentConversationPermissionResponse,
                await stub.getAgentConversationPermission(
                    request,
                    timeout=self._deadline_seconds,
                ),
            )
        except aio.AioRpcError as error:
            raise self._map_rpc_error(
                error,
                AgentErrorCode.INFRASTRUCTURE_UNAVAILABLE,
                "Message Service会话权限不可用",
            ) from error
        if not response.br.success:
            raise RetryableAgentError(
                AgentErrorCode.INFRASTRUCTURE_UNAVAILABLE,
                f"Message Service拒绝查询会话权限, code={response.br.code}",
            )
        try:
            role = ConversationRole(response.role) if response.member else None
            conversation_type = (
                ConversationType(response.conversation_type) if response.member else None
            )
            group_id = int(response.group_id) if response.group_id else None
        except ValueError as error:
            raise NonRetryableAgentError(
                AgentErrorCode.INFRASTRUCTURE_UNAVAILABLE,
                "Message Service返回的会话角色非法",
            ) from error
        return ConversationPermission(
            conversation_id=conversation_id,
            user_id=user_id,
            is_member=bool(response.member),
            role=role,
            conversation_type=conversation_type,
            group_id=group_id,
        )

    async def close(self) -> None:
        """
        关闭 gRPC Channel

        :return: 无返回值
        """

        channel = self._channel
        if channel is None:
            return
        self._channel = None
        self._stub = None
        await channel.close()

    def _get_stub(self) -> message_agent_pb2_grpc.MessageGrpcServiceStub:
        """
        延迟创建并返回当前进程共享的 Message gRPC Stub

        :return: Message Service 异步 gRPC Stub
        """

        if self._stub is None:
            channel = self._channel_factory(self._target)
            self._channel = channel
            self._stub = message_agent_pb2_grpc.MessageGrpcServiceStub(channel)
        return self._stub

    @staticmethod
    def _optional_id(value: int | None) -> str:
        """
        将零值标识转换为空值

        :param value: 待转换或序列化的值
        :return: 有效业务 ID，零值时返回空
        """

        return str(value) if value is not None else ""

    @staticmethod
    def _map_rpc_error(
        error: aio.AioRpcError,
        error_code: AgentErrorCode,
        message: str,
    ) -> RetryableAgentError | NonRetryableAgentError:
        """
        将 gRPC 异常转换为 Agent 基础设施异常

        :param error: 已捕获的异常
        :param error_code: 错误码
        :param message: 待发布的消息内容
        :return: 根据 gRPC 状态分类后的可重试或不可重试异常
        """

        if error.code() in {
            grpc.StatusCode.UNAVAILABLE,
            grpc.StatusCode.DEADLINE_EXCEEDED,
            grpc.StatusCode.RESOURCE_EXHAUSTED,
            grpc.StatusCode.ABORTED,
        }:
            return RetryableAgentError(error_code, message)
        return NonRetryableAgentError(error_code, f"{message}, grpcCode={error.code().name}")
