# 项目代码地图

> 编码：UTF-8。本文档只说明当前项目代码位置和修改入口，适合初学者按功能查找文件。项目是 Spring Boot + MyBatis-Plus + 静态前端页面的学习计划管理系统。

## Frontend JS map

- `src/main/resources/static/js/app.js`：核心状态、共享工具、学习计划、反馈、导航初始化。
- `src/main/resources/static/js/tasks.js`：学习任务相关工具、任务表单、任务列表、批量导入。
- `src/main/resources/static/js/records.js`：学习记录表单、记录列表、AI 改进建议。
- `src/main/resources/static/js/important-nodes.js`：重要节点、关键词规则、重要节点弹窗和批量导入。
- `src/main/resources/static/js/calendar.js`：学习日历渲染、月份切换、日期详情。
- `src/main/resources/static/js/dashboard.js`：首页驾驶舱、统计指标、热力图、计划投入轮盘图。
- `docs/frontend-js-dependencies.md`：前端 JS 加载顺序、跨文件全局依赖、`window.*` 导出和拆分检查清单。

## 一、项目整体结构

- 后端入口：`src/main/java/com/example/studyplan/StudyPlanSystemApplication.java`
- 后端 Controller：`src/main/java/com/example/studyplan/controller/`
- 后端 Service 接口：`src/main/java/com/example/studyplan/service/`
- 后端 Service 实现：`src/main/java/com/example/studyplan/service/impl/`
- 后端 Mapper：`src/main/java/com/example/studyplan/mapper/`
- 后端 Entity / 请求响应对象：`src/main/java/com/example/studyplan/entity/`
- 后端公共返回、Session 工具：`src/main/java/com/example/studyplan/common/`
- 后端配置和启动维护逻辑：`src/main/java/com/example/studyplan/config/`
- 前端页面：`src/main/resources/static/index.html`、`src/main/resources/static/pages/app.html`、`src/main/resources/static/pages/admin.html`
- 前端 JS：`src/main/resources/static/js/auth.js`、`src/main/resources/static/js/app.js`、`src/main/resources/static/js/admin.js`、`src/main/resources/static/js/common.js`、`src/main/resources/static/js/tasks.js`、`src/main/resources/static/js/records.js`、`src/main/resources/static/js/important-nodes.js`、`src/main/resources/static/js/calendar.js`、`src/main/resources/static/js/dashboard.js`
- 前端 CSS：`src/main/resources/static/css/base.css`（主题变量与共享组件）、`auth.css`（登录注册页）、`app.css`（普通用户端）、`admin.css`（管理后台）
- 数据库建表脚本：`src/main/resources/db/schema.sql`
- 数据库升级脚本：`src/main/resources/db/admin_feedback_upgrade.sql`、`plan_investment_upgrade.sql`、`record_reflection_upgrade.sql`、`task_category_upgrade.sql`、`test_data_constraints.sql`
- 项目配置：`src/main/resources/application.yml`
- Maven 配置：`pom.xml`
- 测试代码：`src/test/java/com/example/studyplan/`

## 二、后端地图

### Controller

- `AuthController`：登录注册和当前用户接口，基础路径 `/api/auth`。接口包括 `POST /register`、`POST /login`、`POST /logout`、`GET /current`。
- `PlanController`：学习计划增删改查，基础路径 `/api/plans`。接口包括 `POST /api/plans`、`GET /api/plans`、`GET /api/plans/{id}`、`PUT /api/plans/{id}`、`DELETE /api/plans/{id}`。
- `TaskController`：学习任务、任务分类、批量任务导入，基础路径 `/api/tasks`。重点方法有 `add`、`batchAdd`、`list`、`categories`、`detail`、`update`、`updateStatus`、`delete`。查询支持 `planId`、`status`、`priority`、`date`。
- `RecordController`：学习记录增删改查，基础路径 `/api/records`。接口包括 `POST /api/records`、`GET /api/records`、`GET /api/records/task/{taskId}`、`GET /api/records/recent`、`PUT /api/records/{id}`、`DELETE /api/records/{id}`。
- `DashboardController`：首页驾驶舱数据，基础路径 `/api/dashboard`，`GET /api/dashboard` 返回计划数、任务数、完成率、最近任务、最近记录等。
- `StatisticsController`：统计页数据，基础路径 `/api/statistics`，`GET /api/statistics` 返回计划数、任务数、完成任务数、逾期任务数、学习时长等。
- `CalendarController`：按年月查学习日历任务，基础路径 `/api/calendar`，`GET /api/calendar?year=...&month=...` 查询该月截止日期内的任务。
- `ImportantNodeController`：学习日历里的重要节点 / 重大事件，基础路径 `/api/important-nodes`。接口包括 `GET /api/important-nodes?year=...`、`POST /api/important-nodes`、`POST /api/important-nodes/batch`、`PUT /api/important-nodes/{id}`、`DELETE /api/important-nodes/{id}`。
- `AiController`：AI 改进建议，基础路径 `/api/ai`，核心接口 `POST /api/ai/generate-improvement`。
- `FeedbackController`：用户意见反馈，基础路径 `/api/feedback`。接口包括 `POST /api/feedback`、`GET /api/feedback/my`。
- `AdminController`：后台管理，基础路径 `/api/admin`。接口包括后台统计、用户列表/详情/状态修改、任务列表/详情/删除、反馈列表/详情/处理。

