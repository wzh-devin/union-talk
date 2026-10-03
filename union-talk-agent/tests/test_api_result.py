"""Java ApiResult 兼容响应测试。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/31 14:40
"""

from union_talk_agent.interfaces.http.schemas.api_result import (
    api_failure,
    api_success,
)


def test_success_response_matches_java_api_result() -> None:
    """
    成功响应必须将业务数据放入 data 字段

    :return: 无返回值
    """

    result = api_success([{"runId": "2083078078170533888"}])

    assert result.model_dump(by_alias=True) == {
        "success": True,
        "errCode": None,
        "errMsg": None,
        "data": [{"runId": "2083078078170533888"}],
    }


def test_failure_response_matches_java_api_result() -> None:
    """
    失败响应必须使用 errCode 和 errMsg 字段

    :return: 无返回值
    """

    result = api_failure(400, "请求参数错误")

    assert result.model_dump(by_alias=True) == {
        "success": False,
        "errCode": 400,
        "errMsg": "请求参数错误",
        "data": None,
    }
