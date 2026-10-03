package com.devin.uniontalk.seata.constant;

import io.grpc.Metadata;

/**
 * 2026/05/20 19:30.
 *
 * <p>
 * Seata Grpc常量
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
public final class SeataGrpcConstant {

    /**
     * Seata全局事务XID请求头.
     */
    public static final String XID_HEADER = "seata-xid";

    /**
     * Seata全局事务XID元数据Key.
     */
    public static final Metadata.Key<String> XID_METADATA_KEY = Metadata.Key.of(
            XID_HEADER,
            Metadata.ASCII_STRING_MARSHALLER
    );

    private SeataGrpcConstant() {
    }
}
