import test from 'node:test';
import assert from 'node:assert/strict';

let workflow = {};
try {
  workflow = await import('./workflow.js');
} catch {
  workflow = {};
}

test('task payload keeps editable fields and normalizes defaults', () => {
  assert.equal(typeof workflow.prepareTask, 'function');
  assert.deepEqual(workflow.prepareTask({
    id: 99, userId: 8, taskName: '  完成实验  ', planId: '2', category: '作业实验',
    priority: 'Q1', deadline: '', status: '未完成', description: ' 说明 ',
    repeatType: '', repeatEndDate: '', reminderEnabled: 'false', reminderDaysBefore: '5'
  }), {
    taskName: '完成实验', planId: 2, category: '作业实验', priority: 'Q1',
    deadline: null, status: '未完成', description: '说明', repeatType: 'NONE',
    repeatEndDate: null, reminderEnabled: false, reminderDaysBefore: 5
  });
});

test('repeating tasks require a valid end date and reminder range', () => {
  assert.throws(() => workflow.prepareTask({
    taskName: '复习', planId: 1, category: '课程进度', priority: 'Q2',
    deadline: '2026-09-30', status: '进行中', repeatType: 'WEEKLY',
    repeatEndDate: '2026-09-29', reminderDaysBefore: 3
  }), /重复结束日期/);
  assert.throws(() => workflow.prepareTask({
    taskName: '复习', planId: 1, category: '课程进度', priority: 'Q2',
    status: '进行中', repeatType: 'NONE', reminderDaysBefore: 31
  }), /0 到 30/);
});

test('batch task parser resolves plan names and validates every row', () => {
  assert.equal(typeof workflow.parseBatchTasks, 'function');
  const tasks = workflow.parseBatchTasks(
    '完成报告 | 实验计划 | 作业实验 | q0 | 2026-09-30 | 完成实验二',
    [{ id: 7, planName: '实验计划' }],
    ['作业实验']
  );
  assert.deepEqual(tasks, [{
    taskName: '完成报告', planId: 7, category: '作业实验', priority: 'Q0',
    deadline: '2026-09-30', description: '完成实验二', status: '未完成'
  }]);
  assert.throws(() => workflow.parseBatchTasks(
    '错误任务 | 不存在 | 作业实验 | Q1 | 2026-09-30 | 说明',
    [{ id: 7, planName: '实验计划' }],
    ['作业实验']
  ), /第 1 行.*计划不存在/);
});

test('record payload converts numeric fields and rejects missing content', () => {
  assert.equal(typeof workflow.prepareRecord, 'function');
  assert.deepEqual(workflow.prepareRecord({
    id: 3, userId: 2, taskId: '8', studyDate: '2026-09-22', duration: '45',
    content: ' 复习接口 ', completionStatus: '部分完成', problems: ' 无 ',
    improvementPlan: '继续练习'
  }), {
    taskId: 8, studyDate: '2026-09-22', duration: 45, content: '复习接口',
    completionStatus: '部分完成', problems: '无', improvementPlan: '继续练习'
  });
  assert.throws(() => workflow.prepareRecord({
    taskId: 8, studyDate: '2026-09-22', duration: -1, content: ''
  }), /学习内容/);
});

test('AI request derives plan and task context without private fields', () => {
  assert.equal(typeof workflow.buildRecordAiRequest, 'function');
  assert.deepEqual(workflow.buildRecordAiRequest(
    { taskId: 8, content: '控制器', completionStatus: '部分完成', problems: '跨域失败' },
    [{ id: 8, planId: 7, taskName: '完成实验' }],
    [{ id: 7, planName: '实验计划' }]
  ), {
    planId: 7, planName: '实验计划', taskName: '完成实验',
    studyContent: '控制器', completion: '部分完成',
    problems: '跨域失败', problemSummary: '跨域失败'
  });
});