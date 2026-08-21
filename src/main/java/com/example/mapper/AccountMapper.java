package com.example.mapper;

import com.example.entity.Account;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;

public interface AccountMapper {

    Account selectById(@Param("id") Integer id);

    /**
     * 原子扣减：UPDATE account SET balance = balance - #{amount} WHERE id = #{id} AND balance >= #{amount}。
     * 余额不足或账户不存在时影响行数为 0 —— 用原子 SQL 而非"读-算-写"，
     * 天然防止并发转账下的丢失更新/超扣。
     */
    int debit(@Param("id") Integer id, @Param("amount") BigDecimal amount);

    /** 原子增额：只增不减，并发转账到同一账户时结果仍正确。 */
    int credit(@Param("id") Integer id, @Param("amount") BigDecimal amount);
}