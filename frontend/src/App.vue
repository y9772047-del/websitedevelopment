<script setup>
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue';
import { createApi } from './api.js';
import AuthView from './views/AuthView.vue';
import PlansView from './views/PlansView.vue';
import TasksView from './views/TasksView.vue';
import RecordsView from './views/RecordsView.vue';

const api = createApi(import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api');
const backendRoot = new URL(api.defaults.baseURL.replace(/\/api\/?$/, '').replace(/\/$/, '') + '/', window.location.href);
const legacyUrl = new URL('pages/app.html', backendRoot).href;
const adminUrl = new URL('pages/admin.html', backendRoot).href;
const user = ref(null), ready = ref(false), logoutBusy = ref(false), message = ref(''), navOpen = ref(false), currentView = ref('plans');
const plansView = ref(null), tasksView = ref(null), recordsView = ref(null);
const visited = reactive({ plans: true, tasks: false, records: false });
const sectionBusy = reactive({ plans: false, tasks: false, records: false });
const anyBusy = computed(() => logoutBusy.value || Object.values(sectionBusy).some(Boolean));
const themes = ['theme-pink', 'theme-blue', 'theme-green', 'theme-cream', 'theme-dark'];
watch(user, value => {
  let theme = value ? 'theme-pink' : 'theme-blue';
  if (value) { try { const saved = localStorage.getItem('theme_' + (value.id ?? value.username)); if (themes.includes(saved)) theme = saved; } catch {} }
  document.body.classList.add('prototype-body'); document.body.classList.remove(...themes); document.body.classList.add(theme);
  document.title = value ? '大学生学习计划管理系统' : '登录 - 大学生学习计划管理系统';
}, { immediate: true });
function authenticated(value) { user.value = value; message.value = ''; navOpen.value = false; currentView.value = 'plans'; Object.assign(visited, { plans: true, tasks: false, records: false }); if (value?.role === 'admin') window.location.assign(adminUrl); }
function sessionExpired() { user.value = null; message.value = '登录已失效，请重新登录'; }
async function showView(name) { if (anyBusy.value) return; visited[name] = true; currentView.value = name; navOpen.value = false; await nextTick(); if (name === 'plans') plansView.value?.showList(); if (name === 'tasks') tasksView.value?.refresh(); if (name === 'records') recordsView.value?.refresh(); }
async function logout() {
  if (anyBusy.value) return;
  logoutBusy.value = true; message.value = '';
  try { await api.post('/auth/logout'); user.value = null; }
  catch (error) { if (error.status === 401) sessionExpired(); else message.value = error.message; }
  finally { logoutBusy.value = false; }
}
onMounted(async () => { try { authenticated(await api.get('/auth/current')); } catch (error) { message.value = error.message; } finally { ready.value = true; } });
</script>
<template>
  <main v-if="!ready" class="container app-shell"><p class="muted-text" role="status">正在恢复登录状态…</p></main>
  <AuthView v-else-if="!user" :api="api" :initial-message="message" @authenticated="authenticated" @clear-message="message = ''" />
  <template v-else>
    <nav class="navbar navbar-expand-lg app-navbar sticky-top"><div class="container-fluid px-4">
      <a class="navbar-brand" href="#" @click.prevent="showView('plans')">大学生学习计划管理系统</a>
      <button class="navbar-toggler" type="button" :aria-expanded="navOpen" aria-controls="mainNavbar" aria-label="展开导航" @click="navOpen = !navOpen"><span class="navbar-toggler-icon"></span></button>
      <div id="mainNavbar" class="collapse navbar-collapse" :class="{ show: navOpen }"><div class="navbar-nav ms-auto nav-actions">
        <button class="nav-link" :class="{ active: currentView === 'plans' }" :disabled="anyBusy" @click="showView('plans')">学习计划</button>
        <button class="nav-link" :class="{ active: currentView === 'tasks' }" :disabled="anyBusy" @click="showView('tasks')">学习任务</button>
        <button class="nav-link" :class="{ active: currentView === 'records' }" :disabled="anyBusy" @click="showView('records')">学习记录</button>
        <a class="nav-link" :href="legacyUrl" :aria-disabled="anyBusy" @click="anyBusy && $event.preventDefault()">其他功能（原系统）</a>
        <button class="nav-link" :disabled="anyBusy" @click="logout">{{ logoutBusy ? '退出中…' : '退出' }}</button>
      </div></div>
    </div></nav>
    <div v-if="message" class="container-fluid app-shell pb-0"><p class="alert alert-danger" role="alert">{{ message }}</p></div>
    <PlansView ref="plansView" v-show="currentView === 'plans'" :key="'plans-' + user.id" :api="api" @session-expired="sessionExpired" @busy-change="sectionBusy.plans = $event" />
    <TasksView v-if="visited.tasks" ref="tasksView" v-show="currentView === 'tasks'" :key="'tasks-' + user.id" :api="api" @session-expired="sessionExpired" @busy-change="sectionBusy.tasks = $event" />
    <RecordsView v-if="visited.records" ref="recordsView" v-show="currentView === 'records'" :key="'records-' + user.id" :api="api" @session-expired="sessionExpired" @busy-change="sectionBusy.records = $event" />
  </template>
</template>