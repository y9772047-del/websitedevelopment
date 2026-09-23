import test from 'node:test';
import assert from 'node:assert/strict';

let insights = {};
try {
  insights = await import('./insights.js');
} catch {
  insights = {};
}

test('important node payload keeps editable fields and defaults a single-day event', () => {
  assert.equal(typeof insights.prepareImportantNode, 'function');
  assert.deepEqual(insights.prepareImportantNode({
    id: 99, userId: 8, title: ' 六级考试 ', type: '考试',
    startDate: '2026-12-12', endDate: '', startTime: '15:00', endTime: '17:00',
    planId: '7', taskId: '', description: ' 带准考证 ', importance: '紧急'
  }), {
    title: '六级考试', type: '考试', startDate: '2026-12-12', endDate: '2026-12-12',
    startTime: '15:00', endTime: '17:00', planId: 7, taskId: null, description: '带准考证'
  });
});

test('important node rejects reversed dates and reversed same-day times', () => {
  assert.throws(() => insights.prepareImportantNode({
    title: '答辩', type: '汇报', startDate: '2026-10-02', endDate: '2026-10-01'
  }), /结束日期/);
  assert.throws(() => insights.prepareImportantNode({
    title: '答辩', type: '汇报', startDate: '2026-10-01', endDate: '2026-10-01',
    startTime: '16:00', endTime: '15:00'
  }), /结束时间/);
});

test('important node batch parser validates each seven-field row', () => {
  assert.equal(typeof insights.parseImportantNodeBatch, 'function');
  assert.deepEqual(insights.parseImportantNodeBatch(
    '数模国赛 | 比赛 | 2026-09-10 | 09:00 | 2026-09-13 | 18:00 | 携带电脑'
  ), [{
    title: '数模国赛', type: '比赛', startDate: '2026-09-10', startTime: '09:00',
    endDate: '2026-09-13', endTime: '18:00', description: '携带电脑',
    planId: null, taskId: null
  }]);
  assert.throws(() => insights.parseImportantNodeBatch(
    '错误节点 | 聚会 | 2026-09-10 | 09:00 | 2026-09-10 | 10:00 | 说明'
  ), /第 1 行.*类型/);
});

test('keyword rule payload applies effect-specific score behavior', () => {
  assert.equal(typeof insights.prepareKeywordRule, 'function');
  assert.deepEqual(insights.prepareKeywordRule({
    id: 4, userId: 2, keyword: ' XCPC ', nodeType: '', effect: 'URGENT',
    scoreDelta: 30, enabled: 'false'
  }), {
    keyword: 'XCPC', nodeType: null, effect: 'URGENT', scoreDelta: 0, enabled: false
  });
  assert.throws(() => insights.prepareKeywordRule({
    keyword: '比赛', nodeType: '比赛', effect: 'SCORE', scoreDelta: 51
  }), /1 到 50/);
});

test('calendar month model includes records and every day of a spanning node', () => {
  assert.equal(typeof insights.buildCalendarMonth, 'function');
  const result = insights.buildCalendarMonth(2026, 9, [
    { id: 1, studyDate: '2026-09-11', completionStatus: '已完成', duration: 60 }
  ], [
    { id: 2, title: '国赛', startDate: '2026-09-10', endDate: '2026-09-12' }
  ]);
  assert.equal(result.length, 30);
  assert.equal(result[9].date, '2026-09-10');
  assert.deepEqual(result[9].nodes.map(item => item.id), [2]);
  assert.deepEqual(result[10].records.map(item => item.id), [1]);
  assert.deepEqual(result[11].nodes.map(item => item.id), [2]);
});

test('task summary treats completed overdue tasks as completed rather than overdue', () => {
  assert.equal(typeof insights.summarizeTasks, 'function');
  assert.deepEqual(insights.summarizeTasks([
    { status: '已完成', priority: 'Q1', deadline: '2026-09-01' },
    { status: '进行中', priority: 'Q0', deadline: '2026-09-01' },
    { status: '未完成', priority: 'Q2', deadline: '2026-09-30' }
  ], '2026-09-23'), {
    total: 3, completed: 1, overdue: 1, completionRate: 33.33,
    statusCounts: { '已完成': 1, '延期': 1, '未完成': 1 },
    priorityCounts: { Q0: 1, Q1: 1, Q2: 1, Q3: 0 }
  });
});

test('plan investment ignores records outside the range and resolves task-plan relations', () => {
  assert.equal(typeof insights.calculatePlanInvestment, 'function');
  assert.deepEqual(insights.calculatePlanInvestment(
    [
      { taskId: 10, studyDate: '2026-09-20', duration: 40 },
      { taskId: 11, studyDate: '2026-08-01', duration: 90 }
    ],
    [{ id: 10, planId: 3 }, { id: 11, planId: 4 }],
    [{ id: 3, planName: '实验二' }, { id: 4, planName: '英语六级' }],
    '2026-09-01'
  ), [{ planId: 3, label: '实验二', minutes: 40, percent: 100 }]);
});