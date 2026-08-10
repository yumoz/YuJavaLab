package com.example.typehandler;

import com.example.entity.UserType;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserTypeTypeHandler extends BaseTypeHandler<UserType> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, UserType parameter, JdbcType jdbcType) throws SQLException {
        ps.setInt(i, parameter.getCode());
    }

    @Override
    public UserType getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return UserType.fromCode(rs.getInt(columnName));
    }

    @Override
    public UserType getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return UserType.fromCode(rs.getInt(columnIndex));
    }

    @Override
    public UserType getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return UserType.fromCode(cs.getInt(columnIndex));
    }
}