### Service

- `UserService` / `UserServiceImpl`：用户注册 `register`、登录 `login`，对应 `user` 表。
- `StudyPlanService` / `StudyPlanServiceImpl`：学习计划基础 CRUD，主要依赖 MyBatis-Plus 通用方法。
- `StudyTaskService` / `StudyTaskServiceImpl`：学习任务基础 CRUD、任务分类列表 `listTaskCategories`、分类校验 `isValidCategory`、旧分类归一 `normalizeLegacyCategories`。
- `StudyRecordService` / `StudyRecordServiceImpl`：学习记录 CRUD，并在 `saveAndAccumulateInvestment`、`updateAndRefreshInvestment`、`removeAndDeductInvestment` 中同步更新计划累计学习投入。
- `ImportantNodeService` / `ImportantNodeServiceImpl`：重要节点基础 CRUD。
- `AiService` / `AiServiceImpl`：生成 AI 改进建议。核心方法 `generateImprovement`，内部有 `callDeepSeek`、`buildPrompt`、`fallbackImprovement`、`fallbackActions`、`extractJson`。
- `FeedbackService` / `FeedbackServiceImpl`：用户反馈基础 CRUD。

### Repository / Mapper / DAO

项目没有单独的 Repository/DAO 包，数据库访问集中在 `mapper` 包，全部继承 MyBatis-Plus 的 `BaseMapper`。

- `UserMapper`：操作 `user` 表。
- `StudyPlanMapper`：操作 `study_plan` 表，额外提供 `incrementInvestment(planId, minutesDelta, recordCountDelta)`，用于更新计划累计学习分钟数和记录数。
- `StudyTaskMapper`：操作 `study_task` 表。
- `StudyTaskCategoryMapper`：操作 `study_task_category` 表。
- `StudyRecordMapper`：操作 `study_record` 表。
- `ImportantNodeMapper`：操作 `important_node` 表。
- `FeedbackMapper`：操作 `feedback` 表。

### Entity / Model

- `User`：对应 `user` 表，字段有 `id`、`username`、`password`、`realName`、`role`、`status`、`createTime`。
- `StudyPlan`：对应 `study_plan` 表，字段有 `planName`、`goal`、`description`、`startDate`、`endDate`、`status`、`totalStudyMinutes`、`totalStudyRecordCount` 等。
- `StudyTask`：对应 `study_task` 表，字段有 `planId`、`taskName`、`description`、`category`、`priority`、`deadline`、`status` 等。
- `StudyTaskCategory`：对应 `study_task_category` 表，字段有 `categoryName`、`sortOrder`、`description`。
- `StudyRecord`：对应 `study_record` 表，字段有 `taskId`、`studyDate`、`duration`、`content`、`completionStatus`、`problems`、`improvementPlan`。
- `ImportantNode`：对应 `important_node` 表，字段有 `title`、`type`、`startDate`、`endDate`、`startTime`、`endTime`、`importance`、`description`、`planId`、`taskId`。
- `Feedback`：对应 `feedback` 表，字段有 `userId`、`content`、`status`、`reply`、`createTime`、`updateTime`。

### DTO / VO

项目没有单独的 `dto` 或 `vo` 包，少量接口传输对象放在 `entity` 包：

- `AiImprovementRequest`：`POST /api/ai/generate-improvement` 的请求体，字段包括 `planId`、`planName`、`taskName`、`studyContent`、`completion`、`problems`、`problemSummary`。
- `AiImprovementResponse`：AI 接口响应体，字段包括 `improvementPlan`、`suggestedActions`、`generationSource`、`generationSourceLabel`、`generationSourceDetail`。
- 其他接口大多直接使用 Entity 作为请求体和响应数据。

