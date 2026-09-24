<script setup>
import { computed, onMounted, ref } from 'vue';
import { buildCalendarMonth } from '../insights.js';

const props = defineProps({ api: { type: Function, required: true } });
const emit = defineEmits(['session-expired', 'busy-change']);
const now = new Date();
const year = ref(now.getFullYear()), month = ref(now.getMonth() + 1), selectedDate = ref(now.toLocaleDateString('en-CA'));
const records = ref([]), tasks = ref([]), nodes = ref([]);
const busy = ref(false), error = ref('');
const taskMap = computed(() => Object.fromEntries(tasks.value.map(task => [task.id, task])));
const days = computed(() => buildCalendarMonth(year.value, month.value, records.value, nodes.value));
const leading = computed(() => (new Date(Date.UTC(year.value, month.value - 1, 1)).getUTCDay() + 6) % 7);
const selected = computed(() => days.value.find(day => day.date === selectedDate.value) || { records: [], nodes: [] });
function recordType(record) { return record.completionStatus === '已完成' ? 'done' : record.completionStatus === '未完成' ? 'overdue' : 'doing'; }
function taskName(id) { return taskMap.value[id]?.taskName || '未知任务'; }
function nodeTime(node) { if (!node.startTime) return '全天'; return String(node.startTime).slice(0,5) + (node.endTime ? ' - ' + String(node.endTime).slice(0,5) : ''); }
function nodeDateRange(node) { const start = String(node.startDate || '').slice(5); const end = String(node.endDate || '').slice(5); return end && end !== start ? start + ' - ' + end : start; }
function typeClass(value) { return ({考试:'node-type-exam',截止:'node-type-deadline',比赛:'node-type-contest',汇报:'node-type-report'})[value] || 'node-type-other'; }
function recordBadge(value) { return value === '已完成' ? 'status-done' : value === '未完成' ? 'status-delay' : 'status-doing'; }
function importanceClass(value) { return value === '紧急' ? 'node-urgent' : value === '重要' ? 'node-important' : 'node-normal'; }
function keepSelection() {
  const prefix = year.value + '-' + String(month.value).padStart(2,'0') + '-';
  if (!selectedDate.value.startsWith(prefix)) selectedDate.value = prefix + '01';
}
async function run(action) {
  if (busy.value) return;
  busy.value = true; emit('busy-change', true); error.value = '';
  try { await action(); }
  catch (failure) { if (failure.status === 401) emit('session-expired'); else error.value = failure.message; }
  finally { busy.value = false; emit('busy-change', false); }
}
async function loadAll() {
  [records.value, tasks.value, nodes.value] = await Promise.all([
    props.api.get('/records'), props.api.get('/tasks'),
    props.api.get('/important-nodes', { params: { year: year.value } })
  ]);
  keepSelection();
}
function changeMonth(offset) {
  const date = new Date(Date.UTC(year.value, month.value - 1 + offset, 1));
  year.value = date.getUTCFullYear(); month.value = date.getUTCMonth() + 1;
  return run(loadAll);
}
function goToday() {
  const date = new Date(); year.value = date.getFullYear(); month.value = date.getMonth() + 1;
  selectedDate.value = date.toLocaleDateString('en-CA');
  return run(loadAll);
}
defineExpose({ refresh: () => run(loadAll) });
onMounted(() => run(loadAll));
</script>

