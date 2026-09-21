<script setup>
import { onMounted, reactive, ref } from 'vue';
import { createApi } from './api.js';

const api = createApi(import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api');
const user = ref(null);
const plans = ref([]);
const busy = ref(false);
const ready = ref(false);
const message = ref('');
const failed = ref(false);
const editingId = ref(null);
const originalDates = reactive({ startDate: '', endDate: '' });
const credentials = reactive({ username: '', password: '' });
const emptyPlan = () => ({ planName: '', goal: '', description: '', startDate: '', endDate: '', status: '进行中' });
const form = reactive(emptyPlan());
const statuses = ['未开始', '进行中', '已完成', '已取消'];
function reset() { editingId.value = null; Object.assign(form, emptyPlan()); Object.assign(originalDates, { startDate: '', endDate: '' }); }
async function run(action) {
  if (busy.value) return;
  busy.value = true; message.value = ''; failed.value = false;
  try { await action(); }
  catch (error) {
    failed.value = true; message.value = error.message;
    if (error.status === 401) { user.value = null; plans.value = []; reset(); }
  } finally { busy.value = false; }
}
async function loadPlans() { plans.value = await api.get('/plans'); }
function login() {
  return run(async () => {
    user.value = await api.post('/auth/login', credentials);
    credentials.password = '';
    await loadPlans();
  });
}
function logout() {
  return run(async () => {
    await api.post('/auth/logout');
    user.value = null; plans.value = []; reset();
  });
}
function edit(plan) {
  return run(async () => {
    const detail = await api.get(`/plans/${plan.id}`);
    reset(); editingId.value = detail.id;
    for (const key of Object.keys(emptyPlan())) form[key] = detail[key] ?? '';
    Object.assign(originalDates, { startDate: detail.startDate || '', endDate: detail.endDate || '' });
  });
}
function save() {
  return run(async () => {
    if (!form.planName.trim()) throw new Error('计划名称不能为空');
    if (editingId.value !== null && ((originalDates.startDate && !form.startDate) || (originalDates.endDate && !form.endDate))) throw new Error('现有接口不支持清空已设置的日期，请填写日期后保存');
    if (form.startDate && form.endDate && form.endDate < form.startDate) throw new Error('结束日期不能早于开始日期');
    const payload = { ...form, planName: form.planName.trim(), startDate: form.startDate || null, endDate: form.endDate || null };
    if (editingId.value !== null) await api.put(`/plans/${editingId.value}`, payload);
    else await api.post('/plans', payload);
    reset();
    message.value = '计划已保存';
    await loadPlans();
  });
}
function remove(plan) {
  if (!window.confirm(`删除“${plan.planName}”及其关联任务、学习记录？此操作不可撤销。`)) return;
  return run(async () => {
    await api.delete(`/plans/${plan.id}`);
    if (editingId.value === plan.id) reset();
    message.value = '计划已删除';
    await loadPlans();
  });
}
onMounted(async () => {
  await run(async () => {
    user.value = await api.get('/auth/current');
    if (user.value) await loadPlans();
  });
  ready.value = true;
});
</script>

<template>
  <main class="container app-shell">
    <header class="page-heading">
      <div><span class="soft-label">实验二 · 前后端分离</span><h1 class="page-title mt-2">我的学习计划</h1></div>
      <button v-if="user" class="btn btn-outline-primary" :disabled="busy" @click="logout">退出登录</button>
    </header>
    <p v-if="message" :class="['alert', failed ? 'alert-danger' : 'alert-success']" role="alert">{{ message }}</p>
    <p v-if="busy" role="status">正在处理，请稍候…</p>
    <section v-if="ready && !user" class="content-card">
      <h2>登录已有账号</h2>
      <form @submit.prevent="login">
        <fieldset :disabled="busy">
          <div class="mb-3"><label for="username" class="form-label">用户名</label><input id="username" v-model="credentials.username" class="form-control" required autocomplete="username"></div>
          <div class="mb-3"><label for="password" class="form-label">密码</label><input id="password" v-model="credentials.password" class="form-control" type="password" required autocomplete="current-password"></div>
          <button class="btn btn-primary">登录</button>
        </fieldset>
      </form>
    </section>
    <template v-if="user">
      <p>你好，{{ user.realName || user.username }}</p>
      <section class="content-card mb-4">
        <h2>{{ editingId === null ? '新增计划' : '编辑计划' }}</h2>
        <form @submit.prevent="save">
          <fieldset :disabled="busy">
            <div class="mb-3"><label class="form-label" for="planName">计划名称</label><input id="planName" v-model="form.planName" class="form-control" required maxlength="100"></div>
            <div class="mb-3"><label class="form-label" for="goal">学习目标</label><input id="goal" v-model="form.goal" class="form-control" maxlength="255"></div>
            <div class="mb-3"><label class="form-label" for="description">计划描述</label><textarea id="description" v-model="form.description" class="form-control" rows="3" maxlength="10000"></textarea></div>
            <div class="row">
              <div class="col-md-4 mb-3"><label class="form-label" for="startDate">开始日期</label><input id="startDate" v-model="form.startDate" class="form-control" type="date"></div>
              <div class="col-md-4 mb-3"><label class="form-label" for="endDate">结束日期</label><input id="endDate" v-model="form.endDate" class="form-control" type="date" :min="form.startDate || undefined"></div>
              <div class="col-md-4 mb-3"><label class="form-label" for="status">状态</label><select id="status" v-model="form.status" class="form-select" required><option v-for="status in statuses" :key="status">{{ status }}</option></select></div>
            </div>
            <button class="btn btn-primary me-2">保存计划</button><button type="button" class="btn btn-outline-primary" @click="reset">{{ editingId === null ? '清空' : '取消编辑' }}</button>
          </fieldset>
        </form>
      </section>
      <section class="content-card">
        <div class="card-title-row"><h2>计划列表</h2><button class="btn btn-outline-primary" :disabled="busy" @click="run(loadPlans)">刷新</button></div>
        <p v-if="!plans.length && !busy">还没有学习计划，创建你的第一个计划吧。</p>
        <div v-else class="table-responsive"><table class="table app-table"><thead><tr><th>计划名称</th><th>学习目标</th><th>日期</th><th>状态</th><th>操作</th></tr></thead>
          <tbody><tr v-for="plan in plans" :key="plan.id"><td>{{ plan.planName }}</td><td>{{ plan.goal || '—' }}</td><td>{{ plan.startDate || '未设置' }} 至 {{ plan.endDate || '未设置' }}</td><td>{{ plan.status }}</td><td><button class="btn btn-sm btn-outline-primary me-2" :disabled="busy" @click="edit(plan)">编辑</button><button class="btn btn-sm btn-outline-danger" :disabled="busy" @click="remove(plan)">删除</button></td></tr></tbody>
        </table></div>
      </section>
    </template>
  </main>
</template>
