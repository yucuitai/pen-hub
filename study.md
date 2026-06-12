# 笔枢 (Pen Hub) 后端修改与优化记录

> 记录时间：2026-06-11
> 分支：feature/phase1-tech-debt
> 阶段：Phase 1 地基加固 + Phase 2 核心体验

---

## 一、Article 实体新增 stateSnapshot 字段

**文件：** `pen-hub-backend/src/main/java/com/pen/penhubbackend/model/entity/Article.java`

**改动：** 新增 `stateSnapshot` 字段，用于断点续传。

```java
/**
 * 状态快照（JSON，断点续传用）
 */
private String stateSnapshot;
```

**作用：** 在文章生成的每个阶段完成后，将当前的 `ArticleState` 对象序列化为 JSON 存入数据库。用户中途退出后重新进入创作页时，可通过 `/article/unfinished` 接口获取未完成的文章，从上次中断的阶段恢复执行。

**关联字段：** 与已有的 `phase` 字段配合使用——`phase` 记录当前所处阶段，`stateSnapshot` 记录该阶段的完整状态数据。

---

## 二、ArticleService 新增两个方法

**文件：** `pen-hub-backend/src/main/java/com/pen/penhubbackend/service/ArticleService.java`

### 2.1 saveStateSnapshot

```java
void saveStateSnapshot(String taskId, ArticleState state);
```

**作用：** 将当前 `ArticleState` 序列化为 JSON 并存入 `article.state_snapshot` 字段。

**调用时机：** 应在每个 Agent 阶段完成后调用（如标题生成完成、大纲生成完成、正文生成完成等），确保断点续传时能恢复到最近的完成点。

**实现位置：** `ArticleServiceImpl` 第 398 行

```java
@Override
public void saveStateSnapshot(String taskId, ArticleState state) {
    Article article = getByTaskId(taskId);
    if (article == null) {
        log.error("文章记录不存在, taskId={}", taskId);
        return;
    }
    article.setStateSnapshot(GsonUtils.toJson(state));
    this.updateById(article);
    log.info("状态快照已保存, taskId={}", taskId);
}
```

### 2.2 getUnfinishedArticle

```java
ArticleVO getUnfinishedArticle(User loginUser);
```

**作用：** 查询当前用户最近一篇未完成的文章。

**查询逻辑：**
- 条件：`user_id` 匹配 + `phase` 为非终态（TITLE_GENERATING / TITLE_SELECTING / OUTLINE_GENERATING / OUTLINE_EDITING / CONTENT_GENERATING）+ 未删除
- 排序：按创建时间倒序取最新一条
- 返回：`ArticleVO`（可能为 null）

**实现位置：** `ArticleServiceImpl` 第 410 行

```java
@Override
public ArticleVO getUnfinishedArticle(User loginUser) {
    QueryWrapper query = QueryWrapper.create()
            .eq("user_id", loginUser.getId())
            .in("phase",
                    ArticlePhaseEnum.TITLE_GENERATING.getValue(),
                    ArticlePhaseEnum.TITLE_SELECTING.getValue(),
                    ArticlePhaseEnum.OUTLINE_GENERATING.getValue(),
                    ArticlePhaseEnum.OUTLINE_EDITING.getValue(),
                    ArticlePhaseEnum.CONTENT_GENERATING.getValue())
            .eq("is_delete", 0)
            .orderBy("create_time").desc()
            .limit(1);
    Article article = this.getOne(query);
    return ArticleVO.objToVo(article);
}
```

---

## 三、ArticleController 新增 /unfinished 端点

**文件：** `pen-hub-backend/src/main/java/com/pen/penhubbackend/controller/ArticleController.java`

```java
@GetMapping("/unfinished")
@Operation(summary = "获取用户未完成的文章")
public BaseResponse<ArticleVO> getUnfinishedArticle(HttpServletRequest httpServletRequest) {
    User loginUser = userService.getLoginUser(httpServletRequest);
    ArticleVO articleVO = articleService.getUnfinishedArticle(loginUser);
    return ResultUtils.success(articleVO);
}
```

**接口说明：**
- 路径：`GET /api/article/unfinished`
- 鉴权：需要登录（通过 `userService.getLoginUser` 自动校验）
- 返回：未完成的 `ArticleVO`，若无则返回 `null`（前端需处理 null 情况）

**前端对接方式：** 用户进入创作页时调用此接口，若有返回值则弹窗提示"继续上次创作？"，确认后从 `stateSnapshot` 恢复状态。

---

## 四、接口限流（RateLimit）

### 4.1 限流注解

