package com.devin.uniontalk.base.cursor.utils;

import com.devin.uniontalk.base.cursor.enums.CursorOrderEnum;
import com.devin.uniontalk.base.cursor.model.CursorPageResult;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * 2026/05/20 13:05.
 *
 * <p>
 * 游标分页工具类
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
public final class CursorPageUtils {

    /**
     * 默认分页大小.
     */
    public static final int DEFAULT_PAGE_SIZE = 20;

    /**
     * 最大分页大小.
     */
    public static final int MAX_PAGE_SIZE = 100;

    /**
     * 下一页探测数量.
     */
    private static final int NEXT_PAGE_PROBE_SIZE = 1;

    private CursorPageUtils() {
    }

    /**
     * 归一化分页大小.
     *
     * @param pageSize 分页大小
     * @return 归一化后的分页大小
     */
    public static int normalizePageSize(final Integer pageSize) {
        if (Objects.isNull(pageSize) || pageSize <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    /**
     * 获取带下一页探测的查询数量.
     *
     * @param pageSize 分页大小
     * @return 查询数量
     */
    public static int limitSize(final Integer pageSize) {
        return normalizePageSize(pageSize) + NEXT_PAGE_PROBE_SIZE;
    }

    /**
     * 归一化排序方向.
     *
     * @param order 排序方向
     * @return 排序方向
     */
    public static CursorOrderEnum normalizeOrder(final CursorOrderEnum order) {
        return Objects.nonNull(order) ? order : CursorOrderEnum.DESC;
    }

    /**
     * 构建游标分页响应结果.
     *
     * @param sourceList        原始查询列表
     * @param pageSize          分页大小
     * @param cursorValueGetter 游标排序字段获取函数
     * @param cursorIdGetter    游标主键id获取函数
     * @param <T>               数据类型
     * @param <C>               游标排序字段类型
     * @return 游标分页响应结果
     */
    public static <T, C> CursorPageResult<T, C> buildPageResult(
            final List<T> sourceList,
            final Integer pageSize,
            final Function<T, C> cursorValueGetter,
            final Function<T, BigInteger> cursorIdGetter
    ) {
        int normalizedPageSize = normalizePageSize(pageSize);
        if (Objects.isNull(sourceList) || sourceList.isEmpty()) {
            return CursorPageResult.<T, C>builder()
                    .list(List.of())
                    .hasNext(Boolean.FALSE)
                    .build();
        }

        boolean hasNext = sourceList.size() > normalizedPageSize;
        List<T> currentList = new ArrayList<>(
                sourceList.subList(0, Math.min(sourceList.size(), normalizedPageSize))
        );
        T cursorItem = currentList.getLast();
        return CursorPageResult.<T, C>builder()
                .list(currentList)
                .hasNext(hasNext)
                .nextCursorValue(hasNext ? cursorValueGetter.apply(cursorItem) : null)
                .nextCursorId(hasNext && Objects.nonNull(cursorIdGetter) ? cursorIdGetter.apply(cursorItem) : null)
                .build();
    }
}
