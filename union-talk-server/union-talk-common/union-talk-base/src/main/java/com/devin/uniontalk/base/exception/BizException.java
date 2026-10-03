package com.devin.uniontalk.base.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 2026/5/16 20:14.
 *
 * <p>
 * 业务异常
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class BizException extends RuntimeException {

    private final BaseError error;

    public BizException(final BaseError error) {
        super(error.getErrMsg());
        this.error = error;
    }

    public BizException(final String message, final BaseError error) {
        super(message);
        this.error = error;
    }
}
