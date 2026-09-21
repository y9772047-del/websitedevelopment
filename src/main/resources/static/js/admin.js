const adminState = {
    detailModal: null
};

async function adminApi(url, options = {}) {
    const response = await fetch(url, {
        headers: {"Content-Type": "application/json"},
        ...options
    });
    if (response.status === 401 || response.status === 403) {
        alert("请使用管理员账号登录");
        window.location.href = "../index.html?v=20260705-login-blue";
        return {success: false, data: null};
    }
    const result = await response.json();
    if (!result.success) {
        alert(result.message || "操作失败");
    }
    return result;
}

function adminFormToQuery(formId) {
    const form = document.getElementById(formId);
    const params = new URLSearchParams();
    new FormData(form).forEach((value, key) => {
        if (value) params.append(key, value);
    });
    return params.toString();
}

function resetAdminForm(formId) {
    document.getElementById(formId).reset();
}

function adminBadgeClass(status) {
    if (status === "已完成" || status === "已处理" || status === 1) return "status-done";
    if (status === "已忽略" || status === "延期" || status === 0) return "status-delay";
    if (status === "未完成" || status === "未处理") return "status-waiting";
    return "status-doing";
}

function priorityClass(priority) {
    if (priority === "Q0") return "priority-q0";
    if (priority === "Q1") return "priority-q1";
    if (priority === "Q3") return "priority-q3";
    return "priority-q2";
}

function roleText(role) {
    return role === "admin" ? "管理员" : "普通学生";
}

function statusText(status) {
    return Number(status) === 1 ? "正常" : "禁用";
}

function shortText(text, length = 36) {
    if (!text) return "";
    return text.length > length ? `${text.slice(0, length)}...` : text;
}

function renderAdminMetrics(items) {
    document.getElementById("adminMetrics").innerHTML = items.map(item => `
        <div class="col-sm-6 col-xl-3">
            <div class="metric-card">
                <span>${item.label}</span>
                <strong>${item.value}</strong>
            </div>
        </div>
    `).join("");
}

async function loadAdminDashboard() {
    const result = await adminApi("../api/admin/dashboard/stats");
    const data = result.data || {};
    renderAdminMetrics([
        {label: "用户总数", value: data.userCount || 0},
        {label: "学习任务总数", value: data.taskCount || 0},
        {label: "已完成任务数", value: data.finishedTaskCount || 0},
        {label: "未完成任务数", value: data.unfinishedTaskCount || 0},
        {label: "逾期未完成任务数", value: data.overdueTaskCount || 0},
        {label: "今日新增任务数", value: data.todayNewTaskCount || 0},
        {label: "用户反馈总数", value: data.feedbackCount || 0},
        {label: "未处理反馈数量", value: data.pendingFeedbackCount || 0}
    ]);
    document.getElementById("recentAdminUsers").innerHTML = (data.recentUsers || []).map(user => `
        <div class="list-row">
            <div><h3>${user.username}</h3><p>${roleText(user.role)} · ${user.createTime || ""}</p></div>
            <span class="badge ${adminBadgeClass(user.status)}">${statusText(user.status)}</span>
        </div>
    `).join("") || '<div class="list-row"><p>暂无用户</p></div>';
    document.getElementById("recentAdminFeedbacks").innerHTML = (data.recentFeedbacks || []).map(item => `
        <div class="list-row">
            <div><h3>${shortText(item.content, 24)}</h3><p>${item.createTime || ""}</p></div>
            <span class="badge ${adminBadgeClass(item.status)}">${item.status || ""}</span>
        </div>
    `).join("") || '<div class="list-row"><p>暂无反馈</p></div>';
}

async function loadAdminUsers() {
    const query = adminFormToQuery("adminUserSearchForm");
    const result = await adminApi(`../api/admin/users${query ? `?${query}` : ""}`);
    const users = result.data || [];
    document.getElementById("adminUserTable").innerHTML = users.map(user => `
        <tr>
            <td>${user.id}</td>
            <td>${user.username}</td>
            <td>${roleText(user.role)}</td>
            <td><span class="badge ${adminBadgeClass(user.status)}">${statusText(user.status)}</span></td>
            <td>${user.createTime || ""}</td>
            <td>${user.taskCount || 0}</td>
            <td>
                <button class="btn btn-sm btn-primary" onclick="showAdminUserDetail(${user.id})">查看</button>
                ${user.role === "admin" ? "" : `<button class="btn btn-sm btn-outline-primary" onclick="toggleUserStatus(${user.id}, ${Number(user.status) === 1 ? 0 : 1})">${Number(user.status) === 1 ? "禁用" : "启用"}</button>`}
            </td>
        </tr>
    `).join("") || '<tr><td colspan="7">暂无用户</td></tr>';
}

