package com.devin.uniontalk.infrastructure.file.enums;

/**
 * 2026/07/22 10:30.
 *
 * <p>
 * 资产目录类型枚举
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
public enum AssetFolderTypeEnum {

    /**
     * 会话虚拟根目录，仅用于响应.
     */
    VIRTUAL_ROOT,

    /**
     * 普通目录.
     */
    NORMAL,

    /**
     * 成员目录容器.
     */
    MEMBER_ROOT,

    /**
     * 成员个人根目录.
     */
    MEMBER_HOME
}
