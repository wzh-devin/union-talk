package com.devin.uniontalk.infrastructure.message.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 2026/05/20 16:00.
 *
 * <p>
 * 用户会话状态枚举
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum UserConversationStatusEnum {

    /**
     * 正常会话.
     */
    ACTIVE,

    /**
     * 已隐藏会话.
     */
    HIDDEN,

    /**
     * 已解散会话.
     */
    DISSOLVED
}