**文件：** `pen-hub-backend/src/main/java/com/pen/penhubbackend/annotation/RateLimit.java`（新建）

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    int maxRequests() default 60;      // 时间窗口内最大请求数
    int windowSeconds() default 60;    // 时间窗口（秒）
    String keyPrefix() default "";     // 限流 key 前缀
}
```

**使用方式：** 在 Controller 方法上标注 `@RateLimit(maxRequests = 5, windowSeconds = 60)` 即可。

### 4.2 限流切面

**文件：** `pen-hub-backend/src/main/java/com/pen/penhubbackend/aop/RateLimitAspect.java`（新建）

**实现原理：** Redis 固定窗口计数器

```
请求进入 -> 从注解读取 maxRequests / windowSeconds
         -> Redis INCR key（key = "rate_limit:" + 方法名 + ":" + 客户端IP）
         -> 若 count == 1 则设置过期时间
         -> 若 count > maxRequests 则抛出 BusinessException
         -> 否则放行
```

**Redis Key 格式：** `rate_limit:{方法短名}:{客户端IP}`

**IP 获取逻辑：** 依次尝试 `X-Forwarded-For` -> `X-Real-IP` -> `RemoteAddr`，多个代理时取第一个。

### 4.3 当前限流配置

| 接口 | 限流策略 | 说明 |
|------|----------|------|
| `POST /api/article/create` | 5次/分钟/IP | 文章创建（最昂贵操作，涉及配额扣减 + Agent 调用） |

**扩展方式：** 在其他 Controller 方法上添加 `@RateLimit` 注解即可，例如：
- `POST /api/article/confirm-title` — `@RateLimit(maxRequests = 10)`
- `POST /api/article/confirm-outline` — `@RateLimit(maxRequests = 10)`
- `POST /api/article/ai-modify-outline` — `@RateLimit(maxRequests = 3)`

---

## 五、依赖变更

**前端新增依赖：**

```
pinia-plugin-persistedstate@4
```

用于 Pinia store 持久化到 localStorage，防止刷新丢失用户登录状态。

**后端无新增依赖：** 限流功能基于项目已有的 `spring-boot-starter-data-redis` 实现。

---

## 六、数据库变更

若要使断点续传功能生效，需在数据库执行以下 DDL：

```sql
ALTER TABLE article ADD COLUMN state_snapshot TEXT COMMENT '状态快照（JSON，断点续传用）';
```

**注意：** 该字段为 TEXT 类型，因为 `ArticleState` 序列化后的 JSON 可能较大（包含标题方案、大纲、正文、配图等完整状态）。MySQL 的 TEXT 类型最大支持 65535 字节，对于绝大多数文章生成场景足够。若预期状态数据更大，可改为 MEDIUMTEXT。

---

# Phase 2 核心体验 - 修改记录

> 记录时间：2026-06-11
> 分支：feature/phase1-tech-debt

---

## 一、模板系统

### 1.1 后端（已完成）

**新建文件：**
- `model/entity/Template.java` — 模板实体
- `mapper/TemplateMapper.java` — 模板 Mapper
- `service/TemplateService.java` — 模板服务接口
- `service/impl/TemplateServiceImpl.java` — 模板服务实现
- `controller/TemplateController.java` — 模板 Controller

**接口：**
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/template/list` | 获取模板列表（支持 category/platform 筛选） |
| GET | `/template/{id}` | 获取模板详情 |

### 1.2 前端（本次完成）

**新建文件：**
- `pen-hub-frontend/src/api/templateController.ts` — 模板 API 调用
- `pen-hub-frontend/src/pages/template/TemplatePage.vue` — 模板浏览页

**修改文件：**
- `pen-hub-frontend/src/api/typings.d.ts` — 新增 `Template`、`BaseResponsePageTemplate`、`PageTemplate`、`BaseResponseTemplate` 类型
- `pen-hub-frontend/src/api/index.ts` — 新增 `templateController` 聚合导出
- `pen-hub-frontend/src/router/index.ts` — 新增 `/template` 路由
- `pen-hub-frontend/src/pages/article/ArticleCreatePage.vue` — 输入区添加"浏览模板"入口链接

**功能：**
- 模板浏览页支持按平台（公众号/小红书/抖音/微博）和分类（营销推广/知识科普/故事叙述/评测种草）筛选
- 点击模板卡片跳转到创作页，自动填充示例选题和风格
- 响应式布局，移动端单列显示

---

## 二、文章标签系统

### 2.1 后端（之前已完成）

**改动：**
- `Article.java` 新增 `tags` 字段（JSON数组）
- `ArticleService.java` 新增 `updateTags()` 方法
- `ArticleController.java` 新增 `POST /article/tags/{taskId}` 接口

### 2.2 前端（本次完成）

**改动文件：** `pen-hub-frontend/src/pages/article/ArticleDetailPage.vue`

