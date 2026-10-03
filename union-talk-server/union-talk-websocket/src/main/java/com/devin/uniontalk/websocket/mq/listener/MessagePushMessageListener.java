package com.devin.uniontalk.websocket.mq.listener;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;
import com.devin.uniontalk.rabbitmq.constant.RabbitMqConstant;
import com.devin.uniontalk.rabbitmq.domain.event.ConversationUpdatedEvent;
import com.devin.uniontalk.rabbitmq.domain.event.MessageCreatedEvent;
import com.devin.uniontalk.websocket.domain.vo.resp.WsBaseRespVO;
import com.devin.uniontalk.websocket.service.WebSocketConnectionService;
import com.devin.uniontalk.websocket.service.adapter.WsFrameAdapter;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 2026/05/20 18:00.
 *
 * <p>
 * 消息 WebSocket 推送监听器
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessagePushMessageListener {

    /**
     * WebSocket 连接服务.
     */
    private final WebSocketConnectionService webSocketConnectionService;

    /**
     * 监听消息创建事件并推送给在线成员.
     *
     * @param message RabbitMQ 消息
     */
    @RabbitListener(queues = RabbitMqConstant.MESSAGE_CREATED_WEBSOCKET_QUEUE)
    public void onMessageCreated(final Message message) {
        MessageCreatedEvent event = parseEvent(message, MessageCreatedEvent.class, "消息创建");
        if (Objects.isNull(event) || receiverUserIdListIsEmpty(event.getReceiverUserIdList())) {
            return;
        }
        int sentConnectionCount = pushToUsers(
                event.getReceiverUserIdList(),
                WsFrameAdapter.messageCreated(event)
        );
        log.info(
                "消息创建WebSocket分发完成, eventId={}, messageId={}, "
                        + "conversationId={}, sentConnectionCount={}",
                event.getEventId(),
                event.getMessageId(),
                event.getConversationId(),
                sentConnectionCount
        );
    }

    /**
     * 监听会话更新事件并推送给在线成员.
     *
     * @param message RabbitMQ 消息
     */
    @RabbitListener(queues = RabbitMqConstant.CONVERSATION_UPDATED_WEBSOCKET_QUEUE)
    public void onConversationUpdated(final Message message) {
        ConversationUpdatedEvent event = parseEvent(message, ConversationUpdatedEvent.class, "会话更新");
        if (Objects.isNull(event) || receiverUserIdListIsEmpty(event.getReceiverUserIdList())) {
            return;
        }
        int sentConnectionCount = pushToUsers(
                event.getReceiverUserIdList(),
                WsFrameAdapter.conversationUpdated(event)
        );
        log.info(
                "会话更新WebSocket分发完成, eventId={}, conversationId={}, "
                        + "lastMsgId={}, sentConnectionCount={}",
                event.getEventId(),
                event.getConversationId(),
                event.getLastMsgId(),
                sentConnectionCount
        );
    }

    /**
     * 推送响应帧给指定用户列表.
     *
     * @param userIdList 用户id列表
     * @param resp       响应帧
     * @return 已写入响应帧的本机连接数量
     */
    private int pushToUsers(final List<BigInteger> userIdList, final WsBaseRespVO<?> resp) {
        return userIdList.stream()
                .filter(Objects::nonNull)
                .distinct()
                .mapToInt(userId -> webSocketConnectionService.sendToUser(userId, resp))
                .sum();
    }

    /**
     * 判断推送用户列表是否为空.
     *
     * @param userIdList 用户id列表
     * @return 是否为空
     */
    private boolean receiverUserIdListIsEmpty(final List<BigInteger> userIdList) {
        return Objects.isNull(userIdList) || userIdList.isEmpty();
    }

    /**
     * 解析事件消息.
     *
     * @param message   RabbitMQ 消息
     * @param eventType 事件类型
     * @param scene     事件场景
     * @param <T>       事件泛型
     * @return 事件对象
     */
    private <T> T parseEvent(final Message message, final Class<T> eventType, final String scene) {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        try {
            return JSON.parseObject(body, eventType);
        } catch (JSONException e) {
            log.warn("解析{}MQ事件失败: {}", scene, e.getMessage());
            return null;
        }
    }
}
