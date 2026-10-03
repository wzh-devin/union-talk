package com.devin.uniontalk.infrastructure.file.enums;

/**
 * 2026/08/13 00:15.
 *
 * <p>
 * 资产发件箱事件类型
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
public enum AssetOutboxEventTypeEnum {

    /**
     * 资产内容已变更.
     */
    ASSET_CONTENT_CHANGED,

    /**
     * 资产已删除.
     */
    ASSET_DELETED
}
