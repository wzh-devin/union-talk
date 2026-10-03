package com.devin.uniontalk.infrastructure.user.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 2026/5/14 22:21.
 *
 * <p>
 * 用户状态枚举
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum UserStatusEnum {
    /**
     * 正常.
     */
    NORMAL,

    /**
     * 封禁.
     */
    BLOCKED,

    /**
     * 删除.
     */
    DELETED
}
