<script setup>
import { onMounted, ref, watch } from 'vue';
import { createApi } from './api.js';
import AuthView from './views/AuthView.vue';
import PlansView from './views/PlansView.vue';

const api = createApi(import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api');
const backendRoot = new URL(`${api.defaults.baseURL.replace(/\/api\/?$/, '').replace(/\/$/, '')}/`, window.location.href);
const legacyUrl = new URL('pages/app.html', backendRoot).href;
const adminUrl = new URL('pages/admin.html', backendRoot).href;
const user = ref(null);
const ready = ref(false);
const busy = ref(false);
const message = ref('');
const navOpen = ref(false);
const plansView = ref(null);
const plansBusy = ref(false);
const themes = ['theme-pink', 'theme-blue', 'theme-green', 'theme-cream', 'theme-dark'];
watch(user, value => {
  let theme = value ? 'theme-pink' : 'theme-blue';
  if (value) {
    try {
      const saved = localStorage.getItem(`theme_${value.id ?? value.username}`);
      if (themes.includes(saved)) theme = saved;
    } catch { /* Use the original default theme if storage is unavailable. */ }
  }
  document.body.classList.add('prototype-body');
  document.body.classList.remove(...themes);
  document.body.classList.add(theme);
  document.title = value ? '学习计划 - 大学生学习计划管理系统' : '登录 - 大学生学习计划管理系统';
}, { immediate: true });
function authenticated(value) {
  user.value = value; message.value = ''; navOpen.value = false;
  if (value?.role === 'admin') window.location.assign(adminUrl);
}
function sessionExpired() {
  user.value = null; message.value = '登录已失效，请重新登录';
}
async function logout() {
  if (busy.value || plansBusy.value) return;
  busy.value = true; message.value = '';
  try { await api.post('/auth/logout'); user.value = null; }
  catch (error) { if (error.status === 401) sessionExpired(); else message.value = error.message; }
  finally { busy.value = false; }
}
function showPlans() { if (!plansBusy.value && !busy.value) { plansView.value?.showList(); navOpen.value = false; } }
onMounted(async () => {
  try { authenticated(await api.get('/auth/current')); }
  catch (error) { message.value = error.message; }
  finally { ready.value = true; }
});
</script>

<template>
  <main v-if="!ready" class="container app-shell"><p class="muted-text" role="status">正在恢复登录状态…</p></main>
  <AuthView v-else-if="!user" :api="api" :initial-message="message" @authenticated="authenticated" @clear-message="message = ''" />
  <template v-else>
    <nav class="navbar navbar-expand-lg app-navbar sticky-top">
      <div class="container-fluid px-4">
        <a class="navbar-brand" href="#" @click.prevent="showPlans">大学生学习计划管理系统</a>
        <button class="navbar-toggler" type="button" :aria-expanded="navOpen" aria-controls="mainNavbar" aria-label="展开导航" @click="navOpen = !navOpen"><span class="navbar-toggler-icon"></span></button>
        <div id="mainNavbar" class="collapse navbar-collapse" :class="{ show: navOpen }">
          <div class="navbar-nav ms-auto nav-actions">
            <button class="nav-link active" :disabled="busy || plansBusy" @click="showPlans">学习计划</button>
            <a class="nav-link" :href="legacyUrl" :aria-disabled="busy || plansBusy" @click="(busy || plansBusy) && $event.preventDefault()">其他功能（原系统）</a>
            <button class="nav-link" :disabled="busy || plansBusy" @click="logout">{{ busy ? '退出中…' : '退出' }}</button>
          </div>
        </div>
      </div>
    </nav>
    <div v-if="message" class="container-fluid app-shell pb-0"><p class="alert alert-danger" role="alert">{{ message }}</p></div>
    <PlansView ref="plansView" :key="user.id" :api="api" @session-expired="sessionExpired" @busy-change="plansBusy = $event" />
  </template>
</template>
