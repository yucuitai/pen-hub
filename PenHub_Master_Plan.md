# 笔枢 (Pen Hub) 总体开发计划（最终版）

> 版本：v2.0 Final
> 编制日期：2026-06-11
> 本文档面向 AI 编码代理，所有任务均关联真实代码路径

---

## 一、产品现状

### 1.1 已实现能力

| 模块 | 关键文件 | 成熟度 |
|------|----------|--------|
| 用户系统 | `UserController` / `UserService` / `UserLoginPage.vue` / `UserRegisterPage.vue` / `UserProfilePage.vue` | ★★★★☆ |
| 文章创作（5阶段） | `ArticleAgentOrchestrator` / `ArticleCreatePage.vue` + 5 个 Stage 组件 | ★★★★☆ |
| 多智能体编排 | `TitleGeneratorAgent` / `OutlineGeneratorAgent` / `ContentGeneratorAgent` / `ImageAnalyzerAgent` / `ContentMergerAgent` | ★★★★☆ |
| 流式输出 | `SseEmitterManager` / `StreamHandlerContext` / `ArticleAsyncService` | ★★★☆☆ |
| 配图策略 | `ImageServiceStrategy` + Pexels / Mermaid / Iconify / EmojiPack / SvgDiagram / NanoBanana | ★★★★☆ |
| VIP/兑换码 | `RedemptionService` / `PaymentService` / `VipPage.vue` | ★★★☆☆ |
| 管理后台 | `UserManagePage.vue` / `StatisticsPage.vue` / `StatisticsController` | ★★★☆☆ |
| 导出 | `ArticleDetailPage.vue` 中的 `exportMarkdown()` | ★★★☆☆ |

### 1.2 核心问题（已确认）

**产品层：**
1. 无模板/灵感库 → 新用户"空白页焦虑"
2. 文章生成后不可编辑 → 一次性产品，缺乏粘性
3. 移动端三栏布局直接隐藏侧边栏 → 体验降级
4. 文章列表仅有表格视图 → 不够直观
5. VIP 仅兑换码一种获取方式 → 无自然付费路径

**技术层：**
1. `base.css` 残留旧 `--vt-c-*` 变量，与 `variables.css` 冲突
2. 无统一 API 错误拦截（axios 拦截器缺失）
3. 无断点续传——中断后需从头开始
4. TypeScript 类型定义不完整（部分 `any`）
5. 零测试覆盖

---

## 二、设计优化（增量式，基于现有暖色体系）

### 2.1 色彩体系补充

**原则：不推翻现有设计，做增量补充。**

```css
/* === 新增到 variables.css === */

/* 暗色模式 */
[data-theme="dark"] {
  --color-background: #1A1614;
  --color-background-secondary: #252220;
  --color-background-tertiary: #302C28;
  --color-text: #E7E5E4;
  --color-text-secondary: #A8A29E;
  --color-border: #3D3935;
  --glass-bg: rgba(26, 22, 20, 0.92);
}

/* 语义化 Agent 阶段色 */
--color-stage-title: #D97706;       /* 标题阶段 - 琥珀 */
--color-stage-outline: #7C3AED;     /* 大纲阶段 - 紫色 */
--color-stage-content: #2563EB;     /* 正文阶段 - 蓝色 */
--color-stage-image: #059669;       /* 配图阶段 - 翠绿 */
--color-stage-merge: #DC2626;       /* 合并阶段 - 红色 */
```

### 2.2 排版优化

| 项 | 当前值 | 目标值 | 涉及文件 |
|----|--------|--------|----------|
| 正文最大宽度 | 100% | 720px | `common.css` / `ArticleDetailPage.vue` |
| Markdown 段落缩进 | `text-indent: 2em` | 去除（现代排版不需要） | `common.css` |
| 代码块 | 无复制按钮 | 增加复制按钮 | `common.css` + 自定义渲染器 |
| 图片 | 无点击放大 | 增加 lightbox | `ArticleDetailPage.vue` |

### 2.3 组件样式统一

**当前问题：** 按钮渐变、卡片样式、空状态等在每个 `.vue` 中重复定义。

**解决方案：** 新建 `pen-hub-frontend/src/styles/components.css`，将以下公共样式抽取：
- `.btn-primary-gradient`（已部分存在于 `common.css`，需扩展）
- `.card-elevated`（统一卡片阴影和圆角）
- `.empty-state`（已存在于 `common.css`，需完善）
- `.section-header`（页面区块标题样式）

