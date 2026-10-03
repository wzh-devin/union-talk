import {
  login as generatedLogin,
  register as generatedRegister,
  sendCode as generatedSendCode,
} from '@/services/generated/auth'
import type {
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  SendCodeRequest,
} from '@/services/auth/types'

const isRequiredLoginField = (value: unknown): value is string =>
  typeof value === 'string' && value.length > 0

/**
 * 调用 Gateway 下发的认证接口，并将可选的 Swagger 响应收窄为可持久化会话。
 */
export const login = async (data: LoginRequest): Promise<LoginResponse> => {
  const response = await generatedLogin(data)

  if (
    !isRequiredLoginField(response.token) ||
    !isRequiredLoginField(response.userId) ||
    !isRequiredLoginField(response.username) ||
    !isRequiredLoginField(response.email)
  ) {
    throw new Error('登录响应缺少必要用户信息')
  }

  return {
    token: response.token,
    userId: response.userId,
    username: response.username,
    email: response.email,
    avatarUrl: response.avatarUrl,
  }
}

/** 发送注册验证码。 */
export const sendAuthCode = (data: SendCodeRequest): Promise<boolean> =>
  generatedSendCode(data)

/** 注册新用户。 */
export const register = (data: RegisterRequest): Promise<boolean> =>
  generatedRegister(data)
