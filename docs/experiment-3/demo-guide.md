# 给助教演示：5–8 分钟操作指南

目标不是背名词，而是能找到证据并解释一次完整流程。先打开 [仓库](https://github.com/y9772047-del/websitedevelopment)、[实验三 PR #7](https://github.com/y9772047-del/websitedevelopment/pull/7)、[Actions](https://github.com/y9772047-del/websitedevelopment/actions) 三个浏览器标签。演示前自己点一遍，下面的话可以用自己的语言说。

## 先懂六个词

| 名词 | 你可以怎样理解 | 与相近操作的区别 |
| --- | --- | --- |
| 仓库 repository | 代码和历史所在的位置 | Git 是版本工具，GitHub 是托管与协作网站 |
| 分支 branch | 从某个版本分出来的一条修改路线 | 切换路线不等于已经合并 |
| 提交 commit | 有编号、作者、说明的版本快照 | commit 只保存到本地，push 才上传 |
| PR | 提议把分支的改动合进 main，供别人查看 | 建 PR 不会马上改 main |
| Review | 阅读差异，提出问题或批准 | 绿色 CI 是机器检查，不是人的批准 |
| CI | 推送/PR 后在服务器自动构建测试 | 不替代真实业务验收 |

## 演示顺序与讲法

| 时间 | 你操作什么 | 你说明什么 |
| --- | --- | --- |
| 1 分钟 | Code 页打开分支下拉；展示 main 与两个 feature 分支 | “主版本保持稳定，我在功能分支修改，再合并。这里是同一账号模拟两条协作路线。” |
| 1 分钟 | 打开实验三 evidence.md，再打开冲突原文和解决后的 merge-practice.md | “两边改同一行，Git 无法判断哪边正确。在 AI 辅助下保留测试与审查两个条件，删除冲突标记后提交；我需要核对结果。” |
| 1 分钟 | PR 的 Commits 和 Files changed | “提交保存历史，PR 展示相对 main 的差异。红色是删除、绿色是新增；这次只改实验材料。” |
| 2 分钟 | PR 的 Checks，进入一个 Java 17 或 21 job，展开 Test Vue、两次 Build Vue、Java tests | “远程分别跑前端测试、独立前端构建、JAR 前端构建和 Maven 验证。两种 Java 版本检查兼容性。” |
| 1 分钟 | 查看 Actions 页面下方 Artifacts（未过期时） | “这是服务器生成的报告、Vue 包和 JAR，不只是我电脑上的成功日志。独立包 API 地址仍需按部署环境配置。” |
| 1 分钟 | 展示 review.md，说明自己的验收与 AI 使用 | “AI 做初步整理与自查，我需要看差异、检查日志并负责最终判断，没有独立同学审批就不会说已经有人批准。” |

在 GitHub 中 Files changed 显示差异、Checks 显示检查；进入 Actions 运行后点击左侧具体 job，再展开步骤查看日志。不要只指着绿色图标就结束，要能指出测试和构建的命令。

## 如果助教让你运行 Git 命令

在已用于验收的隔离工作目录运行以下只读命令。不要在有未提交材料的 D 目录练习切分支或合并。

~~~powershell
cd C:\Users\yl\.codex\worktrees\vue-main-merge\websitedevelopment
git status --short --branch
git log --graph --oneline --max-count=12
git show --format=fuller --no-patch d3f7d71
git diff d3f7d71^1 d3f7d71 -- docs/experiment-3/merge-practice.md
~~~

第一条告诉你当前路线和未保存内容；第二条显示提交图；第三条能看到 merge 的两个父版本；第四条比较第一父版本与解决后的文件。`^1` 表示合并提交的第一个父提交。

## 你应该能回答的问题

- 为什么不用直接改 main？功能分支让改动在审查和检查完成前不进入主版本。
- add、commit、push 有什么区别？选择本次内容、本地保存版本、上传已保存版本。
- 冲突是不是项目坏了？不是；Git 停下来要求人决定如何组合，解决前不能完成这次合并。
- CI 红了怎么办？打开失败 job 和步骤，读错误，修改后提交并再次推送，不用删测试或跳过失败。
- CI 绿了是不是 AI 功能就一定正常？不是；当前 CI 不调用 MySQL/DeepSeek，真实浏览器验收是另一层。
- 可以回退吗？Git 可以回退代码；已在数据库发生的删除不会因代码回退自动恢复。共享历史优先用 revert，不盲目 reset --hard。
- AI 帮了什么？初步审查、材料整理、冲突练习与自动化操作；我仍要理解结果、检查证据并承担最终责任。

## PR 最后怎样由你检查并合并

打开本轮 PR → Files changed，读 README、review、冲突记录与这份指南；确认 Checks 全绿。若需要同学评审，先让真实同学提交 review。最后回到 Conversation，使用 Merge pull request 并确认（以仓库实际显示的按钮为准）。合并前 PR 中的材料仍在分支，合并后 main 才包含它们。

截图应来自你实际打开的页面：提交图、冲突原文、PR 差异、CI job 成功日志。不要把本材料中的文字图当成自己操作过程的屏幕截图。

建议现在练一遍：不用看本文，能说出“分支 → commit → push → PR → review/CI → merge”，并实际找到对应页面，就足以展示你掌握了流程。
