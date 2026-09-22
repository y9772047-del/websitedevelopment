<script setup>
import { onMounted, ref } from 'vue';
import PlanForm from '../components/PlanForm.vue';
import { statusClass } from '../plan.js';
const props = defineProps({ api: { type: Function, required: true } });
const emit = defineEmits(['session-expired', 'busy-change']);
const plans = ref([]);
const tasks = ref([]);
const selected = ref(null);
const page = ref('list');
const busy = ref(false);
const message = ref('');
const failed = ref(false);
const loaded = ref(false);
const formVersion = ref(0);
function handleError(error, prefix = '') {
  if (error.status === 401) emit('session-expired');
  failed.value = true; message.value = prefix + error.message;
}
async function perform(action) {
  if (busy.value) return;
  busy.value = true; emit('busy-change', true); message.value = ''; failed.value = false;
  try { await action(); } catch (error) { handleError(error); }
  finally { busy.value = false; emit('busy-change', false); }
}
async function loadPlans() {
  plans.value = await props.api.get('/plans');
  loaded.value = true;
}
async function refreshAfter(successMessage) {
  message.value = successMessage;
  try { await loadPlans(); } catch (error) { handleError(error, `${successMessage}；列表刷新失败：`); }
}
function save(payload) {
  return perform(async () => {
    const editing = page.value === 'edit';
    if (editing) await props.api.put(`/plans/${selected.value.id}`, payload);
    else await props.api.post('/plans', payload);
    page.value = 'list'; selected.value = null; if (!editing) formVersion.value++;
    await refreshAfter('计划已保存');
  });
}
function edit(plan) {
  return perform(async () => {
    selected.value = await props.api.get(`/plans/${plan.id}`);
    page.value = 'edit';
  });
}
function detail(plan) {
  return perform(async () => {
    const current = await props.api.get(`/plans/${plan.id}`);
    const related = await props.api.get('/tasks', { params: { planId: plan.id } });
    selected.value = current; tasks.value = related; page.value = 'detail';
  });
}
function remove(plan) {
  if (busy.value || !window.confirm(`确定删除计划“${plan.planName}”吗？关联任务和学习记录也会删除，且无法撤销。`)) return;
  return perform(async () => {
    await props.api.delete(`/plans/${plan.id}`);
    await refreshAfter('计划已删除');
  });
}
function back() { if (!busy.value) { page.value = 'list'; selected.value = null; message.value = ''; } }
function priorityClass(priority) { return ['Q0', 'Q1', 'Q2', 'Q3'].includes(priority) ? `priority-${priority.toLowerCase()}` : 'priority-q2'; }
defineExpose({ showList: back });
onMounted(() => perform(loadPlans));
</script>

<template>
  <main class="container-fluid app-shell" :aria-busy="busy">
    <p v-if="message" class="alert" :class="failed ? 'alert-danger' : 'alert-success'" role="alert">{{ message }}</p>
    <p v-if="busy" class="muted-text" role="status">正在处理，请稍候…</p>
    <section v-show="page === 'list'" id="plans" class="page-section">
      <div class="page-heading"><div><span class="soft-label">Plans</span><h1 class="page-title">学习计划管理</h1></div></div>
      <div class="content-card mb-4"><h2>计划表单</h2><PlanForm :key="formVersion" :busy="busy" @save="save" /></div>
      <div class="content-card">
        <div class="card-title-row"><h2>计划列表</h2><button class="btn btn-outline-primary btn-sm" :disabled="busy" @click="perform(loadPlans)">刷新列表</button></div>
        <div class="table-responsive"><table class="table app-table align-middle">
          <thead><tr><th>计划名称</th><th>目标</th><th>周期</th><th>状态</th><th>操作</th></tr></thead>
          <tbody>
            <tr v-for="plan in plans" :key="plan.id">
              <td>{{ plan.planName }}</td><td>{{ plan.goal || '—' }}</td><td>{{ plan.startDate || '未设置' }} 至 {{ plan.endDate || '未设置' }}</td>
              <td><span class="badge" :class="statusClass(plan.status)">{{ plan.status || '未设置' }}</span></td>
              <td><div class="d-flex flex-wrap gap-1"><button class="btn btn-sm btn-primary" :disabled="busy" @click="edit(plan)">编辑</button><button class="btn btn-sm btn-outline-primary" :disabled="busy" @click="detail(plan)">查看详情</button><button class="btn btn-sm btn-outline-primary" :disabled="busy" @click="remove(plan)">删除</button></div></td>
            </tr>
            <tr v-if="!plans.length"><td colspan="5" class="muted-text">{{ busy ? '正在加载计划…' : loaded ? '暂无学习计划，请先新增计划。' : '列表尚未加载，请点击“刷新列表”重试。' }}</td></tr>
          </tbody>
        </table></div>
      </div>
    </section>
    <section v-if="page === 'edit'" id="planEdit" class="page-section">
      <div class="page-heading"><div><span class="soft-label">Plan Edit</span><h1 class="page-title">编辑学习计划</h1></div><button class="btn btn-outline-primary" :disabled="busy" @click="back">返回计划列表</button></div>
      <div class="content-card"><h2>计划编辑表单</h2><PlanForm :plan="selected" :busy="busy" form-id="planEditForm" @save="save" @cancel="back" /></div>
    </section>
    <section v-if="page === 'detail'" id="planDetail" class="page-section">
      <div class="page-heading"><div><span class="soft-label">Plan Detail</span><h1 class="page-title">{{ selected.planName }}</h1><p class="muted-text mt-2 mb-0">{{ selected.startDate || '未设置' }} 至 {{ selected.endDate || '未设置' }}｜{{ selected.status }}</p></div><button class="btn btn-outline-primary" :disabled="busy" @click="back">返回计划列表</button></div>
      <div class="content-card mb-4"><h2>计划详情</h2><p>学习目标：{{ selected.goal || '未填写' }}</p><p class="mb-0">计划描述：{{ selected.description || '未填写' }}</p></div>
      <div class="content-card"><h2>关联学习任务</h2><div class="table-responsive"><table class="table app-table align-middle">
        <thead><tr><th>任务名称</th><th>分类</th><th>优先级</th><th>截止日期</th><th>状态</th></tr></thead>
        <tbody><tr v-for="task in tasks" :key="task.id"><td>{{ task.taskName }}</td><td>{{ task.category || '—' }}</td><td><span class="badge" :class="priorityClass(task.priority)">{{ task.priority }}</span></td><td>{{ task.deadline || '—' }}</td><td><span class="badge" :class="statusClass(task.status)">{{ task.status }}</span></td></tr><tr v-if="!tasks.length"><td colspan="5">暂无学习任务</td></tr></tbody>
      </table></div></div>
    </section>
  </main>
</template>
