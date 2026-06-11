# 前端项目配置

> 通用命令和规范见根目录 `CLAUDE.md`

## 项目结构

```
src/
├── assets/          # 静态资源（CSS、图片）
├── components/      # 公共组件
├── layouts/         # 布局组件
├── pages/           # 页面组件
│   ├── admin/       # 管理后台
│   ├── article/     # 文章相关
│   └── user/        # 用户相关
├── stores/          # Pinia 状态管理
├── router/          # Vue Router 路由
├── views/           # 视图组件
└── reques.ts        # Axios 请求封装
```

## 技术细节

- **UI 框架**: Ant Design Vue
- **状态管理**: Pinia
- **路由**: Vue Router
- **HTTP 客户端**: Axios（封装在 `reques.ts`）
- **代码规范**: ESLint + Prettier
- **API 生成**: OpenAPI TypeScript Codegen（`openapi2ts.config.ts`）

## 开发注意事项

1. **组件命名**: PascalCase（如 `UserLoginPage.vue`）
2. **文件命名**: camelCase（如 `loginUser.ts`）
3. **路由配置**: `src/router/index.ts`
4. **全局状态**: `src/stores/` 目录下
5. **请求封装**: 统一使用 `src/reques.ts` 中的实例

## 后端接口

- 基础路径: `http://localhost:8567/api`
- API 文档: `http://localhost:8567/api/doc.html`

## AI 创作功能技术方案

### SSE 连接方案

后端使用 Spring 的 `SseEmitter` 推送创作进度，前端使用浏览器原生 `EventSource` API 接收，无需额外依赖。

```
前端(EventSource) ←— SSE 推送 ←— 后端(SseEmitter)
```

**实现要点：**
- 使用 `EventSource` 监听后端 SSE 端点
- 处理连接建立、消息接收、错误重连等事件
- 创作完成后主动关闭连接

### 流式渲染方案

创作过程有两处需要流式渲染，难点各不相同：

**大纲（JSON 格式）：**
- 后端分块推送时 JSON 可能不完整，不能直接 `JSON.parse`
- 策略：将收到的 chunk 不断拼接，每次拼接后尝试解析
- 能解析出多少章节就先渲染多少，实现渐进式展示

**正文（Markdown 格式）：**
- 处理相对简单，每次收到新 chunk 追加到已有内容
- 使用 `marked` 库将整段 Markdown 转成 HTML 渲染
- 视觉上形成打字机效果

### 页面状态设计

创作页面有三个状态，通过 `v-if` 互斥显示，同一时刻只显示一个状态：

```typescript
// 输入状态：用户填写选题，准备开始创作
!isCreating && !isCompleted

// 创作中状态：AI 正在生成，实时展示进度
isCreating && !isCompleted

// 完成状态：创作结束，展示最终的图文结果
isCompleted
```

**状态说明：**
- **输入状态**：展示选题输入表单，用户填写主题、风格等参数
- **创作中状态**：隐藏输入表单，实时展示大纲和正文的流式生成过程
- **完成状态**：展示最终生成的图文内容，提供编辑和导出功能
