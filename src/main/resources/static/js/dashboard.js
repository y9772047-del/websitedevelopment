// Dashboard, statistics, and visualization logic. Loaded after app.js, tasks.js, records.js, important-nodes.js, and calendar.js.
const CATEGORY_HEATMAP_FILTERS = [
    {label: "全部", categories: null},
    {label: "课业完成型", categories: ["课程进度", "作业实验"]},
    {label: "成绩提升型", categories: ["考试备考", "复盘总结"]},
    {label: "成长拓展型", categories: ["竞赛科研", "专业能力"]},
    {label: "综合提升型", categories: ["自我提升"]}
];

const CATEGORY_HEATMAP_REAL_DAYS = 180;
const HEATMAP_WEEKDAYS = ["周一", "周二", "周三", "周四", "周五", "周六", "周日"];

const PLAN_INVESTMENT_RANGES = [
    {key: "30", label: "最近30天", days: 30},
    {key: "180", label: "最近180天", days: 180},
    {key: "all", label: "全部", days: null}
];

const PLAN_INVESTMENT_COLORS = [
    "#f05f7d",
    "#5fbf8f",
    "#f4a62a",
    "#5c8df6",
    "#9b70d9",
    "#42b8c6",
    "#e66bb5",
    "#7a9a3f"
];

function renderMetricCards(targetId, items) {
    document.getElementById(targetId).innerHTML = items.map(item => `
        <div class="col-sm-6 col-xl-3">
            <div class="metric-card">
                <span>${item.label}</span>
                <strong>${item.value}</strong>
                <small>${item.tip || ""}</small>
            </div>
        </div>
    `).join("");
}

function setPanelHtml(targetId, html) {
    const target = document.getElementById(targetId);
    if (target) target.innerHTML = html;
}

function taskIsOpen(task) {
    return task.status !== TEXT.done;
}

function countByStatus(tasks) {
    return tasks.reduce((counts, task) => {
        const key = isTaskOverdue(task) ? TEXT.delayed : (task.status || TEXT.notDone);
        counts[key] = (counts[key] || 0) + 1;
        return counts;
    }, {});
}

function countByPriority(tasks) {
    return tasks.reduce((counts, task) => {
        const key = task.priority || "Q2";
        counts[key] = (counts[key] || 0) + 1;
        return counts;
    }, {});
}

function renderMiniChart(targetId, rows) {
    const target = document.getElementById(targetId);
    if (!target) return;
    const visibleRows = rows.filter(row => row.value > 0);
    if (visibleRows.length === 0) {
        target.innerHTML = '<p class="mini-chart-empty">暂无可视化数据</p>';
        return;
    }
    const maxValue = Math.max(...visibleRows.map(row => row.value), 1);
    target.innerHTML = visibleRows.map(row => {
        const width = Math.max(8, Math.round((row.value / maxValue) * 100));
        return `
            <div class="mini-chart-row">
                <span class="mini-chart-label">${row.label}</span>
                <span class="mini-chart-track"><i class="mini-chart-bar" style="width:${width}%; background:${row.color || "var(--primary)"}"></i></span>
                <span class="mini-chart-value">${row.value}</span>
            </div>
        `;
    }).join("");
}

