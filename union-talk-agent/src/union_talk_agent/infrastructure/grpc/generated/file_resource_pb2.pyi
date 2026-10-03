from google.protobuf import descriptor as _descriptor
from google.protobuf import message as _message
from collections.abc import Mapping as _Mapping
from typing import ClassVar as _ClassVar, Optional as _Optional, Union as _Union

DESCRIPTOR: _descriptor.FileDescriptor

class AgentResourceContentRequest(_message.Message):
    __slots__ = ("asset_id", "resource_version")
    ASSET_ID_FIELD_NUMBER: _ClassVar[int]
    RESOURCE_VERSION_FIELD_NUMBER: _ClassVar[int]
    asset_id: str
    resource_version: int
    def __init__(self, asset_id: _Optional[str] = ..., resource_version: _Optional[int] = ...) -> None: ...

class AgentResourceContent(_message.Message):
    __slots__ = ("asset_id", "conversation_id", "resource_version", "file_name", "mime_type", "sha256", "etag", "folder_id", "path_text", "download_url")
    ASSET_ID_FIELD_NUMBER: _ClassVar[int]
    CONVERSATION_ID_FIELD_NUMBER: _ClassVar[int]
    RESOURCE_VERSION_FIELD_NUMBER: _ClassVar[int]
    FILE_NAME_FIELD_NUMBER: _ClassVar[int]
    MIME_TYPE_FIELD_NUMBER: _ClassVar[int]
    SHA256_FIELD_NUMBER: _ClassVar[int]
    ETAG_FIELD_NUMBER: _ClassVar[int]
    FOLDER_ID_FIELD_NUMBER: _ClassVar[int]
    PATH_TEXT_FIELD_NUMBER: _ClassVar[int]
    DOWNLOAD_URL_FIELD_NUMBER: _ClassVar[int]
    asset_id: str
    conversation_id: str
    resource_version: int
    file_name: str
    mime_type: str
    sha256: str
    etag: str
    folder_id: str
    path_text: str
    download_url: str
    def __init__(self, asset_id: _Optional[str] = ..., conversation_id: _Optional[str] = ..., resource_version: _Optional[int] = ..., file_name: _Optional[str] = ..., mime_type: _Optional[str] = ..., sha256: _Optional[str] = ..., etag: _Optional[str] = ..., folder_id: _Optional[str] = ..., path_text: _Optional[str] = ..., download_url: _Optional[str] = ...) -> None: ...

class AgentResourceContentResponse(_message.Message):
    __slots__ = ("br", "resource")
    BR_FIELD_NUMBER: _ClassVar[int]
    RESOURCE_FIELD_NUMBER: _ClassVar[int]
    br: BaseResponse
    resource: AgentResourceContent
    def __init__(self, br: _Optional[_Union[BaseResponse, _Mapping]] = ..., resource: _Optional[_Union[AgentResourceContent, _Mapping]] = ...) -> None: ...

class BaseResponse(_message.Message):
    __slots__ = ("success", "code", "message")
    SUCCESS_FIELD_NUMBER: _ClassVar[int]
    CODE_FIELD_NUMBER: _ClassVar[int]
    MESSAGE_FIELD_NUMBER: _ClassVar[int]
    success: bool
    code: int
    message: str
    def __init__(self, success: _Optional[bool] = ..., code: _Optional[int] = ..., message: _Optional[str] = ...) -> None: ...
