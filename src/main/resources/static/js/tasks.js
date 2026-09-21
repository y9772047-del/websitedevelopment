const BATCH_TASK_EXAMPLE = [
    "完成需求分析 | 我的学习计划 | 作业实验 | Q0 | 2026-06-02 | 明确本周必须完成的学习目标",
    "整理知识点笔记 | 我的学习计划 | 复盘总结 | Q1 | 2026-06-05 | 汇总重点概念和容易出错的地方",
    "完成阶段练习 | 我的学习计划 | 专业能力 | Q2 | 2026-06-07 | 完成一组练习并记录错题",
    "优化学习安排 | 我的学习计划 | 自我提升 | Q3 | 2026-06-08 | 根据完成情况调整后续节奏"
].join("\n");

function isTaskOverdue(task) {
    return task.deadline && task.deadline < formatLocalDate(new Date()) && task.status !== TEXT.done;
}


function repeatTypeLabel(type) {
    if (type === "DAILY") return "每日";
    if (type === "WEEKLY") return "每周";
    if (type === "WORKDAY") return "工作日";
    return "不重复";
}

function normalizeTaskRepeatData(data) {
    data.reminderEnabled = data.reminderEnabled !== "false";
    data.reminderDaysBefore = Number(data.reminderDaysBefore ?? 3);
    if (!Number.isInteger(data.reminderDaysBefore) || data.reminderDaysBefore < 0 || data.reminderDaysBefore > 30) {
        return "提前提醒天数必须在 0 到 30 之间";
    }
    data.repeatType = data.repeatType || "NONE";
    if (data.repeatType === "NONE") {
        data.repeatEndDate = null;
        return null;
    }
    if (!data.deadline) {
        return "重复任务必须填写截止日期";
    }
    if (!data.repeatEndDate) {
        return "重复任务必须填写重复结束日期";
    }
    if (data.repeatEndDate < data.deadline) {
        return "重复结束日期不能早于截止日期";
    }
    return null;
}

function taskReminderBadge(task) {
    const info = typeof taskReminderInfo === "function" ? taskReminderInfo(task) : null;
    return info ? `<span class="badge ${info.className}">${info.label}</span>` : `<span class="muted-text">-</span>`;
}

function taskName(taskId) {
    const task = appState.tasks.find(item => String(item.id) === String(taskId));
    return task ? task.taskName : TEXT.unknownTask;
}

function taskDisplayName(task) {
    return task ? `${planName(task.planId)} / ${task.taskName}` : TEXT.unknownTask;
}

function taskDisplayNameById(taskId) {
    return taskDisplayName(taskById(taskId));
}

function taskById(taskId) {
    return appState.tasks.find(item => String(item.id) === String(taskId)) || null;
}

function taskSortTimestamp(task) {
    const timeText = task.deadline || task.createTime || "";
    const timestamp = Date.parse(String(timeText).replace(" ", "T"));
    return Number.isNaN(timestamp) ? 0 : timestamp;
}

function sortTasksByLatestTime(tasks) {
    return [...tasks].sort((first, second) => {
        const timeDiff = taskSortTimestamp(second) - taskSortTimestamp(first);
        if (timeDiff !== 0) return timeDiff;
        return Number(second.id || 0) - Number(first.id || 0);
    });
}

function isValidTaskCategory(category) {
    return appState.taskCategories.includes(category);
}

function fillTaskCategoryOptions() {
    const options = appState.taskCategories
        .map(category => `<option value="${category}">${category}</option>`)
        .join("");
    ["taskCategorySelect", "editTaskCategorySelect"].forEach(id => {
        const select = document.getElementById(id);
        if (select) {
            select.innerHTML = options || '<option value="">分类加载失败</option>';
            select.disabled = appState.taskCategories.length === 0;
        }
    });
}

async function loadTaskCategories() {
    const result = await api("../api/tasks/categories");
    appState.taskCategories = Array.isArray(result.data) ? result.data : [];
    fillTaskCategoryOptions();
}

async function loadPlanDetailTasks(planId) {
    const result = await api(`../api/tasks?planId=${planId}`);
    const tasks = result.data || [];
    document.getElementById("planDetailTaskTable").innerHTML = tasks.map(task => `
        <tr>
            <td>${task.taskName}</td>
            <td>${task.category || ""}</td>
            <td><span class="badge ${priorityClass(task.priority)}">${task.priority || ""}</span></td>
            <td>${task.deadline || ""}</td>
            <td><span class="badge ${badgeClass(task.status)}">${task.status || ""}</span></td>
        </tr>
    `).join("") || '<tr><td colspan="5">暂无学习任务</td></tr>';
}

async function loadTasks() {
    const result = await api("../api/tasks");
    appState.tasks = result.data || [];
    const table = document.getElementById("taskTable");
    if (table) {
        table.innerHTML = appState.tasks.map(task => `
            <tr>
                <td>${task.taskName}</td>
                <td>${planName(task.planId)}</td>
                <td>${task.category || ""}</td>
                <td><span class="badge ${priorityClass(task.priority)}">${task.priority || ""}</span></td>
                <td>${task.deadline || ""}</td>
                <td>${repeatTypeLabel(task.repeatType)}</td>
                <td>${taskReminderBadge(task)}</td>
                <td><span class="badge ${badgeClass(task.status)}">${task.status || ""}</span></td>
                <td>
                    <button class="btn btn-sm btn-primary" onclick='editTask(${JSON.stringify(task)})'>${TEXT.edit}</button>
                    <button class="btn btn-sm btn-outline-primary" onclick="deleteTask(${task.id})">${TEXT.remove}</button>
                </td>
            </tr>
        `).join("");
    }
    fillTaskOptions();
    if (typeof renderTaskReminders === "function") renderTaskReminders();
}

