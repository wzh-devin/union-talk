import axios, {
  AxiosHeaders,
  type InternalAxiosRequestConfig,
  type AxiosResponse,
} from 'axios'

import {
  clearAuthSession,
  getPersistedAuthToken,
} from '@/services/auth/session-service'

const baseURL = import.meta.env.VITE_API_BASE_URL || '/api/v1'

export const apiClient = axios.create({
  baseURL,
  timeout: 15_000,
  headers: {
    'Content-Type': 'application/json',
  },
})

/**
 * 为已登录请求追加 Sa-Token.
 * @param config Axios 请求配置
 * @return 带认证头的请求配置
 */
const attachAuthToken = (
  config: InternalAxiosRequestConfig,
): InternalAxiosRequestConfig => {
  const authToken = getPersistedAuthToken()

  if (!authToken) {
    return config
  }

  const headers = AxiosHeaders.from(config.headers)
  headers.set('Authorization', `${authToken}`)
  config.headers = headers

  return config
}

/**
 * 保持成功响应原样返回.
 * @param response Axios 响应
 * @return Axios 响应
 */
const passThroughResponse = <T>(response: AxiosResponse<T>): AxiosResponse<T> =>
  response

/**
 * 统一处理未授权响应.
 * @param error Axios 请求错误
 * @return 被拒绝的请求错误
 */
const handleUnauthorizedResponse = (error: unknown): Promise<never> => {
  if (axios.isAxiosError(error) && error.response?.status === 401) {
    clearAuthSession()
  }

  return Promise.reject(error)
}

apiClient.interceptors.request.use(attachAuthToken)
apiClient.interceptors.response.use(
  passThroughResponse,
  handleUnauthorizedResponse,
)