### 2.4 移动端适配

**优先级：仅适配核心链路（创作页），其他页面后续迭代。**

```
断点：< 992px（已有 @media 断点）

创作页改动：
├── 隐藏左右侧边栏（已有，需优化）
├── 顶部显示进度条（替代左侧流程栏）
├── 底部固定操作栏（开始创作/确认等按钮）
└── 输入区域全宽显示

涉及文件：
├── ArticleCreatePage.vue
├── components/InputState.vue
├── components/TitleSelectingStage.vue
├── components/OutlineEditingStage.vue
└── components/CompletedState.vue
```

---

## 三、功能优化（按优先级排列）

### P0 - 必须做（直接影响核心价值）

#### 3.1 模板系统

**价值：** 降低上手门槛，提升首次创作转化率。

```
后端新增：
├── entity/Template.java              # 模板实体
├── mapper/TemplateMapper.java
├── service/TemplateService.java
├── service/impl/TemplateServiceImpl.java
├── controller/TemplateController.java # GET /api/template/list, /api/template/{id}
└── 数据库：template 表（id, name, category, platform, style, topic_example,
             recommended_image_methods, preview_content, sort_order, status）

前端新增：
├── pages/template/TemplatePage.vue           # 模板浏览页
├── pages/template/TemplateDetailPage.vue     # 模板详情+预览
├── api/templateController.ts                 # API 调用
└── 修改 ArticleCreatePage.vue 的 InputState  → 增加"从模板创建"入口

模板分类（初始 15 个）：
├── 平台：公众号 / 小红书 / 抖音 / 微博
├── 场景：产品评测 / 教程攻略 / 情感故事 / 行业分析 / 热点评论
└── 风格：种草风 / 干货风 / 故事风 / 观点风 / 幽默风
```

#### 3.2 断点续传

**价值：** 避免用户因网络/意外中断丢失进度。

```
当前机制：ArticleAsyncService 通过 @Async 执行各阶段，
          ArticleState 仅在内存中，不持久化。

改造方案：
├── 后端
│   ├── ArticleState 序列化为 JSON 存入 Article 表新字段 `state_snapshot`
│   ├── 每个阶段完成后更新 snapshot + phase
│   └── 新接口 GET /api/article/unfinished → 返回用户未完成的文章
│
├── 前端
│   ├── 进入创作页时检查未完成任务
│   ├── 如有 → 弹窗提示"继续上次创作？"
│   └── 确认后从对应 phase 恢复
│
└── 涉及文件
    ├── ArticleService.java（新增 saveStateSnapshot / loadStateSnapshot）
    ├── Article.java（新增 stateSnapshot 字段）
    ├── ArticleCreatePage.vue（加载时检查未完成任务）
    └── articleController.ts（新增 getUnfinished 接口）
```

#### 3.3 API 错误拦截

**价值：** 统一错误处理，避免页面白屏或无反馈。

```
新建：pen-hub-frontend/src/api/request.ts

核心逻辑：
├── 创建 axios 实例，baseURL = '/api'
├── 响应拦截器
│   ├── code === 40100 → message.warning + 跳转 /user/login?redirect=当前路径
│   ├── code !== 0 → message.error(message)
│   └── 网络错误 → message.error('网络异常，请稍后重试')
└── 请求拦截器
    └── 自动携带 cookie（withCredentials: true，当前已配置）

迁移：将所有 api/*.ts 中的 raw axios 调用改为 import { request } from './request'
```

### P1 - 应该做（提升体验和粘性）

#### 3.4 文章二次编辑

**价值：** 从"一次性生成"变为"可迭代的创作工具"。

```
Phase 1（Markdown 源码编辑）：
├── ArticleDetailPage.vue 增加"编辑"按钮
├── 编辑模式下 content 区域变为 textarea（Markdown 源码）
├── 右侧实时预览（已有 markdownToHtml）
├── 保存按钮 → PUT /api/article/{taskId}/content
└── 后端新增 updateContent 接口

Phase 2（富文本编辑器，后续迭代）：
├── 引入 Tiptap
├── 扩展：AI 润色/扩写/缩写（选中文本 → 右键菜单）
└── 自动保存（30s 间隔，localStorage + API 双写）
```

