package com.devin.uniontalk.gateway.constant;

/**
 * 2026/07/31 17:10.
 *
 * <p>
 * 网关请求头常量
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
public final class GatewayHeaderConstant {

    /**
     * 网关注入的可信用户标识请求头.
     */
    public static final String TRUSTED_USER_ID_HEADER = "X-User-Id";

    /**
     * 私有构造方法，避免实例化.
     */
    private GatewayHeaderConstant() {
    }
}
