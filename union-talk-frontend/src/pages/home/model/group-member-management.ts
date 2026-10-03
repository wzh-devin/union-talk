import { MIN_GROUP_MEMBER_COUNT } from '@/pages/home/model/group-information'
import type { ConversationMember, FriendOption } from '@/pages/home/model/types'

export interface GroupKickRuleInput {
  isOwner: boolean
  currentUserId: string
  targetMember?: ConversationMember
  currentMemberCount: number
}

/**
 * 获取群聊剩余可邀请人数.
 * @param currentMemberCount 当前成员数
 * @param memberLimit 群聊人数上限
 * @return number 剩余可邀请人数
 */
export const getRemainingGroupCapacity = (
  currentMemberCount: number,
  memberLimit: number,
): number => Math.max(memberLimit - currentMemberCount, 0)

/**
 * 过滤已经在群聊中的好友.
 * @param friendOptionList 好友候选列表
 * @param memberList 当前群成员列表
 * @return FriendOption[] 可邀请好友列表
 */
export const getInvitableFriendList = (
  friendOptionList: FriendOption[],
  memberList: ConversationMember[],
): FriendOption[] => {
  const memberIdSet = new Set(memberList.map((member) => member.id))
  return friendOptionList.filter((friend) => !memberIdSet.has(friend.id))
}

/**
 * 获取邀请成员校验错误.
 * @param selectedUserIdList 已选择用户 ID 列表
 * @param remainingCapacity 剩余可邀请人数
 * @return string 校验错误或空字符串
 */
export const getGroupInviteError = (
  selectedUserIdList: string[],
  remainingCapacity: number,
): string => {
  if (selectedUserIdList.length === 0) {
    return '请至少选择一位好友'
  }
  if (selectedUserIdList.length > remainingCapacity) {
    return `最多还能邀请 ${remainingCapacity} 人`
  }
  return ''
}

/**
 * 获取退出群聊校验错误.
 * @param isOwner 当前用户是否为群主
 * @param currentMemberCount 当前成员数
 * @return string 校验错误或空字符串
 */
export const getGroupLeaveError = (
  isOwner: boolean,
  currentMemberCount: number,
): string => {
  if (isOwner) {
    return '群主暂时不能退出群聊'
  }
  if (currentMemberCount <= MIN_GROUP_MEMBER_COUNT) {
    return `群聊成员不能少于 ${MIN_GROUP_MEMBER_COUNT} 人`
  }
  return ''
}

/**
 * 获取移出群成员校验错误.
 * @param input 移出成员规则输入
 * @return string 校验错误或空字符串
 */
export const getGroupKickError = ({
  isOwner,
  currentUserId,
  targetMember,
  currentMemberCount,
}: GroupKickRuleInput): string => {
  if (!isOwner) {
    return '仅群主可以移出群成员'
  }
  if (!targetMember) {
    return '未找到群成员'
  }
  if (targetMember.role === 'owner') {
    return '不能移出群主'
  }
  if (targetMember.id === currentUserId) {
    return '不能将自己移出群聊'
  }
  if (currentMemberCount <= MIN_GROUP_MEMBER_COUNT) {
    return `群聊成员不能少于 ${MIN_GROUP_MEMBER_COUNT} 人`
  }
  return ''
}
