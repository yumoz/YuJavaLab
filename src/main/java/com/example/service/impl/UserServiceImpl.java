package com.example.service.impl;

import com.example.entity.User;
import com.example.mapper.UserMapper;
import com.example.service.UserService;
import com.example.util.SqlSessionFactoryUtil;
import org.apache.ibatis.session.SqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    @Override
    public User selectById(Integer id) {
        log.debug("Querying user by id: {}", id);
        try (SqlSession session = SqlSessionFactoryUtil.getSqlSessionFactory().openSession(true)) {
            UserMapper mapper = session.getMapper(UserMapper.class);
            return mapper.selectById(id);
        }
    }

    @Override
    public List<User> selectAll() {
        log.debug("Querying all users");
        try (SqlSession session = SqlSessionFactoryUtil.getSqlSessionFactory().openSession(true)) {
            UserMapper mapper = session.getMapper(UserMapper.class);
            return mapper.selectAll();
        }
    }

    @Override
    public int insertUser(User user) {
        log.debug("Inserting user: {}", user.getUsername());
        try (SqlSession session = SqlSessionFactoryUtil.getSqlSessionFactory().openSession(true)) {
            UserMapper mapper = session.getMapper(UserMapper.class);
            int rows = mapper.insertUser(user);
            log.info("Inserted user id: {}", user.getId());
            return rows;
        }
    }

    @Override
    public void deleteById(Integer id) {
        log.debug("Deleting user by id: {}", id);
        try (SqlSession session = SqlSessionFactoryUtil.getSqlSessionFactory().openSession(true)) {
            UserMapper mapper = session.getMapper(UserMapper.class);
            mapper.deleteById(id);
        }
    }
}
