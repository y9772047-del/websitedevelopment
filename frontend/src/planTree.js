export function buildPlanTree(plans, tasks, records) {
  const recordsByTask = new Map();
  for (const record of records) {
    const key = String(record.taskId);
    if (!recordsByTask.has(key)) recordsByTask.set(key, []);
    recordsByTask.get(key).push(record);
  }
  const tasksByPlan = new Map();
  for (const task of tasks) {
    const key = String(task.planId);
    if (!tasksByPlan.has(key)) tasksByPlan.set(key, []);
    const taskRecords = recordsByTask.get(String(task.id)) || [];
    tasksByPlan.get(key).push({ ...task, records: taskRecords });
  }
  return plans.map(plan => {
    const planTasks = tasksByPlan.get(String(plan.id)) || [];
    const completedTasks = planTasks.filter(task => task.status === '已完成').length;
    const planRecords = planTasks.flatMap(task => task.records);
    return {
      ...plan,
      tasks: planTasks,
      completedTasks,
      progress: planTasks.length ? Math.round(completedTasks / planTasks.length * 100) : 0,
      recordCount: planRecords.length,
      minutes: planRecords.reduce((sum, record) => sum + (Number(record.duration) || 0), 0),
    };
  });
}
