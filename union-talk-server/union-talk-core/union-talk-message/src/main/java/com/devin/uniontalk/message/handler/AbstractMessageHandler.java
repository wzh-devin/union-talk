package com.devin.uniontalk.message.handler;

import com.devin.uniontalk.base.exception.BizErrorEnum;
import com.devin.uniontalk.base.utils.AssertUtils;
import org.springframework.util.StringUtils;

/**
 * 2026/07/22 13:30.
 *
 * <p>
 * 抽象消息执行器
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
public abstract class AbstractMessageHandler implements MessageHandler {

    /**
     * 执行消息类型专属处理.
     *
     * @param content 消息内容
     */
    @Override
    public final void handle(final String content) {
        AssertUtils.isTrue(StringUtils.hasText(content), BizErrorEnum.MESSAGE_CONTENT_EMPTY);
        validateContent(content);
    }

    /**
     * 校验消息类型专属内容.
     *
     * @param content 消息内容
     */
    protected abstract void validateContent(String content);
}
