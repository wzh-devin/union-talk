package com.devin.uniontalk.message.handler;

import com.devin.uniontalk.base.exception.BizErrorEnum;
import com.devin.uniontalk.base.utils.AssertUtils;
import com.devin.uniontalk.infrastructure.message.enums.MessageTypeEnum;
import org.springframework.stereotype.Component;

/**
 * 2026/07/22 13:30.
 *
 * <p>
 * Emoji消息执行器
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Component
public class EmojiMessageHandler extends AbstractMessageHandler {

    /**
     * Emoji消息最大Unicode码点数量.
     */
    private static final int EMOJI_CONTENT_MAX_CODE_POINT_COUNT = 64;

    /**
     * 查询支持的消息类型.
     *
     * @return Emoji消息类型
     */
    @Override
    public MessageTypeEnum getMessageType() {
        return MessageTypeEnum.EMOJI;
    }

    /**
     * 校验Emoji消息内容.
     *
     * @param content Emoji消息内容
     */
    @Override
    protected void validateContent(final String content) {
        AssertUtils.isTrue(
                content.codePointCount(0, content.length()) <= EMOJI_CONTENT_MAX_CODE_POINT_COUNT,
                BizErrorEnum.MESSAGE_CONTENT_TOO_LONG
        );
    }
}
