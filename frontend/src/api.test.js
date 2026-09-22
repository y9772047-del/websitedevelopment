import test from 'node:test';
import assert from 'node:assert/strict';
import { createApi } from './api.js';

test('uses credentials and unwraps the existing Result envelope', async () => {
  const api = createApi('http://localhost:8080/api');
  api.defaults.adapter = async config => {
    assert.equal(config.withCredentials, true);
    assert.equal(config.baseURL, 'http://localhost:8080/api');
    return { data: { success: true, data: [{ id: 1 }] }, status: 200, config };
  };
  assert.deepEqual(await api.get('/plans'), [{ id: 1 }]);
});
test('business failures do not appear as successful HTTP requests', async () => {
  const api = createApi('/api');
  api.defaults.adapter = async config => ({ data: { success: false, message: '计划不存在', data: null }, status: 200, config });
  await assert.rejects(api.get('/plans/99'), /计划不存在/);
});
test('session expiry is distinguishable from network failure', async () => {
  const api = createApi('/api');
  api.defaults.adapter = async () => { throw { response: { status: 401, data: { message: '请先登录' } } }; };
  await assert.rejects(api.get('/plans'), error => error.status === 401 && error.message === '请先登录');
});

test('connection failures tell the user which backend to start', async () => {
  const api = createApi('http://localhost:8080/api');
  api.defaults.adapter = async () => { throw { code: 'ERR_NETWORK' }; };
  await assert.rejects(api.get('/auth/current'), error =>
    error.message.includes('localhost:8080') && error.message.includes('后端'));
});

test('timeouts are reported separately from an unreachable backend', async () => {
  const api = createApi('http://localhost:8080/api');
  api.defaults.adapter = async () => { throw { code: 'ECONNABORTED' }; };
  await assert.rejects(api.get('/plans'), /超时/);
});
