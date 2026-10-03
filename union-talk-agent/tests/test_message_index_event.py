"""消息索引事件契约测试。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/08/13 01:20
"""

from union_talk_agent.interfaces.mq.schemas.message_index_event import MessageIndexEvent


def test_message_event_maps_java_type_field() -> None:
    """
    验证 Java 消息事件的 type 字段映射为索引命令类型

    :return: 无返回值
    """

    event = MessageIndexEvent.model_validate(
        {
            "eventId": "message-created:1",
            "messageId": 1,
            "conversationId": 2,
            "senderId": 3,
            "type": "FILE",
            "content": "4",
            "createdAt": "2026-08-13T01:00:00Z",
        }
    )

    assert event.message_type == "FILE"
    assert event.to_command().message_type == "FILE"


def test_agent_reply_event_maps_resource_citation_lineage() -> None:
    """
    验证 Agent 回复引用的资源文件 ID 进入消息索引命令

    :return: 无返回值
    """

    event = MessageIndexEvent.model_validate(
        {
            "eventId": "message-created:11",
            "eventType": "MESSAGE_CREATED",
            "schemaVersion": 2,
            "messageId": 11,
            "conversationId": 22,
            "senderId": 0,
            "senderType": "AGENT",
            "type": "TEXT",
            "content": "根据文件内容生成的回答",
            "citationList": [
                {
                    "citationKey": "1",
                    "sourceType": "RESOURCE_CHUNK",
                    "assetFileId": "101",
                    "resourceVersion": 1,
                    "chunkId": "201",
                },
                {
                    "citationKey": "2",
                    "sourceType": "RESOURCE_CHUNK",
                    "assetFileId": "101",
                    "resourceVersion": 1,
                    "chunkId": "202",
                },
            ],
            "createdAt": "2026-08-14T10:00:00Z",
        }
    )

    command = event.to_command()

    assert command.source_asset_version_map == {101: 1}


def test_agent_reply_event_maps_message_citation_lineage() -> None:
    """
    验证 Agent 回复引用的消息 ID 可继续继承资产血缘

    :return: 无返回值
    """

    event = MessageIndexEvent.model_validate(
        {
            "eventId": "message-created:12",
            "messageId": 12,
            "conversationId": 22,
            "type": "TEXT",
            "content": "基于上一条回答继续生成",
            "citationList": [
                {
                    "sourceType": "MESSAGE_SEGMENT",
                    "messageId": "11",
                }
            ],
            "createdAt": "2026-08-14T10:01:00Z",
        }
    )

    assert event.to_command().source_message_id_list == [11]
