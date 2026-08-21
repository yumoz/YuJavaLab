package com.example.service;

import com.example.exception.BizException;
import com.example.service.impl.AccountServiceImpl;
import com.example.util.DatabaseInit;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * 转账事务测试。
 *
 * 改进点：每个用例在 {@link Before} 中重建数据库，保证用例间状态完全独立，
 * 不再依赖 {@code @FixMethodOrder} 的方法名顺序（原先顺序敏感、脆弱）。
 */
public class AccountServiceTest {

    private final AccountService accountService = new AccountServiceImpl();

    @Before
    public void resetDb() {
        // 每个用例从种子数据（账户1=1000, 账户2=500）开始，互不影响
        DatabaseInit.init();
    }

    @Test
    public void transfer_shouldMoveMoney() {
        accountService.transfer(1, 2, new BigDecimal("100.00"));
        AccountServiceTestHelper.assertBalance(1, new BigDecimal("900.00"));
        AccountServiceTestHelper.assertBalance(2, new BigDecimal("600.00"));
    }

    @Test
    public void transfer_shouldRollbackWhenInsufficientBalance() {
        try {
            accountService.transfer(2, 1, new BigDecimal("99999.00"));
            fail("余额不足应抛出异常");
        } catch (BizException expected) {
            assertEquals("余额不足", expected.getMessage());
        }
        // 回滚后两边余额都保持种子值
        AccountServiceTestHelper.assertBalance(2, new BigDecimal("500.00"));
        AccountServiceTestHelper.assertBalance(1, new BigDecimal("1000.00"));
    }

    @Test
    public void transfer_shouldRejectNonPositiveAmount() {
        try {
            accountService.transfer(1, 2, new BigDecimal("-100.00"));
            fail("负数金额应抛出异常");
        } catch (BizException expected) {
            assertEquals("转账金额必须为正数", expected.getMessage());
        }
        // 负数金额不能反向造钱：两边余额都不变
        AccountServiceTestHelper.assertBalance(1, new BigDecimal("1000.00"));
        AccountServiceTestHelper.assertBalance(2, new BigDecimal("500.00"));
    }

    @Test
    public void transfer_shouldRejectZeroAmount() {
        try {
            accountService.transfer(1, 2, BigDecimal.ZERO);
            fail("零金额应抛出异常");
        } catch (BizException expected) {
            assertEquals("转账金额必须为正数", expected.getMessage());
        }
    }

    @Test
    public void transfer_shouldRejectNonexistentAccount() {
        try {
            accountService.transfer(1, 9999, new BigDecimal("10.00"));
            fail("转入账户不存在应抛出异常");
        } catch (BizException expected) {
            assertEquals("转入账户不存在: 9999", expected.getMessage());
        }
        // 不存在的转入账户时事务回滚，转出账户余额不变
        AccountServiceTestHelper.assertBalance(1, new BigDecimal("1000.00"));
    }
}