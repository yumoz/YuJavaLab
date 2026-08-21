package com.example.service;

import com.example.entity.Account;
import com.example.mapper.AccountMapper;
import com.example.util.SqlSessionFactoryUtil;
import org.apache.ibatis.session.SqlSession;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;

public final class AccountServiceTestHelper {

    private AccountServiceTestHelper() { }

    /**
     * 用 {@code compareTo} 比较余额而非 {@code equals}，
     * 避免 SQLite REAL 读回 BigDecimal 时 scale 不一致导致的误判（教学最佳实践）。
     */
    public static void assertBalance(int accountId, BigDecimal expected) {
        try (SqlSession session = SqlSessionFactoryUtil.getSqlSessionFactory().openSession(true)) {
            AccountMapper mapper = session.getMapper(AccountMapper.class);
            Account account = mapper.selectById(accountId);
            assertEquals(0, expected.compareTo(account.getBalance()));
        }
    }
}