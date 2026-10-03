export type EditableProfileField = 'displayName' | 'email' | 'bio'

export interface EditableProfileFieldMeta {
  label: string
  description: string
  inputType: 'text' | 'email'
  multiline: boolean
}

export interface PasswordInput {
  currentPassword: string
  newPassword: string
  confirmPassword: string
}

export const editableProfileFieldMeta: Record<
  EditableProfileField,
  EditableProfileFieldMeta
> = {
  displayName: {
    label: '显示名称',
    description: '修改在 Union Talk 中展示的名称。',
    inputType: 'text',
    multiline: false,
  },
  email: {
    label: '邮箱',
    description: '修改当前会话内展示的邮箱地址。',
    inputType: 'email',
    multiline: false,
  },
  bio: {
    label: '个人签名',
    description: '修改或清空你的个人签名。',
    inputType: 'text',
    multiline: true,
  },
}

const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

/**
 * 获取可编辑资料字段的校验错误.
 * @param field 资料字段
 * @param value 待保存值
 * @return string 校验错误或空字符串
 */
export const getProfileFieldError = (
  field: EditableProfileField,
  value: string,
): string => {
  if (field === 'displayName' && value.trim().length === 0) {
    return '显示名称不能为空'
  }
  if (field === 'email' && !emailPattern.test(value.trim())) {
    return '请输入有效邮箱地址'
  }
  return ''
}

/**
 * 获取本地密码修改的校验错误.
 * @param input 密码输入
 * @return string 校验错误或空字符串
 */
export const getPasswordError = ({
  currentPassword,
  newPassword,
  confirmPassword,
}: PasswordInput): string => {
  if (currentPassword.length < 10 || currentPassword.length > 16) {
    return '当前密码长度需要在 10～16 位'
  }
  if (newPassword.length < 10 || newPassword.length > 16) {
    return '新密码长度需要在 10～16 位'
  }
  if (newPassword !== confirmPassword) {
    return '两次输入的新密码不一致'
  }
  return ''
}
