<script setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue';
import { parseBatchTasks, prepareTask, parseQuickCapture, prepareCategory } from '../workflow.js';

const props = defineProps({ api: { type: Function, required: true } });
const emit = defineEmits(['session-expired', 'busy-change']);
const plans = ref([]);
const tasks = ref([]);
const busy = ref(false);
const error = ref('');
const message = ref('');
const editingId = ref(null);
const batchText = ref('');
const templates = ref([]), categoryName = ref(''), recommendation = ref('college');
const categoryReady = ref(false), recommendationError = ref('');
const showCategoryCreate = ref(false);
const categoryDetails = ref([]), categoryEditingId = ref(null), categoryEditName = ref('');
const categories = computed(() => categoryDetails.value.map(item => item.categoryName));
const deletingCategory = ref(null), replacementId = ref('');
const replacementCategories = computed(() => categoryDetails.value.filter(item => item.id !== deletingCategory.value?.id));
const recommendedCategories = computed(() => templates.value.find(item => item.code === recommendation.value)?.categories || []);
const captureText = ref(''), capturePreview = ref(null);
const categoryOptions = computed(() => {
  return [...new Set([...categories.value, ...(form.category ? [form.category] : [])])];
});
const captureExample = '目标：准备软件工程考试\n计划：完成课程复习\n时间：2026-11-22 至 2026-12-10\n\n任务：复习需求分析\n分类：课程进度\n截止：2026-11-25\n状态：已完成\n记录：完成需求分析章节\n日期：2026-11-22\n时长：45\n问题：活动图还不熟悉\n反思：继续做两道练习题\n\n任务：复习软件测试\n分类：考试备考\n截止：2026-11-28\n状态：未完成';
const filters = reactive({ planId: '', status: '', priority: '', date: '' });
const emptyTask = () => ({
  taskName: '', planId: '', category: '', priority: 'Q2', deadline: '', status: '未完成',
  description: '', repeatType: 'NONE', repeatEndDate: '', reminderEnabled: 'true', reminderDaysBefore: 3
});
const form = reactive(emptyTask());
const planNames = computed(() => Object.fromEntries(plans.value.map(plan => [plan.id, plan.planName])));
async function focusCategoryControl(id, select = false) {
  await nextTick();
  // Let the busy state clear and DOM controls re-enable before moving focus.
  requestAnimationFrame(() => {
    const control = document.getElementById(id);
    control?.focus();
    if (select) control?.select?.();
  });
}
function toggleCategoryCreate() {
  const opening = !showCategoryCreate.value;
  setCategoryPanel(opening ? 'create' : null);
  focusCategoryControl(opening ? 'new-category' : 'category-create-toggle');
}
function setCategoryPanel(mode, category = null) {
  showCategoryCreate.value = mode === 'create';
  categoryEditingId.value = mode === 'edit' ? category.id : null;
  deletingCategory.value = mode === 'delete' ? category : null;
}
function cancelCategoryEdit() {
  const id = categoryEditingId.value;
  setCategoryPanel(null);
  focusCategoryControl('category-edit-' + id);
}
function cancelCategoryDelete() {
  const id = deletingCategory.value?.id;
  setCategoryPanel(null);
  focusCategoryControl('category-delete-' + id);
}
function resetForm() { editingId.value = null; Object.assign(form, emptyTask(), { category: categories.value[0] || '' }); }
async function run(action) {
  if (busy.value) return;
  busy.value = true; emit('busy-change', true); error.value = ''; message.value = '';
  try { await action(); }
  catch (failure) { if (failure.status === 401) emit('session-expired'); else error.value = failure.message; }
  finally { busy.value = false; emit('busy-change', false); }
}
async function loadTasks() {
  const params = Object.fromEntries(Object.entries(filters).filter(([, value]) => value));
  tasks.value = await props.api.get('/tasks', { params });
}
async function loadCategories() {
  try {
    categoryDetails.value = await props.api.get('/tasks/categories/mine');
    categoryReady.value = true;
  } catch (failure) { categoryReady.value = false; throw failure; }
}
function refreshTasksAndCategories() {
  return Promise.all([loadCategories(), loadTasks()]);
}
async function loadInitial() {
  const results = await Promise.allSettled([
    props.api.get('/plans').then(value => { plans.value = value; }),
    loadCategories(),
    props.api.get('/tasks/categories/templates').then(value => { templates.value = value; recommendationError.value = ''; }),
    loadTasks()
  ]);
  const expired = results.find(result => result.status === 'rejected' && result.reason.status === 401);
  if (expired) throw expired.reason;
  if (results[1].status === 'rejected') categoryReady.value = false;
  if (results[2].status === 'rejected') recommendationError.value = '推荐分类暂不可用，不影响个人分类和任务。';
  error.value = results.filter((result, index) => result.status === 'rejected' && index !== 2).map(result => result.reason.message).join('；');
  if (!form.category) form.category = categories.value[0] || '';
}
function edit(task) {
  editingId.value = task.id;
  Object.assign(form, emptyTask(), {
    ...task,
    planId: String(task.planId ?? ''),
    deadline: task.deadline || '',
    repeatEndDate: task.repeatEndDate || '',
    reminderEnabled: String(task.reminderEnabled ?? true),
    reminderDaysBefore: task.reminderDaysBefore ?? 3
  });
  window.scrollTo({ top: 0, behavior: 'smooth' });
}
function save() {
  return run(async () => {
    const payload = prepareTask(form);
    if (editingId.value) await props.api.put('/tasks/' + editingId.value, payload);
    else await props.api.post('/tasks', payload);
    message.value = editingId.value ? '任务已修改' : '任务已保存';
    resetForm();
    await refreshTasksAndCategories();
  });
}
function removeTask(task) {
  if (!window.confirm('删除“' + task.taskName + '”及其学习记录？此操作不可撤销。')) return;
  return run(async () => {
    await props.api.delete('/tasks/' + task.id);
    if (editingId.value === task.id) resetForm();
    message.value = '任务已删除';
    await refreshTasksAndCategories();
  });
}
function saveBatch() {
  return run(async () => {
    const payload = parseBatchTasks(batchText.value, plans.value, categories.value);
    await props.api.post('/tasks/batch', payload);
    batchText.value = '';
    message.value = '批量任务已保存';
    await refreshTasksAndCategories();
  });
}
function repeatLabel(type) { return ({ DAILY: '每日', WEEKLY: '每周', WORKDAY: '工作日' })[type] || '不重复'; }
function addCategory(name = categoryName.value.trim()) {
  return run(async () => {
    await props.api.post('/tasks/categories', prepareCategory({ categoryName: name }));
    await loadCategories();
    form.category = name; categoryName.value = '';
    setCategoryPanel(null);
    message.value = '已添加到我的分类，现有任务和分类保持原样';
    await focusCategoryControl('category-create-toggle');
  });
}
function startCategoryEdit(category) {
  setCategoryPanel('edit', category); categoryEditName.value = category.categoryName;
  focusCategoryControl('edit-category-name', true);
}
function renameCategory() {
  return run(async () => {
    const category = categoryDetails.value.find(item => item.id === categoryEditingId.value);
    const payload = prepareCategory({ categoryName: categoryEditName.value });
    await props.api.put('/tasks/categories/' + category.id, payload);
    if (form.category === category.categoryName) form.category = payload.categoryName;
    setCategoryPanel(null); capturePreview.value = null;
    await refreshTasksAndCategories();
    message.value = '分类及相关任务已改名，学习记录保持原样';
    await focusCategoryControl('category-edit-' + category.id);
  });
}
function startCategoryDelete(category) {
  return run(async () => {
    await loadCategories();
    const current = categoryDetails.value.find(item => item.id === category.id);
    if (!current) throw new Error('分类已变化，请重新选择');
    setCategoryPanel('delete', current); replacementId.value = '';
    await focusCategoryControl(current.taskCount && replacementCategories.value.length ? 'category-destination' : 'category-delete-cancel');
  });
}
function removeCategory() {
  return run(async () => {
    const category = deletingCategory.value;
    if (category.taskCount > 0 && !replacementId.value) throw new Error('请选择任务转入分类');
    const target = categoryDetails.value.find(item => item.id === replacementId.value);
    await props.api.delete('/tasks/categories/' + category.id, { params: replacementId.value ? { replacementId: replacementId.value } : {} });
    setCategoryPanel(null); capturePreview.value = null;
    await refreshTasksAndCategories();
    if (form.category === category.categoryName) form.category = target?.categoryName || categories.value[0] || '';
    message.value = '分类已删除，任务和学习记录已保留';
    await focusCategoryControl('category-create-toggle');
  });
}
function previewCapture() {
  return run(async () => { capturePreview.value = parseQuickCapture(captureText.value, categories.value); });
}
function saveCapture() {
  return run(async () => {
    const payload = parseQuickCapture(captureText.value, categories.value);
    await props.api.post('/tasks/quick-capture', payload);
    captureText.value = ''; capturePreview.value = null;
    await loadInitial(); message.value = '计划、任务与学习记录已一次性保存';
  });
}
function reminderLabel(task) { return task.reminderEnabled === false ? '关闭' : '提前 ' + (task.reminderDaysBefore ?? 3) + ' 天'; }
defineExpose({ refresh: () => run(loadInitial) });
onMounted(() => run(loadInitial));
</script>

