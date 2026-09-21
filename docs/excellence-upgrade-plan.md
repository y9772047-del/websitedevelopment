# 优秀版升级计划

## 目标

将“大学生学习计划管理系统”从功能完整的课程项目，提升为更适合答辩展示的优秀作品版本。

本文件是短索引，详细设计已拆分到 `docs/excellence/`。后续修改应先更新对应专题文档，再同步本索引中的关键锚点。

## 本轮改造范围

本轮以学生端展示体验为主，遵循“文档先行、小步修改、三层同步检查”的原则。

默认约束：

- 不调整数据库结构，除非功能明确需要。
- 不重写后端接口，除非接口契约明确变更。
- 不引入新的前端框架或 npm 依赖。
- 保留现有页面切换、表单提交、AI 改进和主题切换逻辑。

重点文件：

- `src/main/resources/static/pages/app.html`
- `src/main/resources/static/css/style.css`
- `src/main/resources/static/js/app.js`

## 拆分文档

- [首页学习驾驶舱与工作台](excellence/01-dashboard.md)
- [统计图表、热力图与轮盘图](excellence/02-charts.md)
- [任务分类、学习记录与日历](excellence/03-task-category-and-calendar.md)
- [视觉升级、设计原则与验证](excellence/04-visual-polish.md)

## 功能锚点

### 首页学习驾驶舱

首页学习驾驶舱聚焦今日任务、风险提醒、计划进度、最近学习记录和 AI 改进建议。

### 统计页图表化

统计页图表化保留原有统计数据来源，优先使用原生 HTML/CSS/JavaScript 实现轻量图表。

### 学习记录时间线

学习记录时间线用于展示最近学习轨迹，突出学习日期、学习时长、完成情况、问题反思和改进计划。

### 学习任务分类固定选项

固定分类为：课程进度、作业实验、考试备考、竞赛科研、专业能力、复盘总结、自我提升、其他任务。

分类学习热力图筛选按钮固定为 `[ 全部 ] [ 课业完成型 ] [ 成绩提升型 ] [ 成长拓展型 ] [ 综合提升型 ]`。

### 首页学习活跃热力图

热力图按学习记录日期和学习时长渲染，等级为：无记录、1-60 分钟、60-120 分钟、120-240 分钟、240-360 分钟、360 分钟以上。

热力图色阶需要跟随主题皮肤变化。

### 首页学习计划投入分布轮盘图

学习计划投入分布轮盘图使用后端汇总字段 `total_study_minutes` 和 `total_study_record_count`，并支持最近30天、最近180天和全部范围。

热力图和轮盘图是本轮视觉升级重点。

### 学习日历记录化

学习日历只展示真实学习记录，不再按任务截止日期铺排任务。

### 学习记录任务下拉排序

学习记录任务下拉排序按任务最新时间排序，优先使用 `deadline`，再使用 `createTime`。

## 验证计划

优先执行：

```powershell
node --check src/main/resources/static/js/app.js
.\mvnw.cmd -Dtest=ExcellenceUpgradeStaticResourceTest,RecordSubmitGuardTest test
```

如修改范围扩大，再执行：

```powershell
.\mvnw.cmd clean package -DskipTests
```
