-- 智能温室管理系统数据库表结构
-- 数据库：greenhouse_db

-- 1. 传感器数据表（每小时记录一次）
CREATE TABLE IF NOT EXISTS `sensor_data` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `record_time` DATETIME NOT NULL COMMENT '记录时间（精确到小时）',
  `temperature_c` DECIMAL(5,2) NOT NULL COMMENT '温度（摄氏度）',
  `soil_moisture_pct` DECIMAL(5,2) NOT NULL COMMENT '土壤湿度（百分比）',
  `light_lux` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '光照强度（lux）',
  `is_raining` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否下雨（0否/1是）',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX `idx_record_time` (`record_time`),
  INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='传感器数据表';

-- 2. 配方表
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

-- 3. 地块表
CREATE TABLE IF NOT EXISTS `plots` (
  `id` INT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '地块ID（对应地块编号）',
  `plot_number` INT UNSIGNED NOT NULL UNIQUE COMMENT '地块编号（1-4）',
  `name` VARCHAR(50) DEFAULT NULL COMMENT '地块名称（可选）',
  `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态（0禁用/1启用）',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='地块表';

-- 4. 地块配方分配表（记录当前分配关系）
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

-- 5. 地块定时执行计划表
CREATE TABLE IF NOT EXISTS `plot_schedules` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `plot_id` INT UNSIGNED NOT NULL COMMENT '地块ID',
  `recipe_id` VARCHAR(50) NOT NULL COMMENT '配方ID',
  `schedule_time` TIME NOT NULL COMMENT '执行时间（HH:mm格式）',
  `executions` INT UNSIGNED NOT NULL DEFAULT 1 COMMENT '执行次数',
  `is_enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用（0否/1是）',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX `idx_plot_id` (`plot_id`),
  INDEX `idx_recipe_id` (`recipe_id`),
  INDEX `idx_schedule_time` (`schedule_time`),
  INDEX `idx_is_enabled` (`is_enabled`),
  FOREIGN KEY (`plot_id`) REFERENCES `plots`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`recipe_id`) REFERENCES `recipes`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='地块定时执行计划表';

-- 6. 执行日志表
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

-- 7. 报警记录表
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

-- 8. 自动化设置表
CREATE TABLE IF NOT EXISTS `automation_settings` (
  `id` INT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `setting_key` VARCHAR(50) NOT NULL UNIQUE COMMENT '设置键名',
  `setting_value` TEXT NOT NULL COMMENT '设置值（JSON格式）',
  `description` VARCHAR(200) DEFAULT NULL COMMENT '设置描述',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='自动化设置表';

-- 初始化自动化设置数据
INSERT INTO `automation_settings` (`setting_key`, `setting_value`, `description`) VALUES
('light_lux_threshold', '8000', '光照强度阈值（低于此值自动开灯）'),
('soil_moisture_low_threshold', '35', '土壤湿度低阈值（低于此值自动抽水）'),
('auto_light_enabled', 'true', '自动补光灯开关'),
('auto_pump_enabled', 'true', '自动抽水开关')
ON DUPLICATE KEY UPDATE `updated_at` = CURRENT_TIMESTAMP;

-- 9. 图片表
CREATE TABLE IF NOT EXISTS `images` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `image_url` VARCHAR(500) NOT NULL COMMENT '图片URL',
  `record_time` DATETIME NOT NULL COMMENT '记录时间（对应传感器数据时间）',
  `temperature_c` DECIMAL(5,2) DEFAULT NULL COMMENT '温度（摄氏度）',
  `soil_moisture_pct` DECIMAL(5,2) DEFAULT NULL COMMENT '土壤湿度（百分比）',
  `light_lux` INT UNSIGNED DEFAULT NULL COMMENT '光照强度（lux）',
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

-- 10. 控制操作日志表
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

-- 初始化地块数据（1-4号地块）
INSERT INTO `plots` (`plot_number`, `name`, `status`) VALUES
(1, '地块1', 1),
(2, '地块2', 1),
(3, '地块3', 1),
(4, '地块4', 1)
ON DUPLICATE KEY UPDATE `updated_at` = CURRENT_TIMESTAMP;

-- 初始化基础配方
INSERT INTO `recipes` (`id`, `name`, `water_ml`, `nutrient_ml`, `rooting_powder_ml`, `special_ml`) VALUES
('r1', '基础配方', 500, 50, 0, 0)
ON DUPLICATE KEY UPDATE `updated_at` = CURRENT_TIMESTAMP;

