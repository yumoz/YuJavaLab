package com.example.util;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * SqlSessionFactory 单例（双重检查锁）。
 * 构建时把系统属性 app.db.url 注入 mybatis-config.xml 的 ${app.db.url} 占位符，
 * 使数据库 URL 可配置（默认 jdbc:sqlite:yujavalab.db，测试进程由 surefire 覆盖为 target/test.db）。
 */
public class SqlSessionFactoryUtil {

    private static volatile SqlSessionFactory sqlSessionFactory;

    private SqlSessionFactoryUtil() { }

    public static SqlSessionFactory getSqlSessionFactory() {
        if (sqlSessionFactory == null) {
            synchronized (SqlSessionFactoryUtil.class) {
                if (sqlSessionFactory == null) {
                    Properties props = new Properties();
                    props.setProperty("app.db.url",
                            System.getProperty("app.db.url", DatabaseInit.DEFAULT_DB_URL));
                    try (InputStream inputStream = Resources.getResourceAsStream("mybatis-config.xml")) {
                        sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream, props);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to build SqlSessionFactory", e);
                    }
                }
            }
        }
        return sqlSessionFactory;
    }
}