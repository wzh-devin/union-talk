package com.devin.uniontalk.datasource.utils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.service.IService;
import com.devin.uniontalk.base.cursor.enums.CursorOrderEnum;
import com.devin.uniontalk.base.cursor.model.CursorPageQuery;
import com.devin.uniontalk.base.cursor.model.CursorPageResult;
import com.devin.uniontalk.base.cursor.utils.CursorPageUtils;
import java.math.BigInteger;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * 2026/05/20 13:05.
 *
 * <p>
 * Postgresql 游标分页工具类
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
public final class PgCursorPageUtils {

    /**
     * Postgresql Limit SQL 前缀.
     */
    private static final String LIMIT_SQL_PREFIX = "LIMIT ";

    private PgCursorPageUtils() {
    }

    /**
     * 使用 MyBatis-Plus 执行游标分页查询.
     *
     * @param service           MyBatis-Plus Service
     * @param queryWrapper      查询条件包装器
     * @param cursorQuery       游标分页查询参数
     * @param sortColumn        排序字段列
     * @param idColumn          主键id列
     * @param sortValueGetter   排序字段获取函数
     * @param idGetter          主键id获取函数
     * @param <T>               实体类型
     * @param <C>               排序字段类型
     * @return 游标分页响应结果
     */
    public static <T, C extends Comparable<? super C>> CursorPageResult<T, C> page(
            final IService<T> service,
            final LambdaQueryWrapper<T> queryWrapper,
            final CursorPageQuery<C> cursorQuery,
            final SFunction<T, C> sortColumn,
            final SFunction<T, BigInteger> idColumn,
            final Function<T, C> sortValueGetter,
            final Function<T, BigInteger> idGetter
    ) {
        Objects.requireNonNull(service, "service不能为空");
        Objects.requireNonNull(sortColumn, "排序字段列不能为空");
        Objects.requireNonNull(idColumn, "主键id列不能为空");
        Objects.requireNonNull(sortValueGetter, "排序字段获取函数不能为空");
        Objects.requireNonNull(idGetter, "主键id获取函数不能为空");

        CursorPageQuery<C> safeCursorQuery = safeCursorQuery(cursorQuery);
        LambdaQueryWrapper<T> targetWrapper = safeQueryWrapper(queryWrapper);
        CursorOrderEnum order = CursorPageUtils.normalizeOrder(safeCursorQuery.getOrder());
        appendCursorCondition(targetWrapper, safeCursorQuery, sortColumn, idColumn, order);
        appendOrder(targetWrapper, sortColumn, idColumn, order);
        targetWrapper.last(LIMIT_SQL_PREFIX + CursorPageUtils.limitSize(safeCursorQuery.getPageSize()));

        List<T> sourceList = service.list(targetWrapper);
        return CursorPageUtils.buildPageResult(
                sourceList,
                safeCursorQuery.getPageSize(),
                sortValueGetter,
                idGetter
        );
    }

    /**
     * 获取安全游标查询参数.
     *
     * @param cursorQuery 游标查询参数
     * @param <C>         排序字段类型
     * @return 游标查询参数
     */
    private static <C> CursorPageQuery<C> safeCursorQuery(final CursorPageQuery<C> cursorQuery) {
        return Objects.nonNull(cursorQuery) ? cursorQuery : new CursorPageQuery<>();
    }

    /**
     * 获取安全查询条件包装器.
     *
     * @param queryWrapper 查询条件包装器
     * @param <T>          实体类型
     * @return 查询条件包装器
     */
    private static <T> LambdaQueryWrapper<T> safeQueryWrapper(final LambdaQueryWrapper<T> queryWrapper) {
        return Objects.nonNull(queryWrapper) ? queryWrapper : new LambdaQueryWrapper<>();
    }

    /**
     * 追加游标查询条件.
     *
     * @param queryWrapper 查询条件包装器
     * @param cursorQuery  游标查询参数
     * @param sortColumn   排序字段列
     * @param idColumn     主键id列
     * @param order        排序方向
     * @param <T>          实体类型
     * @param <C>          排序字段类型
     */
    private static <T, C extends Comparable<? super C>> void appendCursorCondition(
            final LambdaQueryWrapper<T> queryWrapper,
            final CursorPageQuery<C> cursorQuery,
            final SFunction<T, C> sortColumn,
            final SFunction<T, BigInteger> idColumn,
            final CursorOrderEnum order
    ) {
        if (Objects.isNull(cursorQuery.getCursorValue()) || Objects.isNull(cursorQuery.getCursorId())) {
            return;
        }
        if (order.isAsc()) {
            queryWrapper.and(wrapper -> wrapper
                    .gt(sortColumn, cursorQuery.getCursorValue())
                    .or(orWrapper -> orWrapper
                            .eq(sortColumn, cursorQuery.getCursorValue())
                            .gt(idColumn, cursorQuery.getCursorId())));
            return;
        }
        queryWrapper.and(wrapper -> wrapper
                .lt(sortColumn, cursorQuery.getCursorValue())
                .or(orWrapper -> orWrapper
                        .eq(sortColumn, cursorQuery.getCursorValue())
                        .lt(idColumn, cursorQuery.getCursorId())));
    }

    /**
     * 追加排序条件.
     *
     * @param queryWrapper 查询条件包装器
     * @param sortColumn   排序字段列
     * @param idColumn     主键id列
     * @param order        排序方向
     * @param <T>          实体类型
     * @param <C>          排序字段类型
     */
    private static <T, C extends Comparable<? super C>> void appendOrder(
            final LambdaQueryWrapper<T> queryWrapper,
            final SFunction<T, C> sortColumn,
            final SFunction<T, BigInteger> idColumn,
            final CursorOrderEnum order
    ) {
        if (order.isAsc()) {
            queryWrapper.orderByAsc(sortColumn).orderByAsc(idColumn);
            return;
        }
        queryWrapper.orderByDesc(sortColumn).orderByDesc(idColumn);
    }
}
