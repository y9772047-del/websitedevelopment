import axios from 'axios';

export function createApi(baseURL) {
  const client = axios.create({ baseURL, withCredentials: true, timeout: 15000 });
  client.interceptors.response.use(response => {
    if (response.data?.success !== true) {
      throw new Error(response.data?.message || '请求失败');
    }
    return response.data.data;
  }, failure => {
    const error = new Error(failure.response?.data?.message || (failure.response ? `请求失败（${failure.response.status}）` : '无法连接服务，请检查网络或稍后重试'));
    error.status = failure.response?.status;
    return Promise.reject(error);
  });
  return client;
}
