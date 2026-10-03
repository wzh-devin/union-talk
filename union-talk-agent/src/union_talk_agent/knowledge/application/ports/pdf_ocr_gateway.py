"""PDF 页面 OCR 外部能力边界。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/08/13 12:15
"""

from abc import ABC, abstractmethod


class PdfOcrGateway(ABC):
    """识别没有原生文本层的 PDF 页面。"""

    @abstractmethod
    def extract_page_text(
        self,
        content: bytes,
        page_number: int,
        page_count: int,
    ) -> str:
        """
        识别指定 PDF 页面的文字

        :param content: PDF 文件内容
        :param page_number: 从一开始的页码
        :param page_count: PDF 总页数
        :return: OCR 识别出的页面文字；空白页返回空字符串
        :raises NotImplementedError: 实现类未提供 OCR 能力
        """

        raise NotImplementedError
