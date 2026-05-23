package com.example;

import com.example.entity.User;
import com.example.service.UserService;
import com.example.service.impl.UserServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class MyBatisDemo {

    private static final Logger log = LoggerFactory.getLogger(MyBatisDemo.class);

    public static void main(String[] args) {
        UserService userService = new UserServiceImpl();

        // 测试 1：根据 ID 查询用户
        User user = userService.selectById(1);
        log.info("根据 ID 查询用户：{}", user);

        // 测试 2：查询所有用户
        List<User> userList = userService.selectAll();
        log.info("查询所有用户：");
        userList.forEach(u -> log.info("  {}", u));

        // 测试 3：新增用户
        User newUser = new User("wangx", "987654", "wangx@example.com");
        int rows = userService.insertUser(newUser);
        log.info("新增用户影响行数：{}", rows);
        log.info("新增用户的自增 ID：{}", newUser.getId());
    }
}
