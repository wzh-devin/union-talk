import type { AxiosRequestConfig } from 'axios'

import { apiClient } from '@/services/http/client'

interface ApiResult<T> {
  success: boolean
  errCode?: number
  errMsg?: string
  data: T
}

/**
 * 表示后端统一响应中的业务错误.
 */
export class ApiRequestError extends Error {
  /** 后端业务错误码. */
  readonly code?: number
  /** 后端业务错误码，与统一响应字段同名。 */
  readonly errCode?: number

  /**
   * 创建业务请求错误.
   * @param message 错误信息
   * @param code 后端业务错误码
   */
  constructor(message: string, code?: number) {
    super(message)
    this.name = 'ApiRequestError'
    this.code = code
    this.errCode = code
  }
}

/**
 * 发送生成 service 的请求并解包统一业务响应.
 * @param config Axios 请求配置
 * @param options 调用方覆盖配置
 * @return Promise<T> 业务响应数据
 */
export const generatedRequest = async <T>(
  config: AxiosRequestConfig,
  options?: AxiosRequestConfig,
): Promise<T> => {
  const response = await apiClient.request<ApiResult<T>>({
    ...config,
    ...options,
  })
  const result = response.data

  if (!result.success) {
    throw new ApiRequestError(result.errMsg || '请求失败', result.errCode)
  }

  return result.data
}

export type ErrorType<Error> = Error
export type BodyType<BodyData> = BodyData
