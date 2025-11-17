-- ============================================================
-- 插入基本图片数据到 images 表
-- 包含正常和异常情况的图片记录
-- ============================================================

USE `greenhouse_db`;

-- 清空现有图片数据（可选，如果需要重新插入）
-- DELETE FROM `images`;

-- 插入图片数据
-- 注意：image_url 使用本地路径格式，符合图片上传接口返回的URL格式
INSERT INTO `images` (
    `image_url`, 
    `record_time`, 
    `temperature_c`, 
    `humidity_pct`, 
    `soil_moisture_pct`, 
    `light_lux`, 
    `is_raining`, 
    `oxygen_pct`, 
    `co2_ppm`, 
    `plot_id`, 
    `is_abnormal`, 
    `abnormal_reason`
) VALUES
-- 正常情况的图片（地块1）
('/api/images/files/sample_001.jpg', DATE_SUB(NOW(), INTERVAL 2 DAY), 25.5, 60.2, 45.2, 12000, 0, 20.5, 400, 1, 0, NULL),
('/api/images/files/sample_002.jpg', DATE_SUB(NOW(), INTERVAL 2 DAY), 26.2, 59.8, 44.8, 15000, 0, 20.6, 410, 1, 0, NULL),
('/api/images/files/sample_003.jpg', DATE_SUB(NOW(), INTERVAL 1 DAY), 24.8, 61.5, 46.5, 10000, 0, 20.4, 390, 1, 0, NULL),
('/api/images/files/sample_004.jpg', DATE_SUB(NOW(), INTERVAL 1 DAY), 25.3, 60.8, 45.8, 13000, 0, 20.7, 405, 1, 0, NULL),

-- 正常情况的图片（地块2）
('/api/images/files/sample_005.jpg', DATE_SUB(NOW(), INTERVAL 2 DAY), 27.1, 58.5, 43.5, 18000, 0, 20.7, 420, 2, 0, NULL),
('/api/images/files/sample_006.jpg', DATE_SUB(NOW(), INTERVAL 1 DAY), 26.5, 59.2, 44.2, 16000, 0, 20.6, 415, 2, 0, NULL),
('/api/images/files/sample_007.jpg', DATE_SUB(NOW(), INTERVAL 12 HOUR), 25.8, 60.0, 45.0, 14000, 0, 20.5, 400, 2, 0, NULL),

-- 正常情况的图片（地块3）
('/api/images/files/sample_008.jpg', DATE_SUB(NOW(), INTERVAL 1 DAY), 28.5, 57.2, 42.2, 22000, 0, 20.8, 430, 3, 0, NULL),
('/api/images/files/sample_009.jpg', DATE_SUB(NOW(), INTERVAL 12 HOUR), 27.8, 58.0, 43.0, 20000, 0, 20.7, 425, 3, 0, NULL),
('/api/images/files/sample_010.jpg', DATE_SUB(NOW(), INTERVAL 6 HOUR), 26.2, 59.5, 44.5, 17000, 0, 20.6, 410, 3, 0, NULL),

-- 正常情况的图片（地块4）
('/api/images/files/sample_011.jpg', DATE_SUB(NOW(), INTERVAL 1 DAY), 29.2, 56.8, 41.8, 25000, 0, 20.9, 440, 4, 0, NULL),
('/api/images/files/sample_012.jpg', DATE_SUB(NOW(), INTERVAL 12 HOUR), 28.5, 57.5, 42.5, 23000, 0, 20.8, 435, 4, 0, NULL),
('/api/images/files/sample_013.jpg', DATE_SUB(NOW(), INTERVAL 6 HOUR), 27.0, 59.0, 44.0, 19000, 0, 20.7, 420, 4, 0, NULL),

-- 异常情况：温度过高（>35°C）
('/api/images/files/sample_014.jpg', DATE_SUB(NOW(), INTERVAL 3 HOUR), 36.5, 75.0, 35.0, 20000, 0, 19.5, 1100, 1, 1, '温度异常'),
('/api/images/files/sample_015.jpg', DATE_SUB(NOW(), INTERVAL 2 HOUR), 38.2, 78.5, 32.5, 22000, 0, 19.2, 1150, 2, 1, '温度异常'),
('/api/images/files/sample_016.jpg', DATE_SUB(NOW(), INTERVAL 1 HOUR), 37.8, 76.8, 33.8, 21000, 0, 19.3, 1120, 3, 1, '温度异常'),

