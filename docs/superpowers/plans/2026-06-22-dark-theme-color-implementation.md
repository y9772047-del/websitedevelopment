# Dark Theme Color Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将夜间模式改为层级清楚的暗蓝灰界面，并以柔和紫色作为局部点缀。

**Architecture:** 保留现有 `body.theme-dark` 切换机制和通用组件结构。通过暗色变量定义基础表面层级，再用少量 `body.theme-dark` 定向选择器处理首页、日历和重要节点的深色可读性。

**Tech Stack:** CSS custom properties、静态 HTML、JUnit 5、AssertJ、Maven

---

### Task 1: 暗色主题回归测试

**Files:**
- Modify: `src/test/java/com/example/studyplan/staticresources/ExcellenceUpgradeStaticResourceTest.java`

- [ ] 新增测试，断言暗色变量包含暗蓝灰页面色、卡片表面色、导航色和热力图色阶。
- [ ] 断言首页、日历和重要节点存在必要的 `body.theme-dark` 定向选择器。
- [ ] 运行 `ExcellenceUpgradeStaticResourceTest#darkThemeUsesLayeredBlueGraySurfaces`，确认测试因样式尚未实现而失败。

### Task 2: 实现暗蓝灰夜间模式

**Files:**
- Modify: `src/main/resources/static/css/style.css:99-116`
- Modify: `src/main/resources/static/css/style.css`（相关组件之后的夜间覆盖区）

- [ ] 更新 `body.theme-dark` 的页面、卡片、边框、文字、紫色点缀、导航和热力图变量。
- [ ] 增加公共暗色表面、首页卡片和图例的定向覆盖。
- [ ] 增加日历格子、详情卡、选中态和状态标签的定向覆盖。
- [ ] 增加重要节点列表项、状态标签和操作按钮的定向覆盖。
- [ ] 重跑目标测试，确认通过。

### Task 3: 缓存与构建验证

**Files:**
- Modify: `src/main/resources/static/index.html:8`
- Modify: `src/main/resources/static/pages/app.html:8`
- Modify: `src/main/resources/static/pages/admin.html:8`

- [ ] 将三个页面的 `style.css` 查询版本统一更新为 `20260622-dark-blue-gray`。
- [ ] 运行完整 `ExcellenceUpgradeStaticResourceTest`。
- [ ] 运行 `.\mvnw.cmd clean package -DskipTests`，确认构建成功。
- [ ] 使用 `rg` 核对修改仅限计划内文件和选择器。
