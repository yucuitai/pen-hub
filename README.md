# 笔枢 (Pen Hub)

> AI 驱动的新媒体文案生成平台

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-green)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3-brightgreen)](https://vuejs.org/)
[![Java](https://img.shields.io/badge/Java-21-orange)](https://www.oracle.com/java/)
[![TypeScript](https://img.shields.io/badge/TypeScript-6-blue)](https://www.typescriptlang.org/)

---

## 项目简介

笔枢是一个面向新媒体运营、内容创作者和营销人员的 AI 文案生成平台。通过多智能体协作，帮助用户快速生成适配各平台的优质文案内容。

### 核心特性

- 🤖 **多智能体协作** - 基于 AI Agent 的文案生成流程
- 📱 **多平台适配** - 支持公众号、小红书、抖音、微博等平台
- ✍️ **风格灵活调整** - 正式、活泼、种草、干货等多种风格
- 🔄 **AI 辅助优化** - 润色、扩写、缩写等智能优化功能
- 💳 **会员兑换系统** - 灵活的兑换码付费模式
- 📊 **数据统计分析** - 用户行为和内容数据分析

## 技术栈

### 后端

| 技术 | 说明 |
|------|------|
| Spring Boot 3.5 | 应用框架 |
| Java 21 | 编程语言 |
| MyBatis-Flex | ORM 框架 |
| MySQL | 关系型数据库 |
| Redis | 缓存 & 会话管理 |
| Spring Session | 分布式会话 |
| Knife4j | API 文档 (OpenAPI 3) |

### 前端

| 技术 | 说明 |
|------|------|
| Vue 3 | 前端框架 |
| TypeScript | 类型安全 |
| Vite | 构建工具 |
| Ant Design Vue | UI 组件库 |
| Pinia | 状态管理 |
| Vue Router | 路由管理 |
| Axios | HTTP 客户端 |
| ECharts | 数据可视化 |

## 项目结构

```
pen-hub/
├── pen-hub-backend/          # 后端服务
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/pen/penhubbackend/
│   │   │   │   ├── agent/        # AI 智能体
│   │   │   │   ├── controller/   # 控制器
│   │   │   │   ├── service/      # 业务逻辑
│   │   │   │   ├── mapper/       # 数据访问
│   │   │   │   ├── model/        # 数据模型
│   │   │   │   └── config/       # 配置类
│   │   │   └── resources/
│   │   └── test/
│   ├── sql/                    # 数据库脚本
│   └── pom.xml
│
├── pen-hub-frontend/         # 前端应用
│   ├── src/
│   │   ├── api/                # API 接口
│   │   ├── components/         # 公共组件
│   │   ├── pages/              # 页面视图
│   │   ├── router/             # 路由配置
│   │   ├── stores/             # 状态管理
│   │   └── utils/              # 工具函数
│   ├── package.json
│   └── vite.config.ts
│
└── README.md
```

## 快速开始

### 环境要求

- **Java**: 21+
- **Node.js**: 20.19+ 或 22.12+
- **MySQL**: 8.0+
- **Redis**: 6.0+

### 后端启动

```bash
cd pen-hub-backend

# 1. 创建数据库并导入初始数据
mysql -u root -p < sql/pen-hub.sql

# 2. 配置数据库连接
# 复制 application-local.yml.example 为 application-local.yml
# 修改数据库和 Redis 连接信息

# 3. 启动应用
./mvnw spring-boot:run
```

后端服务将在 `http://localhost:8567/api` 启动

### 前端启动

```bash
cd pen-hub-frontend

# 1. 安装依赖
npm install

# 2. 配置环境变量（可选）
# 创建 .env.local 文件配置 API 地址

# 3. 启动开发服务器
npm run dev
```

前端应用将在 `http://localhost:5173` 启动

### 构建部署

```bash
# 后端打包
cd pen-hub-backend
./mvnw clean package

# 前端构建
cd pen-hub-frontend
npm run build
```

## 功能模块

### 用户系统
- 用户注册 / 登录
- 个人中心管理
- 权限控制（普通用户 / 管理员）

### 文章创作
- 多智能体协作生成
- 平台风格适配
- AI 内容优化

### 会员系统
- 兑换码激活
- VIP 权限管理

### 后台管理
- 用户管理
- 数据统计分析
- 系统配置

## API 文档

启动后端服务后，访问以下地址查看 API 文档：

- **Knife4j**: http://localhost:8567/api/doc.html
- **OpenAPI**: http://localhost:8567/api/v3/api-docs

## 开发规范

### 提交规范

```
<type>: <description>

类型：feat | fix | refactor | docs | test | chore | perf | ci
```

### 分支策略

- `master` - 生产分支
- `dev` - 开发分支
- `feature/*` - 功能分支
- `fix/*` - 修复分支

## 版本规划

### v1.0 - 新媒体文案生成（当前）
- 短文案生成（标题、正文、结尾）
- 多平台适配
- 文案风格调整
- AI 辅助优化
- 兑换码付费系统

### v2.0 - AI 流程优化
- 智能体重试机制
- Reviewer Agent 审核
- Prompt 持续优化

### 未来版本 - 长文本扩展
- 小说创作
- 剧本写作
- 长文章 / 深度报道
- 书籍章节规划

## 相关文档

- [后端开发文档](pen-hub-backend/CLAUDE.md)
- [前端开发文档](pen-hub-frontend/CLAUDE.md)

## 许可证

MIT License

## 联系方式

如有问题或建议，欢迎提交 [Issue](../../issues) 或 [Pull Request](../../pulls)。
