# 实验二：前后端分离与 RESTful API

## 完成范围

后端沿用 Spring Boot @RestController 提供 RESTful JSON。Vue 3 单文件组件通过 Axios 调用接口；开发时可独立运行 Vite，最终构建由 Spring Boot 在同一端口提供。当前覆盖：

- 登录、注册、退出和刷新后的 Session 恢复。
- 学习计划列表、详情、新增、修改、删除。
- 学习任务 CRUD、重复与提醒设置、计划/状态/优先级/日期筛选和批量导入。
- 学习记录 CRUD、任务关联、AI 改进建议和 AI 周报。
- 首页仪表盘展示加粗的关键指标、任务完成率、热力图和学习投入分布；学习日历按学习记录和重要节点展示每日详情。
- 重要节点 CRUD、批量导入、计划/任务关联和关键词规则管理。
- 学生意见反馈、反馈处理结果查看和五套主题皮肤。
- 管理后台首页、用户管理、全局任务管理、反馈处理和管理员主题。
- HTTP 业务失败、401、403、超时和后端不可达提示。
- 原登录页和学习计划页的主要结构、文案、主题及响应式导航。

业务响应保持 {success,message,data}。Vue 只提交允许编辑的字段，不提交 userId、importance 或服务端累计值。普通用户和管理员共用 Vue 登录入口，登录后按后端返回的 role 显示各自功能。

后端已有的 CORS Filter 在登录拦截器前处理预检。默认允许 http://localhost:5173 和 http://localhost:4173，支持 Cookie 凭证；真实请求仍经过原登录、权限和数据所有权检查。

Vue 复用原 base.css、auth.css、app.css 和 admin.css，不加载原静态 JavaScript。模板插值会转义用户文本。“记住用户名”只保存用户名，不保存密码或 Session 标识。

## 本地运行

需要 Java 17+、Node.js 20.19+ 或 22.12+，以及项目原有 MySQL 数据库和 DB_PASSWORD 环境变量。不要在已有数据库上执行包含 DROP TABLE 的初始化 SQL。

确认 `JAVA_HOME` 指向 JDK 17 或更新版本，并在仓库根目录启动后端：

~~~powershell
.\mvnw.cmd clean package -DskipTests
java -jar target/study-plan-system-1.0.0.jar
~~~

需要前端热更新时，再开一个终端启动 Vite：

~~~powershell
cd frontend
npm run dev
~~~

统一部署访问 http://localhost:8080；开发模式访问 http://localhost:5173。默认演示账号为 student / 123456；也可以在 Vue 注册页创建普通用户。开发模式的前后端统一使用 localhost，不要把其中一端改成 127.0.0.1。

如果 frontend/node_modules 不存在，先按 pnpm-lock.yaml 安装依赖：

~~~powershell
corepack enable
pnpm install --frozen-lockfile
~~~

在 `frontend/` 执行 `npm run build` 后，产物直接写入 `src/main/resources/static/`；随后重新打包 Spring Boot，即可从同一服务访问 Vue 和 `/api`。构建后的 `index.html` 与带哈希的资源文件须一起提交。Bootstrap CSS 使用 CDN，离线环境需要自行托管。

后端地址可在构建前写入 frontend/.env.local：

~~~dotenv
VITE_API_BASE_URL=https://api.example.com/api
~~~

后端跨域白名单由 APP_CORS_ALLOWED_ORIGINS 配置完整 Origin，多个值使用逗号分隔。修改构建时的 API 地址后需要重新构建。

## 接口与字段核对

openapi.json 是 OpenAPI 3.0.3 文档，覆盖后端现有的 49 个 HTTP 操作，包括 Vue 当前使用的接口：认证、计划、任务、记录、AI 改进建议与周报、仪表盘、统计、日历、重要节点、关键词规则、学生反馈及管理后台。

- DB plan_name 对应 Java/API/Vue planName，上限 100；goal 上限 255。
- 日期格式为 yyyy-MM-dd；新建计划可发送 null。现有 ORM 更新策略无法可靠清空已设置日期，因此编辑表单会阻止这种误导性的保存。
- 新表单只允许“未开始、进行中、已完成、已取消”。读取旧数据时保留既有历史状态。
- HTTP 200 仍可能表示业务失败，Axios 会检查 success；401 会清除私有界面状态。
- 任务状态 PATCH 只接收 status；后端目前未校验状态枚举，文档按现有行为标明此边界。不存在或不属于当前用户的任务返回 HTTP 200 且 success=false。
- 删除计划会同时删除其任务和学习记录，界面会在请求前确认。
- 注册界面只提交 username、realName 和 password；角色、状态等由后端规则决定。

接口文档由 AI 辅助整理并已按 Controller、Entity 和 SQL 字段核对。课程材料要求的“人工核对”仍应由提交者最终查看。

## 已执行的联调

2026-09-22 至 2026-09-23 使用真实 Edge、Spring Boot 和 MySQL 完成以下联调：

1. 错误密码、真实账号登录、刷新保持 Session 和退出。
2. 使用临时注册账号完成计划新增、读取、修改和删除；随后精确清理临时账号，未修改原用户数据。
3. 验证计划字段往返数据库，用户输入不会生成 HTML 节点。
4. 验证 CORS、会话失效、后端不可达提示、390px 移动布局和管理员 Vue 入口。
5. 验证新建草稿在页面切换后保留，保存期间导航禁用；模拟保存测试不写数据库。
6. 验证任务和记录页读取真实数据、Q1 筛选、任务草稿跨页保留，以及未选任务时 AI 请求被阻止。
7. 验证首页指标与图表、统计分布、日历月份与日期详情、重要节点及关键词规则读取，并检查 390px 移动导航。
8. 验证学生反馈和主题，以及管理员首页、用户、任务、反馈和主题页面。

命令与结果见 verification.md。

## Git 与回退

原始 HTML 基线为 `96ea080`，完整 Vue 功能提交为 `e582f34`，Spring Boot 统一提供 Vue 的整合提交为 `d1f4f63`。旧静态页面与脚本保留在 Git 历史中；仓库原有未提交文件不属于本次迁移。

Git 只能撤销代码，不能恢复数据库中被删除的数据。撤销已提交迁移时先检查 git status，然后使用 git revert <提交号>；不要对包含个人未提交工作的仓库使用 git reset --hard 或 git clean。