const TASK_PRIORITIES = ['Q0', 'Q1', 'Q2', 'Q3'];
const TASK_STATUSES = ['未完成', '进行中', '已完成', '延期'];
const REPEAT_TYPES = ['NONE', 'DAILY', 'WEEKLY', 'WORKDAY'];
const RECORD_STATUSES = ['已完成', '部分完成', '未完成'];

function text(value) {
  return String(value ?? '').trim();
}

function positiveId(value, label) {
  const result = Number(value);
  if (!Number.isInteger(result) || result <= 0) throw new Error(label + '不能为空');
  return result;
}

function date(value, label, required = false) {
  const result = text(value);
  if (!result && !required) return null;
  if (!/^\d{4}-\d{2}-\d{2}$/.test(result)) throw new Error(label + '格式必须是 YYYY-MM-DD');
  const parsed = new Date(result + 'T00:00:00Z');
  if (Number.isNaN(parsed.getTime()) || parsed.toISOString().slice(0, 10) !== result) {
    throw new Error(label + '不是有效日期');
  }
  return result;
}

export function prepareTask(input) {
  const taskName = text(input.taskName);
  if (!taskName) throw new Error('任务名称不能为空');
  if (taskName.length > 100) throw new Error('任务名称不能超过 100 个字符');
  const priority = text(input.priority || 'Q2').toUpperCase();
  if (!TASK_PRIORITIES.includes(priority)) throw new Error('优先级只能是 Q0、Q1、Q2、Q3');
  const status = text(input.status || '未完成');
  if (!TASK_STATUSES.includes(status)) throw new Error('任务状态不是有效选项');
  const repeatType = text(input.repeatType || 'NONE').toUpperCase();
  if (!REPEAT_TYPES.includes(repeatType)) throw new Error('重复规则不是有效选项');
  const reminderDaysBefore = Number(input.reminderDaysBefore ?? 3);
  if (!Number.isInteger(reminderDaysBefore) || reminderDaysBefore < 0 || reminderDaysBefore > 30) {
    throw new Error('提前提醒天数必须在 0 到 30 之间');
  }
  const deadline = date(input.deadline, '截止日期');
  const repeatEndDate = repeatType === 'NONE' ? null : date(input.repeatEndDate, '重复结束日期', true);
  if (repeatType !== 'NONE' && !deadline) throw new Error('重复任务必须填写截止日期');
  if (deadline && repeatEndDate && repeatEndDate < deadline) throw new Error('重复结束日期不能早于截止日期');
  return {
    taskName,
    planId: positiveId(input.planId, '所属计划'),
    category: text(input.category),
    priority,
    deadline,
    status,
    description: text(input.description),
    repeatType,
    repeatEndDate,
    reminderEnabled: String(input.reminderEnabled ?? true) !== 'false',
    reminderDaysBefore
  };
}

export function parseBatchTasks(value, plans, categories) {
  const lines = String(value ?? '').split(/\r?\n/).map(line => line.trim()).filter(Boolean);
  if (!lines.length) throw new Error('请先输入批量任务内容');
  return lines.map((line, index) => {
    const row = index + 1;
    const parts = line.split('|').map(part => part.trim());
    if (parts.length !== 6) throw new Error('第 ' + row + ' 行格式错误：每行必须包含 6 个字段');
    const [taskName, planName, category, priorityText, deadline, description] = parts;
    if (!taskName) throw new Error('第 ' + row + ' 行格式错误：任务名称不能为空');
    const plan = plans.find(item => text(item.planName) === planName);
    if (!plan) throw new Error('第 ' + row + ' 行格式错误：所属计划不存在');
    if (!categories.includes(category)) throw new Error('第 ' + row + ' 行格式错误：分类必须是固定选项之一');
    const priority = priorityText.toUpperCase();
    if (!TASK_PRIORITIES.includes(priority)) throw new Error('第 ' + row + ' 行格式错误：优先级只能是 Q0、Q1、Q2、Q3');
    const validDeadline = date(deadline, '第 ' + row + ' 行截止日期', true);
    return { taskName, planId: Number(plan.id), category, priority, deadline: validDeadline, description, status: '未完成' };
  });
}

export function prepareRecord(input) {
  const content = text(input.content);
  if (!content) throw new Error('学习内容不能为空');
  const duration = Number(input.duration || 0);
  if (!Number.isInteger(duration) || duration < 0) throw new Error('学习时长必须是大于或等于 0 的整数');
  const completionStatus = text(input.completionStatus || '部分完成');
  if (!RECORD_STATUSES.includes(completionStatus)) throw new Error('完成情况不是有效选项');
  return {
    taskId: positiveId(input.taskId, '学习任务'),
    studyDate: date(input.studyDate, '学习日期', true),
    duration,
    content,
    completionStatus,
    problems: text(input.problems),
    improvementPlan: text(input.improvementPlan)
  };
}

export function buildRecordAiRequest(record, tasks, plans) {
  const task = tasks.find(item => String(item.id) === String(record.taskId));
  const plan = task ? plans.find(item => String(item.id) === String(task.planId)) : null;
  const problems = text(record.problems);
  return {
    planId: plan?.id ?? null,
    planName: text(plan?.planName),
    taskName: text(task?.taskName),
    studyContent: text(record.content),
    completion: text(record.completionStatus),
    problems,
    problemSummary: problems
  };
}