### Config

- `WebConfig`：注册登录拦截器和管理员拦截器。普通接口 `/api/**` 需要登录，但排除 `/api/auth/**`；管理员接口 `/api/admin/**` 和 `/pages/admin.html` 需要管理员权限。
- `LoginInterceptor`：检查 Session 中是否有登录用户。
- `AdminInterceptor`：检查当前用户是否有管理员权限。
- `PlanInvestmentMaintenanceRunner`：启动时检查并补齐 `study_plan.total_study_minutes`、`total_study_record_count`，并根据已有学习记录回填累计投入。
- `TaskCategoryMaintenanceRunner`：启动时执行任务分类兼容处理。
- `ImportantNodeMaintenanceRunner`：启动时检查重要节点相关表结构兼容。

## 三、前端地图

- 登录/注册页面：`src/main/resources/static/index.html`，表单 ID 是 `loginForm`、`registerForm`，提示区域是 `authMessage`。
- 普通用户主页面：`src/main/resources/static/pages/app.html`，导航通过 `data-page` 切换 `dashboard`、`plans`、`tasks`、`records`、`calendar`、`importantNodes`、`feedback`、`themes`。
- 管理后台页面：`src/main/resources/static/pages/admin.html`，导航通过 `data-page` 切换 `adminDashboard`、`adminUsers`、`adminTasks`、`adminFeedbacks`、`adminThemes`。
- 登录注册 JS：`src/main/resources/static/js/auth.js`，主要函数 `formToObject`、`postJson`、`showAuthMessage`，调用 `api/auth/login`、`api/auth/register`。
- 普通用户入口 JS：`src/main/resources/static/js/app.js`，核心请求函数是 `api(url, options)`，保留共享状态、工具、学习计划、反馈、导航和初始化调度。
- 学习任务 JS：`src/main/resources/static/js/tasks.js`，负责学习任务相关表单、列表、筛选、批量导入等逻辑。
- 学习记录 JS：`src/main/resources/static/js/records.js`，负责学习记录表单、记录列表、AI 改进建议展示等逻辑。
- 重要节点 JS：`src/main/resources/static/js/important-nodes.js`，负责重要节点、关键词规则、弹窗和批量导入。
- 学习日历 JS：`src/main/resources/static/js/calendar.js`，负责日历渲染、月份切换、日期详情展示等逻辑。
- 驾驶舱 JS：`src/main/resources/static/js/dashboard.js`，负责首页驾驶舱、统计指标、热力图和计划投入轮盘图。
- 管理后台 JS：`src/main/resources/static/js/admin.js`，核心请求函数是 `adminApi(url, options)`，主要函数 `loadAdminDashboard`、`loadAdminUsers`、`loadAdminTasks`、`loadAdminFeedbacks`。
- 公共主题 JS：`src/main/resources/static/js/common.js`，主要函数 `initUserTheme`、`applyTheme`、`renderThemeCards`、`useTheme`。
- CSS 样式：`src/main/resources/static/css/base.css` 保存主题变量与共享组件；`auth.css` 保存登录注册页样式；`app.css` 保存 `.dashboard-*`、`.category-heatmap-*`、`.plan-investment-*`、`.calendar-*`、`.important-node-*`、`.ai-suggestion-box` 等普通用户端样式；`admin.css` 保存 `.admin-*` 管理后台样式。
- 图表相关：没有引入专门图表库，使用 DOM + CSS 生成，集中在 `dashboard.js`。迷你条形图函数 `renderMiniChart`，热力图函数 `renderCategoryHeatmap`，计划投入轮盘/环形图函数 `renderPlanInvestmentDonut`。

## 四、核心功能地图

### 首页驾驶舱

1. 入口页面：`app.html` 的 `<section id="dashboard">`。
2. 前端主要文件：`dashboard.js` 的 `loadDashboard`、`renderMetricCards`、`renderDashboardFocus`、`renderCategoryHeatmap`、`renderPlanInvestmentDonut`。
3. 后端接口：`GET /api/dashboard`，由 `DashboardController.dashboard` 提供。
4. 数据库相关文件：`schema.sql` 中的 `study_plan`、`study_task`、`study_record`；计划投入还依赖 `study_plan.total_study_minutes`、`study_plan.total_study_record_count`。
5. 修改时优先看：先看 `app.html` 的 `dashboardMetrics` 等 DOM ID，再看 `dashboard.js` 的 `loadDashboard` 和渲染函数，最后看 `DashboardController` 的返回字段。
6. 风险等级：中风险。驾驶舱同时展示任务、计划、记录和图表，字段变化容易影响多个面板。

