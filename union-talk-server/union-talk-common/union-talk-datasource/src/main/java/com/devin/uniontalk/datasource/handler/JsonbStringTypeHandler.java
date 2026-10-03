package com.devin.uniontalk.datasource.handler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.postgresql.util.PGobject;

/**
 * 2026/07/29.
 *
 * <p>
 * PostgreSQL JSONB 字符串类型转换器
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
public class JsonbStringTypeHandler extends BaseTypeHandler<String> {

    /**
     * 设置 JSONB 非空参数.
     *
     * @param ps        预编译语句
     * @param i         参数位置
     * @param parameter JSON字符串
     * @param jdbcType  JDBC类型
     * @throws SQLException SQL异常
     */
    @Override
    public void setNonNullParameter(
            final PreparedStatement ps,
            final int i,
            final String parameter,
            final JdbcType jdbcType
    ) throws SQLException {
        PGobject pgObject = new PGobject();
        pgObject.setType("jsonb");
        pgObject.setValue(parameter);
        ps.setObject(i, pgObject);
    }

    /**
     * 按字段名获取 JSONB 字符串.
     *
     * @param rs         查询结果集
     * @param columnName 字段名称
     * @return JSON字符串
     * @throws SQLException SQL异常
     */
    @Override
    public String getNullableResult(final ResultSet rs, final String columnName) throws SQLException {
        return rs.getString(columnName);
    }

    /**
     * 按字段位置获取 JSONB 字符串.
     *
     * @param rs          查询结果集
     * @param columnIndex 字段位置
     * @return JSON字符串
     * @throws SQLException SQL异常
     */
    @Override
    public String getNullableResult(final ResultSet rs, final int columnIndex) throws SQLException {
        return rs.getString(columnIndex);
    }

    /**
     * 从存储过程结果获取 JSONB 字符串.
     *
     * @param cs          存储过程调用语句
     * @param columnIndex 字段位置
     * @return JSON字符串
     * @throws SQLException SQL异常
     */
    @Override
    public String getNullableResult(final CallableStatement cs, final int columnIndex) throws SQLException {
        return cs.getString(columnIndex);
    }
}
