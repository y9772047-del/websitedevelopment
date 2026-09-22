# 实验二实施方案

已获用户“确认执行”。复用现有 Spring Boot + MyBatis-Plus + MySQL 后端，新增 Vue 3 + Axios + Vite 独立前端。第一阶段覆盖登录、注册、会话恢复和学习计划；第二阶段覆盖任务 CRUD/筛选/批量导入、学习记录 CRUD 与 AI 改进建议。日历、重要节点、统计和管理后台继续由原系统提供。

1. 保存修改前文件快照并创建实验分支，保留原有未提交改动。
2. 用 MockMvc 覆盖跨域预检、非白名单来源、未登录请求；新增先于 MVC 拦截器执行的 CORS Filter，保持鉴权和接口语义。
3. 创建 `frontend/`：`api.js` 统一处理 Result、HTTP、超时和网络错误；Vue 组件恢复原登录页与学习计划页的结构和样式，提供注册、登录、查询、详情、编辑、新增、删除及加载状态。
4. 依据 `AuthController`、`PlanController`、`TaskController`、Entity、Result 和 SQL 字段编写 OpenAPI 文档及部署说明。
5. 运行前端单元测试、语法检查、生产构建和 Maven 打包；使用真实浏览器、Spring Boot 与 MySQL 验证会话、CORS、CRUD、响应式布局及错误边界。

数据库不变，无迁移 SQL；不修改原静态页面、权限检查、API 路径或响应结构。不启动长期服务。
