import type { AxiosRequestConfig } from 'axios'

import { apiClient } from '@/services/http/client'

/**
 * 发送 API 请求并返回业务响应体.
 * @param config Axios 请求配置
 * @return API 响应体
 */
export async function request<T>(config: AxiosRequestConfig): Promise<T> {
  const response = await apiClient.request<T>(config)

  return response.data
}
