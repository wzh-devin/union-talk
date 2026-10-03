import type {
  AddFriendFormInput,
  CreateGroupFormInput,
} from '@/pages/home/model/types'
import { MAX_GROUP_MEMBER_COUNT } from '@/pages/home/model/group-information'

/**
 * 获取添加好友表单校验错误.
 * @param input 添加好友输入
 * @return string 校验错误或空字符串
 */
export const getAddFriendFormError = (input: AddFriendFormInput): string => {
  const friendCode = input.friendCode.trim()
  const applyMessage = input.applyMessage.trim()

  if (!friendCode) {
    return '请输入好友 Code'
  }
  if (friendCode.length > 32) {
    return '好友 Code 最多 32 个字符'
  }
  if (!applyMessage) {
    return '请输入申请简介'
  }
  if (applyMessage.length > 200) {
    return '申请简介最多 200 个字符'
  }
  return ''
}

/**
 * 获取创建群聊表单校验错误.
 * @param input 创建群聊输入
 * @return string 校验错误或空字符串
 */
export const getCreateGroupFormError = (
  input: CreateGroupFormInput,
): string => {
  const name = input.name.trim()
  const description = input.description.trim()

  if (!name) {
    return '请输入群聊名称'
  }
  if (name.length > 30) {
    return '群聊名称最多 30 个字符'
  }
  if (!description) {
    return '请输入群聊简介'
  }
  if (description.length > 200) {
    return '群聊简介最多 200 个字符'
  }
  if (input.friendIdList.length < 2) {
    return '请至少选择两位好友'
  }
  if (input.friendIdList.length + 1 > MAX_GROUP_MEMBER_COUNT) {
    return '群聊人数不能超过 100 人'
  }
  return ''
}

/**
 * 判断创建群聊时是否应禁用新的好友选择.
 * @param selectedFriendCount 已选择好友数
 * @param isSelected 当前好友是否已选择
 * @return boolean 是否禁用选择
 */
export const isGroupFriendSelectionDisabled = (
  selectedFriendCount: number,
  isSelected: boolean,
): boolean => !isSelected && selectedFriendCount >= MAX_GROUP_MEMBER_COUNT - 1
