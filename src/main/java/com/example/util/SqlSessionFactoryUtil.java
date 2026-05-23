package com.example.util;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;

public class SqlSessionFactoryUtil {

    private static volatile SqlSessionFactory sqlSessionFactory;

    private SqlSessionFactoryUtil() { }

    public static SqlSessionFactory getSqlSessionFactory() {
        if (sqlSessionFactory == null) {
            synchronized (SqlSessionFactoryUtil.class) {
                if (sqlSessionFactory == null) {
                    try (InputStream inputStream = Resources.getResourceAsStream("mybatis-config.xml")) {
                        sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to build SqlSessionFactory", e);
                    }
                }
            }
        }
        return sqlSessionFactory;
    }
}
