package com.example.service;

import com.example.entity.User;
import com.example.entity.UserQuery;
import com.example.service.impl.UserServiceImpl;
import com.example.util.DatabaseInit;
import com.github.pagehelper.PageInfo;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;

public class UserServiceTest {

    @BeforeClass
    public static void initDb() {
        DatabaseInit.init();
    }

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

    @Test
    public void selectByCondition_shouldFilterByUsername() {
        UserQuery query = new UserQuery();
        query.setUsername("zhang");
        List<User> users = userService.selectByCondition(query);
        assertFalse(users.isEmpty());
        assertTrue(users.stream().allMatch(u -> u.getUsername().contains("zhang")));
    }

    @Test
    public void selectByConditionOrdered_shouldRejectIllegalColumn() {
        UserQuery query = new UserQuery();
        query.setOrderBy("password; DROP TABLE user; --");
        try {
            userService.selectByConditionOrdered(query);
            fail("应拒绝非白名单排序列");
        } catch (IllegalArgumentException expected) {
            assertEquals("非法的排序列: password; DROP TABLE user; --", expected.getMessage());
        }
    }

    @Test
    public void updateSelective_shouldOnlyUpdateProvidedFields() {
        User target = userService.selectById(1);
        target.setEmail("updated@test.com");
        target.setPassword(null);
        userService.updateSelective(target);

        User reloaded = userService.selectById(1);
        assertEquals("updated@test.com", reloaded.getEmail());
        assertEquals("123456", reloaded.getPassword());
    }

    @Test
    public void selectPageManually_shouldRespectOffsetAndLimit() {
        List<User> page = userService.selectPageManually(1, 1);
        assertEquals(1, page.size());
        assertEquals("lisi", page.get(0).getUsername());
    }

    @Test
    public void selectPageByHelper_shouldReturnPageInfo() {
        PageInfo<User> page = userService.selectPageByHelper(1, 1);
        assertEquals(1, page.getList().size());
        assertEquals(2, page.getTotal());
        assertEquals("zhangsan", page.getList().get(0).getUsername());
    }
}
