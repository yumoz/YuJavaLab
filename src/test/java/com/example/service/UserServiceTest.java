package com.example.service;

import com.example.entity.User;
import com.example.service.impl.UserServiceImpl;
import org.junit.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;

public class UserServiceTest {

    private final UserService userService = new UserServiceImpl();

    @Test
    public void selectById_shouldReturnUser() {
        User user = userService.selectById(1);
        assertNotNull(user);
        assertEquals("zhangsan", user.getUsername());
    }

    @Test
    public void selectAll_shouldReturnNonEmptyList() {
        List<User> users = userService.selectAll();
        assertNotNull(users);
        assertFalse(users.isEmpty());
    }

    @Test
    public void insertUser_shouldPersistAndBackfillId() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        User user = new User("svc_" + suffix, "svc_pass", suffix + "@svc.com");
        try {
            int rows = userService.insertUser(user);
            assertEquals(1, rows);
            assertNotNull(user.getId());
        } finally {
            if (user.getId() != null) {
                userService.deleteById(user.getId());
            }
        }
    }
}
