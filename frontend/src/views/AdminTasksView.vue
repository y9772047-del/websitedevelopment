<script setup>
import { onMounted, reactive, ref } from 'vue';
import { buildQuery } from '../portal.js';

const props = defineProps({ api: { type: Function, required: true } });
const emit = defineEmits(['session-expired', 'busy-change']);
const tasks = ref([]);
const filters = reactive({ username: '', keyword: '', priority: '', status: '', startDate: '', endDate: '' });
const detail = ref(null);
const busy = ref(false);
const error = ref('');
const message = ref('');
const loaded = ref(false);

function badgeClass(status) {
  if (status === '已完成') return 'status-done';
  if (status === '延期') return 'status-delay';
  if (status === '未完成') return 'status-waiting';
  return 'status-doing';
}
function priorityClass(priority) { return ['Q0', 'Q1', 'Q2', 'Q3'].includes(priority) ? 'priority-' + priority.toLowerCase() : 'priority-q2'; }
function handleError(failure) { if (failure.status === 401 || failure.status === 403) emit('session-expired'); else error.value = failure.message; }
async function run(action) {
  if (busy.value) return;
  busy.value = true; emit('busy-change', true); error.value = ''; message.value = '';
  try { await action(); } catch (failure) { handleError(failure); }
  finally { busy.value = false; emit('busy-change', false); }
}
async function loadTasks() {
  const query = buildQuery(filters);
  tasks.value = await props.api.get('/admin/tasks' + (query ? '?' + query : ''));
  loaded.value = true;
}
function search() { return run(loadTasks); }
function reset() { Object.assign(filters, { username: '', keyword: '', priority: '', status: '', startDate: '', endDate: '' }); return search(); }
function showDetail(task) { return run(async () => { detail.value = await props.api.get('/admin/tasks/' + task.id); }); }
function closeDetail() { if (!busy.value) detail.value = null; }
function remove(task) {
  if (!window.confirm(`确定删除任务“${task.taskName}”及其学习记录吗？此操作不可撤销。`)) return;
  return run(async () => {
    await props.api.delete('/admin/tasks/' + task.id);
    detail.value = null; message.value = '任务已删除';
    await loadTasks();
  });
}
function refresh() { return search(); }
defineExpose({ refresh });
onMounted(search);
</script>

<template>
  <section class="admin-section" :aria-busy="busy">
    <div class="page-heading"><div><span class="soft-label">Tasks</span><h1 class="page-title">学习任务管理</h1></div></div>
    <p v-if="error" class="alert alert-danger" role="alert">{{ error }}</p><p v-if="message" class="alert alert-success" role="status">{{ message }}</p>
    <div class="content-card mb-4"><form class="row g-3" @submit.prevent="search">
      <div class="col-md-2"><label class="form-label">用户名</label><input v-model="filters.username" class="form-control"></div><div class="col-md-2"><label class="form-label">任务关键词</label><input v-model="filters.keyword" class="form-control"></div>
      <div class="col-md-2"><label class="form-label">优先级</label><select v-model="filters.priority" class="form-select"><option value="">全部</option><option>Q0</option><option>Q1</option><option>Q2</option><option>Q3</option></select></div>
      <div class="col-md-2"><label class="form-label">状态</label><select v-model="filters.status" class="form-select"><option value="">全部</option><option>未完成</option><option>进行中</option><option>已完成</option><option>延期</option></select></div>
      <div class="col-md-2"><label class="form-label">开始日期</label><input v-model="filters.startDate" class="form-control" type="date"></div><div class="col-md-2"><label class="form-label">结束日期</label><input v-model="filters.endDate" class="form-control" type="date"></div>
      <div class="col-12"><button class="btn btn-primary" :disabled="busy">筛选</button><button class="btn btn-outline-primary ms-2" type="button" :disabled="busy" @click="reset">重置</button></div>
    </form></div>
    <div class="content-card"><div class="card-title-row"><h2>任务列表</h2><span v-if="busy" class="muted-text">正在加载…</span></div><div class="table-responsive"><table class="table app-table align-middle">
      <thead><tr><th>ID</th><th>任务名称</th><th>所属用户</th><th>所属计划</th><th>分类</th><th>优先级</th><th>截止日期</th><th>状态</th><th>创建时间</th><th>操作</th></tr></thead>
      <tbody><tr v-for="task in tasks" :key="task.id"><td>{{ task.id }}</td><td>{{ task.taskName }}</td><td>{{ task.username }}</td><td>{{ task.planName || '未关联' }}</td><td>{{ task.category || '—' }}</td><td><span class="badge" :class="priorityClass(task.priority)">{{ task.priority || '—' }}</span></td><td>{{ task.deadline || '—' }}</td><td><span class="badge" :class="badgeClass(task.status)">{{ task.status }}</span></td><td>{{ task.createTime || '—' }}</td><td><div class="d-flex flex-wrap gap-1"><button class="btn btn-sm btn-primary" :disabled="busy" @click="showDetail(task)">查看</button><button class="btn btn-sm btn-outline-primary" :disabled="busy" @click="remove(task)">删除</button></div></td></tr><tr v-if="!tasks.length"><td colspan="10" class="muted-text">{{ busy ? '正在加载任务…' : loaded ? '暂无任务' : '任务尚未加载' }}</td></tr></tbody>
    </table></div></div>
    <template v-if="detail"><div class="modal fade show d-block" tabindex="-1" role="dialog" aria-modal="true" aria-labelledby="adminTaskDetailTitle"><div class="modal-dialog modal-lg modal-dialog-scrollable"><div class="modal-content admin-modal"><div class="modal-header"><h2 id="adminTaskDetailTitle" class="modal-title fs-5">任务详情</h2><button class="btn-close" type="button" aria-label="关闭" @click="closeDetail"></button></div><div class="modal-body">
      <p><strong>任务名称：</strong>{{ detail.taskName }}</p><p><strong>任务描述：</strong>{{ detail.description || '未填写' }}</p><p><strong>所属用户：</strong>{{ detail.username }}　<strong>所属计划：</strong>{{ detail.planName || '未关联' }}</p><p><strong>分类：</strong>{{ detail.category || '—' }}　<strong>优先级：</strong>{{ detail.priority || '—' }}</p><p><strong>截止日期：</strong>{{ detail.deadline || '—' }}　<strong>状态：</strong>{{ detail.status || '—' }}</p><p><strong>创建时间：</strong>{{ detail.createTime || '—' }}</p><p><strong>更新时间：</strong>{{ detail.updateTime || '—' }}</p>
    </div></div></div></div><div class="modal-backdrop fade show"></div></template>
  </section>
</template>
