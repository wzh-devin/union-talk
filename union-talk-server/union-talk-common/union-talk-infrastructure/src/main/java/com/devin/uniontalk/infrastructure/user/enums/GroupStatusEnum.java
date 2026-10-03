package com.devin.uniontalk.infrastructure.user.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 2026/05/17 15:00.
 *
 * <p>
 * 群聊状态枚举
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum GroupStatusEnum {
    /**
     * 活跃.
     */
    ACTIVE,

    /**
     * 已解散.
     */
    DISSOLVED
}
