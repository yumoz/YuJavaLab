package com.example.service.impl;

import com.example.entity.Account;
import com.example.exception.BizException;
import com.example.mapper.AccountMapper;
import com.example.service.AccountService;
import com.example.util.SqlSessionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

public class AccountServiceImpl implements AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountServiceImpl.class);

    @Override
    public void transfer(Integer fromId, Integer toId, BigDecimal amount) {
        // 非法入参校验：负数金额会"反向造钱"，必须拒绝
        if (amount == null || amount.signum() <= 0) {
            throw new BizException("转账金额必须为正数");
        }
        SqlSessionTemplate.executeInTransaction(session -> {
            AccountMapper mapper = session.getMapper(AccountMapper.class);

            Account from = mapper.selectById(fromId);
            Account to = mapper.selectById(toId);
            if (from == null) {
                throw new BizException("转出账户不存在: " + fromId);
            }
            if (to == null) {
                throw new BizException("转入账户不存在: " + toId);
            }
            if (from.getBalance().compareTo(amount) < 0) {
                throw new BizException("余额不足");
            }

            // 原子扣减兜底：并发场景下若余额已被其它转账耗尽，本行影响行数为 0，避免超扣
            int rows = mapper.debit(fromId, amount);
            if (rows == 0) {
                throw new BizException("余额不足（并发扣减保护）");
            }
            mapper.credit(toId, amount);

            log.info("transfer {} from {} to {}", amount, fromId, toId);
        });
    }
}