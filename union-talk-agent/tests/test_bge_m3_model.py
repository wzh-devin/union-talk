"""BGE-M3 常驻模型与微批队列测试。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/08/13 01:40
"""

import asyncio
from collections.abc import Sequence

from union_talk_agent.infrastructure.embedding.bge_m3_model import BgeM3Model


class _FakeBgeM3Model:
    """记录底层编码批次的 BGE-M3 测试替身。"""

    def __init__(self) -> None:
        """
        初始化批次记录

        :return: 无返回值
        """

        self.batch_list: list[list[str]] = []

    def encode(self, text_list: Sequence[str], **_arguments: object) -> dict[str, object]:
        """
        返回可预测的 Dense 与 Sparse 向量

        :param text_list: 本次编码文本
        :param _arguments: BGE-M3 编码参数
        :return: 模拟的联合向量响应
        """

        self.batch_list.append(list(text_list))
        return {
            "dense_vecs": [[float(index), 1.0] for index, _text in enumerate(text_list)],
            "lexical_weights": [{index: 1.0} for index, _text in enumerate(text_list)],
        }


class _BatchingBgeM3Model(BgeM3Model):
    """从内存加载测试替身的常驻模型。"""

    def __init__(self, fake_model: _FakeBgeM3Model) -> None:
        """
        初始化测试常驻模型

        :param fake_model: BGE-M3 测试替身
        :return: 无返回值
        """

        super().__init__(
            "BAAI/bge-m3",
            batch_size=16,
            batch_wait_milliseconds=20,
        )
        self._fake_model = fake_model

    def _load_sync(self) -> object:
        """
        返回内存中的 BGE-M3 测试替身

        :return: BGE-M3 测试替身
        """

        return self._fake_model


async def test_concurrent_embedding_requests_share_one_micro_batch() -> None:
    """
    并发请求在等待窗口内合并推理并按原边界返回

    :return: 无返回值
    """

    fake_model = _FakeBgeM3Model()
    model = _BatchingBgeM3Model(fake_model)
    try:
        first_result, second_result = await asyncio.gather(
            model.encode(["第一条", "第二条"]),
            model.encode(["第三条"]),
        )
    finally:
        await model.close()

    assert fake_model.batch_list == [["第一条", "第二条", "第三条"]]
    assert len(first_result) == 2
    assert len(second_result) == 1
    assert second_result[0].dense == [2.0, 1.0]


async def test_empty_embedding_request_does_not_start_model() -> None:
    """
    空文本列表直接返回且不创建后台任务

    :return: 无返回值
    """

    model = BgeM3Model("BAAI/bge-m3", batch_size=16)

    assert await model.encode([]) == []
