<script setup>
import { onMounted, ref } from 'vue';

const props = defineProps({ api: { type: Function, required: true } });
const emit = defineEmits(['session-expired', 'busy-change']);
const data = ref({});
const busy = ref(false);
const error = ref('');
const metrics = [
  ['userCount', '用户总数'], ['taskCount', '学习任务总数'], ['finishedTaskCount', '已完成任务数'],
  ['unfinishedTaskCount', '未完成任务数'], ['overdueTaskCount', '逾期未完成任务数'],
  ['todayNewTaskCount', '今日新增任务数'], ['feedbackCount', '用户反馈总数'],
  ['pendingFeedbackCount', '未处理反馈数量']
];
function badgeClass(status) {
  if (status === '已完成' || status === '已处理' || Number(status) === 1) return 'status-done';
  if (status === '已忽略' || status === '延期' || Number(status) === 0) return 'status-delay';
  return 'status-waiting';
}
function roleText(role) { return role === 'admin' ? '管理员' : '普通学生'; }
function statusText(status) { return Number(status) === 1 ? '正常' : '禁用'; }
function shortText(text, length = 24) { const value = String(text || ''); return value.length > length ? value.slice(0, length) + '…' : value; }
async function refresh() {
  if (busy.value) return;
  busy.value = true; emit('busy-change', true); error.value = '';
  try { data.value = await props.api.get('/admin/dashboard/stats'); }
  catch (failure) { if (failure.status === 401 || failure.status === 403) emit('session-expired'); else error.value = failure.message; }
  finally { busy.value = false; emit('busy-change', false); }
}
defineExpose({ refresh });
onMounted(refresh);
</script>

<template>
  <section class="admin-section" :aria-busy="busy">
    <div class="page-heading"><div><span class="soft-label">Admin</span><h1 class="page-title">后台首页</h1></div><button class="btn btn-outline-primary btn-sm" :disabled="busy" @click="refresh">刷新</button></div>
    <p v-if="error" class="alert alert-danger" role="alert">{{ error }}</p>
    <p v-if="busy" class="muted-text" role="status">正在加载后台数据…</p>
    <div class="row g-3 mb-4"><div v-for="[key, label] in metrics" :key="key" class="col-sm-6 col-xl-3"><div class="metric-card"><span>{{ label }}</span><strong>{{ data[key] || 0 }}</strong></div></div></div>
    <div class="row g-4">
      <div class="col-lg-6"><div class="content-card"><h2>最近注册用户</h2><div class="stack-list">
        <div v-for="item in data.recentUsers || []" :key="item.id" class="list-row"><div><h3>{{ item.username }}</h3><p>{{ roleText(item.role) }} · {{ item.createTime || '—' }}</p></div><span class="badge" :class="badgeClass(item.status)">{{ statusText(item.status) }}</span></div>
        <div v-if="!(data.recentUsers || []).length" class="list-row"><p>暂无用户</p></div>
      </div></div></div>
      <div class="col-lg-6"><div class="content-card"><h2>最近提交反馈</h2><div class="stack-list">
        <div v-for="item in data.recentFeedbacks || []" :key="item.id" class="list-row"><div><h3>{{ shortText(item.content) }}</h3><p>{{ item.createTime || '—' }}</p></div><span class="badge" :class="badgeClass(item.status)">{{ item.status }}</span></div>
        <div v-if="!(data.recentFeedbacks || []).length" class="list-row"><p>暂无反馈</p></div>
      </div></div></div>
    </div>
  </section>
</template>
