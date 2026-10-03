package com.devin.uniontalk.netty.utils;

import io.netty.channel.Channel;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

/**
 * 2026/05/19 00:00.
 *
 * <p>
 * Netty Channel 属性工具
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
public final class NettyChannelUtils {

    private NettyChannelUtils() {
    }

    /**
     * 设置 Channel 属性.
     *
     * @param channel Channel
     * @param key     属性Key
     * @param value   属性值
     * @param <T>     属性类型
     */
    public static <T> void setAttr(final Channel channel, final AttributeKey<T> key, final T value) {
        Attribute<T> attr = channel.attr(key);
        attr.set(value);
    }

    /**
     * 获取 Channel 属性.
     *
     * @param channel Channel
     * @param key     属性Key
     * @param <T>     属性类型
     * @return 属性值
     */
    public static <T> T getAttr(final Channel channel, final AttributeKey<T> key) {
        return channel.attr(key).get();
    }

    /**
     * 移除 Channel 属性.
     *
     * @param channel Channel
     * @param key     属性Key
     * @param <T>     属性类型
     */
    public static <T> void removeAttr(final Channel channel, final AttributeKey<T> key) {
        channel.attr(key).set(null);
    }
}
