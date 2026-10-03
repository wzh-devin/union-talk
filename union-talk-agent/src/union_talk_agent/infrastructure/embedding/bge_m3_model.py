"""常驻进程共享的 BGE-M3 本地模型。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/08/12 22:37
"""

import asyncio
from collections.abc import Sequence
from contextlib import suppress
from dataclasses import dataclass
from typing import Any

import torch

from union_talk_agent.knowledge.application.ports.embedding_gateway import HybridEmbedding


@dataclass(slots=True)
class _EmbeddingBatchRequest:
    """等待微批处理的向量请求。"""

    text_list: list[str]
    future: asyncio.Future[list[HybridEmbedding]]


class BgeM3Model:
    """单进程加载一次 BGE-M3 并通过有界微批队列执行推理。"""

    def __init__(
        self,
        model_name: str,
        batch_size: int,
        batch_wait_milliseconds: int = 10,
        queue_capacity: int = 1024,
    ) -> None:
        """
        初始化延迟加载的 BGE-M3 模型

        :param model_name: Hugging Face 模型名称或本地路径
        :param batch_size: 单次推理批大小
        :param batch_wait_milliseconds: 合并并发请求的等待毫秒数
        :param queue_capacity: 待处理请求队列容量
        :return: 无返回值
        """

        self._model_name = model_name
        self._batch_size = batch_size
        self._batch_wait_seconds = batch_wait_milliseconds / 1000
        self._model: Any = None
        self._model_lock = asyncio.Lock()
        self._start_lock = asyncio.Lock()
        self._request_queue: asyncio.Queue[_EmbeddingBatchRequest] = asyncio.Queue(
            maxsize=queue_capacity
        )
        self._batch_task: asyncio.Task[None] | None = None

    async def start(self) -> None:
        """
        加载模型并启动向量微批处理任务

        :return: 无返回值
        """

        await self.load()
        async with self._start_lock:
            if self._batch_task is not None and not self._batch_task.done():
                return
            self._batch_task = asyncio.create_task(
                self._process_embedding_queue(),
                name="bge-m3-embedding-batch",
            )

    async def close(self) -> None:
        """
        停止向量微批处理任务并拒绝未执行请求

        :return: 无返回值
        """

        async with self._start_lock:
            batch_task = self._batch_task
            self._batch_task = None
        if batch_task is not None:
            batch_task.cancel()
            with suppress(asyncio.CancelledError):
                await batch_task
        while not self._request_queue.empty():
            request = self._request_queue.get_nowait()
            if not request.future.done():
                request.future.set_exception(RuntimeError("BGE-M3 模型服务正在关闭"))
            self._request_queue.task_done()

    async def load(self) -> None:
        """
        在工作线程加载模型并自动选择 CUDA、MPS 或 CPU

        :return: 无返回值
        """

        if self._model is not None:
            return
        async with self._model_lock:
            if self._model is not None:
                return
            self._model = await asyncio.to_thread(self._load_sync)

    async def encode(self, text_list: list[str]) -> list[HybridEmbedding]:
        """
        批量生成 Dense 与 Sparse 联合向量

        :param text_list: 待编码文本列表
        :return: 与输入顺序一致的联合向量列表
        """

        if not text_list:
            return []
        await self.start()
        future = asyncio.get_running_loop().create_future()
        await self._request_queue.put(_EmbeddingBatchRequest(text_list=text_list, future=future))
        return await future

    async def rerank(self, query: str, document_list: list[str]) -> list[float]:
        """
        使用 BGE-M3 多向量打分重排候选正文

        :param query: 检索问题
        :param document_list: 候选正文列表
        :return: 与候选顺序一致的相关性分数
        """

        await self.load()
        async with self._model_lock:
            return await asyncio.to_thread(self._rerank_sync, query, document_list)

    async def _process_embedding_queue(self) -> None:
        """
        按短等待窗口合并并执行并发向量请求

        :return: 无返回值
        """

        while True:
            request_list = [await self._request_queue.get()]
            try:
                if self._batch_wait_seconds > 0:
                    await asyncio.sleep(self._batch_wait_seconds)
                text_count = len(request_list[0].text_list)
                while text_count < self._batch_size:
                    try:
                        request = self._request_queue.get_nowait()
                    except asyncio.QueueEmpty:
                        break
                    request_list.append(request)
                    text_count += len(request.text_list)
                await self._execute_embedding_batch(request_list)
            except asyncio.CancelledError:
                for request in request_list:
                    if not request.future.done():
                        request.future.set_exception(RuntimeError("BGE-M3 模型服务正在关闭"))
                raise
            finally:
                for _request in request_list:
                    self._request_queue.task_done()

    async def _execute_embedding_batch(
        self,
        request_list: list[_EmbeddingBatchRequest],
    ) -> None:
        """
        执行一个向量微批并按原请求边界返回结果

        :param request_list: 已合并的向量请求列表
        :return: 无返回值
        """

        try:
            text_list = [text for request in request_list for text in request.text_list]
            async with self._model_lock:
                embedding_list = await asyncio.to_thread(self._encode_sync, text_list)
            offset = 0
            for request in request_list:
                next_offset = offset + len(request.text_list)
                if not request.future.done():
                    request.future.set_result(embedding_list[offset:next_offset])
                offset = next_offset
        except Exception as error:
            for request in request_list:
                if not request.future.done():
                    request.future.set_exception(error)

    def _load_sync(self) -> Any:
        """
        同步加载 FlagEmbedding BGE-M3 模型

        :return: 已加载模型实例
        """

        from FlagEmbedding import BGEM3FlagModel

        return BGEM3FlagModel(
            self._model_name,
            use_fp16=torch.cuda.is_available() or torch.backends.mps.is_available(),
            devices=self._device_name(),
        )

    def _encode_sync(self, text_list: list[str]) -> list[HybridEmbedding]:
        """
        同步执行 BGE-M3 Dense 与 Sparse 推理

        :param text_list: 待编码文本列表
        :return: 联合向量列表
        """

        output = self._model.encode(
            text_list,
            batch_size=self._batch_size,
            return_dense=True,
            return_sparse=True,
            return_colbert_vecs=False,
        )
        dense_vectors: Sequence[Sequence[float]] = output["dense_vecs"]
        sparse_vectors: Sequence[dict[int, float]] = output["lexical_weights"]
        return [
            HybridEmbedding(
                dense=[float(value) for value in dense],
                sparse={int(key): float(value) for key, value in sparse.items()},
            )
            for dense, sparse in zip(dense_vectors, sparse_vectors, strict=True)
        ]

    def _rerank_sync(self, query: str, document_list: list[str]) -> list[float]:
        """
        同步计算 BGE-M3 Dense、Sparse 与 ColBERT 混合分数

        :param query: 检索问题
        :param document_list: 候选正文列表
        :return: 混合相关性分数列表
        """

        pair_list = [[query, document] for document in document_list]
        score_payload = self._model.compute_score(
            pair_list,
            batch_size=self._batch_size,
            weights_for_different_modes=[0.4, 0.2, 0.4],
        )
        score_list = score_payload.get("colbert+sparse+dense", [])
        return [float(value) for value in score_list]

    @staticmethod
    def _device_name() -> str:
        """
        按 CUDA、MPS、CPU 顺序选择模型设备

        :return: FlagEmbedding 设备名称
        """

        if torch.cuda.is_available():
            return "cuda"
        if torch.backends.mps.is_available():
            return "mps"
        return "cpu"
