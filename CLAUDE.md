# CLAUDE.md

笔枢 (Pen Hub) - AI 驱动的写作平台

## 项目架构

Monorepo 结构，前后端代码在同一仓库：

```
pen-hub/
├── pen-hub-backend/    # Spring Boot 3.5 + Java 21
├── pen-hub-frontend/   # Vue 3 + TypeScript + Vite
└── doc/                # 项目文档（可选）
```

## 常用命令

### 后端

```bash
cd pen-hub-backend

# 构建
./mvnw clean package

# 运行（需要 MySQL + Redis）
./mvnw spring-boot:run

# 测试
./mvnw test
```

### 前端

```bash
cd pen-hub-frontend

# 安装依赖
npm install

# 开发服务器
npm run dev

# 构建
npm run build

# 代码检查
npm run lint
```

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端框架 | Spring Boot 3.5, Java 21 |
| ORM | MyBatis-Flex（非 MyBatis-Plus）|
| 会话管理 | Spring Session + Redis |
| 前端框架 | Vue 3 + TypeScript |
| 构建工具 | Vite |
| API 文档 | Knife4j (OpenAPI 3) |

## 子目录配置

详细的项目配置请参阅各子目录的 CLAUDE.md：

- `pen-hub-backend/CLAUDE.md` - 后端项目配置
- `pen-hub-frontend/CLAUDE.md` - 前端项目配置（如存在）

## 开发规范

### 提交规范

```
<type>: <description>

类型：feat, fix, refactor, docs, test, chore, perf, ci
```

### 分支策略

- `master` - 生产分支
- `dev` - 开发分支
- `feature/*` - 功能分支
- `fix/*` - 修复分支

### 端口配置

- 后端：`8567`（上下文路径 `/api`）
- 前端：`5173`（Vite 默认）

## 环境配置

- 后端敏感配置：`application-local.yml`（不提交 Git）
- 前端环境变量：`.env.local`（不提交 Git）
