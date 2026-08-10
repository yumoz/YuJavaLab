package com.example.mapper;

import com.example.entity.Account;
import org.apache.ibatis.annotations.Param;

public interface AccountMapper {
    Account selectById(@Param("id") Integer id);

    int updateBalance(@Param("id") Integer id, @Param("balance") Double balance);
}
