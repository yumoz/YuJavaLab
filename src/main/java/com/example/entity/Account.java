package com.example.entity;

import java.math.BigDecimal;

/**
 * 账户实体。金额使用 {@link BigDecimal} 而非 double/float，
 * 避免浮点精度误差（教学最佳实践：金额一律用 BigDecimal）。
 */
public class Account {
    private Integer id;
    private String accountNo;
    private String name;
    private BigDecimal balance;

    public Account() { }

    public Account(String accountNo, String name, BigDecimal balance) {
        this.accountNo = accountNo;
        this.name = name;
        this.balance = balance;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getAccountNo() { return accountNo; }
    public void setAccountNo(String accountNo) { this.accountNo = accountNo; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }

    @Override
    public String toString() {
        return "Account{" +
                "id=" + id +
                ", accountNo='" + accountNo + '\'' +
                ", name='" + name + '\'' +
                ", balance=" + balance +
                '}';
    }
}