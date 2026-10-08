import test from 'node:test';
import assert from 'node:assert/strict';
import { buildPlanTree } from './planTree.js';

test('groups only records under their own task and calculates plan progress', () => {
  const plans = [{ id: 1, planName: '英语' }, { id: 2, planName: '数学' }];
  const tasks = [{ id: 11, planId: 1, status: '已完成' }, { id: 12, planId: 1, status: '进行中' }, { id: 21, planId: 2, status: '进行中' }];
  const records = [{ id: 1, taskId: 11, duration: 30 }, { id: 2, taskId: 12, duration: 20 }, { id: 3, taskId: 21, duration: 60 }];
  const tree = buildPlanTree(plans, tasks, records);
  assert.equal(tree[0].completedTasks, 1);
  assert.equal(tree[0].progress, 50);
  assert.equal(tree[0].minutes, 50);
  assert.deepEqual(tree[0].tasks.map(task => task.records.map(record => record.id)), [[1], [2]]);
  assert.equal(tree[1].minutes, 60);
});

test('empty plan has zero progress', () => {
  assert.deepEqual(buildPlanTree([{ id: 1 }], [], [])[0].progress, 0);
});
