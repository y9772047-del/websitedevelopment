// Important node and keyword-rule page logic. Loaded after app.js and before calendar.js.
const IMPORTANT_NODE_TYPES = ["考试", "截止", "比赛", "汇报", "其他"];

const KEYWORD_RULE_EFFECT_LABELS = {
    SCORE: "加分",
    MIN_IMPORTANT: "至少重要",
    URGENT: "直接紧急"
};

function initImportantNodeYearSelect() {
    const select = document.getElementById("importantNodeYearSelect");
    if (!select) return;
    const currentYear = new Date().getFullYear();
    const years = [];
    for (let year = currentYear - 2; year <= currentYear + 2; year++) {
        years.push(year);
    }
    if (!years.includes(appState.currentImportantNodeYear)) {
        years.push(appState.currentImportantNodeYear);
        years.sort();
    }
    select.innerHTML = years.map(year => `<option value="${year}">${year} 年</option>`).join("");
    select.value = String(appState.currentImportantNodeYear);
}

async function loadImportantNodes() {
    initImportantNodeYearSelect();
    const year = appState.currentImportantNodeYear;
    const result = await api(`../api/important-nodes?year=${year}`);
    appState.importantNodes = result.data || [];
    renderImportantNodes();
    if (document.getElementById("calendar") && !document.getElementById("calendar").classList.contains("d-none")) {
        loadCalendar();
    }
}

async function loadImportantKeywordRules() {
    const result = await api("../api/important-node-keyword-rules");
    appState.importantKeywordRules = result.data || [];
    renderImportantKeywordRules();
}

function renderImportantKeywordRules() {
    const table = document.getElementById("importantKeywordRuleTable");
    if (!table) return;
    table.innerHTML = appState.importantKeywordRules.map(rule => `
        <tr>
            <td>${escapeHtml(rule.keyword)}</td>
            <td>${escapeHtml(rule.nodeType || "全部类型")}</td>
            <td>${escapeHtml(KEYWORD_RULE_EFFECT_LABELS[rule.effect] || rule.effect || "")}</td>
            <td>${rule.effect === "SCORE" ? escapeHtml(rule.scoreDelta ?? 0) : "-"}</td>
            <td><span class="badge ${rule.enabled ? "status-done" : "status-waiting"}">${rule.enabled ? "启用" : "停用"}</span></td>
            <td>
                <button class="btn btn-sm btn-primary" type="button" onclick="editImportantKeywordRuleById(${rule.id})">${TEXT.edit}</button>
                <button class="btn btn-sm btn-outline-primary" type="button" onclick="deleteImportantKeywordRule(${rule.id})">${TEXT.remove}</button>
            </td>
        </tr>
    `).join("") || '<tr><td colspan="6">暂无关键词规则</td></tr>';
}

function buildKeywordRulePayload(form) {
    const data = formToObject(form);
    data.enabled = Boolean(form.elements.enabled?.checked);
    data.scoreDelta = data.effect === "SCORE" ? Number(data.scoreDelta || 10) : 0;
    if (!data.nodeType) data.nodeType = null;
    return data;
}

function resetKeywordRuleForm() {
    resetForm("importantKeywordRuleForm");
    const form = document.getElementById("importantKeywordRuleForm");
    if (!form) return;
    setFormControlValue(form, "effect", "SCORE");
    setFormControlValue(form, "scoreDelta", "10");
    if (form.elements.enabled) form.elements.enabled.checked = true;
}

function editImportantKeywordRuleById(id) {
    const rule = appState.importantKeywordRules.find(item => String(item.id) === String(id));
    if (rule) editImportantKeywordRule(rule);
}

function editImportantKeywordRule(rule) {
    const form = document.getElementById("importantKeywordRuleForm");
    if (!form) return;
    fillForm("importantKeywordRuleForm", rule);
    if (form.elements.enabled) form.elements.enabled.checked = Boolean(rule.enabled);
    setFormControlValue(form, "scoreDelta", rule.scoreDelta ?? 10);
}

async function deleteImportantKeywordRule(id) {
    if (!confirm(TEXT.confirmKeywordRule)) return;
    await api(`../api/important-node-keyword-rules/${id}`, {method: "DELETE"});
    await loadImportantKeywordRules();
    await loadImportantNodes();
}

function groupImportantNodesByMonth(nodes) {
    return nodes.reduce((groups, node) => {
        const startDate = nodeStartDate(node);
        if (!startDate) return groups;
        const month = Number(startDate.slice(5, 7));
        if (!groups[month]) groups[month] = [];
        groups[month].push(node);
        return groups;
    }, {});
}

