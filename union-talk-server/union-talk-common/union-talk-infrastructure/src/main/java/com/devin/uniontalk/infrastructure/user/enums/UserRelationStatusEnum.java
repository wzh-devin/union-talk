package com.devin.uniontalk.infrastructure.user.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 2026/05/17 15:00.
 *
 * <p>
 * 用户关系状态枚举
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum UserRelationStatusEnum {
    /**
     * 好友.
     */
    FRIEND,

    /**
     * 已拉黑.
     */
    BLOCKED,

    /**
     * 已删除.
     */
    DELETED
}