**功能：**
- 标题区域下方显示标签列表
- 支持添加标签（输入框 + 回车确认）
- 支持删除标签（点击关闭图标）
- 标签变更实时同步后端

**API 调用：**
```typescript
// 更新标签
updateArticleTags(taskId, tags: string[])
```

---

## 三、文章收藏功能

### 3.1 后端（之前已完成）

**改动：**
- `Article.java` 新增 `isFavorited` 字段（0/1）
- `ArticleService.java` 新增 `toggleFavorite()` 方法
- `ArticleController.java` 新增 `POST /article/favorite/{taskId}` 接口

### 3.2 前端（本次完成）

**ArticleDetailPage.vue 改动：**
- 头部操作区添加收藏按钮（心形图标）
- 已收藏状态：红色填充背景
- 点击切换收藏状态，实时反馈

**ArticleListPage.vue 改动：**
- 筛选栏添加"收藏"筛选按钮
- 点击切换仅显示收藏文章
- 收藏按钮样式：红色激活状态

**API 调用：**
```typescript
// 切换收藏
toggleFavorite(taskId): boolean
```

---

## 四、图片点击放大 Lightbox

**改动文件：** `pen-hub-frontend/src/pages/article/ArticleDetailPage.vue`

**实现方式：**
1. **配图区域**：使用 Ant Design Vue 的 `<a-image>` 组件替代 `<img>`，内置预览功能
2. **Markdown 内容图片**：通过点击事件委托，点击图片时打开 Modal 预览

**代码逻辑：**
```typescript
// 处理 Markdown 内容中图片点击
const handleContentClick = (event: MouseEvent) => {
  const target = event.target as HTMLElement
  if (target.tagName === 'IMG' && target.closest('.markdown-content')) {
    previewImageUrl.value = (target as HTMLImageElement).src
    previewVisible.value = true
  }
}
```

---

## 五、多平台导出功能

**改动文件：** `pen-hub-frontend/src/pages/article/ArticleDetailPage.vue`

**导出格式：**

| 格式 | 说明 | 实现方式 |
|------|------|----------|
| Markdown | 原始 Markdown 文件 | 前端 Blob 下载 |
| 公众号 HTML | 带样式的 HTML，可直接粘贴到公众号编辑器 | 前端生成 HTML 模板 |
| 小红书纯文本 | 去除 Markdown 格式的纯文本 | 前端正则清理 |
| PDF | 服务端渲染的 PDF 文件 | 后端 OpenHTMLToPDF |
| Word | 服务端生成的 docx 文件 | 后端 Apache POI |

**UI 变化：**
- 原"导出 Markdown"按钮改为下拉菜单
- 菜单分两组：前端导出（Markdown/公众号/小红书）+ 服务端导出（PDF/Word）

---

## 六、PDF/Word 导出（服务端）

### 6.1 后端实现

**新建文件：**
- `pen-hub-backend/.../service/ExportService.java` — 导出服务接口
- `pen-hub-backend/.../service/impl/ExportServiceImpl.java` — 导出服务实现

**新增依赖（pom.xml）：**
```xml
<!-- Apache POI (Word 文档导出) -->
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.2.5</version>
</dependency>

<!-- OpenHTMLToPDF (PDF 导出) -->
<dependency>
    <groupId>com.openhtmltopdf</groupId>
    <artifactId>openhtmltopdf-pdf-openpdf</artifactId>
    <version>1.0.10</version>
</dependency>
```

**接口：**
```java
@GetMapping("/{taskId}/export")
@RateLimit(maxRequests = 10, windowSeconds = 60)
public void exportArticle(
    @PathVariable String taskId,
    @RequestParam(defaultValue = "pdf") String format,  // pdf 或 docx
    HttpServletRequest httpServletRequest,
    HttpServletResponse response)
```

**实现要点：**
- PDF 导出：Markdown → HTML → OpenHTMLToPDF 渲染
- Word 导出：Markdown → 解析 → Apache POI 构建文档
- 权限校验：仅文章作者或管理员可导出
- 限流保护：10次/分钟/IP
- 安全处理：标题清理控制字符防止 Header 注入

### 6.2 前端对接

**新增 API 函数：**
```typescript
// pen-hub-frontend/src/api/articleController.ts
export async function exportArticle(taskId: string, format: 'pdf' | 'docx' = 'pdf') {
  return request<any>(`/article/${taskId}/export`, {
    method: 'GET',
    params: { format },
    responseType: 'blob',
  })
}
```

---

## 七、移动端创作页适配

**改动文件：** `pen-hub-frontend/src/pages/article/ArticleCreatePage.vue`

**新增元素：**

1. **顶部进度条**（sticky 定位）
   - 显示 6 个智能体步骤的完成状态
   - 进度线动画
   - 仅在非 INPUT 阶段显示

