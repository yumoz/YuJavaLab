USE mybatis_demo;

-- 删除旧表（可选，确保字段一致）
DROP TABLE IF EXISTS `user`;

-- 重新创建 user 表，添加 username/password 字段
CREATE TABLE IF NOT EXISTS `user` (
    `id` INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户ID（主键）',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名（对应 Mapper 中的 username）',
    `password` VARCHAR(100) NOT NULL DEFAULT '123456' COMMENT '密码',
    `email` VARCHAR(100) UNIQUE DEFAULT '' COMMENT '邮箱',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`) COMMENT '主键索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户信息表';

-- 插入适配的测试数据
INSERT INTO `user` (`username`, `password`, `email`) VALUES
('zhangsan', '123456', 'zhangsan@test.com'),
('lisi', '654321', 'lisi@test.com');