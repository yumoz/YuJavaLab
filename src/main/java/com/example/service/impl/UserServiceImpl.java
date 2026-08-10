package com.example.service.impl;

import com.example.entity.User;
import com.example.entity.UserQuery;
import com.example.mapper.UserMapper;
import com.example.service.UserService;
import com.example.util.SqlSessionFactoryUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.apache.ibatis.session.SqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private static final List<String> ORDER_BY_WHITELIST = List.of("id", "username", "email", "create_time");

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
    public int insertBatch(List<User> list) {
        log.debug("Batch inserting {} users", list.size());
        try (SqlSession session = SqlSessionFactoryUtil.getSqlSessionFactory().openSession(true)) {
            UserMapper mapper = session.getMapper(UserMapper.class);
            int rows = mapper.insertBatch(list);
            log.info("Batch inserted {} rows", rows);
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

    @Override
    public List<User> selectByCondition(UserQuery query) {
        try (SqlSession session = SqlSessionFactoryUtil.getSqlSessionFactory().openSession(true)) {
            UserMapper mapper = session.getMapper(UserMapper.class);
            return mapper.selectByCondition(query);
        }
    }

    @Override
    public List<User> selectByConditionOrdered(UserQuery query) {
        String orderBy = query.getOrderBy();
        if (orderBy == null || !ORDER_BY_WHITELIST.contains(orderBy)) {
            throw new IllegalArgumentException("非法的排序列: " + orderBy);
        }
        try (SqlSession session = SqlSessionFactoryUtil.getSqlSessionFactory().openSession(true)) {
            UserMapper mapper = session.getMapper(UserMapper.class);
            return mapper.selectByConditionOrdered(query);
        }
    }

    @Override
    public int updateSelective(User user) {
        try (SqlSession session = SqlSessionFactoryUtil.getSqlSessionFactory().openSession(true)) {
            UserMapper mapper = session.getMapper(UserMapper.class);
            return mapper.updateSelective(user);
        }
    }

    @Override
    public User selectUserWithOrders(Integer id) {
        try (SqlSession session = SqlSessionFactoryUtil.getSqlSessionFactory().openSession(true)) {
            UserMapper mapper = session.getMapper(UserMapper.class);
            return mapper.selectUserWithOrders(id);
        }
    }

    @Override
    public List<User> selectPageManually(int offset, int limit) {
        try (SqlSession session = SqlSessionFactoryUtil.getSqlSessionFactory().openSession(true)) {
            UserMapper mapper = session.getMapper(UserMapper.class);
            return mapper.selectPage(offset, limit);
        }
    }

    @Override
    public PageInfo<User> selectPageByHelper(int pageNum, int pageSize) {
        try (SqlSession session = SqlSessionFactoryUtil.getSqlSessionFactory().openSession(true)) {
            PageHelper.startPage(pageNum, pageSize);
            UserMapper mapper = session.getMapper(UserMapper.class);
            List<User> users = mapper.selectAll();
            return new PageInfo<>(users);
        }
    }
}
