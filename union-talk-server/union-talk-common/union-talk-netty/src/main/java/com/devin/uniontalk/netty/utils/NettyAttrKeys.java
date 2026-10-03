package com.devin.uniontalk.netty.utils;

import io.netty.util.AttributeKey;
import java.math.BigInteger;

/**
 * 2026/05/19 00:00.
 *
 * <p>
 * Netty Channel 属性Key
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
public final class NettyAttrKeys {

    /**
     * 登录 token 属性.
     */
    public static final AttributeKey<String> TOKEN = AttributeKey.valueOf("token");

    /**
     * 客户端 IP 属性.
     */
    public static final AttributeKey<String> IP = AttributeKey.valueOf("ip");

    /**
     * 登录用户id属性.
     */
    public static final AttributeKey<BigInteger> USER_ID = AttributeKey.valueOf("userId");

    /**
     * WebSocket 连接id属性.
     */
    public static final AttributeKey<String> CONNECTION_ID = AttributeKey.valueOf("connectionId");

    /**
     * 客户端设备id属性.
     */
    public static final AttributeKey<String> DEVICE_ID = AttributeKey.valueOf("deviceId");

    /**
     * 客户端设备类型属性.
     */
    public static final AttributeKey<String> DEVICE_TYPE = AttributeKey.valueOf("deviceType");

    private NettyAttrKeys() {
    }
}
