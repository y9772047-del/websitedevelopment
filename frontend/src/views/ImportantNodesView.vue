<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { parseImportantNodeBatch, prepareImportantNode, prepareKeywordRule } from '../insights.js';

const props = defineProps({ api: { type: Function, required: true } });
const emit = defineEmits(['session-expired', 'busy-change']);
const currentYear = new Date().getFullYear();
const year = ref(currentYear), nodes = ref([]), rules = ref([]), plans = ref([]), tasks = ref([]);
const busy = ref(false), error = ref(''), message = ref(''), editingNodeId = ref(null), editingRuleId = ref(null), batchText = ref('');
const years = Array.from({length: 7}, (_, index) => currentYear - 3 + index);
const emptyNode = () => ({ title:'', type:'考试', startDate:'', startTime:'', endDate:'', endTime:'', planId:'', taskId:'', description:'' });
const emptyRule = () => ({ keyword:'', nodeType:'', effect:'SCORE', scoreDelta:10, enabled:true });
const nodeForm = reactive(emptyNode()), ruleForm = reactive(emptyRule());
const planMap = computed(() => Object.fromEntries(plans.value.map(item => [item.id, item.planName])));
const taskMap = computed(() => Object.fromEntries(tasks.value.map(item => [item.id, item.taskName])));
const taskOptions = computed(() => nodeForm.planId ? tasks.value.filter(task => String(task.planId) === String(nodeForm.planId)) : tasks.value);
const sortedNodes = computed(() => [...nodes.value].sort((a,b) => ((a.startDate || '') + (a.startTime || '')).localeCompare((b.startDate || '') + (b.startTime || ''))));
function resetNode() { editingNodeId.value = null; Object.assign(nodeForm, emptyNode()); }
function resetRule() { editingRuleId.value = null; Object.assign(ruleForm, emptyRule()); }
function importanceClass(value) { return value === '紧急' ? 'node-urgent' : value === '重要' ? 'node-important' : 'node-normal'; }
function typeClass(value) { return ({考试:'node-type-exam',截止:'node-type-deadline',比赛:'node-type-contest',汇报:'node-type-report'})[value] || 'node-type-other'; }
function dateRange(node) { return node.startDate === node.endDate ? node.startDate : node.startDate + ' 至 ' + node.endDate; }
function timeRange(node) { if (!node.startTime) return '全天'; return String(node.startTime).slice(0,5) + (node.endTime ? ' - ' + String(node.endTime).slice(0,5) : ''); }
function relation(node) {
  const parts = [];
  if (node.planId) parts.push(planMap.value[node.planId] || '未知计划');
  if (node.taskId) parts.push(taskMap.value[node.taskId] || '未知任务');
  return parts.join(' / ');
}
function effectLabel(rule) { return rule.effect === 'URGENT' ? '直接紧急' : rule.effect === 'MIN_IMPORTANT' ? '至少重要' : '加分'; }
async function run(action) {
  if (busy.value) return;
  busy.value = true; emit('busy-change', true); error.value = ''; message.value = '';
  try { await action(); }
  catch (failure) { if (failure.status === 401) emit('session-expired'); else error.value = failure.message; }
  finally { busy.value = false; emit('busy-change', false); }
}
async function loadNodes() { nodes.value = await props.api.get('/important-nodes', { params: { year: year.value } }); }
async function loadAll() {
  [plans.value, tasks.value, rules.value] = await Promise.all([
    props.api.get('/plans'), props.api.get('/tasks'), props.api.get('/important-node-keyword-rules')
  ]);
  await loadNodes();
}
function editNode(node) {
  editingNodeId.value = node.id;
  Object.assign(nodeForm, emptyNode(), node, {
    planId: node.planId ? String(node.planId) : '', taskId: node.taskId ? String(node.taskId) : '',
    startTime: node.startTime ? String(node.startTime).slice(0,5) : '', endTime: node.endTime ? String(node.endTime).slice(0,5) : ''
  });
  window.scrollTo({top:0,behavior:'smooth'});
}
function saveNode() { return run(async () => {
  const payload = prepareImportantNode(nodeForm);
  if (editingNodeId.value) await props.api.put('/important-nodes/' + editingNodeId.value, payload);
  else await props.api.post('/important-nodes', payload);
  message.value = editingNodeId.value ? '重要节点已修改' : '重要节点已保存';
  year.value = Number(payload.startDate.slice(0,4)); resetNode(); await loadNodes();
}); }
function removeNode(node) {
  if (!window.confirm('删除重要节点“' + node.title + '”？此操作不可撤销。')) return;
  return run(async () => { await props.api.delete('/important-nodes/' + node.id); if (editingNodeId.value === node.id) resetNode(); message.value = '重要节点已删除'; await loadNodes(); });
}
function saveBatch() { return run(async () => {
  const payload = parseImportantNodeBatch(batchText.value);
  await props.api.post('/important-nodes/batch', payload);
  year.value = Number(payload[0].startDate.slice(0,4)); batchText.value = ''; message.value = '重要节点已批量导入'; await loadNodes();
}); }
function editRule(rule) {
  editingRuleId.value = rule.id;
  Object.assign(ruleForm, emptyRule(), rule, { nodeType: rule.nodeType || '', enabled: rule.enabled !== false });
}
function saveRule() { return run(async () => {
  const payload = prepareKeywordRule(ruleForm);
  if (editingRuleId.value) await props.api.put('/important-node-keyword-rules/' + editingRuleId.value, payload);
  else await props.api.post('/important-node-keyword-rules', payload);
  message.value = editingRuleId.value ? '关键词规则已修改' : '关键词规则已保存';
  resetRule(); rules.value = await props.api.get('/important-node-keyword-rules'); await loadNodes();
}); }
function removeRule(rule) {
  if (!window.confirm('删除关键词规则“' + rule.keyword + '”？')) return;
  return run(async () => { await props.api.delete('/important-node-keyword-rules/' + rule.id); if (editingRuleId.value === rule.id) resetRule(); message.value = '关键词规则已删除'; rules.value = await props.api.get('/important-node-keyword-rules'); await loadNodes(); });
}
defineExpose({ refresh: () => run(loadAll) });
onMounted(() => run(loadAll));
</script>