#### 3.5 Reviewer Agent

**价值：** 提升生成内容质量，减少用户手动修改。

```
插入位置：ContentGeneratorAgent → [ReviewerAgent] → ImageAnalyzerAgent

新建：
├── agent/agents/ReviewerAgent.java
│   ├── 输入：content + topic + style
│   ├── 输出：reviewScore (0-100) + suggestions[] + rewrittenContent?
│   └── 评分维度：连贯性 / 准确性 / 风格一致性 / 平台适配度
│
└── ArticleAgentOrchestrator.java
    ├── buildPhase3Graph 中在 ContentGenerator 后插入 Reviewer 节点
    └── reviewScore < 60 → 自动使用 rewrittenContent
        reviewScore 60-80 → 保留原内容 + 记录 suggestions
        reviewScore > 80 → 直接通过

前端展示：
├── CompletedState.vue 增加"质量评分"卡片
└── ArticleDetailPage.vue 展示审核报告
```

#### 3.6 多平台导出增强

```
当前：仅 Markdown 下载

新增导出格式：
├── 微信公众号格式（HTML + 内联样式，适配公众号编辑器）
├── 小红书图文格式（标题 + 正文 + 标签列表，纯文本）
├── PDF（前端 html2pdf.js 或后端 wkhtmltopdf）
└── Word（后端 Apache POI 或前端 docx.js）

涉及文件：
├── ArticleDetailPage.vue（exportDropdown 下拉菜单）
├── 新建 utils/exportUtils.ts（各格式导出逻辑）
└── 后端新增 GET /api/article/{taskId}/export?format=html|pdf|docx
```

#### 3.7 文章标签与收藏

```
数据库：
├── article_tag 表（id, article_id, tag_name）
└── Article 表新增 is_favorited 字段（或单独 favorite 表）

后端：
├── ArticleService 新增 toggleFavorite / addTag / removeTag
└── ArticleQueryRequest 新增 tag / isFavorited 过滤条件

前端：
├── ArticleDetailPage.vue 增加标签输入和收藏按钮
├── ArticleListPage.vue 增加标签筛选和收藏筛选
└── UserProfilePage.vue 展示常用标签云
```

### P2 - 可以做（商业化和长期价值）

#### 3.8 VIP 分级 + Stripe 支付

```
当前：单一 VIP（兑换码），后端已有 StripeConfig + PaymentController

改造：
├── 用户表新增 vip_level 字段（0=普通, 1=基础, 2=专业, 3=旗舰）
├── 各功能接口检查 vip_level（而非仅 isVip）
├── 前端 VipPage.vue 改为分级展示 + Stripe Checkout 集成
└── Stripe Webhook 完善（已有 StripeWebhookController，需补充支付成功回调逻辑）

功能分级：
├── 基础版：无限创作 + 基础配图
├── 专业版：+ AI 配图 + SVG 图表 + 大纲 AI 编辑
└── 旗舰版：+ 批量创作 + 优先队列 + 模板收藏
```

#### 3.9 数据驾驶舱

```
前端新增：pages/user/DashboardPage.vue

展示内容：
├── 创作统计（总数、本周、本月）→ 已有 StatisticsService
├── 创作趋势折线图（ECharts，按周统计）
├── 最近创作列表（取最近 5 篇）
├── 剩余配额
└── 常用标签云

后端：复用 StatisticsController，新增用户维度的统计接口
```

#### 3.10 管理后台增强

```
├── 创作漏斗（各阶段转化率）→ 从 AgentLog 表统计
├── AI 调用统计（token 消耗、平均耗时、失败率）→ AgentLog 表
├── 热门选题词云 → 从 Article.topic 统计
└── 用户留存（次日/7日/30日）→ 需新增用户活跃记录表
```

---

## 四、v2.0 内容类型扩展：脚本/台本

### 4.1 扩展方向

| 类型 | 场景 | 优先级 |
|------|------|--------|
| 短视频脚本 | 抖音/B站/视频号 | P1（v2.0 首批） |
| 直播台本 | 电商带货/知识分享 | P1（v2.0 首批） |
| 访谈/对话脚本 | 播客/访谈节目 | P2（v2.1） |
| 活动/会议台本 | 线下活动/发布会 | P2（v2.1） |
| 剧本/故事脚本 | 短剧/微电影 | P3（v2.2） |

