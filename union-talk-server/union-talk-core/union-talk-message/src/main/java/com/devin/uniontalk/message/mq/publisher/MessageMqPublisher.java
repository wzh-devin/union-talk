package com.devin.uniontalk.message.mq.publisher;

import com.devin.uniontalk.infrastructure.message.enums.MessageOutboxEventTypeEnum;
import com.devin.uniontalk.message.domain.entity.MessageOutbox;
import com.devin.uniontalk.rabbitmq.constant.RabbitMqConstant;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 2026/07/28 22:20.
 *
 * <p>
 * 消息发件箱RabbitMQ发布器
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageMqPublisher {

    /**
     * 发布确认等待时间.
     */
    private static final Duration PUBLISH_CONFIRM_TIMEOUT = Duration.ofSeconds(10);

    /**
     * RabbitMQ模板.
     */
    private final RabbitTemplate rabbitTemplate;

    /**
     * 发布发件箱事件并等待Broker确认.
     *
     * @param outbox 发件箱事件
     */
    public void publish(final MessageOutbox outbox) {
        MessageOutboxEventTypeEnum eventType = MessageOutboxEventTypeEnum.valueOf(outbox.getEventType());
        String exchange = switch (eventType) {
            case MESSAGE_CREATED, AGENT_MENTIONED -> RabbitMqConstant.MESSAGE_EXCHANGE;
            case CONVERSATION_UPDATED -> RabbitMqConstant.CONVERSATION_EXCHANGE;
        };
        String routingKey = switch (eventType) {
            case MESSAGE_CREATED -> RabbitMqConstant.MESSAGE_CREATED_ROUTING_KEY;
            case CONVERSATION_UPDATED -> RabbitMqConstant.CONVERSATION_UPDATED_ROUTING_KEY;
            case AGENT_MENTIONED -> RabbitMqConstant.AGENT_MENTIONED_ROUTING_KEY;
        };
        CorrelationData correlationData = new CorrelationData(outbox.getEventId());
        Message message = new Message(
                outbox.getPayloadJson().getBytes(StandardCharsets.UTF_8),
                buildMessageProperties(outbox.getEventId())
        );
        rabbitTemplate.send(exchange, routingKey, message, correlationData);
        waitForConfirm(outbox, correlationData);
    }

    /**
     * 等待RabbitMQ发布确认.
     *
     * @param outbox          发件箱事件
     * @param correlationData 发布关联数据
     */
    private void waitForConfirm(
            final MessageOutbox outbox,
            final CorrelationData correlationData
    ) {
        try {
            CorrelationData.Confirm confirm = correlationData.getFuture().get(
                    PUBLISH_CONFIRM_TIMEOUT.toMillis(),
                    TimeUnit.MILLISECONDS
            );
            if (!confirm.isAck()) {
                throw new IllegalStateException("RabbitMQ发布未确认: " + confirm.getReason());
            }
            if (correlationData.getReturned() != null) {
                throw new IllegalStateException(
                        "RabbitMQ事件未路由: " + correlationData.getReturned().getReplyText()
                );
            }
            log.info(
                    "消息发件箱事件发布成功, eventId={}, eventType={}",
                    outbox.getEventId(),
                    outbox.getEventType()
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("RabbitMQ发布确认等待被中断", e);
        } catch (ExecutionException | TimeoutException e) {
            throw new IllegalStateException("RabbitMQ发布确认失败", e);
        }
    }

    /**
     * 构建MQ消息属性.
     *
     * @param eventId 事件id
     * @return MQ消息属性
     */
    private MessageProperties buildMessageProperties(final String eventId) {
        MessageProperties messageProperties = new MessageProperties();
        messageProperties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        messageProperties.setContentEncoding(StandardCharsets.UTF_8.name());
        messageProperties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        messageProperties.setMessageId(eventId);
        messageProperties.setTimestamp(new Date());
        return messageProperties;
    }
}
