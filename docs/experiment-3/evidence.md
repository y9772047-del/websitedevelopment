# 实验三真实证据索引

核验日期：2026-09-27。这里仅记录真实存在的提交和运行，不伪造截图、评审人或团队成员。

## 当前项目与已有 PR

- [仓库 main](https://github.com/y9772047-del/websitedevelopment/tree/main)：实验二已验收的 Vue 版本。
- [独立 Vue 构建 PR #6](https://github.com/y9772047-del/websitedevelopment/pull/6)：已合并，可展示 Files changed、Checks 和合并记录。
- [PR #6 成功的 Actions](https://github.com/y9772047-del/websitedevelopment/actions/runs/36161839238)：pull_request，提交 `69f0fa2`，核验状态 success。
- [main 成功的 Actions](https://github.com/y9772047-del/websitedevelopment/actions/runs/36161992919)：push，提交 `d6951b8`，核验状态 success。
- PR #6 的 reviews API 核验结果为 `[]`：没有独立 review，不能声称另一个人已批准。

## 本轮真实分支与冲突

操作由 Codex 按用户授权执行；提交者需要阅读冲突及解决内容，不能声称由不同同学独立完成。

- [CI 功能分支](https://github.com/y9772047-del/websitedevelopment/tree/feature/experiment3-ci)
- [Review 功能分支](https://github.com/y9772047-del/websitedevelopment/tree/feature/experiment3-review)
- [冲突解决提交](https://github.com/y9772047-del/websitedevelopment/commit/d3f7d7149329ec562bc77d44b615c7c7bd8ccfb6)

| 阶段 | 提交 | 实际内容 |
| --- | --- | --- |
| 练习共同基线 | `252c573` | 单一验收条件“完成本地构建” |
| feature/experiment3-ci | `a57227f` | 同一行改成 Vue、Maven 与 Actions 通过 |
| feature/experiment3-review | `bd7d2ec` | 同一行改成 PR 差异审查及人工验收 |
| 合并 CI 分支 | `dbbb00d` | 无冲突合并第一侧 |
| 合并另一侧 | 命令返回 1，文件状态 UU | 真实 content conflict，原文见下方 |
| 综合两侧要求的解决提交 | `d3f7d71` | 保留两侧要求，两个父提交为 dbbb00d 和 bd7d2ec |

[冲突前原文](evidence/conflict-before.txt) 保存的是本轮 Git 产生的标记。解决后的 [练习文档](merge-practice.md) 不含这些标记。`<<<<<<<` 是当前侧，`=======` 分隔两侧，`>>>>>>>` 是待合并分支；选择合理内容并删除标记后，git add 标记已解决，git commit 完成合并。

~~~text
*   d3f7d71 解决冲突，保留 CI 与人工审查要求
|\
| * bd7d2ec feature/experiment3-review
* |   dbbb00d 合并 CI 分支
|\ \
| |/
|/|
| * a57227f feature/experiment3-ci
|/
* 252c573 共同基线
~~~

## 本轮 PR 与 CI

- [实验三 PR #7](https://github.com/y9772047-del/websitedevelopment/pull/7)：打开 Files changed 审查材料，Checks 查看本轮最新提交的测试。
- [材料提交的 push 运行](https://github.com/y9772047-del/websitedevelopment/actions/runs/36326874645)：对应 `0cc192b`。后续补充证据会产生新的 CI 运行，以 PR 最新 Checks 为准。

本轮不自动合并，供提交者阅读差异、检查日志并实际完成最终判断。最终人工评审按实际参与者填写，不虚构；历史 PR #6 的空 reviews 结果不能当成本轮 PR 的审批状态。
