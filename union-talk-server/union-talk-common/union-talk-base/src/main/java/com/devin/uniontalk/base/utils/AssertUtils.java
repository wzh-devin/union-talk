package com.devin.uniontalk.base.utils;

import com.devin.uniontalk.base.exception.BaseError;
import com.devin.uniontalk.base.exception.BizException;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * 2026/5/17 00:00.
 *
 * <p>
 * 断言校验工具类，校验失败时抛出指定异常，默认抛出 {@link BizException}
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
public final class AssertUtils {

    private AssertUtils() {
    }

    // ==================== isTrue ====================

    /**
     * 断言条件为真，否则抛出 BizException.
     *
     * @param condition 条件表达式
     * @param error     错误枚举
     */
    public static void isTrue(final boolean condition, final BaseError error) {
        if (!condition) {
            throw new BizException(error);
        }
    }

    /**
     * 断言条件为真，否则抛出 BizException（自定义消息）.
     *
     * @param condition 条件表达式
     * @param error     错误枚举
     * @param message   自定义错误消息
     */
    public static void isTrue(final boolean condition, final BaseError error, final String message) {
        if (!condition) {
            throw new BizException(message, error);
        }
    }

    /**
     * 断言条件为真，否则抛出自定义异常.
     *
     * @param condition         条件表达式
     * @param exceptionSupplier 异常提供者
     * @param <X>               异常类型
     */
    public static <X extends RuntimeException> void isTrue(final boolean condition, final Supplier<X> exceptionSupplier) {
        if (!condition) {
            throw exceptionSupplier.get();
        }
    }

    // ==================== isFalse ====================

    /**
     * 断言条件为假，否则抛出 BizException.
     *
     * @param condition 条件表达式
     * @param error     错误枚举
     */
    public static void isFalse(final boolean condition, final BaseError error) {
        isTrue(!condition, error);
    }

    /**
     * 断言条件为假，否则抛出 BizException（自定义消息）.
     *
     * @param condition 条件表达式
     * @param error     错误枚举
     * @param message   自定义错误消息
     */
    public static void isFalse(final boolean condition, final BaseError error, final String message) {
        isTrue(!condition, error, message);
    }

    /**
     * 断言条件为假，否则抛出自定义异常.
     *
     * @param condition         条件表达式
     * @param exceptionSupplier 异常提供者
     * @param <X>               异常类型
     */
    public static <X extends RuntimeException> void isFalse(final boolean condition, final Supplier<X> exceptionSupplier) {
        isTrue(!condition, exceptionSupplier);
    }

    // ==================== isNull ====================

    /**
     * 断言对象为null，否则抛出 BizException.
     *
     * @param obj   待校验对象
     * @param error 错误枚举
     */
    public static void isNull(final Object obj, final BaseError error) {
        isTrue(obj == null, error);
    }

    /**
     * 断言对象为null，否则抛出 BizException（自定义消息）.
     *
     * @param obj     待校验对象
     * @param error   错误枚举
     * @param message 自定义错误消息
     */
    public static void isNull(final Object obj, final BaseError error, final String message) {
        isTrue(obj == null, error, message);
    }

    /**
     * 断言对象为null，否则抛出自定义异常.
     *
     * @param obj               待校验对象
     * @param exceptionSupplier 异常提供者
     * @param <X>               异常类型
     */
    public static <X extends RuntimeException> void isNull(final Object obj, final Supplier<X> exceptionSupplier) {
        isTrue(obj == null, exceptionSupplier);
    }

    // ==================== nonNull ====================

    /**
     * 断言对象非null，否则抛出 BizException.
     *
     * @param obj   待校验对象
     * @param error 错误枚举
     */
    public static void nonNull(final Object obj, final BaseError error) {
        isTrue(obj != null, error);
    }

    /**
     * 断言对象非null，否则抛出 BizException（自定义消息）.
     *
     * @param obj     待校验对象
     * @param error   错误枚举
     * @param message 自定义错误消息
     */
    public static void nonNull(final Object obj, final BaseError error, final String message) {
        isTrue(obj != null, error, message);
    }