2. **底部操作栏**（fixed 定位）
   - 根据当前阶段显示对应按钮：
     - INPUT → "开始创作"
     - TITLE_SELECTING → "确认标题"
     - OUTLINE_EDITING → "确认大纲，开始生成"
     - COMPLETED → "查看文章"
   - 适配 iOS 安全区域（`env(safe-area-inset-bottom)`）

**响应式断点：**
```css
@media (max-width: 992px) { /* 单列布局，隐藏侧边栏 */ }
@media (max-width: 768px) { /* 移动端优化 */ }
```

---

## 八、代码审查修复

在 Phase 2 完成后进行了全面代码审查，修复了以下问题：

### 8.1 CRITICAL 修复

| 文件 | 问题 | 修复 |
|------|------|------|
| `main.ts` | 重复的应用初始化代码，导致运行时崩溃 | 删除重复代码，保留带 pinia-plugin-persistedstate 的版本 |
| `App.vue` | 重复的 SFC 块，编译失败 | 合并为单个 script/template/style 块 |

### 8.2 HIGH 修复

| 文件 | 问题 | 修复 |
|------|------|------|
| `ArticleDetailPage.vue` | exportWechatHtml 存储型 XSS 风险 | 添加 `escapeHtml()` 函数转义用户内容 |
| `ArticleController.java` | 导出端点缺少限流保护 | 添加 `@RateLimit(maxRequests = 10, windowSeconds = 60)` |
| `ArticleController.java` | Content-Disposition Header 注入风险 | 清理标题中的控制字符 `\r\n\x00-\x1f` |
| `Article.java` | Javadoc 注释格式损坏（重复 `/**`） | 删除重复注释行 |
| `ArticleController.java` | Javadoc 注释格式损坏 | 删除重复注释行 |

### 8.3 其他修复

| 文件 | 问题 | 修复 |
|------|------|------|
| `loginUser.ts` | Pinia store 重复定义 | 删除重复代码，保留带 persist 的版本 |
| `common.css` | `.markdown-content` 样式重复定义 | 合并为单一定义块 |
| `ArticleCreatePage.vue` | 末尾多余 import 语句 | 删除 `</style>` 后的多余 import |
| `ArticleService.java` | `toggleFavorite`/`updateTags` 方法在接口闭合括号外 | 移入接口内部 |

---

## 九、数据库变更

**新增字段（article 表）：**

```sql
ALTER TABLE article ADD COLUMN state_snapshot TEXT NULL COMMENT '状态快照（JSON，断点续传用）';
ALTER TABLE article ADD COLUMN is_favorited TINYINT NOT NULL DEFAULT 0 COMMENT '收藏状态：0-未收藏 1-已收藏';
ALTER TABLE article ADD COLUMN tags JSON NULL COMMENT '文章标签（JSON数组）';
ALTER TABLE article ADD INDEX idx_user_id_favorited (user_id, is_favorited);
```

**新建表（template）：**

```sql
CREATE TABLE template (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    category VARCHAR(50) NOT NULL,
    platform VARCHAR(50) NOT NULL,
    style VARCHAR(20),
    topic_example VARCHAR(500),
    recommended_image_methods JSON,
    description VARCHAR(500),
    sort_order INT DEFAULT 0,
    status TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete TINYINT DEFAULT 0
);
```

---

## 十、Phase 2 完成状态

| 任务 | 状态 | 说明 |
|------|------|------|
| 2.1 模板系统后端 | ✅ | 5 个新文件 |
| 2.2 模板系统前端 | ✅ | TemplatePage.vue + API |
| 2.3 断点续传前端 | ✅ | 之前完成 |
| 2.4 移动端创作页适配 | ✅ | 进度条 + 操作栏 |
| 2.5 文章详情页排版优化 | ✅ | 之前完成 |
| 2.6 图片点击放大 | ✅ | a-image + Modal |
| 2.7 多平台导出 | ✅ | 5 种格式 |
| 2.8 PDF/Word 导出 | ✅ | 后端服务端渲染 |
| 2.9 文章标签系统 | ✅ | 前端 UI 完成 |
| 2.10 文章收藏功能 | ✅ | 前端 UI 完成 |

---

# Phase 3 质量与商业化 - 修改记录

> 记录时间：2026-06-11
> 分支：feature/phase1-tech-debt

---

## 一、Reviewer Agent 实现（3.1 + 3.2）

### 1.1 新建文件

**ReviewerAgent.java** ()

内容审核 Agent，评估文章质量并给出评分和改进建议。

- 输入：、、
- 输出：（ReviewResult 对象）
- 评分维度：连贯性、准确性、风格一致性、平台适配度
- 评分策略：
  -  分：自动使用 rewrittenContent 替换原文
  -  分：保留原内容 + 记录 suggestions
  -  分：直接通过

