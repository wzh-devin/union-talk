"""File Service 资源内容 gRPC 与下载适配器。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/08/12 23:57
"""

from typing import cast

import httpx
from grpc import aio

from union_talk_agent.agent_run.domain.exceptions import (
    AgentErrorCode,
    RetryableAgentError,
)
from union_talk_agent.infrastructure.grpc.generated import (
    file_resource_pb2,
    file_resource_pb2_grpc,
)
from union_talk_agent.knowledge.domain.indexed_resource import (
    ResourceContent,
    ResourceDescriptor,
)


class FileGrpcResourceContentReader:
    """通过 File Service 获取快照和短期下载地址。"""

    def __init__(
        self,
        target: str,
        deadline_seconds: float,
        http_client: httpx.AsyncClient,
    ) -> None:
        """
        初始化 File Service 资源读取器

        :param target: File Service gRPC 地址
        :param deadline_seconds: gRPC 和下载超时秒数
        :param http_client: 异步 HTTP 客户端
        :return: 无返回值
        """

        self._channel = aio.insecure_channel(target)
        self._stub = file_resource_pb2_grpc.FileGrpcServiceStub(self._channel)
        self._deadline_seconds = deadline_seconds
        self._http_client = http_client

    async def read(self, asset_file_id: int, resource_version: int) -> ResourceContent:
        """
        读取指定资源版本并下载原始内容

        :param asset_file_id: 资源文件 ID
        :param resource_version: 资源版本
        :return: 资源元数据快照和原始内容
        """

        try:
            response = cast(
                file_resource_pb2.AgentResourceContentResponse,
                await self._stub.getAgentResourceContent(
                    file_resource_pb2.AgentResourceContentRequest(
                        asset_id=str(asset_file_id),
                        resource_version=resource_version,
                    ),
                    timeout=self._deadline_seconds,
                ),
            )
            if not response.br.success or not response.HasField("resource"):
                raise RetryableAgentError(
                    AgentErrorCode.INFRASTRUCTURE_UNAVAILABLE,
                    response.br.message or "File Service未返回资源内容",
                )
            resource = response.resource
            download_response = await self._http_client.get(
                resource.download_url,
                timeout=self._deadline_seconds,
            )
            download_response.raise_for_status()
        except (aio.AioRpcError, httpx.HTTPError) as error:
            raise RetryableAgentError(
                AgentErrorCode.INFRASTRUCTURE_UNAVAILABLE,
                "读取File Service资源失败",
            ) from error
        return ResourceContent(
            descriptor=ResourceDescriptor(
                asset_file_id=int(resource.asset_id),
                conversation_id=int(resource.conversation_id),
                resource_version=int(resource.resource_version),
                file_name=resource.file_name,
                mime_type=resource.mime_type,
                sha256=resource.sha256,
                etag=resource.etag,
                folder_id=int(resource.folder_id) if resource.folder_id else None,
                path_text=resource.path_text,
            ),
            content=download_response.content,
        )

    async def close(self) -> None:
        """
        关闭 gRPC Channel

        :return: 无返回值
        """

        await self._channel.close()