<template>
  <main class="container-fluid app-shell">
    <header class="page-heading">
      <div><span class="soft-label">Calendar</span><h1 class="page-title">学习日历</h1><p class="muted-text mt-2 mb-0">按学习记录日期查看每天真实完成的学习情况。</p></div>
      <div class="calendar-toolbar"><button class="btn btn-outline-primary btn-sm" :disabled="busy" @click="changeMonth(-1)">上个月</button><button class="btn btn-primary btn-sm" :disabled="busy" @click="goToday">本月</button><button class="btn btn-outline-primary btn-sm" :disabled="busy" @click="changeMonth(1)">下个月</button></div>
    </header>
    <p v-if="error" class="alert alert-danger" role="alert">{{ error }}</p>
    <section class="row g-4">
      <div class="col-lg-8"><div class="content-card">
        <div class="card-title-row"><h2>{{ year }} 年 {{ month }} 月学习日历</h2><div class="calendar-legend"><span><i class="legend-dot calendar-dot-done"></i>已完成</span><span><i class="legend-dot calendar-dot-doing"></i>部分完成</span><span><i class="legend-dot calendar-dot-overdue"></i>未完成</span><span><i class="legend-dot calendar-dot-node"></i>重要节点</span></div></div>
        <div class="calendar-week-row"><span v-for="label in ['周一','周二','周三','周四','周五','周六','周日']" :key="label">{{ label }}</span></div>
        <div class="calendar-grid">
          <div v-for="index in leading" :key="'empty-' + index" class="calendar-cell calendar-cell-empty"></div>
          <button v-for="day in days" :key="day.date" type="button" class="calendar-cell" :class="{'is-today': day.date === new Date().toLocaleDateString('en-CA'), 'is-selected': day.date === selectedDate}" @click="selectedDate = day.date">
            <span class="calendar-day-number">{{ day.day }}</span>
            <div class="calendar-task-preview">
              <div v-for="record in day.records.slice(0,2)" :key="'r'+record.id" class="calendar-task-pill" :class="'calendar-task-' + recordType(record)" :title="taskName(record.taskId)">{{ taskName(record.taskId) }} · {{ record.duration || 0 }}分钟</div>
              <div v-for="node in day.nodes.slice(0,2)" :key="'n'+node.id" class="calendar-node-pill" :class="importanceClass(node.importance)" :title="node.title">{{ node.type }} · {{ node.title }}</div>
              <span v-if="Math.max(day.records.length - 2, 0) + Math.max(day.nodes.length - 2, 0) > 0" class="calendar-more">+{{ Math.max(day.records.length - 2, 0) + Math.max(day.nodes.length - 2, 0) }} 条</span>
            </div>
          </button>
        </div>
      </div></div>
      <div class="col-lg-4"><div class="content-card calendar-day-panel"><h2>{{ selectedDate }} 学习详情</h2><div class="stack-list">
        <section v-if="selected.nodes.length" class="calendar-detail-section"><span class="soft-label mb-2">重要节点</span><div class="stack-list"><div v-for="node in selected.nodes" :key="node.id" class="list-row calendar-day-task calendar-day-node"><div><h3>{{ node.title }}</h3><p>{{ nodeDateRange(node) }} · {{ nodeTime(node) }} · {{ node.type }} · {{ node.importance || '重要' }}</p><p v-if="node.description">{{ node.description }}</p></div><div class="calendar-task-tags"><span class="badge important-node-type" :class="typeClass(node.type)">{{ node.type }}</span><span class="badge important-node-importance" :class="importanceClass(node.importance)">{{ node.importance || '重要' }}</span></div></div></div></section>
        <section v-if="selected.records.length" class="calendar-detail-section"><span class="soft-label mb-2">学习记录</span><div class="stack-list"><div v-for="record in selected.records" :key="record.id" class="list-row calendar-day-task"><div><h3>{{ taskName(record.taskId) }}</h3><p>{{ record.duration || 0 }} 分钟 · {{ record.completionStatus }}</p><p v-if="record.content">{{ record.content }}</p><p v-if="record.problems">问题与反思：{{ record.problems }}</p></div><div class="calendar-task-tags"><span class="badge" :class="recordBadge(record.completionStatus)">{{ record.completionStatus }}</span></div></div></div></section>
        <div v-if="!selected.nodes.length && !selected.records.length" class="list-row"><p>当天暂无学习记录或重要节点。</p></div>
      </div></div></div>
    </section>
  </main>
</template>