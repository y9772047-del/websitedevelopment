<script setup>
import { computed, onMounted, ref } from 'vue';
import { calculatePlanInvestment, summarizeTasks } from '../insights.js';

const props = defineProps({ api: { type: Function, required: true } });
const emit = defineEmits(['session-expired', 'busy-change']);
const dashboard = ref({}), plans = ref([]), tasks = ref([]), records = ref([]);
const busy = ref(false), error = ref(''), investmentRange = ref('30');
const colors = ['#f05f7d','#5fbf8f','#f4a62a','#5c8df6','#9b70d9','#42b8c6','#e66bb5','#7a9a3f'];
const today = () => new Date().toISOString().slice(0, 10);
const summary = computed(() => summarizeTasks(tasks.value, today()));
const taskMap = computed(() => Object.fromEntries(tasks.value.map(item => [item.id, item])));
function rangeStart(days) { if (!days) return null; const date = new Date(); date.setUTCDate(date.getUTCDate() - days + 1); return date.toISOString().slice(0, 10); }
const investments = computed(() => calculatePlanInvestment(records.value, tasks.value, plans.value, rangeStart(investmentRange.value === 'all' ? null : Number(investmentRange.value))));
const investmentTotal = computed(() => investments.value.reduce((sum, item) => sum + item.minutes, 0));
const donutStyle = computed(() => {
  if (!investmentTotal.value) return { background: 'var(--surface-soft)' };
  let cursor = 0;
  const stops = investments.value.map((item, index) => {
    const start = cursor; cursor += item.percent;
    return colors[index % colors.length] + ' ' + start + '% ' + cursor + '%';
  });
  return { background: 'conic-gradient(' + stops.join(',') + ')' };
});
const reminders = computed(() => [...tasks.value].filter(task => task.status !== '已完成' && task.deadline).sort((a,b) => a.deadline.localeCompare(b.deadline)).slice(0, 6));
const heatmap = computed(() => {
  const totals = new Map();
  records.value.forEach(record => totals.set(record.studyDate, (totals.get(record.studyDate) || 0) + Number(record.duration || 0)));
  const result = [];
  for (let offset = 27; offset >= 0; offset--) {
    const date = new Date(); date.setUTCDate(date.getUTCDate() - offset);
    const key = date.toISOString().slice(0, 10), minutes = totals.get(key) || 0;
    result.push({ date: key, minutes, level: minutes === 0 ? 0 : minutes < 30 ? 1 : minutes < 60 ? 2 : minutes < 120 ? 3 : minutes < 180 ? 4 : 5 });
  }
  return result;
});
function recordLabel(record) { return taskMap.value[record.taskId]?.taskName || '未知任务'; }
async function run(action) {
  if (busy.value) return;
  busy.value = true; emit('busy-change', true); error.value = '';
  try { await action(); }
  catch (failure) { if (failure.status === 401) emit('session-expired'); else error.value = failure.message; }
  finally { busy.value = false; emit('busy-change', false); }
}
async function loadAll() {
  [dashboard.value, plans.value, tasks.value, records.value] = await Promise.all([
    props.api.get('/dashboard'), props.api.get('/plans'), props.api.get('/tasks'), props.api.get('/records')
  ]);
}
defineExpose({ refresh: () => run(loadAll) });
onMounted(() => run(loadAll));
</script>

<template>
  <main class="container-fluid app-shell">
    <header class="page-heading"><div><span class="soft-label">Dashboard</span><h1 class="page-title">首页 / 仪表盘</h1></div><button class="btn btn-outline-primary btn-sm" :disabled="busy" @click="run(loadAll)">刷新</button></header>
    <p v-if="error" class="alert alert-danger" role="alert">{{ error }}</p>
    <section class="row g-3 mb-4">
      <div v-for="item in [
        ['计划总数', dashboard.planCount || 0, '当前学习计划'],
        ['任务总数', dashboard.taskCount || 0, '全部学习任务'],
        ['已完成任务', dashboard.completedTaskCount || 0, '完成率 ' + (dashboard.completionRate || 0) + '%'],
        ['逾期任务', dashboard.overdueTaskCount || 0, '需要优先处理']
      ]" :key="item[0]" class="col-sm-6 col-xl-3"><div class="metric-card"><span>{{ item[0] }}</span><strong>{{ item[1] }}</strong><small>{{ item[2] }}</small></div></div>
    </section>

    <section class="content-card mb-4">
      <div class="card-title-row"><div><h2>学习提醒</h2><p class="muted-text mb-0">即将截止和逾期任务。</p></div></div>
      <div class="stack-list">
        <div v-for="task in reminders" :key="task.id" class="list-row"><div><h3>{{ task.taskName }}</h3><p>{{ task.category || '未分类' }} · {{ task.priority || 'Q2' }}</p></div><span class="badge" :class="task.deadline < today() ? 'text-bg-danger' : 'text-bg-light'">{{ task.deadline }}</span></div>
        <div v-if="!reminders.length" class="list-row"><p>暂无待处理提醒。</p></div>
      </div>
    </section>

    <section class="dashboard-chart-grid mb-4">
      <div class="content-card chart-card category-heatmap">
        <div class="card-title-row"><div><h2>最近 28 天学习热力图</h2><p class="muted-text mb-0">按学习记录时长展示活跃度。</p></div></div>
        <div class="heatmap-scroll"><div class="heatmap-grid">
          <span v-for="day in heatmap" :key="day.date" class="heatmap-swatch" :class="'heatmap-level-' + day.level" :title="day.date + ' · ' + day.minutes + ' 分钟'"></span>
        </div></div>
        <div class="heatmap-legend"><span>少</span><i v-for="level in [0,1,2,3,4,5]" :key="level" class="heatmap-swatch" :class="'heatmap-level-' + level"></i><span>多</span></div>
      </div>

      <div class="content-card chart-card plan-investment-card">
        <div class="card-title-row"><div><h2>学习计划投入分布</h2><p class="muted-text mb-0">按学习记录累计时长。</p></div><div class="plan-investment-range-buttons"><button v-for="item in [['30','最近30天'],['180','最近180天'],['all','全部']]" :key="item[0]" class="btn btn-sm" :class="investmentRange === item[0] ? 'btn-primary' : 'btn-outline-primary'" @click="investmentRange = item[0]">{{ item[1] }}</button></div></div>
        <div v-if="investmentTotal" class="plan-investment-body">
          <div class="plan-investment-donut-wrap"><div class="plan-investment-donut" :style="donutStyle"></div><div class="plan-investment-center"><strong>{{ investmentTotal }}</strong><span>分钟</span></div></div>
          <div class="plan-investment-legend"><div v-for="(item,index) in investments" :key="item.planId"><i :style="{background: colors[index % colors.length]}"></i><span>{{ item.label }}</span><strong>{{ item.minutes }} 分钟（{{ item.percent }}%）</strong></div></div>
        </div>
        <p v-else class="plan-investment-empty">暂无学习投入数据，新增学习记录后将自动生成。</p>
      </div>
    </section>

    <section class="content-card">
      <div class="card-title-row"><h2>最近学习记录</h2><span class="muted-text">任务完成率 {{ summary.completionRate }}%</span></div>
      <div class="stack-list"><div v-for="record in (dashboard.recentRecords || [])" :key="record.id" class="list-row"><div><h3>{{ recordLabel(record) }}</h3><p>{{ record.content || '未填写内容' }}</p></div><span>{{ record.studyDate }} · {{ record.duration || 0 }} 分钟</span></div><div v-if="!(dashboard.recentRecords || []).length" class="list-row"><p>暂无学习记录。</p></div></div>
    </section>
  </main>
</template>