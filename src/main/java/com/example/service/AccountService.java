package com.example.service;

import java.math.BigDecimal;

public interface AccountService {

    /**
     * 转账：fromId 账户扣减 amount，toId 账户增加 amount。
     * 金额必须为正数，否则抛 {@link com.example.exception.BizException}；
     * 任何一步失败（余额不足、账户不存在）整个事务回滚。
     */
    void transfer(Integer fromId, Integer toId, BigDecimal amount);
}