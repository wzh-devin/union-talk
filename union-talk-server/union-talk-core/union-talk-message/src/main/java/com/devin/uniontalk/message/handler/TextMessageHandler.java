package com.devin.uniontalk.message.handler;

import com.devin.uniontalk.base.exception.BizErrorEnum;
import com.devin.uniontalk.base.utils.AssertUtils;
import com.devin.uniontalk.infrastructure.message.enums.MessageTypeEnum;
import org.springframework.stereotype.Component;

/**
 * 2026/7/22 13:14.
 *
 * <p>
 * 文本消息执行器
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Component
public class TextMessageHandler extends AbstractMessageHandler {

    /**
     * 文本消息最大长度.
     */
    private static final int TEXT_CONTENT_MAX_LENGTH = 5000;

    /**
     * 查询支持的消息类型.
     *
     * @return 文本消息类型
     */
    @Override
    public MessageTypeEnum getMessageType() {
        return MessageTypeEnum.TEXT;
    }

    /**
     * 校验文本消息内容.
     *
     * @param content 文本消息内容
     */
    @Override
    protected void validateContent(final String content) {
        AssertUtils.isTrue(
                content.length() <= TEXT_CONTENT_MAX_LENGTH,
                BizErrorEnum.MESSAGE_CONTENT_TOO_LONG
        );
    }
}
