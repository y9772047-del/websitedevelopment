const TEXT = {
    done: "已完成",
    delayed: "延期",
    notStarted: "未开始",
    notDone: "未完成",
    unlinked: "未关联",
    unknownTask: "未知任务",
    selectPlan: "请选择计划",
    selectTask: "请选择任务",
    edit: "编辑",
    remove: "删除",
    to: "至",
    minutes: "分钟",
    noTask: "暂无任务",
    noRecord: "暂无学习记录",
    confirmPlan: "确定删除这个学习计划吗？",
    confirmTask: "确定删除这个学习任务吗？",
    confirmRecord: "确定删除这条学习记录吗？",
    planCount: "学习计划数量",
    taskCount: "任务总数",
    completedTasks: "已完成任务",
    overdueTasks: "延期任务",
    planNumber: "计划数量",
    taskNumber: "任务数量",
    totalDuration: "总学习时长",
    problemReflection: "问题与反思：",
    improvementPlan: "改进计划：",
    operationFailed: "操作失败",
    noCalendarRecord: "当天暂无学习记录",
    noCalendarDetail: "当天暂无学习记录和重要节点",
    confirmImportantNode: "确定删除这个重要节点吗？",
    confirmKeywordRule: "确定删除这条关键词规则吗？"
};

const appState = {
    plans: [],
    tasks: [],
    records: [],
    importantNodes: [],
    importantKeywordRules: [],
    feedbacks: [],
    taskCategories: [],
    currentPlanDetail: null,
    calendarDate: new Date(),
    currentImportantNodeYear: new Date().getFullYear(),
    currentHeatmapFilter: "全部",
    currentPlanInvestmentRange: "30"
};



async function api(url, options = {}) {
    const response = await fetch(url, {
        headers: {"Content-Type": "application/json"},
        ...options
    });
    if (response.status === 401) {
        window.location.href = "../index.html?v=20260705-login-blue";
        return {success: false, data: null};
    }
    const result = await response.json();
    if (!result.success) {
        alert(result.message || TEXT.operationFailed);
    }
    return result;
}

function formToObject(form) {
    const data = Object.fromEntries(new FormData(form).entries());
    Object.keys(data).forEach(key => {
        if (data[key] === "") data[key] = null;
    });
    return data;
}

function resetForm(formId) {
    const form = document.getElementById(formId);
    form.reset();
    const idInput = form.querySelector('input[name="id"]');
    if (idInput) idInput.value = "";
}

function badgeClass(status) {
    if (status === TEXT.done) return "status-done";
    if (status === TEXT.delayed) return "status-delay";
    if (status === TEXT.notStarted || status === TEXT.notDone) return "status-waiting";
    return "status-doing";
}

function priorityClass(priority) {
    if (priority === "Q0") return "priority-q0";
    if (priority === "Q1") return "priority-q1";
    if (priority === "Q3") return "priority-q3";
    return "priority-q2";
}

function planName(planId) {
    const plan = appState.plans.find(item => String(item.id) === String(planId));
    return plan ? plan.planName : TEXT.unlinked;
}

function findPlanByName(planNameValue) {
    return appState.plans.find(plan => plan.planName === planNameValue || String(plan.id) === planNameValue);
}

function isValidDateText(dateText) {
    const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(dateText);
    if (!match) return false;
    const year = Number(match[1]);
    const month = Number(match[2]);
    const day = Number(match[3]);
    const date = new Date(year, month - 1, day);
    return date.getFullYear() === year && date.getMonth() === month - 1 && date.getDate() === day;
}

function formatLocalDate(date) {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const day = String(date.getDate()).padStart(2, "0");
    return `${year}-${month}-${day}`;
}

function formatMonthDay(dateText) {
    if (!dateText || !isValidDateText(dateText)) return "";
    return dateText.slice(5);
}

function formatNodeTime(node) {
    if (!node.startTime) return "全天";
    const start = String(node.startTime).slice(0, 5);
    const end = node.endTime ? String(node.endTime).slice(0, 5) : "";
    return end ? `${start}-${end}` : start;
}

