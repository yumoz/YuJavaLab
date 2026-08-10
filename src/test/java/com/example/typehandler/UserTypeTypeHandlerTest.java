package com.example.typehandler;

import com.example.entity.UserType;
import org.junit.Before;
import org.junit.Test;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UserTypeTypeHandlerTest {

    private UserTypeTypeHandler handler;

    @Before
    public void setUp() {
        handler = new UserTypeTypeHandler();
    }

    @Test
    public void setNonNullParameter_writesCode() throws Exception {
        PreparedStatement ps = mock(PreparedStatement.class);
        handler.setNonNullParameter(ps, 2, UserType.VIP, null);
        verify(ps).setInt(2, 2);
    }

    @Test
    public void getNullableResult_readsByColumnName() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getInt("user_type")).thenReturn(1);
        assertEquals(UserType.ADMIN, handler.getNullableResult(rs, "user_type"));
    }

    @Test
    public void getNullableResult_readsByIndex() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getInt(3)).thenReturn(0);
        assertEquals(UserType.NORMAL, handler.getNullableResult(rs, 3));
    }
}