async function showAdminUserDetail(id) {
    const result = await adminApi(`../api/admin/users/${id}`);
    if (!result.success) return;
    const user = result.data;
    showAdminModal("用户详情", `
        <p><strong>用户名：</strong>${user.username}</p>
        <p><strong>角色：</strong>${roleText(user.role)}　<strong>状态：</strong>${statusText(user.status)}</p>
        <p><strong>注册时间：</strong>${user.createTime || ""}</p>
        <div class="row g-3 mb-3">
            <div class="col-md-3"><div class="metric-card"><span>任务数量</span><strong>${user.taskCount || 0}</strong></div></div>
            <div class="col-md-3"><div class="metric-card"><span>已完成</span><strong>${user.completedTaskCount || 0}</strong></div></div>
            <div class="col-md-3"><div class="metric-card"><span>未完成</span><strong>${user.unfinishedTaskCount || 0}</strong></div></div>
            <div class="col-md-3"><div class="metric-card"><span>逾期</span><strong>${user.overdueTaskCount || 0}</strong></div></div>
        </div>
        <h6>最近创建的学习任务</h6>
        ${(user.recentTasks || []).map(task => `<div class="list-row mb-2"><div><h3>${task.taskName}</h3><p>${task.deadline || ""} · ${task.status || ""}</p></div><span class="badge ${adminBadgeClass(task.status)}">${task.status || ""}</span></div>`).join("") || "<p>暂无任务</p>"}
    `);
}

async function toggleUserStatus(id, status) {
    if (!confirm(status === 0 ? "确定禁用这个用户吗？" : "确定启用这个用户吗？")) return;
    await adminApi(`../api/admin/users/${id}/status`, {method: "PUT", body: JSON.stringify({status})});
    await loadAdminUsers();
    await loadAdminDashboard();
}

async function loadAdminTasks() {
    const query = adminFormToQuery("adminTaskSearchForm");
    const result = await adminApi(`../api/admin/tasks${query ? `?${query}` : ""}`);
    const tasks = result.data || [];
    document.getElementById("adminTaskTable").innerHTML = tasks.map(task => `
        <tr>
            <td>${task.id}</td>
            <td>${task.taskName}</td>
            <td>${task.username}</td>
            <td>${task.planName || ""}</td>
            <td>${task.category || ""}</td>
            <td><span class="badge ${priorityClass(task.priority)}">${task.priority || ""}</span></td>
            <td>${task.deadline || ""}</td>
            <td><span class="badge ${adminBadgeClass(task.status)}">${task.status || ""}</span></td>
            <td>${task.createTime || ""}</td>
            <td>
                <button class="btn btn-sm btn-primary" onclick="showAdminTaskDetail(${task.id})">查看</button>
                <button class="btn btn-sm btn-outline-primary" onclick="deleteAdminTask(${task.id})">删除</button>
            </td>
        </tr>
    `).join("") || '<tr><td colspan="10">暂无任务</td></tr>';
}

async function showAdminTaskDetail(id) {
    const result = await adminApi(`../api/admin/tasks/${id}`);
    if (!result.success) return;
    const task = result.data;
    showAdminModal("任务详情", `
        <p><strong>任务名称：</strong>${task.taskName}</p>
        <p><strong>任务描述：</strong>${task.description || ""}</p>
        <p><strong>所属用户：</strong>${task.username}　<strong>所属计划：</strong>${task.planName || ""}</p>
        <p><strong>分类：</strong>${task.category || ""}　<strong>优先级：</strong>${task.priority || ""}</p>
        <p><strong>截止日期：</strong>${task.deadline || ""}　<strong>状态：</strong>${task.status || ""}</p>
        <p><strong>创建时间：</strong>${task.createTime || ""}</p>
        <p><strong>更新时间：</strong>${task.updateTime || ""}</p>
    `);
}

async function deleteAdminTask(id) {
    if (!confirm("确定删除这个异常任务吗？")) return;
    await adminApi(`../api/admin/tasks/${id}`, {method: "DELETE"});
    await loadAdminTasks();
    await loadAdminDashboard();
}

async function loadAdminFeedbacks() {
    const query = adminFormToQuery("adminFeedbackSearchForm");
    const result = await adminApi(`../api/admin/feedbacks${query ? `?${query}` : ""}`);
    const feedbacks = result.data || [];
    document.getElementById("adminFeedbackTable").innerHTML = feedbacks.map(item => `
        <tr>
            <td>${item.id}</td>
            <td>${item.username}</td>
            <td>${shortText(item.content)}</td>
            <td><span class="badge ${adminBadgeClass(item.status)}">${item.status || ""}</span></td>
            <td>${item.createTime || ""}</td>
            <td>
                <button class="btn btn-sm btn-primary" onclick="showAdminFeedbackDetail(${item.id})">查看</button>
                <button class="btn btn-sm btn-outline-primary" onclick="showProcessFeedback(${item.id})">处理</button>
            </td>
        </tr>
    `).join("") || '<tr><td colspan="6">暂无反馈</td></tr>';
}