### 学习计划

1. 入口页面：`app.html` 的 `<section id="plans">`、`<section id="planEdit">`、`<section id="planDetail">`。
2. 前端主要文件：`app.js` 的 `loadPlans`、`fillPlanOptions`、`displayPlanName`、`renderAvailablePlanList`、`editPlan`、`deletePlan`、`openPlanDetail`、`loadPlanDetailTasks`。
3. 后端接口：`POST /api/plans`、`GET /api/plans`、`GET /api/plans/{id}`、`PUT /api/plans/{id}`、`DELETE /api/plans/{id}`。
4. 数据库相关文件：`schema.sql` 的 `study_plan` 表，升级脚本 `plan_investment_upgrade.sql`。
5. 修改时优先看：`StudyPlan` 字段、`PlanController`、`app.html` 的 `planForm` / `planEditForm` / `planTable`、`app.js` 的 `loadPlans`。
6. 风险等级：中风险。计划被任务、记录累计投入、轮盘图和详情页共同引用。

### 学习任务

1. 入口页面：`app.html` 的 `<section id="tasks">`、`<section id="taskEdit">`。
2. 前端主要文件：`app.js` / `tasks.js` 的 `loadTasks`、`fillTaskOptions`、`editTask`、`deleteTask`、`taskForm` 提交、`taskEditForm` 提交。
3. 后端接口：`POST /api/tasks`、`POST /api/tasks/batch`、`GET /api/tasks`、`GET /api/tasks/categories`、`GET /api/tasks/{id}`、`PUT /api/tasks/{id}`、`PATCH /api/tasks/{id}/status`、`DELETE /api/tasks/{id}`。
4. 数据库相关文件：`schema.sql` 的 `study_task`、`study_task_category`，升级脚本 `task_category_upgrade.sql`。
5. 修改时优先看：`TaskController.prepareTask` 的校验、`StudyTask` 字段、`StudyTaskServiceImpl` 的分类逻辑、`app.html` 的 `taskForm` 和 `taskTable`。
6. 风险等级：高风险。任务关联计划、记录、日历、统计、批量导入和后台管理。

### 学习记录

1. 入口页面：`app.html` 的 `<section id="records">`。
2. 前端主要文件：`app.js` / `records.js` 的 `loadRecords`、`editRecord`、`deleteRecord`、`recordForm` 提交、`resetRecordForm`。
3. 后端接口：`POST /api/records`、`GET /api/records`、`GET /api/records/task/{taskId}`、`GET /api/records/recent`、`PUT /api/records/{id}`、`DELETE /api/records/{id}`。
4. 数据库相关文件：`schema.sql` 的 `study_record`，升级脚本 `record_reflection_upgrade.sql`。
5. 修改时优先看：`StudyRecord` 字段、`RecordController`、`StudyRecordServiceImpl` 的累计投入逻辑、`app.html` 的 `recordForm` 和 `recordTable`。
6. 风险等级：高风险。记录会影响首页、热力图、轮盘图、统计和计划累计投入。

### 学习反思

1. 入口页面：`app.html` 的学习记录表单，字段 ID 是 `recordProblems`、`recordImprovementPlan`。
2. 前端主要文件：`app.js` / `records.js` 的 `buildRecordAiRequest`、`recordForm` 提交、`renderAiSuggestedActions`。
3. 后端接口：学习记录保存走 `/api/records`，AI 建议走 `/api/ai/generate-improvement`。
4. 数据库相关文件：`schema.sql` 的 `study_record.problems`、`study_record.improvement_plan`，升级脚本 `record_reflection_upgrade.sql`。
5. 修改时优先看：`StudyRecord`、`AiImprovementRequest`、`AiImprovementResponse`、`app.html` 的记录表单、`app.js` / `records.js` 的 AI 按钮逻辑。
6. 风险等级：中风险。反思文本本身简单，但同时连接记录保存和 AI 生成。

### AI 改进建议

