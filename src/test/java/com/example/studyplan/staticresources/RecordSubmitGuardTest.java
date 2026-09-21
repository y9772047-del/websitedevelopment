package com.example.studyplan.staticresources;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class RecordSubmitGuardTest {

    @Test
    void recordFormSubmitIsGuardedAgainstDuplicatePosts() throws Exception {
        String script = Files.readString(Path.of("src/main/resources/static/js/records.js"));

        assertThat(script).contains("let recordSubmitInFlight = false;");
        assertThat(script).contains("if (recordSubmitInFlight) return;");
        assertThat(script).contains("recordSubmitInFlight = true;");
        assertThat(script).contains("recordSubmitInFlight = false;");
        assertThat(script).contains("setRecordSubmitDisabled(true)");
        assertThat(script).contains("setRecordSubmitDisabled(false)");
    }

    @Test
    void recordAiTipShowsGenerationSource() throws Exception {
        String script = Files.readString(Path.of("src/main/resources/static/js/records.js"));

        assertThat(script).contains("result.data.generationSourceLabel");
        assertThat(script).contains("result.data.generationSourceDetail");
        assertThat(script).contains("来源：${sourceLabel}");
    }

    @Test
    void splitScriptsAreLoadedAfterCoreAppInOrder() throws Exception {
        String page = Files.readString(Path.of("src/main/resources/static/pages/app.html"));

        String common = "../js/common.js?v=20260608-calendar-range1";
        String app = "../js/app.js?v=20260706-repeat-reminder-weekly";
        String tasks = "../js/tasks.js?v=20260706-repeat-reminder";
        String records = "../js/records.js?v=20260619-split-core";
        String calendar = "../js/calendar.js?v=20260622-detail-label";

        assertThat(page).contains(common, app, tasks, records, calendar);
        assertThat(page.indexOf(common)).isLessThan(page.indexOf(app));
        assertThat(page.indexOf(app)).isLessThan(page.indexOf(tasks));
        assertThat(page.indexOf(tasks)).isLessThan(page.indexOf(records));
        assertThat(page.indexOf(records)).isLessThan(page.indexOf(calendar));
    }

    @Test
    void calendarShowsStudyRecordsByStudyDateOnly() throws Exception {
        String page = Files.readString(Path.of("src/main/resources/static/pages/app.html"));
        String appScript = Files.readString(Path.of("src/main/resources/static/js/app.js"));
        String calendarScript = Files.readString(Path.of("src/main/resources/static/js/calendar.js"));

        assertThat(page).contains("calendarSelectedTitle", "calendarDayTasks");
        assertThat(appScript).contains("noCalendarRecord: \"当天暂无学习记录\"");
        assertThat(calendarScript).contains("function buildCalendarRecordsByDate");
        assertThat(calendarScript).contains("if (!record.studyDate) return;");
        assertThat(calendarScript).contains("recordsByDate[record.studyDate].push(record)");
        assertThat(calendarScript).contains("renderCalendar(year, month, appState.records)");
        assertThat(calendarScript).doesNotContain("function buildCalendarTasksByDate");
        assertThat(calendarScript).doesNotContain("calendarTaskStartDate");
    }

    @Test
    void recordTaskSelectOrdersTasksByLatestTime() throws Exception {
        String page = Files.readString(Path.of("src/main/resources/static/pages/app.html"));
        String script = Files.readString(Path.of("src/main/resources/static/js/tasks.js"));

        assertThat(page).contains("id=\"recordTaskSelect\"");
        assertThat(script).contains("function sortTasksByLatestTime");
        assertThat(script).contains("task.deadline || task.createTime || \"\"");
        assertThat(script).contains("sortTasksByLatestTime(appState.tasks)");
    }

    @Test
    void recordTaskDisplayIncludesPlanName() throws Exception {
        String taskScript = Files.readString(Path.of("src/main/resources/static/js/tasks.js"));
        String recordScript = Files.readString(Path.of("src/main/resources/static/js/records.js"));

        assertThat(taskScript).contains("function taskDisplayName(task)");
        assertThat(taskScript).contains("`${planName(task.planId)} / ${task.taskName}`");
        assertThat(taskScript).contains("<option value=\"${task.id}\">${taskDisplayName(task)}</option>");
        assertThat(recordScript).contains("<td>${taskDisplayNameById(record.taskId)}</td>");
    }

    @Test
    void taskSaveStopsWhenApiFails() throws Exception {
        String script = Files.readString(Path.of("src/main/resources/static/js/tasks.js"));

        assertThat(script).contains("const result = await api(\"../api/tasks\"");
        assertThat(script).contains("if (!result.success) return;");
        assertThat(script).contains("const editResult = await api(`../api/tasks/${data.id}`");
        assertThat(script).contains("if (!editResult.success) return;");
    }

    @Test
    void calendarDayDetailsSeparatesNodesAndRecordsConditionally() throws Exception {
        String calendar = Files.readString(Path.of("src/main/resources/static/js/calendar.js"));

        assertThat(calendar).contains(
                "const nodeSection = nodeHtml ? renderCalendarDetailSection(\"重要节点\", nodeHtml) : \"\";",
                "const recordSection = recordHtml ? renderCalendarDetailSection(\"学习记录\", recordHtml) : \"\";",
                "list.innerHTML = nodeSection + recordSection ||");
        assertThat(calendar.indexOf("const nodeSection ="))
                .isLessThan(calendar.indexOf("const recordSection ="));
        assertThat(calendar).contains("<span class=\"soft-label mb-2\">${title}</span>");
        assertThat(calendar).doesNotContain("<h3 class=\"mb-2\">${title}</h3>");
    }
    @Test
    void splitFunctionsUsedByInlineHandlersAreExplicitlyGlobal() throws Exception {
        String tasks = Files.readString(Path.of("src/main/resources/static/js/tasks.js"));
        String records = Files.readString(Path.of("src/main/resources/static/js/records.js"));
        String calendar = Files.readString(Path.of("src/main/resources/static/js/calendar.js"));

        assertThat(tasks).contains("window.editTask = editTask;");
        assertThat(tasks).contains("window.deleteTask = deleteTask;");
        assertThat(records).contains("window.resetRecordForm = resetRecordForm;");
        assertThat(records).contains("window.editRecord = editRecord;");
        assertThat(records).contains("window.deleteRecord = deleteRecord;");
        assertThat(calendar).contains("window.selectCalendarDate = selectCalendarDate;");
        assertThat(calendar).contains("window.changeCalendarMonth = changeCalendarMonth;");
    }
}