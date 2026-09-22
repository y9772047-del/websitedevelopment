async (page) => {
  await page.getByRole('textbox', { name: '计划名称', exact: true }).fill('未保存草稿-迁移检查');
  await page.getByRole('button', { name: '查看详情', exact: true }).first().click();
  await page.getByRole('button', { name: '返回计划列表', exact: true }).click();
  const draftAfterReturning = await page.getByRole('textbox', { name: '计划名称', exact: true }).inputValue();
  let release;
  const pending = new Promise(resolve => { release = resolve; });
  await page.route('**/api/plans', async route => {
    if (route.request().method() !== 'POST') return route.continue();
    await pending;
    await route.fulfill({ json: { success: false, message: '延迟保存测试（未写数据库）', data: null } });
  });
  try {
    await page.getByRole('textbox', { name: '计划名称', exact: true }).fill('测试忙碌状态');
    await page.getByRole('button', { name: '保存计划', exact: true }).click();
    const navEnabledWhileSaving = await page.getByRole('button', { name: '学习计划', exact: true }).isEnabled();
    release();
    await page.getByRole('alert').filter({ hasText: '延迟保存测试' }).waitFor();
    if (draftAfterReturning !== '未保存草稿-迁移检查') throw new Error('返回列表后草稿丢失');
    if (navEnabledWhileSaving) throw new Error('保存中仍可通过导航重建页面');
    await page.getByRole('button', { name: '清空', exact: true }).click();
    return { draftAfterReturning, navEnabledWhileSaving, databaseWritten: false };
  } finally { release(); await page.unroute('**/api/plans'); }
}
