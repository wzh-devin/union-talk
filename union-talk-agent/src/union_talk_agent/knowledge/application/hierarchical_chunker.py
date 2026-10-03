"""Parent/Child 文档切块。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/29 19:15
"""

import re

from union_talk_agent.agent_run.application.ports.id_generator import IdGenerator
from union_talk_agent.knowledge.domain.constants import (
    CHILD_OVERLAP_TOKENS,
    CHILD_TARGET_TOKENS,
    PARENT_TARGET_TOKENS,
)
from union_talk_agent.knowledge.domain.enums import ChunkKind
from union_talk_agent.knowledge.domain.resource_chunk import ParsedDocument, ResourceChunk


class HierarchicalChunker:
    """生成可扩展父块和用于向量检索的子块。"""

    _CHUNKER_VERSION = "hierarchical-chunker-v2"
    _CHARS_PER_TOKEN = 4

    def __init__(self, id_generator: IdGenerator) -> None:
        """
        初始化 HierarchicalChunker

        :param id_generator: 业务 ID 生成器
        :return: 无返回值
        """

        self._id_generator = id_generator

    @property
    def version(self) -> str:
        """
        返回可记录的切块器版本

        :return: 组件版本号
        """

        return self._CHUNKER_VERSION

    def chunk(
        self,
        resource_id: int,
        conversation_id: int,
        document: ParsedDocument,
    ) -> list[ResourceChunk]:
        """
        按段落构建父块，再使用重叠窗口构建子块

        :param resource_id: 资源索引 ID
        :param conversation_id: 会话 ID
        :param document: 已解析文档
        :return: 按父子层级排序的资源切块列表
        """

        chunk_list: list[ResourceChunk] = []
        chunk_no = 0
        section_list = (
            [
                (page.text_content, page.page_number, page.page_number)
                for page in document.page_list
                if page.text_content.strip()
            ]
            if document.page_list
            else [(document.text_content, None, None)]
        )
        for section_text, page_from, page_to in section_list:
            paragraph_list = [
                paragraph.strip()
                for paragraph in re.split(r"\n\s*\n", section_text)
                if paragraph.strip()
            ]
            parent_text_list = self._pack_paragraphs(
                paragraph_list,
                PARENT_TARGET_TOKENS * self._CHARS_PER_TOKEN,
            )
            for parent_text in parent_text_list:
                parent_id = self._id_generator.next_id()
                chunk_list.append(
                    self._create_chunk(
                        chunk_id=parent_id,
                        resource_id=resource_id,
                        conversation_id=conversation_id,
                        parent_chunk_id=None,
                        chunk_kind=ChunkKind.PARENT,
                        chunk_no=chunk_no,
                        text_content=parent_text,
                        document=document,
                        page_from=page_from,
                        page_to=page_to,
                    )
                )
                chunk_no += 1
                for child_text in self._sliding_window(parent_text):
                    chunk_list.append(
                        self._create_chunk(
                            chunk_id=self._id_generator.next_id(),
                            resource_id=resource_id,
                            conversation_id=conversation_id,
                            parent_chunk_id=parent_id,
                            chunk_kind=ChunkKind.CHILD,
                            chunk_no=chunk_no,
                            text_content=child_text,
                            document=document,
                            page_from=page_from,
                            page_to=page_to,
                        )
                    )
                    chunk_no += 1
        return chunk_list

    @staticmethod
    def _pack_paragraphs(paragraph_list: list[str], target_chars: int) -> list[str]:
        """
        按父切块预算聚合连续段落

        :param paragraph_list: 文档段落列表
        :param target_chars: 单个切块目标字符数
        :return: 聚合后的父切块文本列表
        """

        if not paragraph_list:
            return []
        packed_list: list[str] = []
        current_list: list[str] = []
        current_length = 0
        for paragraph in paragraph_list:
            if len(paragraph) > target_chars:
                if current_list:
                    packed_list.append("\n\n".join(current_list))
                    current_list = []
                    current_length = 0
                packed_list.extend(
                    paragraph[start : start + target_chars]
                    for start in range(0, len(paragraph), target_chars)
                )
                continue
            projected_length = current_length + len(paragraph) + 2
            if current_list and projected_length > target_chars:
                packed_list.append("\n\n".join(current_list))
                current_list = []
                current_length = 0
            current_list.append(paragraph)
            current_length += len(paragraph) + 2
        if current_list:
            packed_list.append("\n\n".join(current_list))
        return packed_list

    def _sliding_window(self, text: str) -> list[str]:
        """
        按重叠窗口切分长文本

        :param text: 待处理文本
        :return: 带重叠区域的文本窗口列表
        """

        target_chars = CHILD_TARGET_TOKENS * self._CHARS_PER_TOKEN
        overlap_chars = CHILD_OVERLAP_TOKENS * self._CHARS_PER_TOKEN
        if len(text) <= target_chars:
            return [text]
        child_list: list[str] = []
        start = 0
        while start < len(text):
            end = min(start + target_chars, len(text))
            child_list.append(text[start:end].strip())
            if end == len(text):
                break
            start = end - overlap_chars
        return [child for child in child_list if child]

    def _create_chunk(
        self,
        *,
        chunk_id: int,
        resource_id: int,
        conversation_id: int,
        parent_chunk_id: int | None,
        chunk_kind: ChunkKind,
        chunk_no: int,
        text_content: str,
        document: ParsedDocument,
        page_from: int | None,
        page_to: int | None,
    ) -> ResourceChunk:
        """
        创建具有稳定标识的资源切块

        :param chunk_id: 资源切块 ID
        :param resource_id: 资源索引 ID
        :param conversation_id: 会话 ID
        :param parent_chunk_id: 父切块 ID
        :param chunk_kind: 资源切块类型
        :param chunk_no: 资源切块顺序号
        :param text_content: 资源切块正文
        :param document: 已解析文档
        :param page_from: 切块起始页码
        :param page_to: 切块结束页码
        :return: 新创建的资源切块
        """

        return ResourceChunk(
            chunk_id=chunk_id,
            resource_id=resource_id,
            conversation_id=conversation_id,
            parent_chunk_id=parent_chunk_id,
            chunk_kind=chunk_kind,
            chunk_no=chunk_no,
            text_content=text_content,
            token_count=max(
                (len(text_content) + self._CHARS_PER_TOKEN - 1) // self._CHARS_PER_TOKEN,
                1,
            ),
            mime_group=document.mime_group,
            heading_path=document.title,
            page_from=page_from,
            page_to=page_to,
        )
