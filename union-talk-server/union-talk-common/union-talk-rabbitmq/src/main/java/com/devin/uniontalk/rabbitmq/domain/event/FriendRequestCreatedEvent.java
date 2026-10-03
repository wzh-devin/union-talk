package com.devin.uniontalk.rabbitmq.domain.event;

import com.devin.uniontalk.infrastructure.user.enums.FriendRequestStatusEnum;
import java.math.BigInteger;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 2026/05/20 00:00.
 *
 * <p>
 * 好友申请创建事件
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FriendRequestCreatedEvent {

    /**
     * 事件id.
     */
    private String eventId;

    /**
     * 好友申请id.
     */
    private BigInteger friendRequestId;

    /**
     * 申请用户id.
     */
    private BigInteger fromUserId;

    /**
     * 目标用户id.
     */
    private BigInteger toUserId;

    /**
     * 申请用户名.
     */
    private String fromUsername;

    /**
     * 申请用户头像.
     */
    private String fromAvatarUrl;

    /**
     * 申请信息.
     */
    private String applyMsg;

    /**
     * 申请状态.
     */
    private FriendRequestStatusEnum status;

    /**
     * 创建时间.
     */
    private Date createdAt;
}
