package com.devin.uniontalk.base.cursor.model;

import com.devin.uniontalk.base.cursor.enums.CursorOrderEnum;
import java.math.BigInteger;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 2026/05/20 13:05.
 *
 * <p>
 * 游标分页查询参数
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
public class CursorPageQuery<C> {

    /**
     * 分页大小.
     */
    private Integer pageSize;

    /**
     * 游标排序字段值.
     */
    private C cursorValue;

    /**
     * 游标主键id.
     */
    private BigInteger cursorId;

    /**
     * 排序方向.
     */
    @Builder.Default
    private CursorOrderEnum order = CursorOrderEnum.DESC;
}
