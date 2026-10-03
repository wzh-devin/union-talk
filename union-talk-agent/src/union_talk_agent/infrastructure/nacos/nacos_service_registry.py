"""基于 Nacos 官方 Python SDK 的服务注册器。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/31 14:05
"""

import logging

from v2.nacos import (
    ClientConfigBuilder,
    DeregisterInstanceParam,
    GRPCConfig,
    NacosNamingService,
    RegisterInstanceParam,
)

from union_talk_agent.settings.nacos_settings import NacosSettings

logger = logging.getLogger(__name__)


async def _shutdown_naming_service(client: NacosNamingService) -> None:
    """
    关闭 Nacos Naming 客户端并兼容 2.0.11 的停止方法返回值

    :param client: 待关闭的 Nacos Naming 客户端
    :return: 无返回值
    :raises TypeError: SDK 抛出非已知停止兼容异常时继续向上抛出
    """

    try:
        await client.shutdown()
    except TypeError as error:
        if "NoneType can't be used in 'await' expression" not in str(error):
            raise
        logger.debug("Nacos Python SDK 2.0.11客户端已完成连接关闭")


class NacosServiceRegistry:
    """管理 Agent API 临时实例的注册和注销生命周期。"""

    def __init__(self, settings: NacosSettings, instance_port: int) -> None:
        """
        初始化 Nacos 服务注册器

        :param settings: Nacos 服务注册配置
        :param instance_port: Agent API 对外监听端口
        :return: 无返回值
        """

        self._settings = settings
        self._instance_port = instance_port
        self._client: NacosNamingService | None = None
        self._registered = False

    @property
    def is_ready(self) -> bool:
        """
        返回当前服务注册就绪状态

        :return: Nacos 未启用或实例已经注册时为真
        """

        return not self._settings.enabled or self._registered

    async def start(self) -> None:
        """
        创建 Nacos 客户端并注册 Agent API 临时实例

        :return: 无返回值
        :raises RuntimeError: Nacos 拒绝注册服务实例时抛出
        """

        if not self._settings.enabled or self._registered:
            return
        client_config = (
            ClientConfigBuilder()
            .server_address(self._settings.server_addresses)
            .namespace_id(self._settings.namespace_id)
            .username(self._settings.username)
            .password(self._settings.password)
            .log_level("INFO")
            .grpc_config(GRPCConfig(grpc_timeout=self._settings.grpc_timeout_ms))
            .build()
        )
        client = await NacosNamingService.create_naming_service(client_config)
        self._client = client
        registered = await client.register_instance(
            RegisterInstanceParam(
                service_name=self._settings.service_name,
                group_name=self._settings.group_name,
                ip=self._settings.instance_ip,
                port=self._instance_port,
                weight=self._settings.weight,
                cluster_name=self._settings.cluster_name,
                metadata={
                    "protocol": "http",
                    "version": "0.1.0",
                    "healthPath": "/health/ready",
                },
                enabled=True,
                healthy=True,
                ephemeral=True,
            )
        )
        if not registered:
            await _shutdown_naming_service(client)
            self._client = None
            raise RuntimeError("Nacos拒绝注册union-talk-agent服务实例")
        self._registered = True
        logger.info(
            "Agent API已注册Nacos, service=%s, group=%s, instance=%s:%s",
            self._settings.service_name,
            self._settings.group_name,
            self._settings.instance_ip,
            self._instance_port,
        )

    async def close(self) -> None:
        """
        注销 Agent API 临时实例并关闭 Nacos 客户端

        :return: 无返回值
        """

        client = self._client
        if client is None:
            return
        try:
            if self._registered:
                deregistered = await client.deregister_instance(
                    DeregisterInstanceParam(
                        service_name=self._settings.service_name,
                        group_name=self._settings.group_name,
                        ip=self._settings.instance_ip,
                        port=self._instance_port,
                        cluster_name=self._settings.cluster_name,
                        ephemeral=True,
                    )
                )
                if not deregistered:
                    logger.warning(
                        "Nacos未确认Agent API实例注销, service=%s, instance=%s:%s",
                        self._settings.service_name,
                        self._settings.instance_ip,
                        self._instance_port,
                    )
        except Exception:
            logger.warning(
                "Agent API注销Nacos实例失败, service=%s, instance=%s:%s",
                self._settings.service_name,
                self._settings.instance_ip,
                self._instance_port,
                exc_info=True,
            )
        finally:
            self._registered = False
            self._client = None
            try:
                await _shutdown_naming_service(client)
            except Exception:
                logger.warning(
                    "Agent API关闭Nacos客户端失败, service=%s",
                    self._settings.service_name,
                    exc_info=True,
                )
