<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { formatImportantNodeDateTime, groupImportantNodesByMonth, parseImportantNodeBatch, prepareImportantNode, prepareKeywordRule } from '../insights.js';

const props = defineProps({ api: { type: Function, required: true } });
const emit = defineEmits(['session-expired', 'busy-change']);
const currentYear = new Date().getFullYear();
const year = ref(currentYear), nodes = ref([]), rules = ref([]), plans = ref([]), tasks = ref([]);
const busy = ref(false), error = ref(''), message = ref(''), editingNodeId = ref(null), editingRuleId = ref(null), batchText = ref('');
const modalOpen = ref(false), modalTab = ref('single');
const years = Array.from({length: 7}, (_, index) => currentYear - 3 + index);
const emptyNode = () => ({ title:'', type:'考试', startDate:'', startTime:'', endDate:'', endTime:'', planId:'', taskId:'', description:'' });
const emptyRule = () => ({ keyword:'', nodeType:'', effect:'SCORE', scoreDelta:10, enabled:true });
const nodeForm = reactive(emptyNode()), ruleForm = reactive(emptyRule());
const planMap = computed(() => Object.fromEntries(plans.value.map(item => [item.id, item.planName])));
const taskMap = computed(() => Object.fromEntries(tasks.value.map(item => [item.id, item.taskName])));
const taskOptions = computed(() => nodeForm.planId ? tasks.value.filter(task => String(task.planId) === String(nodeForm.planId)) : tasks.value);
const nodeMonths = computed(() => groupImportantNodesByMonth(nodes.value));
function resetNode() { editingNodeId.value = null; Object.assign(nodeForm, emptyNode()); }
function openNode() {
  resetNode(); batchText.value = ''; modalTab.value = 'single';
  const date = year.value === new Date().getFullYear() ? new Date().toLocaleDateString('en-CA') : year.value + '-01-01';
  nodeForm.startDate = date; nodeForm.endDate = date; modalOpen.value = true;
}
function closeNode() { if (busy.value) return; modalOpen.value = false; resetNode(); batchText.value = ''; modalTab.value = 'single'; }
function resetRule() { editingRuleId.value = null; Object.assign(ruleForm, emptyRule()); }
function importanceClass(value) { return value === '紧急' ? 'node-urgent' : value === '重要' ? 'node-important' : 'node-normal'; }
function typeClass(value) { return ({考试:'node-type-exam',截止:'node-type-deadline',比赛:'node-type-contest',汇报:'node-type-report'})[value] || 'node-type-other'; }
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
  modalTab.value = 'single'; modalOpen.value = true;
}
function saveNode() { return run(async () => {
  const payload = prepareImportantNode(nodeForm);
  if (editingNodeId.value) await props.api.put('/important-nodes/' + editingNodeId.value, payload);
  else await props.api.post('/important-nodes', payload);
  message.value = editingNodeId.value ? '重要节点已修改' : '重要节点已保存';
  year.value = Number(payload.startDate.slice(0,4)); await loadNodes(); modalOpen.value = false; resetNode();
}); }
function removeNode(node) {
  if (!window.confirm('删除重要节点“' + node.title + '”？此操作不可撤销。')) return;
  return run(async () => { await props.api.delete('/important-nodes/' + node.id); if (editingNodeId.value === node.id) resetNode(); message.value = '重要节点已删除'; await loadNodes(); });
}
function saveBatch() { return run(async () => {
  const payload = parseImportantNodeBatch(batchText.value);
  await props.api.post('/important-nodes/batch', payload);
  year.value = Number(payload[0].startDate.slice(0,4)); batchText.value = ''; message.value = '重要节点已批量导入'; await loadNodes(); modalOpen.value = false;
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
    <header class="page-heading"><div><span class="soft-label">Important Nodes</span><h1 class="page-title">重要节点</h1><p class="muted-text mt-2 mb-0">记录考试、截止、比赛和汇报，重要程度由后端规则自动计算。</p></div><div class="important-node-toolbar"><select v-model="year" class="form-select form-select-sm" :disabled="busy" @change="run(loadNodes)"><option v-for="item in years" :key="item" :value="item">{{ item }} 年</option></select><button class="btn btn-primary btn-sm" :disabled="busy" @click="openNode">+ 新增节点</button></div></header>
    <p v-if="error" class="alert alert-danger" role="alert">{{ error }}</p><p v-if="message" class="alert alert-success" role="status">{{ message }}</p>

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
      <div class="important-node-list"><div v-for="group in nodeMonths" :key="group.month" class="important-node-month"><h3>{{ group.month }}月</h3><div class="important-node-month-list"><article v-for="node in group.nodes" :key="node.id" class="important-node-card"><div class="important-node-row"><div class="important-node-main"><span class="important-node-date">{{ formatImportantNodeDateTime(node) }}</span><span class="badge important-node-type" :class="typeClass(node.type)">[{{ node.type }}]</span><strong>{{ node.title }}</strong><span class="badge important-node-importance" :class="importanceClass(node.importance)">{{ node.importance }}</span></div><div class="important-node-actions"><button class="btn btn-sm btn-primary" :disabled="busy" @click="editNode(node)">编辑</button><button class="btn btn-sm btn-outline-primary" :disabled="busy" @click="removeNode(node)">删除</button></div></div><p v-if="node.description">{{ node.description }}</p></article></div></div>
        <div v-if="!nodeMonths.length" class="important-node-empty"><h3>今年还没有重要节点。</h3><p>可以点击右上角“新增节点”，记录考试、截止和汇报时间。</p><button class="btn btn-primary btn-sm" @click="openNode">+ 新增节点</button></div>
      </div>
    </section>

    <template v-if="modalOpen"><div class="modal-backdrop fade show"></div><div class="modal fade show d-block" role="dialog" aria-modal="true" aria-label="重要节点" @click.self="closeNode"><div class="modal-dialog modal-lg modal-dialog-centered"><div class="modal-content important-node-modal"><div class="modal-header"><h5 class="modal-title">{{ editingNodeId ? '编辑重要节点' : '新增重要节点' }}</h5><button type="button" class="btn-close" aria-label="关闭" :disabled="busy" @click="closeNode"></button></div><div class="modal-body"><p v-if="error" class="alert alert-danger" role="alert">{{ error }}</p>
      <ul class="nav nav-pills soft-tabs mb-3" role="tablist"><li class="nav-item"><button class="nav-link" :class="{ active: modalTab === 'single' }" role="tab" :aria-selected="modalTab === 'single'" @click="modalTab = 'single'">单个新增</button></li><li v-if="!editingNodeId" class="nav-item"><button class="nav-link" :class="{ active: modalTab === 'batch' }" role="tab" :aria-selected="modalTab === 'batch'" @click="modalTab = 'batch'">批量导入</button></li></ul>
      <form v-if="modalTab === 'single'" class="row g-3" @submit.prevent="saveNode"><fieldset class="contents-fieldset" :disabled="busy"><div class="row g-3">
        <div class="col-md-8"><label class="form-label">节点标题</label><input v-model="nodeForm.title" class="form-control" maxlength="120" required></div>
        <div class="col-md-4"><label class="form-label">类型</label><select v-model="nodeForm.type" class="form-select"><option v-for="item in ['考试','截止','比赛','汇报','其他']" :key="item">{{ item }}</option></select></div>
        <div class="col-md-3"><label class="form-label">开始日期</label><input v-model="nodeForm.startDate" class="form-control" type="date" required></div>
        <div class="col-md-3"><label class="form-label">开始时间</label><input v-model="nodeForm.startTime" class="form-control" type="time"></div>
        <div class="col-md-3"><label class="form-label">结束日期</label><input v-model="nodeForm.endDate" class="form-control" type="date" required></div>
        <div class="col-md-3"><label class="form-label">结束时间</label><input v-model="nodeForm.endTime" class="form-control" type="time"></div>
        <div class="col-md-6"><label class="form-label">关联学习计划（选填）</label><select v-model="nodeForm.planId" class="form-select" @change="nodeForm.taskId = ''"><option value="">不关联学习计划</option><option v-for="plan in plans" :key="plan.id" :value="String(plan.id)">{{ plan.planName }}</option></select></div>
        <div class="col-md-6"><label class="form-label">关联学习任务（选填）</label><select v-model="nodeForm.taskId" class="form-select"><option value="">不关联学习任务</option><option v-for="task in taskOptions" :key="task.id" :value="String(task.id)">{{ task.taskName }}</option></select></div>
        <div class="col-12"><label class="form-label">描述</label><textarea v-model="nodeForm.description" class="form-control" rows="3"></textarea></div>
        <div class="col-12 d-flex justify-content-end gap-2"><button class="btn btn-outline-primary" type="button" @click="closeNode">取消</button><button class="btn btn-primary" type="submit">{{ busy ? '保存中…' : '保存' }}</button></div>
      </div></fieldset></form>
      <form v-else class="row g-3" @submit.prevent="saveBatch"><div class="col-12"><div class="batch-help-card important-node-batch-help"><p class="mb-2"><strong>请按以下格式输入，每行一个重要节点：</strong></p><pre>标题 | 类型 | 开始日期 | 开始时间 | 结束日期 | 结束时间 | 描述</pre><p class="mb-2"><strong>示例：</strong></p><pre>六级考试 | 考试 | 2026-06-20 | 15:00 | 2026-06-20 | 17:00 | 带身份证、准考证、耳机
数模国赛 | 比赛 | 2026-06-20 | 09:00 | 2026-06-22 | 18:00 | 组队建模</pre><p class="mb-1">开始和结束日期使用 YYYY-MM-DD；时间使用 HH:mm，可留空；类型为考试、截止、比赛、汇报或其他。</p></div></div><div class="col-12"><textarea v-model="batchText" class="form-control" rows="8" placeholder="在这里粘贴批量节点内容..."></textarea></div><div class="col-12 d-flex justify-content-end gap-2"><button class="btn btn-outline-primary" type="button" :disabled="busy" @click="closeNode">取消</button><button class="btn btn-primary" type="submit" :disabled="busy">{{ busy ? '导入中…' : '导入节点' }}</button></div></form>
    </div></div></div></div></template>
  </main>
</template>