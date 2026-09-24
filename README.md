# 大学生学习计划管理系统 MVP

本项目是软件工程课程项目 MVP，技术栈为 Spring Boot + Maven + MySQL + MyBatis-Plus，前端使用 Vue 3、Vite、Axios 和 Bootstrap 5 CDN。Vue 构建资源由 Spring Boot 统一提供。

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
2. 仅在全新数据库中执行 `src/main/resources/db/schema.sql`；该初始化脚本包含 `DROP TABLE`，已有数据时不要重新执行。
3. 检查 `src/main/resources/application.yml` 中的数据库配置：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/study_plan_system?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
```

4. 已提交的 `src/main/resources/static` 包含 Vue 构建结果；如修改了 `frontend/`，先在该目录运行 `pnpm install --frozen-lockfile` 和 `pnpm build`。`pnpm build` 会更新 Spring Boot 的静态资源。`npm install` 和 `npm run build` 也可用于本地构建。
5. 使用 IntelliJ IDEA 打开项目，等待 Maven 依赖下载完成。
6. 设置环境变量 `DB_PASSWORD`，然后运行 `com.example.studyplan.StudyPlanSystemApplication`。
7. 浏览器访问 `http://localhost:8080`。开发模式可在 `frontend/` 运行 `pnpm dev`，访问 `http://localhost:5173`；API 默认连接 `http://localhost:8080/api`。

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

- 登录、注册、退出登录，使用 Session 保存登录状态。
- 学习计划基本增删改查。
- 学习任务基本增删改查，任务关联学习计划。
- 学习记录基本增删改查，记录关联学习任务，支持填写问题与反思，并根据学习内容、完成情况和问题与反思生成改进计划。
- AI 学习改进措施助手集成在学习记录模块中，生成结果由学生确认后保存到对应学习记录。
- 学习记录页可根据最近 7 天记录生成 AI 周报；使用时需要设置 `DEEPSEEK_API_KEY`。
- 首页仪表盘展示计划数、任务数、已完成任务数、延期任务数、最近任务、最近学习记录。
- 学习日历按学习记录日期展示真实学习情况，并可同时显示重要节点。
- 重要节点独立管理考试、截止、比赛、汇报等一年内的重要学习时间点，支持单个新增、编辑、删除和批量导入。
- 学习统计已整合在 Vue 首页仪表盘，独立导航入口已移除。
- 主题皮肤中心支持 5 套内置主题，并保存到 localStorage。

## 数据库表

- `user`：用户表。
- `study_plan`：学习计划表。
- `study_task`：学习任务表。
- `study_record`：学习记录表，包含学习过程、问题与反思、改进计划。
- `important_node`：重要节点表，独立保存考试、截止、比赛、汇报等学习时间点。

## 实验文档

- [实验二：Vue 前后端分离与 RESTful API](docs/experiment-2/README.md)
- [实验三：Git 协作与持续集成](docs/experiment-3/README.md)

## 说明

MySQL 连接口令通过 `DB_PASSWORD` 环境变量提供，不写入 `application.yml`。这与登录用户的密码存储是两回事：当前课程示例的 `user.password` 仍由后端直接保存并匹配，初始化 SQL 也使用演示密码。正式部署前应设计存量账号迁移，改用 BCrypt 等密码哈希，并补充更严格的参数校验和异常处理。
