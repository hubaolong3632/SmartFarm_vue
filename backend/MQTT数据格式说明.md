# MQTT 传感器数据传输格式说明

## 概述

系统通过 MQTT 协议接收传感器数据，自动解析并保存到数据库。MQTT 消息格式为 JSON。

## 配置信息

- **MQTT Broker**: 配置在 `application.yml` 中
- **订阅主题**: `text1` (可在配置文件中修改)
- **QoS**: 1 (至少一次传递)

## 消息格式

### 标准格式（推荐）

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

### 字段说明

| 字段名 | 类型 | 必填 | 说明 | 示例 |
|--------|------|------|------|------|
| `recordTime` | String | 否 | 记录时间，格式：`yyyy-MM-dd HH:mm:ss`。如果不提供，使用服务器当前时间 | `"2024-11-12 14:30:00"` |
| `temperatureC` | Number | 否* | 温度（摄氏度） | `25.5` |
| `humidityPct` | Number | 否* | 湿度（百分比，0-100） | `60.5` |
| `soilMoisturePct` | Number | 否* | 土壤湿度（百分比，0-100） | `45.2` |
| `lightLux` | Integer | 否* | 光照强度（lux） | `12000` |
| `isRaining` | Boolean | 否 | 是否下雨（true/false） | `false` |
| `oxygenPct` | Number | 否* | 氧气含量（百分比，0-100） | `20.5` |
| `co2Ppm` | Integer | 否* | 二氧化碳含量（ppm，0-10000） | `400` |

**注意**: `temperatureC`、`humidityPct`、`soilMoisturePct`、`lightLux`、`oxygenPct`、`co2Ppm` 至少需要提供一个，否则消息会被忽略。

### 兼容格式

系统也支持以下字段名的变体（向后兼容）：

- `time` → `recordTime`
- `temperature` → `temperatureC`
- `humidity` → `humidityPct`
- `soilMoisture` 或 `moisture` → `soilMoisturePct`
- `light` → `lightLux`
- `raining` → `isRaining`
- `oxygen` → `oxygenPct`
- `co2` 或 `carbonDioxide` → `co2Ppm`

### 简化格式示例

```json
{
  "temperature": 25.5,
  "moisture": 45.2,
  "light": 12000,
  "raining": false
}
```

## 完整示例

### 示例 1：完整数据

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

### 示例 2：仅温度数据

```json
{
  "temperatureC": 25.5
}
```

### 示例 3：使用兼容字段名

```json
{
  "time": "2024-11-12 14:30:00",
  "temperature": 25.5,
  "humidity": 60.5,
  "moisture": 45.2,
  "light": 12000,
  "raining": 0,
  "oxygen": 20.5,
  "co2": 400
}
```

### 示例 4：最小格式（仅必需字段）

```json
{
  "temperatureC": 25.5,
  "humidityPct": 60.5,
  "soilMoisturePct": 45.2
}
```

## 数据验证

- **温度范围**: 建议 -50°C 到 100°C
- **湿度范围**: 0-100%
- **土壤湿度范围**: 0-100%
- **光照强度**: 非负整数
- **氧气含量范围**: 0-100%
- **二氧化碳含量范围**: 0-10000 ppm
- **时间格式**: 必须为 `yyyy-MM-dd HH:mm:ss` 格式（如果不提供，使用服务器当前时间）

## 数据存储

接收到的数据会自动保存到 `sensor_data` 表中，前端可以通过以下 API 获取：

- `GET /api/sensor-data/latest` - 获取最新数据
- `GET /api/sensor-data/last-24-hours` - 获取最近24小时数据

## 测试方法

### 使用 MQTT 客户端发布测试消息

```bash
# 使用 mosquitto_pub 工具
mosquitto_pub -h 124.223.53.96 -p 1883 -t text1 -m '{"temperatureC":25.5,"soilMoisturePct":45.2,"lightLux":12000,"isRaining":false}'
```

### 使用后端 API 发布测试消息

```bash
# Windows PowerShell (需要转义引号)
curl -X POST "http://localhost:11000/api/mqtt/publish?topic=text1&message={\"temperatureC\":25.5,\"soilMoisturePct\":45.2,\"lightLux\":12000,\"isRaining\":false}&qos=1"

# Linux/Mac
curl -X POST 'http://localhost:11000/api/mqtt/publish?topic=text1&message={"temperatureC":25.5,"soilMoisturePct":45.2,"lightLux":12000,"isRaining":false}&qos=1'
```

### 使用 Python 发布测试消息

```python
import paho.mqtt.client as mqtt
import json
import time

# MQTT 配置
broker = "124.223.53.96"
port = 1883
topic = "text1"

# 创建客户端
client = mqtt.Client()
client.connect(broker, port, 60)

# 准备数据
data = {
    "recordTime": time.strftime("%Y-%m-%d %H:%M:%S"),
    "temperatureC": 25.5,
    "humidityPct": 60.5,
    "soilMoisturePct": 45.2,
    "lightLux": 12000,
    "isRaining": False,
    "oxygenPct": 20.5,
    "co2Ppm": 400
}

# 发布消息
client.publish(topic, json.dumps(data), qos=1)
print(f"已发送: {json.dumps(data)}")

client.disconnect()
```

### 使用 Arduino/ESP32 发布消息示例

```cpp
#include <WiFi.h>
#include <PubSubClient.h>
#include <ArduinoJson.h>

const char* ssid = "your_wifi_ssid";
const char* password = "your_wifi_password";
const char* mqtt_server = "124.223.53.96";
const int mqtt_port = 1883;
const char* mqtt_topic = "text1";

WiFiClient espClient;
PubSubClient client(espClient);

void setup() {
  Serial.begin(115200);
  WiFi.begin(ssid, password);
  
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  
  client.setServer(mqtt_server, mqtt_port);
  
  while (!client.connected()) {
    if (client.connect("ESP32Client")) {
      Serial.println("MQTT Connected");
    } else {
      delay(5000);
    }
  }
}

void loop() {
  // 读取传感器数据
  float temperature = 25.5;  // 从传感器读取
  float humidity = 60.5;     // 从传感器读取
  float moisture = 45.2;     // 从传感器读取
  int light = 12000;          // 从传感器读取
  bool raining = false;       // 从传感器读取
  float oxygen = 20.5;        // 从传感器读取
  int co2 = 400;              // 从传感器读取
  
  // 创建 JSON 对象
  StaticJsonDocument<300> doc;
  doc["temperatureC"] = temperature;
  doc["humidityPct"] = humidity;
  doc["soilMoisturePct"] = moisture;
  doc["lightLux"] = light;
  doc["isRaining"] = raining;
  doc["oxygenPct"] = oxygen;
  doc["co2Ppm"] = co2;
  
  // 转换为字符串
  char buffer[200];
  serializeJson(doc, buffer);
  
  // 发布消息
  client.publish(mqtt_topic, buffer, true);
  
  delay(3600000);  // 每小时发送一次
}
```

## 错误处理

如果消息格式不正确或解析失败：
- 消息仍会保存在内存中（可通过 `/api/mqtt/messages` 查看）
- 但不会保存到数据库
- 错误信息会记录在日志中

## 注意事项

1. **时间同步**: 如果设备时间与服务器时间不同步，建议在消息中包含 `recordTime` 字段
2. **数据频率**: 建议每小时发送一次数据，避免过于频繁
3. **数据完整性**: 虽然字段都是可选的，但建议至少包含温度和湿度数据
4. **字符编码**: 消息必须使用 UTF-8 编码