### 1.2 修改文件

**ArticleState.java** - 新增内部类：
- ：包含 、、、
- ：包含 、、、

**PromptConstant.java** - 新增  常量

**SseMessageTypeEnum.java** - 新增  消息类型

**ArticleAgentOrchestrator.java** - Phase 3 图变更：


**ArticleAsyncService.java** - 新增 REVIEWER_COMPLETE 消息处理

---

## 二、AI 生成重试机制（3.3）

**ReviewerAgent.java** 内置重试逻辑：
-  方法
- 最多重试 3 次
- 每次重试记录日志

---

## 三、文章二次编辑（3.5）

### 3.1 后端

**新建文件：**
-  - 内容更新请求 DTO

**ArticleService.java** - 新增  方法

**ArticleServiceImpl.java** - 实现 updateContent：
- 校验文章存在性和权限
- 仅已完成的文章可编辑
- 更新 content 和 fullContent 字段

**ArticleController.java** - 新增端点：


### 3.2 前端

**articleController.ts** - 新增  API 函数

**ArticleDetailPage.vue** - 编辑模式：
- 头部操作区新增编辑/预览切换按钮
- 编辑模式：左侧 Markdown 源码 textarea + 右侧实时预览
- 保存按钮调用  API
- 仅已完成状态的文章显示编辑按钮

---

## 四、质量评分展示（3.4）

**ArticleDetailPage.vue** - 质量评分区域：
- 执行日志面板下方显示内容质量评分
- 评分圆圈：颜色编码（>=80 绿色，60-79 橙色，<60 红色）
- 改进建议列表

**ArticleVO.java** - 新增字段：
- （Integer）
- （List<String>）

**typings.d.ts** - ArticleVO 类型新增  和 

---

## 五、VIP 分级体系（3.6）

### 5.1 后端

**User.java** - 新增  字段（Integer，0-3）

**VipLevelEnum.java**（新建）：
-  - 普通用户
-  - 基础版
-  - 专业版
-  - 旗舰版

**ArticleServiceImpl.java** - 更新  方法：
- 兼容旧的  检查
- 新增  检查

### 5.2 前端

**typings.d.ts** - LoginUserVO 新增  字段

**permission.ts** - 更新  函数，支持 vipLevel 检查；新增  函数

**VipPage.vue** - VIP 分级展示：
- 三列卡片布局：基础版(¥29/月)、专业版(¥59/月，推荐)、旗舰版(¥99/月)
- 各等级功能列表
- 保留原有兑换码区域

---

## 六、数据驾驶舱（3.9）

**新建文件：**
-  ()

**功能：**
- 4 个统计卡片：总创作数、本周创作、本月创作、成功率
- 创作趋势柱状图（ECharts）
- 最近 5 篇创作列表

**router/index.ts** - 新增  路由

---

## 七、管理后台增强（3.10）

### 7.1 后端

**AgentLogService.java** - 新增  方法

**AgentLogServiceImpl.java** - 实现全局 AI 调用统计：
- 总调用次数、成功/失败次数、失败率
- 平均耗时
- 各智能体调用次数分布

**StatisticsController.java** - 新增端点：


### 7.2 前端

**statisticsController.ts** - 新增  API 函数

**StatisticsPage.vue** - 新增 AI 调用统计区域：
- 统计卡片：总调用次数、成功率、平均耗时、失败率
- 各智能体调用次数柱状图

---

## 八、数据库变更

```sql
-- Phase 3 质量与商业化

-- 1. article 表新增审核相关字段
ALTER TABLE article ADD COLUMN review_score INT NULL COMMENT '内容质量评分' AFTER tags;
ALTER TABLE article ADD COLUMN review_suggestions JSON NULL COMMENT '审核改进建议' AFTER review_score;

-- 2. user 表新增 vip_level 字段
ALTER TABLE user ADD COLUMN vip_level TINYINT NOT NULL DEFAULT 0 COMMENT 'VIP等级：0-普通 1-基础 2-专业 3-旗舰' AFTER vip_type;
ALTER TABLE user ADD INDEX idx_vip_level (vip_level);
```

---

## 九、Phase 3 完成状态