function fillTaskOptions() {
    const select = document.getElementById("recordTaskSelect");
    const nodeSelect = document.getElementById("importantNodeTaskSelect");
    const options = sortTasksByLatestTime(appState.tasks)
        .map(task => `<option value="${task.id}">${taskDisplayName(task)}</option>`)
        .join("");
    if (select) {
        select.innerHTML = `<option value="">${TEXT.selectTask}</option>` + options;
    }
    if (nodeSelect) {
        nodeSelect.innerHTML = `<option value="">不关联学习任务</option>` + options;
    }
}

function editTask(task) {
    fillForm("taskEditForm", task);
    showPage("taskEdit");
}

async function deleteTask(id) {
    if (!confirm(TEXT.confirmTask)) return;
    await api(`../api/tasks/${id}`, {method: "DELETE"});
    await refreshAll();
}

document.getElementById("taskForm").addEventListener("submit", async event => {
    event.preventDefault();
    const data = formToObject(event.target);
    delete data.id;
    const repeatError = normalizeTaskRepeatData(data);
    if (repeatError) {
        alert(repeatError);
        return;
    }
    const result = await api("../api/tasks", {method: "POST", body: JSON.stringify(data)});
    if (!result.success) return;
    resetForm("taskForm");
    await refreshAll();
});

document.getElementById("taskEditForm").addEventListener("submit", async event => {
    event.preventDefault();
    const data = formToObject(event.target);
    if (!data.id) {
        alert("缺少任务ID，无法保存修改");
        return;
    }
    const editRepeatError = normalizeTaskRepeatData(data);
    if (editRepeatError) {
        alert(editRepeatError);
        return;
    }
    const editResult = await api(`../api/tasks/${data.id}`, {method: "PUT", body: JSON.stringify(data)});
    if (!editResult.success) return;
    resetForm("taskEditForm");
    await refreshAll();
    showPage("tasks");
});

document.getElementById("batchTaskForm").addEventListener("submit", async event => {
    event.preventDefault();
    const lines = document.getElementById("batchTaskText").value
        .split("\n")
        .map(line => line.trim())
        .filter(Boolean);
    const tasks = [];
    for (let index = 0; index < lines.length; index++) {
        const line = lines[index];
        const rowNumber = index + 1;
        const parts = line.split("|").map(part => part.trim());
        if (parts.length !== 6) {
            alert(`第 ${rowNumber} 行格式错误：每行必须包含 6 个字段`);
            return;
        }
        const taskNameValue = parts[0];
        const planNameValue = parts[1];
        const category = parts[2];
        const priority = parts[3].toUpperCase();
        const deadline = parts[4];
        const description = parts[5];
        if (!taskNameValue) {
            alert(`第 ${rowNumber} 行格式错误：任务名称不能为空`);
            return;
        }
        if (!planNameValue) {
            alert(`第 ${rowNumber} 行格式错误：所属计划不能为空`);
            return;
        }
        const plan = findPlanByName(planNameValue);
        if (!plan) {
            alert(`第 ${rowNumber} 行格式错误：所属计划不存在`);
            return;
        }
        if (!isValidTaskCategory(category)) {
            alert(`第 ${rowNumber} 行格式错误：分类必须是固定选项之一`);
            return;
        }
        if (!["Q0", "Q1", "Q2", "Q3"].includes(priority)) {
            alert(`第 ${rowNumber} 行格式错误：优先级只能是 Q0、Q1、Q2、Q3`);
            return;
        }
        if (!isValidDateText(deadline)) {
            alert(`第 ${rowNumber} 行格式错误：截止日期格式必须是 YYYY-MM-DD`);
            return;
        }
        tasks.push({
            taskName: taskNameValue,
            planId: Number(plan.id),
            category,
            priority,
            deadline,
            description,
            status: TEXT.notDone
        });
    }
    if (tasks.length === 0) {
        alert("请先输入批量任务内容");
        return;
    }
    await api("../api/tasks/batch", {method: "POST", body: JSON.stringify(tasks)});
    document.getElementById("batchTaskText").value = "";
    await refreshAll();
});

document.getElementById("insertBatchExampleBtn").addEventListener("click", () => {
    document.getElementById("batchTaskText").value = BATCH_TASK_EXAMPLE;
});

document.getElementById("clearBatchTaskBtn").addEventListener("click", () => {
    document.getElementById("batchTaskText").value = "";
});

window.isTaskOverdue = isTaskOverdue;
window.taskName = taskName;
window.taskDisplayName = taskDisplayName;
window.taskDisplayNameById = taskDisplayNameById;
window.taskById = taskById;
window.sortTasksByLatestTime = sortTasksByLatestTime;
window.loadTaskCategories = loadTaskCategories;
window.loadPlanDetailTasks = loadPlanDetailTasks;
window.loadTasks = loadTasks;
window.fillTaskOptions = fillTaskOptions;
window.editTask = editTask;
window.deleteTask = deleteTask;