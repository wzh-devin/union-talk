package com.devin.uniontalk.web.response;

import com.devin.uniontalk.base.exception.BaseError;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 2026/5/11 22:48.
 *
 * <p>
 * 通用响应配置
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
public class ApiResult<T> {
    /**
     * 响应是否成功.
     */
    @Schema(description = "响应是否成功")
    private Boolean success;

    /**
     * 失败码.
     */
    @Schema(description = "失败码")
    private Integer errCode;

    /**
     * 失败信息.
     */
    @Schema(description = "失败信息")
    private String errMsg;

    /**
     * 响应数据.
     */
    @Schema(description = "响应数据")
    private T data;

    /**
     * 响应成功方法.
     *
     * @param <T> 响应数据类型
     * @return ApiResult
     */
    public static <T> ApiResult<T> success() {
        ApiResult<T> result = new ApiResult<>();
        result.setSuccess(Boolean.TRUE);
        result.setData(null);
        return result;
    }

    /**
     * 响应成功方法.
     *
     * @param data 响应数据
     * @param <T>  响应数据类型
     * @return ApiResult
     */
    public static <T> ApiResult<T> success(final T data) {
        ApiResult<T> result = new ApiResult<>();
        result.setSuccess(true);
        result.setData(data);
        return result;
    }

    /**
     * 响应失败方法.
     *
     * @param errCode 失败码
     * @param errMsg  失败消息
     * @param <T>     响应数据类型
     * @return ApiResult
     */
    public static <T> ApiResult<T> fail(final Integer errCode, final String errMsg) {
        ApiResult<T> result = new ApiResult<>();
        result.setSuccess(false);
        result.setErrCode(errCode);
        result.setErrMsg(errMsg);
        return result;
    }

    /**
     * 响应失败方法.
     *
     * @param resultEnum 响应枚举
     * @param <T>     响应数据类型
     * @return ApiResult
     */
    public static <T> ApiResult<T> fail(final ResultEnum resultEnum) {
        ApiResult<T> result = new ApiResult<>();
        result.setSuccess(false);
        result.setErrCode(resultEnum.getCode());
        result.setErrMsg(resultEnum.getMessage());
        return result;
    }

    /**
     * 响应失败方法.
     *
     * @param error 响应枚举
     * @param <T>     响应数据类型
     * @return ApiResult
     */
    public static <T> ApiResult<T> fail(final BaseError error) {
        ApiResult<T> result = new ApiResult<>();
        result.setSuccess(false);
        result.setErrCode(error.getErrCode());
        result.setErrMsg(error.getErrMsg());
        return result;
    }

    /**
     * 响应失败方法.
     *
     * @param error 响应枚举
     * @param errMsg 错误信息
     * @param <T>     响应数据类型
     * @return ApiResult
     */
    public static <T> ApiResult<T> fail(final BaseError error, final String errMsg) {
        ApiResult<T> result = new ApiResult<>();
        result.setSuccess(false);
        result.setErrCode(error.getErrCode());
        result.setErrMsg(errMsg);
        return result;
    }
}
