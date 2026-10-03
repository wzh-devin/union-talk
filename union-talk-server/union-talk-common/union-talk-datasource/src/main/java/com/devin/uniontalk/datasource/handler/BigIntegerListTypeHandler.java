package com.devin.uniontalk.datasource.handler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import java.math.BigInteger;
import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * 2026/05/20 16:00.
 *
 * <p>
 * PostgreSQL BIGINT[] 类型转换器
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
public class BigIntegerListTypeHandler extends BaseTypeHandler<List<BigInteger>> {

    @Override
    public void setNonNullParameter(
            final PreparedStatement ps,
            final int i,
            final List<BigInteger> parameter,
            final JdbcType jdbcType
    ) throws SQLException {
        Long[] valueArray = parameter.stream()
                .filter(Objects::nonNull)
                .map(BigInteger::longValue)
                .toArray(Long[]::new);
        Array sqlArray = ps.getConnection().createArrayOf("BIGINT", valueArray);
        ps.setArray(i, sqlArray);
    }

    @Override
    public List<BigInteger> getNullableResult(final ResultSet rs, final String columnName) throws SQLException {
        return toBigIntegerList(rs.getArray(columnName));
    }

    @Override
    public List<BigInteger> getNullableResult(final ResultSet rs, final int columnIndex) throws SQLException {
        return toBigIntegerList(rs.getArray(columnIndex));
    }

    @Override
    public List<BigInteger> getNullableResult(final CallableStatement cs, final int columnIndex) throws SQLException {
        return toBigIntegerList(cs.getArray(columnIndex));
    }

    /**
     * 转换 PostgreSQL 数组为 BigInteger 列表.
     *
     * @param array PostgreSQL 数组
     * @return BigInteger 列表
     * @throws SQLException SQL异常
     */
    private List<BigInteger> toBigIntegerList(final Array array) throws SQLException {
        if (Objects.isNull(array)) {
            return List.of();
        }
        Object[] valueArray = (Object[]) array.getArray();
        array.free();
        return Arrays.stream(valueArray)
                .filter(Objects::nonNull)
                .map(value -> new BigInteger(value.toString()))
                .toList();
    }
}
