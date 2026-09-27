# 实验三：Git 协作开发与持续集成

本轮以已验收的 Vue 版本 `d6951b8` 为基线，仅整理实验材料并完成真实冲突练习，不修改业务代码。课程方案要求：分支管理、Pull Request、Code Review、基础 CI；步骤包含 feature 分支、合并、解决冲突、推送远程和 PR 模板；AI 可初步评审，人工负责最终判断。

## 要求与成果

| 要求 | 本项目成果 | 怎么证明 |
| --- | --- | --- |
| 版本与分支 | main 保持已验收版本；两个 feature 分支模拟不同修改 | [证据索引](evidence.md)的提交图与分支链接 |
| 合并与冲突 | 同一句验收条件发生真实 content conflict；解决时保留测试和审查要求 | [冲突原文](evidence/conflict-before.txt)、解决提交 `d3f7d71` |
| Pull Request | 改动通过专门 PR 提交，展示目的、差异和验证 | 本轮 PR 的 Conversation、Commits、Files changed |
| Code Review | AI 初步自查记录；最终由提交者阅读差异并负责 | [自查记录](review.md)，不冒充独立人工批准 |
| CI | GitHub Actions 用 Java 17/21、Node 22、pnpm 执行前端测试、两种 Vue 构建和 Maven 验证 | Actions 的实际绿色运行及日志 |
| PR 模板 | `.github/pull_request_template.md` 包含验证、影响、AI 使用与评审 | 新建 PR 时自动填入模板 |

本练习是同一账号进行的分支协作模拟。仓库已存在，不必重新 git init。不要把它说成两名同学各自提交的团队协作。

## 当前 CI 到底做什么

1. 检出本次提交，安装 JDK、Node 和 pnpm。
2. 用 `pnpm-lock.yaml` 锁定依赖安装，运行 `pnpm test`。
3. 构建独立 Vue 包 `frontend/dist/`，检查产物和指定 API 地址。
4. 构建写入 Spring Boot 静态目录的 Vue 包。
5. Maven `clean verify` 编译、运行 Java 测试并打包 JAR。
6. 保存测试报告；Java 17 任务额外保存独立 Vue 包和 JAR，保留 7 天。过期后下载入口可能消失，日志仍是构建证据。

基线实测数量是前端 31 项、Java 34 项。旧材料的“没有 npm 构建”“static/js 检查”“49 项测试”描述旧 HTML 版本，不用于当前验收。CI 不连接真实 MySQL、不调用 DeepSeek，也不证明所有界面和外部 AI 调用正确；实验二浏览器证据另见 `docs/experiment-2/verification.md`。

## 本地验证

先停止正在运行、锁定 target JAR 的 Java 进程。在隔离工作目录操作，原 D 目录有未提交文件，不要在那里执行 reset --hard、clean 或直接切换分支。

~~~powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.12.1'
pnpm --dir frontend test
pnpm --dir frontend build
.\mvnw.cmd clean verify
git diff --check
~~~

只查看历史的命令不会修改文件：

~~~powershell
git status --short --branch
git log --graph --oneline --max-count=12
git show --format=fuller --no-patch d3f7d71
git diff d3f7d71^1 d3f7d71 -- docs/experiment-3/merge-practice.md
~~~

## 展示材料

先读 [助教演示指南](demo-guide.md)，再按 [证据索引](evidence.md) 打开 GitHub 页面。只展示真正执行的操作；AI 辅助写作与提交者最终审查须区分。课程未提交的 PPT、DOCX 和个人笔记仍保留在原目录，没有自动上传。

参考：[GitHub 分支与 PR 入门](https://docs.github.com/en/get-started/using-github/hello-world)、[命令行解决冲突](https://docs.github.com/en/pull-requests/how-tos/merge-and-close-pull-requests/resolving-a-merge-conflict-using-the-command-line)、[查看 Actions 运行历史](https://docs.github.com/en/actions/how-tos/monitor-workflows/view-workflow-run-history)。
