<script setup>
import { computed, onMounted, ref } from 'vue';
import PlanForm from '../components/PlanForm.vue';
import PlanHierarchy from '../components/PlanHierarchy.vue';
import { statusClass } from '../plan.js';
import { buildPlanTree } from '../planTree.js';
const props = defineProps({ api: { type: Function, required: true } });
const emit = defineEmits(['session-expired', 'busy-change', 'navigate']);
const plans = ref([]);
const tasks = ref([]);
const records = ref([]);
const review = ref(null);
const reviewBusy = ref(false);
const reviewError = ref('');
const selected = ref(null);
const page = ref('list');
const busy = ref(false);
const message = ref('');
const failed = ref(false);
const loaded = ref(false);
const formVersion = ref(0);
const overview = computed(() => selected.value ? buildPlanTree([selected.value], tasks.value, records.value)[0] : null);
function handleError(error, prefix = '') {
  if (error.status === 401) emit('session-expired');
  failed.value = true; message.value = prefix + error.message;
}
async function perform(action) {
  if (busy.value || reviewBusy.value) return;
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
    const allRecords = await props.api.get('/records');
    const taskIds = new Set(related.map(task => String(task.id)));
    selected.value = current; tasks.value = related; records.value = allRecords.filter(record => taskIds.has(String(record.taskId)));
    review.value = null; reviewError.value = ''; page.value = 'detail';
  });
}
function remove(plan) {
  if (busy.value || !window.confirm(`确定删除计划“${plan.planName}”吗？关联任务和学习记录也会删除，且无法撤销。`)) return;
  return perform(async () => {
    await props.api.delete(`/plans/${plan.id}`);
    await refreshAfter('计划已删除');
  });
}
function back() { if (!busy.value && !reviewBusy.value) { page.value = 'list'; selected.value = null; message.value = ''; } }
async function generateReview() {
  if (busy.value || reviewBusy.value || !selected.value) return;
  reviewBusy.value = true; emit('busy-change', true); reviewError.value = ''; review.value = null;
  try { review.value = await props.api.post(`/ai/plans/${selected.value.id}/review`); }
  catch (error) { if (error.status === 401) emit('session-expired'); else reviewError.value = error.message; }
  finally { reviewBusy.value = false; emit('busy-change', false); }
}
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
    <section v-if="page === 'detail'" id="planDetail" class="page-section plan-detail">
      <div class="plan-detail-toolbar"><button class="btn btn-outline-primary btn-sm" :disabled="busy || reviewBusy" @click="back">← 返回计划列表</button><span class="muted-text">学习计划 / 查看详情</span></div>
      <header class="plan-detail-hero">
        <div class="plan-detail-hero-copy"><div class="d-flex align-items-center gap-2 flex-wrap"><span class="soft-label">学习计划</span><span class="badge" :class="statusClass(selected.status)">{{ selected.status || '未设置状态' }}</span></div><h1>{{ selected.planName }}</h1><p class="plan-detail-goal">{{ selected.goal || '还没有填写学习目标' }}</p><p v-if="selected.description" class="plan-detail-description">{{ selected.description }}</p><div class="plan-detail-period"><span>计划周期</span><strong>{{ selected.startDate || '未设置' }} — {{ selected.endDate || '未设置' }}</strong></div></div>
        <div class="plan-detail-progress"><span>任务完成率</span><strong>{{ overview.progress }}<small>%</small></strong><div class="progress" role="progressbar" :aria-label="`${selected.planName}任务完成率`" :aria-valuenow="overview.progress" aria-valuemin="0" aria-valuemax="100"><div class="progress-bar" :style="{ width: overview.progress + '%' }"></div></div><small>{{ overview.completedTasks }} / {{ overview.tasks.length }} 项任务已完成</small></div>
      </header>
      <div class="plan-detail-metrics"><div class="metric-card"><span>学习任务</span><strong>{{ overview.tasks.length }}</strong><small>围绕当前目标</small></div><div class="metric-card"><span>学习记录</span><strong>{{ overview.recordCount }}</strong><small>实际执行证据</small></div><div class="metric-card"><span>累计投入</span><strong>{{ overview.minutes }}<small> 分钟</small></strong><small>由学习记录汇总</small></div></div>
      <section class="content-card plan-detail-tree-panel mb-4"><div class="plan-detail-section-heading"><div><span class="soft-label">01 · 目标路径</span><h2>从目标到每一次学习</h2><p>点击学习任务，在右侧展开对应记录；窄屏可左右滑动。</p></div><div class="plan-detail-tree-actions"><span class="plan-detail-tree-count">1 个目标 · {{ overview.tasks.length }} 项任务 · {{ overview.recordCount }} 条记录</span><button class="btn btn-outline-primary btn-sm" @click="emit('navigate', 'records')">查看全部记录 →</button></div></div><div class="plan-detail-tree-viewport"><PlanHierarchy :plan="selected" :tasks="tasks" :records="records" /></div></section>
      <section class="content-card plan-detail-review"><div class="plan-detail-section-heading"><div><span class="soft-label">02 · 目标复盘</span><h2>用学习记录检验目标进展</h2><p>综合该计划下的学习内容、问题和任务完成情况。</p></div><button class="btn btn-primary" :disabled="busy || reviewBusy || records.length < 2" @click="generateReview">{{ reviewBusy ? '正在生成…' : review ? '重新生成复盘' : '生成 AI 复盘' }}</button></div>
        <p v-if="records.length < 2" class="plan-detail-review-hint">再添加 {{ 2 - records.length }} 条学习记录，就可以生成目标复盘。</p><p v-if="reviewError" class="alert alert-danger" role="alert">{{ reviewError }}</p><p v-if="reviewBusy" class="muted-text" role="status">正在分析该目标的学习记录，请稍候…</p>
        <div v-if="review" class="plan-detail-review-grid"><div><span>01 / 目标进展</span><p>{{ review.summary }}</p></div><div><span>02 / 主要问题</span><p>{{ review.problems }}</p></div><div><span>03 / 下一步行动</span><ol><li v-for="action in review.actions || []" :key="action">{{ action }}</li></ol></div></div>
        <p v-if="review" class="plan-detail-review-source">依据 {{ review.recordCount }} 条记录 · {{ review.startDate || '未设置日期' }} 至 {{ review.endDate || '未设置日期' }} · {{ review.generationSourceLabel }}</p>
        <div v-else-if="!reviewBusy && records.length >= 2 && !reviewError" class="plan-detail-review-empty">已经有 {{ records.length }} 条记录。点击“生成 AI 复盘”查看目标进展与下一步行动。</div>
      </section>
    </section>
  </main>
</template>
