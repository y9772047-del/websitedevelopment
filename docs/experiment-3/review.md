# 实验三代码审查记录

审查方式：Codex 辅助审查与本地验证。此记录不是另一位 GitHub 用户的人工批准。

| 发现 | 处理 | 验证依据 |
| --- | --- | --- |
| 旧 CI 对 `static/js/*.js` 执行语法检查，但当前运行入口已迁为 Vue 构建产物 | 改为按已跟踪的 `frontend/pnpm-lock.yaml` 安装、测试、构建，再运行 Maven | 本地 Vue 测试、构建与 Maven 验证；远程结果以 PR Checks 为准 |
| Vue 源码和 Spring Boot 中的哈希资源可能不同步 | 构建后提交 `static/index.html` 和引用的 `assets/`；`VueDistributionTest` 检查资源存在及旧目录不存在 | 本地 JAR 资源检查与静态资源测试 |
| 学习记录页在 AI 建议等待期间仍允许刷新、编辑、删除 | 等待期间禁用按钮，并在操作入口阻止并发 | 浏览器中截住请求验证，前端测试与 Maven 回归 |
| 本机配置可能含凭据或个人文件 | 只提交环境变量引用、示例配置和必要项目文件；忽略 `.env`、私钥及本机代理脚本 | 暂存文件列表及敏感信息检查 |
| Windows 换行可能影响 Linux Maven Wrapper | `.gitattributes` 固定 `mvnw` 为 LF，CI 通过 `bash ./mvnw` 调用 | 暂存内容检查；远程结果以 Actions 为准 |

工作流只授予 `contents: read`，使用普通 `pull_request` 事件，不注入生产凭据，也不跳过测试。自动测试覆盖当前单元测试和静态资源检查，不等于真实数据库、所有浏览器路径或外部 AI 服务的完整验收。如课程要求独立评审，应请同学或教师在 PR 上留下真实意见。
