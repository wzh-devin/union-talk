package com.devin.uniontalk.infrastructure.file.enums;

/**
 * 2026/08/13 00:15.
 *
 * <p>
 * 资产发件箱状态
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
public enum AssetOutboxStatusEnum {

    /** 等待发布. */
    PENDING,

    /** 发布中. */
    PUBLISHING,

    /** 已发布. */
    PUBLISHED,

    /** 发布失败. */
    FAILED
}
