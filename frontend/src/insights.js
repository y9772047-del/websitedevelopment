const NODE_TYPES = ['考试', '截止', '比赛', '汇报', '其他'];
const RULE_EFFECTS = ['SCORE', 'MIN_IMPORTANT', 'URGENT'];
const PRIORITIES = ['Q0', 'Q1', 'Q2', 'Q3'];

function text(value) {
  return String(value ?? '').trim();
}

function validDate(value, label, required = false) {
  const result = text(value);
  if (!result && !required) return null;
  if (!/^\d{4}-\d{2}-\d{2}$/.test(result)) throw new Error(label + '格式必须是 YYYY-MM-DD');
  const parsed = new Date(result + 'T00:00:00Z');
  if (Number.isNaN(parsed.getTime()) || parsed.toISOString().slice(0, 10) !== result) {
    throw new Error(label + '不是有效日期');
  }
  return result;
}

function validTime(value, label) {
  const result = text(value);
  if (!result) return null;
  if (!/^([01]\d|2[0-3]):[0-5]\d$/.test(result)) throw new Error(label + '格式必须是 HH:mm');
  return result;
}

function optionalId(value) {
  if (value === null || value === undefined || text(value) === '') return null;
  const result = Number(value);
  if (!Number.isInteger(result) || result <= 0) throw new Error('关联编号必须是正整数');
  return result;
}

function isoDate(date) {
  return date.toISOString().slice(0, 10);
}

export function prepareImportantNode(input) {
  const title = text(input.title);
  if (!title) throw new Error('节点标题不能为空');
  if (title.length > 120) throw new Error('节点标题不能超过 120 个字符');
  const type = text(input.type);
  if (!NODE_TYPES.includes(type)) throw new Error('类型只能填写：考试、截止、比赛、汇报、其他');
  const startDate = validDate(input.startDate, '开始日期', true);
  const endDate = validDate(input.endDate || startDate, '结束日期', true);
  if (endDate < startDate) throw new Error('结束日期不能早于开始日期');
  const startTime = validTime(input.startTime, '开始时间');
  const endTime = validTime(input.endTime, '结束时间');
  if (startDate === endDate && startTime && endTime && endTime < startTime) {
    throw new Error('结束时间不能早于开始时间');
  }
  return {
    title,
    type,
    startDate,
    endDate,
    startTime,
    endTime,
    planId: optionalId(input.planId),
    taskId: optionalId(input.taskId),
    description: text(input.description)
  };
}

export function parseImportantNodeBatch(value) {
  const lines = String(value ?? '').split(/\r?\n/).map(line => line.trim()).filter(Boolean);
  if (!lines.length) throw new Error('请先输入批量节点内容');
  return lines.map((line, index) => {
    const parts = line.split('|').map(part => part.trim());
    const row = index + 1;
    if (parts.length !== 7) throw new Error('第 ' + row + ' 行格式错误：每行必须包含 7 个字段');
    try {
      return prepareImportantNode({
        title: parts[0], type: parts[1], startDate: parts[2], startTime: parts[3],
        endDate: parts[4], endTime: parts[5], description: parts[6]
      });
    } catch (error) {
      throw new Error('第 ' + row + ' 行格式错误：' + error.message);
    }
  });
}

export function prepareKeywordRule(input) {
  const keyword = text(input.keyword);
  if (!keyword) throw new Error('关键词不能为空');
  if (keyword.length > 60) throw new Error('关键词不能超过 60 个字符');
  const nodeType = text(input.nodeType) || null;
  if (nodeType && !NODE_TYPES.includes(nodeType)) throw new Error('适用类型不是有效选项');
  const effect = text(input.effect || 'SCORE').toUpperCase();
  if (!RULE_EFFECTS.includes(effect)) throw new Error('影响方式不是有效选项');
  let scoreDelta = 0;
  if (effect === 'SCORE') {
    scoreDelta = Number(input.scoreDelta ?? 10);
    if (!Number.isInteger(scoreDelta) || scoreDelta < 1 || scoreDelta > 50) {
      throw new Error('加分值必须在 1 到 50 之间');
    }
  }
  return {
    keyword,
    nodeType,
    effect,
    scoreDelta,
    enabled: String(input.enabled ?? true) !== 'false'
  };
}

