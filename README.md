# 大学生学习计划管理系统 MVP

> 课程提交版：环境配置、数据库导入、启动命令和演示账号见 [SUBMISSION.md](SUBMISSION.md)。


本项目是软件工程课程项目 MVP，技术栈为 Spring Boot + Maven + MySQL + MyBatis-Plus，前端使用 Vue 3、Vite、Axios 和 Bootstrap 5 CDN。Vue 可独立构建部署，也可由 Spring Boot 同源提供构建资源。

## 本次模块调整说明

原“学习反思”模块已从学生端功能范围中移除。原因是学习记录本身已经包含“遇到的问题”，继续单独维护反思记录会让学生重复填写问题总结，使用流程不够自然。

调整后的流程为：

1. 学生在“学习记录”中填写学习内容、完成情况和遇到的问题。
2. 系统根据学习记录生成改进计划。
3. 学生确认或修改改进计划后保存学习记录。
4. 问题与反思、改进计划都保存到本次学习记录中，后续可以按任务查看学习记录时间线。

因此，当前系统保留“学习任务 + 学习记录”的闭环，不再提供独立学习反思入口和独立反思数据表作为主要业务模块。

## 运行步骤

1. 确认本机 MySQL 已启动。
2. 仅在全新数据库中执行 `src/main/resources/db/schema.sql`；该初始化脚本包含 `DROP TABLE`，已有数据时不要重新执行。已有数据库升级实验四时，先备份数据库，再执行 `src/main/resources/db/experiment4_auth_upgrade.sql`；首次启动会将旧用户的明文密码转换为 BCrypt 哈希。
3. 检查 `src/main/resources/application.yml` 中的数据库配置：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/study_plan_system?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
```

4. 已提交的 `src/main/resources/static` 包含 Vue 构建结果；如修改了 `frontend/`，先在该目录运行 `pnpm install --frozen-lockfile` 和 `pnpm build`。`pnpm build` 会更新 Spring Boot 的静态资源。`npm install` 和 `npm run build` 也可用于本地构建。
5. 使用 IntelliJ IDEA 打开项目，等待 Maven 依赖下载完成。
6. 设置环境变量 `DB_PASSWORD` 和至少 32 字节的 `JWT_SECRET`，然后运行 `com.example.studyplan.StudyPlanSystemApplication`。也可以在项目根目录使用 PowerShell 启动：

```powershell
$env:DB_PASSWORD = "你的本机 MySQL 密码"
$env:JWT_SECRET = "请替换成至少32字节的随机密钥"
# 需要 AI 目标复盘时再设置：
$env:DEEPSEEK_API_KEY = "你的 DeepSeek API Key"
.\mvnw.cmd spring-boot:run
```
7. 浏览器访问 `http://localhost:8080`。开发模式可在 `frontend/` 运行 `pnpm dev`，访问 `http://localhost:5173`；API 默认连接 `http://localhost:8080/api`。独立生产构建可用 `pnpm build:standalone` 生成 `frontend/dist/`，需在构建前设置 `VITE_API_BASE_URL`；前端和 API 必须使用同一主机名以共享 CSRF Cookie（端口可不同），生产环境推荐同一主机名的 HTTPS 反向代理。命令与跨域配置见 [实验二说明](docs/experiment-2/README.md)。

## 测试账号

全新数据库初始化脚本会插入两个仅供本地演示的账号：`student / 123456` 和 `admin / admin123`。已有数据库的账号以实际数据为准。

## 页面入口

- 登录页：`http://localhost:8080`
- 学生端和管理端：登录后由 Vue 在同一页面按账号角色显示，入口都是 `http://localhost:8080`。
- 进入系统后，通过顶部导航访问：
  - 首页
  - 学习计划
  - 学习任务
  - 学习记录
  - 学习日历
  - 重要节点
  - 意见反馈
  - 主题皮肤

## 已实现功能

- 登录、注册、退出登录，使用 BCrypt 密码哈希、Spring Security 权限控制和 `HttpOnly` Cookie 中的短期 JWT；写操作使用 CSRF 令牌。
- 学习计划基本增删改查。
- 学习计划详情展示左到右的目标树；任务可独立收放，同时展开多个任务后查看各自的学习记录。
- 学习计划详情可根据至少 2 条关联记录按需生成 AI 目标复盘；使用时需要设置 `DEEPSEEK_API_KEY`。
- 学习任务基本增删改查，任务关联学习计划。
- 学习记录基本增删改查，记录关联学习任务，支持填写问题与反思，并根据学习内容、完成情况和问题与反思生成改进计划。
- AI 学习改进措施助手集成在学习记录模块中，生成结果由学生确认后保存到对应学习记录。
- 学习记录页可根据最近 7 天记录生成 AI 周报；使用时需要设置 `DEEPSEEK_API_KEY`。
- 首页仪表盘展示计划数、任务数、已完成任务数、延期任务数、最近任务、最近学习记录。
- 学习日历按学习记录日期展示真实学习情况，并可同时显示重要节点。
- 重要节点独立管理考试、截止、比赛、汇报等一年内的重要学习时间点，支持单个新增、编辑、删除和批量导入。
- Vue 首页仪表盘保留计划、任务、完成和逾期指标、任务完成率、热力图与学习投入分布；重复的学习统计区块和独立导航入口已移除。
- 主题皮肤中心支持 5 套内置主题，并保存到 localStorage。

## 数据库表

- `user`：用户表。
- `role`、`permission`、`user_role`、`role_permission`：角色、权限及其关联。
- `study_plan`：学习计划表。
- `study_task`：学习任务表。
- `study_record`：学习记录表，包含学习过程、问题与反思、改进计划。
- `important_node`：重要节点表，独立保存考试、截止、比赛、汇报等学习时间点。

## 实验文档

- [实验二：Vue 前后端分离与 RESTful API](docs/experiment-2/README.md)
- [实验三：Git 协作与持续集成](docs/experiment-3/README.md)
- [实验四：登录、权限与 AI 安全](docs/experiment-4/README.md)
- [助教演示：GitHub 操作与讲解指南](docs/experiment-3/demo-guide.md)

## 说明

MySQL 连接口令和 JWT 签名密钥分别通过 `DB_PASSWORD`、`JWT_SECRET` 环境变量提供，不写入仓库。`user.password` 只保存 BCrypt 哈希。未设置 `JWT_SECRET` 时开发环境会生成仅本次进程有效的随机密钥，重启后须重新登录；正式部署必须设置稳定的随机密钥并使用 HTTPS。
