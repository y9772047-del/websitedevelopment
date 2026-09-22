# 实验二验证记录

日期：2026-09-22。范围为独立 Vue 前端第一阶段；后端业务代码、数据库结构和原静态页面未在本阶段修改。

| 验证 | 实际结果 |
| --- | --- |
| 计划与 API 单元测试 | node --test frontend/src/api.test.js frontend/src/plan.test.js：10 项通过 |
| JavaScript 语法检查 | api.js、plan.js、main.js 和浏览器回归脚本通过 node --check |
| Vue 生产构建 | Vite 成功构建 71 个模块 |
| Maven 完整测试 | mvnw.cmd test：57 项，0 失败、0 错误、0 跳过 |
| Maven 打包 | mvnw.cmd clean package -DskipTests：BUILD SUCCESS |
| 独立代码审查 | 发现切换页面丢失草稿、保存期间可导航两项问题；均已修复 |
| 真实浏览器与数据库联调 | 登录、注册、Session、计划 CRUD、详情任务读取、CORS、响应式布局及错误边界通过 |
| 测试数据清理 | 临时计划已删除；临时用户按精确 id 和用户名删除，确认无关联数据和残留账号 |
| OpenAPI 检查 | JSON 可解析；路径、请求字段和响应 schema 按 Controller 与 Entity 核对 |
| Git 差异检查 | git diff --check 通过，仅有 Windows 换行提示 |

浏览器回归脚本 frontend/tests/browser/phase1-interactions.js 读取已有计划详情并拦截保存请求，用于验证草稿保留和保存期间导航锁定，不写数据库。

真实 CRUD 使用单独注册的临时普通用户，没有编辑原有用户的计划。数据库只执行了针对该临时账号的精确清理，没有执行建表、删表、清库或 schema 迁移。

浏览器还验证了以下边界：

- 用户输入 <img src=x onerror=alert(1)> 后没有生成图片节点。
- 已设置日期不能在编辑时直接清空，避免后端忽略 null 后仍提示保存成功。
- 记住用户名只写入用户名，Local Storage 不包含演示密码。
- 真实 Session 失效会清除计划界面并提示重新登录。
- 后端不可达时提示 http://localhost:8080/api，恢复后可重新加载。
- 管理员登录仍跳转原 /pages/admin.html。

本阶段没有修改数据库 schema，没有 SQL 迁移；没有修改 API 路径或响应结构；没有弱化登录、角色或 userId 所有权检查。原任务、记录、日历、统计和管理后台继续由旧前端承载。