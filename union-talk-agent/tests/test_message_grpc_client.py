"""Message gRPC Client 生命周期测试。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/31 14:32
"""

import asyncio

from grpc import aio

from union_talk_agent.infrastructure.grpc.message_grpc_client import MessageGrpcClient


class LoopRecordingChannelFactory:
    """记录 gRPC Channel 创建时所在事件循环。"""

    def __init__(self) -> None:
        """
        初始化 Channel 工厂

        :return: 无返回值
        """

        self.call_count = 0
        self.target = ""
        self.event_loop: asyncio.AbstractEventLoop | None = None

    def __call__(self, target: str) -> aio.Channel:
        """
        在当前事件循环创建异步 gRPC Channel

        :param target: Message Service gRPC 地址
        :return: 异步 gRPC Channel
        """

        self.call_count += 1
        self.target = target
        self.event_loop = asyncio.get_running_loop()
        return aio.insecure_channel(target)


async def test_channel_is_created_on_active_api_event_loop() -> None:
    """
    Client 构造阶段不应绑定 Uvicorn 启动前的事件循环

    :return: 无返回值
    """

    channel_factory = LoopRecordingChannelFactory()
    client = MessageGrpcClient(
        "127.0.0.1:19005",
        5.0,
        channel_factory,
    )

    assert channel_factory.call_count == 0

    client.start()
    client.start()

    assert channel_factory.call_count == 1
    assert channel_factory.target == "127.0.0.1:19005"
    assert channel_factory.event_loop is asyncio.get_running_loop()

    await client.close()
