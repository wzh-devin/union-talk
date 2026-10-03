"""与 Java ApiResult 对齐的统一 HTTP 响应。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/31 14:42
"""

from enum import IntEnum

from pydantic import BaseModel, ConfigDict, Field


class AgentApiErrorCode(IntEnum):
    """Agent HTTP 接口错误码。"""

    SYSTEM_ERROR = -1
    PARAM_ERROR = 400
    UNAUTHORIZED = 401
    FORBIDDEN = 403
    NOT_FOUND = 404
    BUSINESS_ERROR = 500
    RUN_NOT_FOUND = 40001
    RUN_STATE_INVALID = 40002
    RUN_CANCELLED = 40003
    TRIGGER_INVALID = 40004
    CONTEXT_UNAVAILABLE = 40005
    CONFIG_NOT_FOUND = 40006
    CONFIG_DISABLED = 40007
    CONFIG_VERSION_CONFLICT = 40008
    MODEL_REQUEST_FAILED = 40009
    MESSAGE_REPLY_FAILED = 40010
    PERMISSION_DENIED = 40011
    REQUEST_INVALID = 40012
    INFRASTRUCTURE_UNAVAILABLE = 40013
    RESOURCE_TOO_LARGE = 40014
    RESOURCE_UNSUPPORTED = 40015
    RESOURCE_OCR_REQUIRED = 40016


class ApiResult[DataT](BaseModel):
    """与 Java `ApiResult<T>` 字段完全一致的响应模型。"""

    model_config = ConfigDict(populate_by_name=True)

    success: bool
    err_code: int | None = Field(default=None, alias="errCode")
    err_msg: str | None = Field(default=None, alias="errMsg")
    data: DataT | None = None


def api_success[DataT](data: DataT) -> ApiResult[DataT]:
    """
    构造包含业务数据的成功响应

    :param data: 待返回的业务数据
    :return: Java ApiResult 兼容成功响应
    """

    return ApiResult[DataT](success=True, data=data)


def api_failure(err_code: int, err_msg: str) -> ApiResult[None]:
    """
    构造不暴露业务数据的失败响应

    :param err_code: 接口错误码
    :param err_msg: 脱敏错误信息
    :return: Java ApiResult 兼容失败响应
    """

    return ApiResult[None](
        success=False,
        errCode=err_code,
        errMsg=err_msg,
        data=None,
    )
