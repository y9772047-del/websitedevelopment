<script setup>
import { onMounted, reactive, ref } from 'vue';
import { prepareFeedback } from '../portal.js';

const props = defineProps({ api: { type: Function, required: true } });
const emit = defineEmits(['session-expired', 'busy-change']);
const feedbacks = ref([]);
const form = reactive({ content: '' });
const busy = ref(false);
const error = ref('');
const message = ref('');
const loaded = ref(false);

function handleError(failure) {
  if (failure.status === 401) emit('session-expired');
  else error.value = failure.message;
}
async function run(action) {
  if (busy.value) return;
  busy.value = true; emit('busy-change', true); error.value = ''; message.value = '';
  try { await action(); } catch (failure) { handleError(failure); }
  finally { busy.value = false; emit('busy-change', false); }
}
async function loadFeedbacks() {
  feedbacks.value = await props.api.get('/feedback/my');
  loaded.value = true;
}
function submit() {
  return run(async () => {
    await props.api.post('/feedback', prepareFeedback(form));
    form.content = '';
    message.value = '反馈提交成功';
    await loadFeedbacks();
  });
}
function clear() { if (!busy.value) { form.content = ''; error.value = ''; message.value = ''; } }
function badgeClass(status) {
  if (status === '已处理') return 'status-done';
  if (status === '已忽略') return 'status-delay';
  return 'status-waiting';
}
function refresh() { return run(loadFeedbacks); }
defineExpose({ refresh });
onMounted(refresh);
</script>

<template>
  <main class="container-fluid app-shell" :aria-busy="busy">
    <p v-if="error" class="alert alert-danger" role="alert">{{ error }}</p>
    <p v-if="message" class="alert alert-success" role="status">{{ message }}</p>
    <p v-if="busy" class="muted-text" role="status">正在处理，请稍候…</p>
    <section id="feedback" class="page-section">
      <div class="page-heading"><div><span class="soft-label">Feedback</span><h1 class="page-title">意见反馈</h1><p class="muted-text mt-2 mb-0">提交使用建议或遇到的问题，管理员会在后台处理。</p></div></div>
      <div class="content-card mb-4">
        <h2>提交反馈</h2>
        <form class="row g-3" @submit.prevent="submit">
          <div class="col-12"><label class="form-label" for="vueFeedbackContent">反馈内容</label><textarea id="vueFeedbackContent" v-model="form.content" class="form-control" rows="4" maxlength="2000" placeholder="请描述你的建议或遇到的问题" required></textarea></div>
          <div class="col-12 d-flex gap-2"><button class="btn btn-primary" :disabled="busy">提交反馈</button><button class="btn btn-outline-primary" type="button" :disabled="busy" @click="clear">清空</button></div>
        </form>
      </div>
      <div class="content-card">
        <div class="card-title-row"><h2>我的反馈</h2><button class="btn btn-outline-primary btn-sm" :disabled="busy" @click="refresh">刷新</button></div>
        <div class="table-responsive"><table class="table app-table align-middle">
          <thead><tr><th>反馈内容</th><th>状态</th><th>管理员回复</th><th>提交时间</th></tr></thead>
          <tbody>
            <tr v-for="item in feedbacks" :key="item.id"><td>{{ item.content }}</td><td><span class="badge" :class="badgeClass(item.status)">{{ item.status }}</span></td><td>{{ item.reply || '暂无回复' }}</td><td>{{ item.createTime || '—' }}</td></tr>
            <tr v-if="!feedbacks.length"><td colspan="4" class="muted-text">{{ busy ? '正在加载反馈…' : loaded ? '暂无反馈' : '反馈尚未加载' }}</td></tr>
          </tbody>
        </table></div>
      </div>
    </section>
  </main>
</template>
