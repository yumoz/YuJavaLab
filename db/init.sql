DROP TABLE IF EXISTS `user`;

CREATE TABLE IF NOT EXISTS `user` (
    `id` INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户ID（主键）',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password` VARCHAR(100) NOT NULL DEFAULT '123456' COMMENT '密码',
    `email` VARCHAR(100) UNIQUE DEFAULT '' COMMENT '邮箱',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户信息表';

INSERT INTO `user` (`username`, `password`, `email`) VALUES
('zhangsan', '123456', 'zhangsan@test.com'),
('lisi', '654321', 'lisi@test.com');
