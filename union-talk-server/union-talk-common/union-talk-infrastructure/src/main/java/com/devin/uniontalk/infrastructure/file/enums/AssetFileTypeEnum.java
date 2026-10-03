package com.devin.uniontalk.infrastructure.file.enums;

import java.util.Locale;

/**
 * 2026/06/30 18:00.
 *
 * <p>
 * 资产文件类型枚举
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
public enum AssetFileTypeEnum {

    /**
     * 图片.
     */
    IMAGE,

    /**
     * 视频.
     */
    VIDEO,

    /**
     * 音频.
     */
    AUDIO,

    /**
     * 普通文件.
     */
    FILE;

    /**
     * 根据 MIME 类型获取文件类型.
     *
     * @param mimeType MIME 类型
     * @return 文件类型
     */
    public static AssetFileTypeEnum getByMimeType(final String mimeType) {
        if (mimeType == null) {
            return FILE;
        }
        String lowerMimeType = mimeType.toLowerCase(Locale.ROOT);
        if (lowerMimeType.startsWith("image/")) {
            return IMAGE;
        }
        if (lowerMimeType.startsWith("video/")) {
            return VIDEO;
        }
        if (lowerMimeType.startsWith("audio/")) {
            return AUDIO;
        }
        return FILE;
    }
}
