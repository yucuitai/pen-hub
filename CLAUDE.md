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

## 版本规划

### v1.0 - 新媒体文案生成（当前）

**核心定位：** 专注于新媒体文案生成场景

**目标用户：** 自媒体运营、内容创作者、营销人员

**核心功能：**
- 短文案生成（标题、正文、结尾）
- 多平台适配（公众号、小红书、抖音、微博等）
- 文案风格调整（正式、活泼、种草、干货等）
- AI 辅助优化（润色、扩写、缩写）

**设计原则：**
- 优化短文本生成体验
- 快速迭代、快速出稿
- 模板化 + AI 灵活生成

### v2.0 - AI 流程优化

**优化方向：**
- 智能体重试机制：大模型返回不合法 JSON 时自动重试，提高成功率
- Reviewer Agent：正文生成后、配图前加入审核智能体，评估内容质量并自动优化重写
- Prompt 优化：持续调试改进，提高 AI 生成内容质量

### 未来版本 - 长文本扩展

**扩展方向：**
- 小说创作（短篇、中篇、长篇）
- 剧本写作
- 长文章/深度报道
- 书籍章节规划

**技术储备：**
- 长文本上下文管理
- 大纲/章节结构化
- 角色/世界观设定系统
- 续写与连贯性保持

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
