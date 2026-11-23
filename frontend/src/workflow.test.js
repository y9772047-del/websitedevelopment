import test from 'node:test';
import assert from 'node:assert/strict';

let workflow = {};
try {
  workflow = await import('./workflow.js');
} catch {
  workflow = {};
}

test('category edit payload trims names and rejects blank or oversized names', () => {
  assert.deepEqual(workflow.prepareCategory({ categoryName: '  阅读  ', userId: 999 }), { categoryName: '阅读' });
  assert.throws(() => workflow.prepareCategory({ categoryName: '   ' }), /分类名称/);
  assert.throws(() => workflow.prepareCategory({ categoryName: '字'.repeat(51) }), /50/);
});

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

test('quick capture parses a plan with multiple tasks and their records', () => {
  assert.equal(typeof workflow.prepareQuickCapture, 'function');
  assert.deepEqual(workflow.prepareQuickCapture({
    planName: '考试复习', goal: '通过考试', startDate: '2026-11-22', endDate: '2026-12-10',
    tasks: [{
      taskName: '复习需求分析', category: '课程进度', deadline: '2026-11-25',
      record: { studyDate: '2026-11-22', duration: '45', content: '整理章节', problems: '图还不熟' }
    }]
  }), {
    plan: { planName: '考试复习', goal: '通过考试', description: '', startDate: '2026-11-22', endDate: '2026-12-10', status: '进行中' },
    tasks: [{
      task: { taskName: '复习需求分析', category: '课程进度', priority: 'Q2', deadline: '2026-11-25',
      status: '未完成', description: '', repeatType: 'NONE', repeatEndDate: null, reminderEnabled: true, reminderDaysBefore: 3 },
      record: { studyDate: '2026-11-22', duration: 45, content: '整理章节', completionStatus: '部分完成', problems: '图还不熟', improvementPlan: '' }
    }]
  });
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


test('text capture associates records and rejects unknown categories and malformed lines', () => {
  const source = '目标：通过考试\n计划：复习\n时间：2026-11-22 至 2026-12-10\n任务：章节一\n分类：阅读\n状态：已完成\n记录：整理章节\n问题：不熟悉\n反思：再练习\n任务：章节二';
  const result = workflow.parseQuickCapture(source, ['阅读', '其他任务'], '2026-11-22');
  assert.equal(result.plan.goal, '通过考试');
  assert.equal(result.tasks.length, 2);
  assert.equal(result.tasks[0].record.improvementPlan, '再练习');
  assert.equal(result.tasks[0].record.studyDate, '2026-11-22');
  assert.equal(result.tasks[0].record.completionStatus, '已完成');
  assert.equal(result.tasks[1].record, undefined);
  assert.throws(() => workflow.parseQuickCapture(source, ['其他任务']), /分类/);
  assert.throws(() => workflow.parseQuickCapture(source + '\n错别字段：内容', ['阅读', '其他任务']), /行/);
});
