import test from 'node:test';
import assert from 'node:assert/strict';

let portal = {};
try {
  portal = await import('./portal.js');
} catch {
  portal = {};
}

test('portal query drops blank filters and preserves meaningful values', () => {
  assert.equal(typeof portal.buildQuery, 'function');
  assert.equal(portal.buildQuery({
    username: ' student ', keyword: '', status: '未完成',
    startDate: null, page: 0
  }), 'username=student&status=%E6%9C%AA%E5%AE%8C%E6%88%90&page=0');
});

test('feedback payload trims content and rejects an empty submission', () => {
  assert.equal(typeof portal.prepareFeedback, 'function');
  assert.deepEqual(portal.prepareFeedback({ content: '  页面很好用  ', status: '已处理' }), {
    content: '页面很好用'
  });
  assert.throws(() => portal.prepareFeedback({ content: '   ' }), /反馈内容不能为空/);
});

test('administrator feedback decision only accepts backend statuses', () => {
  assert.equal(typeof portal.prepareFeedbackDecision, 'function');
  assert.deepEqual(portal.prepareFeedbackDecision({
    status: '已处理', reply: '  已修复  ', content: 'ignore'
  }), { status: '已处理', reply: '已修复' });
  assert.throws(() => portal.prepareFeedbackDecision({ status: '处理中' }), /反馈状态不正确/);
});

test('administrator user status toggles between backend numeric states', () => {
  assert.equal(typeof portal.nextUserStatus, 'function');
  assert.equal(portal.nextUserStatus(1), 0);
  assert.equal(portal.nextUserStatus('0'), 1);
  assert.throws(() => portal.nextUserStatus(2), /用户状态/);
});

test('theme helpers preserve legacy per-user storage and reject unknown themes', () => {
  assert.equal(typeof portal.themeStorageKey, 'function');
  assert.equal(portal.themeStorageKey({ id: 12, username: 'student' }), 'theme_12');
  assert.equal(portal.themeStorageKey({ username: 'student' }), 'theme_student');
  assert.equal(portal.themeStorageKey(null), null);
  assert.equal(portal.resolveTheme('theme-dark').key, 'theme-dark');
  assert.equal(portal.resolveTheme('unknown').key, 'theme-pink');
  assert.equal(portal.THEMES.length, 5);
});
