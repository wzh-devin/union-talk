package com.devin.uniontalk.file.outbox;

import com.devin.uniontalk.file.dao.AssetOutboxDao;
import com.devin.uniontalk.file.domain.entity.AssetOutbox;
import com.devin.uniontalk.file.mq.AssetMqPublisher;
import com.devin.uniontalk.infrastructure.file.constant.AssetOutboxConstant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 2026/08/13 00:25.
 *
 * <p>资产发件箱调度发布服务</p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssetOutboxDispatchService {

    /** 资产发件箱Dao. */
    private final AssetOutboxDao assetOutboxDao;

    /** 资产事件发布器. */
    private final AssetMqPublisher assetMqPublisher;

    /**
     * 定时发布待处理资产事件.
     */
    @Scheduled(fixedDelayString = AssetOutboxConstant.PUBLISH_FIXED_DELAY_EXPRESSION)
    public void dispatch() {
        List<AssetOutbox> outboxList = assetOutboxDao.claimPublishableList(
                AssetOutboxConstant.CLAIM_BATCH_SIZE
        );
        outboxList.forEach(this::publish);
    }

    /**
     * 发布单条资产事件.
     *
     * @param outbox 发件箱事件
     */
    private void publish(final AssetOutbox outbox) {
        try {
            assetMqPublisher.publish(outbox);
            assetOutboxDao.markPublished(outbox);
        } catch (Exception e) {
            try {
                assetOutboxDao.markFailed(outbox);
            } catch (Exception markError) {
                log.error("资产发件箱失败状态回写异常, eventId={}", outbox.getEventId(), markError);
            }
            log.warn("资产发件箱发布失败, eventId={}", outbox.getEventId(), e);
        }
    }
}