function importantNodeType(node) {
    return node.type || "";
}

function nodeStartDate(node) {
    return node.startDate || "";
}

function nodeEndDate(node) {
    return node.endDate || "";
}

function nodeDescription(node) {
    return node.description || "";
}

function parseLocalDate(dateText) {
    if (!isValidDateText(dateText)) return null;
    const [year, month, day] = dateText.split("-").map(Number);
    return new Date(year, month - 1, day);
}

function formatNodeDateRange(node) {
    const startDate = nodeStartDate(node);
    const endDate = nodeEndDate(node);
    if (!startDate) return "";
    if (!endDate || endDate === startDate) return formatMonthDay(startDate);
    return `${formatMonthDay(startDate)} - ${formatMonthDay(endDate)}`;
}

function formatCompactNodeDate(dateText, showYear = false) {
    if (!dateText || !isValidDateText(dateText)) return "";
    return showYear ? dateText.replaceAll("-", ".") : dateText.slice(5).replace("-", ".");
}

function formatCompactNodeTime(timeText) {
    return timeText ? String(timeText).slice(0, 5) : "";
}

function formatImportantNodeTimeRange(node) {
    const startDate = nodeStartDate(node);
    const endDate = nodeEndDate(node);
    const startTime = formatCompactNodeTime(node.startTime);
    const endTime = formatCompactNodeTime(node.endTime);
    if (!startDate) return "";

    const isSameDay = !endDate || endDate === startDate;
    const showYear = endDate && startDate.slice(0, 4) !== endDate.slice(0, 4);
    const startText = [formatCompactNodeDate(startDate, showYear), startTime].filter(Boolean).join(" ");

    if (!endDate) return startText;
    if (isSameDay) {
        if (!startTime) return formatCompactNodeDate(startDate, showYear);
        return endTime ? `${startText}-${endTime}` : startText;
    }

    const endText = [formatCompactNodeDate(endDate, showYear), endTime].filter(Boolean).join(" ");
    return [startText, endText].filter(Boolean).join(" ~ ");
}

function renderImportantNodeCard(node) {
    const timeRange = formatImportantNodeTimeRange(node);
    const type = importantNodeType(node);
    const title = node.title || "";
    const importance = node.importance || "";
    const description = nodeDescription(node);
    return `
        <div class="important-node-card">
            <div class="important-node-row">
                <div class="important-node-main">
                    ${timeRange ? `<span class="important-node-date">${timeRange}</span>` : ""}
                    ${type ? `<span class="badge important-node-type ${typeClass(type)}">[${escapeHtml(type)}]</span>` : ""}
                    ${title ? `<strong>${escapeHtml(title)}</strong>` : ""}
                    ${importance ? `<span class="badge important-node-importance ${importanceClass(importance)}">${escapeHtml(importance)}</span>` : ""}
                </div>
                <div class="important-node-actions">
                    <button class="btn btn-sm btn-primary" type="button" onclick="editImportantNodeById(${node.id})">${TEXT.edit}</button>
                    <button class="btn btn-sm btn-outline-primary" type="button" onclick="deleteImportantNode(${node.id})">${TEXT.remove}</button>
                </div>
            </div>
            ${description ? `<p>${escapeHtml(description)}</p>` : ""}
        </div>
    `;
}

function setFormControlValue(form, name, value) {
    const control = form?.elements?.namedItem(name);
    if (control) control.value = value ?? "";
}

function escapeHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll("\"", "&quot;")
        .replaceAll("'", "&#39;");
}

function showModalById(modalId) {
    const modal = document.getElementById(modalId);
    if (!modal) return;
    if (window.bootstrap?.Modal) {
        bootstrap.Modal.getOrCreateInstance(modal).show();
        return;
    }
    modal.classList.add("show");
    modal.style.display = "block";
    modal.removeAttribute("aria-hidden");
    document.body.classList.add("modal-open");
}

function hideModalById(modalId) {
    const modal = document.getElementById(modalId);
    if (!modal) return;
    if (window.bootstrap?.Modal) {
        bootstrap.Modal.getOrCreateInstance(modal).hide();
        return;
    }
    modal.classList.remove("show");
    modal.style.display = "none";
    modal.setAttribute("aria-hidden", "true");
    document.body.classList.remove("modal-open");
}

