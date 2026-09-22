<script setup>
import { reactive, ref } from 'vue';
const props = defineProps({ api: { type: Function, required: true }, initialMessage: { type: String, default: '' } });
const emit = defineEmits(['authenticated', 'clear-message']);
const tab = ref('login');
const busy = ref(false);
const message = ref('');
const failed = ref(false);
let remembered = '';
try { remembered = localStorage.getItem('study-plan-remembered-username') || ''; } catch { /* Storage may be unavailable. */ }
const login = reactive({ username: remembered || 'student', password: '' });
const registration = reactive({ username: '', realName: '', password: '' });
const remember = ref(Boolean(remembered));
function switchTab(next) { if (busy.value) return; tab.value = next; message.value = ''; failed.value = false; emit('clear-message'); }
async function submit() {
  if (busy.value) return;
  busy.value = true; message.value = ''; failed.value = false; emit('clear-message');
  try {
    if (tab.value === 'register') {
      const username = registration.username.trim();
      if (!username) throw new Error('用户名不能为空');
      await props.api.post('/auth/register', { username, realName: registration.realName.trim(), password: registration.password });
      login.username = username; login.password = '';
      registration.password = ''; tab.value = 'login';
      message.value = '注册成功，请使用新账号登录';
    } else {
      const user = await props.api.post('/auth/login', { username: login.username.trim(), password: login.password });
      try {
        if (remember.value) localStorage.setItem('study-plan-remembered-username', login.username.trim());
        else localStorage.removeItem('study-plan-remembered-username');
      } catch { /* Remembering a username must not prevent login. */ }
      login.password = '';
      emit('authenticated', user);
    }
  } catch (error) { message.value = error.message; failed.value = true; }
  finally { busy.value = false; }
}
function passwordHelp() {
  emit('clear-message'); failed.value = false;
  message.value = '请联系管理员协助核实账号。测试学生账号为 student / 123456（如未修改）。';
}
</script>

<template>
  <nav class="navbar navbar-expand-lg app-navbar">
    <div class="container">
      <a class="navbar-brand" href="#" @click.prevent="switchTab('login')">大学生学习计划管理系统</a>
      <span class="navbar-text d-none d-md-inline">学习计划 · 任务执行 · 复盘统计</span>
    </div>
  </nav>
  <main class="login-wrap">
    <section class="login-hero" aria-label="系统介绍">
      <div class="hero-kicker">学习节奏，从这里梳理清楚</div>
      <h1>大学生学习计划管理系统</h1>
      <p class="hero-summary">用一个清晰的工具管理计划、任务、记录与改进。</p>
      <div class="hero-feature-grid">
        <div class="hero-feature-card"><span class="feature-icon">01</span><strong>学习计划管理</strong><small>制定个性化学习计划，合理安排学习时间。</small></div>
        <div class="hero-feature-card"><span class="feature-icon">02</span><strong>任务执行跟踪</strong><small>跟踪任务完成进度，保持学习节奏不掉队。</small></div>
        <div class="hero-feature-card"><span class="feature-icon">03</span><strong>复盘统计分析</strong><small>多维度数据统计分析，复盘总结，持续改进。</small></div>
      </div>
      <p class="hero-footnote">帮助你更清楚地安排目标、追踪进度，并形成持续改进的学习闭环。</p>
    </section>
    <section class="login-card" aria-label="登录注册" :aria-busy="busy">
      <div class="mb-4"><span class="soft-label">课程项目原型</span><h2 class="page-title mt-3">{{ tab === 'login' ? '欢迎回来' : '创建账号' }}</h2><p class="muted-text mb-0">{{ tab === 'login' ? '登录后继续管理你的学习计划与任务。' : '从一个清晰的学习目标开始。' }}</p></div>
      <ul class="nav nav-pills soft-tabs mb-4" id="loginTabs">
        <li class="nav-item"><button class="nav-link" :class="{ active: tab === 'login' }" :aria-pressed="tab === 'login'" :disabled="busy" type="button" @click="switchTab('login')">登录</button></li>
        <li class="nav-item"><button class="nav-link" :class="{ active: tab === 'register' }" :aria-pressed="tab === 'register'" :disabled="busy" type="button" @click="switchTab('register')">注册</button></li>
      </ul>
      <form v-if="tab === 'login'" id="loginForm" class="auth-form" @submit.prevent="submit">
        <fieldset :disabled="busy">
          <div class="mb-3"><label class="form-label" for="loginUsername">用户名</label><input id="loginUsername" v-model="login.username" class="form-control" name="username" placeholder="请输入用户名" required maxlength="50" autocomplete="username"></div>
          <div class="mb-3"><label class="form-label" for="loginPassword">密码</label><input id="loginPassword" v-model="login.password" class="form-control" name="password" type="password" placeholder="请输入密码" required maxlength="100" autocomplete="current-password"></div>
          <div class="d-flex justify-content-between align-items-center mb-4">
            <div class="form-check"><input id="rememberMe" v-model="remember" class="form-check-input" type="checkbox"><label class="form-check-label" for="rememberMe">记住用户名</label></div>
            <a class="soft-link" href="#" @click.prevent="passwordHelp">忘记密码</a>
          </div>
          <button class="btn btn-primary w-100" type="submit">{{ busy ? '登录中…' : '进入系统' }}</button>
        </fieldset>
      </form>
      <form v-else id="registerForm" class="auth-form" @submit.prevent="submit">
        <fieldset :disabled="busy">
          <div class="mb-3"><label class="form-label" for="registerUsername">用户名</label><input id="registerUsername" v-model="registration.username" class="form-control" name="username" placeholder="例如 student02" required maxlength="50" autocomplete="username"></div>
          <div class="mb-3"><label class="form-label" for="registerName">姓名</label><input id="registerName" v-model="registration.realName" class="form-control" name="realName" placeholder="请输入姓名" maxlength="50" autocomplete="name"></div>
          <div class="mb-4"><label class="form-label" for="registerPassword">密码</label><input id="registerPassword" v-model="registration.password" class="form-control" name="password" type="password" placeholder="请设置密码" required maxlength="100" autocomplete="new-password"></div>
          <button class="btn btn-primary w-100" type="submit">{{ busy ? '创建中…' : '创建账号' }}</button>
        </fieldset>
      </form>
      <div v-if="message || initialMessage" id="authMessage" class="mt-4 alert" :class="failed || initialMessage ? 'alert-danger' : 'alert-success'" role="alert">{{ message || initialMessage }}</div>
      <div class="demo-note mt-4">测试账号：student / 123456</div>
    </section>
  </main>
</template>
