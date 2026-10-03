package com.devin.uniontalk.datasource.handler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;
import org.postgresql.util.PGobject;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 2026/5/16 23:30.
 *
 * <p>
 * Postgresql INET 类型转换器
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@MappedTypes(String.class)
public class InetTypeHandler extends BaseTypeHandler<String> {

    @Override
    public void setNonNullParameter(
            final PreparedStatement ps,
            final int i,
            final String parameter,
            final JdbcType jdbcType
    ) throws SQLException {
        PGobject pgObject = new PGobject();
        pgObject.setType("inet");
        pgObject.setValue(parameter);
        ps.setObject(i, pgObject);
    }

    @Override
    public String getNullableResult(final ResultSet rs, final String columnName) throws SQLException {
        return rs.getString(columnName);
    }

    @Override
    public String getNullableResult(final ResultSet rs, final int columnIndex) throws SQLException {
        return rs.getString(columnIndex);
    }

    @Override
    public String getNullableResult(final CallableStatement cs, final int columnIndex) throws SQLException {
        return cs.getString(columnIndex);
    }
}
