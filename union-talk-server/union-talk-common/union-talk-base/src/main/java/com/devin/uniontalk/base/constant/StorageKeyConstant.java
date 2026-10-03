package com.devin.uniontalk.base.constant;

import com.devin.uniontalk.base.utils.IdGenerator;

/**
 * 2026/7/17 11:29.
 *
 * <p>
 * 对象存储Key常量
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
public class StorageKeyConstant {
    /**
     * 用户模块.
     */
    public static final String USER = "user";

    /**
     * 用户头像.
     */
    public static final String USER_AVATAR = USER + "/avatar";

    /**
     * 构建存储Key.
     *
     * @param storageKey 存储业务目录
     * @param directory  对象所属目录
     * @param suffix     文件后缀
     * @return 对象存储Key
     */
    public static String buildStorageKey(
            final String storageKey,
            final String directory,
            final String suffix
    ) {
        String uuid = IdGenerator.nextKey(32).toLowerCase();
        return storageKey + "/" + directory + "/" + uuid + "." + suffix;
    }
}
