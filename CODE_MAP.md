# 项目代码地图

当前版本为 Spring Boot + MyBatis-Plus 后端，以及 Vue 3 + Vite 前端。旧静态 HTML/JS 只保留在 Git 历史中：`96ea080` 是原始静态版，`e582f34` 是独立 Vue 迁移版。

## 运行入口

- 开发入口：`frontend/index.html` → `frontend/src/main.js` → `frontend/src/App.vue`。
- 发布入口：`pnpm build` 在 `src/main/resources/static/index.html` 和 `assets/` 生成 Vue 文件，Spring Boot 从 `http://localhost:8080/` 提供页面。
- 页面与 API 同源时，Vue 使用 `/api`；Vite 开发模式默认使用 `http://localhost:8080/api`。可通过 `VITE_API_BASE_URL` 覆盖。
- `frontend/src/styles/` 保存全局主题和学生端、管理端样式。静态目录中的 `assets/` 是构建产物，修改样式应改源文件并重新构建。

## 后端

- 启动类：`src/main/java/com/example/studyplan/StudyPlanSystemApplication.java`。
- 接口：`src/main/java/com/example/studyplan/controller/`。`AuthController` 提供登录/注册/会话，`PlanController`、`TaskController`、`RecordController`、`DashboardController`、`CalendarController`、`StatisticsController`、`ImportantNodeController`、`ImportantNodeKeywordRuleController`、`FeedbackController`、`AiController`、`AdminController` 分别处理各业务 API。
- 业务逻辑：`service/` 和 `service/impl/`；数据库映射：`mapper/`；模型：`entity/`。
- `config/WebConfig.java` 注册登录和管理员 API 拦截器，并配置开发模式下带凭证的 CORS。
- 数据库建表与升级脚本：`src/main/resources/db/`；连接配置：`src/main/resources/application.yml`。

## Vue 前端

- `frontend/src/App.vue`：会话恢复、角色入口、导航和主题。
- `frontend/src/views/AuthView.vue`：登录和注册。
- `PlansView.vue` 与 `components/PlanForm.vue`：计划；`TasksView.vue`：任务；`RecordsView.vue`：学习记录、AI 改进建议和周报。
- `DashboardView.vue`：首页指标、焦点提醒、分类热力图、投入分布及已并入首页的学习统计。
- `CalendarView.vue` 与 `ImportantNodesView.vue`：日历、重要节点和关键词规则。
- `FeedbackView.vue`、`ThemeView.vue`：意见反馈和主题。
- `AdminPortal.vue` 与 `Admin*View.vue`：管理员页面。
- `frontend/src/api.js`：Axios、Session 凭证与错误处理。`plan.js`、`workflow.js`、`insights.js` 和 `portal.js`：表单字段、日期、批量导入、统计与主题规则。
- `frontend/src/styles/`：原有视觉样式已经迁入 Vue 源码，保留既有外观。

## 持续集成

- `.github/workflows/ci.yml`：按 `frontend/pnpm-lock.yaml` 安装 Vue 依赖，先测试与构建，再执行 Maven `clean verify`。
- `.github/pull_request_template.md` 与 `docs/experiment-3/`：PR 审查项目及实验三记录。

## 验证

在 `frontend/` 执行 `pnpm test`、`pnpm build`；在项目根目录执行 `./mvnw.cmd test` 和 `./mvnw.cmd clean package -DskipTests`。`VueDistributionTest` 检查 Vue 首页引用的构建资源是否存在，以及旧页面目录是否已移除。
