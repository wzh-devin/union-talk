package com.devin.uniontalk.infrastructure.message.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 2026/05/20 16:00.
 *
 * <p>
 * 消息类型枚举
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum MessageTypeEnum {

    /**
     * 文本消息.
     */
    TEXT,

    /**
     * 音频消息.
     */
    AUDIO,

    /**
     * 视频消息.
     */
    VIDEO,

    /**
     * 文件消息.
     */
    FILE,

    /**
     * Emoji消息.
     */
    EMOJI
}
