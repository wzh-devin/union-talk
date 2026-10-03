package com.devin.uniontalk.infrastructure.message.constant;

/**
 * 2026/08/06 23:00.
 *
 * <p>
 * 消息提及领域常量
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
public final class MessageMentionConstant {

    /**
     * 单条消息最大提及数量.
     */
    public static final int MAX_COUNT = 100;

    /**
     * 提及展示文本最大长度.
     */
    public static final int DISPLAY_TEXT_MAX_LENGTH = 128;

    /**
     * Agent默认提及展示文本.
     */
    public static final String DEFAULT_AGENT_DISPLAY_TEXT = "@AI";

    /**
     * 私有构造方法，避免实例化.
     */
    private MessageMentionConstant() {
    }
}
