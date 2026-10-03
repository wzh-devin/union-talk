"""文档解析与层级切块测试。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/29 19:21
"""

from io import BytesIO

import pytest
from pypdf import PdfWriter

from union_talk_agent.agent_run.domain.exceptions import NonRetryableAgentError
from union_talk_agent.knowledge.application.document_parser import DocumentParser
from union_talk_agent.knowledge.application.hierarchical_chunker import (
    HierarchicalChunker,
)
from union_talk_agent.knowledge.application.ports.pdf_ocr_gateway import PdfOcrGateway
from union_talk_agent.knowledge.domain.enums import ChunkKind, MimeGroup


class IncrementingIdGenerator:
    """测试用递增 ID。"""

    def __init__(self) -> None:
        """
        初始化 IncrementingIdGenerator

        :return: 无返回值
        """

        self._value = 0

    def next_id(self) -> int:
        """
        返回下一个正整数

        :return: 新生成的业务 ID
        """

        self._value += 1
        return self._value


class StubPdfOcrGateway(PdfOcrGateway):
    """测试用 PDF OCR 边界。"""

    def __init__(self, page_text_map: dict[int, str] | None = None) -> None:
        """
        初始化测试 OCR 边界

        :param page_text_map: 页码到识别文字的映射
        :return: 无返回值
        """

        self._page_text_map = page_text_map or {}

    def extract_page_text(
        self,
        content: bytes,
        page_number: int,
        page_count: int,
    ) -> str:
        """
        返回指定页码的预置文字

        :param content: PDF 文件内容
        :param page_number: 从一开始的页码
        :param page_count: PDF 总页数
        :return: 预置 OCR 文字
        """

        del content, page_count
        return self._page_text_map.get(page_number, "")


def build_document_parser(page_text_map: dict[int, str] | None = None) -> DocumentParser:
    """
    构造使用测试 OCR 边界的文档解析器

    :param page_text_map: 页码到识别文字的映射
    :return: 测试使用的文档解析器
    """

    return DocumentParser(StubPdfOcrGateway(page_text_map))


def test_html_is_parsed_and_chunked_into_parent_child() -> None:
    """
    HTML 脚本标签不进入正文，正文生成 Parent/Child

    :return: 无返回值
    """

    document = build_document_parser().parse(
        "guide.html",
        "text/html",
        b"<html><body><h1>Guide</h1><p>Hello world</p></body></html>",
    )
    chunk_list = HierarchicalChunker(IncrementingIdGenerator()).chunk(
        resource_id=10,
        conversation_id=20,
        document=document,
    )

    assert document.mime_group is MimeGroup.HTML
    assert "Hello world" in document.text_content
    assert [chunk.chunk_kind for chunk in chunk_list] == [
        ChunkKind.PARENT,
        ChunkKind.CHILD,
    ]
    assert chunk_list[1].parent_chunk_id == chunk_list[0].chunk_id


def test_unknown_binary_type_is_rejected() -> None:
    """
    未知二进制格式不能尝试猜测解析

    :return: 无返回值
    """

    with pytest.raises(NonRetryableAgentError):
        build_document_parser().parse("archive.zip", "application/zip", b"PK\x03\x04")


def test_json_is_parsed_with_paths_and_chunked() -> None:
    """
    JSON 标量携带字段路径并进入父子切块

    :return: 无返回值
    """

    document = build_document_parser().parse(
        "export.json",
        "application/json",
        (
            b'{"schemaVersion":2,"module":"agent","items":[{"name":"RAG",'
            b'"enabled":true}],"embedded":"{\\"kind\\":\\"TOOL_CALL\\"}"}'
        ),
    )
    chunk_list = HierarchicalChunker(IncrementingIdGenerator()).chunk(
        resource_id=10,
        conversation_id=20,
        document=document,
    )

    assert document.mime_group is MimeGroup.JSON
    assert '$.items[0].name: "RAG"' in document.text_content
    assert "$.items[0].enabled: true" in document.text_content
    assert '$.embedded.kind: "TOOL_CALL"' in document.text_content
    assert [chunk.chunk_kind for chunk in chunk_list] == [
        ChunkKind.PARENT,
        ChunkKind.CHILD,
    ]


def test_invalid_json_is_rejected() -> None:
    """
    格式错误的 JSON 不得进入切块和向量流程

    :return: 无返回值
    """

    with pytest.raises(NonRetryableAgentError):
        build_document_parser().parse("broken.json", "application/json", b'{"name":')


def test_long_text_child_windows_overlap() -> None:
    """
    长父块生成多个有重叠的 Child

    :return: 无返回值
    """

    document = build_document_parser().parse(
        "long.txt",
        "text/plain",
        ("A" * 5000).encode(),
    )
    chunk_list = HierarchicalChunker(IncrementingIdGenerator()).chunk(
        resource_id=10,
        conversation_id=20,
        document=document,
    )
    child_list = [chunk for chunk in chunk_list if chunk.chunk_kind is ChunkKind.CHILD]

    assert len(child_list) >= 3
    assert child_list[0].text_content[-100:] == child_list[1].text_content[220:320]


def test_oversized_paragraph_is_split_into_parent_budget() -> None:
    """
    超长单段不得突破 Parent 目标字符预算

    :return: 无返回值
    """

    document = build_document_parser().parse(
        "oversized.txt",
        "text/plain",
        ("A" * 7000).encode(),
    )
    chunk_list = HierarchicalChunker(IncrementingIdGenerator()).chunk(
        resource_id=10,
        conversation_id=20,
        document=document,
    )
    parent_list = [chunk for chunk in chunk_list if chunk.chunk_kind is ChunkKind.PARENT]

    assert len(parent_list) == 2
    assert max(len(chunk.text_content) for chunk in parent_list) <= 6400


def test_scanned_pdf_uses_ocr_and_keeps_page_number() -> None:
    """
    无文本层 PDF 使用 OCR 并将页码写入父子切块

    :return: 无返回值
    """

    output = BytesIO()
    writer = PdfWriter()
    writer.add_blank_page(width=595, height=842)
    writer.write(output)
    document = build_document_parser({1: "第一页扫描文档内容"}).parse(
        "scan.pdf",
        "application/pdf",
        output.getvalue(),
    )
    chunk_list = HierarchicalChunker(IncrementingIdGenerator()).chunk(
        resource_id=10,
        conversation_id=20,
        document=document,
    )

    assert document.text_content == "第一页扫描文档内容"
    assert document.page_list[0].page_number == 1
    assert [chunk.page_from for chunk in chunk_list] == [1, 1]
    assert [chunk.page_to for chunk in chunk_list] == [1, 1]