    /**
     * 断言对象非null，否则抛出自定义异常.
     *
     * @param obj               待校验对象
     * @param exceptionSupplier 异常提供者
     * @param <X>               异常类型
     */
    public static <X extends RuntimeException> void nonNull(final Object obj, final Supplier<X> exceptionSupplier) {
        isTrue(obj != null, exceptionSupplier);
    }

    // ==================== notEmpty (String) ====================

    /**
     * 断言字符串非空（非null且非空白），否则抛出 BizException.
     *
     * @param str   待校验字符串
     * @param error 错误枚举
     */
    public static void notEmpty(final String str, final BaseError error) {
        isTrue(str != null && !str.trim().isEmpty(), error);
    }

    /**
     * 断言字符串非空，否则抛出 BizException（自定义消息）.
     *
     * @param str     待校验字符串
     * @param error   错误枚举
     * @param message 自定义错误消息
     */
    public static void notEmpty(final String str, final BaseError error, final String message) {
        isTrue(str != null && !str.trim().isEmpty(), error, message);
    }

    /**
     * 断言字符串非空，否则抛出自定义异常.
     *
     * @param str               待校验字符串
     * @param exceptionSupplier 异常提供者
     * @param <X>               异常类型
     */
    public static <X extends RuntimeException> void notEmpty(final String str, final Supplier<X> exceptionSupplier) {
        isTrue(str != null && !str.trim().isEmpty(), exceptionSupplier);
    }

    // ==================== notEmpty (Collection) ====================

    /**
     * 断言集合非空（非null且有元素），否则抛出 BizException.
     *
     * @param collection 待校验集合
     * @param error      错误枚举
     */
    public static void notEmpty(final Collection<?> collection, final BaseError error) {
        isTrue(collection != null && !collection.isEmpty(), error);
    }

    /**
     * 断言集合非空，否则抛出 BizException（自定义消息）.
     *
     * @param collection 待校验集合
     * @param error      错误枚举
     * @param message    自定义错误消息
     */
    public static void notEmpty(final Collection<?> collection, final BaseError error, final String message) {
        isTrue(collection != null && !collection.isEmpty(), error, message);
    }

    /**
     * 断言集合非空，否则抛出自定义异常.
     *
     * @param collection        待校验集合
     * @param exceptionSupplier 异常提供者
     * @param <X>               异常类型
     */
    public static <X extends RuntimeException> void notEmpty(final Collection<?> collection, final Supplier<X> exceptionSupplier) {
        isTrue(collection != null && !collection.isEmpty(), exceptionSupplier);
    }

    // ==================== equal ====================

    /**
     * 断言两个对象相等，否则抛出 BizException.
     *
     * @param expected 期望值
     * @param actual   实际值
     * @param error    错误枚举
     */
    public static void equal(final Object expected, final Object actual, final BaseError error) {
        isTrue(Objects.equals(expected, actual), error);
    }

    /**
     * 断言两个对象相等，否则抛出 BizException（自定义消息）.
     *
     * @param expected 期望值
     * @param actual   实际值
     * @param error    错误枚举
     * @param message  自定义错误消息
     */
    public static void equal(final Object expected, final Object actual, final BaseError error, final String message) {
        isTrue(Objects.equals(expected, actual), error, message);
    }

    /**
     * 断言两个对象相等，否则抛出自定义异常.
     *
     * @param expected          期望值
     * @param actual            实际值
     * @param exceptionSupplier 异常提供者
     * @param <X>               异常类型
     */
    public static <X extends RuntimeException> void equal(final Object expected, final Object actual, final Supplier<X> exceptionSupplier) {
        isTrue(Objects.equals(expected, actual), exceptionSupplier);
    }

    // ==================== notEqual ====================

    /**
     * 断言两个对象不相等，否则抛出 BizException.
     *
     * @param unexpected 不期望的值
     * @param actual     实际值
     * @param error      错误枚举
     */
    public static void notEqual(final Object unexpected, final Object actual, final BaseError error) {
        isTrue(!Objects.equals(unexpected, actual), error);
    }
}
