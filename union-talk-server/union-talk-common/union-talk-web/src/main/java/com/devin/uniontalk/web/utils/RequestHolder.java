package com.devin.uniontalk.web.utils;

import java.math.BigInteger;

/**
 * 2026/5/11 23:31.
 *
 * <p>
 * 请求上下文
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
public class RequestHolder {

    private static final ThreadLocal<BigInteger> USER_ID_HOLDER = new ThreadLocal<>();

    /**
     * 设置用户id.
     *
     * @param uid 用户id
     */
    public static void setUid(final BigInteger uid) {
        USER_ID_HOLDER.set(uid);
    }

    /**
     * 获取用户id.
     *
     * @return 用户id
     */
    public static BigInteger getUid() {
        return USER_ID_HOLDER.get();
    }

    /**
     * 移除用户id.
     */
    public static void remove() {
        USER_ID_HOLDER.remove();
    }
}
