import { useCallback, useEffect, useRef, useState } from 'react'
import { toast } from 'sonner'

import {
  detailConversation,
  pageConversation,
  pageMessage,
  readConversation,
} from '@/services/generated/message'
import {
  blockFriend,
  createGroup,
  deleteFriend,
  dissolveGroup,
  getFriendList,
  getGroupDetail,
  getMemberList,
  inviteMembers,
  kickMember,
  leaveGroup,
  sendRequest,
  unblockFriend,
  updateGroup,
  updateRemark,
} from '@/services/generated/user'
import type { FriendRespVO } from '@/services/generated/user/models'
import {
  sendTypedMessage,
  type MessageMention,
  type MessageType,
  type RichMessageRespVO,
} from '@/services/message/message-contract'
import {
  compareMessageCreatedAt,
  mapConversation,
  mapFriendOptionList,
  mapGroupMemberList,
  mapMessage,
  mapMessageList,
} from '@/pages/home/model/api-adapters'
import {
  getGroupInviteError,
  getGroupKickError,
  getGroupLeaveError,
  getInvitableFriendList,
  getRemainingGroupCapacity,
} from '@/pages/home/model/group-member-management'
import type { GroupInformationFormInput } from '@/pages/home/model/group-information'
import { MAX_GROUP_MEMBER_COUNT } from '@/pages/home/model/group-information'
import type {
  AddFriendFormInput,
  Conversation,
  ConversationMember,
  CreateGroupFormInput,
  FriendOption,
  Message,
} from '@/pages/home/model/types'

interface MessageCursor {
  hasNext: boolean
  nextCursorValue?: string
  nextCursorId?: string
}

export type FriendRelationAction = 'remark' | 'block' | 'unblock' | 'delete'

export interface ProcessingFriendRelation {
  targetUserId: string
  action: FriendRelationAction
}

export type GroupManagementAction =
  'update' | 'invite' | 'kick' | 'leave' | 'dissolve'

export interface ProcessingGroupManagement {
  groupId: string
  action: GroupManagementAction
  targetUserId?: string
}

const mergeMessageList = (
  currentList: Message[],
  incomingList: Message[],
): Message[] => {
  const messageMap = new Map<string, Message>()

  ;[...currentList, ...incomingList]
    .sort(compareMessageCreatedAt)
    .forEach((message) => messageMap.set(message.id, message))

  return Array.from(messageMap.values())
}

