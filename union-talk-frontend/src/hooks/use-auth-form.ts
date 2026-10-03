import { useEffect, useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router'
import { toast } from 'sonner'

import { login, register, sendAuthCode } from '@/services/auth/auth-service'
import { persistAuthSession } from '@/services/auth/session-service'
import type { LoginRequest, RegisterRequest } from '@/services/auth/types'

export type AuthMode = 'login' | 'register'

export interface AuthFormController {
  mode: AuthMode
  loginForm: LoginRequest
  registerForm: RegisterRequest
  isSubmitting: boolean
  isSendingCode: boolean
  codeCooldown: number
  switchMode: (nextMode: AuthMode) => void
  updateLoginForm: (form: Partial<LoginRequest>) => void
  updateRegisterForm: (form: Partial<RegisterRequest>) => void
  handleLogin: (event: FormEvent<HTMLFormElement>) => Promise<void>
  handleRegister: (event: FormEvent<HTMLFormElement>) => Promise<void>
  handleSendCode: () => Promise<void>
}

const codeCooldownSeconds = 60
const initialLoginForm: LoginRequest = {
  account: '',
  password: '',
}
const initialRegisterForm: RegisterRequest = {
  username: '',
  email: '',
  password: '',
  code: '',
}

const getRequestErrorMessage = (error: unknown): string =>
  error instanceof Error ? error.message : '请求失败，请稍后重试'

/**
 * 管理认证表单工作流.
 * @return 认证表单状态与业务操作
 */
export const useAuthForm = (): AuthFormController => {
  const navigate = useNavigate()
  const [mode, setMode] = useState<AuthMode>('login')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [isSendingCode, setIsSendingCode] = useState(false)
  const [codeCooldown, setCodeCooldown] = useState(0)
  const [loginForm, setLoginForm] = useState<LoginRequest>(initialLoginForm)
  const [registerForm, setRegisterForm] =
    useState<RegisterRequest>(initialRegisterForm)

  useEffect(() => {
    const intervalId = window.setInterval(() => {
      setCodeCooldown((currentCooldown) =>
        currentCooldown > 0 ? currentCooldown - 1 : 0,
      )
    }, 1000)

    return () => window.clearInterval(intervalId)
  }, [])

  const switchMode = (nextMode: AuthMode): void => {
    setMode(nextMode)
  }

  const updateLoginForm = (form: Partial<LoginRequest>): void => {
    setLoginForm((currentForm) => ({ ...currentForm, ...form }))
  }

  const updateRegisterForm = (form: Partial<RegisterRequest>): void => {
    setRegisterForm((currentForm) => ({ ...currentForm, ...form }))
  }

  const handleLogin = async (
    event: FormEvent<HTMLFormElement>,
  ): Promise<void> => {
    event.preventDefault()
    setIsSubmitting(true)

    try {
      const user = await login(loginForm)
      persistAuthSession(user)
      toast.success('登录成功')
      navigate('/', { replace: true })
    } catch (error) {
      toast.error(getRequestErrorMessage(error))
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleSendCode = async (): Promise<void> => {
    setIsSendingCode(true)

    try {
      await sendAuthCode({ email: registerForm.email })
      setCodeCooldown(codeCooldownSeconds)
      toast.success('验证码已发送')
    } catch (error) {
      toast.error(getRequestErrorMessage(error))
    } finally {
      setIsSendingCode(false)
    }
  }

  const handleRegister = async (
    event: FormEvent<HTMLFormElement>,
  ): Promise<void> => {
    event.preventDefault()
    setIsSubmitting(true)

    try {
      await register(registerForm)
      toast.success('注册成功，请返回登录')
    } catch (error) {
      toast.error(getRequestErrorMessage(error))
    } finally {
      setIsSubmitting(false)
    }
  }

  return {
    mode,
    loginForm,
    registerForm,
    isSubmitting,
    isSendingCode,
    codeCooldown,
    switchMode,
    updateLoginForm,
    updateRegisterForm,
    handleLogin,
    handleRegister,
    handleSendCode,
  }
}
