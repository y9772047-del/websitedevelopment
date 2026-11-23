# Vue 前端依赖

当前页面由 Vue 构建；旧的 `src/main/resources/static/js/` 脚本链已移除。历史脚本顺序可从 Git 提交 `e582f34` 查阅。

- `frontend/index.html` 加载 `frontend/src/main.js`。
- `main.js` 导入 Vue、`App.vue` 和 `frontend/src/styles/` 中的四个 CSS 文件。
- `App.vue` 导入 `api.js`、`portal.js` 和学生/管理端视图组件；登录后按角色显示对应页面。
- 各视图使用 `api.js` 的 Axios 实例调用 Spring Boot `/api`，不依赖 `window.*` 全局变量或 HTML 内联事件。
- `plan.js`、`workflow.js`、`insights.js`、`portal.js` 提供可单独测试的共享业务规则。
- `workflow.js` 导入 `plan.js` 的 `preparePlan`，供 `TasksView.vue` 将快速收集文本校验并转换为计划、任务及可选记录；Vue 模块依赖由 Vite 排序，无新增全局变量或内联处理器。
- `TasksView.vue` 加载个人 DIY 分类及大学生/打工人推荐，推荐需点击添加才进入个人列表；`DashboardView.vue` 使用 `insights.js` 按实际任务分类统计。
- 分类管理通过 `/tasks/categories/mine` 获取分类 ID 和不受筛选影响的任务总数；`workflow.js` 的 `prepareCategory` 用于新增及改名。删除接收分类使用受控下拉框，改名/删除后刷新分类、任务，并清除旧快速收集预览；保留模块导入顺序及现有 API 调用方式。
- `TasksView.vue` 仅存储分类详情，分类名称列表使用 `computed` 派生；`refreshTasksAndCategories` 复用联合刷新，`setCategoryPanel` 统一面板互斥。请求对象仍通过 `props.api` 注入，未增加请求框架或跨文件脚本依赖。
- `DashboardView.vue` 导入 `dashboardData.js`，各请求成功后独立赋值；分类失败不影响计划、任务与记录，核心统计资源失败时显示错误而非零统计。未增加全局变量、HTML 内联处理器或全局脚本顺序依赖。
- `vite.config.js` 将 `pnpm build` 输出到 `src/main/resources/static/`。生成的 `index.html` 与哈希资源应一同提交，Spring Boot 才能直接提供完整页面。

修改 Vue 源文件后，在 `frontend/` 运行 `pnpm test` 和 `pnpm build`；随后在项目根目录运行 `./mvnw.cmd test`。Vite 开发服务默认是 `http://localhost:5173`，使用后端 `http://localhost:8080/api`；Spring Boot 提供的构建页面使用同源 `/api`。
