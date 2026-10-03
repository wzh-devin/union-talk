package com.devin.uniontalk.infrastructure.file.constant;

import java.math.BigInteger;

/**
 * 2026/06/30 19:30.
 *
 * <p>
 * 资产目录常量
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
public final class AssetFolderConstant {

    /**
     * 虚拟根目录id.
     */
    public static final BigInteger ROOT_FOLDER_ID = BigInteger.ZERO;

    /**
     * 根目录父级id.
     */
    public static final BigInteger ROOT_PARENT_ID = BigInteger.ZERO;

    /**
     * 根目录名称.
     */
    public static final String ROOT_FOLDER_NAME = "/";

    /**
     * 成员目录容器数据库名称.
     */
    public static final String MEMBER_ROOT_FOLDER_NAME = "__member_root__";

    /**
     * 成员目录容器展示名称.
     */
    public static final String MEMBER_ROOT_FOLDER_DISPLAY_NAME = "Member";

    /**
     * 目录路径分隔符.
     */
    public static final String PATH_SEPARATOR = "/";

    /**
     * 私有构造方法，避免实例化.
     */
    private AssetFolderConstant() {
    }
}
