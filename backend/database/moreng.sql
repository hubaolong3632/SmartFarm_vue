-- ============================================================
-- 智能温室管理系统完整数据库脚本
-- 数据库：greenhouse_db
-- 说明：此脚本包含所有表结构、索引、外键和初始化数据
-- 可以直接在MySQL数据库中执行此文件
-- ============================================================

-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS `greenhouse_db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 使用数据库
USE `greenhouse_db`;

-- ============================================================
-- 1. 传感器数据表（每小时记录一次）
-- ============================================================
CREATE TABLE IF NOT EXISTS `sensor_data` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `record_time` DATETIME NOT NULL COMMENT '记录时间（精确到小时）',
  `temperature_c` DECIMAL(5,2) NOT NULL DEFAULT 0 COMMENT '温度（摄氏度）',
  `humidity_pct` DECIMAL(5,2) NOT NULL DEFAULT 0 COMMENT '湿度（百分比）',
  `soil_moisture_pct` DECIMAL(5,2) NOT NULL DEFAULT 0 COMMENT '土壤湿度（百分比）',
  `light_lux` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '光照强度（lux）',
  `is_raining` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否下雨（0否/1是）',
  `oxygen_pct` DECIMAL(5,2) NOT NULL DEFAULT 0 COMMENT '氧气含量（百分比）',
  `co2_ppm` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '二氧化碳含量（ppm）',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX `idx_record_time` (`record_time`),
  INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='传感器数据表';

-- ============================================================
-- 2. 配方表
-- ============================================================
CREATE TABLE IF NOT EXISTS `recipes` (
  `id` VARCHAR(50) NOT NULL PRIMARY KEY COMMENT '配方ID',
  `name` VARCHAR(100) NOT NULL COMMENT '配方名称',
  `water_ml` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '水（毫升）',
  `nutrient_ml` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '营养液（毫升）',
  `rooting_powder_ml` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '生根粉（毫升）',
  `special_ml` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '特殊营养（毫升）',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX `idx_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='配方表';

-- ============================================================
-- 3. 地块表
-- ============================================================
CREATE TABLE IF NOT EXISTS `plots` (
  `id` INT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '地块ID（对应地块编号）',
  `plot_number` INT UNSIGNED NOT NULL UNIQUE COMMENT '地块编号（1-4）',
  `name` VARCHAR(50) DEFAULT NULL COMMENT '地块名称（可选）',
  `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态（0禁用/1启用）',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='地块表';

-- ============================================================
-- 4. 地块配方分配表（记录当前分配关系）
-- ============================================================
CREATE TABLE IF NOT EXISTS `plot_assignments` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `plot_id` INT UNSIGNED NOT NULL COMMENT '地块ID',
  `recipe_id` VARCHAR(50) NOT NULL COMMENT '配方ID',
  `assigned_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '分配时间',
  `is_active` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否激活（0否/1是）',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX `idx_plot_id` (`plot_id`),
  INDEX `idx_recipe_id` (`recipe_id`),
  INDEX `idx_is_active` (`is_active`),
  FOREIGN KEY (`plot_id`) REFERENCES `plots`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`recipe_id`) REFERENCES `recipes`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='地块配方分配表';

