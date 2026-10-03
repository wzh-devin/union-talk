import { LoginForm } from '@/components/auth/login-form'
import { RegisterForm } from '@/components/auth/register-form'
import type { AuthFormController } from '@/hooks/use-auth-form'

export interface AuthFormSwitcherProps {
  authForm: AuthFormController
}

/**
 * 根据认证模式切换登录或注册表单.
 * @param props 认证表单控制器属性
 * @return 当前认证模式表单
 */
export const AuthFormSwitcher = ({ authForm }: AuthFormSwitcherProps) =>
  authForm.mode === 'login' ? (
    <LoginForm
      form={authForm.loginForm}
      isSubmitting={authForm.isSubmitting}
      onChange={authForm.updateLoginForm}
      onSwitchMode={() => authForm.switchMode('register')}
      onSubmit={authForm.handleLogin}
    />
  ) : (
    <RegisterForm
      form={authForm.registerForm}
      isSendingCode={authForm.isSendingCode}
      isSubmitting={authForm.isSubmitting}
      codeCooldown={authForm.codeCooldown}
      onChange={authForm.updateRegisterForm}
      onSendCode={authForm.handleSendCode}
      onSwitchMode={() => authForm.switchMode('login')}
      onSubmit={authForm.handleRegister}
    />
  )
