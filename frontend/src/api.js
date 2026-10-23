import axios from 'axios';

export function createApi(baseURL) {
  const client = axios.create({ baseURL, withCredentials: true, withXSRFToken: true, timeout: 15000 });
  client.interceptors.response.use(response => {
    if (response.data?.success !== true) {
      throw new Error(response.data?.message || '请求失败');
    }
    return response.data.data;
  }, failure => {
    const timeout = failure.code === 'ECONNABORTED' || failure.code === 'ETIMEDOUT';
    const fallback = failure.response
      ? `请求失败（${failure.response.status}）`
      : timeout ? '请求超时，请稍后重试；保存操作可先刷新列表确认结果'
        : `无法连接后端 ${baseURL}。请确认后端已启动；若已启动，请检查接口地址和跨域配置。`;
    const error = new Error(failure.response?.data?.message || fallback);
    error.status = failure.response?.status;
    return Promise.reject(error);
  });
  return client;
}
