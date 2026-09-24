<script setup>
import { onMounted, reactive, ref } from 'vue';
import { buildQuery, nextUserStatus } from '../portal.js';

const props = defineProps({ api: { type: Function, required: true } });
const emit = defineEmits(['session-expired', 'busy-change']);
const users = ref([]);
const filters = reactive({ username: '' });
const detail = ref(null);
const busy = ref(false);
const error = ref('');
const message = ref('');
const loaded = ref(false);

function badgeClass(status) { return Number(status) === 1 ? 'status-done' : 'status-delay'; }
function statusText(status) { return Number(status) === 1 ? '正常' : '禁用'; }
function roleText(role) { return role === 'admin' ? '管理员' : '普通学生'; }
function handleError(failure) {
  if (failure.status === 401 || failure.status === 403) emit('session-expired');
  else error.value = failure.message;
}
async function run(action) {
  if (busy.value) return;
  busy.value = true; emit('busy-change', true); error.value = ''; message.value = '';
  try { await action(); } catch (failure) { handleError(failure); }
  finally { busy.value = false; emit('busy-change', false); }
}
async function loadUsers() {
  const query = buildQuery(filters);
  users.value = await props.api.get('/admin/users' + (query ? '?' + query : ''));
  loaded.value = true;
}
function search() { return run(loadUsers); }
function reset() { filters.username = ''; return search(); }
function showDetail(user) { return run(async () => { detail.value = await props.api.get('/admin/users/' + user.id); }); }
function closeDetail() { if (!busy.value) detail.value = null; }
function toggle(user) {
  const status = nextUserStatus(user.status);
  const action = status === 0 ? '禁用' : '启用';
  if (!window.confirm(`确定${action}用户“${user.username}”吗？`)) return;
  return run(async () => {
    await props.api.put('/admin/users/' + user.id + '/status', { status });
    message.value = action + '成功';
    await loadUsers();
  });
}
function refresh() { return search(); }
defineExpose({ refresh });
onMounted(search);
</script>

<template>
  <section class="admin-section" :aria-busy="busy">
    <div class="page-heading"><div><span class="soft-label">Users</span><h1 class="page-title">用户管理</h1></div></div>
    <p v-if="error" class="alert alert-danger" role="alert">{{ error }}</p><p v-if="message" class="alert alert-success" role="status">{{ message }}</p>
    <div class="content-card mb-4"><form class="row g-3" @submit.prevent="search"><div class="col-md-4"><label class="form-label" for="adminUsername">用户名</label><input id="adminUsername" v-model="filters.username" class="form-control" placeholder="按用户名搜索"></div><div class="col-md-8 d-flex align-items-end gap-2"><button class="btn btn-primary" :disabled="busy">搜索</button><button class="btn btn-outline-primary" type="button" :disabled="busy" @click="reset">重置</button></div></form></div>
    <div class="content-card"><div class="card-title-row"><h2>用户列表</h2><span v-if="busy" class="muted-text">正在加载…</span></div><div class="table-responsive"><table class="table app-table align-middle">
      <thead><tr><th>ID</th><th>用户名</th><th>角色</th><th>状态</th><th>注册时间</th><th>任务数</th><th>操作</th></tr></thead>
      <tbody><tr v-for="user in users" :key="user.id"><td>{{ user.id }}</td><td>{{ user.username }}</td><td>{{ roleText(user.role) }}</td><td><span class="badge" :class="badgeClass(user.status)">{{ statusText(user.status) }}</span></td><td>{{ user.createTime || '—' }}</td><td>{{ user.taskCount || 0 }}</td><td><div class="d-flex flex-wrap gap-1"><button class="btn btn-sm btn-primary" :disabled="busy" @click="showDetail(user)">查看</button><button v-if="user.role !== 'admin'" class="btn btn-sm btn-outline-primary" :disabled="busy" @click="toggle(user)">{{ Number(user.status) === 1 ? '禁用' : '启用' }}</button></div></td></tr><tr v-if="!users.length"><td colspan="7" class="muted-text">{{ busy ? '正在加载用户…' : loaded ? '暂无用户' : '用户尚未加载' }}</td></tr></tbody>
    </table></div></div>
    <template v-if="detail"><div class="modal fade show d-block" tabindex="-1" role="dialog" aria-modal="true" aria-labelledby="adminUserDetailTitle"><div class="modal-dialog modal-lg modal-dialog-scrollable"><div class="modal-content admin-modal"><div class="modal-header"><h2 id="adminUserDetailTitle" class="modal-title fs-5">用户详情</h2><button class="btn-close" type="button" aria-label="关闭" @click="closeDetail"></button></div><div class="modal-body">
      <p><strong>用户名：</strong>{{ detail.username }}</p><p><strong>角色：</strong>{{ roleText(detail.role) }}　<strong>状态：</strong>{{ statusText(detail.status) }}</p><p><strong>注册时间：</strong>{{ detail.createTime || '—' }}</p>
      <div class="row g-3 mb-3"><div class="col-md-3"><div class="metric-card"><span>任务数量</span><strong>{{ detail.taskCount || 0 }}</strong></div></div><div class="col-md-3"><div class="metric-card"><span>已完成</span><strong>{{ detail.completedTaskCount || 0 }}</strong></div></div><div class="col-md-3"><div class="metric-card"><span>未完成</span><strong>{{ detail.unfinishedTaskCount || 0 }}</strong></div></div><div class="col-md-3"><div class="metric-card"><span>逾期</span><strong>{{ detail.overdueTaskCount || 0 }}</strong></div></div></div>
      <h3 class="fs-6">最近创建的学习任务</h3><div v-for="task in detail.recentTasks || []" :key="task.id" class="list-row mb-2"><div><h3>{{ task.taskName }}</h3><p>{{ task.deadline || '—' }} · {{ task.status || '—' }}</p></div></div><p v-if="!(detail.recentTasks || []).length">暂无任务</p>
    </div></div></div></div><div class="modal-backdrop fade show"></div></template>
  </section>
</template>