-- ============================================================
-- 5. 地块定时执行计划表（增强版：支持每天/每周/每月执行）
-- ============================================================
CREATE TABLE IF NOT EXISTS `plot_schedules` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `plot_id` INT UNSIGNED NOT NULL COMMENT '地块ID',
  `recipe_id` VARCHAR(50) NOT NULL COMMENT '配方ID',
  `schedule_time` TIME NOT NULL COMMENT '执行时间（HH:mm格式）',
  `schedule_type` VARCHAR(20) NOT NULL DEFAULT 'daily' COMMENT '执行周期类型（daily每天/weekly每周/monthly每月）',
  `day_of_week` TINYINT(1) DEFAULT NULL COMMENT '周几（0-6，0=周日，1=周一，...，6=周六）',
  `schedule_datetime` DATETIME DEFAULT NULL COMMENT '精确执行时间（年月日时分秒）',
  `last_executed_at` DATETIME DEFAULT NULL COMMENT '上次执行时间',
  `executions` INT UNSIGNED NOT NULL DEFAULT 1 COMMENT '执行次数',
  `is_enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用（0否/1是）',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX `idx_plot_id` (`plot_id`),
  INDEX `idx_recipe_id` (`recipe_id`),
  INDEX `idx_schedule_time` (`schedule_time`),
  INDEX `idx_schedule_type` (`schedule_type`),
  INDEX `idx_day_of_week` (`day_of_week`),
  INDEX `idx_schedule_datetime` (`schedule_datetime`),
  INDEX `idx_is_enabled` (`is_enabled`),
  FOREIGN KEY (`plot_id`) REFERENCES `plots`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`recipe_id`) REFERENCES `recipes`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='地块定时执行计划表';

-- ============================================================
-- 6. 执行日志表
-- ============================================================
CREATE TABLE IF NOT EXISTS `execution_logs` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `plot_id` INT UNSIGNED NOT NULL COMMENT '地块ID',
  `recipe_id` VARCHAR(50) NOT NULL COMMENT '配方ID',
  `executions` INT UNSIGNED NOT NULL DEFAULT 1 COMMENT '执行次数',
  `executed_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '执行时间',
  `execution_type` VARCHAR(20) NOT NULL DEFAULT 'manual' COMMENT '执行类型（manual手动/scheduled定时）',
  `schedule_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '关联的定时计划ID（如果是定时执行）',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  INDEX `idx_plot_id` (`plot_id`),
  INDEX `idx_recipe_id` (`recipe_id`),
  INDEX `idx_executed_at` (`executed_at`),
  INDEX `idx_execution_type` (`execution_type`),
  FOREIGN KEY (`plot_id`) REFERENCES `plots`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`recipe_id`) REFERENCES `recipes`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`schedule_id`) REFERENCES `plot_schedules`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='执行日志表';

-- ============================================================
-- 7. 报警记录表
-- ============================================================
CREATE TABLE IF NOT EXISTS `alerts` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `level` VARCHAR(20) NOT NULL COMMENT '报警级别（info/warning/error）',
  `message` VARCHAR(500) NOT NULL COMMENT '报警消息',
  `alert_type` VARCHAR(50) DEFAULT NULL COMMENT '报警类型（temperature/soil_moisture/light/automation等）',
  `related_data` JSON DEFAULT NULL COMMENT '关联数据（JSON格式，存储相关传感器数据等）',
  `is_read` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已读（0未读/1已读）',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  INDEX `idx_level` (`level`),
  INDEX `idx_alert_type` (`alert_type`),
  INDEX `idx_is_read` (`is_read`),
  INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='报警记录表';

-- ============================================================
-- 8. 自动化设置表
-- ============================================================
CREATE TABLE IF NOT EXISTS `automation_settings` (
  `id` INT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `setting_key` VARCHAR(50) NOT NULL UNIQUE COMMENT '设置键名',
  `setting_value` TEXT NOT NULL COMMENT '设置值（JSON格式）',
  `description` VARCHAR(200) DEFAULT NULL COMMENT '设置描述',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='自动化设置表';

-- ============================================================
-- 9. 图片表
-- ============================================================
CREATE TABLE IF NOT EXISTS `images` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `image_url` VARCHAR(500) NOT NULL COMMENT '图片URL',
  `record_time` DATETIME NOT NULL COMMENT '记录时间（对应传感器数据时间）',
  `temperature_c` DECIMAL(5,2) DEFAULT NULL COMMENT '温度（摄氏度）',
  `humidity_pct` DECIMAL(5,2) DEFAULT NULL COMMENT '湿度（百分比）',
  `soil_moisture_pct` DECIMAL(5,2) DEFAULT NULL COMMENT '土壤湿度（百分比）',
  `light_lux` INT UNSIGNED DEFAULT NULL COMMENT '光照强度（lux）',
  `is_raining` TINYINT(1) DEFAULT NULL COMMENT '是否下雨（0否/1是）',
  `oxygen_pct` DECIMAL(5,2) DEFAULT NULL COMMENT '氧气含量（百分比）',
  `co2_ppm` INT UNSIGNED DEFAULT NULL COMMENT '二氧化碳含量（ppm）',
  `plot_id` INT UNSIGNED DEFAULT NULL COMMENT '关联地块ID（可选）',
  `is_abnormal` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否异常（0否/1是）',
  `abnormal_reason` VARCHAR(200) DEFAULT NULL COMMENT '异常原因',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  INDEX `idx_record_time` (`record_time`),
  INDEX `idx_plot_id` (`plot_id`),
  INDEX `idx_is_abnormal` (`is_abnormal`),
  INDEX `idx_created_at` (`created_at`),
  FOREIGN KEY (`plot_id`) REFERENCES `plots`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='图片表';

-- ============================================================
-- 10. 控制操作日志表
-- ============================================================
CREATE TABLE IF NOT EXISTS `control_logs` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `control_type` VARCHAR(50) NOT NULL COMMENT '控制类型（light/cleaning等）',
  `action` VARCHAR(50) NOT NULL COMMENT '操作动作（on/off/start/stop等）',
  `status` VARCHAR(20) NOT NULL COMMENT '操作状态（success/failed）',
  `message` VARCHAR(200) DEFAULT NULL COMMENT '操作消息',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  INDEX `idx_control_type` (`control_type`),
  INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='控制操作日志表';

-- ============================================================
-- 11. AI执行操作日志表
-- ============================================================
CREATE TABLE IF NOT EXISTS `ai_execution_logs` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `operation_type` VARCHAR(50) NOT NULL COMMENT '操作类型（light/pump/recipe）',
  `action` VARCHAR(50) DEFAULT NULL COMMENT '动作（on/off等）',
  `plot_id` INT UNSIGNED DEFAULT NULL COMMENT '地块ID',
  `recipe_id` VARCHAR(100) DEFAULT NULL COMMENT '配方ID',
  `executions` INT UNSIGNED DEFAULT NULL COMMENT '执行次数',
  `reason` VARCHAR(255) DEFAULT NULL COMMENT 'AI建议原因',
  `payload` TEXT DEFAULT NULL COMMENT '原始MQTT消息内容',
  `execute_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '执行时间',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  INDEX `idx_operation_type` (`operation_type`),
  INDEX `idx_execute_time` (`execute_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI自动执行操作日志表';

-- ============================================================
-- 初始化数据
-- ============================================================

-- 初始化地块数据（1-4号地块）
INSERT INTO `plots` (`plot_number`, `name`, `status`) VALUES
(1, '地块1', 1),
(2, '地块2', 1),
(3, '地块3', 1),
(4, '地块4', 1)
ON DUPLICATE KEY UPDATE `updated_at` = CURRENT_TIMESTAMP;

-- 初始化配方数据（多个配方）
INSERT INTO `recipes` (`id`, `name`, `water_ml`, `nutrient_ml`, `rooting_powder_ml`, `special_ml`) VALUES
('r1', '基础配方', 500, 50, 0, 0),
('r2', '营养强化配方', 600, 80, 10, 0),
('r3', '生根专用配方', 500, 40, 30, 0),
('r4', '特殊营养配方', 550, 60, 5, 20),
('r5', '高浓度配方', 400, 100, 20, 30),
('r6', '低浓度配方', 700, 30, 0, 0)
ON DUPLICATE KEY UPDATE `updated_at` = CURRENT_TIMESTAMP;

-- 初始化自动化设置数据
INSERT INTO `automation_settings` (`setting_key`, `setting_value`, `description`) VALUES
('light_lux_threshold', '8000', '光照强度阈值（低于此值自动开灯）'),
('soil_moisture_low_threshold', '35', '土壤湿度低阈值（低于此值自动抽水）'),
('temperature_high_threshold', '35', '温度高阈值（高于此值报警）'),
('temperature_low_threshold', '10', '温度低阈值（低于此值报警）'),
('humidity_high_threshold', '80', '湿度高阈值（高于此值报警）'),
('humidity_low_threshold', '30', '湿度低阈值（低于此值报警）'),
('oxygen_low_threshold', '18', '氧气含量低阈值（低于此值报警）'),
('co2_high_threshold', '1000', '二氧化碳含量高阈值（高于此值报警）'),
('auto_light_enabled', 'true', '自动补光灯开关'),
('auto_pump_enabled', 'true', '自动抽水开关')
ON DUPLICATE KEY UPDATE `updated_at` = CURRENT_TIMESTAMP;

-- ============================================================
-- 测试数据插入
-- ============================================================

-- 设置当前时间变量
SET @now = NOW();
SET @start_time = DATE_SUB(@now, INTERVAL 23 HOUR);

-- 插入传感器数据（最近24小时的数据，包含所有新字段）
INSERT INTO `sensor_data` (`record_time`, `temperature_c`, `humidity_pct`, `soil_moisture_pct`, `light_lux`, `is_raining`, `oxygen_pct`, `co2_ppm`) VALUES
(DATE_SUB(@now, INTERVAL 23 HOUR), 18.5, 55.2, 45.2, 0, 0, 20.5, 400),
(DATE_SUB(@now, INTERVAL 22 HOUR), 17.8, 56.1, 46.1, 0, 0, 20.6, 410),
(DATE_SUB(@now, INTERVAL 21 HOUR), 17.2, 57.3, 47.3, 0, 0, 20.7, 420),
(DATE_SUB(@now, INTERVAL 20 HOUR), 16.5, 58.5, 48.5, 0, 0, 20.8, 430),
(DATE_SUB(@now, INTERVAL 19 HOUR), 16.0, 59.2, 49.2, 0, 0, 20.9, 440),
(DATE_SUB(@now, INTERVAL 18 HOUR), 15.8, 60.1, 50.1, 0, 0, 21.0, 450),
(DATE_SUB(@now, INTERVAL 17 HOUR), 16.2, 61.3, 51.3, 0, 0, 21.1, 460),
(DATE_SUB(@now, INTERVAL 16 HOUR), 17.5, 62.0, 52.0, 0, 0, 21.2, 470),
(DATE_SUB(@now, INTERVAL 15 HOUR), 19.2, 61.5, 51.5, 500, 0, 21.3, 480),
(DATE_SUB(@now, INTERVAL 14 HOUR), 21.5, 60.8, 50.8, 2500, 0, 21.4, 490),
(DATE_SUB(@now, INTERVAL 13 HOUR), 24.2, 59.5, 49.5, 8000, 0, 21.5, 500),
(DATE_SUB(@now, INTERVAL 12 HOUR), 26.8, 58.2, 48.2, 15000, 0, 21.6, 510),
(DATE_SUB(@now, INTERVAL 11 HOUR), 28.5, 56.8, 46.8, 22000, 0, 21.7, 520),
(DATE_SUB(@now, INTERVAL 10 HOUR), 29.2, 55.5, 45.5, 28000, 0, 21.8, 530),
(DATE_SUB(@now, INTERVAL 9 HOUR), 30.1, 54.2, 44.2, 32000, 0, 21.9, 540),
(DATE_SUB(@now, INTERVAL 8 HOUR), 29.8, 53.5, 43.5, 30000, 0, 22.0, 550),
(DATE_SUB(@now, INTERVAL 7 HOUR), 28.5, 52.8, 42.8, 25000, 0, 22.1, 560),
(DATE_SUB(@now, INTERVAL 6 HOUR), 26.2, 51.5, 41.5, 18000, 0, 22.2, 570),
(DATE_SUB(@now, INTERVAL 5 HOUR), 23.5, 50.2, 40.2, 10000, 0, 22.3, 580),
(DATE_SUB(@now, INTERVAL 4 HOUR), 21.2, 49.5, 39.5, 5000, 0, 22.4, 590),
(DATE_SUB(@now, INTERVAL 3 HOUR), 19.5, 48.8, 38.8, 2000, 0, 22.5, 600),
(DATE_SUB(@now, INTERVAL 2 HOUR), 18.2, 47.5, 37.5, 500, 0, 22.6, 610),
(DATE_SUB(@now, INTERVAL 1 HOUR), 17.8, 46.2, 36.2, 0, 0, 22.7, 620),
(@now, 17.5, 45.8, 35.8, 0, 0, 22.8, 630);

-- 插入一些异常传感器数据用于测试
INSERT INTO `sensor_data` (`record_time`, `temperature_c`, `humidity_pct`, `soil_moisture_pct`, `light_lux`, `is_raining`, `oxygen_pct`, `co2_ppm`) VALUES
(DATE_SUB(@now, INTERVAL 2 DAY), 41.5, 85.0, 20.0, 2000, 0, 15.5, 1200),
(DATE_SUB(@now, INTERVAL 2 DAY), 8.5, 25.0, 12.0, 500, 1, 18.0, 800),
(DATE_SUB(@now, INTERVAL 1 DAY), 36.2, 82.5, 28.5, 15000, 0, 19.2, 1100);

-- 地块配方分配（plot_assignments）
INSERT INTO `plot_assignments` (`plot_id`, `recipe_id`, `assigned_at`, `is_active`) 
SELECT p.id, 'r1', NOW(), 1 FROM plots p WHERE p.plot_number = 1
ON DUPLICATE KEY UPDATE `updated_at` = CURRENT_TIMESTAMP;

INSERT INTO `plot_assignments` (`plot_id`, `recipe_id`, `assigned_at`, `is_active`) 
SELECT p.id, 'r2', NOW(), 1 FROM plots p WHERE p.plot_number = 2
ON DUPLICATE KEY UPDATE `updated_at` = CURRENT_TIMESTAMP;

INSERT INTO `plot_assignments` (`plot_id`, `recipe_id`, `assigned_at`, `is_active`) 
SELECT p.id, 'r3', NOW(), 1 FROM plots p WHERE p.plot_number = 3
ON DUPLICATE KEY UPDATE `updated_at` = CURRENT_TIMESTAMP;

INSERT INTO `plot_assignments` (`plot_id`, `recipe_id`, `assigned_at`, `is_active`) 
SELECT p.id, 'r1', NOW(), 1 FROM plots p WHERE p.plot_number = 4
ON DUPLICATE KEY UPDATE `updated_at` = CURRENT_TIMESTAMP;

-- 定时执行计划（plot_schedules）- 包含增强字段
INSERT INTO `plot_schedules` (`plot_id`, `recipe_id`, `schedule_time`, `schedule_type`, `day_of_week`, `schedule_datetime`, `executions`, `is_enabled`) 
SELECT p.id, 'r1', '08:00', 'daily', NULL, NULL, 2, 1 FROM plots p WHERE p.plot_number = 1
ON DUPLICATE KEY UPDATE `updated_at` = CURRENT_TIMESTAMP;

INSERT INTO `plot_schedules` (`plot_id`, `recipe_id`, `schedule_time`, `schedule_type`, `day_of_week`, `schedule_datetime`, `executions`, `is_enabled`) 
SELECT p.id, 'r1', '14:00', 'daily', NULL, NULL, 1, 1 FROM plots p WHERE p.plot_number = 1
ON DUPLICATE KEY UPDATE `updated_at` = CURRENT_TIMESTAMP;

INSERT INTO `plot_schedules` (`plot_id`, `recipe_id`, `schedule_time`, `schedule_type`, `day_of_week`, `schedule_datetime`, `executions`, `is_enabled`) 
SELECT p.id, 'r2', '09:00', 'weekly', 1, NULL, 3, 1 FROM plots p WHERE p.plot_number = 2
ON DUPLICATE KEY UPDATE `updated_at` = CURRENT_TIMESTAMP;

INSERT INTO `plot_schedules` (`plot_id`, `recipe_id`, `schedule_time`, `schedule_type`, `day_of_week`, `schedule_datetime`, `executions`, `is_enabled`) 
SELECT p.id, 'r3', '10:00', 'weekly', 3, NULL, 2, 1 FROM plots p WHERE p.plot_number = 3
ON DUPLICATE KEY UPDATE `updated_at` = CURRENT_TIMESTAMP;

INSERT INTO `plot_schedules` (`plot_id`, `recipe_id`, `schedule_time`, `schedule_type`, `day_of_week`, `schedule_datetime`, `executions`, `is_enabled`) 
SELECT p.id, 'r1', '16:00', 'monthly', NULL, CONCAT(DATE_FORMAT(NOW(), '%Y-%m-'), '15 16:00:00'), 1, 1 FROM plots p WHERE p.plot_number = 4
ON DUPLICATE KEY UPDATE `updated_at` = CURRENT_TIMESTAMP;

-- 执行日志（execution_logs）
INSERT INTO `execution_logs` (`plot_id`, `recipe_id`, `executions`, `executed_at`, `execution_type`, `schedule_id`)
SELECT p.id, 'r1', 2, DATE_SUB(NOW(), INTERVAL 2 HOUR), 'manual', NULL FROM plots p WHERE p.plot_number = 1;

INSERT INTO `execution_logs` (`plot_id`, `recipe_id`, `executions`, `executed_at`, `execution_type`, `schedule_id`)
SELECT p.id, 'r1', 1, DATE_SUB(NOW(), INTERVAL 1 HOUR), 'scheduled', NULL FROM plots p WHERE p.plot_number = 1;

INSERT INTO `execution_logs` (`plot_id`, `recipe_id`, `executions`, `executed_at`, `execution_type`, `schedule_id`)
SELECT p.id, 'r2', 3, DATE_SUB(NOW(), INTERVAL 3 HOUR), 'manual', NULL FROM plots p WHERE p.plot_number = 2;

INSERT INTO `execution_logs` (`plot_id`, `recipe_id`, `executions`, `executed_at`, `execution_type`, `schedule_id`)
SELECT p.id, 'r2', 2, DATE_SUB(NOW(), INTERVAL 5 HOUR), 'scheduled', NULL FROM plots p WHERE p.plot_number = 2;

INSERT INTO `execution_logs` (`plot_id`, `recipe_id`, `executions`, `executed_at`, `execution_type`, `schedule_id`)
SELECT p.id, 'r1', 1, DATE_SUB(NOW(), INTERVAL 4 HOUR), 'manual', NULL FROM plots p WHERE p.plot_number = 3;

INSERT INTO `execution_logs` (`plot_id`, `recipe_id`, `executions`, `executed_at`, `execution_type`, `schedule_id`)
SELECT p.id, 'r3', 2, DATE_SUB(NOW(), INTERVAL 6 HOUR), 'scheduled', NULL FROM plots p WHERE p.plot_number = 3;

INSERT INTO `execution_logs` (`plot_id`, `recipe_id`, `executions`, `executed_at`, `execution_type`, `schedule_id`)
SELECT p.id, 'r1', 1, DATE_SUB(NOW(), INTERVAL 7 HOUR), 'manual', NULL FROM plots p WHERE p.plot_number = 4;

INSERT INTO `execution_logs` (`plot_id`, `recipe_id`, `executions`, `executed_at`, `execution_type`, `schedule_id`)
SELECT p.id, 'r4', 1, DATE_SUB(NOW(), INTERVAL 1 DAY), 'manual', NULL FROM plots p WHERE p.plot_number = 1;

INSERT INTO `execution_logs` (`plot_id`, `recipe_id`, `executions`, `executed_at`, `execution_type`, `schedule_id`)
SELECT p.id, 'r5', 2, DATE_SUB(NOW(), INTERVAL 2 DAY), 'manual', NULL FROM plots p WHERE p.plot_number = 2;

-- 报警记录（alerts）
INSERT INTO `alerts` (`level`, `message`, `alert_type`, `related_data`, `is_read`) VALUES
('warning', '温度异常：温度 41.5°C', 'temperature', '{"temperature": 41.5, "threshold": 35}', 0),
('warning', '土壤湿度异常：湿度 20%', 'soil_moisture', '{"moisture": 20, "threshold": 35}', 0),
('warning', '温度异常：温度 8.5°C', 'temperature', '{"temperature": 8.5, "threshold": 10}', 0),
('error', '氧气含量异常：氧气 15.5%', 'oxygen', '{"oxygen": 15.5, "threshold": 18}', 0),
('error', '二氧化碳含量异常：二氧化碳 1200ppm', 'co2', '{"co2": 1200, "threshold": 1000}', 0),
('warning', '湿度异常：湿度 85%', 'humidity', '{"humidity": 85, "threshold": 80}', 0),
('info', '自动化：光照(500) 低于阈值，已开启补光灯', 'automation', '{"lightLux": 500, "threshold": 8000}', 0),
('info', '自动化：土壤湿度(25%) 低，执行抽水', 'automation', '{"moisture": 25, "threshold": 35}', 0),
('info', '地块1 分配配方后执行 2 次', 'assignment', '{"plot": 1, "recipe": "r1", "executions": 2}', 1),
('info', '地块2 添加定时 09:00 执行 3 次', 'schedule', '{"plot": 2, "time": "09:00", "executions": 3}', 1),
('warning', '温度持续偏高：温度 36.2°C', 'temperature', '{"temperature": 36.2, "threshold": 35}', 0),
('info', '自动化：光照恢复，已关闭补光灯', 'automation', '{"lightLux": 15000, "threshold": 8000}', 1);

-- 图片数据（images）- 包含所有新字段
INSERT INTO `images` (`image_url`, `record_time`, `temperature_c`, `humidity_pct`, `soil_moisture_pct`, `light_lux`, `is_raining`, `oxygen_pct`, `co2_ppm`, `plot_id`, `is_abnormal`, `abnormal_reason`) VALUES
('https://example.com/images/plant_001.jpg', DATE_SUB(NOW(), INTERVAL 1 DAY), 25.5, 60.2, 45.2, 12000, 0, 20.5, 400, 1, 0, NULL),
('https://example.com/images/plant_002.jpg', DATE_SUB(NOW(), INTERVAL 1 DAY), 26.2, 59.8, 44.8, 15000, 0, 20.6, 410, 1, 0, NULL),
('https://example.com/images/plant_003.jpg', DATE_SUB(NOW(), INTERVAL 1 DAY), 27.1, 58.5, 43.5, 18000, 0, 20.7, 420, 2, 0, NULL),
('https://example.com/images/plant_004.jpg', DATE_SUB(NOW(), INTERVAL 1 DAY), 28.5, 57.2, 42.2, 22000, 0, 20.8, 430, 2, 0, NULL),
('https://example.com/images/plant_005.jpg', DATE_SUB(NOW(), INTERVAL 1 DAY), 29.2, 56.8, 41.8, 25000, 0, 20.9, 440, 3, 0, NULL),
('https://example.com/images/plant_006.jpg', DATE_SUB(NOW(), INTERVAL 1 DAY), 30.1, 55.5, 40.5, 28000, 0, 21.0, 450, 3, 0, NULL),
('https://example.com/images/plant_007.jpg', DATE_SUB(NOW(), INTERVAL 1 DAY), 28.8, 54.2, 39.2, 26000, 0, 21.1, 460, 4, 0, NULL),
('https://example.com/images/plant_008.jpg', DATE_SUB(NOW(), INTERVAL 1 DAY), 27.5, 53.5, 38.5, 20000, 0, 21.2, 470, 4, 0, NULL),
('https://example.com/images/plant_009.jpg', DATE_SUB(NOW(), INTERVAL 3 HOUR), 41.5, 85.0, 20.0, 2000, 0, 15.5, 1200, 1, 1, '温度异常, 土壤湿度异常'),
('https://example.com/images/plant_010.jpg', DATE_SUB(NOW(), INTERVAL 2 HOUR), 19.5, 48.8, 38.8, 2000, 0, 22.5, 600, 2, 0, NULL),
('https://example.com/images/plant_011.jpg', DATE_SUB(NOW(), INTERVAL 1 HOUR), 18.2, 47.5, 37.5, 500, 1, 22.6, 610, 3, 0, NULL),
('https://example.com/images/plant_012.jpg', NOW(), 17.5, 45.8, 35.8, 0, 0, 22.8, 630, 4, 0, NULL),
('https://example.com/images/plant_013.jpg', DATE_SUB(NOW(), INTERVAL 8 HOUR), 29.8, 53.5, 25.0, 30000, 0, 22.0, 550, 1, 1, '土壤湿度异常'),
('https://example.com/images/plant_014.jpg', DATE_SUB(NOW(), INTERVAL 15 HOUR), 8.5, 60.1, 50.1, 500, 0, 21.0, 450, 2, 1, '温度异常'),
('https://example.com/images/plant_015.jpg', DATE_SUB(NOW(), INTERVAL 2 DAY), 36.2, 82.5, 28.5, 15000, 0, 19.2, 1100, 1, 1, '温度异常, 湿度异常, 二氧化碳含量异常'),
('https://example.com/images/plant_016.jpg', DATE_SUB(NOW(), INTERVAL 2 DAY), 22.5, 55.0, 42.0, 12000, 0, 21.5, 500, 2, 0, NULL);

-- 控制操作日志（control_logs）
INSERT INTO `control_logs` (`control_type`, `action`, `status`, `message`) VALUES
('light', 'on', 'success', '植物补光灯已打开'),
('light', 'off', 'success', '植物补光灯已关闭'),
('cleaning', 'start', 'success', '清理搅拌熔炉操作已启动'),
('cleaning', 'stop', 'success', '清理搅拌熔炉操作已完成'),
('light', 'on', 'success', '自动化：光照低于阈值，已开启补光灯'),
('light', 'off', 'success', '自动化：光照恢复，已关闭补光灯'),
('pump', 'start', 'success', '自动化：土壤湿度低，已启动抽水'),
('pump', 'stop', 'success', '抽水操作已完成'),
('ventilation', 'on', 'success', '温度过高，已开启通风系统'),
('ventilation', 'off', 'success', '温度恢复正常，已关闭通风系统');

-- ============================================================
-- 数据库迁移脚本（用于更新现有数据库）
-- 如果表已存在但缺少新字段，执行此部分
-- ============================================================

-- 检查并添加 sensor_data 表的新字段（如果表已存在）
-- 注意：如果字段已存在会报错，可以忽略

-- 添加 humidity_pct 字段
SET @exist := (SELECT COUNT(*) FROM information_schema.COLUMNS 
                WHERE TABLE_SCHEMA = DATABASE() 
                AND TABLE_NAME = 'sensor_data' 
                AND COLUMN_NAME = 'humidity_pct');
SET @sqlstmt := IF(@exist = 0, 
    'ALTER TABLE `sensor_data` ADD COLUMN `humidity_pct` DECIMAL(5,2) NOT NULL DEFAULT 0 COMMENT ''湿度（百分比）'' AFTER `temperature_c`', 
    'SELECT ''Column humidity_pct already exists'' AS message');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加 oxygen_pct 字段
SET @exist := (SELECT COUNT(*) FROM information_schema.COLUMNS 
                WHERE TABLE_SCHEMA = DATABASE() 
                AND TABLE_NAME = 'sensor_data' 
                AND COLUMN_NAME = 'oxygen_pct');
SET @sqlstmt := IF(@exist = 0, 
    'ALTER TABLE `sensor_data` ADD COLUMN `oxygen_pct` DECIMAL(5,2) NOT NULL DEFAULT 0 COMMENT ''氧气含量（百分比）'' AFTER `is_raining`', 
    'SELECT ''Column oxygen_pct already exists'' AS message');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加 co2_ppm 字段
SET @exist := (SELECT COUNT(*) FROM information_schema.COLUMNS 
                WHERE TABLE_SCHEMA = DATABASE() 
                AND TABLE_NAME = 'sensor_data' 
                AND COLUMN_NAME = 'co2_ppm');
SET @sqlstmt := IF(@exist = 0, 
    'ALTER TABLE `sensor_data` ADD COLUMN `co2_ppm` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT ''二氧化碳含量（ppm）'' AFTER `oxygen_pct`', 
    'SELECT ''Column co2_ppm already exists'' AS message');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 检查并添加 plot_schedules 表的新字段（如果表已存在）
-- 注意：如果字段已存在会报错，可以忽略

-- 添加 schedule_type 字段
SET @exist := (SELECT COUNT(*) FROM information_schema.COLUMNS 
                WHERE TABLE_SCHEMA = DATABASE() 
                AND TABLE_NAME = 'plot_schedules' 
                AND COLUMN_NAME = 'schedule_type');
SET @sqlstmt := IF(@exist = 0, 
    'ALTER TABLE `plot_schedules` ADD COLUMN `schedule_type` VARCHAR(20) NOT NULL DEFAULT ''daily'' COMMENT ''执行周期类型（daily每天/weekly每周/monthly每月）'' AFTER `schedule_time`', 
    'SELECT ''Column schedule_type already exists'' AS message');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加 day_of_week 字段
SET @exist := (SELECT COUNT(*) FROM information_schema.COLUMNS 
                WHERE TABLE_SCHEMA = DATABASE() 
                AND TABLE_NAME = 'plot_schedules' 
                AND COLUMN_NAME = 'day_of_week');
SET @sqlstmt := IF(@exist = 0, 
    'ALTER TABLE `plot_schedules` ADD COLUMN `day_of_week` TINYINT(1) DEFAULT NULL COMMENT ''周几（0-6，0=周日，1=周一，...，6=周六）'' AFTER `schedule_type`', 
    'SELECT ''Column day_of_week already exists'' AS message');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加 schedule_datetime 字段
SET @exist := (SELECT COUNT(*) FROM information_schema.COLUMNS 
                WHERE TABLE_SCHEMA = DATABASE() 
                AND TABLE_NAME = 'plot_schedules' 
                AND COLUMN_NAME = 'schedule_datetime');
SET @sqlstmt := IF(@exist = 0, 
    'ALTER TABLE `plot_schedules` ADD COLUMN `schedule_datetime` DATETIME DEFAULT NULL COMMENT ''精确执行时间（年月日时分秒）'' AFTER `day_of_week`', 
    'SELECT ''Column schedule_datetime already exists'' AS message');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加 last_executed_at 字段
SET @exist := (SELECT COUNT(*) FROM information_schema.COLUMNS 
                WHERE TABLE_SCHEMA = DATABASE() 
                AND TABLE_NAME = 'plot_schedules' 
                AND COLUMN_NAME = 'last_executed_at');
SET @sqlstmt := IF(@exist = 0, 
    'ALTER TABLE `plot_schedules` ADD COLUMN `last_executed_at` DATETIME DEFAULT NULL COMMENT ''上次执行时间'' AFTER `schedule_datetime`', 
    'SELECT ''Column last_executed_at already exists'' AS message');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加索引 idx_schedule_type
SET @exist := (SELECT COUNT(*) FROM information_schema.STATISTICS 
                WHERE TABLE_SCHEMA = DATABASE() 
                AND TABLE_NAME = 'plot_schedules' 
                AND INDEX_NAME = 'idx_schedule_type');
SET @sqlstmt := IF(@exist = 0, 
    'ALTER TABLE `plot_schedules` ADD INDEX `idx_schedule_type` (`schedule_type`)', 
    'SELECT ''Index idx_schedule_type already exists'' AS message');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加索引 idx_day_of_week
SET @exist := (SELECT COUNT(*) FROM information_schema.STATISTICS 
                WHERE TABLE_SCHEMA = DATABASE() 
                AND TABLE_NAME = 'plot_schedules' 
                AND INDEX_NAME = 'idx_day_of_week');
SET @sqlstmt := IF(@exist = 0, 
    'ALTER TABLE `plot_schedules` ADD INDEX `idx_day_of_week` (`day_of_week`)', 
    'SELECT ''Index idx_day_of_week already exists'' AS message');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加索引 idx_schedule_datetime
SET @exist := (SELECT COUNT(*) FROM information_schema.STATISTICS 
                WHERE TABLE_SCHEMA = DATABASE() 
                AND TABLE_NAME = 'plot_schedules' 
                AND INDEX_NAME = 'idx_schedule_datetime');
SET @sqlstmt := IF(@exist = 0, 
    'ALTER TABLE `plot_schedules` ADD INDEX `idx_schedule_datetime` (`schedule_datetime`)', 
    'SELECT ''Index idx_schedule_datetime already exists'' AS message');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 更新现有数据：为没有 schedule_type 的记录设置默认值
UPDATE `plot_schedules` 
SET `schedule_type` = 'daily'
WHERE `schedule_type` IS NULL OR `schedule_type` = '';

-- ============================================================
-- 脚本执行完成
-- ============================================================
SELECT '数据库初始化和测试数据插入完成！' AS message;
SELECT COUNT(*) AS plots_count FROM plots;
SELECT COUNT(*) AS recipes_count FROM recipes;
SELECT COUNT(*) AS automation_settings_count FROM automation_settings;
SELECT COUNT(*) AS sensor_data_count FROM sensor_data;
SELECT COUNT(*) AS plot_assignments_count FROM plot_assignments;
SELECT COUNT(*) AS plot_schedules_count FROM plot_schedules;
SELECT COUNT(*) AS execution_logs_count FROM execution_logs;
SELECT COUNT(*) AS alerts_count FROM alerts;
SELECT COUNT(*) AS images_count FROM images;
SELECT COUNT(*) AS control_logs_count FROM control_logs;

