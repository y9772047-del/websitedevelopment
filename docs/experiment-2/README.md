# 实验二：前后端分离与 RESTful API

## 完成范围

后端沿用 `@RestController` 的 `/api/auth` 和 `/api/plans` JSON 接口。新增 `frontend/`，使用 Vue 3 单文件组件与 Axios 实现已有账号登录、会话恢复、计划列表、详情编辑、新增、修改、删除、退出。业务响应保持 `{success,message,data}`。

`WebConfig.apiCorsFilter` 在 MVC 登录/管理员拦截器前处理 CORS 预检，真实请求仍进入原鉴权和所有权检查。默认只允许 `http://localhost:5173` 和 `http://localhost:4173`，支持 Cookie 凭证；禁止通配符来源。

复用原 Bootstrap 5.3.3 CDN 和 `base.css` 样式，Vue 插值自动转义文本。新前端没有共享全局变量，不依赖旧 app.js，原页面和脚本版本不变。

## 本地运行与独立构建

需要 Java 17+、Node 20.19+ 或 22.12+、pnpm，以及项目原有 MySQL 数据库、已有账号和 DB_PASSWORD 环境变量。不要在已有数据库上执行会 DROP TABLE 的 schema.sql。

后端在仓库根目录：

```powershell
# 本机验证使用的 JDK 路径；其他机器按实际安装位置设置
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.12.1'
.\mvnw.cmd clean package -DskipTests
java -jar target/study-plan-system-1.0.0.jar
```

前端另开终端：

```powershell
cd frontend
pnpm install --frozen-lockfile
pnpm dev
```

打开 http://localhost:5173，用现有系统账号登录。前端直连 http://localhost:8080/api，不用开发代理，因此会实际触发跨域预检。前后端统一使用 localhost，不混用 127.0.0.1。

生产构建和本地预览：

```powershell
cd frontend
pnpm build
pnpm preview
```

预览地址 http://localhost:4173；产物为 `frontend/dist/`，由独立静态服务器托管，后端为独立 JAR。HTML/CSS 中的 Bootstrap CDN 需要网络。

后端地址可在构建前通过 `frontend/.env.local` 配置：

```dotenv
VITE_API_BASE_URL=https://api.example.com/api
```

后端白名单通过 `APP_CORS_ALLOWED_ORIGINS` 设置为前端完整 Origin（协议、域名、端口，无路径），多个值逗号分隔。例如 `https://study.example.com`。构建后的后端地址不能靠修改服务器环境变量替换，需重新构建前端。

生产部署建议前后端使用同站点 HTTPS 子域；不同站点部署还涉及 SameSite/第三方 Cookie 策略，本实验没有修改原 Session Cookie 设置。不得以关闭鉴权解决 Cookie 问题。

## 接口文档与字段核对

`openapi.json` 是 OpenAPI 3.0.3 文档，可导入 Swagger Editor 或 Apifox。覆盖本实验使用的 8 个操作。文档由 AI 辅助编写并按代码逐字段核对；课程要求的人工核对仍须由提交者完成，不能将 AI 核对冒称人工核对。

- DB `plan_name` 对应 Java/API/Vue `planName`，数据库上限 100；`goal` 上限 255。
- 日期为 `yyyy-MM-dd`；前端空日期发送 null，不提交 userId、累计投入等服务端字段。
- 计划状态使用现有四个选项。后端当前只强制检查名称非空；长度、日期顺序及状态选择在新表单约束，未改变原后端验证规则。
- HTTP 200 也可能业务失败，Axios 必须检查 success。HTTP 401 清理界面登录状态；网络失败、超时、400/500 显示错误。
- 修改时原 ORM 对 null 的处理会保留旧日期；新前端明确阻止清空已设置日期，避免显示保存成功但未生效。删除计划同时删除关联任务和记录，确认框明确告知。

## 联调步骤与验收记录

1. 启动原后端及 Vue 前端，浏览器 Network 检查 API JSON 和不同端口。
2. 错误密码登录应显示错误；正确密码登录后检查 Cookie，刷新页面仍保持登录。
3. 新增一个专用实验计划，查询、修改，再删除；观察 POST/GET/PUT/DELETE。勿删除正式数据。
4. 检查 OPTIONS 预检 200、精确 Allow-Origin 和 Allow-Credentials=true。
5. 退出后调用 /api/plans 应为 401；错误日期 400；空名称业务失败；其他用户计划不可读写。
6. 后端停止时刷新列表应显示网络错误；恢复后可重试。

自动化结果见 `verification.md`。MockMvc/模拟响应验证与真实数据库、浏览器联调分开记录；未运行的步骤不得写“已通过”。

## Git 与回退

实验分支：`codex/experiment-2-rest-vue`；起点提交：`96ea080`。原有实验三、CI 和测试等未提交改动保留。

修改前文件快照：`C:\Users\yl\AppData\Local\Temp\study-plan-before-experiment2-20260921-193500`。快照包括跟踪文件及未忽略的新文件（不含 Word 临时锁文件），不包括数据库、忽略的本地配置或 .git，不能作为数据库备份；临时目录可能被系统清理。

未推送到 GitHub 时只有本地保护。若需撤销本次已提交代码，先检查 `git status`，再对本次实验提交使用 `git revert <提交号>`。不要使用 `git reset --hard` 或 `git clean` 清除现有未提交成果。

## 参考

- [Spring MVC CORS](https://docs.spring.io/spring-framework/reference/web/webmvc-cors.html)
- [Vite 构建与部署](https://vite.dev/guide/static-deploy)
