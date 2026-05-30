// 根据后端接口的 OpenAPI 文档生成前端请求和 TS 模型代码
export default {
  requestLibPath: "import request from '@/request'",
  schemaPath: 'http://localhost:8567/api/v3/api-docs',
  serversPath: './src',
}
/* 
 这是 Knife4j 的设计机制：

**两个不同的端点：**

1. **`/api/doc.html`** - Knife4j 的**UI 界面**
   - 这是 Knife4j 提供的前端页面入口
   - 用于可视化查看和测试 API
   - 由 Knife4j 框架直接处理

2. **`/api/v3/api-docs/default`** - OpenAPI **JSON 数据**
   - 这是 SpringDoc 生成的 OpenAPI 3.0 规范的 JSON 数据
   - 前端工具（如你提到的配置）需要读取这个 JSON 来生成代码或文档
   - 遵循 OpenAPI 3.0 标准路径格式

**关系：**
- `doc.html` 页面内部会自动请求 `/v3/api-docs/default` 获取 API 数据
- 你的配置文件中 `schemaPath` 指向的是**数据接口**，不是 UI 页面
- 所以前端工具需要的是 `/v3/api-docs/default`，而浏览器访问用的是 `/doc.html`

**总结：** 一个给人看（HTML），一个给工具用（JSON）。
*/
