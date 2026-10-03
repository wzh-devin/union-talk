package com.devin.uniontalk.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.devin.uniontalk.message.domain.entity.MessageOutbox;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 2026/07/28 22:15.
 *
 * <p>
 * 消息事件发件箱Mapper层
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Mapper
public interface MessageOutboxMapper extends BaseMapper<MessageOutbox> {

    /**
     * 获取事件事务锁.
     *
     * @param eventId 事件id
     * @return 锁定结果
     */
    Integer lockEvent(@Param("eventId") String eventId);

    /**
     * 认领可发布事件列表.
     *
     * @param pendingStatus    等待发布状态
     * @param failedStatus     发布失败状态
     * @param publishingStatus 发布中状态
     * @param staleBefore      发布租约失效时间
     * @param batchSize        批量数量
     * @return 发件箱事件列表
     */
    List<MessageOutbox> claimPublishableList(
            @Param("pendingStatus") String pendingStatus,
            @Param("failedStatus") String failedStatus,
            @Param("publishingStatus") String publishingStatus,
            @Param("staleBefore") Date staleBefore,
            @Param("batchSize") Integer batchSize
    );

    /**
     * 标记事件发布成功.
     *
     * @param id               发件箱记录id
     * @param publishingStatus 发布中状态
     * @param publishedStatus  已发布状态
     * @param publishedAt      发布时间
     * @return 受影响行数
     */
    int markPublished(
            @Param("id") BigInteger id,
            @Param("publishingStatus") String publishingStatus,
            @Param("publishedStatus") String publishedStatus,
            @Param("publishedAt") Date publishedAt
    );

    /**
     * 标记事件发布失败.
     *
     * @param outbox           发件箱事件
     * @param publishingStatus 发布中状态
     * @return 受影响行数
     */
    int markFailed(
            @Param("outbox") MessageOutbox outbox,
            @Param("publishingStatus") String publishingStatus
    );
}
