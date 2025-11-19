-- 用户表
CREATE TABLE IF NOT EXISTS `users` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名',
  `password` VARCHAR(255) NOT NULL COMMENT '密码（明文存储）',
  `nickname` VARCHAR(100) DEFAULT NULL COMMENT '昵称',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 插入默认管理员账户（用户名：admin，密码：admin123）
-- 密码使用明文存储
INSERT INTO `users` (`username`, `password`, `nickname`, `email`) VALUES
('admin', 'admin123', '管理员', 'admin@greenhouse.com')
ON DUPLICATE KEY UPDATE `username`=`username`;

