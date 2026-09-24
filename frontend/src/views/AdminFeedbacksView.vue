<script setup>
import { onMounted, reactive, ref } from 'vue';
import { buildQuery, prepareFeedbackDecision } from '../portal.js';

const props = defineProps({ api: { type: Function, required: true } });
const emit = defineEmits(['session-expired', 'busy-change']);
const feedbacks = ref([]);
const filters = reactive({ username: '', status: '', keyword: '' });
const detail = ref(null);
const processing = ref(null);
const decision = reactive({ status: '未处理', reply: '' });
const busy = ref(false);
const error = ref('');
const message = ref('');
const loaded = ref(false);

function badgeClass(status) { if (status === '已处理') return 'status-done'; if (status === '已忽略') return 'status-delay'; return 'status-waiting'; }
function shortText(text, length = 36) { const value = String(text || ''); return value.length > length ? value.slice(0, length) + '…' : value; }
function handleError(failure) { if (failure.status === 401 || failure.status === 403) emit('session-expired'); else error.value = failure.message; }
async function run(action) {
  if (busy.value) return;
  busy.value = true; emit('busy-change', true); error.value = ''; message.value = '';
  try { await action(); } catch (failure) { handleError(failure); }
  finally { busy.value = false; emit('busy-change', false); }
}
async function loadFeedbacks() {
  const query = buildQuery(filters);
  feedbacks.value = await props.api.get('/admin/feedbacks' + (query ? '?' + query : ''));
  loaded.value = true;
}
function search() { return run(loadFeedbacks); }
function reset() { Object.assign(filters, { username: '', status: '', keyword: '' }); return search(); }
function showDetail(item) { return run(async () => { detail.value = await props.api.get('/admin/feedbacks/' + item.id); processing.value = null; }); }
function showProcess(item) { return run(async () => { const value = await props.api.get('/admin/feedbacks/' + item.id); processing.value = value; detail.value = null; decision.status = value.status; decision.reply = value.reply || ''; }); }
function closeDialog() { if (!busy.value) { detail.value = null; processing.value = null; } }
function saveDecision() {
  if (!processing.value) return;
  return run(async () => {
    await props.api.put('/admin/feedbacks/' + processing.value.id, prepareFeedbackDecision(decision));
    processing.value = null; message.value = '反馈处理结果已保存';
    await loadFeedbacks();
  });
}
function refresh() { return search(); }
defineExpose({ refresh });
onMounted(search);
</script>

<template>
  <section class="admin-section" :aria-busy="busy">
    <div class="page-heading"><div><span class="soft-label">Feedback</span><h1 class="page-title">用户反馈管理</h1></div></div>
    <p v-if="error" class="alert alert-danger" role="alert">{{ error }}</p><p v-if="message" class="alert alert-success" role="status">{{ message }}</p>
    <div class="content-card mb-4"><form class="row g-3" @submit.prevent="search"><div class="col-md-3"><label class="form-label">用户名</label><input v-model="filters.username" class="form-control"></div><div class="col-md-3"><label class="form-label">处理状态</label><select v-model="filters.status" class="form-select"><option value="">全部</option><option>未处理</option><option>已处理</option><option>已忽略</option></select></div><div class="col-md-3"><label class="form-label">关键词</label><input v-model="filters.keyword" class="form-control"></div><div class="col-md-3 d-flex align-items-end gap-2"><button class="btn btn-primary" :disabled="busy">筛选</button><button class="btn btn-outline-primary" type="button" :disabled="busy" @click="reset">重置</button></div></form></div>
    <div class="content-card"><div class="card-title-row"><h2>反馈列表</h2><span v-if="busy" class="muted-text">正在加载…</span></div><div class="table-responsive"><table class="table app-table align-middle">
      <thead><tr><th>ID</th><th>提交用户</th><th>反馈内容摘要</th><th>状态</th><th>提交时间</th><th>操作</th></tr></thead><tbody><tr v-for="item in feedbacks" :key="item.id"><td>{{ item.id }}</td><td>{{ item.username }}</td><td>{{ shortText(item.content) }}</td><td><span class="badge" :class="badgeClass(item.status)">{{ item.status }}</span></td><td>{{ item.createTime || '—' }}</td><td><div class="d-flex flex-wrap gap-1"><button class="btn btn-sm btn-primary" :disabled="busy" @click="showDetail(item)">查看</button><button class="btn btn-sm btn-outline-primary" :disabled="busy" @click="showProcess(item)">处理</button></div></td></tr><tr v-if="!feedbacks.length"><td colspan="6" class="muted-text">{{ busy ? '正在加载反馈…' : loaded ? '暂无反馈' : '反馈尚未加载' }}</td></tr></tbody>
    </table></div></div>
    <template v-if="detail || processing"><div class="modal fade show d-block" tabindex="-1" role="dialog" aria-modal="true" aria-labelledby="adminFeedbackDialogTitle"><div class="modal-dialog modal-lg modal-dialog-scrollable"><div class="modal-content admin-modal"><div class="modal-header"><h2 id="adminFeedbackDialogTitle" class="modal-title fs-5">{{ processing ? '处理反馈' : '反馈详情' }}</h2><button class="btn-close" type="button" aria-label="关闭" @click="closeDialog"></button></div><div class="modal-body">
      <template v-if="detail"><p><strong>提交用户：</strong>{{ detail.username }}</p><p><strong>提交时间：</strong>{{ detail.createTime || '—' }}</p><p><strong>当前状态：</strong>{{ detail.status }}</p><p><strong>反馈内容：</strong></p><div class="admin-detail-box">{{ detail.content }}</div><p class="mt-3"><strong>管理员处理备注：</strong></p><div class="admin-detail-box">{{ detail.reply || '暂无备注' }}</div></template>
      <form v-else-if="processing" class="row g-3" @submit.prevent="saveDecision"><p><strong>提交用户：</strong>{{ processing.username }}</p><p><strong>反馈内容：</strong>{{ processing.content }}</p><div class="col-md-4"><label class="form-label">处理状态</label><select v-model="decision.status" class="form-select"><option>未处理</option><option>已处理</option><option>已忽略</option></select></div><div class="col-12"><label class="form-label">处理备注</label><textarea v-model="decision.reply" class="form-control" rows="4"></textarea></div><div class="col-12"><button class="btn btn-primary" :disabled="busy">保存处理结果</button></div></form>
    </div></div></div></div><div class="modal-backdrop fade show"></div></template>
  </section>
</template>
