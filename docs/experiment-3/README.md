# 实验三：Git 协作开发与持续集成

本实验以[大学生学习计划管理系统](https://github.com/y9772047-del/websitedevelopment)为例，保存实验二的 Vue 整合结果，通过分支、Pull Request、代码审查和 GitHub Actions 验证后合入主分支。

## 提交与运行范围

当前前端源码在 `frontend/`。`pnpm build` 把 Vue 页面、JavaScript 和 CSS 构建到 `src/main/resources/static/index.html` 与 `assets/`，Spring Boot 从 `/` 提供页面；旧手写 `static/pages/`、`static/js/`、`static/css/` 不参与运行。构建产物仍然是浏览器必需的 JS/CSS 文件，应与源码修改一起提交，以便直接运行后端 JAR。

本次工作使用 `codex/vue-final-integrated` 隔离分支。`git add` 选择要保存的文件，`git commit` 在本地记录版本，`git push` 上传分支；Pull Request 展示相对 `main` 的差异和自动检查，检查通过后才合并。旧 HTML 版本可从 Git 历史查阅，不需要保留为当前运行入口。

## 配置文件

- `.github/workflows/ci.yml`：主分支和 PR 的自动验证。
- `.github/pull_request_template.md`：说明改动、验证、影响及 AI 使用。
- `.gitattributes`：固定 Maven Linux 脚本的 LF 换行。
- `.gitignore`：排除构建缓存、IDE、本机环境文件、密钥等。
- `docs/experiment-3/review.md`：本轮审查及验证依据。
- `docs/experiment-3/merge-practice.md`：合并冲突的独立练习说明，不代表已经发生冲突。

`.env`、本机数据库口令、私钥和个人课程文件不应提交。`frontend/.env.example` 与 `src/main/resources/application.yml` 可以提交，因为只包含示例或环境变量引用；提交前仍要检查实际内容。数据库连接口令通过 `DB_PASSWORD` 注入。课程示例中的登录用户密码仍按现有逻辑保存，不能把两者混为一谈。

## CI 验证

工作流在 Ubuntu 的 JDK 17、21 矩阵中使用 Node 22 和 pnpm 11.19.0，依据已跟踪的 `frontend/pnpm-lock.yaml` 执行 `pnpm install --frozen-lockfile`，接着运行 `pnpm test`、`pnpm build`，最后运行 Maven `clean verify`。这样 Maven 的静态资源测试和打包检查的是本次 Vue 构建结果，而不是已删除的旧脚本目录。成功时保存 JDK 17 的 JAR；无论成功与否都尝试保存测试报告。

本地可在项目根目录的 PowerShell 运行：

```powershell
cd frontend
pnpm install --frozen-lockfile
pnpm test
pnpm build
cd ..
.\mvnw.cmd --batch-mode --no-transfer-progress clean verify
```

若本机没有设置 `JAVA_HOME`，先在当前终端将它指向已安装的 JDK。Maven 单元和静态资源测试不要求真实数据库或 DeepSeek Key；它们不能替代数据库、浏览器及外部 AI 的完整端到端验收。

## 证据与审查

实验二最终整合时，本机 Vue 测试 27 项通过，Maven 测试 34 项通过，并完成构建资源、角色权限和部分浏览器联调。旧静态版曾有 49 项测试，那是历史版本，不能作为当前 Vue 版的测试数量。远程 PR、审查意见、Actions 运行和合并结果应以实际 GitHub 链接与页面为准，不能把本地成功当作远程成功。课程报告可附提交图、PR 差异、Actions 结果及测试报告；如另做合并冲突练习，应保存真实操作记录。

AI 辅助了代码定位、CI 适配、文档整理和验证；`review.md` 记录的是 AI 辅助审查，不代表独立人工批准。

参考：[GitHub 工作流语法](https://docs.github.com/en/actions/reference/workflows-and-actions/workflow-syntax)、[setup-node 的 pnpm 缓存说明](https://github.com/actions/setup-node/blob/main/docs/advanced-usage.md#caching-packages-data)、[pnpm/action-setup](https://github.com/pnpm/action-setup)。