function renderDashboardFocus(data) {
    const todayText = formatLocalDate(new Date());
    const todayTasks = appState.tasks.filter(task => task.deadline === todayText && taskIsOpen(task));
    const overdueTasks = appState.tasks.filter(task => isTaskOverdue(task));
    const totalTasks = appState.tasks.length;
    const completedTasks = appState.tasks.filter(task => task.status === TEXT.done).length;
    const completionRate = totalTasks === 0 ? 0 : Math.round((completedTasks / totalTasks) * 100);
    const latestImprovement = (data.recentRecords || appState.records || [])
        .find(record => record.improvementPlan && String(record.improvementPlan).trim());

    setPanelHtml("dashboardFocusPanel", `
        <span>今日学习焦点</span>
        <h2>${todayTasks.length > 0 ? "今天有任务要推进" : "今天暂无截止任务"}</h2>
        <strong>${todayTasks.length}</strong>
        <p>${todayTasks.length > 0 ? compactText(todayTasks.map(task => task.taskName).join("、"), 48) : "可以复盘最近记录，或提前推进高优先级任务。"}</p>
    `);

    setPanelHtml("dashboardRiskPanel", `
        <span>风险提醒</span>
        <h2>${overdueTasks.length > 0 ? "存在延期任务" : "暂无延期风险"}</h2>
        <strong>${overdueTasks.length}</strong>
        <div class="dashboard-focus-list">
            ${overdueTasks.slice(0, 2).map(task => `<div class="dashboard-focus-item"><b>${compactText(task.taskName, 18)}</b><span>${task.deadline || ""}</span></div>`).join("") || '<div class="dashboard-focus-item"><b>节奏正常</b><span>继续保持</span></div>'}
        </div>
    `);

    setPanelHtml("dashboardProgressPanel", `
        <span>计划执行进度</span>
        <h2>任务完成率</h2>
        <strong>${completionRate}%</strong>
        <p>已完成 ${completedTasks} / ${totalTasks} 个任务，优先处理 Q0 和 Q1 任务。</p>
    `);

    setPanelHtml("dashboardImprovementPanel", `
        <span>最近改进建议</span>
        <h2>${latestImprovement ? taskName(latestImprovement.taskId) : "暂无 AI 改进记录"}</h2>
        <p>${latestImprovement ? compactText(latestImprovement.improvementPlan, 78) : "在学习记录中填写问题与反思后，可以生成并保存改进计划。"}</p>
    `);
}


function daysUntilTaskDeadline(task) {
    if (!task || !task.deadline || !isValidDateText(task.deadline)) return null;
    const today = parseLocalDate(formatLocalDate(new Date()));
    const deadline = parseLocalDate(task.deadline);
    return Math.round((deadline - today) / 86400000);
}

function taskReminderInfo(task) {
    if (!taskIsOpen(task)) return null;
    const days = daysUntilTaskDeadline(task);
    if (days === null) return null;
    if (days < 0) return {label: "已逾期", className: "status-delay", sort: 0};
    if (days === 0) return {label: "今日待办", className: "status-waiting", sort: 1};
    if (days <= 3) return {label: `${days} 天内截止`, className: "status-doing", sort: 2};
    return null;
}

function renderTaskReminders() {
    const list = document.getElementById("taskReminderList");
    if (!list) return;
    const reminders = appState.tasks
        .map(task => ({task, reminder: taskReminderInfo(task)}))
        .filter(row => row.reminder)
        .sort((a, b) => a.reminder.sort - b.reminder.sort || String(a.task.deadline || "").localeCompare(String(b.task.deadline || "")))
        .slice(0, 6);
    list.innerHTML = reminders.map(row => `
        <div class="list-row">
            <div><h3>${row.task.taskName}</h3><p>${planName(row.task.planId)} · ${row.task.deadline || ""}</p></div>
            <span class="badge ${row.reminder.className}">${row.reminder.label}</span>
        </div>
    `).join("") || `<div class="list-row"><p>暂无今日待办、临近截止或逾期任务。</p></div>`;
}

function recordCategory(record) {
    const task = taskById(record.taskId);
    return task ? (task.category || "其他任务") : "其他任务";
}

function activeHeatmapFilter() {
    return CATEGORY_HEATMAP_FILTERS.find(filter => filter.label === appState.currentHeatmapFilter) || CATEGORY_HEATMAP_FILTERS[0];
}

function recordMatchesHeatmapFilter(record, filter) {
    if (!filter.categories) return true;
    return filter.categories.includes(recordCategory(record));
}

function addLocalDays(date, dayCount) {
    const next = new Date(date);
    next.setDate(next.getDate() + dayCount);
    return next;
}

function startOfLocalDay(date) {
    return new Date(date.getFullYear(), date.getMonth(), date.getDate());
}

function getWeekdayIndex(date) {
    return (date.getDay() + 6) % 7;
}

