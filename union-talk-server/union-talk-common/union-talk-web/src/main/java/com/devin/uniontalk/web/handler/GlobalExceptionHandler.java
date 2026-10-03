package com.devin.uniontalk.web.handler;

import cn.dev33.satoken.exception.NotLoginException;
import com.alibaba.fastjson2.JSONObject;
import com.devin.uniontalk.base.exception.BizException;
import com.devin.uniontalk.web.response.ApiResult;
import com.devin.uniontalk.web.response.ResultEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.annotation.ResponseStatus;
import java.util.HashMap;
import java.util.Map;

/**
 * 2026/5/11 23:35.
 *
 * <p>
 * 全局异常处理
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 统一处理参数校验异常.
     *
     * @param e 参数校验异常
     * @return 统一响应结果
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<?> handleValidationException(final MethodArgumentNotValidException e) {
        log.error("MethodArgumentNotValidException ======> ", e);
        Map<String, String> errorMap = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach(error -> {
            String field = ((FieldError) error).getField();
            String message = error.getDefaultMessage();
            errorMap.put(field, message);
        });
        return ApiResult.fail(ResultEnum.PARAM_ERROR.getCode(), JSONObject.toJSONString(errorMap));
    }

    /**
     * 统一处理未登录异常.
     *
     * @param e Sa-Token未登录异常
     * @return 统一响应结果
     */
    @ExceptionHandler(NotLoginException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResult<?> handleNotLoginException(final NotLoginException e) {
        log.warn("NotLoginException ======> {}", e.getMessage());
        return ApiResult.fail(ResultEnum.UNAUTHORIZED);
    }

    /**
     * 统一处理业务异常.
     *
     * @param e 业务异常
     * @return 统一响应结果
     */
    @ExceptionHandler(BizException.class)
    @ResponseStatus(HttpStatus.OK)
    public ApiResult<?> handleBizException(final BizException e) {
        log.error("BizException ======> code={}, msg={}", e.getError().getErrCode(), e.getMessage());
        return ApiResult.fail(e.getError());
    }
}
