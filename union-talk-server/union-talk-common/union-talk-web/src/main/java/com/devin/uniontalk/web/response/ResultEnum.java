package com.devin.uniontalk.web.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 2026/5/11 23:46.
 *
 * <p>
 * 返回枚举
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum ResultEnum {

    SYSTEM_ERROR(-1, "系统未知异常"),

    SUCCESS(0, "成功"),

    PARAM_ERROR(400, "请求参数错误"),

    UNAUTHORIZED(401, "未登录"),

    BIZ_ERROR(500, "业务异常");

    private final Integer code;

    private final String message;
}
