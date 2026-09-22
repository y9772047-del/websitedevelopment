<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { parseBatchTasks, prepareTask } from '../workflow.js';

const props = defineProps({ api: { type: Function, required: true } });
const emit = defineEmits(['session-expired', 'busy-change']);
const plans = ref([]);
const categories = ref([]);
const tasks = ref([]);
const busy = ref(false);
const error = ref('');
const message = ref('');
const editingId = ref(null);
const batchText = ref('');
const filters = reactive({ planId: '', status: '', priority: '', date: '' });
const emptyTask = () => ({
  taskName: '', planId: '', category: '', priority: 'Q2', deadline: '', status: '未完成',
  description: '', repeatType: 'NONE', repeatEndDate: '', reminderEnabled: 'true', reminderDaysBefore: 3
});
const form = reactive(emptyTask());
const planNames = computed(() => Object.fromEntries(plans.value.map(plan => [plan.id, plan.planName])));
function resetForm() { editingId.value = null; Object.assign(form, emptyTask()); }
async function run(action) {
  if (busy.value) return;
  busy.value = true; emit('busy-change', true); error.value = ''; message.value = '';
  try { await action(); }
  catch (failure) { if (failure.status === 401) emit('session-expired'); else error.value = failure.message; }
  finally { busy.value = false; emit('busy-change', false); }
}
async function loadTasks() {
  const params = Object.fromEntries(Object.entries(filters).filter(([, value]) => value));
  tasks.value = await props.api.get('/tasks', { params });
}
async function loadInitial() {
  [plans.value, categories.value] = await Promise.all([props.api.get('/plans'), props.api.get('/tasks/categories')]);
  if (!form.category) form.category = categories.value[0] || '';
  await loadTasks();
}
function edit(task) {
  editingId.value = task.id;
  Object.assign(form, emptyTask(), {
    ...task,
    planId: String(task.planId ?? ''),
    deadline: task.deadline || '',
    repeatEndDate: task.repeatEndDate || '',
    reminderEnabled: String(task.reminderEnabled ?? true),
    reminderDaysBefore: task.reminderDaysBefore ?? 3
  });
  window.scrollTo({ top: 0, behavior: 'smooth' });
}
function save() {
  return run(async () => {
    const payload = prepareTask(form);
    if (editingId.value) await props.api.put('/tasks/' + editingId.value, payload);
    else await props.api.post('/tasks', payload);
    message.value = editingId.value ? '任务已修改' : '任务已保存';
    resetForm();
    await loadTasks();
  });
}
function removeTask(task) {
  if (!window.confirm('删除“' + task.taskName + '”及其学习记录？此操作不可撤销。')) return;
  return run(async () => {
    await props.api.delete('/tasks/' + task.id);
    if (editingId.value === task.id) resetForm();
    message.value = '任务已删除';
    await loadTasks();
  });
}
function saveBatch() {
  return run(async () => {
    const payload = parseBatchTasks(batchText.value, plans.value, categories.value);
    await props.api.post('/tasks/batch', payload);
    batchText.value = '';
    message.value = '批量任务已保存';
    await loadTasks();
  });
}
function repeatLabel(type) { return ({ DAILY: '每日', WEEKLY: '每周', WORKDAY: '工作日' })[type] || '不重复'; }
function reminderLabel(task) { return task.reminderEnabled === false ? '关闭' : '提前 ' + (task.reminderDaysBefore ?? 3) + ' 天'; }
defineExpose({ refresh: () => run(loadInitial) });
onMounted(() => run(loadInitial));
</script>

