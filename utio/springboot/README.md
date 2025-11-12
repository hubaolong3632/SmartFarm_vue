# Spring Boot 后端 API 文档

智能温室管理系统后端 API，基于 Spring Boot 3.2.0 开发。

## 项目结构

```
springboot/
├── pom.xml                                    # Maven 依赖配置
├── src/main/
│   ├── java/com/greenhouse/
│   │   ├── GreenhouseApplication.java        # 主启动类
│   │   ├── common/                           # 通用类
│   │   │   ├── Result.java                   # 统一响应封装
│   │   │   └── GlobalExceptionHandler.java   # 全局异常处理
│   │   ├── entity/                           # 实体类（对应数据库表）
│   │   │   ├── SensorData.java
│   │   │   ├── Recipe.java
│   │   │   ├── Plot.java
│   │   │   ├── PlotAssignment.java
│   │   │   ├── PlotSchedule.java
│   │   │   ├── ExecutionLog.java
│   │   │   ├── Alert.java
│   │   │   ├── AutomationSetting.java
│   │   │   ├── Image.java
│   │   │   └── ControlLog.java
│   │   ├── repository/                       # 数据访问层
│   │   │   ├── SensorDataRepository.java
│   │   │   ├── RecipeRepository.java
│   │   │   └── ...
│   │   ├── service/                          # 业务逻辑层
│   │   │   ├── SensorDataService.java
│   │   │   └── RecipeService.java
│   │   ├── controller/                       # 控制器层（REST API）
│   │   │   ├── SensorDataController.java
│   │   │   ├── RecipeController.java
│   │   │   ├── PlotController.java
│   │   │   ├── ExecutionLogController.java
│   │   │   ├── AlertController.java
│   │   │   ├── AutomationController.java
│   │   │   ├── ImageController.java
│   │   │   └── ControlController.java
│   │   └── dto/                              # 数据传输对象
│   │       ├── SensorDataDTO.java
│   │       ├── RecipeDTO.java
│   │       └── ...
│   └── resources/
│       └── application.yml                   # 配置文件
└── README.md
```

## 环境要求

- JDK 17+
- Maven 3.6+
- MySQL 8.0+

## 配置说明

修改 `src/main/resources/application.yml` 中的数据库连接信息：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/greenhouse_db?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: your_password
```

## 运行项目

```bash
# 编译打包
mvn clean package

# 运行
mvn spring-boot:run

# 或运行 jar 包
java -jar target/greenhouse-api-1.0.0.jar
```

服务启动后访问：http://localhost:8080/api

## API 接口文档

### 1. 传感器数据接口

#### 创建传感器数据
```
POST /api/sensor-data
Content-Type: application/json

{
  "recordTime": "2024-01-01T12:00:00",
  "temperatureC": 25.5,
  "soilMoisturePct": 45.2,
  "lightLux": 12000,
  "isRaining": false
}
```

#### 获取最新数据
```
GET /api/sensor-data/latest
```

#### 获取最近24小时数据
```
GET /api/sensor-data/last-24-hours
```

#### 获取指定时间范围数据
```
GET /api/sensor-data/range?startTime=2024-01-01 00:00:00&endTime=2024-01-01 23:59:59
```

### 2. 配方管理接口

#### 创建配方
```
POST /api/recipes
Content-Type: application/json

{
  "id": "r1",
  "name": "基础配方",
  "waterMl": 500,
  "nutrientMl": 50,
  "rootingPowderMl": 0,
  "specialMl": 0
}
```

#### 获取所有配方
```
GET /api/recipes
```

#### 获取单个配方
```
GET /api/recipes/{id}
```

#### 更新配方
```
PUT /api/recipes/{id}
Content-Type: application/json

{
  "id": "r1",
  "name": "基础配方",
  "waterMl": 600,
  "nutrientMl": 60,
  "rootingPowderMl": 0,
  "specialMl": 0
}
```

#### 删除配方
```
DELETE /api/recipes/{id}
```

### 3. 地块管理接口

#### 获取所有地块
```
GET /api/plots
```

#### 分配配方到地块
```
POST /api/plots/{plotId}/assign
Content-Type: application/json

{
  "recipeId": "r1",
  "executions": 3
}
```

#### 获取地块的当前分配
```
GET /api/plots/{plotId}/assignment
```

#### 添加定时执行计划
```
POST /api/plots/{plotId}/schedules
Content-Type: application/json

{
  "recipeId": "r1",
  "timeHHmm": "08:00",
  "executions": 2
}
```

#### 获取地块的定时计划列表
```
GET /api/plots/{plotId}/schedules
```

#### 删除定时计划
```
DELETE /api/plots/schedules/{scheduleId}
```

#### 获取所有地块的分配情况
```
GET /api/plots/assignments
```

### 4. 执行日志接口

#### 获取所有执行日志
```
GET /api/execution-logs
```

#### 获取指定地块的执行日志
```
GET /api/execution-logs/plot/{plotId}
```

#### 获取最近24小时的执行统计
```
GET /api/execution-logs/last-24-hours
```

### 5. 报警接口

#### 获取所有报警
```
GET /api/alerts
```

#### 获取未读报警
```
GET /api/alerts/unread
```

#### 获取指定级别的报警
```
GET /api/alerts/level/{level}
```

#### 标记为已读
```
PUT /api/alerts/{id}/read
```

### 6. 自动化设置接口

#### 获取所有自动化设置
```
GET /api/automation
```

#### 更新自动化设置
```
PUT /api/automation
Content-Type: application/json

{
  "lightLuxThreshold": 8000,
  "soilMoistureLowThreshold": 35,
  "autoLightEnabled": true,
  "autoPumpEnabled": true
}
```

### 7. 图片管理接口

#### 创建图片记录
```
POST /api/images
Content-Type: application/json

{
  "url": "https://example.com/image.jpg",
  "recordTime": "2024-01-01T12:00:00",
  "temperatureC": 25.5,
  "soilMoisturePct": 45.2,
  "lightLux": 12000,
  "plotId": 1
}
```

#### 根据日期查询图片
```
GET /api/images/date/2024-01-01
```

#### 获取异常图片
```
GET /api/images/abnormal
```

### 8. 控制操作接口

#### 清理搅拌熔炉
```
POST /api/control/cleaning
```

#### 打开/关闭植物补光灯
```
POST /api/control/light/on
POST /api/control/light/off
```

#### 获取控制日志
```
GET /api/control/logs
```

## 统一响应格式

所有接口返回统一的响应格式：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {},
  "timestamp": 1704067200000
}
```

- `code`: 响应码，200表示成功，其他表示失败
- `message`: 响应消息
- `data`: 响应数据
- `timestamp`: 时间戳

## 错误处理

当发生错误时，返回格式：

```json
{
  "code": 400,
  "message": "错误信息",
  "data": null,
  "timestamp": 1704067200000
}
```

## 注意事项

1. 所有时间格式使用 ISO 8601 格式（如：2024-01-01T12:00:00）
2. 日期查询使用格式：yyyy-MM-dd
3. 时间范围查询使用格式：yyyy-MM-dd HH:mm:ss
4. 所有接口都需要在请求头中设置 `Content-Type: application/json`（POST/PUT 请求）
5. 项目使用 JPA 进行数据持久化，确保数据库表已创建（执行 `utio/schema.sql`）

## 扩展开发

如需添加新功能：

1. 在 `entity` 包下创建实体类
2. 在 `repository` 包下创建 Repository 接口
3. 在 `service` 包下创建 Service 类（可选）
4. 在 `controller` 包下创建 Controller 类
5. 在 `dto` 包下创建 DTO 类（用于接收请求参数）

