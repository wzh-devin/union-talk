from google.protobuf.internal import containers as _containers
from google.protobuf import descriptor as _descriptor
from google.protobuf import message as _message
from collections.abc import Iterable as _Iterable, Mapping as _Mapping
from typing import ClassVar as _ClassVar, Optional as _Optional, Union as _Union

DESCRIPTOR: _descriptor.FileDescriptor

class AgentCitation(_message.Message):
    __slots__ = ("citation_key", "source_type", "message_id", "asset_file_id", "resource_version", "chunk_id", "page_from", "page_to", "heading_path")
    CITATION_KEY_FIELD_NUMBER: _ClassVar[int]
    SOURCE_TYPE_FIELD_NUMBER: _ClassVar[int]
    MESSAGE_ID_FIELD_NUMBER: _ClassVar[int]
    ASSET_FILE_ID_FIELD_NUMBER: _ClassVar[int]
    RESOURCE_VERSION_FIELD_NUMBER: _ClassVar[int]
    CHUNK_ID_FIELD_NUMBER: _ClassVar[int]
    PAGE_FROM_FIELD_NUMBER: _ClassVar[int]
    PAGE_TO_FIELD_NUMBER: _ClassVar[int]
    HEADING_PATH_FIELD_NUMBER: _ClassVar[int]
    citation_key: str
    source_type: str
    message_id: str
    asset_file_id: str
    resource_version: int
    chunk_id: str
    page_from: int
    page_to: int
    heading_path: str
    def __init__(self, citation_key: _Optional[str] = ..., source_type: _Optional[str] = ..., message_id: _Optional[str] = ..., asset_file_id: _Optional[str] = ..., resource_version: _Optional[int] = ..., chunk_id: _Optional[str] = ..., page_from: _Optional[int] = ..., page_to: _Optional[int] = ..., heading_path: _Optional[str] = ...) -> None: ...

class CreateAgentReplyRequest(_message.Message):
    __slots__ = ("run_id", "conversation_id", "trigger_message_id", "agent_id", "model_id", "content", "citations", "idempotency_key", "reply_to_user_id")
    RUN_ID_FIELD_NUMBER: _ClassVar[int]
    CONVERSATION_ID_FIELD_NUMBER: _ClassVar[int]
    TRIGGER_MESSAGE_ID_FIELD_NUMBER: _ClassVar[int]
    AGENT_ID_FIELD_NUMBER: _ClassVar[int]
    MODEL_ID_FIELD_NUMBER: _ClassVar[int]
    CONTENT_FIELD_NUMBER: _ClassVar[int]
    CITATIONS_FIELD_NUMBER: _ClassVar[int]
    IDEMPOTENCY_KEY_FIELD_NUMBER: _ClassVar[int]
    REPLY_TO_USER_ID_FIELD_NUMBER: _ClassVar[int]
    run_id: str
    conversation_id: str
    trigger_message_id: str
    agent_id: str
    model_id: str
    content: str
    citations: _containers.RepeatedCompositeFieldContainer[AgentCitation]
    idempotency_key: str
    reply_to_user_id: str
    def __init__(self, run_id: _Optional[str] = ..., conversation_id: _Optional[str] = ..., trigger_message_id: _Optional[str] = ..., agent_id: _Optional[str] = ..., model_id: _Optional[str] = ..., content: _Optional[str] = ..., citations: _Optional[_Iterable[_Union[AgentCitation, _Mapping]]] = ..., idempotency_key: _Optional[str] = ..., reply_to_user_id: _Optional[str] = ...) -> None: ...

class BaseResponse(_message.Message):
    __slots__ = ("success", "code", "message")
    SUCCESS_FIELD_NUMBER: _ClassVar[int]
    CODE_FIELD_NUMBER: _ClassVar[int]
    MESSAGE_FIELD_NUMBER: _ClassVar[int]
    success: bool
    code: int
    message: str
    def __init__(self, success: _Optional[bool] = ..., code: _Optional[int] = ..., message: _Optional[str] = ...) -> None: ...

class CreateAgentReplyResponse(_message.Message):
    __slots__ = ("br", "answer_message_id", "created")
    BR_FIELD_NUMBER: _ClassVar[int]
    ANSWER_MESSAGE_ID_FIELD_NUMBER: _ClassVar[int]
    CREATED_FIELD_NUMBER: _ClassVar[int]
    br: BaseResponse
    answer_message_id: str
    created: bool
    def __init__(self, br: _Optional[_Union[BaseResponse, _Mapping]] = ..., answer_message_id: _Optional[str] = ..., created: _Optional[bool] = ...) -> None: ...