export function buildCalendarMonth(year, month, records = [], nodes = []) {
  const first = new Date(Date.UTC(year, month - 1, 1));
  const last = new Date(Date.UTC(year, month, 0));
  const recordMap = new Map();
  records.forEach(record => {
    const date = text(record.studyDate);
    if (date < isoDate(first) || date > isoDate(last)) return;
    if (!recordMap.has(date)) recordMap.set(date, []);
    recordMap.get(date).push(record);
  });
  const nodeMap = new Map();
  nodes.forEach(node => {
    const startText = text(node.startDate);
    const endText = text(node.endDate || node.startDate);
    if (!startText || !endText) return;
    let cursor = new Date(Math.max(new Date(startText + 'T00:00:00Z').getTime(), first.getTime()));
    const end = new Date(Math.min(new Date(endText + 'T00:00:00Z').getTime(), last.getTime()));
    while (cursor <= end) {
      const date = isoDate(cursor);
      if (!nodeMap.has(date)) nodeMap.set(date, []);
      nodeMap.get(date).push(node);
      cursor = new Date(cursor.getTime() + 86400000);
    }
  });
  const days = [];
  for (let day = 1; day <= last.getUTCDate(); day++) {
    const date = isoDate(new Date(Date.UTC(year, month - 1, day)));
    days.push({ date, day, records: recordMap.get(date) || [], nodes: nodeMap.get(date) || [] });
  }
  return days;
}

export function summarizeTasks(tasks = [], today = isoDate(new Date())) {
  const statusCounts = {};
  const priorityCounts = Object.fromEntries(PRIORITIES.map(value => [value, 0]));
  let completed = 0;
  let overdue = 0;
  tasks.forEach(task => {
    const done = task.status === '已完成';
    const delayed = !done && text(task.deadline) && task.deadline < today;
    const status = delayed ? '延期' : (text(task.status) || '未完成');
    statusCounts[status] = (statusCounts[status] || 0) + 1;
    const priority = PRIORITIES.includes(task.priority) ? task.priority : 'Q2';
    priorityCounts[priority]++;
    if (done) completed++;
    if (delayed) overdue++;
  });
  const total = tasks.length;
  return {
    total,
    completed,
    overdue,
    completionRate: total ? Math.round(completed * 10000 / total) / 100 : 0,
    statusCounts,
    priorityCounts
  };
}

export function calculatePlanInvestment(records = [], tasks = [], plans = [], startDate = null) {
  const taskPlans = new Map(tasks.map(task => [String(task.id), task.planId]));
  const planNames = new Map(plans.map(plan => [String(plan.id), text(plan.planName) || '未命名计划']));
  const totals = new Map();
  records.forEach(record => {
    if (startDate && text(record.studyDate) < startDate) return;
    const planId = taskPlans.get(String(record.taskId));
    if (planId === undefined || planId === null) return;
    const minutes = Number(record.duration || 0);
    if (!Number.isFinite(minutes) || minutes <= 0) return;
    totals.set(String(planId), (totals.get(String(planId)) || 0) + minutes);
  });
  const total = [...totals.values()].reduce((sum, value) => sum + value, 0);
  return [...totals.entries()]
    .map(([planId, minutes]) => ({
      planId: Number(planId),
      label: planNames.get(planId) || '未知计划',
      minutes,
      percent: total ? Math.round(minutes * 10000 / total) / 100 : 0
    }))
    .sort((left, right) => right.minutes - left.minutes);
}
const HEATMAP_CATEGORIES = {
  '课业完成型': ['课程进度', '作业实验'],
  '成绩提升型': ['考试备考', '复盘总结'],
  '成长拓展型': ['竞赛科研', '专业能力'],
  '综合提升型': ['自我提升']
};

