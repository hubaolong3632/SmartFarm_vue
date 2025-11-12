# 数据库代码说明

本目录包含智能温室管理系统的数据库相关代码。

## 目录结构

```
utio/
├── schema.sql              # 数据库表结构 SQL 脚本
├── db.config.js            # 数据库连接配置文件
├── models/                 # 数据模型目录
│   ├── index.js            # 模型统一导出
│   ├── SensorData.js       # 传感器数据模型
│   ├── Recipe.js           # 配方模型
│   ├── Plot.js             # 地块模型
│   ├── PlotAssignment.js   # 地块配方分配模型
│   ├── PlotSchedule.js     # 地块定时执行计划模型
│   ├── ExecutionLog.js     # 执行日志模型
│   ├── Alert.js            # 报警记录模型
│   ├── AutomationSetting.js # 自动化设置模型
│   ├── Image.js            # 图片模型
│   └── ControlLog.js       # 控制操作日志模型
└── services/               # 数据库服务层目录
    ├── DatabaseService.js  # 数据库服务基类
    ├── SensorDataService.js # 传感器数据服务
    └── RecipeService.js    # 配方服务
```

## 数据库表说明

### 1. sensor_data - 传感器数据表
存储每小时记录的传感器数据（温度、土壤湿度、光照、降雨）。

### 2. recipes - 配方表
存储植物配方的配置信息（水、营养液、生根粉、特殊营养的毫升数）。

### 3. plots - 地块表
存储地块基本信息（编号、名称、状态）。

### 4. plot_assignments - 地块配方分配表
记录地块与配方的当前分配关系。

### 5. plot_schedules - 地块定时执行计划表
存储每个地块的定时执行计划（时间、配方、执行次数）。

### 6. execution_logs - 执行日志表
记录所有配方执行的历史记录。

### 7. alerts - 报警记录表
存储系统产生的所有报警信息。

### 8. automation_settings - 自动化设置表
存储自动化控制的配置参数。

### 9. images - 图片表
存储植物图片及其关联的传感器数据。

### 10. control_logs - 控制操作日志表
记录所有控制操作（如开灯、清理等）的日志。

## 使用方法

### 1. 初始化数据库

执行 `schema.sql` 文件创建所有表：

```bash
mysql -u root -p greenhouse_db < utio/schema.sql
```

### 2. 配置数据库连接

修改 `db.config.js` 中的数据库连接信息：

```javascript
const dbConfig = {
  development: {
    host: 'localhost',
    port: 3306,
    user: 'your_username',
    password: 'your_password',
    database: 'greenhouse_db',
    // ...
  }
}
```

### 3. 使用模型

```javascript
const { SensorData, Recipe } = require('./utio/models')

// 创建模型实例
const sensorData = new SensorData({
  recordTime: new Date(),
  temperatureC: 25.5,
  soilMoisturePct: 45.2,
  lightLux: 12000,
  isRaining: 0
})

// 转换为数据库格式
const dbData = sensorData.toDbFormat()

// 转换为前端格式
const frontendData = sensorData.toFrontendFormat()

// 验证数据
const errors = sensorData.validate()
```

### 4. 使用服务层

```javascript
const SensorDataService = require('./utio/services/SensorDataService')
const dbService = require('./your-db-driver') // 需要实现 DatabaseService

const sensorService = new SensorDataService(dbService)

// 创建传感器数据
const data = await sensorService.create({
  recordTime: new Date(),
  temperatureC: 25.5,
  soilMoisturePct: 45.2,
  lightLux: 12000,
  isRaining: 0
})

// 查询最近24小时数据
const last24Hours = await sensorService.findLast24Hours()
```

## 注意事项

1. **数据库驱动**: `DatabaseService` 是基类，需要根据实际使用的数据库驱动（如 mysql2、sequelize 等）进行实现。

2. **字段命名**: 数据库使用下划线命名（snake_case），模型使用驼峰命名（camelCase），模型提供了 `toDbFormat()` 和 `toFrontendFormat()` 方法进行转换。

3. **数据验证**: 所有模型都提供了 `validate()` 方法，在保存前应进行验证。

4. **事务支持**: 服务层支持事务操作，复杂操作建议使用事务保证数据一致性。

5. **索引优化**: SQL 脚本中已为常用查询字段添加了索引，可根据实际查询需求调整。

## 扩展开发

如需添加新的表或服务：

1. 在 `schema.sql` 中添加表结构
2. 在 `models/` 目录下创建对应的模型文件
3. 在 `models/index.js` 中导出新模型
4. 在 `services/` 目录下创建对应的服务文件（可选）

## 环境变量

生产环境可通过环境变量配置数据库连接：

- `DB_HOST` - 数据库主机
- `DB_PORT` - 数据库端口
- `DB_USER` - 数据库用户名
- `DB_PASSWORD` - 数据库密码
- `DB_NAME` - 数据库名称

