// Each response is applied independently: a category error cannot discard records.
export async function loadDashboardData(api, apply) {
  const resources = { dashboard: '/dashboard', plans: '/plans', tasks: '/tasks', records: '/records', categories: '/tasks/categories/available' };
  const failures = {};
  await Promise.allSettled(Object.entries(resources).map(async ([key, path]) => {
    try { apply(key, await api.get(path)); }
    catch (error) { failures[key] = error; }
  }));
  return { failures };
}
