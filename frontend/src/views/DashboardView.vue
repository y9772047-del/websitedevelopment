<script setup>
import { computed, onMounted, ref } from 'vue';
import { buildDashboardHeatmap, buildDashboardReminders, calculatePlanInvestment, summarizeTasks } from '../insights.js';

const props = defineProps({ api: { type: Function, required: true } });
const emit = defineEmits(['session-expired', 'busy-change']);
const dashboard = ref({}), statistics = ref({}), plans = ref([]), tasks = ref([]), records = ref([]);
const busy = ref(false), error = ref(''), investmentRange = ref('30'), heatmapFilter = ref('全部');
const heatmapFilters = ['全部', '课业完成型', '成绩提升型', '成长拓展型', '综合提升型'];
const colors = ['#f05f7d','#5fbf8f','#f4a62a','#5c8df6','#9b70d9','#42b8c6','#e66bb5','#7a9a3f'];
const today = () => new Date().toLocaleDateString('en-CA');
const summary = computed(() => summarizeTasks(tasks.value, today()));
const taskMap = computed(() => Object.fromEntries(tasks.value.map(item => [item.id, item])));
const planMap = computed(() => Object.fromEntries(plans.value.map(item => [item.id, item])));
const todayTasks = computed(() => tasks.value.filter(task => task.status !== '已完成' && task.deadline === today()));
const overdueTasks = computed(() => tasks.value.filter(task => task.status !== '已完成' && task.deadline && task.deadline < today()));
const latestImprovement = computed(() => (dashboard.value.recentRecords || records.value).find(record => String(record.improvementPlan || '').trim()));
const reminders = computed(() => buildDashboardReminders(tasks.value, today()));
const heatmap = computed(() => buildDashboardHeatmap(records.value, tasks.value, heatmapFilter.value, today()));
const heatmapMinutes = computed(() => heatmap.value.reduce((sum, cell) => sum + cell.minutes, 0));
function rangeStart(days) {
  if (!days) return null;
  const date = new Date(); date.setDate(date.getDate() - days + 1);
  return date.toLocaleDateString('en-CA');
}
const investments = computed(() => calculatePlanInvestment(records.value, tasks.value, plans.value, rangeStart(investmentRange.value === 'all' ? null : Number(investmentRange.value))));
const visibleInvestments = computed(() => {
  if (investments.value.length <= 5) return investments.value;
  const rest = investments.value.slice(5);
  return [...investments.value.slice(0, 5), {
    planId: 'other', label: '其他计划', minutes: rest.reduce((sum, item) => sum + item.minutes, 0),
    percent: rest.reduce((sum, item) => sum + item.percent, 0)
  }];
});
const investmentTotal = computed(() => visibleInvestments.value.reduce((sum, item) => sum + item.minutes, 0));
const donutStyle = computed(() => {
  if (!investmentTotal.value) return { background: 'var(--primary-soft)' };
  let cursor = 0;
  return { background: 'conic-gradient(' + visibleInvestments.value.map((item, index) => {
    const start = cursor; cursor += item.minutes / investmentTotal.value * 100;
    return colors[index % colors.length] + ' ' + start + '% ' + cursor + '%';
  }).join(',') + ')' };
});
const statusColors = { '已完成':'var(--success)', '进行中':'var(--primary)', '未完成':'var(--warning)', '延期':'var(--danger)' };
const priorityColors = { Q0:'var(--danger)', Q1:'var(--primary)', Q2:'#5b8def', Q3:'#64748b' };
function chartRows(counts, keys, palette) {
  const values = keys.map(label => ({ label, value: counts[label] || 0, color: palette[label] }));
  const maximum = Math.max(1, ...values.map(row => row.value));
  return values.filter(row => row.value > 0).map(row => ({ ...row, width: Math.max(8, Math.round(row.value * 100 / maximum)) }));
}
const statusRows = computed(() => chartRows(summary.value.statusCounts, ['已完成','进行中','未完成','延期'], statusColors));
const priorityRows = computed(() => chartRows(summary.value.priorityCounts, ['Q0','Q1','Q2','Q3'], priorityColors));
function compact(value, limit = 78) { const text = String(value || ''); return text.length > limit ? text.slice(0, limit) + '…' : text; }
function taskName(id) { return taskMap.value[id]?.taskName || '未知任务'; }
function planName(id) { return planMap.value[id]?.planName || '未关联计划'; }
function hours(minutes) { return (Math.round(Number(minutes || 0) / 6) / 10).toFixed(1); }
function heatmapTitle(cell) {
  if (!cell.inRange) return '范围外日期';
  return cell.date + ' · ' + (cell.count ? cell.minutes + ' 分钟 · ' + cell.count + ' 条学习记录' : '暂无学习记录') + ' · ' + heatmapFilter.value;
}
async function run(action) {
  if (busy.value) return;
  busy.value = true; emit('busy-change', true); error.value = '';
  try { await action(); }
  catch (failure) { if (failure.status === 401) emit('session-expired'); else error.value = failure.message; }
  finally { busy.value = false; emit('busy-change', false); }
}
async function loadAll() {
  [dashboard.value, statistics.value, plans.value, tasks.value, records.value] = await Promise.all([
    props.api.get('/dashboard'), props.api.get('/statistics'), props.api.get('/plans'), props.api.get('/tasks'), props.api.get('/records')
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
        ['计划总数', dashboard.planCount || 0], ['任务总数', dashboard.taskCount || 0],
        ['已完成任务', dashboard.completedTaskCount || 0], ['逾期任务', dashboard.overdueTaskCount || 0]
      ]" :key="item[0]" class="col-sm-6 col-xl-3"><div class="metric-card"><span>{{ item[0] }}</span><strong>{{ item[1] }}</strong></div></div>
    </section>

    <section class="insight-grid mb-4">
      <div class="dashboard-focus-card"><span>今日学习焦点</span><h2>{{ todayTasks.length ? '今天有任务要推进' : '今天暂无截止任务' }}</h2><strong>{{ todayTasks.length }}</strong><p>{{ todayTasks.length ? compact(todayTasks.map(task => task.taskName).join('、'), 48) : '可以复盘最近记录，或提前推进高优先级任务。' }}</p></div>
      <div class="dashboard-focus-card dashboard-risk-card"><span>风险提醒</span><h2>{{ overdueTasks.length ? '存在延期任务' : '暂无延期风险' }}</h2><strong>{{ overdueTasks.length }}</strong><div class="dashboard-focus-list"><div v-for="task in overdueTasks.slice(0,2)" :key="task.id" class="dashboard-focus-item"><b>{{ compact(task.taskName,18) }}</b><span>{{ task.deadline }}</span></div><div v-if="!overdueTasks.length" class="dashboard-focus-item"><b>节奏正常</b><span>继续保持</span></div></div></div>
      <div class="dashboard-focus-card"><span>计划执行进度</span><h2>任务完成率</h2><strong>{{ summary.completionRate }}%</strong><p>已完成 {{ summary.completed }} / {{ summary.total }} 个任务，优先处理 Q0 和 Q1 任务。</p></div>
      <div class="dashboard-focus-card"><span>最近改进建议</span><h2>{{ latestImprovement ? taskName(latestImprovement.taskId) : '暂无 AI 改进记录' }}</h2><p>{{ latestImprovement ? compact(latestImprovement.improvementPlan) : '在学习记录中填写问题与反思后，可以生成并保存改进计划。' }}</p></div>
    </section>

    <section class="content-card mb-4"><div class="card-title-row"><div><h2>学习提醒</h2><p class="muted-text mb-0">今日待办、即将截止和逾期任务。</p></div></div>
      <div class="stack-list"><div v-for="row in reminders" :key="row.task.id" class="list-row"><div><h3>{{ row.task.taskName }}</h3><p>{{ planName(row.task.planId) }} · {{ row.task.deadline }}</p></div><span class="badge" :class="row.className">{{ row.label }}</span></div><div v-if="!reminders.length" class="list-row"><p>暂无今日待办、临近截止或逾期任务。</p></div></div>
    </section>

    <section class="dashboard-chart-grid mb-4">
      <div class="content-card chart-card category-heatmap"><div class="card-title-row"><div><h2>分类学习热力图</h2><p class="muted-text mb-0">{{ heatmapFilter }} · 近 180 天累计 {{ hours(heatmapMinutes) }} 小时</p></div><div class="category-heatmap-filters"><button v-for="filter in heatmapFilters" :key="filter" class="heatmap-filter-btn" :class="{ active: heatmapFilter === filter }" @click="heatmapFilter = filter">{{ filter }}</button></div></div>
        <div class="heatmap-board"><div class="heatmap-week-labels"><span v-for="label in ['周一','周二','周三','周四','周五','周六','周日']" :key="label">{{ label }}</span></div><div class="heatmap-scroll"><div class="heatmap-grid"><span v-for="cell in heatmap" :key="cell.date" class="heatmap-cell" :class="['heatmap-level-' + cell.level, { 'out-of-range': !cell.inRange }]" :title="heatmapTitle(cell)" :aria-label="heatmapTitle(cell)"></span></div></div></div>
        <div class="heatmap-legend"><span>少</span><i v-for="level in [0,1,2,3,4,5]" :key="level" class="heatmap-swatch" :class="'heatmap-level-' + level"></i><span>多</span></div>
      </div>
      <div class="content-card chart-card plan-investment-card"><div class="card-title-row"><div><h2>学习计划投入分布</h2><p class="muted-text mb-0">按学习记录统计当前时间范围内的累计学习时长占比。</p></div><div class="plan-investment-range-buttons"><button v-for="item in [['30','最近30天'],['180','最近180天'],['all','全部']]" :key="item[0]" class="plan-investment-range-btn" :class="{ active: investmentRange === item[0] }" @click="investmentRange = item[0]">{{ item[1] }}</button></div></div>
        <div class="plan-investment-body"><div class="plan-investment-donut-wrap"><div class="plan-investment-donut" :style="donutStyle"></div><div class="plan-investment-center"><strong>{{ hours(investmentTotal) }} 小时</strong><span>总学习时长</span></div></div><div><div class="plan-investment-summary"><span>{{ investmentRange === 'all' ? '全部' : '最近' + investmentRange + '天' }}</span><small>{{ visibleInvestments.length }} 个计划</small></div><div class="plan-investment-legend"><div v-for="(item,index) in visibleInvestments" :key="item.planId" class="plan-investment-legend-item"><i :style="{ background: colors[index % colors.length] }"></i><span>{{ item.label }}</span><b class="plan-investment-progress"><u :style="{ width: item.percent + '%', background: colors[index % colors.length] }"></u></b><strong>{{ item.minutes }} 分钟</strong><em>{{ Math.round(item.percent * 10) / 10 }}%</em></div></div></div></div>
        <p v-if="!investmentTotal" class="plan-investment-empty">暂无学习投入数据，新增学习记录后将自动生成计划投入分布。</p>
      </div>
    </section>

    <section class="row g-4 mb-4"><div class="col-lg-6"><div class="content-card"><h2>最近任务</h2><div class="stack-list"><div v-for="task in (dashboard.recentTasks || [])" :key="task.id" class="list-row"><div><h3>{{ task.taskName }}</h3><p>{{ planName(task.planId) }} · {{ task.deadline || '' }}</p></div><span class="badge" :class="task.status === '已完成' ? 'status-done' : 'status-doing'">{{ task.status }}</span></div><div v-if="!(dashboard.recentTasks || []).length" class="list-row"><p>暂无任务。</p></div></div></div></div>
      <div class="col-lg-6"><div class="content-card"><h2>最近学习记录</h2><div class="stack-list"><div v-for="record in (dashboard.recentRecords || [])" :key="record.id" class="list-row"><div><h3>{{ taskName(record.taskId) }}</h3><p>{{ record.studyDate }} · {{ record.duration || 0 }} 分钟</p><p v-if="record.improvementPlan">改进计划：{{ compact(record.improvementPlan,54) }}</p></div></div><div v-if="!(dashboard.recentRecords || []).length" class="list-row"><p>暂无学习记录。</p></div></div></div></div>
    </section>

    <section class="content-card"><div class="card-title-row"><div><h2>学习统计</h2><p class="muted-text mb-0">计划、任务和学习投入概览。</p></div></div>
      <div class="row g-3 mb-4"><div v-for="item in [['计划数量', statistics.planCount || 0],['任务数量',statistics.taskCount || 0],['已完成任务',statistics.completedTaskCount || 0],['累计学习时长',(statistics.totalDuration || 0) + ' 分钟']]" :key="item[0]" class="col-sm-6 col-xl-3"><div class="metric-card"><span>{{ item[0] }}</span><strong>{{ item[1] }}</strong></div></div></div>
      <div class="row g-4"><div class="col-lg-4"><div class="content-card stat-summary-card"><h2>任务完成率</h2><strong>{{ statistics.completionRate || 0 }}%</strong><p class="muted-text">已完成 {{ statistics.completedTaskCount || 0 }} / {{ statistics.taskCount || 0 }}</p><div class="progress stat-progress"><div class="progress-bar" :style="{ width: (statistics.completionRate || 0) + '%' }">{{ statistics.completionRate || 0 }}%</div></div></div></div>
        <div v-for="chart in [{ title:'任务状态分布', rows:statusRows }, { title:'优先级分布', rows:priorityRows }]" :key="chart.title" class="col-lg-4"><div class="content-card"><h2>{{ chart.title }}</h2><div class="mini-chart"><div v-for="row in chart.rows" :key="row.label" class="mini-chart-row"><span class="mini-chart-label">{{ row.label }}</span><span class="mini-chart-track"><i class="mini-chart-bar" :style="{ width: row.width + '%', background: row.color }"></i></span><span class="mini-chart-value">{{ row.value }}</span></div><p v-if="!chart.rows.length" class="mini-chart-empty">暂无可视化数据</p></div></div></div>
      </div>
    </section>
  </main>
</template>
