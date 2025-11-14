-- 智能温室管理系统测试数据
-- 执行此脚本前请确保已执行 schema.sql 创建表结构

-- 1. 配方数据（recipes）
INSERT INTO recipes (id, name, water_ml, nutrient_ml, rooting_powder_ml, special_ml) VALUES
('r1', '基础配方', 500, 50, 0, 0),
('r2', '营养强化配方', 600, 80, 10, 0),
('r3', '生根专用配方', 500, 40, 30, 0),
('r4', '特殊营养配方', 550, 60, 5, 20)
ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP;

-- 2. 地块数据（plots）- 如果不存在则插入
INSERT INTO plots (plot_number, name, status) VALUES
(1, '地块1', 1),
(2, '地块2', 1),
(3, '地块3', 1),
(4, '地块4', 1)
ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP;

-- 3. 传感器数据（sensor_data）- 最近24小时的数据
-- 生成从当前时间往前24小时的数据
SET @now = NOW();
SET @start_time = DATE_SUB(@now, INTERVAL 23 HOUR);

-- 删除可能存在的测试数据（可选）
-- DELETE FROM sensor_data WHERE record_time >= @start_time;

-- 插入24小时的传感器数据
INSERT INTO sensor_data (record_time, temperature_c, humidity_pct, soil_moisture_pct, light_lux, is_raining, oxygen_pct, co2_ppm) VALUES
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

-- 插入一些异常数据用于测试
UPDATE sensor_data 
SET temperature_c = 41.5, soil_moisture_pct = 20.0 
WHERE record_time = DATE_SUB(@now, INTERVAL 3 HOUR);

UPDATE sensor_data 
SET soil_moisture_pct = 25.0 
WHERE record_time = DATE_SUB(@now, INTERVAL 8 HOUR);

UPDATE sensor_data 
SET temperature_c = 8.5 
WHERE record_time = DATE_SUB(@now, INTERVAL 15 HOUR);

-- 4. 地块配方分配（plot_assignments）
INSERT INTO plot_assignments (plot_id, recipe_id, assigned_at, is_active) 
SELECT p.id, 'r1', NOW(), 1 FROM plots p WHERE p.plot_number = 1
ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP;

INSERT INTO plot_assignments (plot_id, recipe_id, assigned_at, is_active) 
SELECT p.id, 'r2', NOW(), 1 FROM plots p WHERE p.plot_number = 2
ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP;

INSERT INTO plot_assignments (plot_id, recipe_id, assigned_at, is_active) 
SELECT p.id, 'r1', NOW(), 1 FROM plots p WHERE p.plot_number = 3
ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP;

-- 5. 定时执行计划（plot_schedules）
INSERT INTO plot_schedules (plot_id, recipe_id, schedule_time, executions, is_enabled)
SELECT p.id, 'r1', '08:00', 2, 1 FROM plots p WHERE p.plot_number = 1
ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP;

INSERT INTO plot_schedules (plot_id, recipe_id, schedule_time, executions, is_enabled)
SELECT p.id, 'r1', '14:00', 1, 1 FROM plots p WHERE p.plot_number = 1
ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP;

INSERT INTO plot_schedules (plot_id, recipe_id, schedule_time, executions, is_enabled)
SELECT p.id, 'r2', '09:00', 3, 1 FROM plots p WHERE p.plot_number = 2
ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP;

INSERT INTO plot_schedules (plot_id, recipe_id, schedule_time, executions, is_enabled)
SELECT p.id, 'r3', '10:00', 2, 1 FROM plots p WHERE p.plot_number = 3
ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP;

INSERT INTO plot_schedules (plot_id, recipe_id, schedule_time, executions, is_enabled)
SELECT p.id, 'r1', '16:00', 1, 1 FROM plots p WHERE p.plot_number = 4
ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP;

