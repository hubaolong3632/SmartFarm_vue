# 测试数据说明

## 测试数据文件

### 1. test-data.sql
完整的测试数据脚本，包含所有表的测试数据。

**使用方法：**
```bash
# 方式1：使用 MySQL 命令行
mysql -u root -p greenhouse_db < src/main/resources/test-data.sql

# 方式2：在 MySQL 客户端中执行
source src/main/resources/test-data.sql;
```

**包含的测试数据：**
- ✅ 4个配方（基础配方、营养强化、生根专用、特殊营养）
- ✅ 4个地块（1-4号地块）
- ✅ 24小时的传感器数据（包含正常和异常数据）
- ✅ 3个地块的配方分配
- ✅ 5个定时执行计划
- ✅ 7条执行日志
- ✅ 7条报警记录（包含未读和已读）
- ✅ 自动化设置（4项配置）
- ✅ 14张图片数据（包含正常和异常图片）
- ✅ 6条控制操作日志

### 2. data.sql
Spring Boot 自动执行的初始化脚本（仅基础数据）。

**注意：** 此文件会在应用启动时自动执行（如果配置了 `spring.sql.init.mode=always`），建议使用 `test-data.sql` 手动执行。

## 测试数据特点

### 传感器数据
- 包含最近24小时的完整数据
- 模拟一天的温度、湿度、光照变化
- 包含3条异常数据用于测试：
  - 高温异常（41.5°C）
  - 低温异常（8.5°C）
  - 低湿度异常（20%、25%）

### 图片数据
- 包含最近1-2天的图片
- 包含正常图片和异常图片
- 异常图片会自动标记 `is_abnormal = 1`

### 执行日志
- 包含手动执行和定时执行的记录
- 分布在不同的地块和时间

### 报警记录
- 包含不同级别的报警（info、warning）
- 包含已读和未读的报警
- 包含温度、湿度、自动化等不同类型的报警

## 数据验证

执行测试数据后，可以运行以下 SQL 查询验证：

```sql
-- 查看传感器数据统计
SELECT COUNT(*) as total, 
       MIN(record_time) as earliest, 
       MAX(record_time) as latest 
FROM sensor_data;

-- 查看异常传感器数据
SELECT * FROM sensor_data 
WHERE temperature_c < 10 OR temperature_c > 35 OR soil_moisture_pct < 35;

-- 查看未读报警
SELECT * FROM alerts WHERE is_read = 0;

-- 查看异常图片
SELECT * FROM images WHERE is_abnormal = 1;

-- 查看执行日志统计
SELECT plot_id, COUNT(*) as count, SUM(executions) as total_executions 
FROM execution_logs 
GROUP BY plot_id;
```

## 清理测试数据

如果需要清理测试数据，可以执行：

```sql
-- 清理所有测试数据（谨慎操作！）
DELETE FROM control_logs;
DELETE FROM images;
DELETE FROM execution_logs;
DELETE FROM alerts;
DELETE FROM plot_schedules;
DELETE FROM plot_assignments;
DELETE FROM sensor_data;
-- 注意：recipes 和 plots 的基础数据建议保留
```

## 注意事项

1. **时间相关数据**：测试数据使用 `NOW()` 和相对时间，确保数据始终是"最近"的
2. **外键约束**：确保先执行 `schema.sql` 创建表结构
3. **重复执行**：使用 `ON DUPLICATE KEY UPDATE` 避免重复插入错误
4. **异常数据**：测试数据中包含异常数据，用于测试报警和图片异常标记功能

## 扩展测试数据

如果需要更多测试数据，可以：

1. 修改 `test-data.sql` 中的数量和时间范围
2. 使用循环生成更多数据：

```sql
-- 示例：生成更多传感器数据
SET @i = 0;
WHILE @i < 100 DO
    INSERT INTO sensor_data (record_time, temperature_c, soil_moisture_pct, light_lux, is_raining)
    VALUES (DATE_SUB(NOW(), INTERVAL @i HOUR), 
            20 + RAND() * 10, 
            40 + RAND() * 20, 
            RAND() * 35000, 
            IF(RAND() < 0.1, 1, 0));
    SET @i = @i + 1;
END WHILE;
```

