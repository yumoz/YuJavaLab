package com.example.mapper;

import com.example.entity.User;
import com.example.entity.UserQuery;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface UserMapper {
    // 根据 ID 查询用户
    User selectById(@Param("id") Integer id);

    // 查询所有用户
    List<User> selectAll();

    // 新增用户
    int insertUser(User user);

    // 批量新增用户
    int insertBatch(@Param("list") List<User> list);

    // 根据 ID 删除用户（主要用于测试清理）
    int deleteById(@Param("id") Integer id);

    // 动态条件查询（<where>/<if>/<choose> 演示）
    List<User> selectByCondition(UserQuery query);

    // 动态排序列（${} 演示，列名必须由 service 白名单校验）
    List<User> selectByConditionOrdered(UserQuery query);

    // 动态更新非空字段（<set> 演示）
    int updateSelective(User user);

    // 一对多联表查询（<collection> 演示）
    User selectUserWithOrders(@Param("id") Integer id);

    // 手写分页（LIMIT/OFFSET）
    List<User> selectPage(@Param("offset") int offset, @Param("limit") int limit);
}