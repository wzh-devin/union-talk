"""File Service 资源内容读取边界。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/29 19:15
"""

from typing import Protocol

from union_talk_agent.knowledge.domain.indexed_resource import ResourceContent


class ResourceContentReader(Protocol):
    """通过 File Service 描述与临时下载地址读取资源。"""

    async def read(
        self,
        asset_file_id: int,
        resource_version: int,
    ) -> ResourceContent:
        """
        读取指定资源版本，且由 File Service 执行权限和版本校验

        :param asset_file_id: 资源文件 ID
        :param resource_version: 资源版本
        :return: 通过权限和版本校验的资源内容
        """

        ...
