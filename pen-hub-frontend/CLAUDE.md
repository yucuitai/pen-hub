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
