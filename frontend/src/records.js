export function filterRecords(records, tasks, filters) {
  const taskMap = new Map(tasks.map(task => [String(task.id), task]));
  return records.filter(record => {
    if (filters.planId && String(taskMap.get(String(record.taskId))?.planId) !== String(filters.planId)) return false;
    if (filters.taskId && String(record.taskId) !== String(filters.taskId)) return false;
    if (filters.completionStatus && record.completionStatus !== filters.completionStatus) return false;
    if (filters.date && record.studyDate !== filters.date) return false;
    return true;
  });
}
