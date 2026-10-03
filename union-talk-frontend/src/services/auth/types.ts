import type {
  LoginReqVO,
  RegisterReqVO,
  SendCodeReqVO,
} from '@/services/generated/auth/models'

export type LoginRequest = LoginReqVO
export type RegisterRequest = RegisterReqVO
export type SendCodeRequest = SendCodeReqVO

export interface LoginResponse {
  token: string
  userId: string
  username: string
  email: string
  avatarUrl?: string
}
