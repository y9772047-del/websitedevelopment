export const THEMES = Object.freeze([
  { key: 'theme-pink', name: '浅粉暖色', desc: '适合温柔清爽的学习风格', primary: '#bd6b7a', hover: '#a95767', background: '#fffaf8', border: '#efd1d7' },
  { key: 'theme-blue', name: '清新蓝色', desc: '适合简洁理性的学习风格', primary: '#5b8def', hover: '#3f74df', background: '#f5f8ff', border: '#d7e4ff' },
  { key: 'theme-green', name: '抹茶绿色', desc: '适合护眼安静的学习风格', primary: '#6fa878', hover: '#5b9164', background: '#f7fbf6', border: '#d7ead8' },
  { key: 'theme-cream', name: '奶油米白', desc: '适合简洁柔和的学习风格', primary: '#c69c6d', hover: '#ad8455', background: '#fffaf2', border: '#eddcc4' },
  { key: 'theme-dark', name: '夜间模式', desc: '适合夜间学习', primary: '#8b7cf6', hover: '#7767e6', background: '#1f1f2e', border: '#45415f' }
]);

export function buildQuery(filters = {}) {
  const query = new URLSearchParams();
  Object.entries(filters).forEach(([key, value]) => {
    if (value === null || value === undefined) return;
    const normalized = typeof value === 'string' ? value.trim() : String(value);
    if (normalized !== '') query.append(key, normalized);
  });
  return query.toString();
}

export function prepareFeedback(input = {}) {
  const content = String(input.content ?? '').trim();
  if (!content) throw new Error('反馈内容不能为空');
  return { content };
}

export function prepareFeedbackDecision(input = {}) {
  const status = String(input.status ?? '').trim();
  if (!['未处理', '已处理', '已忽略'].includes(status)) throw new Error('反馈状态不正确');
  return { status, reply: String(input.reply ?? '').trim() };
}

export function nextUserStatus(status) {
  const normalized = Number(status);
  if (normalized !== 0 && normalized !== 1) throw new Error('用户状态只能是 0 或 1');
  return normalized === 1 ? 0 : 1;
}

export function themeStorageKey(user) {
  if (!user) return null;
  if (user.id !== null && user.id !== undefined) return `theme_${user.id}`;
  return user.username ? `theme_${user.username}` : null;
}

export function resolveTheme(key) {
  return THEMES.find(theme => theme.key === key) || THEMES[0];
}
