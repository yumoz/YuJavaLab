package com.example.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class DatabaseInit {

    private static final Logger log = LoggerFactory.getLogger(DatabaseInit.class);

    /** 默认数据库 URL；可通过系统属性 app.db.url 覆盖（测试用 target/test.db 与主库隔离）。 */
    public static final String DEFAULT_DB_URL = "jdbc:sqlite:yujavalab.db";

    public static void init() {
        String url = System.getProperty("app.db.url", DEFAULT_DB_URL);
        // 注意：此处 URL 必须与 SqlSessionFactoryUtil 注入 mybatis-config.xml 的 ${app.db.url} 保持一致
        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement();
             InputStream is = DatabaseInit.class.getResourceAsStream("/db/init.sql");
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

            StringBuilder sql = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sql.append(line).append("\n");
            }

            for (String s : sql.toString().split(";")) {
                String trimmed = s.trim();
                if (!trimmed.isEmpty()) {
                    stmt.execute(trimmed);
                }
            }
            log.info("SQLite database initialized successfully");
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize SQLite database", e);
        }
    }
}
