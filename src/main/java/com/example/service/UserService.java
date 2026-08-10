package com.example.service;

import com.example.entity.User;
import com.example.entity.UserQuery;
import com.github.pagehelper.PageInfo;
import java.util.List;

public interface UserService {

    User selectById(Integer id);

    List<User> selectAll();

    int insertUser(User user);

    int insertBatch(List<User> list);

    void deleteById(Integer id);

    List<User> selectByCondition(UserQuery query);

    List<User> selectByConditionOrdered(UserQuery query);

    int updateSelective(User user);

    User selectUserWithOrders(Integer id);

    List<User> selectPageManually(int offset, int limit);

    PageInfo<User> selectPageByHelper(int pageNum, int pageSize);
}