<template>
  <main class="container-fluid app-shell">
    <header class="page-heading"><div><span class="soft-label">Tasks</span><h1 class="page-title">学习任务管理</h1></div></header>
    <p v-if="error" class="alert alert-danger" role="alert">{{ error }}</p>
    <p v-if="message" class="alert alert-success" role="status">{{ message }}</p>
    <section class="content-card mb-4 category-manager" aria-labelledby="my-categories-title">
      <div class="card-title-row category-manager-heading">
        <div class="category-heading-label"><h2 id="my-categories-title">我的分类</h2><span v-if="categoryReady" class="soft-label">{{ categoryDetails.length }} 个</span></div>
        <button id="category-create-toggle" class="btn btn-sm btn-primary" :disabled="busy || !categoryReady" :aria-expanded="showCategoryCreate" aria-controls="category-create-form" @click="toggleCategoryCreate">{{ showCategoryCreate ? '收起新增' : '＋ 新增分类' }}</button>
      </div>
      <form v-if="showCategoryCreate" id="category-create-form" class="category-create-form" @submit.prevent="addCategory()">
        <div class="category-field"><label class="form-label" for="new-category">分类名称</label><input id="new-category" v-model="categoryName" class="form-control" maxlength="50" required :disabled="busy" placeholder="如：健身、工作、阅读" aria-describedby="new-category-help" @keydown.esc="!busy && toggleCategoryCreate()"><span id="new-category-help" class="category-field-hint">{{ categoryName.length }} / 50 个字符</span></div>
        <div class="category-inline-actions"><button class="btn btn-primary" :disabled="busy || !categoryReady || !categoryName.trim()">{{ busy ? '添加中…' : '添加分类' }}</button><button class="btn btn-outline-primary" type="button" :disabled="busy" @click="toggleCategoryCreate">取消</button></div>
      </form>
      <details class="category-recommendations" :open="!categoryReady || !categoryDetails.length">
        <summary class="category-recommendation-summary">添加推荐分类<span>大学生 / 打工人</span></summary>
        <div class="category-recommendation-heading"><span class="category-section-label">参考推荐</span><div class="category-preset-tabs" role="group" aria-label="分类推荐方案"><button v-for="item in templates" :key="item.code" class="btn btn-sm btn-outline-primary category-preset-tab" :class="{ 'is-selected': recommendation === item.code }" :aria-pressed="recommendation === item.code" :disabled="busy" @click="recommendation = item.code">{{ item.name }}</button></div></div>
        <p v-if="recommendationError" class="muted-text mb-0" role="status">{{ recommendationError }}</p>
        <div class="category-heatmap-filters category-suggestions"><button v-for="name in recommendedCategories" :key="name" class="btn btn-sm btn-outline-primary category-suggestion" :class="{ 'is-added': categories.includes(name) }" :disabled="busy || !categoryReady || categories.includes(name)" :aria-label="name + (categories.includes(name) ? '，已添加' : '，添加到我的分类')" @click="addCategory(name)"><span>{{ name }}</span><small>{{ categories.includes(name) ? '已添加' : '＋ 添加' }}</small></button></div>
      </details>
      <p v-if="!categoryReady" class="category-empty muted-text" role="status">{{ busy ? '正在加载我的分类…' : '分类加载失败，请刷新页面重试' }}</p>
      <div v-else-if="!categoryDetails.length" class="category-empty"><strong>创建你的第一个分类</strong><p class="muted-text mb-0">从上方推荐中添加，或点击“新增分类”。</p></div>
      <div v-else class="table-responsive category-table-wrap"><table class="table app-table align-middle category-table"><thead><tr><th>分类名称</th><th class="category-count-column">任务数</th><th class="category-actions-column">操作</th></tr></thead><tbody>
        <template v-for="category in categoryDetails" :key="category.id">
          <tr :class="{ 'category-row-active': categoryEditingId === category.id || deletingCategory?.id === category.id }">
            <td v-if="categoryEditingId === category.id" colspan="3">
              <form class="category-edit-form" @submit.prevent="renameCategory">
                <div class="category-field"><label for="edit-category-name" class="form-label">修改分类名称<span class="category-field-context">{{ category.taskCount }} 个关联任务</span></label><input id="edit-category-name" v-model="categoryEditName" class="form-control form-control-sm" maxlength="50" required :disabled="busy" aria-describedby="edit-category-help" @keydown.esc="!busy && cancelCategoryEdit()"><span id="edit-category-help" class="category-field-hint">{{ categoryEditName.length }} / 50 个字符 · Enter 保存，Esc 取消</span></div>
                <div class="category-inline-actions"><button class="btn btn-sm btn-primary" :disabled="busy || !categoryReady || !categoryEditName.trim() || categoryEditName.trim() === category.categoryName">{{ busy ? '保存中…' : '保存名称' }}</button><button class="btn btn-sm btn-outline-primary" type="button" :disabled="busy" @click="cancelCategoryEdit">取消</button></div>
              </form>
            </td>
            <template v-else>
            <td><span class="category-name">{{ category.categoryName }}</span></td>
            <td class="category-count-column"><span class="category-task-count">{{ category.taskCount }}</span></td>
            <td class="category-actions-column"><div class="category-row-actions"><button :id="'category-edit-' + category.id" class="btn btn-sm btn-outline-primary category-row-action" :disabled="busy" :aria-label="'修改分类：' + category.categoryName" @click="startCategoryEdit(category)">修改</button><button :id="'category-delete-' + category.id" class="btn btn-sm btn-outline-primary category-row-action category-remove-action" :disabled="busy" :aria-label="'删除分类：' + category.categoryName" :aria-expanded="deletingCategory?.id === category.id" @click="startCategoryDelete(category)">删除</button></div></td>
            </template>
          </tr>
          <tr v-if="deletingCategory?.id === category.id" class="category-detail-row"><td colspan="3">
            <div class="category-delete-panel" role="group" :aria-label="'删除分类：' + category.categoryName">
              <div><strong>删除“{{ deletingCategory.categoryName }}”</strong><p v-if="deletingCategory.taskCount" class="muted-text">将 {{ deletingCategory.taskCount }} 个任务转入其他分类，学习记录保留。</p><p v-else class="muted-text">该分类暂无任务，确认后将从我的分类中移除。</p>
                <template v-if="deletingCategory.taskCount"><label for="category-destination" class="form-label">任务转入</label><select id="category-destination" v-model="replacementId" class="form-select form-select-sm" :disabled="busy"><option value="">请选择接收分类</option><option v-for="item in replacementCategories" :key="item.id" :value="item.id">{{ item.categoryName }}</option></select><p v-if="!replacementCategories.length" class="muted-text mb-0">请先添加另一个分类，再迁移任务。</p></template>
              </div>
              <div class="category-inline-actions"><button id="category-delete-cancel" class="btn btn-sm btn-outline-primary" :disabled="busy" @click="cancelCategoryDelete">取消</button><button class="btn btn-sm btn-primary" :disabled="busy || !categoryReady || (deletingCategory.taskCount > 0 && !replacementId)" @click="removeCategory">{{ busy ? '处理中…' : (deletingCategory.taskCount ? '转移任务并删除' : '确认删除分类') }}</button></div>
            </div>
          </td></tr>
        </template>
      </tbody></table></div>
    </section>
    <section class="content-card mb-4">
      <h2>{{ editingId ? '编辑学习任务' : '任务表单' }}</h2>
      <form class="row g-3" @submit.prevent="save"><fieldset class="contents-fieldset" :disabled="busy || !categoryReady || !categories.length">
        <div class="row g-3">
          <div class="col-md-3"><label class="form-label">任务名称</label><input v-model="form.taskName" class="form-control" required maxlength="100"></div>
          <div class="col-md-3"><label class="form-label">所属计划</label><select v-model="form.planId" class="form-select" required><option value="">请选择学习计划</option><option v-for="plan in plans" :key="plan.id" :value="String(plan.id)">{{ plan.planName }}</option></select></div>
          <div class="col-md-2"><label class="form-label">分类</label><select v-model="form.category" class="form-select" required><option v-for="category in categoryOptions" :key="category">{{ category }}</option></select></div>
          <div class="col-md-2"><label class="form-label">优先级</label><select v-model="form.priority" class="form-select"><option v-for="value in ['Q0','Q1','Q2','Q3']" :key="value">{{ value }}</option></select></div>
          <div class="col-md-2"><label class="form-label">截止日期</label><input v-model="form.deadline" class="form-control" type="date"></div>
          <div class="col-md-3"><label class="form-label">状态</label><select v-model="form.status" class="form-select"><option v-for="value in ['未完成','进行中','已完成','延期']" :key="value">{{ value }}</option></select></div>
          <div class="col-md-9"><label class="form-label">任务描述</label><input v-model="form.description" class="form-control"></div>
          <div class="col-12"><details><summary class="btn btn-outline-primary btn-sm">高级设置</summary><div class="row g-3 mt-1">
            <div class="col-md-3"><label class="form-label">重复规则</label><select v-model="form.repeatType" class="form-select"><option value="NONE">不重复</option><option value="DAILY">每日</option><option value="WEEKLY">每周</option><option value="WORKDAY">工作日</option></select></div>
            <div class="col-md-3"><label class="form-label">重复结束日期</label><input v-model="form.repeatEndDate" class="form-control" type="date" :disabled="form.repeatType === 'NONE'"></div>
            <div class="col-md-3"><label class="form-label">提醒</label><select v-model="form.reminderEnabled" class="form-select"><option value="true">启用提醒</option><option value="false">关闭提醒</option></select></div>
            <div class="col-md-3"><label class="form-label">提前提醒天数</label><input v-model="form.reminderDaysBefore" class="form-control" type="number" min="0" max="30"></div>
          </div></details></div>
          <div class="col-12"><button class="btn btn-primary">{{ busy ? '保存中…' : (editingId ? '保存修改' : '保存任务') }}</button> <button class="btn btn-outline-primary" type="button" @click="resetForm">{{ editingId ? '取消' : '清空' }}</button></div>
        </div>
      </fieldset></form>
    </section>
    <section class="content-card mb-4">
      <h2>快速收集：计划、任务与记录</h2>
      <p class="muted-text">按示例粘贴，一次预览并确认保存整组内容。记录可选；日期、时长可省略，默认今天与 0 分钟。分类须先创建。</p>
      <button class="btn btn-outline-primary mb-3" :disabled="busy || !!captureText" @click="captureText = captureExample; capturePreview = null">填入格式示例</button>
      <textarea v-model="captureText" class="form-control mb-3" rows="10" :disabled="busy" aria-label="快速收集内容" @input="capturePreview = null"></textarea>
      <button class="btn btn-outline-primary" :disabled="busy || !categoryReady || !categories.length || !captureText.trim()" @click="previewCapture">预览内容</button>
      <div v-if="capturePreview" class="mt-3">
        <p>计划：{{ capturePreview.plan.planName }} · 目标：{{ capturePreview.plan.goal || '—' }}</p>
        <p>{{ capturePreview.plan.startDate || '未设开始日期' }} 至 {{ capturePreview.plan.endDate || '未设结束日期' }} · 共 {{ capturePreview.tasks.length }} 个任务，{{ capturePreview.tasks.filter(row => row.record).length }} 条记录</p>
        <ul><li v-for="(row, index) in capturePreview.tasks" :key="index">{{ row.task.taskName }} · {{ row.task.category }} · {{ row.task.status }} · {{ row.task.deadline || '无截止日期' }}<span v-if="row.record"> · 记录：{{ row.record.content }} · {{ row.record.duration }} 分钟 · 问题：{{ row.record.problems || '—' }} · 反思：{{ row.record.improvementPlan || '—' }}</span></li></ul>
        <button class="btn btn-primary" :disabled="busy || !categoryReady" @click="saveCapture">确认保存整组内容</button>
      </div>
    </section>
    <section class="content-card mb-4">
      <h2>批量新增任务</h2>
      <p class="muted-text">每行格式：任务名称 | 所属计划 | 分类 | 优先级 | 截止日期 | 任务描述</p>
      <textarea v-model="batchText" class="form-control mb-3" rows="5" placeholder="每行一个任务"></textarea>
      <button class="btn btn-primary" :disabled="busy || !categoryReady || !categories.length" @click="saveBatch">批量保存任务</button>
      <button class="btn btn-outline-primary ms-2" :disabled="busy" @click="batchText = ''">清空输入</button>
    </section>
    <section class="content-card">
      <div class="card-title-row"><h2>任务列表</h2><button class="btn btn-outline-primary btn-sm" :disabled="busy" @click="run(loadTasks)">刷新</button></div>
      <div class="row g-2 mb-3">
        <div class="col-md-3"><select v-model="filters.planId" class="form-select"><option value="">全部计划</option><option v-for="plan in plans" :key="plan.id" :value="String(plan.id)">{{ plan.planName }}</option></select></div>
        <div class="col-md-2"><select v-model="filters.priority" class="form-select"><option value="">全部优先级</option><option v-for="value in ['Q0','Q1','Q2','Q3']" :key="value">{{ value }}</option></select></div>
        <div class="col-md-2"><select v-model="filters.status" class="form-select"><option value="">全部状态</option><option v-for="value in ['未完成','进行中','已完成','延期']" :key="value">{{ value }}</option></select></div>
        <div class="col-md-3"><input v-model="filters.date" class="form-control" type="date"></div>
        <div class="col-md-2"><button class="btn btn-outline-primary w-100" :disabled="busy" @click="run(loadTasks)">筛选</button></div>
      </div>
      <div class="table-responsive"><table class="table app-table align-middle"><thead><tr><th>任务名称</th><th>所属计划</th><th>分类</th><th>优先级</th><th>截止日期</th><th>重复</th><th>提醒</th><th>状态</th><th>操作</th></tr></thead><tbody>
        <tr v-for="task in tasks" :key="task.id"><td>{{ task.taskName }}</td><td>{{ planNames[task.planId] || '未知计划' }}</td><td>{{ task.category }}</td><td>{{ task.priority }}</td><td>{{ task.deadline || '—' }}</td><td>{{ repeatLabel(task.repeatType) }}</td><td>{{ reminderLabel(task) }}</td><td>{{ task.status }}</td><td><button class="btn btn-sm btn-primary me-2" :disabled="busy" @click="edit(task)">编辑</button><button class="btn btn-sm btn-outline-primary" :disabled="busy" @click="removeTask(task)">删除</button></td></tr>
        <tr v-if="!tasks.length"><td colspan="9">暂无学习任务</td></tr>
      </tbody></table></div>
    </section>
  </main>
