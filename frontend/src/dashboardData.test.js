import test from 'node:test';
import assert from 'node:assert/strict';
import * as data from './dashboardData.js';

test('category failure still delivers plans, tasks and records to the dashboard', async () => {
  const values = { '/dashboard': { taskCount: 1 }, '/plans': [{ id: 2 }], '/tasks': [{ id: 3, category: '课程进度' }], '/records': [{ taskId: 3, duration: 45 }] };
  const state = {};
  const result = await data.loadDashboardData({ get: async path => {
    if (path.endsWith('/available')) throw new Error('分类表未升级');
    return values[path];
  } }, (key, value) => { state[key] = value; });
  assert.equal(state.records[0].duration, 45);
  assert.equal(state.tasks.length, 1);
  assert.equal(state.plans.length, 1);
  assert.equal(result.failures.categories.message, '分类表未升级');
  assert.equal(result.failures.tasks, undefined);
});

test('failed record reload preserves previous data and reports failure instead of zero', async () => {
  const state = { records: [{ duration: 60 }] };
  const result = await data.loadDashboardData({ get: async path => {
    if (path === '/records') throw new Error('记录加载失败');
    return [];
  } }, (key, value) => { state[key] = value; });
  assert.equal(state.records[0].duration, 60);
  assert.ok(result.failures.records);
});

test('authentication errors remain distinguishable when resources load independently', async () => {
  const result = await data.loadDashboardData({ get: async () => { throw Object.assign(new Error('登录过期'), { status: 401 }); } }, () => {});
  assert.equal(result.failures.tasks.status, 401);
});
