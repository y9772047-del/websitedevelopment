<script setup>
import { onMounted, ref } from 'vue';
import { THEMES, resolveTheme, themeStorageKey } from '../portal.js';

const props = defineProps({ user: { type: Object, required: true }, embedded: { type: Boolean, default: false } });
const emit = defineEmits(['theme-change']);
const savedKey = ref('theme-pink');
const previewKey = ref('theme-pink');
const notice = ref('');
let noticeTimer;

function storageKey() { return themeStorageKey(props.user); }
function readSaved() {
  let key = 'theme-pink';
  try { key = localStorage.getItem(storageKey()) || key; } catch {}
  savedKey.value = resolveTheme(key).key;
  previewKey.value = savedKey.value;
  emit('theme-change', previewKey.value);
}
function notify(text) {
  notice.value = text;
  clearTimeout(noticeTimer);
  noticeTimer = setTimeout(() => { notice.value = ''; }, 1800);
}
function preview(key) {
  previewKey.value = resolveTheme(key).key;
  emit('theme-change', previewKey.value);
  notify('正在预览：' + resolveTheme(key).name);
}
function use(key) {
  const theme = resolveTheme(key);
  try { localStorage.setItem(storageKey(), theme.key); } catch {}
  savedKey.value = theme.key; previewKey.value = theme.key;
  emit('theme-change', theme.key);
  notify('已切换为：' + theme.name);
}
function refresh() { readSaved(); }
defineExpose({ refresh });
onMounted(readSaved);
</script>

<template>
  <main :class="embedded ? '' : 'container-fluid app-shell'">
    <p v-if="notice" class="alert alert-success" role="status">{{ notice }}</p>
    <section id="themes" class="page-section">
      <div class="page-heading"><div><span class="soft-label">Theme Center</span><h1 class="page-title">主题皮肤中心</h1><p class="muted-text mt-2 mb-0">选择喜欢的界面风格，打造更适合自己的学习环境。</p></div></div>
      <div class="content-card mb-4"><div class="card-title-row"><h2>当前使用主题</h2><span class="badge soft-badge">{{ resolveTheme(savedKey).name }}</span></div><p class="muted-text mb-0">{{ resolveTheme(savedKey).desc }}</p></div>
      <div class="row g-4">
        <div v-for="theme in THEMES" :key="theme.key" class="col-md-6 col-xl-4">
          <div class="content-card theme-card" :class="{ 'active-theme': theme.key === savedKey }">
            <div class="card-title-row"><h2>{{ theme.name }}</h2><span v-if="theme.key === savedKey" class="badge soft-badge">使用中</span></div>
            <p class="muted-text">{{ theme.desc }}</p>
            <div class="theme-swatch"><span :style="{ background: theme.primary }"></span><span :style="{ background: theme.hover }"></span><span :style="{ background: theme.background }"></span><span :style="{ background: theme.border }"></span></div>
            <div class="d-flex gap-2"><button class="btn btn-outline-primary btn-sm" type="button" @click="preview(theme.key)">预览</button><button class="btn btn-primary btn-sm" type="button" @click="use(theme.key)">立即使用</button></div>
          </div>
        </div>
      </div>
    </section>
  </main>
</template>
