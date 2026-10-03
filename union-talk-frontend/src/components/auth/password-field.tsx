import { authFieldLabelClassName } from '@/components/auth/auth-field-styles'
import { Input } from '@/components/shadcn-ui/input'

export interface PasswordFieldProps {
  id: string
  value: string
  autoComplete: string
  placeholder: string
  onChange: (password: string) => void
}

/**
 * 渲染认证密码输入项.
 * @param props 密码输入项属性
 * @return 密码输入项
 */
export const PasswordField = ({
  id,
  value,
  autoComplete,
  placeholder,
  onChange,
}: PasswordFieldProps) => (
  <div className="space-y-2">
    <label className={authFieldLabelClassName} htmlFor={id}>
      Password
    </label>
    <Input
      id={id}
      type="password"
      value={value}
      minLength={10}
      maxLength={16}
      autoComplete={autoComplete}
      placeholder={placeholder}
      onChange={(event) => onChange(event.target.value)}
      required
    />
  </div>
)
