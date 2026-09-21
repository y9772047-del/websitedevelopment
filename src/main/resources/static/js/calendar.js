async function loadCalendar() {
    const current = appState.calendarDate;
    const year = current.getFullYear();
    const month = current.getMonth() + 1;
    if (appState.currentImportantNodeYear !== year) {
        appState.currentImportantNodeYear = year;
        await loadImportantNodes();
        return;
    }
    renderCalendar(year, month, appState.records);
}

function calendarRecordType(record) {
    if (record.completionStatus === TEXT.done) return "done";
    if (record.completionStatus === TEXT.notDone) return "overdue";
    return "doing";
}

function buildCalendarRecordsByDate(records, monthStart, monthEnd) {
    const recordsByDate = {};
    records.forEach(record => {
        if (!record.studyDate) return;
        if (!isValidDateText(record.studyDate)) return;
        const studyDate = new Date(record.studyDate);
        if (studyDate < monthStart || studyDate > monthEnd) return;
        if (!recordsByDate[record.studyDate]) recordsByDate[record.studyDate] = [];
        recordsByDate[record.studyDate].push(record);
    });
    return recordsByDate;
}

function buildCalendarNodesByDate(nodes, monthStart, monthEnd) {
    const nodesByDate = {};
    nodes.forEach(node => {
        const startText = nodeStartDate(node);
        const endText = nodeEndDate(node);
        if (!isValidDateText(startText) || !isValidDateText(endText)) return;
        const eventStart = parseLocalDate(startText);
        const eventEnd = parseLocalDate(endText);
        if (!eventStart || !eventEnd || eventEnd < eventStart) return;
        const firstVisible = eventStart < monthStart ? new Date(monthStart) : new Date(eventStart);
        const lastVisible = eventEnd > monthEnd ? new Date(monthEnd) : new Date(eventEnd);
        for (let current = firstVisible; current <= lastVisible; current.setDate(current.getDate() + 1)) {
            const dateText = formatLocalDate(current);
            if (!nodesByDate[dateText]) nodesByDate[dateText] = [];
            nodesByDate[dateText].push(node);
        }
    });
    Object.keys(nodesByDate).forEach(date => {
        nodesByDate[date] = sortImportantNodes(nodesByDate[date]);
    });
    return nodesByDate;
}

function renderCalendar(year, month, records, nodes = appState.importantNodes) {
    const grid = document.getElementById("calendarGrid");
    const title = document.getElementById("calendarMonthTitle");
    if (!grid || !title) return;

    title.textContent = `${year} 年 ${month} 月学习日历`;
    const firstDate = new Date(year, month - 1, 1);
    const daysInMonth = new Date(year, month, 0).getDate();
    const lastDate = new Date(year, month, 0);
    const leadingBlankCount = (firstDate.getDay() + 6) % 7;
    const recordsByDate = buildCalendarRecordsByDate(records, firstDate, lastDate);
    const nodesByDate = buildCalendarNodesByDate(nodes, firstDate, lastDate);

    const todayText = formatLocalDate(new Date());
    const selectedDate = appState.selectedCalendarDate
        || (year === new Date().getFullYear() && month === new Date().getMonth() + 1 ? todayText : formatLocalDate(firstDate));
    appState.selectedCalendarDate = selectedDate.startsWith(`${year}-${String(month).padStart(2, "0")}`)
        ? selectedDate
        : formatLocalDate(firstDate);

    const cells = [];
    for (let i = 0; i < leadingBlankCount; i++) {
        cells.push('<div class="calendar-cell calendar-cell-empty"></div>');
    }
    for (let day = 1; day <= daysInMonth; day++) {
        const dateText = `${year}-${String(month).padStart(2, "0")}-${String(day).padStart(2, "0")}`;
        const dayRecords = recordsByDate[dateText] || [];
        const dayNodes = nodesByDate[dateText] || [];
        const previewRecords = dayRecords.slice(0, 2).map(record => `
            <div class="calendar-task-pill calendar-task-${calendarRecordType(record)}" title="${taskName(record.taskId)} · ${record.duration || 0} ${TEXT.minutes}">
                ${taskName(record.taskId)} · ${record.duration || 0}${TEXT.minutes}
            </div>
        `).join("");
        const previewNodes = dayNodes.slice(0, 2).map(node => `
            <div class="calendar-node-pill ${importanceClass(node.importance)}" title="${importantNodeType(node)} · ${node.title} · ${formatNodeTime(node)}">
                ${importantNodeType(node)} · ${node.title}
            </div>
        `).join("");
        const hiddenCount = Math.max(dayRecords.length - 2, 0) + Math.max(dayNodes.length - 2, 0);
        const moreCount = hiddenCount > 0 ? `<span class="calendar-more">+${hiddenCount} 条</span>` : "";
        cells.push(`
            <button class="calendar-cell ${dateText === todayText ? "is-today" : ""} ${dateText === appState.selectedCalendarDate ? "is-selected" : ""}" type="button" onclick="selectCalendarDate('${dateText}')">
                <span class="calendar-day-number">${day}</span>
                <div class="calendar-task-preview">${previewRecords}${previewNodes}${moreCount}</div>
            </button>
        `);
    }
    grid.innerHTML = cells.join("");
    renderCalendarDayDetails(appState.selectedCalendarDate, recordsByDate[appState.selectedCalendarDate] || [], nodesByDate[appState.selectedCalendarDate] || []);
}