<template>
  <main class="container-fluid app-shell">
    <header class="page-heading"><div><span class="soft-label">Important Nodes</span><h1 class="page-title">重要节点</h1><p class="muted-text mt-2 mb-0">记录考试、截止、比赛和汇报，重要程度由后端规则自动计算。</p></div><div class="important-node-toolbar"><select v-model="year" class="form-select form-select-sm" :disabled="busy" @change="run(loadNodes)"><option v-for="item in years" :key="item" :value="item">{{ item }} 年</option></select></div></header>
    <p v-if="error" class="alert alert-danger" role="alert">{{ error }}</p><p v-if="message" class="alert alert-success" role="status">{{ message }}</p>

    <section class="content-card mb-4"><h2>{{ editingNodeId ? '编辑重要节点' : '新增重要节点' }}</h2>
      <form class="row g-3" @submit.prevent="saveNode"><fieldset class="contents-fieldset" :disabled="busy"><div class="row g-3">
        <div class="col-md-8"><label class="form-label">节点标题</label><input v-model="nodeForm.title" class="form-control" maxlength="120" required></div>
        <div class="col-md-4"><label class="form-label">类型</label><select v-model="nodeForm.type" class="form-select"><option v-for="item in ['考试','截止','比赛','汇报','其他']" :key="item">{{ item }}</option></select></div>
        <div class="col-md-3"><label class="form-label">开始日期</label><input v-model="nodeForm.startDate" class="form-control" type="date" required></div>
        <div class="col-md-3"><label class="form-label">开始时间</label><input v-model="nodeForm.startTime" class="form-control" type="time"></div>
        <div class="col-md-3"><label class="form-label">结束日期</label><input v-model="nodeForm.endDate" class="form-control" type="date"></div>
        <div class="col-md-3"><label class="form-label">结束时间</label><input v-model="nodeForm.endTime" class="form-control" type="time"></div>
        <div class="col-md-6"><label class="form-label">关联学习计划（选填）</label><select v-model="nodeForm.planId" class="form-select" @change="nodeForm.taskId = ''"><option value="">不关联学习计划</option><option v-for="plan in plans" :key="plan.id" :value="String(plan.id)">{{ plan.planName }}</option></select></div>
        <div class="col-md-6"><label class="form-label">关联学习任务（选填）</label><select v-model="nodeForm.taskId" class="form-select"><option value="">不关联学习任务</option><option v-for="task in taskOptions" :key="task.id" :value="String(task.id)">{{ task.taskName }}</option></select></div>
        <div class="col-12"><label class="form-label">描述</label><textarea v-model="nodeForm.description" class="form-control" rows="3"></textarea></div>
        <div class="col-12"><button class="btn btn-primary">{{ busy ? '保存中…' : '保存节点' }}</button><button class="btn btn-outline-primary ms-2" type="button" @click="resetNode">{{ editingNodeId ? '取消编辑' : '清空' }}</button></div>
      </div></fieldset></form>
    </section>

    <section class="content-card mb-4"><h2>批量导入重要节点</h2><p class="muted-text">每行格式：标题 | 类型 | 开始日期 | 开始时间 | 结束日期 | 结束时间 | 描述</p><textarea v-model="batchText" class="form-control mb-3" rows="5" placeholder="每行一个重要节点"></textarea><button class="btn btn-primary" :disabled="busy" @click="saveBatch">批量导入</button><button class="btn btn-outline-primary ms-2" :disabled="busy" @click="batchText = ''">清空输入</button></section>

    <section class="content-card mb-4"><div class="card-title-row"><h2>关键词规则</h2></div>
      <form class="row g-3 mb-3" @submit.prevent="saveRule"><fieldset class="contents-fieldset" :disabled="busy"><div class="row g-3">
        <div class="col-md-3"><label class="form-label">关键词</label><input v-model="ruleForm.keyword" class="form-control" maxlength="60" required></div>
        <div class="col-md-2"><label class="form-label">适用类型</label><select v-model="ruleForm.nodeType" class="form-select"><option value="">全部类型</option><option v-for="item in ['考试','截止','比赛','汇报','其他']" :key="item">{{ item }}</option></select></div>
        <div class="col-md-3"><label class="form-label">影响方式</label><select v-model="ruleForm.effect" class="form-select"><option value="SCORE">加分</option><option value="MIN_IMPORTANT">至少重要</option><option value="URGENT">直接紧急</option></select></div>
        <div class="col-md-2"><label class="form-label">加分值</label><input v-model="ruleForm.scoreDelta" class="form-control" type="number" min="1" max="50" :disabled="ruleForm.effect !== 'SCORE'"></div>
        <div class="col-md-2 d-flex align-items-end"><label class="form-check mb-2"><input v-model="ruleForm.enabled" class="form-check-input" type="checkbox"> 启用</label></div>
        <div class="col-12"><button class="btn btn-primary">保存规则</button><button class="btn btn-outline-primary ms-2" type="button" @click="resetRule">{{ editingRuleId ? '取消编辑' : '清空' }}</button></div>
      </div></fieldset></form>
      <div class="table-responsive"><table class="table app-table align-middle"><thead><tr><th>关键词</th><th>适用类型</th><th>影响方式</th><th>加分</th><th>状态</th><th>操作</th></tr></thead><tbody><tr v-for="rule in rules" :key="rule.id"><td>{{ rule.keyword }}</td><td>{{ rule.nodeType || '全部类型' }}</td><td>{{ effectLabel(rule) }}</td><td>{{ rule.effect === 'SCORE' ? rule.scoreDelta : '—' }}</td><td>{{ rule.enabled ? '启用' : '停用' }}</td><td><button class="btn btn-sm btn-primary me-2" @click="editRule(rule)">编辑</button><button class="btn btn-sm btn-outline-primary" @click="removeRule(rule)">删除</button></td></tr><tr v-if="!rules.length"><td colspan="6">暂无关键词规则</td></tr></tbody></table></div>
    </section>

    <section class="content-card"><div class="card-title-row"><h2>{{ year }} 年重要节点</h2><button class="btn btn-outline-primary btn-sm" :disabled="busy" @click="run(loadNodes)">刷新</button></div>
      <div class="important-node-list"><article v-for="node in sortedNodes" :key="node.id" class="important-node-card"><div class="important-node-row"><div class="important-node-main"><span class="important-node-date">{{ dateRange(node) }} · {{ timeRange(node) }}</span><span class="badge important-node-type" :class="typeClass(node.type)">[{{ node.type }}]</span><h3>{{ node.title }}</h3><span class="badge important-node-importance" :class="importanceClass(node.importance)">{{ node.importance }}</span><p v-if="node.description">{{ node.description }}</p><p v-if="relation(node)" class="muted-text">{{ relation(node) }}</p></div><div class="important-node-actions"><button class="btn btn-sm btn-primary" @click="editNode(node)">编辑</button><button class="btn btn-sm btn-outline-primary" @click="removeNode(node)">删除</button></div></div></article><div v-if="!sortedNodes.length" class="important-node-empty"><h3>这一年还没有重要节点。</h3></div></div>
    </section>
  </main>
</template>