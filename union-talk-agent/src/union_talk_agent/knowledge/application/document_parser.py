"""受支持文档的安全文本解析。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/29 19:15
"""

from io import BytesIO
from pathlib import Path
from typing import cast

import orjson
from bs4 import BeautifulSoup
from docx import Document
from pypdf import PdfReader

from union_talk_agent.agent_run.domain.exceptions import (
    AgentDomainError,
    AgentErrorCode,
    NonRetryableAgentError,
)
from union_talk_agent.knowledge.application.ports.pdf_ocr_gateway import PdfOcrGateway
from union_talk_agent.knowledge.domain.constants import (
    MAX_DOCUMENT_BYTES,
    MAX_JSON_DEPTH,
    MAX_JSON_SCALAR_COUNT,
)
from union_talk_agent.knowledge.domain.enums import MimeGroup
from union_talk_agent.knowledge.domain.resource_chunk import ParsedDocument, ParsedPage

type JsonValue = bool | int | float | str | list["JsonValue"] | dict[str, "JsonValue"] | None


class DocumentParser:
    """按 MIME 白名单解析 PDF、DOCX、文本、Markdown、HTML 和 JSON。"""

    _PARSER_VERSION = "document-parser-v3"

    def __init__(self, pdf_ocr_gateway: PdfOcrGateway) -> None:
        """
        初始化文档解析器

        :param pdf_ocr_gateway: 扫描型 PDF 页面 OCR 边界
        :return: 无返回值
        """

        self._pdf_ocr_gateway = pdf_ocr_gateway

    @property
    def version(self) -> str:
        """
        获取当前文档解析器版本

        :return: 文档解析器版本
        """

        return self._PARSER_VERSION

    def parse(
        self,
        file_name: str,
        mime_type: str,
        content: bytes,
    ) -> ParsedDocument:
        """
        校验大小和 MIME 后提取纯文本

        :param file_name: 资源文件名称
        :param mime_type: 资源 MIME 类型
        :param content: 待处理的内容
        :return: 包含纯文本和文档类型的解析结果
        """

        if len(content) > MAX_DOCUMENT_BYTES:
            raise NonRetryableAgentError(
                AgentErrorCode.RESOURCE_TOO_LARGE,
                "资源文件超过50MB限制",
            )
        mime_group = self._identify_mime_group(file_name, mime_type)
        page_list: tuple[ParsedPage, ...] = ()
        if mime_group is MimeGroup.PDF:
            page_list = tuple(
                ParsedPage(
                    page_number=page.page_number,
                    text_content="\n".join(
                        line.rstrip() for line in page.text_content.replace("\x00", "").splitlines()
                    ).strip(),
                )
                for page in self._parse_pdf(content)
            )
            text = "\n\n".join(page.text_content for page in page_list)
        elif mime_group is MimeGroup.DOCX:
            text = self._parse_docx(content)
        elif mime_group is MimeGroup.HTML:
            text = self._parse_html(content)
        elif mime_group is MimeGroup.JSON:
            text = self._parse_json(content)
        elif mime_group in {MimeGroup.TEXT, MimeGroup.MARKDOWN}:
            text = self._decode_text(content)
        else:
            raise NonRetryableAgentError(
                AgentErrorCode.RESOURCE_UNSUPPORTED,
                "当前文件类型不支持索引",
            )
        normalized_text = "\n".join(
            line.rstrip() for line in text.replace("\x00", "").splitlines()
        ).strip()
        if not normalized_text:
            error_code = (
                AgentErrorCode.RESOURCE_OCR_REQUIRED
                if mime_group is MimeGroup.PDF
                else AgentErrorCode.RESOURCE_UNSUPPORTED
            )
            raise NonRetryableAgentError(error_code, "资源未提取到可索引文本")
        return ParsedDocument(
            text_content=normalized_text,
            mime_group=mime_group,
            title=Path(file_name).stem,
            page_list=page_list,
        )

    @staticmethod
    def _identify_mime_group(file_name: str, mime_type: str) -> MimeGroup:
        """
        根据文件信息识别文档类型

        :param file_name: 资源文件名称
        :param mime_type: 资源 MIME 类型
        :return: 识别出的文档类型
        """

        normalized_mime = mime_type.lower().split(";", maxsplit=1)[0].strip()
        extension = Path(file_name).suffix.lower()
        if normalized_mime == "application/pdf" or extension == ".pdf":
            return MimeGroup.PDF
        if (
            normalized_mime
            == "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            or extension == ".docx"
        ):
            return MimeGroup.DOCX
        if normalized_mime in {"text/markdown", "text/x-markdown"} or extension in {
            ".md",
            ".markdown",
        }:
            return MimeGroup.MARKDOWN
        if normalized_mime in {"text/html", "application/xhtml+xml"} or extension in {
            ".html",
            ".htm",
        }:
            return MimeGroup.HTML
        if (
            normalized_mime == "application/json"
            or normalized_mime.endswith("+json")
            or extension == ".json"
        ):
            return MimeGroup.JSON
        if normalized_mime.startswith("text/") or extension in {".txt", ".log", ".csv"}:
            return MimeGroup.TEXT
        return MimeGroup.UNKNOWN

    def _parse_pdf(self, content: bytes) -> tuple[ParsedPage, ...]:
        """
        解析 PDF 文档正文

        :param content: 待处理的内容
        :return: 按页保存的 PDF 正文
        """

        try:
            reader = PdfReader(BytesIO(content))
            page_count = len(reader.pages)
            page_list: list[ParsedPage] = []
            for page_index, page in enumerate(reader.pages):
                text = (page.extract_text() or "").strip()
                if not text:
                    text = self._pdf_ocr_gateway.extract_page_text(
                        content,
                        page_index + 1,
                        page_count,
                    )
                page_list.append(
                    ParsedPage(
                        page_number=page_index + 1,
                        text_content=text,
                    )
                )
            return tuple(page_list)
        except AgentDomainError:
            raise
        except Exception as error:
            raise NonRetryableAgentError(
                AgentErrorCode.RESOURCE_UNSUPPORTED,
                "PDF解析失败",
            ) from error

    @staticmethod
    def _parse_docx(content: bytes) -> str:
        """
        解析 DOCX 文档正文

        :param content: 待处理的内容
        :return: DOCX 正文
        """

        try:
            document = Document(BytesIO(content))
            return "\n".join(paragraph.text for paragraph in document.paragraphs)
        except Exception as error:
            raise NonRetryableAgentError(
                AgentErrorCode.RESOURCE_UNSUPPORTED,
                "DOCX解析失败",
            ) from error

    @classmethod
    def _parse_html(cls, content: bytes) -> str:
        """
        解析 HTML 文档正文

        :param content: 待处理的内容
        :return: HTML 正文
        """

        return BeautifulSoup(cls._decode_text(content), "html.parser").get_text(
            "\n",
            strip=True,
        )

    @classmethod
    def _parse_json(cls, content: bytes) -> str:
        """
        将 JSON 叶子节点转换为带路径的检索文本

        :param content: 待处理的 JSON 内容
        :return: 按 JSONPath 标识字段来源的检索文本
        """

        try:
            value = cast(JsonValue, orjson.loads(content.removeprefix(b"\xef\xbb\xbf")))
        except orjson.JSONDecodeError as error:
            raise NonRetryableAgentError(
                AgentErrorCode.RESOURCE_UNSUPPORTED,
                "JSON文档格式无效",
            ) from error
        entry_list: list[str] = []
        cls._append_json_entry_list(value, "$", 0, entry_list)
        if not entry_list:
            raise NonRetryableAgentError(
                AgentErrorCode.RESOURCE_UNSUPPORTED,
                "JSON文档未包含可索引内容",
            )
        return "\n\n".join(entry_list)

    @classmethod
    def _append_json_entry_list(
        cls,
        value: JsonValue,
        path: str,
        depth: int,
        entry_list: list[str],
    ) -> None:
        """
        递归收集 JSON 标量值并保留字段路径

        :param value: 当前 JSON 节点
        :param path: 当前节点 JSONPath
        :param depth: 当前嵌套层级
        :param entry_list: 已收集的检索文本列表
        :return: 无返回值
        """

        if depth > MAX_JSON_DEPTH:
            raise NonRetryableAgentError(
                AgentErrorCode.RESOURCE_TOO_LARGE,
                f"JSON文档嵌套层级超过{MAX_JSON_DEPTH}层限制",
            )
        if isinstance(value, dict):
            for key, child in value.items():
                child_path = (
                    f"{path}.{key}"
                    if key.isidentifier()
                    else f"{path}[{orjson.dumps(key).decode('utf-8')}]"
                )
                cls._append_json_entry_list(child, child_path, depth + 1, entry_list)
            return
        if isinstance(value, list):
            for index, child in enumerate(value):
                cls._append_json_entry_list(
                    child,
                    f"{path}[{index}]",
                    depth + 1,
                    entry_list,
                )
            return
        if value is None or value == "":
            return
        if isinstance(value, str) and value.lstrip().startswith(("{", "[")):
            try:
                nested_value = cast(JsonValue, orjson.loads(value))
            except orjson.JSONDecodeError:
                pass
            else:
                cls._append_json_entry_list(
                    nested_value,
                    path,
                    depth + 1,
                    entry_list,
                )
                return
        if len(entry_list) >= MAX_JSON_SCALAR_COUNT:
            raise NonRetryableAgentError(
                AgentErrorCode.RESOURCE_TOO_LARGE,
                f"JSON文档标量数量超过{MAX_JSON_SCALAR_COUNT}项限制",
            )
        entry_list.append(f"{path}: {orjson.dumps(value).decode('utf-8')}")

    @staticmethod
    def _decode_text(content: bytes) -> str:
        """
        按候选编码解码文本内容

        :param content: 待处理的内容
        :return: 解码后的正文
        """

        try:
            return content.decode("utf-8-sig")
        except UnicodeDecodeError as error:
            raise NonRetryableAgentError(
                AgentErrorCode.RESOURCE_UNSUPPORTED,
                "文本资源不是有效UTF-8编码",
            ) from error
