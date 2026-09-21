package com.example.studyplan.staticresources;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ExcellenceUpgradeStaticResourceTest {

    private String readStylesheet() throws Exception {
        return Files.readString(Path.of("src/main/resources/static/css/base.css"))
                + Files.readString(Path.of("src/main/resources/static/css/app.css"))
                + Files.readString(Path.of("src/main/resources/static/css/admin.css"))
                + Files.readString(Path.of("src/main/resources/static/css/auth.css"));
    }

    @Test
    void excellenceUpgradePlanDocumentsDashboardAndStatisticsScope() throws Exception {
        String plan = Files.readString(Path.of("docs/excellence-upgrade-plan.md"));

        assertThat(plan).contains("首页学习驾驶舱");
        assertThat(plan).contains("统计页图表化");
        assertThat(plan).contains("文档先行");
    }

    @Test
    void studentAppContainsDashboardAndStatisticsUpgradeAnchors() throws Exception {
        String page = Files.readString(Path.of("src/main/resources/static/pages/app.html"));

        assertThat(page).contains("dashboardFocusPanel");
        assertThat(page).contains("dashboardRiskPanel");
        assertThat(page).contains("statusChart");
        assertThat(page).contains("priorityChart");
    }

    @Test
    void appScriptRendersDashboardFocusAndStatisticsCharts() throws Exception {
        String script = Files.readString(Path.of("src/main/resources/static/js/app.js"));

        assertThat(script).contains("function renderDashboardFocus");
        assertThat(script).contains("function renderStatisticsCharts");
        assertThat(script).contains("countByStatus");
        assertThat(script).contains("countByPriority");
    }

    @Test
    void stylesheetContainsUpgradeVisualComponents() throws Exception {
        String style = readStylesheet();

        assertThat(style).contains(".dashboard-focus-card");
        assertThat(style).contains(".mini-chart-bar");
        assertThat(style).contains(".insight-grid");
    }

    @Test
    void taskCategoryUsesDocumentedFixedOptions() throws Exception {
        String plan = Files.readString(Path.of("docs/excellence-upgrade-plan.md"));
        String page = Files.readString(Path.of("src/main/resources/static/pages/app.html"));
        String script = Files.readString(Path.of("src/main/resources/static/js/tasks.js"));

        assertThat(plan).contains("课程进度", "作业实验", "考试备考", "竞赛科研", "专业能力", "复盘总结", "自我提升", "其他任务");
        assertThat(page).contains("name=\"category\"");
        assertThat(page).contains("<select class=\"form-select\" name=\"category\"");
        assertThat(script).contains("../api/tasks/categories");
        assertThat(script).contains("function loadTaskCategories");
        assertThat(script).contains("function fillTaskCategoryOptions");
        assertThat(script).contains("isValidTaskCategory(category)");
        assertThat(script).contains("分类必须是固定选项之一");
    }

    @Test
    void dashboardCategoryHeatmapUsesFixedFilterGroups() throws Exception {
        String plan = Files.readString(Path.of("docs/excellence-upgrade-plan.md"));
        String page = Files.readString(Path.of("src/main/resources/static/pages/app.html"));
        String script = Files.readString(Path.of("src/main/resources/static/js/app.js"));
        String style = readStylesheet();

        assertThat(plan).contains("[ 全部 ] [ 课业完成型 ] [ 成绩提升型 ] [ 成长拓展型 ] [ 综合提升型 ]");
        assertThat(page).contains("categoryHeatmapFilters", "categoryHeatmapGrid");
        assertThat(style).contains(".category-heatmap", ".heatmap-cell");
        assertThat(script).contains("CATEGORY_HEATMAP_FILTERS");
        assertThat(script).contains("全部", "课业完成型", "成绩提升型", "成长拓展型", "综合提升型");
        assertThat(script).contains("课程进度", "作业实验", "考试备考", "复盘总结", "竞赛科研", "专业能力", "自我提升");
        assertThat(script).doesNotContain("其他任务: [");
    }

    @Test
    void dashboardHeatmapUsesSixDurationLevels() throws Exception {
        String plan = Files.readString(Path.of("docs/excellence-upgrade-plan.md"));
        String page = Files.readString(Path.of("src/main/resources/static/pages/app.html"));
        String script = Files.readString(Path.of("src/main/resources/static/js/app.js"));
        String style = readStylesheet();

        assertThat(plan).contains("无记录", "1-60 分钟", "60-120 分钟", "120-240 分钟", "240-360 分钟", "360 分钟以上");
        assertThat(page).contains("heatmap-level-0", "heatmap-level-1", "heatmap-level-2", "heatmap-level-3", "heatmap-level-4", "heatmap-level-5");
        assertThat(script).contains("if (minutes <= 60) return 1");
        assertThat(script).contains("if (minutes <= 120) return 2");
        assertThat(script).contains("if (minutes <= 240) return 3");
        assertThat(script).contains("if (minutes <= 360) return 4");
        assertThat(script).contains("return 5");
        assertThat(style).contains(".heatmap-level-5");
    }

    @Test
    void dashboardHeatmapUsesFixedGithubStyleRecent180DayGrid() throws Exception {
        String script = Files.readString(Path.of("src/main/resources/static/js/app.js"));
        String style = readStylesheet();

        assertThat(script).contains("const CATEGORY_HEATMAP_REAL_DAYS = 180");
        assertThat(script).contains("function getHeatmapDateRange");
        assertThat(script).contains("function buildHeatmapGrid");
        assertThat(script).contains("function aggregateRecordsByDate");
        assertThat(script).contains("function getHeatmapLevel");
        assertThat(script).contains("isInRealRange");
        assertThat(script).contains("displayStartDate", "displayEndDate");
        assertThat(script).contains("近 180 天累计");
        assertThat(script).doesNotContain("最近 13 周");

        assertThat(style).contains("grid-template-rows: repeat(7, 15px);");
        assertThat(style).contains("grid-auto-columns: 15px;");
        assertThat(style).contains("gap: 6px;");
        assertThat(style).contains(".heatmap-cell.out-of-range");
    }

    @Test
    void dashboardHeatmapColorsFollowThemeSkins() throws Exception {
        String plan = Files.readString(Path.of("docs/excellence-upgrade-plan.md"));
        String style = readStylesheet();

        assertThat(plan).contains("热力图色阶需要跟随主题皮肤变化");
        assertThat(style).contains("--heatmap-level-0", "--heatmap-level-1", "--heatmap-level-2", "--heatmap-level-3", "--heatmap-level-4", "--heatmap-level-5");
        assertThat(style).contains("body.theme-pink", "body.theme-blue", "body.theme-green", "body.theme-cream", "body.theme-dark");
        assertThat(style).contains(".heatmap-level-1 {\n    background: var(--heatmap-level-1);");
        assertThat(style).contains(".heatmap-level-5 {\n    background: var(--heatmap-level-5);");
    }
    @Test
    void pageBackgroundFollowsSelectedThemeSkin() throws Exception {
        String style = readStylesheet();
        int backgroundStart = style.indexOf(".prototype-body {");
        int backgroundEnd = style.indexOf("}\n", backgroundStart);
        String pageBackgroundRule = style.substring(backgroundStart, backgroundEnd);

        assertThat(pageBackgroundRule).contains("var(--page-bg)", "var(--primary)");
        assertThat(pageBackgroundRule).doesNotContain("#fff7f8", "#fffdfc", "#fff1f4");
    }
    @Test
    void darkThemeUsesLayeredBlueGraySurfaces() throws Exception {
        String style = readStylesheet();
        String indexPage = Files.readString(Path.of("src/main/resources/static/index.html"));
        String appPage = Files.readString(Path.of("src/main/resources/static/pages/app.html"));
        String adminPage = Files.readString(Path.of("src/main/resources/static/pages/admin.html"));

        assertThat(style).contains(
                "--page-bg: #0f1724;",
                "--page-bg-soft: #151e2d;",
                "--card-bg: #1b2534;",
                "--card-bg-soft: #222d3d;",
                "--nav-bg: rgba(18, 27, 40, 0.96);",
                "--page-glow: rgba(139, 124, 246, 0.10);"
        );
        assertThat(style).contains(
                "--heatmap-level-0: #202b3a;",
                "--heatmap-level-1: #29364b;",
                "--heatmap-level-5: #a99cff;"
        );
        assertThat(style).contains(
                "body.theme-dark.prototype-body",
                "body.theme-dark .app-navbar",
                "body.theme-dark .chart-card",
                "body.theme-dark .calendar-cell",
                "body.theme-dark .calendar-day-panel",
                "body.theme-dark .important-node-card",
                "body.theme-dark .important-node-actions .btn-outline-primary"
        );
        assertThat(indexPage).contains("css/base.css?v=20260622-css-split", "css/auth.css?v=20260705-login-blue");
        assertThat(appPage).contains("../css/base.css?v=20260622-css-split", "../css/app.css?v=20260622-css-split");
        assertThat(adminPage).contains("../css/base.css?v=20260622-css-split", "../css/admin.css?v=20260622-css-split");
    }
    @Test
    void dashboardPlanInvestmentDonutUsesCachedAllRangeAndFixedButtons() throws Exception {
        String plan = Files.readString(Path.of("docs/excellence-upgrade-plan.md"));
        String page = Files.readString(Path.of("src/main/resources/static/pages/app.html"));
        String script = Files.readString(Path.of("src/main/resources/static/js/app.js"));
        String style = readStylesheet();

        assertThat(plan).contains("学习计划投入分布轮盘图", "total_study_minutes", "total_study_record_count");
        assertThat(page).contains("planInvestmentRangeButtons", "planInvestmentDonut", "planInvestmentLegend", "planInvestmentEmpty");
        assertThat(script).contains("PLAN_INVESTMENT_RANGES");
        assertThat(script).contains("最近30天", "最近180天", "全部");
        assertThat(script).contains("function buildPlanInvestmentStats");
        assertThat(script).contains("function buildAllPlanInvestmentStatsFromRecords");
        assertThat(script).contains("function renderPlanInvestmentDonut");
        assertThat(script).contains("cachedRows.some(row => row.minutes > 0)");
        assertThat(script).contains("totalStudyMinutes", "totalStudyRecordCount");
        assertThat(script).contains("appState.currentPlanInvestmentRange");
        assertThat(script).doesNotContain("todayStudyMinutes", "weekStudyMinutes", "monthStudyMinutes", "semesterStudyMinutes");
        assertThat(style).contains(".plan-investment-donut", ".plan-investment-legend", ".plan-investment-range-btn");
    }

    @Test
    void importantNodeFeatureHasNavigationPageModalBatchImportAndCalendarAnchors() throws Exception {
        String scope = Files.readString(Path.of("PROJECT_SCOPE.md"));
        String readme = Files.readString(Path.of("README.md"));
        String schema = Files.readString(Path.of("src/main/resources/db/schema.sql"));
        String page = Files.readString(Path.of("src/main/resources/static/pages/app.html"));
        String script = Files.readString(Path.of("src/main/resources/static/js/app.js"));
        String calendarScript = Files.readString(Path.of("src/main/resources/static/js/calendar.js"));
        String style = readStylesheet();

        assertThat(scope).contains("重要节点", "important_node", "顶部导航移除“学习统计”入口");
        assertThat(readme).contains("重要节点", "学习统计页面代码暂时保留，但当前顶部导航不显示该入口");
        assertThat(schema).contains("CREATE TABLE important_node", "type", "start_date", "end_date", "description", "importance", "plan_id", "task_id");

        assertThat(page).contains("data-page=\"importantNodes\"", "id=\"importantNodes\"", "importantNodeYearSelect");
        assertThat(page).contains("importantNodeModal", "单个新增", "批量导入");
        assertThat(page).contains("请按以下格式输入，每行一个重要节点", "在这里粘贴批量节点内容");
        assertThat(page).doesNotContain("data-page=\"statistics\">学习统计");

        assertThat(script).contains("importantNodes: []", "function loadImportantNodes", "function renderImportantNodes");
        assertThat(script).contains("function parseImportantNodeBatch", "第 ${rowNumber} 行导入失败");
        assertThat(script).contains("结束日期不能早于开始日期");
        assertThat(calendarScript).contains("function buildCalendarNodesByDate", "renderCalendarDayDetails");
        assertThat(script).contains("../api/important-nodes");

        assertThat(style).contains(".important-node-card", ".important-node-month", ".calendar-node-pill");
    }
    @Test
    void dashboardChartsUsePolishedHeatmapAndDonutStructure() throws Exception {
        String plan = Files.readString(Path.of("docs/excellence-upgrade-plan.md"));
        String page = Files.readString(Path.of("src/main/resources/static/pages/app.html"));
        String script = Files.readString(Path.of("src/main/resources/static/js/app.js"));
        String style = readStylesheet();

        assertThat(plan).contains("热力图和轮盘图是本轮视觉升级重点");
        assertThat(page).contains("categoryHeatmapPanel", "categoryHeatmapWeekLabels");
        assertThat(page).doesNotContain("categoryHeatmapStats");
        assertThat(page).doesNotContain("categoryHeatmapMonthAxis");
        assertThat(page).contains("planInvestmentPanel", "planInvestmentSummary", "planInvestmentCenter");
        assertThat(script).doesNotContain("function renderHeatmapStats");
        assertThat(script).doesNotContain("活跃天数", "日均分钟");
        assertThat(script).doesNotContain("function renderHeatmapMonthAxis");
        assertThat(script).contains("plan-investment-progress", "planInvestmentSummary");
        assertThat(script).contains("function planInvestmentColor");
        assertThat(script).contains("#f05f7d", "#5fbf8f", "#f4a62a", "#5c8df6", "#9b70d9", "#42b8c6");
        String planInvestmentPalette = script.substring(
                script.indexOf("const PLAN_INVESTMENT_COLORS"),
                script.indexOf("];", script.indexOf("const PLAN_INVESTMENT_COLORS"))
        );
        assertThat(planInvestmentPalette).doesNotContain("var(");
        assertThat(style).contains(".heatmap-level-1 {\n    background: var(--heatmap-level-1);");
        assertThat(style).contains(".heatmap-level-5 {\n    background: var(--heatmap-level-5);");
        assertThat(style).contains(".chart-card", ".heatmap-board", ".plan-investment-progress");
        assertThat(style).doesNotContain(".heatmap-stats");
        assertThat(style).doesNotContain(".heatmap-month-axis");
    }

    @Test
    void dashboardChartsUseCompactTwoColumnLayoutWithoutHeatmapSummaryCards() throws Exception {
        String page = Files.readString(Path.of("src/main/resources/static/pages/app.html"));
        String style = readStylesheet();

        assertThat(page).contains("dashboard-chart-grid");
        assertThat(page).contains("content-card chart-card category-heatmap");
        assertThat(page).contains("content-card chart-card plan-investment-card");
        assertThat(page).doesNotContain("content-card chart-card category-heatmap mb-4");
        assertThat(page).doesNotContain("content-card chart-card plan-investment-card mb-4");

        assertThat(style).contains(".dashboard-chart-grid");
        assertThat(style).contains("grid-template-columns: minmax(0, 45fr) minmax(0, 55fr);");
        assertThat(style).contains("grid-template-areas: \"heatmap investment\";");
        assertThat(style).contains("grid-area: heatmap;");
        assertThat(style).contains("grid-area: investment;");
        assertThat(style).contains("min-width: 0;");
        assertThat(style).contains("@media (max-width: 992px)");
        assertThat(style).contains("grid-template-columns: 1fr;");
    }

    @Test
    void dashboardPlanInvestmentUsesSingleRingAndHourFocusedLabels() throws Exception {
        String script = Files.readString(Path.of("src/main/resources/static/js/app.js"));
        String style = readStylesheet();

        assertThat(script).contains("总学习时长");
        assertThat(script).contains("个计划 · ${totalRecords} 条记录");
        assertThat(script).doesNotContain("<strong>${formatStudyDuration(totalMinutes)}</strong>");
        assertThat(script).doesNotContain("<span>分钟</span>");

        assertThat(style).contains(".plan-investment-donut");
        assertThat(style).doesNotContain(".plan-investment-donut::after");
        assertThat(style).doesNotContain("box-shadow: inset 0 0 0 18px");
    }
}