| 任务 | 状态 | 说明 |
|------|------|------|
| 3.1 Reviewer Agent 实现 | ✅ | ReviewerAgent.java + Prompt + ArticleState |
| 3.2 Reviewer Agent 集成 | ✅ | 插入 Phase 3 图，REVIEWER_COMPLETE SSE |
| 3.3 AI 生成重试机制 | ✅ | ReviewerAgent 内置 callLlmWithRetry |
| 3.4 质量评分展示 | ✅ | ArticleDetailPage 评分卡片 + 建议列表 |
| 3.5 文章二次编辑 | ✅ | 编辑模式 + PUT /content/{taskId} |
| 3.6 VIP 分级体系 | ✅ | vipLevel + VipLevelEnum + 权限检查 |
| 3.7 Stripe 支付 | ⏳ | 后端已有 StripeConfig，需完善 Webhook |
| 3.8 VipPage 改版 | ✅ | 三列分级展示 + 兑换码并存 |
| 3.9 数据驾驶舱 | ✅ | DashboardPage.vue + 统计图表 |
| 3.10 管理后台增强 | ✅ | AI 调用统计 + 各智能体调用分布 |

---

# Phase 4 v2.0 内容类型扩展 - 修改记录

> 记录时间：2026-06-12
> 分支：feature/phase1-tech-debt

---

## 一、数据模型扩展（4.1）

### 1.1 Article 实体新增字段

**文件：** `pen-hub-backend/src/main/java/com/pen/penhubbackend/model/entity/Article.java`

```java
private String contentType;      // 内容类型：ARTICLE/SHORT_VIDEO_SCRIPT/LIVE_SCRIPT
private String scriptStructure;  // 脚本结构（JSON）
private String platform;         // 平台：douyin/bilibili/weixin_video
private String duration;         // 时长：15s/30s/60s/3min/1h/2h/4h
```

### 1.2 数据库变更

**文件：** `pen-hub-backend/sql/pen-hub.sql`

```sql
-- Phase 4 DDL（已追加到文件末尾）
ALTER TABLE article ADD COLUMN content_type VARCHAR(30) NOT NULL DEFAULT 'ARTICLE';
ALTER TABLE article ADD COLUMN script_structure TEXT NULL;
ALTER TABLE article ADD COLUMN platform VARCHAR(30) NULL;
ALTER TABLE article ADD COLUMN duration VARCHAR(20) NULL;
ALTER TABLE article ADD INDEX idx_content_type (content_type);

-- 脚本模板表 + 20 条模板数据（10 短视频 + 10 直播）
CREATE TABLE script_template (...);
```

### 1.3 枚举扩展

- **ContentTypeEnum** — 6 种内容类型
- **ArticlePhaseEnum** — 新增 10 个脚本/直播阶段
- **SseMessageTypeEnum** — 新增 13 个脚本/直播消息类型

---

## 二、短视频脚本 Agent 链（4.2 - 4.6）

| 文件 | 说明 |
|------|------|
| `model/dto/script/ScriptState.java` | 脚本状态 DTO |
| `agent/agents/ScriptHookAgent.java` | Hook 生成（非流式） |
| `agent/agents/ScriptOutlineAgent.java` | 分镜规划（流式） |
| `agent/agents/ScriptContentAgent.java` | 台词生成（流式） |
| `agent/agents/ScriptReviewAgent.java` | 脚本审核（重试） |
| `agent/agents/ScriptMergerAgent.java` | 脚本合成 |

流程：`Hook → 分镜 → 内容 → 审核 → 合成`

---

## 三、脚本编排器（4.7）

**文件：** `agent/ScriptAgentOrchestrator.java`

StateGraph 编排：`START → hook_generator → outline_generator → content_generator → reviewer → merger → END`

---

## 四、直播台本 Agent 链（4.11）

| 文件 | 说明 |
|------|------|
| `model/dto/script/LiveScriptState.java` | 直播台本状态 DTO |
| `agent/agents/LiveOutlineAgent.java` | 流程规划（非流式） |
| `agent/agents/LiveScriptAgent.java` | 话术生成（流式） |
| `agent/agents/LiveInteractionAgent.java` | 互动设计（非流式） |
| `agent/agents/LiveEmergencyAgent.java` | 应急话术（非流式） |
| `agent/agents/LiveMergerAgent.java` | 台本合成 |

流程：`大纲 → 话术 → 互动 → 应急 → 合成`

---

## 五、直播台本编排器（4.12）

**文件：** `agent/LiveScriptOrchestrator.java`

StateGraph 编排：`START → outline_generator → script_generator → interaction_designer → emergency_generator → merger → END`

---

## 六、后端服务层扩展

- **ArticleCreateRequest** — 新增 contentType/platform/duration/liveType/productInfo/participantCount
- **ArticleVO** — 新增 contentType/scriptStructure
- **ArticleAsyncService** — 新增 executeScriptGeneration / executeLiveScriptGeneration + 消息处理
- **ArticleController** — createArticle 存储脚本字段；startArticle 按 contentType 路由到不同生成流程

---

## 七、前端改动

