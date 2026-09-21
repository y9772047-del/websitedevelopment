let recordSubmitInFlight = false;

function resetRecordForm() {
    resetForm("recordForm");
    renderAiSuggestedActions([]);
    const tip = document.getElementById("recordAiImprovementTip");
    if (tip) tip.textContent = "填写学习内容和问题与反思后生成改进计划。";
}

function setRecordSubmitDisabled(disabled) {
    const button = document.querySelector("[data-record-submit]");
    if (button) button.disabled = disabled;
}

async function loadRecords() {
    const result = await api("../api/records");
    appState.records = result.data || [];
    const table = document.getElementById("recordTable");
    if (!table) return;
    table.innerHTML = appState.records.map(record => `
        <tr>
            <td>${taskDisplayNameById(record.taskId)}</td>
            <td>${record.studyDate || ""}</td>
            <td>${record.duration || 0} ${TEXT.minutes}</td>
            <td title="${record.content || ""}">${compactText(record.content)}</td>
            <td title="${record.problems || ""}">${compactText(record.problems)}</td>
            <td title="${record.improvementPlan || ""}">${compactText(record.improvementPlan)}</td>
            <td><span class="badge ${badgeClass(record.completionStatus)}">${record.completionStatus || ""}</span></td>
            <td>
                <button class="btn btn-sm btn-primary" onclick='editRecord(${JSON.stringify(record)})'>${TEXT.edit}</button>
                <button class="btn btn-sm btn-outline-primary" onclick="deleteRecord(${record.id})">${TEXT.remove}</button>
            </td>
        </tr>
    `).join("");
}

function editRecord(record) {
    fillForm("recordForm", record);
    renderAiSuggestedActions([]);
    showPage("records");
}

async function deleteRecord(id) {
    if (!confirm(TEXT.confirmRecord)) return;
    await api(`../api/records/${id}`, {method: "DELETE"});
    await refreshAll();
}

document.getElementById("recordForm").addEventListener("submit", async event => {
    event.preventDefault();
    if (recordSubmitInFlight) return;
    recordSubmitInFlight = true;
    setRecordSubmitDisabled(true);
    try {
        const data = formToObject(event.target);
        const url = data.id ? `../api/records/${data.id}` : "../api/records";
        const method = data.id ? "PUT" : "POST";
        await api(url, {method, body: JSON.stringify(data)});
        resetRecordForm();
        await refreshAll();
    } finally {
        recordSubmitInFlight = false;
        setRecordSubmitDisabled(false);
    }
});

function renderAiSuggestedActions(actions) {
    const box = document.getElementById("recordAiSuggestedActionsBox");
    const list = document.getElementById("recordAiSuggestedActionsList");
    if (!box || !list) return;
    list.innerHTML = "";
    const validActions = Array.isArray(actions)
        ? actions.map(action => String(action || "").trim()).filter(Boolean)
        : [];
    if (validActions.length === 0) {
        box.classList.add("d-none");
        return;
    }
    validActions.forEach(action => {
        const item = document.createElement("li");
        item.textContent = action;
        list.appendChild(item);
    });
    box.classList.remove("d-none");
}

function buildRecordAiRequest(form) {
    const task = taskById(form.elements.taskId.value);
    const plan = task ? appState.plans.find(item => String(item.id) === String(task.planId)) : null;
    const studyContent = form.elements.content.value.trim();
    const completion = form.elements.completionStatus.value;
    const problems = form.elements.problems.value.trim();
    return {
        planId: plan ? plan.id : null,
        planName: plan ? displayPlanName(plan) : "",
        taskName: task ? task.taskName : "",
        studyContent,
        completion,
        problems,
        problemSummary: problems
    };
}

document.getElementById("recordAiImprovementBtn").addEventListener("click", async () => {
    const button = document.getElementById("recordAiImprovementBtn");
    const tip = document.getElementById("recordAiImprovementTip");
    const form = document.getElementById("recordForm");
    const data = buildRecordAiRequest(form);
    if (!data.taskName) {
        tip.textContent = "请先选择学习任务。";
        return;
    }
    if (!data.studyContent && !data.problems) {
        tip.textContent = "请先填写学习内容，或写下问题与反思。";
        return;
    }
    button.disabled = true;
    const oldText = button.textContent;
    button.textContent = "生成中...";
    renderAiSuggestedActions([]);
    tip.textContent = "正在根据本次学习记录生成改进计划。";
    try {
        const result = await api("../api/ai/generate-improvement", {method: "POST", body: JSON.stringify(data)});
        if (result.success && result.data) {
            form.elements.improvementPlan.value = result.data.improvementPlan || "";
            renderAiSuggestedActions(result.data.suggestedActions);
            const sourceLabel = result.data.generationSourceLabel || "未知来源";
            const sourceDetail = result.data.generationSourceDetail ? `；${result.data.generationSourceDetail}` : "";
            tip.textContent = `已生成改进计划（来源：${sourceLabel}${sourceDetail}），检查后保存学习记录。`;
        } else {
            tip.textContent = "AI 生成失败，请稍后重试。";
        }
    } finally {
        button.disabled = false;
        button.textContent = oldText;
    }
});

window.resetRecordForm = resetRecordForm;
window.setRecordSubmitDisabled = setRecordSubmitDisabled;
window.loadRecords = loadRecords;
window.editRecord = editRecord;
window.deleteRecord = deleteRecord;
window.renderAiSuggestedActions = renderAiSuggestedActions;
window.buildRecordAiRequest = buildRecordAiRequest;
