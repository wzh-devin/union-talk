package com.devin.uniontalk.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.devin.uniontalk.file.domain.entity.AssetOutbox;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 2026/08/13 00:20.
 *
 * <p>
 * 资产事件发件箱Mapper
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Mapper
public interface AssetOutboxMapper extends BaseMapper<AssetOutbox> {

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
     * @param pendingStatus 等待发布状态
     * @param failedStatus 发布失败状态
     * @param publishingStatus 发布中状态
     * @param staleBefore 租约过期时间
     * @param batchSize 批量数量
     * @return 可发布事件列表
     */
    List<AssetOutbox> claimPublishableList(
            @Param("pendingStatus") String pendingStatus,
            @Param("failedStatus") String failedStatus,
            @Param("publishingStatus") String publishingStatus,
            @Param("staleBefore") Date staleBefore,
            @Param("batchSize") Integer batchSize
    );

    /**
     * 标记发布成功.
     *
     * @param id 发件箱记录id
     * @param publishingStatus 发布中状态
     * @param publishedStatus 已发布状态
     * @param publishedAt 发布时间
     * @return 更新数量
     */
    int markPublished(
            @Param("id") BigInteger id,
            @Param("publishingStatus") String publishingStatus,
            @Param("publishedStatus") String publishedStatus,
            @Param("publishedAt") Date publishedAt
    );

    /**
     * 标记发布失败.
     *
     * @param outbox 发件箱事件
     * @param publishingStatus 发布中状态
     * @return 更新数量
     */
    int markFailed(
            @Param("outbox") AssetOutbox outbox,
            @Param("publishingStatus") String publishingStatus
    );
}
