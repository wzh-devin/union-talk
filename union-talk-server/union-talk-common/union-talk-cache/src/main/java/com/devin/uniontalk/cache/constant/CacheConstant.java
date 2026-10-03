package com.devin.uniontalk.cache.constant;

/**
 * 2026/5/11 23:08.
 *
 * <p>
 * 缓存常量
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
public class CacheConstant {

    /**
     * 邮箱验证码缓存key.
     */
    public static final String EMAIL_CODE = "email_%s:code";

    /**
     * WebSocket 连接信息缓存key.
     */
    public static final String WS_CONNECTION = "ws:conn:%s";

    /**
     * 用户维度 WebSocket 连接集合缓存key.
     */
    public static final String WS_USER_CONNECTIONS = "ws:user:%s";

    /**
     * 服务节点维度 WebSocket 连接集合缓存key.
     */
    public static final String WS_SERVER_CONNECTIONS = "ws:server:%s";

    /**
     * 项目缓存key统一前缀.
     */
    private static final String BASE_KEY = "union_talk:";

    /**
     * 生成缓存key.
     *
     * @param key    key
     * @param params 参数
     * @return 缓存key
     */
    public static String generateKey(final String key, final Object... params) {
        return String.format(BASE_KEY + key, params);
    }

}
