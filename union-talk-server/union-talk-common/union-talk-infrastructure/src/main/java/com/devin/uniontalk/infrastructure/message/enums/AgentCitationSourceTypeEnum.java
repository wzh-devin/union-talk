package com.devin.uniontalk.infrastructure.message.enums;

import java.util.Arrays;
import java.util.Objects;

/**
 * 2026/07/28 23:50.
 *
 * <p>
 * Agent引用来源类型枚举
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
public enum AgentCitationSourceTypeEnum {

    /**
     * 会话消息片段.
     */
    MESSAGE_SEGMENT,

    /**
     * 会话资源块.
     */
    RESOURCE_CHUNK;

    /**
     * 判断来源类型编码是否受支持.
     *
     * @param code 来源类型编码
     * @return 是否受支持
     */
    public static boolean isSupported(final String code) {
        return Objects.nonNull(code)
                && Arrays.stream(values()).anyMatch(value -> value.name().equals(code));
    }
}
