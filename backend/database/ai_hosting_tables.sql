-- ============================================================
-- AI自动托管功能数据库表
-- ============================================================

-- AI托管配置表
CREATE TABLE IF NOT EXISTS `ai_hosting_config` (
  `id` INT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `enabled` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否启用（0否/1是）',
  `email_enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用邮件报警（0否/1是）',
  `email_address` VARCHAR(200) DEFAULT NULL COMMENT '接收报警的邮箱地址',
  `check_interval_minutes` INT UNSIGNED NOT NULL DEFAULT 10 COMMENT '检查间隔（分钟）',
  `water_control_enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用自动控制水（0否/1是）',
  `light_control_enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用自动控制补光（0否/1是）',
  `recipe_execution_enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用自动执行土壤配方（0否/1是）',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI托管配置表';

-- 插入默认配置
INSERT INTO `ai_hosting_config` (`enabled`, `email_enabled`, `email_address`, `check_interval_minutes`, `water_control_enabled`, `light_control_enabled`, `recipe_execution_enabled`)
VALUES (0, 1, NULL, 10, 1, 1, 1)
ON DUPLICATE KEY UPDATE `updated_at` = CURRENT_TIMESTAMP;

-- AI托管执行日志表
CREATE TABLE IF NOT EXISTS `ai_hosting_logs` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `execution_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '执行时间',
  `status` VARCHAR(20) NOT NULL DEFAULT 'success' COMMENT '执行状态（success/failed/partial）',
  `actions_taken` JSON DEFAULT NULL COMMENT '执行的操作列表（JSON格式）',
  `issues_detected` JSON DEFAULT NULL COMMENT '检测到的问题列表（JSON格式）',
  `email_sent` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已发送邮件（0否/1是）',
  `email_content` TEXT DEFAULT NULL COMMENT '邮件内容',
  `execution_duration_ms` INT UNSIGNED DEFAULT NULL COMMENT '执行耗时（毫秒）',
  `error_message` TEXT DEFAULT NULL COMMENT '错误信息',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  INDEX `idx_execution_time` (`execution_time`),
  INDEX `idx_status` (`status`),
  INDEX `idx_email_sent` (`email_sent`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI托管执行日志表';