function getHeatmapLevel(minutes) {
    if (minutes <= 0) return 0;
    if (minutes <= 60) return 1;
    if (minutes <= 120) return 2;
    if (minutes <= 240) return 3;
    if (minutes <= 360) return 4;
    return 5;
}

function heatmapLevel(minutes) {
    return getHeatmapLevel(minutes);
}

function getHeatmapDateRange(today = new Date()) {
    const endDate = startOfLocalDay(today);
    const startDate = addLocalDays(endDate, -(CATEGORY_HEATMAP_REAL_DAYS - 1));
    const displayStartDate = addLocalDays(startDate, -getWeekdayIndex(startDate));
    const displayEndDate = addLocalDays(endDate, 6 - getWeekdayIndex(endDate));
    return {startDate, endDate, displayStartDate, displayEndDate};
}

function aggregateRecordsByDate(records, range) {
    return records.reduce((groups, record) => {
        if (!record.studyDate || !isValidDateText(record.studyDate)) return groups;
        const studyDate = parseLocalDate(record.studyDate);
        if (!studyDate || studyDate < range.startDate || studyDate > range.endDate) return groups;
        if (!groups[record.studyDate]) groups[record.studyDate] = {minutes: 0, count: 0};
        groups[record.studyDate].minutes += Number(record.duration) || 0;
        groups[record.studyDate].count += 1;
        return groups;
    }, {});
}

function buildHeatmapGrid(groupedRecords, range) {
    const cells = [];
    for (let current = new Date(range.displayStartDate); current <= range.displayEndDate; current = addLocalDays(current, 1)) {
        const date = new Date(current);
        const dateKey = formatLocalDate(date);
        const isInRealRange = date >= range.startDate && date <= range.endDate;
        const stat = isInRealRange ? (groupedRecords[dateKey] || {minutes: 0, count: 0}) : {minutes: 0, count: 0};
        cells.push({
            date,
            dateKey,
            weekdayIndex: getWeekdayIndex(date),
            isInRealRange,
            minutes: stat.minutes,
            count: stat.count,
            level: isInRealRange ? getHeatmapLevel(stat.minutes) : 0
        });
    }
    return cells;
}

function formatHeatmapHours(minutes) {
    const hours = Math.round(((Number(minutes) || 0) / 60) * 10) / 10;
    return Number.isInteger(hours) ? String(hours) : hours.toFixed(1);
}

function heatmapCellTitle(cell, categoryLabel) {
    if (!cell.isInRealRange) return "范围外日期";
    const weekday = HEATMAP_WEEKDAYS[cell.weekdayIndex];
    if (cell.count === 0) {
        return `${cell.dateKey} ${weekday}\n暂无学习记录\n分类：${categoryLabel}`;
    }
    return `${cell.dateKey} ${weekday}\n学习时长：${cell.minutes} 分钟\n学习记录：${cell.count} 条\n分类：${categoryLabel}`;
}

function renderCategoryHeatmap() {
    const filters = document.getElementById("categoryHeatmapFilters");
    const grid = document.getElementById("categoryHeatmapGrid");
    const summary = document.getElementById("categoryHeatmapSummary");
    if (!filters || !grid || !summary) return;

    const currentFilter = activeHeatmapFilter();
    filters.innerHTML = CATEGORY_HEATMAP_FILTERS.map(filter => `
        <button class="heatmap-filter-btn ${filter.label === currentFilter.label ? "active" : ""}" type="button" data-heatmap-filter="${filter.label}">${filter.label}</button>
    `).join("");

    const records = appState.records.filter(record => record.studyDate && recordMatchesHeatmapFilter(record, currentFilter));
    const range = getHeatmapDateRange();
    const grouped = aggregateRecordsByDate(records, range);
    const cells = buildHeatmapGrid(grouped, range);
    const totalMinutes = Object.values(grouped).reduce((sum, stat) => sum + stat.minutes, 0);

    summary.textContent = `${currentFilter.label} · 近 180 天累计 ${formatHeatmapHours(totalMinutes)} 小时`;
    grid.innerHTML = cells.map(cell => {
        const title = heatmapCellTitle(cell, currentFilter.label);
        const outOfRangeClass = cell.isInRealRange ? "" : " out-of-range";
        return `<span class="heatmap-cell heatmap-level-${cell.level}${outOfRangeClass}" title="${title}" aria-label="${title}"></span>`;
    }).join("");

    filters.querySelectorAll("[data-heatmap-filter]").forEach(button => {
        button.addEventListener("click", () => {
            appState.currentHeatmapFilter = button.dataset.heatmapFilter;
            renderCategoryHeatmap();
        });
    });
}

