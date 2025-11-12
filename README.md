# 智能温室管理系统

智能温室管理系统，包含前端 Vue 3 应用和后端 Spring Boot API。

## 项目结构

```
vue_text/
├── frontend/          # 前端 Vue 3 项目
│   ├── src/          # 源代码
│   ├── public/       # 静态资源
│   ├── package.json  # 前端依赖配置
│   └── vite.config.js # Vite 配置
│
├── backend/          # 后端 Spring Boot 项目
│   ├── src/         # Java 源代码
│   ├── database/    # 数据库脚本
│   ├── pom.xml      # Maven 依赖配置
│   └── README.md    # 后端说明文档
│
└── utio/            # 旧文件（可删除）
```

## 快速开始

### 前端开发

```bash
cd frontend
npm install
npm run dev
```

前端服务将运行在 `http://localhost:2002`

### 后端开发

```bash
cd backend
mvn clean install
mvn spring-boot:run
```

后端 API 将运行在 `http://localhost:11000/api`

## 技术栈

### 前端
- Vue 3
- Element Plus
- Pinia
- Vue Router
- Axios
- Vite

### 后端
- Spring Boot 3.1.5
- MyBatis-Plus
- MySQL
- MQTT (EMQX)

## 功能模块

1. **概览页** - 传感器数据图表、控制面板、报警信息
2. **配方管理** - 创建、编辑、删除植物配方
3. **地块分配** - 分配配方到地块，设置定时执行
4. **自动化设置** - 配置自动化阈值和规则
5. **图片画廊** - 查看植物图片及环境数据

## 数据库

数据库脚本位于 `backend/database/schema.sql`

## 更多信息

- 前端详细说明：查看 `frontend/` 目录
- 后端详细说明：查看 `backend/README.md`
