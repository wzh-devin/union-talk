package com.devin.uniontalk.infrastructure.message.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 2026/05/20 16:00.
 *
 * <p>
 * 会话类型枚举
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum ConversationTypeEnum {

    /**
     * 私聊会话.
     */
    PRIVATE,

    /**
     * 群聊会话.
     */
    GROUP
}
