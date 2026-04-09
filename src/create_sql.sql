-- ==============================================
-- MyBatis 示例库初始化脚本
-- 功能：创建数据库、自定义用户、授权、建表、插测试数据
-- 适用：MySQL 8.0+（兼容 5.7，注意密码插件差异）
-- ==============================================

-- 1. 切换到 MySQL 系统库（执行授权需要）
USE mysql;

-- 2. 创建专用数据库（避免使用 root 库，规范管理）
-- 字符集：utf8mb4 兼容所有中文/特殊字符；排序规则：utf8mb4_unicode_ci 更通用
CREATE DATABASE IF NOT EXISTS mybatis_demo 
DEFAULT CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

-- 3. 创建自定义数据库用户（避免直接使用 root，遵循最小权限原则）
-- 格式：CREATE USER '用户名'@'访问来源' IDENTIFIED BY '密码';
-- 访问来源说明：
-- - localhost：仅允许本地访问（推荐）
-- - %：允许任意主机访问（测试环境可用，生产禁止）
CREATE USER IF NOT EXISTS 'mybatis_user'@'localhost' 
IDENTIFIED BY 'MyBatis@123456'; -- 密码要求：大小写+数字+特殊字符，符合 MySQL 8.0 密码策略

-- 4. 给自定义用户授权（仅授予必要权限，避免过度授权）
-- 授权范围：mybatis_demo 库下所有表（mybatis_demo.*）
-- 授权权限：SELECT/INSERT/UPDATE/DELETE（增删改查），测试环境可加 CREATE/DROP
GRANT SELECT, INSERT, UPDATE, DELETE ON mybatis_demo.* TO 'mybatis_user'@'localhost';

-- 5. 刷新权限（使授权立即生效）
FLUSH PRIVILEGES;

-- 6. 切换到目标数据库，创建业务表
USE mybatis_demo;

-- 6.1 创建用户表（适配 MyBatis 查询场景）
-- 字段注释清晰，主键自增，非空约束，默认值等符合规范
CREATE TABLE IF NOT EXISTS `user` (
    `id` INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户ID（主键）',
    `name` VARCHAR(50) NOT NULL COMMENT '用户名',
    `age` TINYINT UNSIGNED DEFAULT 0 COMMENT '年龄（无符号，0-255）',
    `email` VARCHAR(100) UNIQUE DEFAULT '' COMMENT '邮箱（唯一约束）',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（自动填充当前时间）',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间（自动更新）',
    PRIMARY KEY (`id`) COMMENT '主键索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户信息表';

-- 7. 插入测试数据（方便 MyBatis 测试查询）
INSERT INTO `user` (`name`, `age`, `email`) VALUES
('张三', 20, 'zhangsan@test.com'),
('李四', 25, 'lisi@test.com'),
('王五', 30, 'wangwu@test.com');

-- 8. 查询验证（可选，确认数据插入成功）
SELECT * FROM `user`;

-- ==============================================
-- 脚本执行完成提示
-- ==============================================
SELECT 'MyBatis 数据库初始化完成！' AS '执行结果';