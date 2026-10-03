package com.devin.uniontalk.infrastructure.user.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 2026/5/14 22:22.
 *
 * <p>
 *     注册方式枚举
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum RegisterWayEnum {
    /**
     * 邮箱注册.
     */
    EMAIL,

    /**
     * 手机注册.
     */
    PHONE,

    /**
     * 短信注册.
     */
    SMS,

    /**
     * 邀请注册.
     */
    INVITE,

    /**
     * 微信注册.
     */
    WEI_CHT
}
