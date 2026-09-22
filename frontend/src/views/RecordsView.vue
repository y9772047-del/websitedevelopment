<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { buildRecordAiRequest, prepareRecord } from '../workflow.js';

const props = defineProps({ api: { type: Function, required: true } });
const emit = defineEmits(['session-expired', 'busy-change']);
const tasks = ref([]), plans = ref([]), records = ref([]);
const busy = ref(false), aiBusy = ref(false), error = ref(''), message = ref('');
const editingId = ref(null), suggestions = ref([]), aiTip = ref('填写学习内容和问题与反思后生成改进计划。');
const today = () => new Date().toLocaleDateString('en-CA');
const emptyRecord = () => ({ taskId: '', studyDate: today(), duration: 0, content: '', completionStatus: '部分完成', problems: '', improvementPlan: '' });
const form = reactive(emptyRecord());
const taskMap = computed(() => Object.fromEntries(tasks.value.map(task => [task.id, task])));
const planMap = computed(() => Object.fromEntries(plans.value.map(plan => [plan.id, plan])));
function taskLabel(task) { return (planMap.value[task.planId]?.planName || '未知计划') + ' / ' + task.taskName; }
function resetForm() { editingId.value = null; Object.assign(form, emptyRecord()); suggestions.value = []; aiTip.value = '填写学习内容和问题与反思后生成改进计划。'; }
async function run(action) {
  if (busy.value) return;
  busy.value = true; emit('busy-change', true); error.value = ''; message.value = '';
  try { await action(); }
  catch (failure) { if (failure.status === 401) emit('session-expired'); else error.value = failure.message; }
  finally { busy.value = false; emit('busy-change', false); }
}
async function loadAll() { [plans.value, tasks.value, records.value] = await Promise.all([props.api.get('/plans'), props.api.get('/tasks'), props.api.get('/records')]); }
async function refreshRecords() { records.value = await props.api.get('/records'); }
function edit(record) { editingId.value = record.id; Object.assign(form, emptyRecord(), record, { taskId: String(record.taskId), studyDate: record.studyDate || today() }); suggestions.value = []; window.scrollTo({ top: 0, behavior: 'smooth' }); }
function save() { return run(async () => { const payload = prepareRecord(form); if (editingId.value) await props.api.put('/records/' + editingId.value, payload); else await props.api.post('/records', payload); message.value = editingId.value ? '学习记录已修改' : '学习记录已保存'; resetForm(); records.value = await props.api.get('/records'); }); }
function removeRecord(record) { if (!window.confirm('删除这条学习记录？此操作不可撤销。')) return; return run(async () => { await props.api.delete('/records/' + record.id); if (editingId.value === record.id) resetForm(); message.value = '学习记录已删除'; records.value = await props.api.get('/records'); }); }
async function generateAi() {
  if (busy.value || aiBusy.value) return;
  const payload = buildRecordAiRequest(form, tasks.value, plans.value);
  if (!payload.taskName) { aiTip.value = '请先选择学习任务。'; return; }
  if (!payload.studyContent && !payload.problems) { aiTip.value = '请先填写学习内容，或写下问题与反思。'; return; }
  aiBusy.value = true; emit('busy-change', true); error.value = ''; suggestions.value = []; aiTip.value = '正在根据本次学习记录生成改进计划。';
  try { const result = await props.api.post('/ai/generate-improvement', payload); form.improvementPlan = result?.improvementPlan || ''; suggestions.value = Array.isArray(result?.suggestedActions) ? result.suggestedActions.filter(Boolean) : []; aiTip.value = '已生成（来源：' + (result?.generationSourceLabel || '未知来源') + '），请检查后保存。'; }
  catch (failure) { if (failure.status === 401) emit('session-expired'); else error.value = failure.message; }
  finally { aiBusy.value = false; emit('busy-change', false); }
}
function compact(value) { const text = String(value || ''); return text.length > 30 ? text.slice(0, 30) + '…' : text || '—'; }
defineExpose({ refresh: () => run(loadAll) });
onMounted(() => run(loadAll));
</script>
<template>
  <main class="container-fluid app-shell">
    <header class="page-heading"><div><span class="soft-label">Records</span><h1 class="page-title">学习记录管理</h1></div></header>
    <p v-if="error" class="alert alert-danger" role="alert">{{ error }}</p><p v-if="message" class="alert alert-success" role="status">{{ message }}</p>
    <section class="content-card mb-4"><h2>{{ editingId ? '编辑学习记录' : '记录表单' }}</h2>
      <form class="row g-3" @submit.prevent="save"><fieldset class="contents-fieldset" :disabled="busy || aiBusy"><div class="row g-3">
        <div class="col-md-3"><label class="form-label">学习任务</label><select v-model="form.taskId" class="form-select" required><option value="">请选择学习任务</option><option v-for="task in tasks" :key="task.id" :value="String(task.id)">{{ taskLabel(task) }}</option></select></div>
        <div class="col-md-3"><label class="form-label">学习日期</label><input v-model="form.studyDate" class="form-control" type="date" required></div>
        <div class="col-md-3"><label class="form-label">学习时长（分钟）</label><input v-model="form.duration" class="form-control" type="number" min="0"></div>
        <div class="col-md-3"><label class="form-label">完成情况</label><select v-model="form.completionStatus" class="form-select"><option>已完成</option><option>部分完成</option><option>未完成</option></select></div>
        <div class="col-md-6"><label class="form-label">学习内容</label><textarea v-model="form.content" class="form-control" rows="4" required></textarea></div>
        <div class="col-md-6"><label class="form-label">问题与反思</label><textarea v-model="form.problems" class="form-control" rows="4"></textarea></div>
        <div class="col-12"><label class="form-label">改进计划</label><textarea v-model="form.improvementPlan" class="form-control" rows="4"></textarea></div>
        <div class="col-12 d-flex align-items-center gap-2 flex-wrap"><button class="btn btn-primary">{{ busy ? '保存中…' : '保存记录' }}</button><button class="btn btn-outline-primary" type="button" @click="resetForm">清空</button><button class="btn btn-outline-primary" type="button" @click="generateAi">{{ aiBusy ? '生成中…' : 'AI 生成改进计划' }}</button><span class="muted-text">{{ aiTip }}</span></div>
        <div v-if="suggestions.length" class="col-12"><div class="ai-suggestion-box"><strong>行动建议</strong><ul><li v-for="item in suggestions" :key="item">{{ item }}</li></ul></div></div>
      </div></fieldset></form>
    </section>
    <section class="content-card"><div class="card-title-row"><h2>记录列表</h2><button class="btn btn-outline-primary btn-sm" :disabled="busy" @click="run(refreshRecords)">刷新</button></div>
      <div class="table-responsive"><table class="table app-table align-middle"><thead><tr><th>任务</th><th>日期</th><th>时长</th><th>内容</th><th>问题与反思</th><th>改进计划</th><th>完成情况</th><th>操作</th></tr></thead><tbody>
        <tr v-for="record in records" :key="record.id"><td>{{ taskMap[record.taskId] ? taskLabel(taskMap[record.taskId]) : '未知任务' }}</td><td>{{ record.studyDate }}</td><td>{{ record.duration || 0 }} 分钟</td><td :title="record.content">{{ compact(record.content) }}</td><td :title="record.problems">{{ compact(record.problems) }}</td><td :title="record.improvementPlan">{{ compact(record.improvementPlan) }}</td><td>{{ record.completionStatus }}</td><td><button class="btn btn-sm btn-primary me-2" :disabled="busy" @click="edit(record)">编辑</button><button class="btn btn-sm btn-outline-primary" :disabled="busy" @click="removeRecord(record)">删除</button></td></tr>
        <tr v-if="!records.length"><td colspan="8">暂无学习记录</td></tr>
      </tbody></table></div>
    </section>
  </main>
</template>