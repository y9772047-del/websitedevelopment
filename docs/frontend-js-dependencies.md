# Frontend JS Dependencies

This file records the current static frontend JavaScript dependency map. Update it whenever frontend JS files are split, moved, added, removed, or when cross-file globals/functions change.

`CODE_MAP.md` is still the feature navigation map. This file is the script dependency map.

## Script Load Order

Current entry page:

- `src/main/resources/static/pages/app.html`

Current script order:

1. Bootstrap bundle CDN
2. `../js/common.js?v=20260608-calendar-range1`
3. `../js/app.js?v=20260707-split-dashboard`
4. `../js/tasks.js?v=20260706-repeat-reminder`
5. `../js/records.js?v=20260619-split-core`
6. `../js/important-nodes.js?v=20260707-split-important-nodes`
7. `../js/calendar.js?v=20260622-detail-label`
8. `../js/dashboard.js?v=20260707-split-dashboard`

Do not change this order unless the dependency map below is updated and the affected page is verified.

## File Responsibilities

- `common.js`: shared theme helpers and theme initialization.
- `app.js`: shared app state, shared request/util functions, plans, feedback, navigation, and startup orchestration.
- `tasks.js`: task helpers, task category loading, task CRUD UI, task batch import, task option population.
- `records.js`: record form/list logic, record CRUD UI, AI improvement request/display logic.
- `important-nodes.js`: important node and keyword-rule page logic, modal handling, batch import parsing, and node formatting helpers.
- `calendar.js`: calendar loading, month navigation, date selection, day detail rendering.
- `dashboard.js`: dashboard, statistics, heatmap, plan-investment donut, mini charts, and dashboard task reminders.

## Shared Globals Provided By app.js

These are used by other frontend JS files and must stay available before those files execute:

- `TEXT`
- `appState`
- `api(url, options)`
- `formToObject(form)`
- `resetForm(formId)`
- `badgeClass(status)`
- `priorityClass(priority)`
- `planName(planId)`
- `findPlanByName(planNameValue)`
- `isValidDateText(dateText)`
- `formatLocalDate(date)`
- `formatMonthDay(dateText)`
- `formatNodeTime(node)`
- `fillForm(formId, data)`
- `displayPlanName(plan)`
- `compactText(value, maxLength)`
- `showPage(pageId)`
- `refreshAll()`

## Cross-File Dependencies

### tasks.js depends on app.js and dashboard.js

- From `app.js`: `appState`, `TEXT`, `api`, `formatLocalDate`, `planName`, `fillForm`, `badgeClass`, `priorityClass`, `showPage`, `refreshAll`
- From `dashboard.js`: optional runtime calls to `taskReminderInfo` and `renderTaskReminders` when those functions are loaded.

### records.js depends on app.js and tasks.js

- From `app.js`: `api`, `appState`, `showPage`, `refreshAll`, `fillForm`, `resetForm`, `displayPlanName`, `compactText`
- From `tasks.js`: `taskDisplayNameById`, `taskById`

### important-nodes.js depends on app.js

- State/constants: `appState`, `TEXT`
- API/utilities: `api`, `formToObject`, `resetForm`, `fillForm`, `formatMonthDay`, `isValidDateText`, `formatNodeTime`
- Runtime callback dependency: `loadCalendar` may be called after calendar.js has loaded when the calendar page is visible.

### calendar.js depends on app.js, tasks.js, and important-nodes.js

- From `app.js`: `appState`, `TEXT`, `formatLocalDate`, `formatNodeTime`, `compactText`
- From `tasks.js`: `taskName`
- From `important-nodes.js`: `loadImportantNodes`, `nodeStartDate`, `nodeEndDate`, `nodeDescription`, `parseLocalDate`, `sortImportantNodes`, `importanceClass`, `typeClass`, `importantNodeType`, `formatNodeDateRange`

### dashboard.js depends on app.js, tasks.js, records data, and important-nodes.js

- From `app.js`: `TEXT`, `appState`, `api`, `formatLocalDate`, `isValidDateText`, `compactText`, `planName`, `displayPlanName`, `badgeClass`
- From `tasks.js`: `isTaskOverdue`, `taskById`, `taskName`
- From `important-nodes.js`: `parseLocalDate`
- Data loaded by `records.js`: reads `appState.records`

