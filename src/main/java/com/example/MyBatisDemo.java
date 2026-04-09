package com.example;

import com.example.entity.User;
import com.example.mapper.UserMapper;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class MyBatisDemo {
    public static void main(String[] args) throws IOException {
        // 1. 加载 MyBatis 全局配置文件
        String resource = "mybatis-config.xml";
        InputStream inputStream = Resources.getResourceAsStream(resource);

        // 2. 创建 SqlSessionFactory（核心：单例，全局唯一）
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);

        // 3. 打开 SqlSession（注意：SqlSession 线程不安全，每次使用后需关闭）
        // openSession(true) 表示自动提交事务，默认 false 需手动 commit
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            // 4. 获取 Mapper 代理对象
            UserMapper userMapper = session.getMapper(UserMapper.class);

            // 测试 1：根据 ID 查询用户
            User user = userMapper.selectById(1);
            System.out.println("根据 ID 查询用户：" + user);

            // 测试 2：查询所有用户
            List<User> userList = userMapper.selectAll();
            System.out.println("\n查询所有用户：");
            userList.forEach(System.out::println);

            // 测试 3：新增用户
            //User newUser = new User("wangx", "987654", "wangx@example.com");
            //int rows = userMapper.insertUser(newUser);
            //System.out.println("\n新增用户影响行数：" + rows);
            //System.out.println("新增用户的自增 ID：" + newUser.getId()); // 自增主键已回填
        }
    }
}