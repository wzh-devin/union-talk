package com.devin.uniontalk.base.cursor.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 2026/05/20 13:05.
 *
 * <p>
 * Redis Scan 游标响应结果
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
public class RedisScanCursorResult<T> {

    /**
     * 当前页数据列表.
     */
    @Builder.Default
    private List<T> list = List.of();

    /**
     * 下一次扫描游标.
     */
    private String nextCursor;

    /**
     * 是否已经扫描完成.
     */
    @Builder.Default
    private Boolean finished = Boolean.FALSE;
}