### 4.2 数据模型扩展

```sql
-- 文章表新增字段
ALTER TABLE article ADD COLUMN content_type VARCHAR(20) DEFAULT 'ARTICLE';
-- ARTICLE / SHORT_VIDEO_SCRIPT / LIVE_SCRIPT / INTERVIEW_SCRIPT / EVENT_SCRIPT / DRAMA_SCRIPT

-- 脚本结构存储（JSON）
ALTER TABLE article ADD COLUMN script_structure TEXT;
-- 存储分镜/环节/对话等结构化数据
```

### 4.3 短视频脚本生成流程

```
用户输入：
├── 主题
├── 平台（抖音/B站/视频号）
├── 时长（15s/30s/60s/3min）
└── 风格（搞笑/知识/情感/产品）

Agent 流程：
├── ScriptHookAgent      → 生成开头 Hook（前 3 秒）
├── ScriptOutlineAgent   → 规划分镜和节奏
├── ScriptContentAgent   → 生成每个分镜的文案/台词/画面描述
├── ScriptReviewAgent    → 检查节奏和吸引力
└── ScriptMergerAgent    → 合并为完整脚本

输出格式：
├── Markdown 脚本（带时间轴标记）
├── JSON 结构化数据（供前端渲染）
└── 纯文本版（复制到剪贴板）
```

### 4.4 直播台本生成流程

```
用户输入：
├── 直播主题
├── 时长（1h/2h/4h）
├── 类型（电商带货/知识分享/活动直播）
├── 产品信息（电商场景）
└── 参与人数

Agent 流程：
├── LiveOutlineAgent     → 规划直播流程和时间节点
├── LiveScriptAgent      → 生成各环节话术
├── LiveInteractionAgent → 设计互动节点和福利环节
├── LiveEmergencyAgent   → 生成应急话术库
└── LiveMergerAgent      → 合并为完整台本
```

### 4.5 前端适配

```
新增页面：
├── pages/script/ScriptCreatePage.vue    # 脚本创作页（复用 ArticleCreatePage 的三栏布局）
├── pages/script/ScriptDetailPage.vue    # 脚本详情页
└── pages/script/components/
    ├── ScriptTypeSelector.vue           # 内容类型选择器
    ├── ScriptInputState.vue             # 脚本输入表单
    └── ScriptPreviewState.vue           # 脚本预览

修改：
├── HomePage.vue → 创作入口增加类型选择
├── GlobalHeader.vue → 导航增加"脚本"入口
└── router/index.ts → 新增脚本相关路由
```

---

## 五、技术债清理清单

| # | 任务 | 涉及文件 | 复杂度 |
|---|------|----------|--------|
| T1 | 清理 `base.css` 旧变量 | `src/assets/base.css` | 低 |
| T2 | 统一 API 错误拦截 | 新建 `src/api/request.ts` + 迁移所有 `api/*.ts` | 中 |
| T3 | 补充 TypeScript 类型 | `src/api/typings.d.ts` + 各 API 文件 | 中 |
| T4 | 提取公共组件样式 | 新建 `src/styles/components.css` | 低 |
| T5 | Ant Design ConfigProvider 主题 token | `App.vue` 或 `main.ts` | 低 |
| T6 | Pinia 持久化插件 | `src/stores/` + `pinia-plugin-persistedstate` | 低 |
| T7 | 后端接口限流 | 新建 `aop/RateLimitAspect.java` + Redis Lua | 中 |
| T8 | 后端 ArticleState 持久化 | `Article.java` + `ArticleService` | 中 |
| T9 | 前端路由鉴权守卫 | `src/router/index.ts` + `permission.ts` | 低 |
| T10 | ECharts 按需加载 | `StatisticsPage.vue` / `DashboardPage.vue` | 低 |

---

## 六、Phase 任务清单与排期

### Phase 1：地基加固（第 1-2 周）

> 目标：清理技术债，统一基础设施

