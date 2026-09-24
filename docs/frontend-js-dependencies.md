# Vue 前端依赖

当前页面由 Vue 构建；旧的 `src/main/resources/static/js/` 脚本链已移除。历史脚本顺序可从 Git 提交 `e582f34` 查阅。

- `frontend/index.html` 加载 `frontend/src/main.js`。
- `main.js` 导入 Vue、`App.vue` 和 `frontend/src/styles/` 中的四个 CSS 文件。
- `App.vue` 导入 `api.js`、`portal.js` 和学生/管理端视图组件；登录后按角色显示对应页面。
- 各视图使用 `api.js` 的 Axios 实例调用 Spring Boot `/api`，不依赖 `window.*` 全局变量或 HTML 内联事件。
- `plan.js`、`workflow.js`、`insights.js`、`portal.js` 提供可单独测试的共享业务规则。
- `vite.config.js` 将 `pnpm build` 输出到 `src/main/resources/static/`。生成的 `index.html` 与哈希资源应一同提交，Spring Boot 才能直接提供完整页面。

修改 Vue 源文件后，在 `frontend/` 运行 `pnpm test` 和 `pnpm build`；随后在项目根目录运行 `./mvnw.cmd test`。Vite 开发服务默认是 `http://localhost:5173`，使用后端 `http://localhost:8080/api`；Spring Boot 提供的构建页面使用同源 `/api`。