</template>

<style scoped>
.category-manager-heading { margin-bottom: 18px; flex-wrap: wrap; }
.category-heading-label { display: flex; align-items: center; gap: 10px; }
.category-heading-label .soft-label { font-size: 12px; padding: 3px 9px; }
.category-heading-label { min-width: 0; }
.category-manager .btn:focus-visible, .category-recommendation-summary:focus-visible { outline: 2px solid var(--primary); outline-offset: 3px; }
.category-recommendations { padding: 14px 16px; border: 1px solid var(--border-soft); border-radius: 12px; background: var(--card-bg-soft, var(--primary-soft)); margin-bottom: 18px; }
.category-recommendation-summary { cursor: pointer; color: var(--title); font-size: 13px; font-weight: 600; overflow-wrap: anywhere; }
.category-recommendation-summary::marker { color: var(--muted); }
.category-recommendation-summary span { color: var(--muted); font-weight: 400; font-size: 12px; margin-left: 12px; }
.category-recommendations[open] .category-recommendation-summary { margin-bottom: 16px; }
.category-recommendation-heading { display: flex; align-items: center; gap: 16px; flex-wrap: wrap; margin-bottom: 12px; }
.category-section-label { font-size: 12px; color: var(--muted); font-weight: 600; }
.category-preset-tabs { display: flex; flex-wrap: wrap; gap: 4px; }
.category-preset-tab { border-color: transparent; color: var(--muted); font-size: 12px; padding: 4px 10px; box-shadow: none; }
.category-preset-tab.is-selected { color: var(--primary-hover); border-color: var(--border-soft); background: var(--card-bg); }
.category-suggestion { display: inline-flex; align-items: center; gap: 10px; background: var(--card-bg); border-radius: 8px; font-size: 12px; font-weight: 500; padding: 6px 10px; }
.category-suggestion small { font-size: 11px; opacity: .8; white-space: nowrap; }
.category-suggestion.is-added { color: var(--muted); border-color: var(--border-soft); background: transparent; opacity: .75; }
.category-create-form { display: grid; grid-template-columns: minmax(0, 1fr) auto; align-items: center; gap: 14px; padding: 16px; margin-bottom: 18px; border: 1px solid var(--border-soft); border-radius: 12px; background: var(--primary-soft); }
.category-field { min-width: 0; }
.category-field .form-label { display: flex; align-items: baseline; gap: 8px; flex-wrap: wrap; font-size: 13px; font-weight: 600; margin-bottom: 8px; }
.category-field .form-control { width: 100%; }
.category-field-hint, .category-field-context { display: block; font-size: 12px; line-height: 1.6; font-weight: 400; color: var(--muted); }
.category-field-hint { margin-top: 7px; }
.category-inline-actions { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.category-inline-actions .btn { white-space: nowrap; }
.category-table-wrap { border: 1px solid var(--border-soft); border-radius: 12px; }
.category-table { margin-bottom: 0; table-layout: fixed; width: 100%; }
.category-table th, .category-table td { padding: 12px 16px; overflow-wrap: anywhere; }
.category-table th { font-size: 12px; color: var(--muted); font-weight: 600; }
.category-name { color: var(--title); font-size: 14px; font-weight: 600; }
.category-count-column { width: 90px; text-align: center; }
.category-task-count { display: inline-block; min-width: 28px; font-size: 13px; font-variant-numeric: tabular-nums; color: var(--muted); }
.category-actions-column { width: 150px; text-align: right; }
.category-row-actions { display: flex; justify-content: flex-end; gap: 6px; }
.category-row-action { border-color: transparent; padding: 4px 8px; font-size: 12px; font-weight: 500; box-shadow: none; }
.category-remove-action { color: var(--danger); }
.category-row-active td { background: var(--primary-soft); }
.category-edit-form { display: grid; grid-template-columns: minmax(0, 1fr) auto; align-items: center; gap: 16px; padding: 4px 0; }
.category-edit-form .category-field { max-width: 560px; }
.category-table .category-row-active td:first-child { border-left: 3px solid var(--primary); }
@media (hover: hover) {
  .category-table tbody tr:not(.category-detail-row):not(.category-row-active):hover td { background: var(--primary-soft); }
}
.category-detail-row td { background: var(--card-bg-soft, var(--primary-soft)); padding: 18px 16px; }
.category-delete-panel { display: grid; grid-template-columns: minmax(0, 1fr) auto; align-items: end; gap: 18px; }
.category-delete-panel strong { display: block; color: var(--title); font-size: 14px; overflow-wrap: anywhere; }
.category-delete-panel p { font-size: 13px; line-height: 1.6; margin: 6px 0 12px; }
.category-delete-panel .form-label { font-size: 12px; margin-bottom: 6px; }
.category-delete-panel .form-select { max-width: 360px; }
.category-empty { text-align: center; padding: 28px 16px; border: 1px dashed var(--border-soft); border-radius: 12px; font-size: 13px; }
.category-empty strong { display: block; margin-bottom: 8px; color: var(--title); font-size: 15px; }
@media (max-width: 575px) {
  .category-manager { padding: 16px; }
  .category-recommendations { padding: 12px; }
  .category-recommendation-heading { gap: 8px; }
  .category-create-form, .category-delete-panel, .category-edit-form { grid-template-columns: 1fr; gap: 12px; }
  .category-table th, .category-table td { padding: 10px 8px; }
  .category-count-column { width: 54px; }
  .category-actions-column { width: 94px; }
  .category-row-actions { gap: 0; flex-wrap: wrap; }
  .category-row-action { min-height: 40px; padding: 5px 6px; }
  .category-inline-actions .btn, .category-manager-heading > .btn { min-height: 40px; }
  .category-preset-tab, .category-suggestion { min-height: 36px; }
  .category-create-form { padding: 12px; }
  .category-edit-form .category-inline-actions { justify-content: flex-end; }
  .category-detail-row td { padding: 14px 12px; }
}
</style>