| # | 任务 | 类型 | 预估工时 | 依赖 |
|---|------|------|----------|------|
| 1.1 | 清理 `base.css` 旧变量，统一到 `variables.css` | 前端 | 2h | 无 |
| 1.2 | 新建 `api/request.ts` 统一 axios 拦截器 | 前端 | 3h | 无 |
| 1.3 | 迁移所有 `api/*.ts` 使用统一 request 实例 | 前端 | 4h | 1.2 |
| 1.4 | 路由鉴权守卫（未登录跳登录页，记录 redirect） | 前端 | 2h | 1.2 |
| 1.5 | 提取公共组件样式到 `styles/components.css` | 前端 | 3h | 1.1 |
| 1.6 | ConfigProvider 主题 token 统一 | 前端 | 1h | 1.1 |
| 1.7 | 补充 API 响应 TypeScript 类型定义 | 前端 | 4h | 1.3 |
| 1.8 | Pinia 持久化插件集成 | 前端 | 1h | 无 |
| 1.9 | 后端 ArticleState 持久化到 Article.state_snapshot | 后端 | 4h | 无 |
| 1.10 | 后端接口限流（Redis + Lua 令牌桶） | 后端 | 4h | 无 |

**Phase 1 总工时：约 28h（3-4 个工作日）**

### Phase 2：核心体验（第 3-5 周）

> 目标：提升创作链路体验，建立用户粘性

| # | 任务 | 类型 | 预估工时 | 依赖 |
|---|------|------|----------|------|
| 2.1 | 模板系统 - 后端（实体/Mapper/Service/Controller + 初始数据） | 全栈 | 6h | 无 |
| 2.2 | 模板系统 - 前端（模板浏览页 + 模板详情 + 创作入口集成） | 前端 | 6h | 2.1 |
| 2.3 | 断点续传 - 前端（创作页加载时检查未完成任务 + 恢复逻辑） | 前端 | 4h | 1.9 |
| 2.4 | 移动端创作页适配（单列布局 + 顶部进度条 + 底部操作栏） | 前端 | 6h | 1.5 |
| 2.5 | 文章详情页排版优化（正文 720px / 去除缩进 / 代码块复制） | 前端 | 3h | 1.5 |
| 2.6 | 图片点击放大（lightbox） | 前端 | 2h | 2.5 |
| 2.7 | Markdown 导出增强（公众号 HTML / 小红书纯文本） | 全栈 | 4h | 无 |
| 2.8 | PDF/Word 导出 | 后端 | 4h | 2.7 |
| 2.9 | 文章标签系统（后端 + 前端） | 全栈 | 4h | 无 |
| 2.10 | 文章收藏功能（后端 + 前端） | 全栈 | 2h | 无 |

**Phase 2 总工时：约 41h（5-6 个工作日）**

### Phase 3：质量与商业化（第 6-9 周）

> 目标：提升内容质量，打通付费路径

| # | 任务 | 类型 | 预估工时 | 依赖 |
|---|------|------|----------|------|
| 3.1 | Reviewer Agent 实现 | 后端 | 6h | 无 |
| 3.2 | Reviewer Agent 集成到 ArticleAgentOrchestrator | 后端 | 3h | 3.1 |
| 3.3 | AI 生成重试机制（JSON Schema 校验 + 重试 + 降级） | 后端 | 4h | 无 |
| 3.4 | 质量评分展示（前端卡片 + 详情页报告） | 前端 | 3h | 3.1 |
| 3.5 | 文章二次编辑 - Markdown 源码编辑模式 | 全栈 | 6h | 无 |
| 3.6 | VIP 分级体系（后端 vip_level + 权限检查改造） | 后端 | 4h | 无 |
| 3.7 | Stripe 支付完整流程（前端 Checkout + 后端 Webhook） | 全栈 | 6h | 3.6 |
| 3.8 | VipPage 改版（分级展示 + 在线支付 + 兑换码并存） | 前端 | 4h | 3.7 |
| 3.9 | 数据驾驶舱（DashboardPage.vue + 后端统计接口） | 全栈 | 6h | 无 |
| 3.10 | 管理后台 - 创作漏斗 + AI 调用统计 | 全栈 | 4h | 无 |

**Phase 3 总工时：约 46h（6-7 个工作日）**

### Phase 4：v2.0 内容扩展（第 10-15 周）

> 目标：支持短视频脚本和直播台本生成

