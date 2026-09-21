# 大学生学习计划管理系统 MVP

本项目是软件工程课程项目 MVP，技术栈为 Spring Boot + Maven + MySQL + MyBatis-Plus，前端使用 HTML + CSS + JavaScript + Bootstrap 5 CDN。

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
2. 在 MySQL 中执行 `src/main/resources/db/schema.sql`。
3. 检查 `src/main/resources/application.yml` 中的数据库配置：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/study_plan_system?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
```

4. 使用 IntelliJ IDEA 打开项目，等待 Maven 依赖下载完成。
5. 运行 `com.example.studyplan.StudyPlanSystemApplication`。
6. 浏览器访问 `http://localhost:8080`。

## 测试账号

数据库 SQL 会自动插入一个测试账号：



## 页面入口

- 登录页：`http://localhost:8080`
- 系统主页：`http://localhost:8080/pages/app.html`
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
- 首页仪表盘展示计划数、任务数、已完成任务数、延期任务数、最近任务、最近学习记录。
- 学习日历按学习记录日期展示真实学习情况，并可同时显示重要节点。
- 重要节点独立管理考试、截止、比赛、汇报等一年内的重要学习时间点，支持单个新增、编辑、删除和批量导入。
- 学习统计页面代码暂时保留，但当前顶部导航不显示该入口。
- 主题皮肤中心支持 5 套内置主题，并保存到 localStorage。

## 数据库表

- `user`：用户表。
- `study_plan`：学习计划表。
- `study_task`：学习任务表。
- `study_record`：学习记录表，包含学习过程、问题与反思、改进计划。
- `important_node`：重要节点表，独立保存考试、截止、比赛、汇报等学习时间点。

## 说明

本项目为了课程演示简化了密码处理，数据库中密码为明文保存。正式项目应使用 BCrypt 等方式加密存储密码，并补充更严格的参数校验和异常处理。