1. 入口页面：`app.html` 的 `recordAiImprovementBtn` 按钮、`recordAiSuggestedActionsBox` 建议区域。
2. 前端主要文件：`app.js` / `records.js` 的 `buildRecordAiRequest`、`renderAiSuggestedActions`、`recordAiImprovementBtn` 点击事件。
3. 后端接口：`POST /api/ai/generate-improvement`，由 `AiController.generateImprovement` 调用 `AiServiceImpl.generateImprovement`。
4. 数据库相关文件：AI 请求会带 `planId` 查询 `study_plan`，生成结果最终通常保存到 `study_record.improvement_plan`。
5. 修改时优先看：`AiImprovementRequest`、`AiImprovementResponse`、`AiServiceImpl.buildPrompt`、`AiServiceImpl.fallbackImprovement`、`app.js` / `records.js` 的展示逻辑。
6. 风险等级：中风险。DeepSeek 配置在 `application.yml` 的 `ai.deepseek.*`，未配置 Key 时会走本地兜底建议。

### 学习日历 / 重大事件

1. 入口页面：`app.html` 的 `<section id="calendar">` 和 `<section id="importantNodes">`。
2. 前端主要文件：`calendar.js` 的 `loadCalendar`、`renderCalendar`、`selectCalendarDate`、`renderCalendarDayDetails`、`changeCalendarMonth`，以及 `important-nodes.js` 的 `loadImportantNodes`、`renderImportantNodes`。
3. 后端接口：日历任务接口 `GET /api/calendar?year=...&month=...`；重要节点接口 `GET /api/important-nodes?year=...`、`POST /api/important-nodes`、`PUT /api/important-nodes/{id}`、`DELETE /api/important-nodes/{id}`。
4. 数据库相关文件：`schema.sql` 的 `study_task.deadline` 和 `important_node` 表。
5. 修改时优先看：日历视图先看 `renderCalendar`；重要节点先看 `ImportantNodeController.prepareNode`、`ImportantNode`、`app.html` 的 `importantNodeModal`。
6. 风险等级：高风险。日期范围、跨天事件、任务截止日期和节点展示彼此影响。

### 热力图

1. 入口页面：`app.html` 的 `categoryHeatmapPanel`、`categoryHeatmapFilters`、`categoryHeatmapGrid`、`categoryHeatmapSummary`。
2. 前端主要文件：`dashboard.js` 的 `renderCategoryHeatmap`、`getHeatmapDateRange`、`aggregateRecordsByDate`、`buildHeatmapGrid`、`getHeatmapLevel`、`recordMatchesHeatmapFilter`。
3. 后端接口：主要依赖 `GET /api/records` 加载到 `appState.records`。
4. 数据库相关文件：`schema.sql` 的 `study_record.study_date`、`study_record.duration`，还通过任务关联分类 `study_task.category`。
5. 修改时优先看：`dashboard.js` 的热力图函数，再看 CSS 的 `.heatmap-*`。
6. 风险等级：中风险。主要是前端计算，但数据来源依赖记录和任务分类。

### 轮盘图

1. 入口页面：`app.html` 的 `planInvestmentPanel`、`planInvestmentDonut`、`planInvestmentCenter`、`planInvestmentSummary`、`planInvestmentLegend`。
2. 前端主要文件：`dashboard.js` 的 `renderPlanInvestmentDonut`、`buildPlanInvestmentStats`、`buildAllPlanInvestmentStatsFromRecords`、`planInvestmentColor`、`activePlanInvestmentRange`。
3. 后端接口：主要依赖 `GET /api/plans`、`GET /api/tasks`、`GET /api/records`。
4. 数据库相关文件：`study_plan.total_study_minutes`、`study_plan.total_study_record_count`，以及 `study_record.duration`、`study_task.plan_id`。
5. 修改时优先看：前端轮盘计算函数、`StudyRecordServiceImpl.applyInvestmentDelta`、`StudyPlanMapper.incrementInvestment`、`PlanInvestmentMaintenanceRunner`。
6. 风险等级：高风险。记录新增、修改、删除都会影响累计投入，历史数据回填也在启动维护类里。

### 批量导入

1. 入口页面：任务批量导入在 `app.html` 的 `batchTaskForm`、`batchTaskText`；重要节点批量导入在 `importantNodeBatchForm`、`importantNodeBatchText`。
2. 前端主要文件：任务导入在 `app.js` / `tasks.js` 的 `batchTaskForm` 提交事件、`BATCH_TASK_EXAMPLE`、`insertBatchExampleBtn`、`clearBatchTaskBtn`；重要节点导入在 `important-nodes.js` 的 `validateImportantNodeLine`、`parseImportantNodeBatch`、`importantNodeBatchForm` 提交事件。
3. 后端接口：任务 `POST /api/tasks/batch`；重要节点 `POST /api/important-nodes/batch`。
4. 数据库相关文件：任务写入 `study_task`；重要节点写入 `important_node`；分类校验依赖 `study_task_category`。
5. 修改时优先看：前端解析格式、后端 `TaskController.batchAdd` / `ImportantNodeController.batchAdd`、对应 Entity 和 SQL 约束。
6. 风险等级：高风险。批量导入一旦格式、字段或校验不同步，会整批失败或写入错误数据。

