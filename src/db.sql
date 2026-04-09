CREATE DATABASE IF NOT EXISTS mybatis_demo;
USE mybatis_demo;

-- 用户表
CREATE TABLE IF NOT EXISTS user (
                                    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
                                    username VARCHAR(50) NOT NULL COMMENT '用户名',
    password VARCHAR(50) NOT NULL COMMENT '密码',
    email VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 插入测试数据
INSERT INTO user (username, password, email) VALUES
                                                 ('zhangsan', '123456', 'zhangsan@example.com'),
                                                 ('lisi', '654321', 'lisi@example.com');