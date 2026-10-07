import test from 'node:test';
import assert from 'node:assert/strict';
import { filterRecords } from './records.js';

const tasks = [
  { id: 11, planId: 1 },
  { id: 12, planId: 2 },
];
const records = [
  { id: 1, taskId: 11, studyDate: '2026-10-01', completionStatus: '已完成' },
  { id: 2, taskId: 11, studyDate: '2026-10-02', completionStatus: '未完成' },
  { id: 3, taskId: 12, studyDate: '2026-10-01', completionStatus: '已完成' },
];

test('record filters combine plan, task, completion, and date', () => {
  assert.deepEqual(filterRecords(records, tasks, { planId: '1', taskId: '11', completionStatus: '已完成', date: '2026-10-01' }).map(record => record.id), [1]);
});

test('empty filters show every record and missing tasks do not match a plan', () => {
  const orphan = { id: 4, taskId: 99, studyDate: '2026-10-01', completionStatus: '已完成' };
  assert.deepEqual(filterRecords([...records, orphan], tasks, {}).map(record => record.id), [1, 2, 3, 4]);
  assert.deepEqual(filterRecords([...records, orphan], tasks, { planId: '1' }).map(record => record.id), [1, 2]);
});