### app.js depends on later files at runtime

`app.js` calls these functions after the page `load` event or user navigation. The files that define them must be loaded before those runtime calls happen:

- From `tasks.js`: `loadTaskCategories`, `loadTasks`, `fillTaskOptions`, `loadPlanDetailTasks`, `isTaskOverdue`, `taskName`
- From `records.js`: `loadRecords`
- From `important-nodes.js`: `loadImportantNodes`, `loadImportantKeywordRules`, `parseLocalDate`
- From `calendar.js`: `loadCalendar`
- From `dashboard.js`: `loadDashboard`, `loadStatistics`

## window Exports

Current explicit `window.*` exports:

- `tasks.js`: `isTaskOverdue`, `taskName`, `taskDisplayName`, `taskDisplayNameById`, `taskById`, `sortTasksByLatestTime`, `loadTaskCategories`, `loadPlanDetailTasks`, `loadTasks`, `fillTaskOptions`, `editTask`, `deleteTask`
- `records.js`: `resetRecordForm`, `setRecordSubmitDisabled`, `loadRecords`, `editRecord`, `deleteRecord`, `renderAiSuggestedActions`, `buildRecordAiRequest`
- `important-nodes.js`: `loadImportantNodes`, `loadImportantKeywordRules`, `editImportantKeywordRuleById`, `deleteImportantKeywordRule`, `nodeStartDate`, `nodeEndDate`, `nodeDescription`, `parseLocalDate`, `formatNodeDateRange`, `formatNodeTime`, `importantNodeType`, `sortImportantNodes`, `importanceClass`, `typeClass`, `openImportantNodeModal`, `editImportantNodeById`, `deleteImportantNode`
- `calendar.js`: `loadCalendar`, `calendarRecordType`, `buildCalendarRecordsByDate`, `buildCalendarNodesByDate`, `renderCalendar`, `selectCalendarDate`, `renderCalendarDayDetails`, `changeCalendarMonth`
- `dashboard.js`: `loadDashboard`, `loadStatistics`, `renderTaskReminders`, `taskReminderInfo`, `renderCategoryHeatmap`, `renderPlanInvestmentDonut`, `renderStatisticsCharts`

If a function is used by inline HTML handlers or generated HTML `onclick` attributes, keep it globally reachable or replace the inline handler with a verified event binding.

## Event Binding Notes

Top-level `document.getElementById(...).addEventListener(...)` calls run as soon as each script loads. If a handler references a function that is moved to another file, either:

- move the event binding with that function, or
- ensure the target function is already loaded and globally reachable before the binding script runs.

Important-node event bindings live in `important-nodes.js`:

- `importantKeywordRuleForm` submit handler
- `resetKeywordRuleBtn` click handler
- `importantNodeForm` submit handler
- `importantNodeBatchForm` submit handler
- `importantNodeYearSelect` change handler
- `openImportantNodeModalBtn` click handler

Dashboard event bindings are generated inside render functions in `dashboard.js`:

- Heatmap filter buttons are bound in `renderCategoryHeatmap`
- Plan-investment range buttons are bound in `renderPlanInvestmentDonut`

## Split Checklist

When splitting frontend JS:

1. Search references with `rg -n "functionName|globalName" src/main/resources/static/js src/main/resources/static/pages`.
2. Move functions together with their directly related event bindings when possible.
3. Update `app.html` script tags and query-string versions.
4. Keep loading order consistent with this dependency map.
5. Update this file and the affected `CODE_MAP.md` section.
6. Run `node --check` on every changed JS file when Node is available.
7. Verify the affected page in browser or with a focused runtime check when the change affects user interaction.

## Experiment 2 independent Vue entry

- `frontend/index.html` loads `src/main.js` as an ES module; Vite bundles and hashes assets.
- `main.js` imports Vue, `App.vue`, and existing `static/css/base.css`.
- `App.vue` imports `api.js`; `api.js` imports Axios. No window exports, inline handlers, or legacy JS dependencies.
- The original static page script order and version query strings remain unchanged.
