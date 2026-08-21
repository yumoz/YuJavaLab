package com.example.exception;

/**
 * 业务异常：余额不足、账户不存在、参数非法等可预期的业务规则违背。
 * 与系统异常（如 MyBatis 抛出的持久化异常）区分，供上层统一捕获提示。
 */
public class BizException extends RuntimeException {

    public BizException(String message) {
        super(message);
    }
}