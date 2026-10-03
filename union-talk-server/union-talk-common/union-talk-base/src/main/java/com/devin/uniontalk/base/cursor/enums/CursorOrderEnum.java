package com.devin.uniontalk.base.cursor.enums;

/**
 * 2026/05/20 13:05.
 *
 * <p>
 * 游标排序枚举
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
public enum CursorOrderEnum {

    /**
     * 正序.
     */
    ASC,

    /**
     * 倒序.
     */
    DESC;

    /**
     * 判断是否正序.
     *
     * @return 是否正序
     */
    public boolean isAsc() {
        return ASC.equals(this);
    }

    /**
     * 判断是否倒序.
     *
     * @return 是否倒序
     */
    public boolean isDesc() {
        return DESC.equals(this);
    }
}
