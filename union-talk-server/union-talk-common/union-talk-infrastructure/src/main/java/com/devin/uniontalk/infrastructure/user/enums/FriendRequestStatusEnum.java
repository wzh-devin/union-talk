package com.devin.uniontalk.infrastructure.user.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 2026/05/17 15:00.
 *
 * <p>
 * 好友申请状态枚举
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum FriendRequestStatusEnum {
    /**
     * 待处理.
     */
    PENDING,

    /**
     * 已接受.
     */
    ACCEPTED,

    /**
     * 已拒绝.
     */
    REJECTED
}
