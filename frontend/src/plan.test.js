import test from 'node:test';
import assert from 'node:assert/strict';
import { preparePlan } from './plan.js';

test('plan payload only includes editable fields and normalizes empty dates', () => {
  assert.deepEqual(preparePlan({ id: 9, userId: 8, totalStudyMinutes: 99, planName: '  期末复习  ', status: '进行中' }), {
    planName: '期末复习', goal: '', description: '', startDate: null, endDate: null, status: '进行中',
  });
});

test('rejects a blank name, oversized DB fields and invalid status before sending', () => {
  const valid = { planName: '复习', status: '进行中' };
  for (const change of [{ planName: ' ' }, { planName: '文'.repeat(101) }, { goal: '文'.repeat(256) }, { status: '任意值' }]) {
    assert.throws(() => preparePlan({ ...valid, ...change }));
  }
});

test('rejects reversed dates and impossible calendar dates', () => {
  assert.throws(() => preparePlan({ planName: '复习', status: '进行中', startDate: '2026-09-22', endDate: '2026-09-21' }));
  assert.throws(() => preparePlan({ planName: '复习', status: '进行中', startDate: '2026-02-30' }));
});

test('cannot silently clear existing dates with the current backend update contract', () => {
  assert.throws(() => preparePlan({ planName: '复习', status: '进行中', startDate: '' }, { startDate: '2026-09-21' }), /日期/);
});

test('preserves an existing legacy status without introducing it for new plans', () => {
  assert.equal(preparePlan({ planName: '复习', status: '未完成' }, { status: '未完成' }).status, '未完成');
  assert.throws(() => preparePlan({ planName: '复习', status: '未完成' }));
});
