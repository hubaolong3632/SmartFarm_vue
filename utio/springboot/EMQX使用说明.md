# EMQX MQTT 集成使用说明

## 功能说明

项目已集成 EMQX MQTT 客户端，可以：
- 自动连接到 EMQX 服务器
- 订阅 `text1` 主题并接收消息
- 通过 REST API 查看接收到的消息
- 发布消息到指定主题

## 配置说明

在 `application.yml` 中配置 EMQX 连接信息：

```yaml
emqx:
  broker-url: tcp://localhost:1883  # EMQX 服务器地址
  client-id: greenhouse-client-${random.value}  # 客户端ID
  username:  # MQTT 用户名（可选，如果 EMQX 启用了认证）
  password:  # MQTT 密码（可选）
  topic: text1  # 订阅的主题
  qos: 1  # 服务质量等级 (0, 1, 2)
  connection-timeout: 30  # 连接超时时间（秒）
  keep-alive-interval: 60  # 心跳间隔（秒）
  automatic-reconnect: true  # 自动重连
```

### 配置参数说明

- **broker-url**: EMQX 服务器地址，格式为 `tcp://host:port`
  - 默认端口：1883（非加密）
  - SSL/TLS 端口：8883（使用 `ssl://` 前缀）
  
- **client-id**: MQTT 客户端ID，必须唯一
  - 使用 `${random.value}` 可以自动生成随机ID，避免冲突

- **username/password**: 如果 EMQX 启用了认证，需要填写用户名和密码

- **topic**: 要订阅的主题名称，默认为 `text1`

- **qos**: 服务质量等级
  - 0: 最多一次（可能丢失消息）
  - 1: 至少一次（可能重复消息）
  - 2: 仅一次（保证消息不丢失且不重复）

## API 接口

### 1. 获取所有消息

```
GET /api/mqtt/messages
```

**响应示例：**
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "topic": "text1",
      "message": "Hello MQTT",
      "qos": 1,
      "timestamp": 1704067200000
    }
  ]
}
```

### 2. 获取最新的 N 条消息

```
GET /api/mqtt/messages/latest?count=10
```

**参数：**
- `count`: 要获取的消息数量（默认 10）

### 3. 获取连接状态

```
GET /api/mqtt/status
```

**响应示例：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "connected": true,
    "messageCount": 5
  }
}
```

### 4. 清空所有消息

```
DELETE /api/mqtt/messages
```

### 5. 发布消息

```
POST /api/mqtt/publish?topic=text1&message=Hello&qos=1
```

**参数：**
- `topic`: 主题名称（必填）
- `message`: 消息内容（必填）
- `qos`: 服务质量等级（可选，默认 1）

## 使用示例

### 1. 启动 EMQX

确保 EMQX 服务器已启动并运行在 `localhost:1883`

### 2. 启动 Spring Boot 应用

应用启动后会自动：
- 连接到 EMQX
- 订阅 `text1` 主题
- 开始接收消息

### 3. 测试消息接收

使用 MQTT 客户端工具（如 MQTT.fx、mosquitto_pub）发布消息：

```bash
# 使用 mosquitto_pub 发布消息
mosquitto_pub -h localhost -p 1883 -t text1 -m "Hello from MQTT"
```

### 4. 查看接收到的消息

```bash
# 获取所有消息
curl http://localhost:11000/api/mqtt/messages

# 获取最新 10 条消息
curl http://localhost:11000/api/mqtt/messages/latest?count=10

# 查看连接状态
curl http://localhost:11000/api/mqtt/status
```

## 代码结构

```
com.greenhouse
├── config
│   └── EmqxConfig.java          # EMQX 配置类
├── service
│   └── MqttService.java         # MQTT 服务类（连接、订阅、接收消息）
└── controller
    └── MqttController.java      # MQTT REST API 控制器
```

## 注意事项

1. **消息存储**: 接收到的消息存储在内存中，最多保存 1000 条。应用重启后消息会丢失。

2. **自动重连**: 如果连接断开，客户端会自动尝试重连。

3. **线程安全**: 消息列表使用 `CopyOnWriteArrayList`，保证线程安全。

4. **日志**: MQTT 相关日志会输出到控制台，可以通过日志查看连接状态和接收到的消息。

## 故障排查

### 连接失败

1. 检查 EMQX 服务器是否启动
2. 检查 `broker-url` 配置是否正确
3. 检查网络连接
4. 查看应用日志中的错误信息

### 收不到消息

1. 检查主题名称是否正确
2. 确认消息已成功发布到 EMQX
3. 检查 QoS 设置
4. 查看应用日志确认是否订阅成功

### 认证失败

如果 EMQX 启用了认证，需要在 `application.yml` 中配置正确的用户名和密码。

## 扩展功能

如果需要将接收到的消息保存到数据库，可以在 `MqttService.messageArrived()` 方法中添加数据库保存逻辑。

