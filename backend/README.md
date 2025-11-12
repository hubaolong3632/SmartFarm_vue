# 后端项目 - 智能温室管理系统 API

基于 Spring Boot 3.1.5 + MyBatis-Plus 的后端 API 服务。

## 技术栈

- **Spring Boot 3.1.5** - Java 应用框架
- **MyBatis-Plus 3.5.7** - ORM 框架
- **MySQL 8.0** - 数据库
- **MQTT (Eclipse Paho)** - MQTT 客户端
- **Maven** - 构建工具

## 环境要求

- JDK 17+
- Maven 3.6+
- MySQL 8.0+

## 快速开始

### 1. 数据库配置

1. 创建数据库：
```sql
CREATE DATABASE greenhouse_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. 执行数据库脚本：
```bash
mysql -u root -p greenhouse_db < database/schema.sql
```

3. 导入测试数据（可选）：
```bash
mysql -u root -p greenhouse_db < src/main/resources/test-data.sql
```

### 2. 配置文件

修改 `src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/greenhouse_db?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: 123456  # 修改为你的数据库密码
```

### 3. 运行项目

```bash
mvn clean install
mvn spring-boot:run
```

API 服务运行在 `http://localhost:11000/api`

## 项目结构

```
backend/
├── src/main/java/com/greenhouse/
│   ├── common/           # 公共类
│   │   ├── Result.java              # 统一响应结果
│   │   └── GlobalExceptionHandler.java  # 全局异常处理
│   ├── config/           # 配置类
│   │   ├── MyBatisPlusConfig.java   # MyBatis-Plus 配置
│   │   └── EmqxConfig.java          # EMQX MQTT 配置
│   ├── controller/      # 控制器
│   │   ├── SensorDataController.java    # 传感器数据
│   │   ├── RecipeController.java        # 配方管理
│   │   ├── PlotController.java          # 地块管理
│   │   ├── ControlController.java       # 控制操作
│   │   ├── AlertController.java         # 报警记录
│   │   ├── AutomationController.java    # 自动化设置
│   │   ├── ExecutionLogController.java  # 执行日志
│   │   ├── ImageController.java         # 图片管理
│   │   └── MqttController.java          # MQTT 管理
│   ├── entity/          # 实体类
│   ├── dto/             # 数据传输对象
│   ├── mapper/          # MyBatis Mapper
│   ├── service/         # 业务服务
│   └── GreenhouseApplication.java  # 启动类
├── src/main/resources/
│   ├── application.yml  # 应用配置
│   ├── data.sql         # 初始化数据
│   └── test-data.sql    # 测试数据
├── database/
│   └── schema.sql       # 数据库表结构
└── pom.xml              # Maven 配置
```

## API 接口

### 传感器数据
- `GET /sensor-data/latest` - 获取最新数据
- `GET /sensor-data/last-24-hours` - 获取最近24小时数据
- `POST /sensor-data` - 创建传感器数据

### 配方管理
- `GET /recipes` - 获取所有配方
- `POST /recipes` - 创建配方
- `PUT /recipes` - 更新配方
- `DELETE /recipes` - 删除配方

### 地块管理
- `GET /plots` - 获取所有地块
- `POST /plots/assign` - 分配配方到地块
- `GET /plots/assignments` - 获取所有分配
- `POST /plots/schedules` - 添加定时计划
- `DELETE /plots/schedules` - 删除定时计划

### 控制操作
- `POST /control/cleaning` - 清理搅拌熔炉
- `POST /control/light?action=1` - 开关补光灯（1=开，0=关）

### 报警记录
- `GET /alerts` - 获取所有报警
- `GET /alerts/unread` - 获取未读报警

### 自动化设置
- `GET /automation` - 获取所有设置
- `PUT /automation` - 更新设置

### 执行日志
- `GET /execution-logs` - 获取所有日志
- `GET /execution-logs/last-24-hours` - 获取最近24小时统计

### 图片管理
- `GET /images/date?date=2024-01-01` - 按日期查询图片
- `POST /images` - 创建图片记录

### MQTT
- `GET /mqtt/messages` - 获取所有消息
- `GET /mqtt/status` - 获取连接状态
- `POST /mqtt/publish` - 发布消息

## 跨域配置

所有控制器已添加 `@CrossOrigin("*")` 注解，允许跨域访问。

## 数据库表

- `sensor_data` - 传感器数据
- `recipes` - 配方表
- `plots` - 地块表
- `plot_assignments` - 地块分配
- `plot_schedules` - 定时计划
- `execution_logs` - 执行日志
- `alerts` - 报警记录
- `automation_settings` - 自动化设置
- `images` - 图片记录
- `control_logs` - 控制日志

## 更多文档

- `启动说明.md` - 详细启动步骤
- `README-MYBATIS-PLUS.md` - MyBatis-Plus 使用说明
- `EMQX使用说明.md` - MQTT 配置说明
