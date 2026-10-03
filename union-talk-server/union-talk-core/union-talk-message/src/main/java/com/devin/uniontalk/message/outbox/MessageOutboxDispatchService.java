package com.devin.uniontalk.message.outbox;

import com.devin.uniontalk.message.dao.MessageOutboxDao;
import com.devin.uniontalk.message.domain.entity.MessageOutbox;
import com.devin.uniontalk.message.mq.publisher.MessageMqPublisher;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 2026/07/28 22:20.
 *
 * <p>
 * 消息发件箱调度发布服务
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageOutboxDispatchService {

    /**
     * 单次认领数量.
     */
    private static final Integer CLAIM_BATCH_SIZE = 20;

    /**
     * 消息发件箱Dao.
     */
    private final MessageOutboxDao messageOutboxDao;

    /**
     * 消息RabbitMQ发布器.
     */
    private final MessageMqPublisher messageMqPublisher;

    /**
     * 定时发布待处理发件箱事件.
     */
    @Scheduled(fixedDelayString = "${message.outbox.publish-fixed-delay-millis:500}")
    public void dispatch() {
        List<MessageOutbox> outboxList = messageOutboxDao.claimPublishableList(CLAIM_BATCH_SIZE);
        outboxList.forEach(this::publish);
    }

    /**
     * 发布单条发件箱事件.
     *
     * @param outbox 发件箱事件
     */
    private void publish(final MessageOutbox outbox) {
        try {
            messageMqPublisher.publish(outbox);
            messageOutboxDao.markPublished(outbox);
        } catch (Exception e) {
            try {
                messageOutboxDao.markFailed(outbox);
            } catch (Exception markFailedException) {
                log.error(
                        "消息发件箱失败状态回写异常, eventId={}, eventType={}",
                        outbox.getEventId(),
                        outbox.getEventType(),
                        markFailedException
                );
            }
            log.warn(
                    "消息发件箱事件发布失败, eventId={}, eventType={}, retryCount={}",
                    outbox.getEventId(),
                    outbox.getEventType(),
                    outbox.getRetryCount(),
                    e
            );
        }
    }
}
