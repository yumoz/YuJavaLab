package com.example.mapper;

import com.example.entity.Order;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface OrderMapper {
    List<Order> selectByUserId(@Param("userId") Integer userId);

    Order selectById(@Param("id") Integer id);

    int insertOrder(Order order);
}
