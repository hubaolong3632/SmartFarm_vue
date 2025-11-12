# API 路径参数变更说明

所有使用路径参数的接口已改为查询参数风格。

## 变更列表

### 1. 控制操作 (ControlController)

**变更前：**
- `POST /api/control/light/{action}` - action: "on" 或 "off"

**变更后：**
- `POST /api/control/light?action=1` - action: 1=打开, 0=关闭

**示例：**
```bash
# 打开灯光
POST /api/control/light?action=1

# 关闭灯光
POST /api/control/light?action=0
```

### 2. 报警记录 (AlertController)

**变更前：**
- `GET /api/alerts/level/{level}`
- `PUT /api/alerts/{id}/read`

**变更后：**
- `GET /api/alerts/level?level=warning`
- `PUT /api/alerts/read?id=1`

**示例：**
```bash
# 获取警告级别的报警
GET /api/alerts/level?level=warning

# 标记为已读
PUT /api/alerts/read?id=1
```

### 3. 自动化设置 (AutomationController)

**变更前：**
- `GET /api/automation/{key}`

**变更后：**
- `GET /api/automation/setting?key=auto_light_threshold`

**示例：**
```bash
GET /api/automation/setting?key=auto_light_threshold
```

### 4. 执行日志 (ExecutionLogController)

**变更前：**
- `GET /api/execution-logs/plot/{plotId}`

**变更后：**
- `GET /api/execution-logs/plot?plotId=1`

**示例：**
```bash
GET /api/execution-logs/plot?plotId=1
```

### 5. 图片管理 (ImageController)

**变更前：**
- `GET /api/images/date/{date}`
- `GET /api/images/plot/{plotId}`

**变更后：**
- `GET /api/images/date?date=2024-01-01`
- `GET /api/images/plot?plotId=1`

**示例：**
```bash
# 根据日期查询
GET /api/images/date?date=2024-01-01

# 根据地块查询
GET /api/images/plot?plotId=1
```

### 6. 地块管理 (PlotController)

**变更前：**
- `POST /api/plots/{plotId}/assign`
- `GET /api/plots/{plotId}/assignment`
- `POST /api/plots/{plotId}/schedules`
- `GET /api/plots/{plotId}/schedules`
- `DELETE /api/plots/schedules/{scheduleId}`

**变更后：**
- `POST /api/plots/assign?plotId=1`
- `GET /api/plots/assignment?plotId=1`
- `POST /api/plots/schedules?plotId=1`
- `GET /api/plots/schedules?plotId=1`
- `DELETE /api/plots/schedules?scheduleId=1`

**示例：**
```bash
# 分配配方到地块
POST /api/plots/assign?plotId=1
Content-Type: application/json
{
  "recipeId": "r1",
  "executions": 3
}

# 获取地块分配
GET /api/plots/assignment?plotId=1

# 添加定时计划
POST /api/plots/schedules?plotId=1
Content-Type: application/json
{
  "recipeId": "r1",
  "timeHHmm": "12:00",
  "executions": 2
}

# 获取定时计划列表
GET /api/plots/schedules?plotId=1

# 删除定时计划
DELETE /api/plots/schedules?scheduleId=1
```

### 7. 配方管理 (RecipeController)

**变更前：**
- `GET /api/recipes/{id}`
- `PUT /api/recipes/{id}`
- `DELETE /api/recipes/{id}`

**变更后：**
- `GET /api/recipes/detail?id=r1`
- `PUT /api/recipes?id=r1`
- `DELETE /api/recipes?id=r1`

**示例：**
```bash
# 获取配方详情
GET /api/recipes/detail?id=r1

# 更新配方
PUT /api/recipes?id=r1
Content-Type: application/json
{
  "name": "新配方",
  "waterMl": 600
}

# 删除配方
DELETE /api/recipes?id=r1
```

## 总结

所有路径参数 `{param}` 已改为查询参数 `?param=value` 风格，使 API 更加统一和易于使用。

## 注意事项

1. **控制灯光接口**：action 参数从字符串改为数字（1=打开, 0=关闭）
2. **日期格式**：日期参数格式仍为 `yyyy-MM-dd`（如：2024-01-01）
3. **所有查询参数都是必需的**，除非特别说明

