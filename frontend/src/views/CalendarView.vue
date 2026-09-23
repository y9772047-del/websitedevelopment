<script setup>
import { computed, onMounted, ref } from 'vue';
import { buildCalendarMonth } from '../insights.js';

const props = defineProps({ api: { type: Function, required: true } });
const emit = defineEmits(['session-expired', 'busy-change']);
const now = new Date();
const year = ref(now.getFullYear()), month = ref(now.getMonth() + 1), selectedDate = ref(now.toISOString().slice(0,10));
const records = ref([]), tasks = ref([]), nodes = ref([]), deadlines = ref([]);
const busy = ref(false), error = ref('');
const taskMap = computed(() => Object.fromEntries(tasks.value.map(task => [task.id, task])));
const days = computed(() => buildCalendarMonth(year.value, month.value, records.value, nodes.value));
const leading = computed(() => (new Date(Date.UTC(year.value, month.value - 1, 1)).getUTCDay() + 6) % 7);
const deadlineMap = computed(() => {
  const map = {};
  deadlines.value.forEach(task => { if (!map[task.deadline]) map[task.deadline] = []; map[task.deadline].push(task); });
  return map;
});
const selected = computed(() => days.value.find(day => day.date === selectedDate.value) || { records: [], nodes: [] });
const selectedDeadlines = computed(() => deadlineMap.value[selectedDate.value] || []);
function recordType(record) { return record.completionStatus === '已完成' ? 'done' : record.completionStatus === '未完成' ? 'overdue' : 'doing'; }
function taskName(id) { return taskMap.value[id]?.taskName || '未知任务'; }
function nodeTime(node) { if (!node.startTime) return '全天'; return String(node.startTime).slice(0,5) + (node.endTime ? ' - ' + String(node.endTime).slice(0,5) : ''); }
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
  [records.value, tasks.value, nodes.value, deadlines.value] = await Promise.all([
    props.api.get('/records'), props.api.get('/tasks'),
    props.api.get('/important-nodes', { params: { year: year.value } }),
    props.api.get('/calendar', { params: { year: year.value, month: month.value } })
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
  selectedDate.value = date.toISOString().slice(0,10);
  return run(loadAll);
}
defineExpose({ refresh: () => run(loadAll) });
onMounted(() => run(loadAll));
</script>

<template>
  <main class="container-fluid app-shell">
    <header class="page-heading">
      <div><span class="soft-label">Calendar</span><h1 class="page-title">学习日历</h1><p class="muted-text mt-2 mb-0">按学习记录、任务截止日期和重要节点查看每日安排。</p></div>
      <div class="calendar-toolbar"><button class="btn btn-outline-primary btn-sm" :disabled="busy" @click="changeMonth(-1)">上个月</button><button class="btn btn-primary btn-sm" :disabled="busy" @click="goToday">本月</button><button class="btn btn-outline-primary btn-sm" :disabled="busy" @click="changeMonth(1)">下个月</button></div>
    </header>
    <p v-if="error" class="alert alert-danger" role="alert">{{ error }}</p>
    <section class="row g-4">
      <div class="col-lg-8"><div class="content-card">
        <div class="card-title-row"><h2>{{ year }} 年 {{ month }} 月学习日历</h2><div class="calendar-legend"><span><i class="legend-dot calendar-dot-done"></i>已完成</span><span><i class="legend-dot calendar-dot-doing"></i>部分完成</span><span><i class="legend-dot calendar-dot-overdue"></i>未完成/截止</span><span><i class="legend-dot calendar-dot-node"></i>重要节点</span></div></div>
        <div class="calendar-week-row"><span v-for="label in ['周一','周二','周三','周四','周五','周六','周日']" :key="label">{{ label }}</span></div>
        <div class="calendar-grid">
          <div v-for="index in leading" :key="'empty-' + index" class="calendar-cell calendar-cell-empty"></div>
          <button v-for="day in days" :key="day.date" type="button" class="calendar-cell" :class="{'is-today': day.date === new Date().toISOString().slice(0,10), 'is-selected': day.date === selectedDate}" @click="selectedDate = day.date">
            <span class="calendar-day-number">{{ day.day }}</span>
            <div class="calendar-task-preview">
              <div v-for="record in day.records.slice(0,2)" :key="'r'+record.id" class="calendar-task-pill" :class="'calendar-task-' + recordType(record)" :title="taskName(record.taskId)">{{ taskName(record.taskId) }}</div>
              <div v-for="node in day.nodes.slice(0,2)" :key="'n'+node.id" class="calendar-node-pill" :class="importanceClass(node.importance)" :title="node.title">{{ node.type }} · {{ node.title }}</div>
              <div v-for="task in (deadlineMap[day.date] || []).slice(0,1)" :key="'t'+task.id" class="calendar-task-pill calendar-task-overdue" :title="task.taskName">截止 · {{ task.taskName }}</div>
            </div>
          </button>
        </div>
      </div></div>
      <div class="col-lg-4"><div class="content-card calendar-day-panel"><h2>{{ selectedDate }} 当天详情</h2>
        <section class="calendar-detail-section"><h3>学习记录</h3><div v-for="record in selected.records" :key="record.id" class="list-row calendar-day-task"><div><h3>{{ taskName(record.taskId) }}</h3><p>{{ record.content || '未填写内容' }}</p></div><span>{{ record.duration || 0 }} 分钟 · {{ record.completionStatus }}</span></div><p v-if="!selected.records.length" class="muted-text">暂无学习记录。</p></section>
        <section class="calendar-detail-section"><h3>重要节点</h3><div v-for="node in selected.nodes" :key="node.id" class="list-row calendar-day-task calendar-day-node"><div><h3>{{ node.title }}</h3><p>{{ node.type }} · {{ nodeTime(node) }}</p></div><span class="badge important-node-importance" :class="importanceClass(node.importance)">{{ node.importance }}</span></div><p v-if="!selected.nodes.length" class="muted-text">暂无重要节点。</p></section>
        <section class="calendar-detail-section"><h3>任务截止</h3><div v-for="task in selectedDeadlines" :key="task.id" class="list-row calendar-day-task"><div><h3>{{ task.taskName }}</h3><p>{{ task.category || '未分类' }} · {{ task.priority || 'Q2' }}</p></div><span>{{ task.status }}</span></div><p v-if="!selectedDeadlines.length" class="muted-text">暂无截止任务。</p></section>
      </div></div>
    </section>
  </main>
</template>