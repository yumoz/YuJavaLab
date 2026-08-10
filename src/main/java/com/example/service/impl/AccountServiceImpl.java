package com.example.service.impl;

import com.example.entity.Account;
import com.example.mapper.AccountMapper;
import com.example.service.AccountService;
import com.example.util.SqlSessionFactoryUtil;
import org.apache.ibatis.session.SqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AccountServiceImpl implements AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountServiceImpl.class);

    @Override
    public void transfer(Integer fromId, Integer toId, double amount) {
        try (SqlSession session = SqlSessionFactoryUtil.getSqlSessionFactory().openSession(false)) {
            AccountMapper mapper = session.getMapper(AccountMapper.class);
            try {
                Account from = mapper.selectById(fromId);
                Account to = mapper.selectById(toId);
                if (from == null || to == null) {
                    throw new IllegalArgumentException("账户不存在");
                }
                if (from.getBalance() < amount) {
                    throw new IllegalStateException("余额不足");
                }
                mapper.updateBalance(fromId, from.getBalance() - amount);
                mapper.updateBalance(toId, to.getBalance() + amount);
                log.info("transfer {} from {} to {}", amount, fromId, toId);
                session.commit();
            } catch (RuntimeException e) {
                session.rollback();
                throw e;
            }
        }
    }
}