function selectCalendarDate(dateText) {
    appState.selectedCalendarDate = dateText;
    loadCalendar();
}

function renderCalendarDetailSection(title, content) {
    return `
        <section class="calendar-detail-section">
            <span class="soft-label mb-2">${title}</span>
            <div class="stack-list">${content}</div>
        </section>
    `;
}
function renderCalendarDayDetails(dateText, records, nodes) {
    const title = document.getElementById("calendarSelectedTitle");
    const list = document.getElementById("calendarDayTasks");
    if (!title || !list) return;
    title.textContent = `${dateText} 学习详情`;
    const recordHtml = records.map(record => `
        <div class="list-row calendar-day-task">
            <div>
                <h3>${taskName(record.taskId)}</h3>
                <p>${record.duration || 0} ${TEXT.minutes} · ${record.completionStatus || ""}</p>
                ${record.content ? `<p>${compactText(record.content, 52)}</p>` : ""}
                ${record.problems ? `<p>${TEXT.problemReflection}${compactText(record.problems, 52)}</p>` : ""}
            </div>
            <div class="calendar-task-tags">
                <span class="badge ${badgeClass(record.completionStatus)}">${record.completionStatus || ""}</span>
            </div>
        </div>
    `).join("");
    const nodeHtml = nodes.map(node => `
        <div class="list-row calendar-day-task calendar-day-node">
            <div>
                <h3>${node.title}</h3>
                <p>${formatNodeDateRange(node)} · ${formatNodeTime(node)} · ${importantNodeType(node)} · ${node.importance || "重要"}</p>
                ${nodeDescription(node) ? `<p>${compactText(nodeDescription(node), 52)}</p>` : ""}
            </div>
            <div class="calendar-task-tags">
                <span class="badge important-node-type ${typeClass(importantNodeType(node))}">${importantNodeType(node)}</span>
                <span class="badge important-node-importance ${importanceClass(node.importance)}">${node.importance || "重要"}</span>
            </div>
        </div>
    `).join("");
    const nodeSection = nodeHtml ? renderCalendarDetailSection("重要节点", nodeHtml) : "";
    const recordSection = recordHtml ? renderCalendarDetailSection("学习记录", recordHtml) : "";
    list.innerHTML = nodeSection + recordSection || `<div class="list-row"><p>${TEXT.noCalendarDetail}</p></div>`;
}

function changeCalendarMonth(offset) {
    const current = appState.calendarDate;
    appState.calendarDate = new Date(current.getFullYear(), current.getMonth() + offset, 1);
    appState.selectedCalendarDate = "";
    loadCalendar();
}

document.getElementById("prevMonthBtn").addEventListener("click", () => changeCalendarMonth(-1));
document.getElementById("nextMonthBtn").addEventListener("click", () => changeCalendarMonth(1));
document.getElementById("todayMonthBtn").addEventListener("click", () => {
    appState.calendarDate = new Date();
    appState.selectedCalendarDate = formatLocalDate(new Date());
    loadCalendar();
});

window.loadCalendar = loadCalendar;
window.calendarRecordType = calendarRecordType;
window.buildCalendarRecordsByDate = buildCalendarRecordsByDate;
window.buildCalendarNodesByDate = buildCalendarNodesByDate;
window.renderCalendar = renderCalendar;
window.selectCalendarDate = selectCalendarDate;
window.renderCalendarDayDetails = renderCalendarDayDetails;
window.changeCalendarMonth = changeCalendarMonth;
