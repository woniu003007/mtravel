package com.mtravel.platform.purchase.resource.material.typehandler;

import org.apache.ibatis.type.BaseTypeHandler;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import org.apache.ibatis.type.JdbcType;

/** PostgreSQL JSONB 字符串类型处理器，保持业务层已经序列化好的 JSON 数组内容不被二次编码。 */
public class PostgreSqlJsonbStringTypeHandler extends BaseTypeHandler<String> {

    @Override
    public void setNonNullParameter(PreparedStatement statement, int index, String value, JdbcType jdbcType)
            throws SQLException {
        // Types.OTHER 让 PostgreSQL 按 jsonb 参数接收，而不是把 JSON 文本当成 varchar。
        statement.setObject(index, value, Types.OTHER);
    }

    @Override
    public String getNullableResult(ResultSet resultSet, String columnName) throws SQLException {
        return resultSet.getString(columnName);
    }

    @Override
    public String getNullableResult(ResultSet resultSet, int columnIndex) throws SQLException {
        return resultSet.getString(columnIndex);
    }

    @Override
    public String getNullableResult(CallableStatement statement, int columnIndex) throws SQLException {
        return statement.getString(columnIndex);
    }

}