class GetAgentConversationContextRequest(_message.Message):
    __slots__ = ("conversation_id", "trigger_message_id", "recent_message_limit")
    CONVERSATION_ID_FIELD_NUMBER: _ClassVar[int]
    TRIGGER_MESSAGE_ID_FIELD_NUMBER: _ClassVar[int]
    RECENT_MESSAGE_LIMIT_FIELD_NUMBER: _ClassVar[int]
    conversation_id: str
    trigger_message_id: str
    recent_message_limit: int
    def __init__(self, conversation_id: _Optional[str] = ..., trigger_message_id: _Optional[str] = ..., recent_message_limit: _Optional[int] = ...) -> None: ...

class ConversationMessage(_message.Message):
    __slots__ = ("message_id", "sender_type", "sender_display_name", "content", "created_at_ms")
    MESSAGE_ID_FIELD_NUMBER: _ClassVar[int]
    SENDER_TYPE_FIELD_NUMBER: _ClassVar[int]
    SENDER_DISPLAY_NAME_FIELD_NUMBER: _ClassVar[int]
    CONTENT_FIELD_NUMBER: _ClassVar[int]
    CREATED_AT_MS_FIELD_NUMBER: _ClassVar[int]
    message_id: str
    sender_type: str
    sender_display_name: str
    content: str
    created_at_ms: int
    def __init__(self, message_id: _Optional[str] = ..., sender_type: _Optional[str] = ..., sender_display_name: _Optional[str] = ..., content: _Optional[str] = ..., created_at_ms: _Optional[int] = ...) -> None: ...

class GetAgentConversationContextResponse(_message.Message):
    __slots__ = ("br", "conversation_id", "trigger_message_id", "question", "recent_messages", "receiver_user_ids", "quoted_message_id", "referenced_resource_ids", "referenced_asset_file_ids")
    BR_FIELD_NUMBER: _ClassVar[int]
    CONVERSATION_ID_FIELD_NUMBER: _ClassVar[int]
    TRIGGER_MESSAGE_ID_FIELD_NUMBER: _ClassVar[int]
    QUESTION_FIELD_NUMBER: _ClassVar[int]
    RECENT_MESSAGES_FIELD_NUMBER: _ClassVar[int]
    RECEIVER_USER_IDS_FIELD_NUMBER: _ClassVar[int]
    QUOTED_MESSAGE_ID_FIELD_NUMBER: _ClassVar[int]
    REFERENCED_RESOURCE_IDS_FIELD_NUMBER: _ClassVar[int]
    REFERENCED_ASSET_FILE_IDS_FIELD_NUMBER: _ClassVar[int]
    br: BaseResponse
    conversation_id: str
    trigger_message_id: str
    question: str
    recent_messages: _containers.RepeatedCompositeFieldContainer[ConversationMessage]
    receiver_user_ids: _containers.RepeatedScalarFieldContainer[str]
    quoted_message_id: str
    referenced_resource_ids: _containers.RepeatedScalarFieldContainer[str]
    referenced_asset_file_ids: _containers.RepeatedScalarFieldContainer[str]
    def __init__(self, br: _Optional[_Union[BaseResponse, _Mapping]] = ..., conversation_id: _Optional[str] = ..., trigger_message_id: _Optional[str] = ..., question: _Optional[str] = ..., recent_messages: _Optional[_Iterable[_Union[ConversationMessage, _Mapping]]] = ..., receiver_user_ids: _Optional[_Iterable[str]] = ..., quoted_message_id: _Optional[str] = ..., referenced_resource_ids: _Optional[_Iterable[str]] = ..., referenced_asset_file_ids: _Optional[_Iterable[str]] = ...) -> None: ...

class GetAgentConversationPermissionRequest(_message.Message):
    __slots__ = ("conversation_id", "user_id")
    CONVERSATION_ID_FIELD_NUMBER: _ClassVar[int]
    USER_ID_FIELD_NUMBER: _ClassVar[int]
    conversation_id: str
    user_id: str
    def __init__(self, conversation_id: _Optional[str] = ..., user_id: _Optional[str] = ...) -> None: ...

class GetAgentConversationPermissionResponse(_message.Message):
    __slots__ = ("br", "member", "role", "conversation_type", "group_id")
    BR_FIELD_NUMBER: _ClassVar[int]
    MEMBER_FIELD_NUMBER: _ClassVar[int]
    ROLE_FIELD_NUMBER: _ClassVar[int]
    CONVERSATION_TYPE_FIELD_NUMBER: _ClassVar[int]
    GROUP_ID_FIELD_NUMBER: _ClassVar[int]
    br: BaseResponse
    member: bool
    role: str
    conversation_type: str
    group_id: str
    def __init__(self, br: _Optional[_Union[BaseResponse, _Mapping]] = ..., member: _Optional[bool] = ..., role: _Optional[str] = ..., conversation_type: _Optional[str] = ..., group_id: _Optional[str] = ...) -> None: ...
