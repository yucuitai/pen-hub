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
- **数据库连接池**: HikariCP（必须显式添加，MyBatis-Flex starter 不包含）
- **会话管理**: Spring Session + Redis（非 JWT）
- **图片存储**: FTP 上传（宝塔 Pure-FTP）+ Nginx 静态资源映射
- **API 文档**: Knife4j (OpenAPI 3)，访问 `/api/doc.html`
- **工具库**: Hutool 5.x、Lombok、OkHttp、Gson
- **AOP**: 已启用 `@EnableAspectJAutoProxy(exposeProxy = true)`

### 编码补充（ECC Java 规则之外）

**实体类 Lombok 组合：**
```java
@Data @Builder @NoArgsConstructor @AllArgsConstructor
```

**接口实现：**
- 必须加 `@Override`

**参数校验：**
- 使用 Hutool：`StrUtil.hasBlank()`、`ObjectUtil.isNull()`
- 避免手写 `if (x == null || x.isEmpty())`

**开发顺序：**
- 先定义 Service 接口，再写 Impl 实现

## 代码架构

### 分层结构

```
com.pen.penhubbackend/
├── annotation/   # 自定义注解（@AuthCheck 权限校验）
├── aop/          # AOP 切面（AuthCheckAspect）
├── common/       # 通用 DTO：BaseResponse、PageRequest、DeleteRequest
├── config/       # 配置类（CORS 等）
├── constant/     # 常量定义（UserConstant）
├── controller/   # REST 控制器
├── exception/    # 异常体系：ErrorCode → BusinessException → ThrowUtils → GlobalExceptionHandler
├── mapper/       # MyBatis-Flex Mapper 接口
├── model/
│   ├── dto/      # 请求对象（DTO）
│   ├── entity/   # 数据库实体
│   ├── enums/    # 枚举类
│   └── vo/       # 视图对象（VO，脱敏）
└── service/      # 业务逻辑层
    └── impl/     # 服务实现类
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

## 用户模块 API

| 接口 | 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|------|
| 用户注册 | POST | `/api/user/register` | 无 | 返回用户ID |
| 用户登录 | POST | `/api/user/login` | 无 | 返回 UserVO（脱敏） |
| 获取当前用户 | GET | `/api/user/current` | 登录 | 返回当前登录用户 |
| 用户注销 | POST | `/api/user/logout` | 登录 | 清除 Session |
| 创建用户 | POST | `/api/user` | admin | 管理员创建用户 |
| 删除用户 | DELETE | `/api/user/{id}` | admin | 管理员删除用户 |
| 更新用户 | PUT | `/api/user` | admin | 管理员更新用户 |
| 分页查询 | GET | `/api/user/page` | admin | 管理员分页查询 |

### 权限校验

使用 `@AuthCheck(mustRole = "admin")` 注解标记需要管理员权限的接口，AOP 切面自动校验。

## 项目文档

完整架构设计和模块规划见 `doc/笔枢-完整文档.md`，包含：
- 8 大模块规划（用户、创作、三阶段交互、多模态图片、智能编排、VIP、文章管理、日志监控）
- 三阶段创作流程：标题生成 → 大纲生成（用户编辑）→ 内容生成 + 并行图片生成
- 图片策略模式：Pexels、Mermaid、Iconify、NanoBanana AI、Picsum 兜底
- 存储：MySQL 8.0 + Redis 7.x + FTP 图片服务器（腾讯云 COS 备选）
- 支付：Stripe + Webhook 验证

## 开发日志

### 2026-05-30

**用户模块完整实现：**
- 数据库：user 表（SQL 脚本在 `sql/pen-hub.sql`）
- 实体类：User.java（MyBatis-Flex 语法）
- 数据访问：UserMapper.java
- 业务层：UserService.java + UserServiceImpl.java
- 控制器：UserController.java（8 个接口）
- 请求类：UserLoginRequest、UserRegisterRequest
- 响应类：UserVO（脱敏）
- 枚举类：UserRoleEnum
- 权限控制：@AuthCheck 注解 + AuthCheckAspect 切面

**配置调整：**
- 敏感配置拆分到 `application-local.yml`（已加入 .gitignore）
- `.gitignore` 补充完整（环境变量、日志、OS 文件等）
- 创建 `CLAUDE.md` 项目指导文件

**User 实体类优化：**
- 添加 Lombok 注解：`@Builder`、`@NoArgsConstructor`、`@AllArgsConstructor`
- ID 生成改为雪花算法：`@Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)`
- 使用 `@Serial` 注解替代手动 serialVersionUID
- 移除冗余的手动 getter/setter/equals/hashCode/toString
- 确保 `camelToUnderline` 默认为 true（数据库下划线 ↔ 实体类驼峰）

### 2026-05-31

**MyBatis-Flex 启动修复：**
- 问题：`Property 'sqlSessionFactory' or 'sqlSessionTemplate' are required`
- 原因：缺少 HikariCP 数据库连接池依赖。MyBatis-Flex 的 `mybatis-flex-spring-boot3-starter` 只引入了 `spring-jdbc`，不包含 `spring-boot-starter-jdbc`（含 HikariCP + 自动配置）
- 修复：`pom.xml` 添加 `com.zaxxer:HikariCP`（版本由 Spring Boot Parent 管理）
- 注意：`@MapperScan("com.pen.penhubbackend.mapper")` 必须保留（官方文档要求）

**FTP 图片上传功能：**
- 实现：`CosService` 中新增 FTP 上传方法（`uploadToFtp`、`uploadFromUrlToFtp`、`uploadFromDataUrlToFtp`、`uploadImageDataToFtp`）
- 配置类：`FtpConfig.java`（prefix: `ftp`）
- 服务器：宝塔面板 + Pure-FTP（虚拟用户体系）
- Nginx 映射：`/images/` → `/www/wwwroot/picture/`（配置文件在服务器 `/www/server/panel/vhost/nginx/images.conf`）

**踩坑记录：**
- Pure-FTP `UnixAuthentication no` + `MinUID 100`：禁止系统用户（如 root）登录，必须在宝塔面板创建 FTP 虚拟用户
- Pure-FTP `ChrootEveryone yes`：FTP 用户被限制在主目录内，`base-path` 必须设为 `/`（不能写完整路径，否则路径会重复：`/www/wwwroot/picture/www/wwwroot/picture/...`）
- 被动模式端口范围：`39000-40000`，服务器防火墙需放行

**待完成：**
- 图片存储策略完善（FTP 为主，COS 为备）
- 图片访问 URL 统一处理
- FTP 上传的异常处理和重试机制
