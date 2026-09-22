# 实验二：前后端分离与 RESTful API

## 完成范围

后端沿用 Spring Boot @RestController 提供 RESTful JSON。独立 frontend 使用 Vue 3 单文件组件和 Axios，前两阶段覆盖：

- 登录、注册、退出和刷新后的 Session 恢复。
- 学习计划列表、详情、新增、修改、删除。
- 学习任务 CRUD、重复与提醒设置、计划/状态/优先级/日期筛选和批量导入。
- 学习记录 CRUD、任务关联和 AI 改进建议。
- HTTP 业务失败、401、超时和后端不可达提示。
- 原登录页和学习计划页的主要结构、文案、主题及响应式导航。

业务响应保持 {success,message,data}。Vue 只提交允许编辑的字段，不提交 userId 或服务端累计值。日历、重要节点、统计和管理后台仍从“其他功能（原系统）”进入，尚未迁移为 Vue。

后端已有的 CORS Filter 在登录拦截器前处理预检。默认允许 http://localhost:5173 和 http://localhost:4173，支持 Cookie 凭证；真实请求仍经过原登录、权限和数据所有权检查。

Vue 复用原 base.css、auth.css 和 app.css，不加载原静态 JavaScript。模板插值会转义用户文本。“记住用户名”只保存用户名，不保存密码或 Session 标识。

## 本地运行

需要 Java 17+、Node.js 20.19+ 或 22.12+，以及项目原有 MySQL 数据库和 DB_PASSWORD 环境变量。不要在已有数据库上执行包含 DROP TABLE 的初始化 SQL。

先在仓库根目录启动后端：

~~~powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.12.1'
.\mvnw.cmd clean package -DskipTests
java -jar target/study-plan-system-1.0.0.jar
~~~

再开一个终端启动 Vue：

~~~powershell
cd D:\websitedevelopment\frontend
npm run dev
~~~

打开 http://localhost:5173。默认演示账号为 student / 123456；也可以在 Vue 注册页创建普通用户。前后端统一使用 localhost，不要把其中一端改成 127.0.0.1。

如果 frontend/node_modules 不存在，先按 pnpm-lock.yaml 安装依赖：

~~~powershell
corepack enable
pnpm install --frozen-lockfile
~~~

生产构建可在 frontend 目录执行 npm run build，再以 npm run preview 查看。产物在 frontend/dist，前后端可以分别部署。Bootstrap CSS 使用 CDN，离线环境需要自行托管。

后端地址可在构建前写入 frontend/.env.local：

~~~dotenv
VITE_API_BASE_URL=https://api.example.com/api
~~~

后端跨域白名单由 APP_CORS_ALLOWED_ORIGINS 配置完整 Origin，多个值使用逗号分隔。修改构建时的 API 地址后需要重新构建。

## 接口与字段核对

openapi.json 是 OpenAPI 3.0.3 文档，覆盖当前 Vue 前两阶段实际调用的 20 个操作：认证、计划、任务、任务分类与批量导入、学习记录和 AI 改进建议。

- DB plan_name 对应 Java/API/Vue planName，上限 100；goal 上限 255。
- 日期格式为 yyyy-MM-dd；新建计划可发送 null。现有 ORM 更新策略无法可靠清空已设置日期，因此编辑表单会阻止这种误导性的保存。
- 新表单只允许“未开始、进行中、已完成、已取消”。读取旧数据时保留既有历史状态。
- HTTP 200 仍可能表示业务失败，Axios 会检查 success；401 会清除私有界面状态。
- 删除计划会同时删除其任务和学习记录，界面会在请求前确认。
- 注册界面只提交 username、realName 和 password；角色、状态等由后端规则决定。

接口文档由 AI 辅助整理并已按 Controller、Entity 和 SQL 字段核对。课程材料要求的“人工核对”仍应由提交者最终查看。

## 已执行的联调

2026-09-22 使用真实 Edge、Spring Boot 和 MySQL 完成以下联调：

1. 错误密码、真实账号登录、刷新保持 Session 和退出。
2. 使用临时注册账号完成计划新增、读取、修改和删除；随后精确清理临时账号，未修改原用户数据。
3. 验证计划字段往返数据库，用户输入不会生成 HTML 节点。
4. 验证 CORS、会话失效、后端不可达提示、390px 移动布局和管理员跳转。
5. 验证新建草稿在页面切换后保留，保存期间导航禁用；模拟保存测试不写数据库。
6. 验证任务和记录页读取真实数据、Q1 筛选、任务草稿跨页保留，以及未选任务时 AI 请求被阻止。

命令与结果见 verification.md。

## Git 与回退

初始实验二提交为 cf14ef7，第一阶段提交为 f180e03；第二阶段位于分支 codex/vue-phase2-tasks-records。仓库原有未提交文件继续保留，不属于本次迁移。

Git 只能撤销代码，不能恢复数据库中被删除的数据。撤销已提交迁移时先检查 git status，然后使用 git revert <提交号>；不要对包含个人未提交工作的仓库使用 git reset --hard 或 git clean。