package com.example.mapper;

import com.example.entity.Order;
import com.example.util.DatabaseInit;
import com.example.util.SqlSessionFactoryUtil;
import org.apache.ibatis.session.SqlSession;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class OrderMapperTest {

    private SqlSession session;
    private OrderMapper orderMapper;

    @BeforeClass
    public static void initDb() {
        DatabaseInit.init();
    }

    @Before
    public void setUp() {
        session = SqlSessionFactoryUtil.getSqlSessionFactory().openSession(true);
        orderMapper = session.getMapper(OrderMapper.class);
    }

    @After
    public void tearDown() {
        if (session != null) {
            session.close();
        }
    }

    @Test
    public void selectByUserId_shouldReturnOrders() {
        List<Order> orders = orderMapper.selectByUserId(1);
        assertNotNull(orders);
        assertEquals(2, orders.size());
    }

    @Test
    public void selectById_shouldReturnOrder() {
        Order order = orderMapper.selectById(1);
        assertNotNull(order);
        assertEquals("A1001", order.getOrderNo());
    }

    @Test
    public void insertOrder_shouldBackfillId() {
        Order order = new Order(2, "T1001", 12.5);
        try {
            int rows = orderMapper.insertOrder(order);
            assertEquals(1, rows);
            assertNotNull(order.getId());
        } finally {
            if (order.getId() != null) {
                session.delete("com.example.mapper.OrderMapper.deleteById", order.getId());
            }
        }
    }
}
