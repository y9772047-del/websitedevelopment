# 实验二实施方案

已获用户“确认执行”。复用现有 Spring Boot + MyBatis-Plus + MySQL 后端，新增 Vue 3 + Axios + Vite 独立前端，范围为已有账号登录与学习计划 CRUD。

1. 保存修改前文件快照并创建实验分支，保留原有未提交改动。
2. 用 MockMvc 覆盖跨域预检、非白名单来源、未登录请求；新增先于 MVC 拦截器执行的 CORS Filter，保持鉴权和接口语义。
3. 创建 frontend/：api.js 统一处理 Result 和 HTTP 错误；App.vue 提供登录、查询、编辑、新增、删除和加载状态；复用现有 base.css。
4. 依据 PlanController、AuthController、StudyPlan、Result 和 schema.sql 编写 OpenAPI 文档及部署说明。
5. 运行后端测试、Maven 打包、前端测试与构建；记录无法实际完成的数据库或浏览器联调。

数据库不变，无迁移 SQL；不修改原静态页面、权限检查、API 路径或响应结构。不启动长期服务。