function showTabButton(button) {
    if (!button) return;
    if (window.bootstrap?.Tab) {
        bootstrap.Tab.getOrCreateInstance(button).show();
        return;
    }
    button.click();
}

function sortImportantNodes(nodes) {
    return [...nodes].sort((first, second) => {
        const firstKey = `${nodeStartDate(first)} ${first.startTime || "00:00"}`;
        const secondKey = `${nodeStartDate(second)} ${second.startTime || "00:00"}`;
        return firstKey.localeCompare(secondKey);
    });
}

function importanceClass(importance) {
    if (importance === "紧急") return "node-urgent";
    if (importance === "重要") return "node-important";
    return "node-normal";
}

function typeClass(type) {
    if (type === "考试") return "node-type-exam";
    if (type === "截止") return "node-type-deadline";
    if (type === "比赛") return "node-type-contest";
    if (type === "汇报") return "node-type-report";
    return "node-type-other";
}

function renderImportantNodes() {
    const title = document.getElementById("importantNodeListTitle");
    const list = document.getElementById("importantNodeList");
    if (!title || !list) return;
    title.textContent = `${appState.currentImportantNodeYear} 年重要节点`;
    const nodes = sortImportantNodes(appState.importantNodes);
    if (nodes.length === 0) {
        list.innerHTML = `
            <div class="important-node-empty">
                <h3>今年还没有重要节点。</h3>
                <p>可以点击右上角“新增节点”，记录考试、截止和汇报时间。</p>
                <button class="btn btn-primary btn-sm" type="button" onclick="openImportantNodeModal()">+ 新增节点</button>
            </div>
        `;
        return;
    }

    const grouped = groupImportantNodesByMonth(nodes);
    list.innerHTML = Object.keys(grouped).map(month => `
        <div class="important-node-month">
            <h3>${Number(month)}月</h3>
            <div class="important-node-month-list">
                ${grouped[month].map(renderImportantNodeCard).join("")}
            </div>
        </div>
    `).join("");
}

function openImportantNodeModal(node = null) {
    const form = document.getElementById("importantNodeForm");
    const modalTitle = document.getElementById("importantNodeModalTitle");
    const batchTab = document.getElementById("batchImportantNodeTab");
    const singleTab = document.getElementById("singleImportantNodeTab");
    if (!form || !modalTitle) return;
    resetForm("importantNodeForm");
    document.getElementById("importantNodeBatchText").value = "";
    if (node) {
        modalTitle.textContent = "编辑重要节点";
        batchTab.classList.add("d-none");
        fillForm("importantNodeForm", {
            ...node,
            type: importantNodeType(node),
            startDate: nodeStartDate(node),
            endDate: nodeEndDate(node),
            description: nodeDescription(node),
            startTime: node.startTime ? String(node.startTime).slice(0, 5) : "",
            endTime: node.endTime ? String(node.endTime).slice(0, 5) : ""
        });
    } else {
        modalTitle.textContent = "新增重要节点";
        batchTab.classList.remove("d-none");
        setFormControlValue(form, "type", "考试");
        const defaultDate = `${appState.currentImportantNodeYear}-${String(new Date().getMonth() + 1).padStart(2, "0")}-${String(new Date().getDate()).padStart(2, "0")}`;
        setFormControlValue(form, "startDate", defaultDate);
        setFormControlValue(form, "endDate", defaultDate);
    }
    showTabButton(singleTab);
    showModalById("importantNodeModal");
}

function editImportantNodeById(id) {
    const node = appState.importantNodes.find(item => String(item.id) === String(id));
    if (node) editImportantNode(node);
}

function editImportantNode(node) {
    openImportantNodeModal(node);
}

async function deleteImportantNode(id) {
    if (!confirm(TEXT.confirmImportantNode)) return;
    await api(`../api/important-nodes/${id}`, {method: "DELETE"});
    await loadImportantNodes();
}

function validateImportantNodeLine(parts, rowNumber) {
    if (parts.length !== 7) {
        return `第 ${rowNumber} 行导入失败：字段数量不正确，请使用 7 个字段。`;
    }
    const title = parts[0];
    const type = parts[1];
    const startDate = parts[2];
    const startTime = parts[3];
    const endDate = parts[4];
    const endTime = parts[5];
    if (!title) return `第 ${rowNumber} 行导入失败：标题不能为空。`;
    if (!IMPORTANT_NODE_TYPES.includes(type)) return `第 ${rowNumber} 行导入失败：类型不在固定范围内。`;
    if (!isValidDateText(startDate) || !isValidDateText(endDate)) {
        return `第 ${rowNumber} 行导入失败：开始日期和结束日期格式必须为 YYYY-MM-DD。`;
    }
    if (endDate < startDate) return "结束日期不能早于开始日期";
    if ((startTime && !/^([01]\d|2[0-3]):[0-5]\d$/.test(startTime))
        || (endTime && !/^([01]\d|2[0-3]):[0-5]\d$/.test(endTime))) {
        return `第 ${rowNumber} 行导入失败：时间格式不正确，请使用 HH:mm。`;
    }
    return "";
}