/** 管理会话、好友、消息分页与相关写操作。 */
export const useHomeConversations = (
  currentUserId?: string,
  currentUserAvatarUrl?: string,
) => {
  const [conversationList, setConversationList] = useState<Conversation[]>([])
  const [friendOptionList, setFriendOptionList] = useState<FriendOption[]>([])
  const [messageCursorByConversationId, setMessageCursorByConversationId] =
    useState<Record<string, MessageCursor>>({})
  const [
    loadingNextMessageConversationId,
    setLoadingNextMessageConversationId,
  ] = useState('')
  const [processingFriendRelation, setProcessingFriendRelation] =
    useState<ProcessingFriendRelation | null>(null)
  const [processingGroupManagement, setProcessingGroupManagement] =
    useState<ProcessingGroupManagement | null>(null)
  const friendListRef = useRef<FriendRespVO[]>([])
  const detailRequestVersionRef = useRef(0)
  const listRequestVersionRef = useRef(0)

  const refreshConversations = useCallback(async (): Promise<
    Conversation[]
  > => {
    const requestVersion = ++listRequestVersionRef.current
    const [conversationPage, friendList] = await Promise.all([
      pageConversation({ pageSize: 100 }),
      getFriendList(),
    ])
    const friendMap = new Map(
      friendList.map((friend) => [friend.userId || '', friend]),
    )
    const nextConversationList = (conversationPage.list || [])
      .filter(
        (conversation) =>
          conversation.type?.toUpperCase().includes('GROUP') ||
          friendMap.has(conversation.targetUser?.userId || ''),
      )
      .map((conversation) =>
        mapConversation(
          conversation,
          friendMap.get(conversation.targetUser?.userId || ''),
          currentUserId,
          currentUserAvatarUrl,
        ),
      )

    if (requestVersion !== listRequestVersionRef.current) {
      return nextConversationList
    }

    friendListRef.current = friendList
    setFriendOptionList(mapFriendOptionList(friendList, currentUserId))
    setConversationList((currentList) =>
      nextConversationList.map((conversation) => {
        const currentConversation = currentList.find(
          (item) => item.id === conversation.id,
        )
        return currentConversation
          ? {
              ...conversation,
              messages: currentConversation.messages,
              memberList:
                conversation.kind === 'group'
                  ? currentConversation.memberList
                  : conversation.memberList,
              groupMemberLimit:
                currentConversation.groupMemberLimit ??
                conversation.groupMemberLimit,
            }
          : conversation
      }),
    )

    return nextConversationList
  }, [currentUserAvatarUrl, currentUserId])

  /**
   * 从服务端刷新创建群聊可选好友.
   * @return 刷新流程
   */
  const refreshFriendOptionList = useCallback(async (): Promise<void> => {
    const friendList = await getFriendList()
    friendListRef.current = friendList
    setFriendOptionList(mapFriendOptionList(friendList, currentUserId))
  }, [currentUserId])

  useEffect(() => {
    let isCurrent = true
    const loadTimer = setTimeout(() => {
      void refreshConversations().catch(() => {
        if (isCurrent) {
          toast.error('会话列表加载失败，请稍后重试')
        }
      })
    }, 0)

    return () => {
      isCurrent = false
      clearTimeout(loadTimer)
      detailRequestVersionRef.current += 1
    }
  }, [refreshConversations])

  /**
   * 将会话标记为已读到指定消息.
   * @param conversationId 会话 ID
   * @param targetLastReadMsgId 用户实际看到的最后一条消息 ID
   * @return 是否成功写入已读游标
   */
  const markConversationRead = useCallback(
    async (
      conversationId: string,
      targetLastReadMsgId?: string,
    ): Promise<boolean> => {
      let previousUnreadCount = 0
      let lastReadMsgId = targetLastReadMsgId

      setConversationList((currentList) =>
        currentList.map((conversation) => {
          if (conversation.id !== conversationId) {
            return conversation
          }
          previousUnreadCount = conversation.unreadCount
          lastReadMsgId ||= conversation.lastMessageId
          return { ...conversation, unreadCount: 0 }
        }),
      )

      try {
        await readConversation({ conversationId, lastReadMsgId })
        setConversationList((currentList) =>
          currentList.map((conversation) =>
            conversation.id === conversationId
              ? { ...conversation, unreadCount: 0 }
              : conversation,
          ),
        )
        return true
      } catch {
        setConversationList((currentList) =>
          currentList.map((conversation) =>
            conversation.id === conversationId
              ? { ...conversation, unreadCount: previousUnreadCount }
              : conversation,
          ),
        )
        toast.error('会话已读状态更新失败')
        return false
      }
    },
    [],
  )

  const openConversation = useCallback(
    async (conversationId: string): Promise<void> => {
      if (!conversationId) {
        return
      }
      const requestVersion = ++detailRequestVersionRef.current

      const detailPromise = detailConversation(conversationId)
      const messagePromise = pageMessage({ conversationId, pageSize: 50 })
      void markConversationRead(conversationId)

      try {
        const [detail, messagePage] = await Promise.all([
          detailPromise,
          messagePromise,
        ])
        const groupId = detail.groupId || detail.groupInfo?.groupId
        const [memberList, groupDetail] = groupId
          ? await Promise.all([
              getMemberList(groupId),
              getGroupDetail(groupId).catch(() => {
                toast.error('群聊设置加载失败，请稍后重试')
                return undefined
              }),
            ])
          : [[], undefined]

        if (requestVersion !== detailRequestVersionRef.current) {
          return
        }

        const friend = friendListRef.current.find(
          (item) => item.userId === detail.targetUser?.userId,
        )
        const mappedDetail = mapConversation(
          detail,
          friend,
          currentUserId,
          currentUserAvatarUrl,
        )
        setMessageCursorByConversationId((currentMap) => ({
          ...currentMap,
          [conversationId]: {
            hasNext: Boolean(messagePage.hasNext),
            nextCursorValue: messagePage.nextCursorValue,
            nextCursorId: messagePage.nextCursorId,
          },
        }))
        setConversationList((currentList) =>
          currentList.map((conversation) =>
            conversation.id === conversationId
              ? {
                  ...conversation,
                  ...mappedDetail,
                  unreadCount: conversation.unreadCount,
                  messages: mapMessageList(
                    messagePage.list || [],
                    currentUserId,
                  ),
                  memberList: groupId
                    ? mapGroupMemberList(memberList, mappedDetail.ownerId)
                    : mappedDetail.memberList,
                  groupMemberLimit: groupId
                    ? (groupDetail?.memberLimit ??
                      conversation.groupMemberLimit ??
                      100)
                    : undefined,
                }
              : conversation,
          ),
        )
      } catch {
        toast.error('会话详情加载失败，请稍后重试')
      }
    },
    [currentUserAvatarUrl, currentUserId, markConversationRead],
  )

  const refreshConversationDetail = useCallback(
    async (conversationId: string): Promise<void> => {
      try {
        const detail = await detailConversation(conversationId)
        const groupId = detail.groupId || detail.groupInfo?.groupId
        const groupDetail = groupId
          ? await getGroupDetail(groupId).catch(() => {
              toast.error('群聊设置加载失败，请稍后重试')
              return undefined
            })
          : undefined
        const friend = friendListRef.current.find(
          (item) => item.userId === detail.targetUser?.userId,
        )
        const mappedDetail = mapConversation(
          detail,
          friend,
          currentUserId,
          currentUserAvatarUrl,
        )
        setConversationList((currentList) =>
          currentList.map((conversation) =>
            conversation.id === conversationId
              ? {
                  ...conversation,
                  ...mappedDetail,
                  messages: conversation.messages,
                  memberList:
                    mappedDetail.kind === 'group'
                      ? conversation.memberList
                      : mappedDetail.memberList,
                  groupMemberLimit: groupId
                    ? (groupDetail?.memberLimit ??
                      conversation.groupMemberLimit ??
                      100)
                    : undefined,
                }
              : conversation,
          ),
        )
      } catch {
        toast.error('会话信息更新失败，请稍后重试')
      }
    },
    [currentUserAvatarUrl, currentUserId],
  )

  /**
   * 补拉会话最新消息并与本地历史记录合并.
   * @param conversationId 会话 ID
   * @param answerMessageId 期望同步的 Agent 正式回复消息 ID
   * @return 是否已经同步期望的正式回复
   */
  const refreshConversationMessages = useCallback(
    async (
      conversationId: string,
      answerMessageId: string,
    ): Promise<boolean> => {
      const page = await pageMessage({
        conversationId,
        pageSize: 50,
      })
      const incomingMessageList = mapMessageList(page.list || [], currentUserId)
      setConversationList((currentList) =>
        currentList.map((conversation) =>
          conversation.id === conversationId
            ? {
                ...conversation,
                messages: mergeMessageList(
                  conversation.messages,
                  incomingMessageList,
                ),
              }
            : conversation,
        ),
      )
      return incomingMessageList.some(
        (message) => message.id === answerMessageId,
      )
    },
    [currentUserId],
  )

  const loadNextMessages = useCallback(
    async (conversationId: string): Promise<void> => {
      const cursor = messageCursorByConversationId[conversationId]
      if (!cursor?.hasNext || loadingNextMessageConversationId) {
        return
      }

      setLoadingNextMessageConversationId(conversationId)
      try {
        const page = await pageMessage({
          conversationId,
          pageSize: 50,
          cursorValue: cursor.nextCursorValue,
          cursorId: cursor.nextCursorId,
        })
        const nextMessageList = mapMessageList(page.list || [], currentUserId)
        setConversationList((currentList) =>
          currentList.map((conversation) =>
            conversation.id === conversationId
              ? {
                  ...conversation,
                  messages: mergeMessageList(
                    nextMessageList,
                    conversation.messages,
                  ),
                }
              : conversation,
          ),
        )
        setMessageCursorByConversationId((currentMap) => ({
          ...currentMap,
          [conversationId]: {
            hasNext: Boolean(page.hasNext),
            nextCursorValue: page.nextCursorValue,
            nextCursorId: page.nextCursorId,
          },
        }))
      } catch {
        toast.error('更多消息加载失败，请稍后重试')
      } finally {
        setLoadingNextMessageConversationId('')
      }
    },
    [
      currentUserId,
      loadingNextMessageConversationId,
      messageCursorByConversationId,
    ],
  )

  const mergeRealtimeMessage = useCallback(
    (
      messageResponse: RichMessageRespVO,
      activeConversationId?: string,
    ): void => {
      const message = mapMessage(messageResponse, currentUserId)
      setConversationList((currentList) =>
        currentList.map((conversation) => {
          if (conversation.id !== messageResponse.conversationId) {
            return conversation
          }

          const isKnown = conversation.messages.some(
            (item) => item.id === message.id,
          )
          return {
            ...conversation,
            lastMessageId: message.id,
            lastMessage: message.body,
            lastActive: message.time,
            unreadCount:
              activeConversationId === conversation.id || isKnown
                ? conversation.unreadCount
                : conversation.unreadCount + 1,
            messages: isKnown
              ? conversation.messages
              : mergeMessageList(conversation.messages, [message]),
          }
        }),
      )
    },
    [currentUserId],
  )

  const sendConversationMessage = useCallback(
    async (
      conversationId: string,
      type: MessageType,
      content: string,
      mentionList: MessageMention[] = [],
    ): Promise<void> => {
      const response = await sendTypedMessage({
        conversationId,
        type,
        content,
        mentionList,
      })
      mergeRealtimeMessage(response, conversationId)
    },
    [mergeRealtimeMessage],
  )

  const sendFriendRequest = useCallback(
    async (input: AddFriendFormInput): Promise<void> => {
      await sendRequest({
        toUserCode: input.friendCode.trim(),
        applyMsg: input.applyMessage.trim(),
      })
      toast.success('好友申请已发送')
    },
    [],
  )

  /**
   * 拉黑指定好友并同步本地关系状态.
   * @param targetUserId 目标好友用户 ID
   * @return 是否拉黑成功
   */
  const blockConversationFriend = useCallback(
    async (targetUserId: string): Promise<boolean> => {
      setProcessingFriendRelation({ targetUserId, action: 'block' })
      try {
        const isSuccessful = await blockFriend(targetUserId)
        if (!isSuccessful) {
          throw new Error('block friend rejected')
        }
        friendListRef.current = friendListRef.current.map((friend) =>
          friend.userId === targetUserId
            ? { ...friend, status: 'BLOCKED' }
            : friend,
        )
        setConversationList((currentList) =>
          currentList.map((conversation) =>
            conversation.targetUserId === targetUserId
              ? { ...conversation, friendRelationStatus: 'blocked' }
              : conversation,
          ),
        )
        toast.success('已拉黑好友')
        return true
      } catch {
        toast.error('拉黑好友失败，请稍后重试')
        return false
      } finally {
        setProcessingFriendRelation(null)
      }
    },
    [],
  )

  /**
   * 取消拉黑指定好友并同步本地关系状态.
   * @param targetUserId 目标好友用户 ID
   * @return 是否取消拉黑成功
   */
  const unblockConversationFriend = useCallback(
    async (targetUserId: string): Promise<boolean> => {
      setProcessingFriendRelation({ targetUserId, action: 'unblock' })
      try {
        const isSuccessful = await unblockFriend(targetUserId)
        if (!isSuccessful) {
          throw new Error('unblock friend rejected')
        }
        friendListRef.current = friendListRef.current.map((friend) =>
          friend.userId === targetUserId
            ? { ...friend, status: 'NORMAL' }
            : friend,
        )
        setConversationList((currentList) =>
          currentList.map((conversation) =>
            conversation.targetUserId === targetUserId
              ? { ...conversation, friendRelationStatus: 'normal' }
              : conversation,
          ),
        )
        toast.success('已取消拉黑')
        return true
      } catch {
        toast.error('取消拉黑失败，请稍后重试')
        return false
      } finally {
        setProcessingFriendRelation(null)
      }
    },
    [],
  )

  /**
   * 修改好友备注并同步所有本地好友名称入口.
   * @param targetUserId 目标好友用户 ID
   * @param remark 好友备注，空字符串表示清除
   * @return 是否修改成功
   */
  const updateConversationFriendRemark = useCallback(
    async (targetUserId: string, remark: string): Promise<boolean> => {
      const nextRemark = remark.trim()
      setProcessingFriendRelation({ targetUserId, action: 'remark' })
      try {
        const isSuccessful = await updateRemark({
          targetId: targetUserId,
          remark: nextRemark,
        })
        if (!isSuccessful) {
          throw new Error('update friend remark rejected')
        }

        friendListRef.current = friendListRef.current.map((friend) =>
          friend.userId === targetUserId
            ? { ...friend, remark: nextRemark }
            : friend,
        )
        const nextFriend = friendListRef.current.find(
          (friend) => friend.userId === targetUserId,
        )
        setFriendOptionList(
          mapFriendOptionList(friendListRef.current, currentUserId),
        )
        setConversationList((currentList) =>
          currentList.map((conversation) => {
            if (conversation.targetUserId !== targetUserId) {
              return conversation
            }
            const nextFriendName =
              nextRemark ||
              nextFriend?.username ||
              nextFriend?.code ||
              conversation.friendUsername ||
              conversation.name
            return {
              ...conversation,
              name: nextFriendName,
              friendRemark: nextRemark,
              memberList: conversation.memberList.map((member) =>
                member.id === targetUserId
                  ? { ...member, name: nextFriendName }
                  : member,
              ),
            }
          }),
        )
        toast.success(nextRemark ? '好友备注已更新' : '好友备注已清除')
        return true
      } catch {
        toast.error('好友备注修改失败，请稍后重试')
        return false
      } finally {
        setProcessingFriendRelation(null)
      }
    },
    [currentUserId],
  )

  /**
   * 更新群聊信息并同步本地群聊展示数据.
   * @param groupId 群聊 ID
   * @param input 群聊信息输入
   * @return 是否更新成功
   */
  const updateConversationGroupInformation = useCallback(
    async (
      groupId: string,
      input: GroupInformationFormInput,
    ): Promise<boolean> => {
      const targetGroup = conversationList.find(
        (conversation) => conversation.groupId === groupId,
      )
      if (!targetGroup || targetGroup.ownerId !== currentUserId) {
        toast.error('仅群主可以修改群聊信息')
        return false
      }

      const nextName = input.name.trim()
      const nextDescription = input.description.trim()
      setProcessingGroupManagement({ groupId, action: 'update' })
      try {
        const isSuccessful = await updateGroup(groupId, {
          name: nextName,
          description: nextDescription,
          memberLimit: input.memberLimit,
        })
        if (!isSuccessful) {
          throw new Error('update group rejected')
        }
        setConversationList((currentList) =>
          currentList.map((conversation) =>
            conversation.groupId === groupId
              ? {
                  ...conversation,
                  name: nextName,
                  description: nextDescription || '暂无群聊描述',
                  groupMemberLimit: input.memberLimit,
                }
              : conversation,
          ),
        )
        toast.success('群聊信息已更新')
        return true
      } catch {
        toast.error('群聊信息更新失败，请稍后重试')
        return false
      } finally {
        setProcessingGroupManagement(null)
      }
    },
    [conversationList, currentUserId],
  )

  /**
   * 重新加载群成员并同步对应会话.
   * @param groupId 群聊 ID
   * @param ownerId 群主用户 ID
   * @return ConversationMember[] 最新群成员列表
   */
  const refreshGroupMemberList = useCallback(
    async (
      groupId: string,
      ownerId?: string,
    ): Promise<ConversationMember[]> => {
      const memberResponseList = await getMemberList(groupId)
      const nextMemberList = mapGroupMemberList(memberResponseList, ownerId)
      setConversationList((currentList) =>
        currentList.map((conversation) =>
          conversation.groupId === groupId
            ? { ...conversation, memberList: nextMemberList }
            : conversation,
        ),
      )
      return nextMemberList
    },
    [],
  )

  /**
   * 邀请好友加入指定群聊.
   * @param groupId 群聊 ID
   * @param userIdList 被邀请用户 ID 列表
   * @return 是否邀请成功
   */
  const inviteConversationGroupMembers = useCallback(
    async (groupId: string, userIdList: string[]): Promise<boolean> => {
      const targetGroup = conversationList.find(
        (conversation) => conversation.groupId === groupId,
      )
      if (
        !targetGroup ||
        !currentUserId ||
        !targetGroup.memberList.some((member) => member.id === currentUserId)
      ) {
        toast.error('仅群成员可以邀请新成员')
        return false
      }

      const nextUserIdList = Array.from(new Set(userIdList))
      const remainingCapacity = getRemainingGroupCapacity(
        targetGroup.memberList.length,
        targetGroup.groupMemberLimit ?? MAX_GROUP_MEMBER_COUNT,
      )
      const inviteError = getGroupInviteError(nextUserIdList, remainingCapacity)
      if (inviteError) {
        toast.error(inviteError)
        return false
      }

      const invitableUserIdSet = new Set(
        getInvitableFriendList(friendOptionList, targetGroup.memberList).map(
          (friend) => friend.id,
        ),
      )
      if (nextUserIdList.some((userId) => !invitableUserIdSet.has(userId))) {
        toast.error('请选择尚未入群的好友')
        return false
      }

      setProcessingGroupManagement({ groupId, action: 'invite' })
      try {
        const isSuccessful = await inviteMembers({
          groupId,
          uidList: nextUserIdList,
        })
        if (!isSuccessful) {
          throw new Error('invite members rejected')
        }
        await refreshGroupMemberList(groupId, targetGroup.ownerId)
        toast.success(`已邀请 ${nextUserIdList.length} 位成员`)
        return true
      } catch {
        toast.error('邀请成员失败，请稍后重试')
        return false
      } finally {
        setProcessingGroupManagement(null)
      }
    },
    [conversationList, currentUserId, friendOptionList, refreshGroupMemberList],
  )

  /**
   * 将普通成员移出指定群聊.
   * @param groupId 群聊 ID
   * @param targetUserId 目标成员用户 ID
   * @return 是否移出成功
   */
  const kickConversationGroupMember = useCallback(
    async (groupId: string, targetUserId: string): Promise<boolean> => {
      const targetGroup = conversationList.find(
        (conversation) => conversation.groupId === groupId,
      )
      const kickError = getGroupKickError({
        isOwner: Boolean(targetGroup && targetGroup.ownerId === currentUserId),
        currentUserId: currentUserId || '',
        targetMember: targetGroup?.memberList.find(
          (member) => member.id === targetUserId,
        ),
        currentMemberCount: targetGroup?.memberList.length ?? 0,
      })
      if (kickError) {
        toast.error(kickError)
        return false
      }

      setProcessingGroupManagement({
        groupId,
        action: 'kick',
        targetUserId,
      })
      try {
        const isSuccessful = await kickMember(groupId, targetUserId)
        if (!isSuccessful) {
          throw new Error('kick member rejected')
        }
        await refreshGroupMemberList(groupId, targetGroup?.ownerId)
        toast.success('已将成员移出群聊')
        return true
      } catch {
        toast.error('移出群成员失败，请稍后重试')
        return false
      } finally {
        setProcessingGroupManagement(null)
      }
    },
    [conversationList, currentUserId, refreshGroupMemberList],
  )

  /**
   * 普通成员退出指定群聊.
   * @param groupId 群聊 ID
   * @return 移除群聊后的会话列表，失败时为 null
   */
  const leaveConversationGroup = useCallback(
    async (groupId: string): Promise<Conversation[] | null> => {
      const targetGroup = conversationList.find(
        (conversation) => conversation.groupId === groupId,
      )
      if (!targetGroup) {
        toast.error('未找到群聊')
        return null
      }
      const leaveError = getGroupLeaveError(
        targetGroup.ownerId === currentUserId,
        targetGroup.memberList.length,
      )
      if (leaveError) {
        toast.error(leaveError)
        return null
      }

      setProcessingGroupManagement({ groupId, action: 'leave' })
      try {
        const isSuccessful = await leaveGroup(groupId)
        if (!isSuccessful) {
          throw new Error('leave group rejected')
        }
        const nextConversationList = conversationList.filter(
          (conversation) => conversation.groupId !== groupId,
        )
        setConversationList(nextConversationList)
        toast.success('已退出群聊')
        return nextConversationList
      } catch {
        toast.error('退出群聊失败，请稍后重试')
        return null
      } finally {
        setProcessingGroupManagement(null)
      }
    },
    [conversationList, currentUserId],
  )

  /**
   * 群主解散指定群聊.
   * @param groupId 群聊 ID
   * @return 移除群聊后的会话列表，失败时为 null
   */
  const dissolveConversationGroup = useCallback(
    async (groupId: string): Promise<Conversation[] | null> => {
      const targetGroup = conversationList.find(
        (conversation) => conversation.groupId === groupId,
      )
      if (!targetGroup || targetGroup.ownerId !== currentUserId) {
        toast.error('仅群主可以解散群聊')
        return null
      }

      setProcessingGroupManagement({ groupId, action: 'dissolve' })
      try {
        const isSuccessful = await dissolveGroup(groupId)
        if (!isSuccessful) {
          throw new Error('dissolve group rejected')
        }
        const nextConversationList = conversationList.filter(
          (conversation) => conversation.groupId !== groupId,
        )
        setConversationList(nextConversationList)
        toast.success('群聊已解散')
        return nextConversationList
      } catch {
        toast.error('解散群聊失败，请稍后重试')
        return null
      } finally {
        setProcessingGroupManagement(null)
      }
    },
    [conversationList, currentUserId],
  )

  /**
   * 删除指定好友并返回刷新后的会话列表.
   * @param targetUserId 目标好友用户 ID
   * @return 刷新后的会话列表，失败时为 null
   */
  const deleteConversationFriend = useCallback(
    async (targetUserId: string): Promise<Conversation[] | null> => {
      setProcessingFriendRelation({ targetUserId, action: 'delete' })
      try {
        const isSuccessful = await deleteFriend(targetUserId)
        if (!isSuccessful) {
          throw new Error('delete friend rejected')
        }
        const nextConversationList = await refreshConversations()
        toast.success('已删除好友')
        return nextConversationList
      } catch {
        toast.error('删除好友失败，请稍后重试')
        return null
      } finally {
        setProcessingFriendRelation(null)
      }
    },
    [refreshConversations],
  )

  const createConversationGroup = useCallback(
    async (input: CreateGroupFormInput): Promise<string> => {
      const group = await createGroup({
        name: input.name.trim(),
        description: input.description.trim(),
        memberLimit: MAX_GROUP_MEMBER_COUNT,
        uidList: input.friendIdList,
      })
      const nextList = await refreshConversations()
      const createdConversation = nextList.find(
        (conversation) => conversation.groupId === group.id,
      )
      toast.success('群聊已创建')
      return createdConversation?.id || ''
    },
    [refreshConversations],
  )

  return {
    conversationList,
    friendOptionList,
    messageCursorByConversationId,
    loadingNextMessageConversationId,
    processingFriendRelation,
    processingGroupId: processingGroupManagement?.groupId || '',
    processingGroupManagement,
    refreshConversations,
    refreshFriendOptionList,
    refreshConversationDetail,
    refreshConversationMessages,
    openConversation,
    markConversationRead,
    loadNextMessages,
    sendConversationMessage,
    sendFriendRequest,
    blockConversationFriend,
    unblockConversationFriend,
    updateConversationFriendRemark,
    updateConversationGroupInformation,
    inviteConversationGroupMembers,
    kickConversationGroupMember,
    leaveConversationGroup,
    dissolveConversationGroup,
    deleteConversationFriend,
    createConversationGroup,
    mergeRealtimeMessage,
  }
}