| 文件 | 改动 |
|------|------|
| `components/ContentTypeSelector.vue` | **新建** — 内容类型选择器弹窗 |
| `api/typings.d.ts` | ArticleCreateRequest + ArticleVO 新增字段 |
| `pages/HomePage.vue` | 集成 ContentTypeSelector |
| `pages/article/ArticleCreatePage.vue` | 读取 contentType；动态 agentSteps；脚本输入字段；动态标题 |

---

## 八、Phase 4 完成状态

| 任务 | 状态 | 说明 |
|------|------|------|
| 4.1 数据模型扩展 | ✅ | Article + DDL + 枚举 |
| 4.2-4.6 脚本 Agent 链 | ✅ | 5 个 Agent |
| 4.7 ScriptAgentOrchestrator | ✅ | StateGraph 编排 |
| 4.8 内容类型选择器 | ✅ | ContentTypeSelector.vue |
| 4.9 脚本创作页 | ✅ | 扩展 ArticleCreatePage |
| 4.10 脚本详情页 | ✅ | 复用 ArticleDetailPage |
| 4.11 直播台本 Agent 链 | ✅ | 5 个 Agent |
| 4.12 LiveScriptOrchestrator | ✅ | StateGraph 编排 |
| 4.13 直播台本创作页 | ✅ | 集成到 ArticleCreatePage |
| 4.14 脚本/台本模板库 | ✅ | SQL 20 条模板 |
| 4.15 首页/导航改造 | ✅ | HomePage 集成 |

---

## 九、新增文件清单

**后端（14 个）：**
```
model/dto/script/ScriptState.java
model/dto/script/LiveScriptState.java
agent/ScriptAgentOrchestrator.java
agent/LiveScriptOrchestrator.java
agent/agents/ScriptHookAgent.java
agent/agents/ScriptOutlineAgent.java
agent/agents/ScriptContentAgent.java
agent/agents/ScriptReviewAgent.java
agent/agents/ScriptMergerAgent.java
agent/agents/LiveOutlineAgent.java
agent/agents/LiveScriptAgent.java
agent/agents/LiveInteractionAgent.java
agent/agents/LiveEmergencyAgent.java
agent/agents/LiveMergerAgent.java
```

**前端（1 个）：**
```
components/ContentTypeSelector.vue
```

---

# Phase 5 长期迭代 - 修改记录

> 记录时间：2026-06-12
> 分支：feature/phase1-tech-debt

---

## 一、暗色模式（5.5）

### 1.1 CSS 暗色变量

**文件：** `pen-hub-frontend/src/styles/variables.css`

新增 `[data-theme="dark"]` 块，包含所有色彩、阴影、毛玻璃等变量的暗色版本。

### 1.2 主题 Store

**新建文件：** `pen-hub-frontend/src/stores/themeStore.ts`

- Pinia store，持久化到 localStorage
- `theme` 状态：`'light'` | `'dark'`
- `toggleTheme()` 切换方法
- 监听变化自动设置 `data-theme` 属性到 `document.documentElement`

### 1.3 App.vue 改动

**文件：** `pen-hub-frontend/src/App.vue`

- `antTheme` 从静态对象改为 `computed`
- 根据 `themeStore.theme` 动态切换 `theme.darkAlgorithm` / `theme.defaultAlgorithm`

### 1.4 GlobalHeader 改动

**文件：** `pen-hub-frontend/src/components/GlobalHeader.vue`

- 导入 `useThemeStore`
- 右侧操作区新增主题切换按钮（☀️/🌙 emoji）
- `.theme-toggle` 样式：圆形按钮，hover 变色

---

## 二、用户反馈闭环（5.8）

### 2.1 数据库

**新增表：** `article_feedback`

```sql
CREATE TABLE article_feedback (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    article_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    rating TINYINT NOT NULL COMMENT '1=满意 0=不满意',
    comment VARCHAR(500) NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_article_user (article_id, user_id)
);
```

### 2.2 后端

**新建文件：**
- `model/entity/ArticleFeedback.java` — 反馈实体
- `mapper/ArticleFeedbackMapper.java` — Mapper
- `service/FeedbackService.java` — 服务接口
- `service/impl/FeedbackServiceImpl.java` — 服务实现
- `controller/FeedbackController.java` — 接口

