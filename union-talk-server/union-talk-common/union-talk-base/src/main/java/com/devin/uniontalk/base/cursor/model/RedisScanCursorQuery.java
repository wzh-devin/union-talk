package com.devin.uniontalk.base.cursor.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 2026/05/20 13:05.
 *
 * <p>
 * Redis Scan 游标查询参数
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
public class RedisScanCursorQuery {

    /**
     * Redis 原生游标.
     */
    private String cursor;

    /**
     * 匹配表达式.
     */
    private String pattern;

    /**
     * 分页大小.
     */
    private Integer pageSize;
}