-- 6. 执行日志（execution_logs）
INSERT INTO execution_logs (plot_id, recipe_id, executions, executed_at, execution_type)
SELECT p.id, 'r1', 2, DATE_SUB(NOW(), INTERVAL 2 HOUR), 'manual' FROM plots p WHERE p.plot_number = 1;

INSERT INTO execution_logs (plot_id, recipe_id, executions, executed_at, execution_type)
SELECT p.id, 'r1', 1, DATE_SUB(NOW(), INTERVAL 1 HOUR), 'scheduled' FROM plots p WHERE p.plot_number = 1;

INSERT INTO execution_logs (plot_id, recipe_id, executions, executed_at, execution_type)
SELECT p.id, 'r2', 3, DATE_SUB(NOW(), INTERVAL 3 HOUR), 'manual' FROM plots p WHERE p.plot_number = 2;

INSERT INTO execution_logs (plot_id, recipe_id, executions, executed_at, execution_type)
SELECT p.id, 'r2', 2, DATE_SUB(NOW(), INTERVAL 5 HOUR), 'scheduled' FROM plots p WHERE p.plot_number = 2;

INSERT INTO execution_logs (plot_id, recipe_id, executions, executed_at, execution_type)
SELECT p.id, 'r1', 1, DATE_SUB(NOW(), INTERVAL 4 HOUR), 'manual' FROM plots p WHERE p.plot_number = 3;

INSERT INTO execution_logs (plot_id, recipe_id, executions, executed_at, execution_type)
SELECT p.id, 'r3', 2, DATE_SUB(NOW(), INTERVAL 6 HOUR), 'scheduled' FROM plots p WHERE p.plot_number = 3;

INSERT INTO execution_logs (plot_id, recipe_id, executions, executed_at, execution_type)
SELECT p.id, 'r1', 1, DATE_SUB(NOW(), INTERVAL 7 HOUR), 'manual' FROM plots p WHERE p.plot_number = 4;

-- 7. 报警记录（alerts）
INSERT INTO alerts (level, message, alert_type, related_data, is_read) VALUES
('warning', '温度异常：温度 41.5°C', 'temperature', '{"temperature": 41.5, "threshold": 35}', 0),
('warning', '土壤湿度异常：湿度 20%', 'soil_moisture', '{"moisture": 20, "threshold": 35}', 0),
('warning', '温度异常：温度 8.5°C', 'temperature', '{"temperature": 8.5, "threshold": 10}', 0),
('info', '自动化：光照(500) 低于阈值，已开启补光灯', 'automation', '{"lightLux": 500, "threshold": 8000}', 0),
('info', '自动化：湿度(25%) 低，执行抽水', 'automation', '{"moisture": 25, "threshold": 35}', 0),
('info', '地块1 分配配方后执行 2 次', 'assignment', '{"plot": 1, "recipe": "r1", "executions": 2}', 1),
('info', '地块2 添加定时 09:00 执行 3 次', 'schedule', '{"plot": 2, "time": "09:00", "executions": 3}', 1);

-- 8. 自动化设置（automation_settings）- 如果不存在则插入
INSERT INTO automation_settings (setting_key, setting_value, description) VALUES
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
ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP;

