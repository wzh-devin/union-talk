export const MIN_GROUP_MEMBER_COUNT = 3
export const MAX_GROUP_MEMBER_COUNT = 100
export const MAX_GROUP_NAME_LENGTH = 30
export const MAX_GROUP_DESCRIPTION_LENGTH = 200

export interface GroupInformationFormInput {
  name: string
  description: string
  memberLimit: number
}

/**
 * 获取群聊信息表单校验错误.
 * @param input 群聊信息输入
 * @param currentMemberCount 当前成员数
 * @return string 校验错误或空字符串
 */
export const getGroupInformationFormError = (
  input: GroupInformationFormInput,
  currentMemberCount: number,
): string => {
  const name = input.name.trim()
  const description = input.description.trim()
  const minimumMemberLimit = Math.max(
    MIN_GROUP_MEMBER_COUNT,
    currentMemberCount,
  )

  if (!name) {
    return '请输入群聊名称'
  }
  if (name.length > MAX_GROUP_NAME_LENGTH) {
    return '群聊名称最多 30 个字符'
  }
  if (description.length > MAX_GROUP_DESCRIPTION_LENGTH) {
    return '群聊描述最多 200 个字符'
  }
  if (!Number.isInteger(input.memberLimit)) {
    return '人数上限必须为整数'
  }
  if (input.memberLimit < minimumMemberLimit) {
    return `人数上限不能少于 ${minimumMemberLimit} 人`
  }
  if (input.memberLimit > MAX_GROUP_MEMBER_COUNT) {
    return '人数上限不能超过 100 人'
  }
  return ''
}