function activePlanInvestmentRange() {
    return PLAN_INVESTMENT_RANGES.find(range => range.key === appState.currentPlanInvestmentRange) || PLAN_INVESTMENT_RANGES[0];
}

function formatStudyDuration(minutes) {
    const value = Number(minutes) || 0;
    if (value < 60) return `${value} 分钟`;
    const hours = Math.round((value / 60) * 10) / 10;
    return `${hours} 小时`;
}

function formatStudyHours(minutes) {
    const hours = Math.round(((Number(minutes) || 0) / 60) * 10) / 10;
    return `${hours.toFixed(1)} 小时`;
}

function recordInPlanInvestmentRange(record, range) {
    if (!range.days) return true;
    if (!record.studyDate || !isValidDateText(record.studyDate)) return false;
    const start = new Date();
    start.setHours(0, 0, 0, 0);
    start.setDate(start.getDate() - range.days + 1);
    const studyDate = new Date(record.studyDate);
    return studyDate >= start;
}

function planInvestmentTaskMap(tasks) {
    return tasks.reduce((map, task) => {
        map[String(task.id)] = task;
        return map;
    }, {});
}

function planInvestmentName(planId) {
    const plan = appState.plans.find(item => String(item.id) === String(planId));
    return plan ? displayPlanName(plan) : TEXT.unlinked;
}

function mergeTopPlanInvestmentRows(rows, limit = 5) {
    const sorted = rows
        .filter(row => row.minutes > 0)
        .sort((first, second) => second.minutes - first.minutes);
    if (sorted.length <= limit) return sorted;
    const visible = sorted.slice(0, limit);
    const rest = sorted.slice(limit).reduce((total, row) => ({
        planId: "other",
        planName: "其他计划",
        minutes: total.minutes + row.minutes,
        recordCount: total.recordCount + row.recordCount
    }), {planId: "other", planName: "其他计划", minutes: 0, recordCount: 0});
    return [...visible, rest].filter(row => row.minutes > 0);
}

function planInvestmentColor(index) {
    return PLAN_INVESTMENT_COLORS[index % PLAN_INVESTMENT_COLORS.length];
}

function buildAllPlanInvestmentStatsFromRecords(records, tasks) {
    const taskMap = planInvestmentTaskMap(tasks);
    const grouped = {};
    records.forEach(record => {
        const task = taskMap[String(record.taskId)];
        if (!task || !task.planId) return;
        const key = String(task.planId);
        if (!grouped[key]) {
            grouped[key] = {
                planId: task.planId,
                planName: planInvestmentName(task.planId),
                minutes: 0,
                recordCount: 0
            };
        }
        grouped[key].minutes += Number(record.duration) || 0;
        grouped[key].recordCount += 1;
    });
    return mergeTopPlanInvestmentRows(Object.values(grouped));
}

function buildPlanInvestmentStats(records, tasks, plans, rangeKey) {
    const range = PLAN_INVESTMENT_RANGES.find(item => item.key === rangeKey) || PLAN_INVESTMENT_RANGES[0];
    if (range.key === "all") {
        const cachedRows = mergeTopPlanInvestmentRows(plans.map(plan => ({
            planId: plan.id,
            planName: displayPlanName(plan),
            minutes: Number(plan.totalStudyMinutes) || 0,
            recordCount: Number(plan.totalStudyRecordCount) || 0
        })));
        return cachedRows.some(row => row.minutes > 0)
            ? cachedRows
            : buildAllPlanInvestmentStatsFromRecords(records, tasks);
    }

    const taskMap = planInvestmentTaskMap(tasks);
    const grouped = {};
    records.forEach(record => {
        if (!recordInPlanInvestmentRange(record, range)) return;
        const task = taskMap[String(record.taskId)];
        if (!task || !task.planId) return;
        const key = String(task.planId);
        if (!grouped[key]) {
            grouped[key] = {
                planId: task.planId,
                planName: planInvestmentName(task.planId),
                minutes: 0,
                recordCount: 0
            };
        }
        grouped[key].minutes += Number(record.duration) || 0;
        grouped[key].recordCount += 1;
    });
    return mergeTopPlanInvestmentRows(Object.values(grouped));
}

