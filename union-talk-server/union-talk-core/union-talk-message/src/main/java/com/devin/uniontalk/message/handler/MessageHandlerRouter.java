package com.devin.uniontalk.message.handler;

import com.devin.uniontalk.base.exception.BizErrorEnum;
import com.devin.uniontalk.base.utils.AssertUtils;
import com.devin.uniontalk.infrastructure.message.enums.MessageTypeEnum;
import jakarta.annotation.PostConstruct;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 2026/07/22 13:30.
 *
 * <p>
 * 消息执行器路由器
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
public class MessageHandlerRouter {

    /**
     * Spring自动发现的消息执行器列表.
     */
    private final List<MessageHandler> messageHandlerList;

    /**
     * 消息类型与执行器映射.
     */
    private final Map<MessageTypeEnum, MessageHandler> messageHandlerMap =
            new EnumMap<>(MessageTypeEnum.class);

    /**
     * 注册消息执行器并校验注册完整性.
     */
    @PostConstruct
    public void registerMessageHandler() {
        messageHandlerList.forEach(messageHandler -> {
            MessageTypeEnum messageType = messageHandler.getMessageType();
            AssertUtils.nonNull(
                    messageType,
                    () -> new IllegalStateException("消息执行器类型不能为空")
            );
            MessageHandler registeredHandler = messageHandlerMap.putIfAbsent(messageType, messageHandler);
            AssertUtils.isNull(
                    registeredHandler,
                    () -> new IllegalStateException("消息执行器重复注册: " + messageType)
            );
        });
        AssertUtils.isTrue(
                messageHandlerMap.size() == MessageTypeEnum.values().length,
                () -> new IllegalStateException("消息执行器注册不完整")
        );
    }

    /**
     * 根据消息类型路由执行器.
     *
     * @param type 消息类型
     * @return 消息执行器
     */
    public MessageHandler route(final MessageTypeEnum type) {
        AssertUtils.nonNull(type, BizErrorEnum.MESSAGE_TYPE_UNSUPPORTED);
        MessageHandler messageHandler = messageHandlerMap.get(type);
        AssertUtils.nonNull(messageHandler, BizErrorEnum.MESSAGE_TYPE_UNSUPPORTED);
        return messageHandler;
    }
}
