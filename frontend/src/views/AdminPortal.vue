<script setup>
import { nextTick, ref } from 'vue';
import AdminDashboardView from './AdminDashboardView.vue';
import AdminUsersView from './AdminUsersView.vue';
import AdminTasksView from './AdminTasksView.vue';
import AdminFeedbacksView from './AdminFeedbacksView.vue';
import ThemeView from './ThemeView.vue';

const props = defineProps({
  api: { type: Function, required: true },
  user: { type: Object, required: true },
  logoutBusy: { type: Boolean, default: false },
  message: { type: String, default: '' }
});
const emit = defineEmits(['session-expired', 'logout', 'theme-change']);
const current = ref('dashboard');
const viewBusy = ref(false);
const dashboard = ref(null);
const users = ref(null);
const tasks = ref(null);
const feedbacks = ref(null);
const themes = ref(null);
const visited = ref(new Set(['dashboard']));
const items = [
  ['dashboard', '后台首页'], ['users', '用户管理'], ['tasks', '学习任务管理'],
  ['feedbacks', '用户反馈管理'], ['themes', '主题皮肤']
];

async function select(name) {
  if (viewBusy.value || props.logoutBusy) return;
  visited.value = new Set([...visited.value, name]);
  current.value = name;
  await nextTick();
  const views = { dashboard, users, tasks, feedbacks, themes };
  views[name]?.value?.refresh?.();
}
</script>

<template>
  <div class="admin-layout">
    <aside class="admin-sidebar">
      <h1>管理员端</h1>
      <button v-for="[key, label] in items" :key="key" class="admin-menu" :class="{ active: current === key }" :disabled="viewBusy || logoutBusy" @click="select(key)">{{ label }}</button>
      <button class="admin-menu" :disabled="viewBusy || logoutBusy" @click="emit('logout')">{{ logoutBusy ? '退出中…' : '退出登录' }}</button>
    </aside>
    <main class="admin-main">
      <p v-if="message" class="alert alert-danger" role="alert">{{ message }}</p>
      <AdminDashboardView v-if="visited.has('dashboard')" v-show="current === 'dashboard'" ref="dashboard" :api="api" @session-expired="emit('session-expired')" @busy-change="viewBusy = $event" />
      <AdminUsersView v-if="visited.has('users')" v-show="current === 'users'" ref="users" :api="api" @session-expired="emit('session-expired')" @busy-change="viewBusy = $event" />
      <AdminTasksView v-if="visited.has('tasks')" v-show="current === 'tasks'" ref="tasks" :api="api" @session-expired="emit('session-expired')" @busy-change="viewBusy = $event" />
      <AdminFeedbacksView v-if="visited.has('feedbacks')" v-show="current === 'feedbacks'" ref="feedbacks" :api="api" @session-expired="emit('session-expired')" @busy-change="viewBusy = $event" />
      <ThemeView v-if="visited.has('themes')" v-show="current === 'themes'" ref="themes" :user="user" embedded @theme-change="emit('theme-change', $event)" />
    </main>
  </div>
</template>
