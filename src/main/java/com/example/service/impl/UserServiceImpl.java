package com.example.service.impl;

import com.example.entity.User;
import com.example.entity.UserQuery;
import com.example.mapper.UserMapper;
import com.example.service.UserService;
import com.example.util.SqlSessionTemplate;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private static final List<String> ORDER_BY_WHITELIST = List.of("id", "username", "email", "create_time");

    @Override
    public User selectById(Integer id) {
        log.debug("Querying user by id: {}", id);
        return SqlSessionTemplate.execute(session ->
                session.getMapper(UserMapper.class).selectById(id));
    }

    @Override
    public List<User> selectAll() {
        log.debug("Querying all users");
        return SqlSessionTemplate.execute(session ->
                session.getMapper(UserMapper.class).selectAll());
    }

    @Override
    public int insertUser(User user) {
        log.debug("Inserting user: {}", user.getUsername());
        int rows = SqlSessionTemplate.execute(session ->
                session.getMapper(UserMapper.class).insertUser(user));
        log.info("Inserted user id: {}", user.getId());
        return rows;
    }

    @Override
    public int insertBatch(List<User> list) {
        log.debug("Batch inserting {} users", list.size());
        int rows = SqlSessionTemplate.execute(session -> {
            UserMapper mapper = session.getMapper(UserMapper.class);
            return mapper.insertBatch(list);
        });
        log.info("Batch inserted {} rows", rows);
        return rows;
    }

    @Override
    public void deleteById(Integer id) {
        log.debug("Deleting user by id: {}", id);
        SqlSessionTemplate.execute(session -> {
            session.getMapper(UserMapper.class).deleteById(id);
            return null;
        });
    }

    @Override
    public List<User> selectByCondition(UserQuery query) {
        return SqlSessionTemplate.execute(session ->
                session.getMapper(UserMapper.class).selectByCondition(query));
    }

    @Override
    public List<User> selectByConditionOrdered(UserQuery query) {
        String orderBy = query.getOrderBy();
        if (orderBy == null || !ORDER_BY_WHITELIST.contains(orderBy)) {
            throw new IllegalArgumentException("非法的排序列: " + orderBy);
        }
        return SqlSessionTemplate.execute(session ->
                session.getMapper(UserMapper.class).selectByConditionOrdered(query));
    }

    @Override
    public int updateSelective(User user) {
        return SqlSessionTemplate.execute(session ->
                session.getMapper(UserMapper.class).updateSelective(user));
    }

    @Override
    public User selectUserWithOrders(Integer id) {
        return SqlSessionTemplate.execute(session ->
                session.getMapper(UserMapper.class).selectUserWithOrders(id));
    }

    @Override
    public List<User> selectPageManually(int offset, int limit) {
        return SqlSessionTemplate.execute(session ->
                session.getMapper(UserMapper.class).selectPage(offset, limit));
    }

    @Override
    public PageInfo<User> selectPageByHelper(int pageNum, int pageSize) {
        return SqlSessionTemplate.execute(session -> {
            PageHelper.startPage(pageNum, pageSize);
            UserMapper mapper = session.getMapper(UserMapper.class);
            return new PageInfo<>(mapper.selectAll());
        });
    }
}