**接口：**
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/feedback/submit` | 提交反馈（rating=0/1） |
| GET | `/feedback/stats/{taskId}` | 获取反馈统计 |
| GET | `/feedback/my/{taskId}` | 获取当前用户反馈 |

### 2.3 前端

**新建文件：**
- `pen-hub-frontend/src/api/feedbackController.ts` — API 调用

**改动文件：**
- `pen-hub-frontend/src/api/index.ts` — 新增 feedbackController 聚合
- `pen-hub-frontend/src/api/typings.d.ts` — 新增 ArticleFeedback 类型
- `pen-hub-frontend/src/pages/article/ArticleDetailPage.vue` — 底部反馈区域（👍/👎 按钮 + 统计）

---

## 三、新内容类型 Agent（5.1/5.2/5.3）

### 3.1 枚举扩展

**ArticlePhaseEnum.java** — 新增 13 个阶段：
- 访谈：INTERVIEW_OUTLINE/DIALOGUE/REVIEW/MERGING
- 活动：EVENT_OUTLINE/SCRIPT/CUE/MERGING
- 剧本：DRAMA_CHARACTER/PLOT/SCRIPT/REVIEW/MERGING

**SseMessageTypeEnum.java** — 新增 16 个消息类型

### 3.2 Prompt 模板

**PromptConstant.java** — 新增 16 个 Prompt

### 3.3 后端 Agent 文件（由后台 Agent 创建）

**访谈脚本（5.1）：** 6 个新文件（State + 4 Agent + Orchestrator）
**活动台本（5.2）：** 6 个新文件
**剧本（5.3）：** 7 个新文件

### 3.4 集成改动

**ArticleAsyncService.java** — 3 个 Orchestrator 注入 + 3 个执行方法
**ArticleController.java** — startArticle() 新增 3 个分支
**ContentTypeSelector.vue** — 新增 3 个内容类型卡片

---

## 四、Pre-existing Bug 修复

| 文件 | 问题 | 修复 |
|------|------|------|
| `pom.xml` | openhtmltopdf artifact ID 错误 | 改为 openhtmltopdf-pdfbox |
| `ArticleController.java` | 缺少 BusinessException import | 添加 import |
| `ArticleVO.java` | 缺少 reviewScore/reviewSuggestions | 添加字段 |
| `TemplateServiceImpl.java` | and() lambda 语法错误 | 简化为 eq 条件 |
| `VipPage.vue` | 模板语法错误 | 修复引号 |
| `ContentTypeSelector.vue` | LiveOutlined 不存在 | 改为 PlayCircleOutlined |
| `ArticleDetailPage.vue` | exportMarkdown 重复声明 | 删除重复 |

---

## 五、数据库变更

```sql
-- article_feedback 表 + 30 条新模板数据（访谈/活动/剧本各 10 条）+ prompt_template 表
```

---

## 六、Tiptap 富文本编辑器（5.4）

**新建文件：** `pen-hub-frontend/src/components/TiptapEditor.vue`

**改动文件：** `pen-hub-frontend/src/pages/article/ArticleDetailPage.vue`

编辑模式支持两种切换：
- WYSIWYG 模式：TiptapEditor 富文本编辑
- 源码模式：Markdown textarea + 实时预览

---

## 七、Prompt 管理后台（5.6）

**后端新建：**
- `model/entity/PromptTemplate.java`
- `mapper/PromptTemplateMapper.java`
- `service/PromptTemplateService.java` + `impl/PromptTemplateServiceImpl.java`
- `controller/admin/PromptTemplateController.java`

**前端新建：**
- `api/promptTemplateController.ts`
- `pages/admin/PromptManagePage.vue`

**接口：** GET/POST/PUT/DELETE `/admin/prompt/*`（管理员权限）

---

## 八、批量创作（5.9）

**后端新建：** `controller/BatchArticleController.java`
**前端新建：** `api/batchController.ts` + `pages/article/BatchCreatePage.vue`
**接口：** POST `/batch/create`（VIP 旗舰版限制）

---

## 九、Pre-existing Bug 修复（补充）

| 文件 | 问题 | 修复 |
|------|------|------|
| `pom.xml` | openhtmltopdf Maven Central 不可用 | 替换为 flying-saucer-pdf |
| `ExportServiceImpl.java` | PdfRendererBuilder 类不存在 | 替换为 ITextRenderer |

---

## 十、Phase 5 完成状态

| 任务 | 状态 | 说明 |
|------|------|------|
| 5.1 访谈脚本 Agent | ✅ | 6 个新文件 + 集成 |
| 5.2 活动台本 Agent | ✅ | 6 个新文件 + 集成 |
| 5.3 剧本 Agent | ✅ | 7 个新文件 + 集成 |
| 5.4 Tiptap 编辑器 | ✅ | TiptapEditor 组件 + 集成 |
| 5.5 暗色模式 | ✅ | themeStore + CSS + 切换按钮 |
| 5.6 Prompt 管理后台 | ✅ | CRUD 后端 + PromptManagePage 前端 |
| 5.7 多平台发布 | ❌ | 需平台资质 |
| 5.8 用户反馈闭环 | ✅ | 全栈完成 |
| 5.9 批量创作 | ✅ | BatchArticleController + BatchCreatePage |
| 5.10 测试覆盖 | ⏳ | 持续进行 |
