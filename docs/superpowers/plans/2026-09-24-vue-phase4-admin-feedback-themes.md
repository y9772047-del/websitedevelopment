# Vue Phase 4 Administration, Feedback, and Themes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** Complete Experiment 2 by moving administration, student feedback, and theme selection into the independent Vue application and removing the remaining legacy-page entry.

**Architecture:** Keep the Spring Boot controllers, database schema, session authentication, and JSON response contracts unchanged. Add a small tested frontend domain module for query/payload/theme rules, focused Vue views for student features, and a role-aware administrator shell that calls the existing `/api/admin` endpoints through the shared Axios client.

**Tech Stack:** Vue 3 single-file components, Axios, Vite, Node test runner, existing Bootstrap/base/app/admin CSS, Spring Boot REST backend.

**Spec:** `docs/experiment-2/implementation-plan.md`

## Global Constraints

- Do not change database schema, API paths, response shapes, login/auth rules, or ownership checks.
- Reuse existing CSS classes and the five existing theme keys.
- Preserve `theme_<user id>` localStorage compatibility.
- Do not load legacy JavaScript or Bootstrap JavaScript in the Vue entry.
- Confirm destructive administrator operations before sending requests.
- Escape user text through Vue interpolation.
- Do not stage unrelated working-tree changes.

---

### Task 1: Tested frontend domain rules

**Files:**
- Create: `frontend/src/portal.test.js`
- Create: `frontend/src/portal.js`

**Interfaces:**
- Produces: `buildQuery(filters)`, `prepareFeedback(input)`, `prepareFeedbackDecision(input)`, `nextUserStatus(status)`, `themeStorageKey(user)`, `resolveTheme(key)`, and `THEMES`.

- [x] **Step 1: Write failing tests** for blank-filter removal, feedback trimming/validation, controlled administrator statuses, numeric user status toggling, and theme fallback/storage compatibility.
- [x] **Step 2: Run** `npm test -- --test-name-pattern="portal|feedback|theme|administrator"` and confirm failure because the functions do not exist.
- [x] **Step 3: Implement minimal pure functions** with literal allowed values matching the current backend.
- [x] **Step 4: Run** `npm test` and confirm all frontend unit tests pass.

### Task 2: Student feedback and shared themes

**Files:**
- Create: `frontend/src/views/FeedbackView.vue`
- Create: `frontend/src/views/ThemeView.vue`
- Modify: `frontend/src/App.vue`

**Interfaces:**
- Consumes: shared Axios client; `prepareFeedback`, `THEMES`, `themeStorageKey`, and `resolveTheme`.
- Produces: student navigation for feedback and themes; emits `theme-change`, `busy-change`, and `session-expired`.

- [x] **Step 1: Add feedback submission/list rendering** against `POST /feedback` and `GET /feedback/my`, with validation errors and busy-state navigation locking.
- [x] **Step 2: Add theme preview/use behavior** with the existing five themes and per-user localStorage key.
- [x] **Step 3: Add both views to student navigation** and remove the legacy-page link.
- [x] **Step 4: Run** `npm test` and `npm run build`.

### Task 3: Role-aware Vue administrator portal

**Files:**
- Create: `frontend/src/views/AdminPortal.vue`
- Create: `frontend/src/views/AdminDashboardView.vue`
- Create: `frontend/src/views/AdminUsersView.vue`
- Create: `frontend/src/views/AdminTasksView.vue`
- Create: `frontend/src/views/AdminFeedbacksView.vue`
- Modify: `frontend/src/App.vue`
- Modify: `frontend/src/main.js`

**Interfaces:**
- Consumes: `/admin/dashboard/stats`, `/admin/users`, `/admin/users/{id}`, `/admin/users/{id}/status`, `/admin/tasks`, `/admin/tasks/{id}`, `/admin/feedbacks`, and `/admin/feedbacks/{id}`.
- Produces: role-selected admin shell, detail dialogs, confirmed user status changes/task deletion, and feedback processing.

- [x] **Step 1: Build dashboard and filtered list views** using existing endpoint fields and CSS classes.
- [x] **Step 2: Build Vue-controlled detail dialogs and mutation flows** with confirmation and post-success refresh.
- [x] **Step 3: Route authenticated administrators into `AdminPortal`** rather than redirecting to the legacy HTML page.
- [x] **Step 4: Import existing `admin.css`** and verify mobile layout remains usable.
- [x] **Step 5: Run** `npm test` and `npm run build`.

### Task 4: Documentation and full verification

**Files:**
- Modify: `docs/experiment-2/README.md`
- Modify: `docs/experiment-2/verification.md`
- Modify: `docs/experiment-2/openapi.json`
- Modify: `docs/frontend-js-dependencies.md`
- Modify: `CODE_MAP.md` only through a staged index update that preserves unrelated user edits.

- [x] **Step 1: Add feedback and administrator paths/schemas** to OpenAPI without changing runtime contracts.
- [x] **Step 2: Update Experiment 2 and dependency documentation** to describe the complete Vue scope and role-aware entry.
- [x] **Step 3: Run frontend unit tests and production build.**
- [x] **Step 4: Run Maven tests/package and an OpenAPI reference integrity check.**
- [x] **Step 5: Exercise student/admin desktop and mobile flows in a real browser**, using read-only actions or intercepted mutations where existing data could be changed.
- [x] **Step 6: Inspect the final diff and commit only Phase 4 files.**
