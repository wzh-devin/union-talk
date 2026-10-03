"""扫描型 PDF OCR 配置。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/08/13 12:15
"""

from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class OcrSettings(BaseSettings):
    """Poppler 页面渲染与 Tesseract 识别配置。"""

    model_config = SettingsConfigDict(
        env_prefix="AGENT_OCR_",
        env_file=".env",
        extra="ignore",
    )

    enabled: bool = Field(default=True, description="是否启用扫描型PDF OCR")
    languages: str = Field(default="chi_sim+eng", min_length=1, description="OCR语言组合")
    dpi: int = Field(default=220, ge=150, le=400, description="PDF页面渲染分辨率")
    page_timeout_seconds: int = Field(
        default=60,
        ge=5,
        le=300,
        description="单页OCR超时秒数",
    )
    max_pages: int = Field(default=100, ge=1, le=1000, description="单份PDF最大OCR页数")
