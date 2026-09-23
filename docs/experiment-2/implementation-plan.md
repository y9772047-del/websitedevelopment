# 实验二实施方案

已获用户确认执行。复用现有 Spring Boot + MyBatis-Plus + MySQL 后端，以 Vue 3 + Axios + Vite 构建独立前端。

迁移阶段：

1. 第一阶段：登录、注册、会话恢复和学习计划 CRUD、详情。
2. 第二阶段：任务 CRUD、筛选、重复与提醒、批量导入；学习记录 CRUD 与 AI 改进建议。
3. 第三阶段：首页仪表盘、学习统计、学习日历；重要节点 CRUD、批量导入、计划/任务关联和关键词规则。
4. 第四阶段：管理后台，完成后统一入口。

实施约束：

- api.js 统一处理 Result、Session 凭证、HTTP、超时和网络错误。
- Vue 复用原 base.css、auth.css 和 app.css，不加载原静态 JavaScript。
- 前端 payload 使用字段白名单并在发送前校验日期、固定选项和关联编号。
- OpenAPI 按 Controller、Entity 与 SQL 字段人工核对。
- 每阶段运行前端测试、语法检查、生产构建和 Maven 验证，并使用真实浏览器与 MySQL 做联调。
- 数据库不变，无迁移 SQL；不修改原静态页面、权限检查、API 路径或响应结构；不启动长期服务。