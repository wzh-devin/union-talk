package com.devin.uniontalk.base.cursor.model;

import java.math.BigInteger;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 2026/05/20 13:05.
 *
 * <p>
 * 游标分页响应结果
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CursorPageResult<T, C> {

    /**
     * 当前页数据列表.
     */
    @Builder.Default
    private List<T> list = List.of();

    /**
     * 是否存在下一页.
     */
    @Builder.Default
    private Boolean hasNext = Boolean.FALSE;

    /**
     * 下一页游标排序字段值.
     */
    private C nextCursorValue;

    /**
     * 下一页游标主键id.
     */
    private BigInteger nextCursorId;
}