-- 9. 图片数据（images）- 最近几天的图片数据
INSERT INTO images (image_url, record_time, temperature_c, humidity_pct, soil_moisture_pct, light_lux, is_raining, oxygen_pct, co2_ppm, plot_id, is_abnormal, abnormal_reason) VALUES
('https://example.com/images/plant_001.jpg', DATE_SUB(NOW(), INTERVAL 1 DAY), 25.5, 60.2, 45.2, 12000, 0, 20.5, 400, 1, 0, NULL),
('https://example.com/images/plant_002.jpg', DATE_SUB(NOW(), INTERVAL 1 DAY), 26.2, 59.8, 44.8, 15000, 0, 20.6, 410, 1, 0, NULL),
('https://example.com/images/plant_003.jpg', DATE_SUB(NOW(), INTERVAL 1 DAY), 27.1, 58.5, 43.5, 18000, 0, 20.7, 420, 2, 0, NULL),
('https://example.com/images/plant_004.jpg', DATE_SUB(NOW(), INTERVAL 1 DAY), 28.5, 57.2, 42.2, 22000, 0, 20.8, 430, 2, 0, NULL),
('https://example.com/images/plant_005.jpg', DATE_SUB(NOW(), INTERVAL 1 DAY), 29.2, 56.8, 41.8, 25000, 0, 20.9, 440, 3, 0, NULL),
('https://example.com/images/plant_006.jpg', DATE_SUB(NOW(), INTERVAL 1 DAY), 30.1, 55.5, 40.5, 28000, 0, 21.0, 450, 3, 0, NULL),
('https://example.com/images/plant_007.jpg', DATE_SUB(NOW(), INTERVAL 1 DAY), 28.8, 54.2, 39.2, 26000, 0, 21.1, 460, 4, 0, NULL),
('https://example.com/images/plant_008.jpg', DATE_SUB(NOW(), INTERVAL 1 DAY), 27.5, 53.5, 38.5, 20000, 0, 21.2, 470, 4, 0, NULL),
-- 今天的图片数据
('https://example.com/images/plant_009.jpg', DATE_SUB(NOW(), INTERVAL 3 HOUR), 41.5, 85.0, 20.0, 2000, 0, 15.5, 1200, 1, 1, '温度异常, 湿度异常, 土壤湿度异常, 氧气含量异常, 二氧化碳含量异常'),
('https://example.com/images/plant_010.jpg', DATE_SUB(NOW(), INTERVAL 2 HOUR), 19.5, 48.8, 38.8, 2000, 0, 22.5, 600, 2, 0, NULL),
('https://example.com/images/plant_011.jpg', DATE_SUB(NOW(), INTERVAL 1 HOUR), 18.2, 47.5, 37.5, 500, 1, 22.6, 610, 3, 0, NULL),
('https://example.com/images/plant_012.jpg', NOW(), 17.5, 45.8, 35.8, 0, 0, 22.8, 630, 4, 0, NULL),
-- 异常图片
('https://example.com/images/plant_013.jpg', DATE_SUB(NOW(), INTERVAL 8 HOUR), 29.8, 53.5, 25.0, 30000, 0, 22.0, 550, 1, 1, '土壤湿度异常'),
('https://example.com/images/plant_014.jpg', DATE_SUB(NOW(), INTERVAL 15 HOUR), 8.5, 60.1, 50.1, 500, 0, 21.0, 450, 2, 1, '温度异常');

-- 10. 控制操作日志（control_logs）
INSERT INTO control_logs (control_type, action, status, message) VALUES
('light', 'on', 'success', '植物补光灯已打开'),
('light', 'off', 'success', '植物补光灯已关闭'),
('cleaning', 'start', 'success', '清理搅拌熔炉操作已启动'),
('cleaning', 'stop', 'success', '清理搅拌熔炉操作已完成'),
('light', 'on', 'success', '自动化：光照低于阈值，已开启补光灯'),
('light', 'off', 'success', '自动化：光照恢复，已关闭补光灯');

-- 查询统计信息
SELECT '测试数据插入完成！' AS message;
SELECT COUNT(*) AS sensor_data_count FROM sensor_data;
SELECT COUNT(*) AS recipe_count FROM recipes;
SELECT COUNT(*) AS plot_count FROM plots;
SELECT COUNT(*) AS assignment_count FROM plot_assignments;
SELECT COUNT(*) AS schedule_count FROM plot_schedules;
SELECT COUNT(*) AS execution_log_count FROM execution_logs;
SELECT COUNT(*) AS alert_count FROM alerts;
SELECT COUNT(*) AS image_count FROM images;
SELECT COUNT(*) AS control_log_count FROM control_logs;

