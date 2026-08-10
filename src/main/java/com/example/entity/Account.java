package com.example.entity;

public class Account {
    private Integer id;
    private String accountNo;
    private String name;
    private Double balance;

    public Account() { }

    public Account(String accountNo, String name, Double balance) {
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
    public Double getBalance() { return balance; }
    public void setBalance(Double balance) { this.balance = balance; }
}
