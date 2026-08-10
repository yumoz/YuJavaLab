package com.example.service.impl;

import com.example.entity.Order;
import com.example.mapper.OrderMapper;
import com.example.service.OrderService;
import com.example.util.SqlSessionFactoryUtil;
import org.apache.ibatis.session.SqlSession;

import java.util.List;

public class OrderServiceImpl implements OrderService {

    @Override
    public List<Order> selectByUserId(Integer userId) {
        try (SqlSession session = SqlSessionFactoryUtil.getSqlSessionFactory().openSession(true)) {
            OrderMapper mapper = session.getMapper(OrderMapper.class);
            return mapper.selectByUserId(userId);
        }
    }

    @Override
    public Order selectById(Integer id) {
        try (SqlSession session = SqlSessionFactoryUtil.getSqlSessionFactory().openSession(true)) {
            OrderMapper mapper = session.getMapper(OrderMapper.class);
            return mapper.selectById(id);
        }
    }
}