function fillForm(formId, data) {
    const form = document.getElementById(formId);
    Object.keys(data).forEach(key => {
        if (form.elements[key]) form.elements[key].value = data[key] ?? "";
    });
}


function compactText(value, maxLength = 70) {
    const text = String(value || "").trim();
    if (!text) return "";
    return text.length > maxLength ? `${text.slice(0, maxLength)}...` : text;
}


async function loadPlans() {
    const result = await api("../api/plans");
    appState.plans = result.data || [];
    const table = document.getElementById("planTable");
    if (table) {
        table.innerHTML = appState.plans.map(plan => `
            <tr>
                <td>${plan.planName}</td>
                <td>${plan.goal || ""}</td>
                <td>${plan.startDate || ""} ${TEXT.to} ${plan.endDate || ""}</td>
                <td><span class="badge ${badgeClass(plan.status)}">${plan.status || ""}</span></td>
                <td>
                    <button class="btn btn-sm btn-primary" onclick='editPlan(${JSON.stringify(plan)})'>${TEXT.edit}</button>
                    <button class="btn btn-sm btn-outline-primary" onclick='openPlanDetail(${JSON.stringify(plan)})'>查看详情</button>
                    <button class="btn btn-sm btn-outline-primary" onclick="deletePlan(${plan.id})">${TEXT.remove}</button>
                </td>
            </tr>
        `).join("");
    }
    fillPlanOptions();
}

function fillPlanOptions() {
    const select = document.getElementById("taskPlanSelect");
    const editSelect = document.getElementById("editTaskPlanSelect");
    const nodeSelect = document.getElementById("importantNodePlanSelect");
    const options = `<option value="">${TEXT.selectPlan}</option>` + appState.plans
        .map(plan => `<option value="${plan.id}">${displayPlanName(plan)}</option>`)
        .join("");
    if (select) select.innerHTML = options;
    if (editSelect) editSelect.innerHTML = options;
    if (nodeSelect) nodeSelect.innerHTML = `<option value="">不关联学习计划</option>` + appState.plans
        .map(plan => `<option value="${plan.id}">${displayPlanName(plan)}</option>`)
        .join("");
    renderAvailablePlanList();
}

function displayPlanName(plan) {
    const name = plan && plan.planName ? plan.planName.trim() : "";
    return name || `未命名计划（ID：${plan.id}）`;
}

function renderAvailablePlanList() {
    const list = document.getElementById("availablePlanList");
    if (!list) return;
    if (appState.plans.length === 0) {
        list.innerHTML = '<div class="available-plan-empty">暂无学习计划，请先新增学习计划。</div>';
        return;
    }
    list.innerHTML = appState.plans
        .map(plan => `<span class="available-plan-item">${displayPlanName(plan)}</span>`)
        .join("");
}

function editPlan(plan) {
    fillForm("planEditForm", plan);
    showPage("planEdit");
}

async function deletePlan(id) {
    if (!confirm(TEXT.confirmPlan)) return;
    await api(`../api/plans/${id}`, {method: "DELETE"});
    await refreshAll();
}

document.getElementById("planForm").addEventListener("submit", async event => {
    event.preventDefault();
    const data = formToObject(event.target);
    delete data.id;
    await api("../api/plans", {method: "POST", body: JSON.stringify(data)});
    resetForm("planForm");
    await refreshAll();
});

document.getElementById("planEditForm").addEventListener("submit", async event => {
    event.preventDefault();
    const data = formToObject(event.target);
    if (!data.id) {
        alert("缺少计划ID，无法保存修改");
        return;
    }
    await api(`../api/plans/${data.id}`, {method: "PUT", body: JSON.stringify(data)});
    resetForm("planEditForm");
    await refreshAll();
    showPage("plans");
});

async function openPlanDetail(plan) {
    appState.currentPlanDetail = plan;
    document.getElementById("planDetailTitle").textContent = plan.planName;
    document.getElementById("planDetailMeta").textContent = `${plan.startDate || ""} ${TEXT.to} ${plan.endDate || ""}｜${plan.status || ""}`;
    await loadPlanDetailTasks(plan.id);
    showPage("planDetail");
}



