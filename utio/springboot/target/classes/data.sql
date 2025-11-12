-- Spring Boot 自动执行的初始化数据脚本
-- 注意：此文件会在应用启动时自动执行（如果配置了 spring.sql.init.mode=always）
-- 建议使用 test-data.sql 手动执行，避免每次启动都执行

-- 只在表为空时插入基础数据
INSERT INTO recipes (id, name, water_ml, nutrient_ml, rooting_powder_ml, special_ml) 
SELECT 'r1', '基础配方', 500, 50, 0, 0
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE id = 'r1');

INSERT INTO plots (plot_number, name, status) 
SELECT 1, '地块1', 1
WHERE NOT EXISTS (SELECT 1 FROM plots WHERE plot_number = 1);

INSERT INTO plots (plot_number, name, status) 
SELECT 2, '地块2', 1
WHERE NOT EXISTS (SELECT 1 FROM plots WHERE plot_number = 2);

INSERT INTO plots (plot_number, name, status) 
SELECT 3, '地块3', 1
WHERE NOT EXISTS (SELECT 1 FROM plots WHERE plot_number = 3);

INSERT INTO plots (plot_number, name, status) 
SELECT 4, '地块4', 1
WHERE NOT EXISTS (SELECT 1 FROM plots WHERE plot_number = 4);

INSERT INTO automation_settings (setting_key, setting_value, description) 
SELECT 'light_lux_threshold', '8000', '光照强度阈值（低于此值自动开灯）'
WHERE NOT EXISTS (SELECT 1 FROM automation_settings WHERE setting_key = 'light_lux_threshold');

INSERT INTO automation_settings (setting_key, setting_value, description) 
SELECT 'soil_moisture_low_threshold', '35', '土壤湿度低阈值（低于此值自动抽水）'
WHERE NOT EXISTS (SELECT 1 FROM automation_settings WHERE setting_key = 'soil_moisture_low_threshold');

INSERT INTO automation_settings (setting_key, setting_value, description) 
SELECT 'auto_light_enabled', 'true', '自动补光灯开关'
WHERE NOT EXISTS (SELECT 1 FROM automation_settings WHERE setting_key = 'auto_light_enabled');

INSERT INTO automation_settings (setting_key, setting_value, description) 
SELECT 'auto_pump_enabled', 'true', '自动抽水开关'
WHERE NOT EXISTS (SELECT 1 FROM automation_settings WHERE setting_key = 'auto_pump_enabled');