| # | 任务 | 类型 | 预估工时 | 依赖 |
|---|------|------|----------|------|
| 4.1 | 数据模型扩展（content_type / script_structure 字段） | 后端 | 2h | 无 |
| 4.2 | ScriptHookAgent 实现 | 后端 | 4h | 无 |
| 4.3 | ScriptOutlineAgent 实现 | 后端 | 4h | 无 |
| 4.4 | ScriptContentAgent 实现 | 后端 | 6h | 4.3 |
| 4.5 | ScriptReviewAgent 实现 | 后端 | 3h | 4.4 |
| 4.6 | ScriptMergerAgent 实现 | 后端 | 3h | 4.4 |
| 4.7 | 脚本编排器 ScriptAgentOrchestrator | 后端 | 6h | 4.2-4.6 |
| 4.8 | 内容类型选择器前端组件 | 前端 | 4h | 无 |
| 4.9 | 脚本创作页（ScriptCreatePage.vue） | 前端 | 8h | 4.7, 4.8 |
| 4.10 | 脚本详情页 + 脚本专用渲染 | 前端 | 4h | 4.9 |
| 4.11 | 直播台本 Agent 链（LiveOutline/Script/Interaction/Emergency） | 后端 | 10h | 无 |
| 4.12 | 直播台本编排器 LiveScriptOrchestrator | 后端 | 4h | 4.11 |
| 4.13 | 直播台本创作页 | 前端 | 6h | 4.12, 4.8 |
| 4.14 | 脚本/台本模板库（初始各 10 个） | 全栈 | 4h | 2.1, 4.9 |
| 4.15 | 首页/导航改造（增加脚本入口） | 前端 | 2h | 4.8 |

**Phase 4 总工时：约 70h（9-10 个工作日）**

### Phase 5：长期迭代（第 16 周+）

> 方向性任务，按需排期

| # | 任务 | 类型 | 说明 |
|---|------|------|------|
| 5.1 | 访谈/对话脚本 Agent | 后端 | v2.1 |
| 5.2 | 活动/会议台本 Agent | 后端 | v2.1 |
| 5.3 | 剧本/故事脚本 Agent | 后端 | v2.2 |
| 5.4 | 富文本编辑器（Tiptap 集成） | 前端 | Phase 3 后迭代 |
| 5.5 | 暗色模式 | 前端 | 变量已准备好，实现切换逻辑即可 |
| 5.6 | Prompt 管理后台 | 全栈 | 运营需要时做 |
| 5.7 | 多平台一键发布（微信/小红书 API） | 全栈 | 需要平台资质 |
| 5.8 | 用户反馈闭环（满意/不满意评分） | 全栈 | 数据积累后做 |
| 5.9 | 批量创作 | 全栈 | VIP 旗舰功能 |
| 5.10 | 测试覆盖（后端单元测试 + 前端 E2E） | 全栈 | 持续进行 |

---

## 七、关键设计决策

### 7.1 编辑器选型

| 方案 | 推荐度 | 理由 |
|------|--------|------|
| **Markdown 源码编辑 + 右侧预览** | ✅ Phase 3 首选 | 开发成本低，与现有 Markdown 渲染链路一致 |
| Tiptap (ProseMirror) | Phase 5 迭代 | 生态成熟，但集成成本高，适合长期 |
| Milkdown | 备选 | Vue 3 原生，但生态小 |

### 7.2 状态管理

```
当前：Pinia（loginUserStore）

新增 Store：
├── useArticleCreationStore  — 管理创作页状态（替代 ArticleCreatePage 中的散落 ref）
│   ├── topic, style, selectedImageMethods
│   ├── currentPhase, titleOptions, outline
│   └── 持久化到 localStorage（防刷新丢失）
│
├── useTemplateStore         — 模板列表和收藏
└── useScriptCreationStore   — 脚本创作状态（v2.0）
```

### 7.3 API 目录规范化

```
pen-hub-frontend/src/api/
├── request.ts               # 统一 axios 实例 + 拦截器（新建）
├── types/
│   ├── article.ts           # 文章相关类型
│   ├── user.ts              # 用户相关类型
│   ├── template.ts          # 模板相关类型（新建）
│   └── script.ts            # 脚本相关类型（v2.0 新建）
├── articleController.ts     # 文章接口
├── templateController.ts    # 模板接口（新建）
├── scriptController.ts      # 脚本接口（v2.0 新建）
├── yonghuguanli.ts          # 用户管理接口（建议重命名为 userController.ts）
├── redemptionController.ts  # 兑换码接口
└── adminController.ts       # 管理后台接口（建议从分散文件合并）
```

