package com.devin.uniontalk.infrastructure.file.constant;

/**
 * 2026/08/13 01:10.
 *
 * <p>
 * 资产发件箱常量
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
public final class AssetOutboxConstant {

    /**
     * 资产内容变更事件id前缀.
     */
    public static final String ASSET_CONTENT_CHANGED_EVENT_ID_PREFIX = "asset-content-changed:";

    /**
     * 资产删除事件id前缀.
     */
    public static final String ASSET_DELETED_EVENT_ID_PREFIX = "asset-deleted:";

    /**
     * 初始重试等待时间.
     */
    public static final long INITIAL_RETRY_DELAY_MILLIS = 1000L;

    /**
     * 最大指数退避次数.
     */
    public static final int MAX_BACKOFF_SHIFT = 6;

    /**
     * 最大重试等待时间.
     */
    public static final long MAX_RETRY_DELAY_MILLIS = 60000L;

    /**
     * 单次认领事件数量.
     */
    public static final int CLAIM_BATCH_SIZE = 20;

    /**
     * 单次删除事件对账数量.
     */
    public static final int DELETION_RECONCILE_BATCH_SIZE = 100;

    /**
     * 发件箱调度周期配置表达式.
     */
    public static final String PUBLISH_FIXED_DELAY_EXPRESSION =
            "${asset.outbox.publish-fixed-delay-millis:500}";

    /**
     * 删除事件对账周期配置表达式.
     */
    public static final String DELETION_RECONCILE_FIXED_DELAY_EXPRESSION =
            "${asset.outbox.deletion-reconcile-fixed-delay-millis:30000}";

    /**
     * 私有构造方法，避免实例化.
     */
    private AssetOutboxConstant() {
    }
}
