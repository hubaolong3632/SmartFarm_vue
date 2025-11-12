# 前端项目 - 智能温室管理系统

基于 Vue 3 + Element Plus 的前端应用。

## 技术栈

- **Vue 3** - 渐进式 JavaScript 框架
- **Element Plus** - Vue 3 UI 组件库
- **Pinia** - 状态管理
- **Vue Router** - 路由管理
- **Axios** - HTTP 客户端
- **Vite** - 构建工具

## 安装依赖

```bash
npm install
```

## 开发

```bash
npm run dev
```

服务运行在 `http://localhost:2002`

## 构建

```bash
npm run build
```

## 项目结构

```
frontend/
├── src/
│   ├── components/    # 公共组件
│   │   ├── AlertsPanel.vue    # 报警面板
│   │   ├── ControlsPanel.vue  # 控制面板
│   │   └── LineChart.vue       # 折线图组件
│   ├── views/         # 页面视图
│   │   ├── Dashboard.vue           # 概览页
│   │   ├── RecipeEditor.vue        # 配方管理
│   │   ├── SoilAssignment.vue       # 地块分配
│   │   ├── AutomationSettings.vue  # 自动化设置
│   │   └── ImageGallery.vue         # 图片画廊
│   ├── stores/        # Pinia 状态管理
│   │   └── greenhouse.js  # 温室数据状态
│   ├── router/        # 路由配置
│   ├── utils/         # 工具函数
│   │   └── request.js  # HTTP 请求封装
│   ├── App.vue        # 根组件
│   └── main.js        # 入口文件
├── public/            # 静态资源
├── package.json       # 依赖配置
└── vite.config.js    # Vite 配置
```

## API 配置

后端 API 基础路径配置在 `src/utils/request.js`：

```javascript
baseURL: 'http://localhost:11000/api'
```

## 功能说明

### 1. 概览页 (Dashboard)
- 显示传感器数据折线图（温度、湿度、光照、降雨）
- 显示执行日志图表
- 显示当前状态和异常报警
- 提供控制按钮（清理熔炉、开关补光灯）

### 2. 配方管理 (RecipeEditor)
- 创建、编辑、删除植物配方
- 配置水、营养液、生根粉、特殊营养的用量

### 3. 地块分配 (SoilAssignment)
- 为 4 个地块分配配方
- 设置定时执行计划
- 查看执行日志

### 4. 自动化设置 (AutomationSettings)
- 配置光照阈值自动开灯
- 配置土壤湿度阈值自动浇水

### 5. 图片画廊 (ImageGallery)
- 按日期查看植物图片
- 显示每张图片的环境数据（温度、湿度、光照）
- 异常图片高亮显示