-- 异常情况：土壤湿度过低（<10%）
('/api/images/files/sample_017.jpg', DATE_SUB(NOW(), INTERVAL 4 HOUR), 28.5, 55.0, 8.5, 18000, 0, 21.0, 500, 1, 1, '土壤湿度异常'),
('/api/images/files/sample_018.jpg', DATE_SUB(NOW(), INTERVAL 3 HOUR), 29.2, 54.5, 7.2, 19000, 0, 21.1, 510, 2, 1, '土壤湿度异常'),
('/api/images/files/sample_019.jpg', DATE_SUB(NOW(), INTERVAL 2 HOUR), 27.8, 56.0, 9.0, 17000, 0, 20.9, 490, 3, 1, '土壤湿度异常'),

-- 异常情况：温度过高且土壤湿度过低
('/api/images/files/sample_020.jpg', DATE_SUB(NOW(), INTERVAL 5 HOUR), 40.5, 82.0, 5.5, 25000, 0, 18.5, 1200, 1, 1, '温度异常, 土壤湿度异常'),
('/api/images/files/sample_021.jpg', DATE_SUB(NOW(), INTERVAL 4 HOUR), 39.8, 80.5, 6.8, 24000, 0, 18.7, 1180, 2, 1, '温度异常, 土壤湿度异常'),

-- 下雨情况的图片
('/api/images/files/sample_022.jpg', DATE_SUB(NOW(), INTERVAL 6 HOUR), 22.5, 75.0, 50.0, 5000, 1, 21.5, 450, 1, 0, NULL),
('/api/images/files/sample_023.jpg', DATE_SUB(NOW(), INTERVAL 5 HOUR), 21.8, 78.5, 52.5, 3000, 1, 21.6, 460, 2, 0, NULL),
('/api/images/files/sample_024.jpg', DATE_SUB(NOW(), INTERVAL 4 HOUR), 20.5, 80.0, 55.0, 2000, 1, 21.8, 480, 3, 0, NULL),

-- 低光照情况的图片
('/api/images/files/sample_025.jpg', DATE_SUB(NOW(), INTERVAL 8 HOUR), 19.5, 48.8, 38.8, 2000, 0, 22.5, 600, 1, 0, NULL),
('/api/images/files/sample_026.jpg', DATE_SUB(NOW(), INTERVAL 7 HOUR), 18.2, 47.5, 37.5, 500, 1, 22.6, 610, 2, 0, NULL),
('/api/images/files/sample_027.jpg', DATE_SUB(NOW(), INTERVAL 6 HOUR), 17.5, 45.8, 35.8, 0, 0, 22.8, 630, 3, 0, NULL),

-- 高二氧化碳情况的图片
('/api/images/files/sample_028.jpg', DATE_SUB(NOW(), INTERVAL 10 HOUR), 30.5, 65.0, 40.0, 15000, 0, 19.0, 950, 1, 0, NULL),
('/api/images/files/sample_029.jpg', DATE_SUB(NOW(), INTERVAL 9 HOUR), 31.2, 66.5, 41.5, 16000, 0, 18.8, 980, 2, 0, NULL),

-- 低氧气情况的图片
('/api/images/files/sample_030.jpg', DATE_SUB(NOW(), INTERVAL 12 HOUR), 28.0, 58.0, 42.0, 18000, 0, 17.5, 550, 1, 0, NULL),
('/api/images/files/sample_031.jpg', DATE_SUB(NOW(), INTERVAL 11 HOUR), 27.5, 57.5, 41.5, 17000, 0, 17.8, 560, 2, 0, NULL),

-- 今天的图片（正常情况）
('/api/images/files/sample_032.jpg', DATE_SUB(NOW(), INTERVAL 3 HOUR), 25.0, 60.0, 45.0, 12000, 0, 20.5, 400, 1, 0, NULL),
('/api/images/files/sample_033.jpg', DATE_SUB(NOW(), INTERVAL 2 HOUR), 24.5, 61.0, 46.0, 11000, 0, 20.6, 410, 2, 0, NULL),
('/api/images/files/sample_034.jpg', DATE_SUB(NOW(), INTERVAL 1 HOUR), 26.0, 59.5, 44.5, 13000, 0, 20.4, 390, 3, 0, NULL),
('/api/images/files/sample_035.jpg', NOW(), 25.5, 60.5, 45.5, 12500, 0, 20.5, 405, 4, 0, NULL);

-- 查询插入结果
SELECT 
    COUNT(*) AS total_images,
    SUM(CASE WHEN is_abnormal = 1 THEN 1 ELSE 0 END) AS abnormal_images,
    SUM(CASE WHEN is_abnormal = 0 THEN 1 ELSE 0 END) AS normal_images
FROM `images`;

-- 显示最近插入的图片
SELECT 
    id,
    image_url,
    record_time,
    temperature_c,
    humidity_pct,
    soil_moisture_pct,
    light_lux,
    is_raining,
    oxygen_pct,
    co2_ppm,
    plot_id,
    is_abnormal,
    abnormal_reason,
    created_at
FROM `images`
ORDER BY created_at DESC
LIMIT 10;

