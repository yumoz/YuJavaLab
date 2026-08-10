package com.example.service;

import com.example.entity.Account;
import com.example.mapper.AccountMapper;
import com.example.util.SqlSessionFactoryUtil;
import org.apache.ibatis.session.SqlSession;

import static org.junit.Assert.assertEquals;

public final class AccountServiceTestHelper {

    private AccountServiceTestHelper() { }

    public static void assertBalance(int accountId, double expected) {
        try (SqlSession session = SqlSessionFactoryUtil.getSqlSessionFactory().openSession(true)) {
            AccountMapper mapper = session.getMapper(AccountMapper.class);
            Account account = mapper.selectById(accountId);
            assertEquals(expected, account.getBalance(), 0.001);
        }
    }
}
