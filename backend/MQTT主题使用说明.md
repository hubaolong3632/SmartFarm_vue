# MQTT 主题使用说明

## 概述

系统使用 MQTT 协议进行设备通信，目前共有 **3个MQTT主题**，分别用于不同的功能场景。

## MQTT 配置

- **MQTT Broker**: `tcp://mqtt.00000.work:1883` (可在 `application.yml` 中配置)
- **客户端ID**: `springboot-3` (可在 `application.yml` 中配置)
- **QoS**: 1 (至少一次传递)

---

## 主题列表

### 1. `text1` - 传感器数据接收（订阅）

**用途**: 接收传感器设备发送的环境数据

**方向**: 设备 → 系统（订阅）

**消息格式**:

```json
{
  "recordTime": "2024-11-12 14:30:00",
  "temperatureC": 25.5,
  "humidityPct": 60.5,
  "soilMoisturePct": 45.2,
  "lightLux": 12000,
  "isRaining": false,
  "oxygenPct": 20.5,
  "co2Ppm": 400
}
```

**字段说明**:

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| `recordTime` | String | 否 | 记录时间，格式：`yyyy-MM-dd HH:mm:ss`。如果不提供，使用服务器当前时间 |
| `temperatureC` | Number | 否* | 温度（摄氏度） |
| `humidityPct` | Number | 否* | 湿度（百分比，0-100） |
| `soilMoisturePct` | Number | 否* | 土壤湿度（百分比，0-100） |
| `lightLux` | Integer | 否* | 光照强度（lux） |
| `isRaining` | Boolean | 否 | 是否下雨（true/false） |
| `oxygenPct` | Number | 否* | 氧气含量（百分比，0-100） |
| `co2Ppm` | Integer | 否* | 二氧化碳含量（ppm，0-10000） |

**注意**: `temperatureC`、`humidityPct`、`soilMoisturePct`、`lightLux`、`oxygenPct`、`co2Ppm` 至少需要提供一个，否则消息会被忽略。

**系统行为**:
- 系统自动订阅此主题
- 接收到的数据会自动解析并保存到 `sensor_data` 表
- 前端可通过 API 获取数据：`GET /api/sensor-data/latest`

**测试方法**:

```bash
# 使用 mosquitto_pub 工具
mosquitto_pub -h mqtt.00000.work -p 1883 -t text1 -m '{"temperatureC":25.5,"soilMoisturePct":45.2,"lightLux":12000,"isRaining":false}'
```

---

### 2. `time` - 配方执行指令（发布）

**用途**: 发送配方执行指令到设备，用于定时任务或立即执行

**方向**: 系统 → 设备（发布）

**触发场景**:
1. 定时任务自动执行
2. 用户点击"立即执行"按钮
3. 用户点击"重新执行"按钮

**消息格式**:

```json
{
  "plotId": 1,
  "plotName": "地块1",
  "recipeId": "recipe_001",
  "recipeName": "标准配方",
  "waterMl": 500,
  "nutrientMl": 100,
  "rootingPowderMl": 50,
  "specialMl": 20,
  "executions": 1,
  "executeTime": 1702454400000
}
```

**字段说明**:

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| `plotId` | Integer | 是 | 地块ID |
| `plotName` | String | 是 | 地块名称 |
| `recipeId` | String | 是 | 配方ID |
| `recipeName` | String | 是 | 配方名称 |
| `waterMl` | Integer | 是 | 水量（毫升） |
| `nutrientMl` | Integer | 是 | 营养液量（毫升） |
| `rootingPowderMl` | Integer | 是 | 生根粉量（毫升） |
| `specialMl` | Integer | 是 | 特殊液体量（毫升） |
| `executions` | Integer | 是 | 执行次数 |
| `executeTime` | Long | 是 | 执行时间戳（毫秒） |

**系统行为**:
- 系统在以下情况会发布消息到此主题：
  - 定时任务触发时
  - 用户手动执行配方时
- 消息发布后，系统会记录执行日志到 `execution_logs` 表

**使用示例**:

系统会自动在以下场景发布消息：
- 土壤分配页面：点击"立即执行"按钮
- 土壤分配页面：点击"重新执行"按钮
- 定时任务：到达预定时间自动执行

---

### 3. `voluntarily` - AI自动执行建议（发布）

**用途**: 发送AI自动执行建议的操作指令到设备

**方向**: 系统 → 设备（发布）

**触发场景**:
- 用户在"AI自动化分析"页面点击"执行"按钮，执行AI建议的操作

**消息格式**:

#### 3.1 补光灯控制

```json
{
  "type": "light",
  "action": "on",
  "reason": "光照不足，建议开启补光灯",
  "executeTime": 1702454400000,
  "source": "ai_auto_execution"
}
```

#### 3.2 抽水操作

```json
{
  "type": "pump",
  "action": "on",
  "reason": "土壤湿度过低，建议启动抽水系统",
  "executeTime": 1702454400000,
  "source": "ai_auto_execution"
}
```

