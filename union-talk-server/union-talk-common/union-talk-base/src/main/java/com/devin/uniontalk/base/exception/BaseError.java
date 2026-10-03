package com.devin.uniontalk.base.exception;

/**
 * 2026/5/16 20:17.
 *
 * <p>
 * 基础错误接口
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
public interface BaseError {
    /**
     * 错误码.
     *
     * @return 错误码
     */
    Integer getErrCode();

    /**
     * 错误信息.
     *
     * @return 错误信息
     */
    String getErrMsg();
}