async function showAdminFeedbackDetail(id) {
    const result = await adminApi(`../api/admin/feedbacks/${id}`);
    if (!result.success) return;
    const item = result.data;
    showAdminModal("反馈详情", `
        <p><strong>提交用户：</strong>${item.username}</p>
        <p><strong>提交时间：</strong>${item.createTime || ""}</p>
        <p><strong>当前状态：</strong>${item.status || ""}</p>
        <p><strong>反馈内容：</strong></p>
        <div class="admin-detail-box">${item.content || ""}</div>
        <p class="mt-3"><strong>管理员处理备注：</strong></p>
        <div class="admin-detail-box">${item.reply || "暂无备注"}</div>
    `);
}

async function showProcessFeedback(id) {
    const result = await adminApi(`../api/admin/feedbacks/${id}`);
    if (!result.success) return;
    const item = result.data;
    showAdminModal("处理反馈", `
        <p><strong>提交用户：</strong>${item.username}</p>
        <p><strong>反馈内容：</strong>${item.content || ""}</p>
        <form id="processFeedbackForm" class="row g-3">
            <div class="col-md-4"><label class="form-label">处理状态</label><select class="form-select" name="status"><option ${item.status === "未处理" ? "selected" : ""}>未处理</option><option ${item.status === "已处理" ? "selected" : ""}>已处理</option><option ${item.status === "已忽略" ? "selected" : ""}>已忽略</option></select></div>
            <div class="col-12"><label class="form-label">处理备注</label><textarea class="form-control" name="reply" rows="4">${item.reply || ""}</textarea></div>
            <div class="col-12"><button class="btn btn-primary">保存处理结果</button></div>
        </form>
    `);
    document.getElementById("processFeedbackForm").addEventListener("submit", async event => {
        event.preventDefault();
        const data = Object.fromEntries(new FormData(event.target).entries());
        await adminApi(`../api/admin/feedbacks/${id}`, {method: "PUT", body: JSON.stringify(data)});
        adminState.detailModal.hide();
        await loadAdminFeedbacks();
        await loadAdminDashboard();
    });
}

function showAdminModal(title, html) {
    document.getElementById("adminDetailTitle").textContent = title;
    document.getElementById("adminDetailBody").innerHTML = html;
    adminState.detailModal.show();
}

function showAdminPage(pageId) {
    document.querySelectorAll(".admin-section").forEach(section => section.classList.add("d-none"));
    document.getElementById(pageId).classList.remove("d-none");
    document.querySelectorAll(".admin-menu[data-page]").forEach(button => {
        button.classList.toggle("active", button.dataset.page === pageId);
    });
    if (pageId === "adminDashboard") loadAdminDashboard();
    if (pageId === "adminUsers") loadAdminUsers();
    if (pageId === "adminTasks") loadAdminTasks();
    if (pageId === "adminFeedbacks") loadAdminFeedbacks();
    if (pageId === "adminThemes") {
        applyTheme(getSavedThemeKey());
        renderThemeCards();
    }
}

async function checkAdminLogin() {
    const result = await adminApi("../api/auth/current");
    if (!result.success || !result.data || result.data.role !== "admin") {
        alert("请使用管理员账号登录");
        window.location.href = "../index.html?v=20260705-login-blue";
    }
}

async function adminLogout() {
    await adminApi("../api/auth/logout", {method: "POST"});
    window.location.href = "../index.html?v=20260705-login-blue";
}

document.querySelectorAll(".admin-menu[data-page]").forEach(button => {
    button.addEventListener("click", () => showAdminPage(button.dataset.page));
});

document.getElementById("adminLogoutBtn").addEventListener("click", adminLogout);
document.getElementById("adminUserSearchForm").addEventListener("submit", event => {
    event.preventDefault();
    loadAdminUsers();
});
document.getElementById("adminTaskSearchForm").addEventListener("submit", event => {
    event.preventDefault();
    loadAdminTasks();
});
document.getElementById("adminFeedbackSearchForm").addEventListener("submit", event => {
    event.preventDefault();
    loadAdminFeedbacks();
});

window.addEventListener("load", async () => {
    adminState.detailModal = new bootstrap.Modal(document.getElementById("adminDetailModal"));
    await checkAdminLogin();
    await loadAdminDashboard();
});
