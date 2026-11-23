# 任务 2：分类与快速收集

## 已有数据库升级

执行 `src/main/resources/db/task2_categories_upgrade.sql`。该脚本新增个人分类表，将原八种分类和历史任务分类补入已有账号，并移除任务分类对全局分类目录的外键。原目录、分类名称、任务和记录均保留。可重复执行。

`schema.sql` 仅用于全新数据库，其中包含重建表操作，已有数据库不要执行它。

## 分类

统一使用“我的分类”，按用户存储。已有账号保留原八种分类及历史任务分类。上方“大学生推荐”“打工人推荐”只提供参考，点击分类旁的“添加”才会加入个人列表；已添加项禁用，不覆盖或删除已有内容。也可直接创建新分类。新注册账号按需添加自己的分类。

首页移除旧的固定分组，只按实际任务分类过滤热力图，并展示个人分类的任务数、已完成数和完成率。分类列表加载失败时仍按已有任务分类统计；计划、任务或记录加载失败时显示错误与重试提示，避免显示假零值。

## 任务 3：分类修改与删除

“我的分类”表格显示每类的全部任务数量，不受下方任务筛选条件影响。

分类卡片标题右侧的“新增分类”展开输入框并自动聚焦；已有分类时推荐区默认收起，空分类列表时展开，推荐方案以轻量按钮切换，已添加项弱化显示。修改名称在原分类行内使用整行宽度，显示关联任务数和字符数；取消后焦点返回原操作按钮。新增、编辑和删除面板互斥，删除确认紧跟对应分类展开，窄屏时操作自动排列为单列，按钮增加点击高度。布局样式仅作用于该分类区域。

- 修改：点击分类行的“修改”，在原行输入 1 到 50 个字符的名称并保存，按 Esc 或点击取消退出编辑。系统拒绝同一账号内的重名，并在同一事务中更新分类及当前账号相关任务的分类名称；学习记录、任务状态、计划归属保持原样。
- 删除未使用分类：点击“删除”后确认，仅删除这条个人分类。
- 删除已使用分类：在确认区选择另一个个人分类作为接收分类，再确认。系统先迁移当前账号相关任务，再删除原分类；任务和学习记录均保留。没有其他分类时需先创建接收分类。
- 推荐目录保持原样；从推荐添加的个人分类同样可以修改或删除。重新点击推荐中的“添加”可再次加入该推荐项，不会覆盖已改名分类。
- 分类操作后刷新任务与分类选项，表单当前分类同步调整，旧的快速收集预览清除。任务新增、修改、删除、批量保存后同步刷新分类任务数，删除确认前再次读取当前分类数据。批量文本中的旧分类名称需要自行调整后重新预览。

新增接口均使用现有 Result 包装：`GET /api/tasks/categories/mine` 返回 `{id, categoryName, taskCount}` 数组；`PUT /api/tasks/categories/{id}` 接收 `{categoryName}`；`DELETE /api/tasks/categories/{id}` 可带 `replacementId` 查询参数。全部按登录用户校验，已有接口路径和结构不变。

本任务不修改数据库结构，无需额外 SQL。仅用户主动改名/删除时，才事务性更新个人分类及相关任务。

## DRY / DIP 重构

前端复用联合刷新和面板切换方法，分类名称从详情列表派生，不维护重复状态。后端 Controller 仅依赖业务 Service 接口，分类列表查询及新增逻辑移入已有服务实现，名称校验和重名查询复用；不新增工厂或仓储层。分类名称输入的处理顺序、Result 包装、请求路径、用户归属、事务和行锁保持原有约定。

本次文件：`TasksView.vue`、`TaskController.java`、`StudyTaskService.java`、`StudyTaskServiceImpl.java`、`QuickCaptureTest.java`、`TaskCategoryManagementTest.java`、`TaskCategoryDatabaseTest.java`，以及本说明、代码地图、前端依赖说明和静态构建资源。数据库结构和 UI 样式不变，无需 SQL。

任务新增、批量新增、修改和快速收集在同一事务内校验并锁定个人分类，与改名/删除使用同一行锁，防止并发保存写入已经删除或改名前的分类。

任务 3 文件：`TaskController.java`、`StudyTaskService.java`、`StudyTaskServiceImpl.java`、`UserTaskCategoryMapper.java`、`dto/TaskCategorySummary.java`、`TasksView.vue`、`workflow.js`、`workflow.test.js`、`TaskCategoryManagementTest.java`、`TaskCategoryDatabaseTest.java`，以及静态构建资源、`CODE_MAP.md`、`docs/frontend-js-dependencies.md` 和本说明。