async function loadFeedbacks() {
    const result = await api("../api/feedback/my");
    appState.feedbacks = result.data || [];
    const table = document.getElementById("feedbackTable");
    if (!table) return;
    table.innerHTML = appState.feedbacks.map(item => `
        <tr>
            <td>${item.content || ""}</td>
            <td><span class="badge ${badgeClass(item.status)}">${item.status || ""}</span></td>
            <td>${item.reply || "暂无回复"}</td>
            <td>${item.createTime || ""}</td>
        </tr>
    `).join("") || '<tr><td colspan="4">暂无反馈</td></tr>';
}


document.getElementById("feedbackForm").addEventListener("submit", async event => {
    event.preventDefault();
    const data = formToObject(event.target);
    await api("../api/feedback", {method: "POST", body: JSON.stringify(data)});
    resetForm("feedbackForm");
    alert("反馈提交成功");
    await loadFeedbacks();
});



async function generateWeeklyReport() {
    const button = document.getElementById("weeklyReportBtn");
    const box = document.getElementById("weeklyReportBox");
    if (!button || !box) return;
    button.disabled = true;
    const oldText = button.textContent;
    button.textContent = "生成中...";
    box.innerHTML = `<div class="list-row"><p>DeepSeek 正在生成周报，请稍候。</p></div>`;
    try {
        const result = await api("../api/ai/generate-weekly-report", {method: "POST"});
        if (result.success && result.data) {
            const report = result.data;
            box.innerHTML = `
                <div class="list-row"><div><h3>本周总结</h3><p>${report.weeklySummary || "暂无总结"}</p></div></div>
                <div class="list-row"><div><h3>主要问题</h3><p>${report.mainProblems || "暂无明确问题"}</p></div></div>
                <div class="list-row"><div><h3>下周建议</h3><p>${report.nextWeekSuggestions || "暂无建议"}</p></div></div>
                <div class="list-row"><div><h3>行动清单</h3><p>${(report.suggestedActions || []).join("；") || "暂无行动清单"}</p></div><span class="badge status-doing">${report.generationSourceLabel || "DeepSeek"}</span></div>
            `;
        } else {
            box.innerHTML = `<div class="list-row"><p>${result.message || "AI 周报生成失败"}</p></div>`;
        }
    } finally {
        button.disabled = false;
        button.textContent = oldText;
    }
}

async function logout() {
    await api("../api/auth/logout", {method: "POST"});
    window.location.href = "../index.html?v=20260705-login-blue";
}

function showPage(pageId) {
    document.querySelectorAll(".page-section").forEach(section => section.classList.add("d-none"));
    document.getElementById(pageId).classList.remove("d-none");
    document.querySelectorAll(".nav-actions .nav-link").forEach(item => {
        item.classList.toggle("active", item.dataset.page === pageId);
    });
    if (pageId === "dashboard") loadDashboard();
    if (pageId === "calendar") loadCalendar();
    if (pageId === "importantNodes") {
        loadImportantKeywordRules();
        loadImportantNodes();
    }
    if (pageId === "statistics") loadStatistics();
    if (pageId === "feedback") loadFeedbacks();
}

async function refreshAll() {
    await loadTaskCategories();
    await loadPlans();
    await loadTasks();
    await loadRecords();
    await loadImportantNodes();
    await loadImportantKeywordRules();
    await loadFeedbacks();
    await loadDashboard();
    await loadStatistics();
}

document.querySelectorAll(".nav-actions [data-page]").forEach(button => {
    button.addEventListener("click", () => showPage(button.dataset.page));
});

document.getElementById("logoutBtn").addEventListener("click", logout);
document.getElementById("weeklyReportBtn")?.addEventListener("click", generateWeeklyReport);


window.addEventListener("load", async () => {
    await refreshAll();
    showPage("dashboard");
});