#### 3.3 配方执行

```json
{
  "type": "recipe",
  "plotId": 1,
  "recipeId": "recipe_001",
  "executions": 1,
  "reason": "根据当前环境数据，建议执行此配方",
  "executeTime": 1702454400000,
  "source": "ai_auto_execution"
}
```

**字段说明**:

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| `type` | String | 是 | 操作类型：`light`（补光灯）、`pump`（抽水）、`recipe`（配方） |
| `action` | String | 是* | 动作：`on`（开启）或 `off`（关闭）。仅当 `type` 为 `light` 或 `pump` 时必填 |
| `plotId` | Integer | 是* | 地块ID。仅当 `type` 为 `recipe` 时必填 |
| `recipeId` | String | 是* | 配方ID。仅当 `type` 为 `recipe` 时必填 |
| `executions` | Integer | 是* | 执行次数。仅当 `type` 为 `recipe` 时必填 |
| `reason` | String | 是 | AI建议的原因说明 |
| `executeTime` | Long | 是 | 执行时间戳（毫秒） |
| `source` | String | 是 | 消息来源，固定为 `ai_auto_execution` |

**系统行为**:
- 用户在"AI自动化分析"页面获取AI自动执行建议
- 用户点击建议列表中的"执行"按钮
- 系统将操作指令发布到 `voluntarily` 主题
- 设备订阅此主题并执行相应操作

**使用示例**:

1. 打开"AI自动化分析"页面
2. 点击"获取执行建议"按钮
3. AI会分析当前环境数据并生成建议
4. 在建议列表中点击"执行"按钮
5. 系统会将操作指令发送到 `voluntarily` 主题

---

## 主题对比总结

| 主题名 | 方向 | 用途 | 消息来源 |
|--------|------|------|----------|
| `text1` | 设备 → 系统 | 接收传感器数据 | 传感器设备 |
| `time` | 系统 → 设备 | 发送配方执行指令 | 定时任务/手动执行 |
| `voluntarily` | 系统 → 设备 | 发送AI自动执行建议 | AI分析建议 |

---

## 设备端订阅建议

### 订阅 `time` 主题

设备应订阅 `time` 主题以接收配方执行指令：

```python
# Python 示例
import paho.mqtt.client as mqtt

def on_message(client, userdata, message):
    payload = message.payload.decode()
    data = json.loads(payload)
    
    if data['type'] == 'recipe':
        # 执行配方
        execute_recipe(
            plot_id=data['plotId'],
            water_ml=data['waterMl'],
            nutrient_ml=data['nutrientMl'],
            # ... 其他参数
        )

client = mqtt.Client()
client.on_message = on_message
client.connect("mqtt.00000.work", 1883, 60)
client.subscribe("time", qos=1)
client.loop_forever()
```

### 订阅 `voluntarily` 主题

设备应订阅 `voluntarily` 主题以接收AI自动执行建议：

```python
# Python 示例
import paho.mqtt.client as mqtt

def on_message(client, userdata, message):
    payload = message.payload.decode()
    data = json.loads(payload)
    
    if data['type'] == 'light':
        # 控制补光灯
        control_light(data['action'] == 'on')
    elif data['type'] == 'pump':
        # 控制抽水系统
        control_pump(data['action'] == 'on')
    elif data['type'] == 'recipe':
        # 执行配方
        execute_recipe(
            plot_id=data['plotId'],
            recipe_id=data['recipeId'],
            executions=data['executions']
        )

client = mqtt.Client()
client.on_message = on_message
client.connect("mqtt.00000.work", 1883, 60)
client.subscribe("voluntarily", qos=1)
client.loop_forever()
```

---

## 测试方法

### 测试 `text1` 主题（发布传感器数据）

```bash
# 使用 mosquitto_pub
mosquitto_pub -h mqtt.00000.work -p 1883 -t text1 -m '{"temperatureC":25.5,"soilMoisturePct":45.2,"lightLux":12000,"isRaining":false}'
```

### 测试 `time` 主题（订阅配方执行指令）

```bash
# 使用 mosquitto_sub
mosquitto_sub -h mqtt.00000.work -p 1883 -t time -v
```

### 测试 `voluntarily` 主题（订阅AI执行建议）

```bash
# 使用 mosquitto_sub
mosquitto_sub -h mqtt.00000.work -p 1883 -t voluntarily -v
```

---

## 注意事项

1. **QoS级别**: 所有主题使用 QoS 1（至少一次传递），确保消息可靠传输
2. **消息格式**: 所有消息均为 JSON 格式，UTF-8 编码
3. **时间戳**: `executeTime` 字段使用毫秒级时间戳
4. **错误处理**: 设备端应妥善处理消息解析错误和网络异常
5. **重连机制**: 建议设备端实现自动重连机制，确保连接稳定

---

## 相关文档

- [MQTT数据格式说明.md](./MQTT数据格式说明.md) - 详细的传感器数据格式说明
- [application.yml](../src/main/resources/application.yml) - MQTT 配置信息