本机真实数据库验证通过 `RUN_TASK3_DB_TEST=true` 显式启用 `TaskCategoryDatabaseTest`，检查改名、重名拒绝、跨账号拒绝、删除迁移及记录保留，并用第二个 MySQL 连接的 NOWAIT 锁请求验证任务校验会持有分类锁。临时测试行在事务结束时全部回滚，并比对五张业务表所有行确认原数据一致。默认测试不连接真实数据库。

## 快速收集

在任务页“快速收集”粘贴以下格式，先预览，再确认保存整组内容：

```text
目标：准备软件工程考试
计划：完成课程复习
时间：2026-11-22 至 2026-12-10

任务：复习需求分析
分类：课程进度
截止：2026-11-25
状态：已完成
记录：完成需求分析章节
日期：2026-11-22
时长：45
问题：活动图还不熟悉
反思：继续做两道练习题

任务：复习软件测试
分类：考试备考
截止：2026-11-28
状态：未完成
```

必填计划名称和至少一个任务名称。分类须属于“我的分类”，省略时优先使用“其他任务”，未添加时使用个人列表第一项。任务可另填“优先级”“描述”；优先级支持 Q0/Q1/Q2/Q3，状态支持未完成/进行中/已完成/延期。记录可选；填写问题或反思时需同时填写记录内容。记录日期默认提交当天，时长默认 0 分钟。时间范围和任务截止日期可省略。

整组保存使用同一事务，校验失败不写入，持久化失败回滚；记录沿用原有计划投入累计逻辑。快速收集不创建重复任务，可在创建后编辑重复规则。

新增接口保持原 Result 包装；`GET /tasks/categories` 仍返回原模板字符串数组。`POST /tasks/quick-capture` 的载荷为 `{ plan, tasks: [{ task, record? }] }`。

## 本次改动文件

- 后端（`src/main/java/com/example/studyplan/`）：`controller/TaskController.java`、`controller/QuickCaptureRequest.java`、`service/StudyTaskService.java`、`service/impl/StudyTaskServiceImpl.java`、`entity/UserTaskCategory.java`、`mapper/UserTaskCategoryMapper.java`。
- 数据库（`src/main/resources/db/`）：`schema.sql`、`task2_categories_upgrade.sql`。
- 前端（`frontend/src/`）：`views/TasksView.vue`、`views/DashboardView.vue`、`dashboardData.js`、`dashboardData.test.js`、`workflow.js`、`workflow.test.js`、`insights.js`、`insights.test.js`。
- 后端测试（`src/test/java/com/example/studyplan/`）：`controller/TaskControllerTest.java`、`controller/QuickCaptureTest.java`、`service/impl/StudyTaskServiceImplTest.java`。
- 构建产物（`src/main/resources/static/`）：`index.html` 和其引用的哈希 JS 资源，旧 JS 由构建替换。
- 文档：`README.md`、`CODE_MAP.md`、`docs/frontend-js-dependencies.md`、`docs/task2-usage.md`。

工作区已有的认证、AI 等其他任务改动继续保留；上述清单包含本次续接的任务 2 文件。

## 验证

- 根目录：设置 `$env:RUN_TASK3_DB_TEST='true'` 后执行 `./mvnw.cmd clean package`，80 项测试通过并成功打包（含显式启用的真实 MySQL 验证）。默认不设置该变量时，真实数据库测试跳过。
- 前端：`node --test --experimental-test-isolation=none frontend/src/*.test.js`，44 项通过。
- `frontend/`：`node node_modules/vite/bin/vite.js build`，生产构建通过。
- `node --check frontend/src/workflow.js` 与 `node --check frontend/src/insights.js` 通过。
- 本机已执行真实数据库增量升级，并重复执行验证幂等性。原有 17 计划、41 任务、121 记录及八种系统分类的全部字段 SHA-256 摘要前后相同；个人分类 24 条，历史分类漏收录数及计划累计统计不一致数均为 0。校验报告在 `output/diy-category-migration.txt`。
- `node output/check-diy-statistics.mjs` 使用 JDBC 导出的只含统计字段的数据，验证本机三个账号的分类任务总数、完成数、计划投入分钟数、180 天热力图总和及分分类汇总均与 SQL 一致；结果在 `output/diy-statistics-check.json`。诊断辅助文件为 `output/DiyCategoryMigration.java`、`output/DiyStatisticsInput.java` 与 `output/check-diy-statistics.mjs`，不属于应用运行依赖。
- 未启动长期服务、未重启用户后端、未执行浏览器交互验证。回滚测试通过 Spring 事务代理与测试存储验证事务异常路径，未使用真实 MySQL。