<template>
  <main class="container-fluid app-shell">
    <header class="page-heading"><div><span class="soft-label">Tasks</span><h1 class="page-title">学习任务管理</h1></div></header>
    <p v-if="error" class="alert alert-danger" role="alert">{{ error }}</p>
    <p v-if="message" class="alert alert-success" role="status">{{ message }}</p>
    <section class="content-card mb-4">
      <h2>{{ editingId ? '编辑学习任务' : '任务表单' }}</h2>
      <form class="row g-3" @submit.prevent="save"><fieldset class="contents-fieldset" :disabled="busy">
        <div class="row g-3">
          <div class="col-md-3"><label class="form-label">任务名称</label><input v-model="form.taskName" class="form-control" required maxlength="100"></div>
          <div class="col-md-3"><label class="form-label">所属计划</label><select v-model="form.planId" class="form-select" required><option value="">请选择学习计划</option><option v-for="plan in plans" :key="plan.id" :value="String(plan.id)">{{ plan.planName }}</option></select></div>
          <div class="col-md-2"><label class="form-label">分类</label><select v-model="form.category" class="form-select" required><option v-for="category in categories" :key="category">{{ category }}</option></select></div>
          <div class="col-md-2"><label class="form-label">优先级</label><select v-model="form.priority" class="form-select"><option v-for="value in ['Q0','Q1','Q2','Q3']" :key="value">{{ value }}</option></select></div>
          <div class="col-md-2"><label class="form-label">截止日期</label><input v-model="form.deadline" class="form-control" type="date"></div>
          <div class="col-md-3"><label class="form-label">状态</label><select v-model="form.status" class="form-select"><option v-for="value in ['未完成','进行中','已完成','延期']" :key="value">{{ value }}</option></select></div>
          <div class="col-md-9"><label class="form-label">任务描述</label><input v-model="form.description" class="form-control"></div>
          <div class="col-12"><details><summary class="btn btn-outline-primary btn-sm">高级设置</summary><div class="row g-3 mt-1">
            <div class="col-md-3"><label class="form-label">重复规则</label><select v-model="form.repeatType" class="form-select"><option value="NONE">不重复</option><option value="DAILY">每日</option><option value="WEEKLY">每周</option><option value="WORKDAY">工作日</option></select></div>
            <div class="col-md-3"><label class="form-label">重复结束日期</label><input v-model="form.repeatEndDate" class="form-control" type="date" :disabled="form.repeatType === 'NONE'"></div>
            <div class="col-md-3"><label class="form-label">提醒</label><select v-model="form.reminderEnabled" class="form-select"><option value="true">启用提醒</option><option value="false">关闭提醒</option></select></div>
            <div class="col-md-3"><label class="form-label">提前提醒天数</label><input v-model="form.reminderDaysBefore" class="form-control" type="number" min="0" max="30"></div>
          </div></details></div>
          <div class="col-12"><button class="btn btn-primary">{{ busy ? '保存中…' : (editingId ? '保存修改' : '保存任务') }}</button> <button class="btn btn-outline-primary" type="button" @click="resetForm">{{ editingId ? '取消' : '清空' }}</button></div>
        </div>
      </fieldset></form>
    </section>
    <section class="content-card mb-4">
      <h2>批量新增任务</h2>
      <p class="muted-text">每行格式：任务名称 | 所属计划 | 分类 | 优先级 | 截止日期 | 任务描述</p>
      <textarea v-model="batchText" class="form-control mb-3" rows="5" placeholder="每行一个任务"></textarea>
      <button class="btn btn-primary" :disabled="busy" @click="saveBatch">批量保存任务</button>
      <button class="btn btn-outline-primary ms-2" :disabled="busy" @click="batchText = ''">清空输入</button>
    </section>
    <section class="content-card">
      <div class="card-title-row"><h2>任务列表</h2><button class="btn btn-outline-primary btn-sm" :disabled="busy" @click="run(loadTasks)">刷新</button></div>
      <div class="row g-2 mb-3">
        <div class="col-md-3"><select v-model="filters.planId" class="form-select"><option value="">全部计划</option><option v-for="plan in plans" :key="plan.id" :value="String(plan.id)">{{ plan.planName }}</option></select></div>
        <div class="col-md-2"><select v-model="filters.priority" class="form-select"><option value="">全部优先级</option><option v-for="value in ['Q0','Q1','Q2','Q3']" :key="value">{{ value }}</option></select></div>
        <div class="col-md-2"><select v-model="filters.status" class="form-select"><option value="">全部状态</option><option v-for="value in ['未完成','进行中','已完成','延期']" :key="value">{{ value }}</option></select></div>
        <div class="col-md-3"><input v-model="filters.date" class="form-control" type="date"></div>
        <div class="col-md-2"><button class="btn btn-outline-primary w-100" :disabled="busy" @click="run(loadTasks)">筛选</button></div>
      </div>
      <div class="table-responsive"><table class="table app-table align-middle"><thead><tr><th>任务名称</th><th>所属计划</th><th>分类</th><th>优先级</th><th>截止日期</th><th>重复</th><th>提醒</th><th>状态</th><th>操作</th></tr></thead><tbody>
        <tr v-for="task in tasks" :key="task.id"><td>{{ task.taskName }}</td><td>{{ planNames[task.planId] || '未知计划' }}</td><td>{{ task.category }}</td><td>{{ task.priority }}</td><td>{{ task.deadline || '—' }}</td><td>{{ repeatLabel(task.repeatType) }}</td><td>{{ reminderLabel(task) }}</td><td>{{ task.status }}</td><td><button class="btn btn-sm btn-primary me-2" :disabled="busy" @click="edit(task)">编辑</button><button class="btn btn-sm btn-outline-primary" :disabled="busy" @click="removeTask(task)">删除</button></td></tr>
        <tr v-if="!tasks.length"><td colspan="9">暂无学习任务</td></tr>
      </tbody></table></div>
    </section>
  </main>
</template>