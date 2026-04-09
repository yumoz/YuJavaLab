package com.example.mapper;

import com.example.entity.User;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface UserMapper {
    // 根据 ID 查询用户
    User selectById(@Param("id") Integer id);

    // 查询所有用户
    List<User> selectAll();

    // 新增用户
    int insertUser(User user);
}