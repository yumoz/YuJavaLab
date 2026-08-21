package com.example.util;

import org.apache.ibatis.session.SqlSession;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * SqlSession 生命周期模板：统一"打开 → 执行 → 关闭"样板代码，
 * 让 Service 层只关心业务逻辑，不再每个方法重复 try-with-resources。
 *
 * 两种模式：
 * - {@link #execute(Function)}：自动提交（默认 openSession(true)），适用于查询与单条写；
 * - {@link #executeInTransaction(Consumer)}：程序化事务（openSession(false)），
 *   action 内抛出 RuntimeException 时自动回滚，否则提交。
 */
public final class SqlSessionTemplate {

    private SqlSessionTemplate() { }

    /** 自动提交模式下执行一个动作（查询/单条写），负责打开与关闭 SqlSession。 */
    public static <T> T execute(Function<SqlSession, T> action) {
        try (SqlSession session = SqlSessionFactoryUtil.getSqlSessionFactory().openSession(true)) {
            return action.apply(session);
        }
    }

    /** 程序化事务：手动提交；action 抛 RuntimeException 时回滚并重抛。 */
    public static void executeInTransaction(Consumer<SqlSession> action) {
        try (SqlSession session = SqlSessionFactoryUtil.getSqlSessionFactory().openSession(false)) {
            try {
                action.accept(session);
                session.commit();
            } catch (RuntimeException e) {
                session.rollback();
                throw e;
            }
        }
    }
}