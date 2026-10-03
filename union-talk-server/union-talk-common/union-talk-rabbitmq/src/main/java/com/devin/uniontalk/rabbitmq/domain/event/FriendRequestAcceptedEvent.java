package com.devin.uniontalk.rabbitmq.domain.event;

import com.devin.uniontalk.infrastructure.user.enums.FriendRequestStatusEnum;
import java.math.BigInteger;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 2026/05/20 18:30.
 *
 * <p>
 * 好友申请同意事件
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
public class FriendRequestAcceptedEvent {

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
     * 同意用户id.
     */
    private BigInteger toUserId;

    /**
     * 同意用户名.
     */
    private String toUsername;

    /**
     * 同意用户头像.
     */
    private String toAvatarUrl;

    /**
     * 会话id.
     */
    private BigInteger conversationId;

    /**
     * 申请状态.
     */
    private FriendRequestStatusEnum status;

    /**
     * 处理时间.
     */
    private Date handledAt;

    /**
     * 推送内容.
     */
    private String content;
}
