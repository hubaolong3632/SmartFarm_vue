-- ============================================================
-- AI分析报告表
-- 用于存储AI生成的各种分析报告
-- ============================================================
CREATE TABLE IF NOT EXISTS `ai_reports` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `report_type` VARCHAR(50) NOT NULL COMMENT '报告类型（image_analysis/sensor_analysis/automation_advice/comprehensive_report/auto_execution）',
  `report_title` VARCHAR(200) DEFAULT NULL COMMENT '报告标题',
  `report_content` TEXT NOT NULL COMMENT '报告内容（Markdown格式）',
  `start_date` DATE DEFAULT NULL COMMENT '数据开始日期',
  `end_date` DATE DEFAULT NULL COMMENT '数据结束日期',
  `data_count` INT UNSIGNED DEFAULT 0 COMMENT '分析的数据条数',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX `idx_report_type` (`report_type`),
  INDEX `idx_created_at` (`created_at`),
  INDEX `idx_start_date` (`start_date`),
  INDEX `idx_end_date` (`end_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI分析报告表';

