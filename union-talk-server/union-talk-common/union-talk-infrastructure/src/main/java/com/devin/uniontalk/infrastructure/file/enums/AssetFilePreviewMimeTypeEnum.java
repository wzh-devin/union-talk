package com.devin.uniontalk.infrastructure.file.enums;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.ContentType;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * 2026/07/22 02:29:23.
 *
 * <p>
 * 资产文件浏览器预览 MIME 类型枚举
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
public enum AssetFilePreviewMimeTypeEnum {

    /**
     * PDF 文件.
     */
    PDF("application/pdf", "pdf"),

    /**
     * 纯文本文件.
     */
    PLAIN_TEXT(
            ContentType.TEXT_PLAIN.toString(StandardCharsets.UTF_8),
            "md", "markdown", "txt", "log", "conf", "ini", "ts", "tsx", "js", "jsx", "java", "kt",
            "py", "go", "rs", "sql", "sh", "yaml", "yml", "html", "htm", "css"
    ),

    /**
     * JSON 文件.
     */
    JSON(ContentType.JSON.toString(StandardCharsets.UTF_8), "json"),

    /**
     * XML 文件.
     */
    XML(ContentType.XML.toString(StandardCharsets.UTF_8), "xml"),

    /**
     * CSV 文件.
     */
    CSV(ContentType.build("text/csv", StandardCharsets.UTF_8), "csv");

    /**
     * 浏览器预览 MIME 类型.
     */
    private final String mimeType;

    /**
     * 文件扩展名数组.
     */
    private final String[] fileExtArray;

    /**
     * 构造资产文件浏览器预览 MIME 类型枚举.
     *
     * @param mimeType     浏览器预览 MIME 类型
     * @param fileExtArray 文件扩展名数组
     */
    AssetFilePreviewMimeTypeEnum(final String mimeType, final String... fileExtArray) {
        this.mimeType = mimeType;
        this.fileExtArray = fileExtArray;
    }

    /**
     * 获取浏览器预览 MIME 类型.
     *
     * @param fileExt  文件扩展名
     * @param mimeType 原始 MIME 类型
     * @return 浏览器预览 MIME 类型
     */
    public static String getMimeType(final String fileExt, final String mimeType) {
        AssetFilePreviewMimeTypeEnum previewMimeTypeEnum = ArrayUtil.firstMatch(
                previewMimeType -> ArrayUtil.containsIgnoreCase(previewMimeType.fileExtArray, fileExt),
                values()
        );
        return Optional.ofNullable(previewMimeTypeEnum)
                .map(previewMimeType -> previewMimeType.mimeType)
                .orElseGet(() -> StrUtil.blankToDefault(mimeType, ContentType.OCTET_STREAM.getValue()));
    }
}