export function buildDashboardReminders(tasks = [], today = isoDate(new Date())) {
  const todayTime = Date.parse(today + 'T00:00:00Z');
  return tasks.flatMap(task => {
    if (task.status === '已完成' || !/^\d{4}-\d{2}-\d{2}$/.test(text(task.deadline))) return [];
    const days = Math.round((Date.parse(task.deadline + 'T00:00:00Z') - todayTime) / 86400000);
    if (!Number.isFinite(days) || days > 3) return [];
    return [{ task, days, label: days < 0 ? '已逾期' : days === 0 ? '今日待办' : days + ' 天内截止',
      className: days < 0 ? 'status-delay' : days === 0 ? 'status-waiting' : 'status-doing' }];
  }).sort((left, right) => Math.sign(left.days) - Math.sign(right.days)
    || String(left.task.deadline).localeCompare(String(right.task.deadline))).slice(0, 6);
}

export function buildDashboardHeatmap(records = [], tasks = [], filter = '全部', today = isoDate(new Date())) {
  const end = Date.parse(today + 'T00:00:00Z');
  const start = end - 179 * 86400000;
  const firstWeekday = (new Date(start).getUTCDay() + 6) % 7;
  const displayStart = start - firstWeekday * 86400000;
  const lastWeekday = (new Date(end).getUTCDay() + 6) % 7;
  const displayEnd = end + (6 - lastWeekday) * 86400000;
  const taskMap = new Map(tasks.map(task => [String(task.id), task]));
  const allowed = HEATMAP_CATEGORIES[filter];
  const totals = new Map();
  records.forEach(record => {
    const time = Date.parse(text(record.studyDate) + 'T00:00:00Z');
    if (!Number.isFinite(time) || time < start || time > end) return;
    const category = taskMap.get(String(record.taskId))?.category || '其他任务';
    if (allowed && !allowed.includes(category)) return;
    const current = totals.get(record.studyDate) || { minutes: 0, count: 0 };
    current.minutes += Number(record.duration) || 0;
    current.count++;
    totals.set(record.studyDate, current);
  });
  const cells = [];
  for (let time = displayStart; time <= displayEnd; time += 86400000) {
    const date = isoDate(new Date(time));
    const inRange = time >= start && time <= end;
    const current = inRange ? totals.get(date) : null;
    const minutes = current?.minutes || 0;
    cells.push({ date, weekdayIndex: (new Date(time).getUTCDay() + 6) % 7, inRange,
      minutes, count: current?.count || 0,
      level: !minutes ? 0 : minutes <= 60 ? 1 : minutes <= 120 ? 2 : minutes <= 240 ? 3 : minutes <= 360 ? 4 : 5 });
  }
  return cells;
}

export function formatImportantNodeDateTime(node) {
  const startDate = text(node.startDate);
  if (!startDate) return '';
  const endDate = text(node.endDate);
  const crossYear = Boolean(endDate && startDate.slice(0, 4) !== endDate.slice(0, 4));
  const dateLabel = value => crossYear ? value.replaceAll('-', '.') : value.slice(5).replace('-', '.');
  const startTime = text(node.startTime).slice(0, 5);
  const endTime = text(node.endTime).slice(0, 5);
  const start = dateLabel(startDate) + (startTime ? ' ' + startTime : '');
  if (!endDate) return start;
  if (endDate === startDate) return startTime && endTime ? start + '-' + endTime : start;
  return start + ' ~ ' + dateLabel(endDate) + (endTime ? ' ' + endTime : '');
}

export function groupImportantNodesByMonth(nodes = []) {
  const grouped = new Map();
  [...nodes].sort((a, b) => (text(a.startDate) + ' ' + text(a.startTime)).localeCompare(text(b.startDate) + ' ' + text(b.startTime)))
    .forEach(node => {
      const month = Number(text(node.startDate).slice(5, 7));
      if (!Number.isInteger(month) || month < 1 || month > 12) return;
      if (!grouped.has(month)) grouped.set(month, []);
      grouped.get(month).push(node);
    });
  return [...grouped.entries()].sort((a, b) => a[0] - b[0]).map(([month, monthNodes]) => ({ month, nodes: monthNodes }));
}