import { Input } from '@/components/shadcn-ui/input'
import { authFieldLabelClassName } from '@/components/auth/auth-field-styles'

export interface TextFieldProps {
  id: string
  label: string
  value: string
  onChange: (value: string) => void
  autoComplete?: string
  maxLength?: number
  placeholder: string
  type?: 'email' | 'text'
}

/**
 * 渲染认证文本输入项.
 * @param props 文本输入项属性
 * @return 文本输入项
 */
export const TextField = ({
  id,
  label,
  value,
  onChange,
  autoComplete,
  maxLength,
  placeholder,
  type = 'text',
}: TextFieldProps) => (
  <div className="space-y-2">
    <label className={authFieldLabelClassName} htmlFor={id}>
      {label}
    </label>
    <Input
      id={id}
      type={type}
      value={value}
      maxLength={maxLength}
      autoComplete={autoComplete}
      placeholder={placeholder}
      onChange={(event) => onChange(event.target.value)}
      required
    />
  </div>
)