function renderPlanInvestmentDonut() {
    const buttons = document.getElementById("planInvestmentRangeButtons");
    const donut = document.getElementById("planInvestmentDonut");
    const center = document.getElementById("planInvestmentCenter");
    const legend = document.getElementById("planInvestmentLegend");
    const summary = document.getElementById("planInvestmentSummary");
    const empty = document.getElementById("planInvestmentEmpty");
    if (!buttons || !donut || !center || !legend || !summary || !empty) return;

    const range = activePlanInvestmentRange();
    buttons.innerHTML = PLAN_INVESTMENT_RANGES.map(item => `
        <button class="plan-investment-range-btn ${item.key === range.key ? "active" : ""}" type="button" data-plan-investment-range="${item.key}">${item.label}</button>
    `).join("");

    const rows = buildPlanInvestmentStats(appState.records, appState.tasks, appState.plans, range.key);
    const totalMinutes = rows.reduce((sum, row) => sum + row.minutes, 0);
    const totalRecords = rows.reduce((sum, row) => sum + row.recordCount, 0);
    center.innerHTML = `<strong>${formatStudyHours(totalMinutes)}</strong><span>总学习时长</span>`;
    summary.innerHTML = `
        <span>${range.label}</span>
        <small>${rows.length} 个计划 · ${totalRecords} 条记录</small>
    `;

    if (totalMinutes <= 0) {
        donut.style.background = "var(--primary-soft)";
        donut.title = "暂无学习投入数据";
        legend.innerHTML = "";
        center.innerHTML = `<strong>0.0 小时</strong><span>总学习时长</span>`;
        summary.innerHTML = `<span>${range.label}</span><small>暂无计划投入</small>`;
        empty.classList.remove("d-none");
    } else {
        let cursor = 0;
        const segments = rows.map((row, index) => {
        const percent = row.minutes / totalMinutes * 100;
        const start = cursor;
        cursor += percent;
        return `${planInvestmentColor(index)} ${start}% ${cursor}%`;
        });
        donut.style.background = `conic-gradient(${segments.join(", ")})`;
        donut.title = `${range.label}：累计 ${formatStudyDuration(totalMinutes)}`;
        empty.classList.add("d-none");
        legend.innerHTML = rows.map((row, index) => {
            const percent = Math.round(row.minutes / totalMinutes * 1000) / 10;
            const title = `${row.planName}｜${range.label}｜${formatStudyDuration(row.minutes)}｜${row.recordCount} 条记录｜${percent}%`;
            return `
                <div class="plan-investment-legend-item" title="${title}">
                    <i style="background:${planInvestmentColor(index)}"></i>
                    <span>${row.planName}</span>
                    <b class="plan-investment-progress"><u style="width:${percent}%; background:${planInvestmentColor(index)}"></u></b>
                    <strong>${formatStudyDuration(row.minutes)}</strong>
                    <em>${percent}%</em>
                </div>
            `;
        }).join("");
    }

    buttons.querySelectorAll("[data-plan-investment-range]").forEach(button => {
        button.addEventListener("click", () => {
            appState.currentPlanInvestmentRange = button.dataset.planInvestmentRange;
            renderPlanInvestmentDonut();
        });
    });
}

