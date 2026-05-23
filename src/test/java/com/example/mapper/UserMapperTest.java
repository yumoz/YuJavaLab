package com.example.mapper;

import com.example.entity.User;
import com.example.util.SqlSessionFactoryUtil;
import org.apache.ibatis.session.SqlSession;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;

public class UserMapperTest {

    private SqlSession session;
    private UserMapper userMapper;

    @Before
    public void setUp() {
        session = SqlSessionFactoryUtil.getSqlSessionFactory().openSession(true);
        userMapper = session.getMapper(UserMapper.class);
    }

    @After
    public void tearDown() {
        if (session != null) {
            session.close();
        }
    }

    @Test
    public void selectById_shouldReturnUserWhenIdExists() {
        User user = userMapper.selectById(1);
        assertNotNull(user);
        assertEquals("zhangsan", user.getUsername());
    }

    @Test
    public void selectById_shouldReturnNullWhenIdNotExists() {
        User user = userMapper.selectById(999);
        assertNull(user);
    }

    @Test
    public void selectAll_shouldReturnAllUsers() {
        List<User> users = userMapper.selectAll();
        assertNotNull(users);
        assertTrue(users.size() >= 2);
    }

    @Test
    public void insertUser_shouldBackfillId() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        User user = new User("testuser_" + suffix, "pass123", suffix + "@example.com");
        try {
            int rows = userMapper.insertUser(user);
            assertEquals(1, rows);
            assertNotNull(user.getId());
        } finally {
            if (user.getId() != null) {
                userMapper.deleteById(user.getId());
            }
        }
    }
}