## 五、常见小改动定位表

| 改动 | 优先查看文件/函数 | 风险 |
| --- | --- | --- |
| 改按钮文案 | `app.html`、`admin.html`、`index.html` 中对应按钮文本；部分提示在 `app.js` 的 `TEXT` 常量 | 低风险 |
| 改样式 | 共享样式看 `src/main/resources/static/css/base.css`；登录页看 `auth.css`；普通用户端按 `.dashboard-*`、`.calendar-*`、`.important-node-*` 在 `app.css` 搜索；管理后台按 `.admin-*` 在 `admin.css` 搜索 | 低风险 |
| 字段为空不显示 | `app.js`、`admin.js` 的渲染模板，例如 `loadPlans`、`loadTasks`、`loadRecords`、`renderImportantNodeCard` | 中风险 |
| 改日期格式 | `app.js` 的 `formatLocalDate`、`formatMonthDay`、`formatNodeDateRange`、`formatCompactNodeDate`；后端 Jackson 日期配置在 `application.yml` | 中风险 |
| 改查询条件 | 前端请求参数在 `app.js` / `admin.js`，后端查询条件在对应 Controller 的 `list` 方法 | 中风险 |
| 新增普通字段 | 同步 Entity、Controller 请求体、前端表单、前端列表/详情渲染 | 中风险 |
| 新增数据库字段 | 先看 `schema.sql` 和升级 SQL，再看 Entity、Mapper/Service、Controller、前端表单和展示 | 高风险 |
| 修改 AI 返回结果展示 | `AiImprovementResponse`、`AiServiceImpl`、`app.js` 的 `renderAiSuggestedActions` 和 `recordAiImprovementBtn` 点击事件 | 中风险 |
| 修改图表标题或数据来源 | 标题先看 `app.html`；图表逻辑看 `dashboard.js` 的 `renderCategoryHeatmap`、`renderPlanInvestmentDonut`、`renderMiniChart` 和 `renderStatisticsCharts` | 中风险 |

## 六、风险等级总览

- 低风险：只改静态文案、按钮文字、局部 CSS，且不改变字段名、接口参数、数据库结构。
- 中风险：改前端展示逻辑、筛选条件、日期格式、AI 文案结构、统计展示字段，需要同步确认接口返回数据是否存在。
- 高风险：改数据库字段或约束、批量导入格式、任务/记录/计划关联逻辑、学习记录累计计划投入、重要节点日期范围，必须同时检查数据库层、后端 Java 层、前端页面层。

## 七、本次文档变更

- 本次只新增或更新了文档文件：`CODE_MAP.md`。
- 未修改任何业务代码。
- 未新增数据库迁移。
- 未修改后端接口。
- 未修改前端页面。
## 重要节点关键词规则补充

- 数据库表：`important_node_keyword_rule`，保存每个用户自己的重要节点关键词规则，用于自动计算重要程度。
- 升级脚本：`src/main/resources/db/important_node_keyword_rule_upgrade.sql`。
- 后端接口：`ImportantNodeKeywordRuleController`，路径 `/api/important-node-keyword-rules`，提供当前用户规则的增删改查。
- 后端计算入口：`ImportantNodeController`，新增、编辑、批量导入和列表查询重要节点时都会按事件类型、距离当前日期、当前用户关键词规则计算 `importance`。
- 前端入口：`src/main/resources/static/pages/app.html` 的“重要节点”页面和 `src/main/resources/static/js/important-nodes.js` 的关键词规则表单、列表渲染、批量导入解析。

## 实验二：独立 Vue 前端

- `frontend/index.html` → `frontend/src/main.js` → `App.vue`：登录与学习计划 CRUD。`api.js` 负责 Axios、Session 凭证及 Result 错误处理。
- `frontend/vite.config.js`：独立构建入口；复用原 `static/css/base.css`，不依赖原静态 JS。
- `WebConfig.apiCorsFilter`：API 跨域白名单，`APP_CORS_ALLOWED_ORIGINS` 可配置。
- `docs/experiment-2/`：运行说明、OpenAPI 文档和验证记录。
