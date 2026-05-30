# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

笔枢 (Pen Hub) -- AI 驱动的写作平台后端，基于 Spring Boot 3.5 + Java 21。

## 常用命令

```bash
# 构建项目
./mvnw clean package

# 运行项目（需要先启动 application-local.yml 中配置的 MySQL 和 Redis）
./mvnw spring-boot:run

# 运行所有测试
./mvnw test

# 运行单个测试类
./mvnw test -Dtest=PenHubBackendApplicationTests

# 运行单个测试方法
./mvnw test -Dtest=PenHubBackendApplicationTests#methodName

# 跳过测试打包
./mvnw clean package -DskipTests
```

## 技术栈

- **ORM**: MyBatis-Flex（不是 MyBatis-Plus），使用 `@Table`、`@Column` 注解
- **会话管理**: Spring Session + Redis（非 JWT）
- **API 文档**: Knife4j (OpenAPI 3)，访问 `/api/doc.html`
- **工具库**: Hutool 5.x、Lombok
- **AOP**: 已启用 `@EnableAspectJAutoProxy(exposeProxy = true)`

## 代码架构

### 分层结构

```
com.pen.penhubbackend/
├── common/       # 通用 DTO：BaseResponse、PageRequest、DeleteRequest
├── config/       # 配置类（CORS 等）
├── controller/   # REST 控制器
├── exception/    # 异常体系：ErrorCode → BusinessException → ThrowUtils → GlobalExceptionHandler
└── [未来模块]    # entity/、mapper/、service/、service/impl/
```

### 响应规范

所有接口统一返回 `BaseResponse<T>`：
- 成功：`code=0`，使用 `ResultUtils.success(data)`
- 失败：使用 `ResultUtils.error(ErrorCode.XXX)` 或抛出 `BusinessException`

### 异常处理模式

```java
// 条件抛出
ThrowUtils.throwIf(condition, ErrorCode.PARAMS_ERROR);

// 直接抛出
throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
```

### 数据库配置

- 主配置 `application.yml`：公共配置
- 本地配置 `application-local.yml`：敏感信息（不提交 Git），通过 `spring.profiles.active: local` 激活

### 服务端口与路径

- 端口：`8567`
- 上下文路径：`/api`
- 健康检查：`GET /api/health/`

## 项目文档

完整架构设计和模块规划见 `doc/笔枢-完整文档.md`，包含：
- 8 大模块规划（用户、创作、三阶段交互、多模态图片、智能编排、VIP、文章管理、日志监控）
- 三阶段创作流程：标题生成 → 大纲生成（用户编辑）→ 内容生成 + 并行图片生成
- 图片策略模式：Pexels、Mermaid、Iconify、NanoBanana AI、Picsum 兜底
- 存储：MySQL 8.0 + Redis 7.x + 腾讯云 COS
- 支付：Stripe + Webhook 验证
