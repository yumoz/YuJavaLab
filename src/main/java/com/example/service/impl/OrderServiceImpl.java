package com.example.service.impl;

import com.example.entity.Order;
import com.example.mapper.OrderMapper;
import com.example.service.OrderService;
import com.example.util.SqlSessionTemplate;

import java.util.List;

public class OrderServiceImpl implements OrderService {

    @Override
    public List<Order> selectByUserId(Integer userId) {
        return SqlSessionTemplate.execute(session ->
                session.getMapper(OrderMapper.class).selectByUserId(userId));
    }

    @Override
    public Order selectById(Integer id) {
        return SqlSessionTemplate.execute(session ->
                session.getMapper(OrderMapper.class).selectById(id));
    }
}