### 7.4 Agent 架构扩展策略

```
当前架构：ArticleAgentOrchestrator 使用 StateGraph 编排 6 个 Agent

扩展策略：
├── 每种内容类型一个 Orchestrator
│   ├── ArticleAgentOrchestrator    （已有）
│   ├── ScriptAgentOrchestrator     （v2.0 新建）
│   └── LiveScriptAgentOrchestrator （v2.0 新建）
│
├── 共用基础设施
│   ├── StreamHandler               （已有，SSE 流式输出）
│   ├── SseEmitterManager           （已有，连接管理）
│   ├── AgentLog + AgentExecution 注解 （已有，执行日志）
│   └── AgentConfig                 （已有，模型配置）
│
└── 新 Agent 遵循现有模式
    ├── 继承/实现方式参考现有 Agent（如 TitleGeneratorAgent）
    ├── 使用 @AgentExecution 注解记录日志
    └── 通过 StreamHandler 输出流式内容
```

---

## 八、风险与缓解

| 风险 | 概率 | 影响 | 缓解措施 |
|------|------|------|----------|
| AI 生成 JSON 不合法 | 高 | 单次生成失败 | Phase 3 的重试机制（任务 3.3） |
| 脚本/台本生成质量不达标 | 中 | v2.0 用户体验差 | 先上线短视频脚本（结构简单），验证后再做台本 |
| 移动端适配工作量超预期 | 中 | Phase 2 延期 | 先只适配创作页，其他页面降级为可滚动单列即可 |
| 外部 API（Pexels/LLM）不可用 | 低 | 配图失败 | 已有降级策略（配图失败时跳过），需确保前端容错 |
| Stripe 支付对接复杂度 | 中 | Phase 3 延期 | 后端已有 StripeConfig，优先完善 Webhook 回调 |

---

## 九、验收标准（每个 Phase）

### Phase 1 验收
- [ ] 删除 `base.css` 中所有 `--vt-c-*` 变量，页面无样式异常
- [ ] 未登录访问 `/create` 自动跳转 `/user/login`，登录后回跳
- [ ] API 返回非 0 code 时自动弹出 Toast 错误提示
- [ ] 刷新创作页不丢失已输入的选题和配置

### Phase 2 验收
- [ ] 模板页可浏览 15+ 模板，点击"使用模板"自动填充到创作页
- [ ] 创作中断后重新进入，弹窗提示继续，恢复到中断阶段
- [ ] 移动端（< 992px）创作页可正常使用，进度清晰可见
- [ ] 文章详情页正文宽度 720px 居中，图片可点击放大
- [ ] 支持导出为公众号 HTML 和小红书纯文本格式

### Phase 3 验收
- [ ] Reviewer Agent 对低于 60 分的内容自动重写，评分展示在详情页
- [ ] AI 返回非法 JSON 时自动重试（最多 3 次），用户无感知
- [ ] 文章详情页可切换到编辑模式，修改后可保存
- [ ] VipPage 展示三个分级，支持 Stripe 在线支付
- [ ] DashboardPage 展示创作统计和趋势图

### Phase 4 验收
- [ ] 首页可选择"短视频脚本"进入脚本创作流程
- [ ] 输入主题+平台+时长后，生成包含分镜的完整脚本
- [ ] 选择"直播台本"可生成带时间节点的完整台本
- [ ] 脚本/台本各有 10+ 可用模板
- [ ] 脚本可导出为 Markdown 和纯文本格式

---

## 附录：技术栈速查

| 层级 | 技术 | 版本 |
|------|------|------|
| 后端框架 | Spring Boot | 3.5 |
| 语言 | Java | 21 |
| ORM | MyBatis-Flex | — |
| 会话 | Spring Session + Redis | — |
| AI 编排 | Spring AI Alibaba StateGraph | — |
| API 文档 | Knife4j (OpenAPI 3) | — |
| 前端框架 | Vue 3 | 3.5 |
| 语言 | TypeScript | — |
| 构建工具 | Vite | — |
| UI 库 | Ant Design Vue | 4.2 |
| 图表 | ECharts | 6.1 |
| 状态管理 | Pinia | 3.0 |
| 路由 | Vue Router | 5.0 |
| Markdown | marked | 18.0 |
| 拖拽 | SortableJS | 1.15 |
