package com.example.service;

import com.example.service.impl.AccountServiceImpl;
import com.example.util.DatabaseInit;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AccountServiceTest {

    private final AccountService accountService = new AccountServiceImpl();

    @BeforeClass
    public static void initDb() {
        DatabaseInit.init();
    }

    @Test
    public void transfer_shouldMoveMoney() {
        accountService.transfer(1, 2, 100.0);
        AccountServiceTestHelper.assertBalance(1, 900.0);
        AccountServiceTestHelper.assertBalance(2, 600.0);
    }

    @Test
    public void transfer_shouldRollbackWhenInsufficientBalance() {
        try {
            accountService.transfer(2, 1, 99999.0);
            fail("余额不足应抛出异常");
        } catch (IllegalStateException expected) {
            assertEquals("余额不足", expected.getMessage());
        }
        AccountServiceTestHelper.assertBalance(2, 600.0);
        AccountServiceTestHelper.assertBalance(1, 900.0);
    }
}
