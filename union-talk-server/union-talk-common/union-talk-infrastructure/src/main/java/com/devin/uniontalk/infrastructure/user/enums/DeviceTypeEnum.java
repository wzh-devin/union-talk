package com.devin.uniontalk.infrastructure.user.enums;

import java.util.Locale;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 2026/5/16 23:00.
 *
 * <p>
 * 设备类型枚举
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum DeviceTypeEnum {
    /**
     * Web端.
     */
    WEB,

    /**
     * Android端.
     */
    ANDROID,

    /**
     * iOS端.
     */
    IOS,

    /**
     * 桌面端.
     */
    DESKTOP,

    /**
     * 未知设备.
     */
    UNKNOWN;

    /**
     * 根据设备类型编码解析枚举.
     *
     * @param code 设备类型编码
     * @return 设备类型，无法识别时返回未知设备
     */
    public static DeviceTypeEnum of(final String code) {
        if (code == null || code.isBlank()) {
            return UNKNOWN;
        }
        try {
            return valueOf(code.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return UNKNOWN;
        }
    }
}
