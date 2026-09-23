<script setup>
import { computed, onMounted, ref } from 'vue';
import { summarizeTasks } from '../insights.js';

const props = defineProps({ api: { type: Function, required: true } });
const emit = defineEmits(['session-expired', 'busy-change']);
const statistics = ref({}), tasks = ref([]), busy = ref(false), error = ref('');
const today = () => new Date().toISOString().slice(0, 10);
const summary = computed(() => summarizeTasks(tasks.value, today()));
const statusColors = { '未完成':'#f4a62a','进行中':'#5c8df6','已完成':'#5fbf8f','延期':'#f05f7d' };
const priorityColors = { Q0:'#d94b64',Q1:'#f05f7d',Q2:'#f4a62a',Q3:'#5fbf8f' };
function chartRows(values, colors) {
  const maximum = Math.max(1, ...Object.values(values));
  return Object.entries(values).map(([label,value]) => ({ label, value, width: Math.round(value * 100 / maximum), color: colors[label] || 'var(--primary)' }));
}
const statusRows = computed(() => chartRows(summary.value.statusCounts, statusColors));
const priorityRows = computed(() => chartRows(summary.value.priorityCounts, priorityColors));
async function run(action) {
  if (busy.value) return;
  busy.value = true; emit('busy-change', true); error.value = '';
  try { await action(); }
  catch (failure) { if (failure.status === 401) emit('session-expired'); else error.value = failure.message; }
  finally { busy.value = false; emit('busy-change', false); }
}
async function loadAll() { [statistics.value, tasks.value] = await Promise.all([props.api.get('/statistics'), props.api.get('/tasks')]); }
defineExpose({ refresh: () => run(loadAll) });
onMounted(() => run(loadAll));
</script>

<template>
  <main class="container-fluid app-shell">
    <header class="page-heading"><div><span class="soft-label">Statistics</span><h1 class="page-title">学习统计</h1></div><button class="btn btn-outline-primary btn-sm" :disabled="busy" @click="run(loadAll)">刷新</button></header>
    <p v-if="error" class="alert alert-danger" role="alert">{{ error }}</p>
    <section class="row g-3 mb-4">
      <div v-for="item in [
        ['计划数量', statistics.planCount || 0],
        ['任务数量', statistics.taskCount || 0],
        ['逾期任务', statistics.overdueTaskCount || 0],
        ['累计学习时长', (statistics.totalDuration || 0) + ' 分钟']
      ]" :key="item[0]" class="col-sm-6 col-xl-3"><div class="metric-card"><span>{{ item[0] }}</span><strong>{{ item[1] }}</strong></div></div>
    </section>
    <section class="row g-4">
      <div class="col-lg-4"><div class="content-card stat-summary-card"><h2>任务完成率</h2><strong>{{ statistics.completionRate || 0 }}%</strong><p class="muted-text">已完成 {{ statistics.completedTaskCount || 0 }} / {{ statistics.taskCount || 0 }}</p><div class="progress stat-progress"><div class="progress-bar" :style="{width: (statistics.completionRate || 0) + '%'}">{{ statistics.completionRate || 0 }}%</div></div></div></div>
      <div class="col-lg-4"><div class="content-card"><h2>任务状态分布</h2><div class="mini-chart"><div v-for="row in statusRows" :key="row.label" class="mini-chart-row"><span class="mini-chart-label">{{ row.label }}</span><span class="mini-chart-track"><i class="mini-chart-bar" :style="{width: row.width + '%', background: row.color}"></i></span><span class="mini-chart-value">{{ row.value }}</span></div><p v-if="!tasks.length" class="mini-chart-empty">暂无任务数据</p></div></div></div>
      <div class="col-lg-4"><div class="content-card"><h2>优先级分布</h2><div class="mini-chart"><div v-for="row in priorityRows" :key="row.label" class="mini-chart-row"><span class="mini-chart-label">{{ row.label }}</span><span class="mini-chart-track"><i class="mini-chart-bar" :style="{width: row.width + '%', background: row.color}"></i></span><span class="mini-chart-value">{{ row.value }}</span></div></div></div></div>
    </section>
  </main>
</template>