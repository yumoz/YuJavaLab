package com.example.entity;

import java.util.Date;

public class Order {
    private Integer id;
    private Integer userId;
    private String orderNo;
    private Double amount;
    private Date createTime;

    public Order() { }

    public Order(Integer userId, String orderNo, Double amount) {
        this.userId = userId;
        this.orderNo = orderNo;
        this.amount = amount;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }

    @Override
    public String toString() {
        return "Order{id=" + id + ", userId=" + userId + ", orderNo='" + orderNo + "', amount=" + amount + "}";
    }
}