function parseImportantNodeBatch(text) {
    const lines = text.split("\n").map(line => line.trim()).filter(Boolean);
    const nodes = [];
    for (let index = 0; index < lines.length; index++) {
        const rowNumber = index + 1;
        const parts = lines[index].split("|").map(part => part.trim());
        const error = validateImportantNodeLine(parts, rowNumber);
        if (error) {
            return {error, nodes: []};
        }
        nodes.push({
            title: parts[0],
            type: parts[1],
            startDate: parts[2],
            startTime: parts[3] || null,
            endDate: parts[4],
            endTime: parts[5] || null,
            description: parts[6] || null
        });
    }
    if (nodes.length === 0) {
        return {error: "请先输入批量节点内容。", nodes: []};
    }
    return {error: "", nodes};
}

document.getElementById("importantKeywordRuleForm").addEventListener("submit", async event => {
    event.preventDefault();
    const data = buildKeywordRulePayload(event.target);
    const method = data.id ? "PUT" : "POST";
    const url = data.id ? `../api/important-node-keyword-rules/${data.id}` : "../api/important-node-keyword-rules";
    const result = await api(url, {method, body: JSON.stringify(data)});
    if (!result.success) return;
    resetKeywordRuleForm();
    await loadImportantKeywordRules();
    await loadImportantNodes();
});

document.getElementById("resetKeywordRuleBtn").addEventListener("click", resetKeywordRuleForm);

document.getElementById("importantNodeForm").addEventListener("submit", async event => {
    event.preventDefault();
    const data = formToObject(event.target);
    if (data.endDate < data.startDate) {
        alert("结束日期不能早于开始日期");
        return;
    }
    const method = data.id ? "PUT" : "POST";
    const url = data.id ? `../api/important-nodes/${data.id}` : "../api/important-nodes";
    const result = await api(url, {method, body: JSON.stringify(data)});
    if (!result.success) return;
    appState.currentImportantNodeYear = Number(String(data.startDate).slice(0, 4));
    hideModalById("importantNodeModal");
    resetForm("importantNodeForm");
    await loadImportantNodes();
});

document.getElementById("importantNodeBatchForm").addEventListener("submit", async event => {
    event.preventDefault();
    const parsed = parseImportantNodeBatch(document.getElementById("importantNodeBatchText").value);
    if (parsed.error) {
        alert(parsed.error);
        return;
    }
    const result = await api("../api/important-nodes/batch", {method: "POST", body: JSON.stringify(parsed.nodes)});
    if (!result.success) return;
    const firstYear = Number(String(parsed.nodes[0].startDate).slice(0, 4));
    if (firstYear) appState.currentImportantNodeYear = firstYear;
    hideModalById("importantNodeModal");
    document.getElementById("importantNodeBatchText").value = "";
    await loadImportantNodes();
});

document.getElementById("importantNodeYearSelect").addEventListener("change", event => {
    appState.currentImportantNodeYear = Number(event.target.value);
    loadImportantNodes();
});

document.getElementById("openImportantNodeModalBtn").addEventListener("click", () => openImportantNodeModal());

window.loadImportantNodes = loadImportantNodes;
window.loadImportantKeywordRules = loadImportantKeywordRules;
window.editImportantKeywordRuleById = editImportantKeywordRuleById;
window.deleteImportantKeywordRule = deleteImportantKeywordRule;
window.nodeStartDate = nodeStartDate;
window.nodeEndDate = nodeEndDate;
window.nodeDescription = nodeDescription;
window.parseLocalDate = parseLocalDate;
window.formatNodeDateRange = formatNodeDateRange;
window.formatNodeTime = formatNodeTime;
window.importantNodeType = importantNodeType;
window.sortImportantNodes = sortImportantNodes;
window.importanceClass = importanceClass;
window.typeClass = typeClass;
window.openImportantNodeModal = openImportantNodeModal;
window.editImportantNodeById = editImportantNodeById;
window.deleteImportantNode = deleteImportantNode;
