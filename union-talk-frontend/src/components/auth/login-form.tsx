import type { FormEvent } from 'react'

import { AuthHeader } from '@/components/auth/auth-header'
import { PasswordField } from '@/components/auth/password-field'
import { TextField } from '@/components/auth/text-field'
import { Button } from '@/components/shadcn-ui/button'
import type { LoginRequest } from '@/services/auth/types'

export interface LoginFormProps {
  form: LoginRequest
  isSubmitting: boolean
  onChange: (form: Partial<LoginRequest>) => void
  onSwitchMode: () => void
  onSubmit: (event: FormEvent<HTMLFormElement>) => void
}

/**
 * 渲染登录表单.
 * @param props 登录表单属性
 * @return 登录表单
 */
export const LoginForm = ({
  form,
  isSubmitting,
  onChange,
  onSwitchMode,
  onSubmit,
}: LoginFormProps) => (
  <form className="space-y-6" onSubmit={onSubmit}>
    <AuthHeader
      title="Login"
      description="Enter your credentials to access your account."
    />

    <div className="space-y-4">
      <TextField
        id="login-account"
        label="Account"
        value={form.account}
        maxLength={48}
        autoComplete="username"
        placeholder="Enter your account"
        onChange={(account) => onChange({ account })}
      />
      <PasswordField
        id="login-password"
        value={form.password}
        autoComplete="current-password"
        placeholder="Enter your password"
        onChange={(password) => onChange({ password })}
      />
    </div>

    <div className="flex items-center justify-between gap-3 pt-1">
      <Button
        type="button"
        variant="outline"
        className="h-9 px-4"
        onClick={onSwitchMode}
      >
        Register
      </Button>
      <Button className="h-9 px-5" type="submit" disabled={isSubmitting}>
        {isSubmitting ? 'Logging in...' : 'Login'}
      </Button>
    </div>
  </form>
)