function renderStatisticsCharts(data) {
    const statusCounts = countByStatus(appState.tasks);
    const priorityCounts = countByPriority(appState.tasks);
    const totalTasks = appState.tasks.length || data.taskCount || 0;
    const completedTasks = appState.tasks.filter(task => task.status === TEXT.done).length || data.completedTaskCount || 0;
    const rate = totalTasks === 0 ? 0 : Math.round((completedTasks / totalTasks) * 100);

    const rateText = document.getElementById("completionRateText");
    const rateDetail = document.getElementById("completionRateDetail");
    if (rateText) rateText.textContent = `${rate}%`;
    if (rateDetail) rateDetail.textContent = totalTasks === 0 ? "暂无任务数据" : `已完成 ${completedTasks} / ${totalTasks} 个任务`;

    renderMiniChart("statusChart", [
        {label: TEXT.done, value: statusCounts[TEXT.done] || 0, color: "var(--success)"},
        {label: "进行中", value: statusCounts["进行中"] || 0, color: "var(--primary)"},
        {label: TEXT.notDone, value: statusCounts[TEXT.notDone] || 0, color: "var(--warning)"},
        {label: TEXT.delayed, value: statusCounts[TEXT.delayed] || 0, color: "var(--danger)"}
    ]);

    renderMiniChart("priorityChart", [
        {label: "Q0", value: priorityCounts.Q0 || 0, color: "var(--danger)"},
        {label: "Q1", value: priorityCounts.Q1 || 0, color: "var(--primary)"},
        {label: "Q2", value: priorityCounts.Q2 || 0, color: "#5b8def"},
        {label: "Q3", value: priorityCounts.Q3 || 0, color: "#64748b"}
    ]);


}
async function loadDashboard() {
    const result = await api("../api/dashboard");
    const data = result.data || {};
    renderMetricCards("dashboardMetrics", [
        {label: TEXT.planCount, value: data.planCount || 0},
        {label: TEXT.taskCount, value: data.taskCount || 0},
        {label: TEXT.completedTasks, value: data.completedTaskCount || 0},
        {label: TEXT.overdueTasks, value: data.overdueTaskCount || 0}
    ]);
    renderDashboardFocus(data);
    renderTaskReminders();
    renderCategoryHeatmap();
    renderPlanInvestmentDonut();
    document.getElementById("recentTaskList").innerHTML = (data.recentTasks || []).map(task => `
        <div class="list-row">
            <div><h3>${task.taskName}</h3><p>${planName(task.planId)} · ${task.deadline || ""}</p></div>
            <span class="badge ${badgeClass(task.status)}">${task.status || ""}</span>
        </div>
    `).join("") || `<div class="list-row"><p>${TEXT.noTask}</p></div>`;
    document.getElementById("recentRecordList").innerHTML = (data.recentRecords || []).map(record => `
        <div class="list-row">
            <div>
                <h3>${taskName(record.taskId)}</h3>
                <p>${record.studyDate || ""} · ${record.duration || 0} ${TEXT.minutes}</p>
                ${record.improvementPlan ? `<p>${TEXT.improvementPlan}${compactText(record.improvementPlan, 54)}</p>` : ""}
            </div>
        </div>
    `).join("") || `<div class="list-row"><p>${TEXT.noRecord}</p></div>`;
}

async function loadStatistics() {
    const result = await api("../api/statistics");
    const data = result.data || {};
    renderMetricCards("statisticsMetrics", [
        {label: TEXT.planNumber, value: data.planCount || 0},
        {label: TEXT.taskNumber, value: data.taskCount || 0},
        {label: TEXT.completedTasks, value: data.completedTaskCount || 0},
        {label: TEXT.totalDuration, value: `${data.totalDuration || 0} ${TEXT.minutes}`}
    ]);
    const rate = data.completionRate || 0;
    const progress = document.getElementById("completionProgress");
    progress.style.width = `${rate}%`;
    progress.textContent = `${rate}%`;
    renderStatisticsCharts(data);
}

window.loadDashboard = loadDashboard;
window.loadStatistics = loadStatistics;
window.renderTaskReminders = renderTaskReminders;
window.taskReminderInfo = taskReminderInfo;
window.renderCategoryHeatmap = renderCategoryHeatmap;
window.renderPlanInvestmentDonut = renderPlanInvestmentDonut;
window.renderStatisticsCharts = renderStatisticsCharts;
