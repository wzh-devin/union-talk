"""基于 Poppler 与 Tesseract 的 PDF 页面 OCR 适配器。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/08/13 12:15
"""

from typing import cast

import pdf2image
import pytesseract
from pdf2image.exceptions import (
    PDFInfoNotInstalledError,
    PDFPageCountError,
    PDFPopplerTimeoutError,
    PDFSyntaxError,
)
from pytesseract import TesseractError, TesseractNotFoundError

from union_talk_agent.agent_run.domain.exceptions import (
    AgentErrorCode,
    NonRetryableAgentError,
    RetryableAgentError,
)
from union_talk_agent.knowledge.application.ports.pdf_ocr_gateway import PdfOcrGateway


class TesseractPdfOcr(PdfOcrGateway):
    """按页渲染扫描型 PDF 并调用本地 Tesseract。"""

    def __init__(
        self,
        *,
        enabled: bool,
        languages: str,
        dpi: int,
        page_timeout_seconds: int,
        max_pages: int,
    ) -> None:
        """
        初始化 Tesseract PDF OCR 适配器

        :param enabled: 是否允许执行 OCR
        :param languages: Tesseract 语言组合
        :param dpi: PDF 页面渲染分辨率
        :param page_timeout_seconds: 单页渲染和识别超时秒数
        :param max_pages: 单份 PDF 允许 OCR 的最大页数
        :return: 无返回值
        """

        self._enabled = enabled
        self._languages = languages
        self._dpi = dpi
        self._page_timeout_seconds = page_timeout_seconds
        self._max_pages = max_pages

    def extract_page_text(
        self,
        content: bytes,
        page_number: int,
        page_count: int,
    ) -> str:
        """
        渲染并识别一个没有原生文本层的 PDF 页面

        :param content: PDF 文件内容
        :param page_number: 从一开始的页码
        :param page_count: PDF 总页数
        :return: OCR 识别出的页面文字；空白页返回空字符串
        :raises NonRetryableAgentError: OCR 未启用、页数超限或文件无法识别
        :raises RetryableAgentError: Poppler 或 Tesseract 运行环境不可用
        """

        if not self._enabled:
            raise NonRetryableAgentError(
                AgentErrorCode.RESOURCE_OCR_REQUIRED,
                "扫描型PDF需要启用OCR后才能建立索引",
            )
        if page_count > self._max_pages:
            raise NonRetryableAgentError(
                AgentErrorCode.RESOURCE_TOO_LARGE,
                f"扫描型PDF页数超过{self._max_pages}页OCR限制",
            )
        image_list = []
        try:
            image_list = pdf2image.convert_from_bytes(
                content,
                dpi=self._dpi,
                first_page=page_number,
                last_page=page_number,
                thread_count=1,
                grayscale=True,
                timeout=self._page_timeout_seconds,
            )
            if not image_list:
                return ""
            return cast(
                str,
                pytesseract.image_to_string(
                    image_list[0],
                    lang=self._languages,
                    timeout=self._page_timeout_seconds,
                ),
            ).strip()
        except PDFInfoNotInstalledError as error:
            raise RetryableAgentError(
                AgentErrorCode.INFRASTRUCTURE_UNAVAILABLE,
                "OCR运行环境缺少Poppler",
            ) from error
        except TesseractNotFoundError as error:
            raise RetryableAgentError(
                AgentErrorCode.INFRASTRUCTURE_UNAVAILABLE,
                "OCR运行环境缺少Tesseract",
            ) from error
        except PDFPopplerTimeoutError as error:
            raise NonRetryableAgentError(
                AgentErrorCode.RESOURCE_OCR_REQUIRED,
                f"PDF第{page_number}页OCR渲染超时",
            ) from error
        except (PDFPageCountError, PDFSyntaxError, TesseractError, RuntimeError) as error:
            raise NonRetryableAgentError(
                AgentErrorCode.RESOURCE_OCR_REQUIRED,
                f"PDF第{page_number}页OCR识别失败",
            ) from error
        finally:
            for image in image_list:
                image.close()
