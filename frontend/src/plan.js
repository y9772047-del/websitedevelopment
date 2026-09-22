export const PLAN_STATUSES = ['未开始', '进行中', '已完成', '已取消'];
export const emptyPlan = () => ({ planName: '', goal: '', description: '', startDate: '', endDate: '', status: '进行中' });

export function planStatuses(original) {
  const previous = original?.status;
  return previous && !PLAN_STATUSES.includes(previous) ? [...PLAN_STATUSES, previous] : PLAN_STATUSES;
}

export function preparePlan(input, original = null) {
  const planName = String(input.planName ?? '').trim();
  const goal = String(input.goal ?? '');
  const description = String(input.description ?? '');
  if (!planName) throw new Error('计划名称不能为空');
  if (planName.length > 100) throw new Error('计划名称不能超过 100 个字符');
  if (goal.length > 255) throw new Error('学习目标不能超过 255 个字符');
  if (description.length > 10000) throw new Error('计划描述不能超过 10000 个字符');
  if (!planStatuses(original).includes(input.status)) throw new Error('请选择有效的计划状态');
  const startDate = input.startDate || null;
  const endDate = input.endDate || null;
  for (const value of [startDate, endDate]) {
    if (!value) continue;
    const parsed = new Date(`${value}T00:00:00Z`);
    if (!/^\d{4}-\d{2}-\d{2}$/.test(value) || Number.isNaN(parsed.valueOf()) || parsed.toISOString().slice(0, 10) !== value) {
      throw new Error('请填写有效日期');
    }
  }
  if (startDate && endDate && endDate < startDate) throw new Error('结束日期不能早于开始日期');
  if (original && ((original.startDate && !startDate) || (original.endDate && !endDate))) {
    throw new Error('已设置的日期暂不支持清空，请选择新的日期后保存');
  }
  return { planName, goal, description, startDate, endDate, status: input.status };
}

export function statusClass(status) {
  if (status === '已完成') return 'status-done';
  if (status === '延期' || status === '已取消') return 'status-delay';
  if (status === '未开始' || status === '未完成') return 'status-waiting';
  return 'status-doing';
}
