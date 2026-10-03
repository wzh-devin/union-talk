"""RabbitMQ 和 gRPC 跨服务契约测试。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/29 19:22
"""

from union_talk_agent.infrastructure.grpc.generated import message_agent_pb2
from union_talk_agent.interfaces.mq.schemas.agent_mentioned_event import (
    AgentMentionedEvent,
)


def test_agent_mentioned_event_matches_current_java_flat_payload() -> None:
    """
    当前 Java 平铺事件可直接解析为应用命令

    :return: 无返回值
    """

    event = AgentMentionedEvent.model_validate(
        {
            "eventId": "agent-mentioned:100",
            "eventType": "AGENT_MENTIONED",
            "schemaVersion": 3,
            "triggerMessageId": "100",
            "conversationId": "200",
            "agentId": "400",
            "requesterUserId": "300",
            "requesterDisplayName": "张三",
            "messageType": "TEXT",
            "occurredAt": "2026-07-29T12:00:00+08:00",
        }
    )
    command = event.to_command()

    assert command.trigger_message_id == 100
    assert command.conversation_id == 200
    assert command.agent_id == 400
    assert command.requester_user_id == 300


def test_create_agent_reply_wire_field_numbers_are_stable() -> None:
    """
    Python Proto 与已上线 Java CreateAgentReply 字段号保持一致

    :return: 无返回值
    """

    descriptor = message_agent_pb2.CreateAgentReplyRequest.DESCRIPTOR
    expected_number_map = {
        "run_id": 1,
        "conversation_id": 2,
        "trigger_message_id": 3,
        "agent_id": 4,
        "model_id": 5,
        "content": 6,
        "citations": 7,
        "idempotency_key": 8,
        "reply_to_user_id": 9,
    }

    assert {field.name: field.number for field in descriptor.fields} == expected_number_map
    service = message_agent_pb2.DESCRIPTOR.services_by_name["MessageGrpcService"]
    assert service.full_name == "message.service.MessageGrpcService"
    assert service.methods_by_name["createAgentReply"].name == "createAgentReply"


def test_permission_response_exposes_conversation_facts() -> None:
    """
    权限响应必须显式返回会话类型和群聊 ID

    :return: 无返回值
    """

    descriptor = message_agent_pb2.GetAgentConversationPermissionResponse.DESCRIPTOR

    assert {field.name: field.number for field in descriptor.fields} == {
        "br": 1,
        "member": 2,
        "role": 3,
        "conversation_type": 4,
        "group_id": 5,
    }
