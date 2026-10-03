import type { FormEvent } from 'react'

import { authFieldLabelClassName } from '@/components/auth/auth-field-styles'
import { AuthHeader } from '@/components/auth/auth-header'
import { PasswordField } from '@/components/auth/password-field'
import { TextField } from '@/components/auth/text-field'
import { Button } from '@/components/shadcn-ui/button'
import { Input } from '@/components/shadcn-ui/input'
import type { RegisterRequest } from '@/services/auth/types'

export interface RegisterFormProps {
  form: RegisterRequest
  isSendingCode: boolean
  isSubmitting: boolean
  codeCooldown: number
  onChange: (form: Partial<RegisterRequest>) => void
  onSendCode: () => void
  onSwitchMode: () => void
  onSubmit: (event: FormEvent<HTMLFormElement>) => void
}

/**
 * 渲染注册表单.
 * @param props 注册表单属性
 * @return 注册表单
 */
export const RegisterForm = ({
  form,
  isSendingCode,
  isSubmitting,
  codeCooldown,
  onChange,
  onSendCode,
  onSwitchMode,
  onSubmit,
}: RegisterFormProps) => {
  const canSendCode =
    form.email.trim().length > 0 && !isSendingCode && codeCooldown === 0
  const sendCodeLabel = isSendingCode
    ? 'Sending...'
    : codeCooldown > 0
      ? `${codeCooldown}s`
      : 'Send code'

  return (
    <form className="space-y-5" onSubmit={onSubmit}>
      <AuthHeader
        title="Register"
        description="Create an account to start using Union Talk."
      />

      <div className="space-y-3">
        <TextField
          id="register-username"
          label="Username"
          value={form.username}
          maxLength={48}
          autoComplete="username"
          placeholder="Enter your username"
          onChange={(username) => onChange({ username })}
        />
        <TextField
          id="register-email"
          label="Email"
          type="email"
          value={form.email}
          autoComplete="email"
          placeholder="Enter your email"
          onChange={(email) => onChange({ email })}
        />
        <PasswordField
          id="register-password"
          value={form.password}
          autoComplete="new-password"
          placeholder="Enter your password"
          onChange={(password) => onChange({ password })}
        />
        <div className="space-y-2">
          <label className={authFieldLabelClassName} htmlFor="register-code">
            Code
          </label>
          <div className="flex gap-2">
            <Input
              id="register-code"
              value={form.code}
              inputMode="numeric"
              pattern="[0-9]{6}"
              maxLength={6}
              placeholder="Enter code"
              onChange={(event) => onChange({ code: event.target.value })}
              required
            />
            <Button
              type="button"
              variant="outline"
              className="h-9 min-w-16 shrink-0 px-3"
              disabled={!canSendCode}
              onClick={onSendCode}
            >
              {sendCodeLabel}
            </Button>
          </div>
        </div>
      </div>

      <div className="flex items-center justify-between gap-3 pt-1">
        <Button
          type="button"
          variant="outline"
          className="h-9 px-4"
          onClick={onSwitchMode}
        >
          Login
        </Button>
        <Button className="h-9 px-4" type="submit" disabled={isSubmitting}>
          {isSubmitting ? 'Creating...' : 'Create account'}
        </Button>
      </div>
    </form>
  )
}
