# 实验二验证记录

日期：2026-09-21。验证对象为当前工作区（包含用户原有未提交测试修正）。

| 验证 | 实际结果 |
| --- | --- |
| `mvnw.cmd -Dtest=ExperimentCorsTest test`，CORS 实现前 | 3 项中 2 项失败：缺少跨域响应头、预检 403；用于确认问题 |
| `mvnw.cmd test`，实现后 | 57 项，0 失败、0 错误、0 跳过 |
| `mvnw.cmd clean package -DskipTests` | BUILD SUCCESS，生成 `target/study-plan-system-1.0.0.jar` |
| `node --test frontend/src/api.test.js` | 3 项全部通过 |
| `node --check`，api.js、main.js、vite.config.js | 全部通过 |
| `pnpm --dir frontend build`，审查修复后 | 成功，66 个模块，输出 `frontend/dist/` |
| OpenAPI JSON 解析与内部引用检查 | 8 个操作，19 个引用均可解析；不是完整 OpenAPI 规范校验器 |
| `git diff --check` | 通过 |
| 独立代码审查 | 发现编辑清空日期无法生效的问题；已在前端阻止该操作并提示，不改后端更新语义 |
| 真实 MySQL + 浏览器人工联调、界面视觉验收 | 未执行，按 README 中步骤完成；不能将 MockMvc 和 Axios 模拟测试当作实际联调 |
| 远程 GitHub 同步 | 未完成推送，不声称已有云端备份 |

MockMvc 覆盖：可信来源 PUT 预检无需登录、真实请求 401 且含 CORS 头、不可信来源拒绝、已登录列表、创建计划从 Session 取 userId 且清空输入 id、空名称业务失败、错误日期 400、不可见计划查询失败。服务层使用 Mock，不连接数据库。

Axios 测试覆盖：凭证开关及 Result 解包、HTTP 200 业务失败、HTTP 401 标记。

环境处理：通过已有 JDK 21 设置当前命令的 JAVA_HOME；未修改系统环境变量。前端使用已锁定 pnpm-lock.yaml，明确允许 esbuild 安装脚本。

未执行数据库脚本，不改变表结构、现有控制器/响应结构、登录或所有权检查、原页面及全局样式。新增前端只覆盖实验二所需的登录和学习计划功能。
