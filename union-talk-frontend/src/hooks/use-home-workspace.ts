import { useEffect, useMemo, useRef, useState } from 'react'
import { toast } from 'sonner'

import { useHomeConversations } from '@/hooks/use-home-conversations'
import { useHomeNotifications } from '@/hooks/use-home-notifications'
import { useProfileSettings } from '@/hooks/use-profile-settings'
import { useRealtimeEvent } from '@/realtime/use-realtime-event'
import { uploadConversationFile } from '@/services/file/conversation-file-upload'
import {
  getMessageType,
  messageType,
  type MessageAssetInfo,
  type MessageMention,
  type MessageMentionType,
  type MessageType,
  type RichMessageRespVO,
} from '@/services/message/message-contract'
import { defaultSelectedIdBySection } from '@/pages/home/model/constants'
import type {
  AddFriendFormInput,
  Conversation,
  CreateGroupFormInput,
  NotificationItem,
  ProfilePane,
  Section,
} from '@/pages/home/model/types'
import {
  emptyComposerDocument,
  type ComposerDocument,
} from '@/pages/home/model/composer-document'

const profilePaneIdList: ProfilePane[] = ['account', 'privacy', 'devices']

interface MessageUploadState {
  fileName: string
  fileIndex: number
  fileCount: number
  progress: number
}

const audioFileExtensionSet = new Set([
  'aac',
  'flac',
  'm4a',
  'mp3',
  'ogg',
  'wav',
  'wma',
])

const videoFileExtensionSet = new Set([
  'avi',
  'flv',
  'm4v',
  'mkv',
  'mov',
  'mp4',
  'webm',
  'wmv',
])

const getRealtimeData = (value: unknown): Record<string, unknown> | null =>
  typeof value === 'object' && value !== null && !Array.isArray(value)
    ? (value as Record<string, unknown>)
    : null

const getFirstSelectionId = (
  section: Section,
  groupConversationList: Conversation[],
  friendConversationList: Conversation[],
  notificationList: NotificationItem[],
): string => {
  if (section === 'groups') {
    return groupConversationList[0]?.id ?? ''
  }
  if (section === 'friends') {
    return friendConversationList[0]?.id ?? ''
  }
  if (section === 'notifications') {
    return notificationList[0]?.id ?? ''
  }
  return defaultSelectedIdBySection.profile
}

const getAvailableSelectionId = (
  section: Section,
  selectedId: string,
  groupConversationList: Conversation[],
  friendConversationList: Conversation[],
  notificationList: NotificationItem[],
): string => {
  const hasSelectedId =
    section === 'groups'
      ? groupConversationList.some((item) => item.id === selectedId)
      : section === 'friends'
        ? friendConversationList.some((item) => item.id === selectedId)
        : section === 'notifications'
          ? notificationList.some((item) => item.id === selectedId)
          : profilePaneIdList.includes(selectedId as ProfilePane)

  return hasSelectedId
    ? selectedId
    : getFirstSelectionId(
        section,
        groupConversationList,
        friendConversationList,
        notificationList,
      )
}

/** 组合首页布局状态与三个独立的异步业务 hook。 */
export const useHomeWorkspace = (activeSection: Section) => {
  const profileSettings = useProfileSettings()
  const conversations = useHomeConversations(
    profileSettings.profile.userId,
    profileSettings.profile.avatarUrl,
  )
  const openConversation = conversations.openConversation
  const notifications = useHomeNotifications(conversations.refreshConversations)
  const [selectedIdBySection, setSelectedIdBySection] = useState<
    Record<Section, string>
  >(defaultSelectedIdBySection)
  const [isDetailsOpen, setIsDetailsOpen] = useState(true)
  const [isDetailsSheetOpen, setIsDetailsSheetOpen] = useState(false)
  const [isMobileListOpen, setIsMobileListOpen] = useState(true)
  const [
    composerDocumentByConversationId,
    setComposerDocumentByConversationId,
  ] = useState<Record<string, ComposerDocument>>({})
  const [messageUploadState, setMessageUploadState] =
    useState<MessageUploadState | null>(null)
  const [
    conversationFileRefreshVersionByConversationId,
    setConversationFileRefreshVersionByConversationId,
  ] = useState<Record<string, number>>({})
  const conversationRefreshTimerMapRef = useRef(
    new Map<string, ReturnType<typeof setTimeout>>(),
  )
  const messageUploadBusyRef = useRef(false)

  const groupConversationList = useMemo(
    () =>
      conversations.conversationList.filter((item) => item.kind === 'group'),
    [conversations.conversationList],
  )
  const friendConversationList = useMemo(
    () =>
      conversations.conversationList.filter((item) => item.kind === 'friend'),
    [conversations.conversationList],
  )

  useEffect(() => {
    const nextSelectedId = getAvailableSelectionId(
      activeSection,
      selectedIdBySection[activeSection],
      groupConversationList,
      friendConversationList,
      notifications.notificationList,
    )
    if (
      !nextSelectedId ||
      nextSelectedId === selectedIdBySection[activeSection]
    ) {
      return
    }
    const selectionTimer = setTimeout(() => {
      setSelectedIdBySection((current) => ({
        ...current,
        [activeSection]: nextSelectedId,
      }))
      if (activeSection === 'groups' || activeSection === 'friends') {
        void openConversation(nextSelectedId)
      }
    }, 0)
    return () => clearTimeout(selectionTimer)
  }, [
    activeSection,
    openConversation,
    friendConversationList,
    groupConversationList,
    notifications.notificationList,
    selectedIdBySection,
  ])

  const activeConversation =
    activeSection === 'groups' || activeSection === 'friends'
      ? conversations.conversationList.find(
          (item) => item.id === selectedIdBySection[activeSection],
        )
      : undefined
  const activeNotification =
    activeSection === 'notifications'
      ? notifications.notificationList.find(
          (item) => item.id === selectedIdBySection.notifications,
        )
      : undefined
  const composerDocument = activeConversation
    ? (composerDocumentByConversationId[activeConversation.id] ??
      emptyComposerDocument)
    : emptyComposerDocument

  /**
   * 标记指定会话的文件目录数据已经变化.
   * @param conversationId 会话标识
   * @return void
   */
  const markConversationFileChanged = (conversationId: string): void => {
    setConversationFileRefreshVersionByConversationId((current) => ({
      ...current,
      [conversationId]: (current[conversationId] ?? 0) + 1,
    }))
  }

  useRealtimeEvent((frame) => {
    if (frame.type === 'FRIEND_REQUEST_CREATED') {
      const data = getRealtimeData(frame.data)
      if (!data?.friendRequestId) {
        console.warn('忽略缺少申请 ID 的好友申请 WebSocket 帧')
        return
      }
      notifications.upsertFriendRequest({
        id: String(data.friendRequestId || ''),
        fromUserId: String(data.fromUserId || ''),
        fromUsername:
          typeof data.fromUsername === 'string' ? data.fromUsername : undefined,
        fromAvatarUrl:
          typeof data.fromAvatarUrl === 'string'
            ? data.fromAvatarUrl
            : undefined,
        applyMsg: typeof data.applyMsg === 'string' ? data.applyMsg : undefined,
        status: typeof data.status === 'string' ? data.status : 'PENDING',
        createdAt:
          typeof data.createdAt === 'string' ? data.createdAt : undefined,
      })
      return
    }
    if (frame.type === 'FRIEND_REQUEST_ACCEPTED') {
      void conversations
        .refreshConversations()
        .catch(() => toast.error('好友与会话列表刷新失败，请稍后重试'))
      return
    }
    if (frame.type === 'MESSAGE_CREATED') {
      const data = getRealtimeData(frame.data)
      if (!data?.messageId || !data.conversationId) {
        console.warn('忽略缺少必要 ID 的消息 WebSocket 帧')
        return
      }
      const senderUser = getRealtimeData(data.senderUser)
      const senderAgent = getRealtimeData(data.senderAgent)
      const assetInfo = getRealtimeData(data.assetInfo)
      const realtimeMessageType = getMessageType(
        typeof data.type === 'string' ? data.type : undefined,
      )
      conversations.mergeRealtimeMessage(
        {
          id: String(data.messageId || ''),
          conversationId: String(data.conversationId || ''),
          senderId: String(senderUser?.userId || ''),
          senderType:
            typeof data.senderType === 'string' ? data.senderType : undefined,
          senderUser: senderUser
            ? {
                userId: String(senderUser.userId || ''),
                code:
                  typeof senderUser.code === 'string'
                    ? senderUser.code
                    : undefined,
                username:
                  typeof senderUser.username === 'string'
                    ? senderUser.username
                    : undefined,
                avatarUrl:
                  typeof senderUser.avatarUrl === 'string'
                    ? senderUser.avatarUrl
                    : undefined,
                status:
                  typeof senderUser.status === 'string'
                    ? senderUser.status
                    : undefined,
              }
            : undefined,
          senderAgent: senderAgent
            ? {
                agentId: String(senderAgent.agentId || ''),
                displayName:
                  typeof senderAgent.displayName === 'string'
                    ? senderAgent.displayName
                    : undefined,
              }
            : undefined,
          type: realtimeMessageType,
          content: typeof data.content === 'string' ? data.content : undefined,
          assetInfo: assetInfo
            ? ({
                id: typeof assetInfo.id === 'string' ? assetInfo.id : undefined,
                conversationId:
                  typeof assetInfo.conversationId === 'string'
                    ? assetInfo.conversationId
                    : undefined,
                name:
                  typeof assetInfo.name === 'string'
                    ? assetInfo.name
                    : undefined,
                fileExt:
                  typeof assetInfo.fileExt === 'string'
                    ? assetInfo.fileExt
                    : undefined,
                fileType:
                  typeof assetInfo.fileType === 'string'
                    ? assetInfo.fileType
                    : undefined,
                fileSize:
                  typeof assetInfo.fileSize === 'string'
                    ? assetInfo.fileSize
                    : undefined,
                mimeType:
                  typeof assetInfo.mimeType === 'string'
                    ? assetInfo.mimeType
                    : undefined,
              } satisfies MessageAssetInfo)
            : undefined,
          recalled: Boolean(data.recalled),
          mentionList: Array.isArray(data.mentionList)
            ? data.mentionList
                .map((mentionValue): MessageMention | null => {
                  const mention = getRealtimeData(mentionValue)
                  if (
                    !mention ||
                    typeof mention.mentionType !== 'string' ||
                    typeof mention.displayText !== 'string' ||
                    typeof mention.startOffset !== 'number' ||
                    typeof mention.length !== 'number'
                  ) {
                    return null
                  }
                  return {
                    mentionType: mention.mentionType as MessageMentionType,
                    ...(mention.targetId
                      ? { targetId: String(mention.targetId) }
                      : {}),
                    displayText: mention.displayText,
                    startOffset: mention.startOffset,
                    length: mention.length,
                  }
                })
                .filter(
                  (mention): mention is MessageMention => mention !== null,
                )
            : [],
          agentRunId: data.agentRunId ? String(data.agentRunId) : undefined,
          triggerMessageId: data.triggerMessageId
            ? String(data.triggerMessageId)
            : undefined,
          citationList: Array.isArray(data.citationList)
            ? data.citationList.map((citationValue) => {
                const citation = getRealtimeData(citationValue)
                return {
                  citationKey:
                    typeof citation?.citationKey === 'string'
                      ? citation.citationKey
                      : undefined,
                  sourceType:
                    typeof citation?.sourceType === 'string'
                      ? citation.sourceType
                      : undefined,
                  messageId: citation?.messageId
                    ? String(citation.messageId)
                    : undefined,
                  assetFileId: citation?.assetFileId
                    ? String(citation.assetFileId)
                    : undefined,
                  resourceVersion:
                    typeof citation?.resourceVersion === 'number'
                      ? citation.resourceVersion
                      : undefined,
                  chunkId: citation?.chunkId
                    ? String(citation.chunkId)
                    : undefined,
                  pageFrom:
                    typeof citation?.pageFrom === 'number'
                      ? citation.pageFrom
                      : undefined,
                  pageTo:
                    typeof citation?.pageTo === 'number'
                      ? citation.pageTo
                      : undefined,
                  headingPath:
                    typeof citation?.headingPath === 'string'
                      ? citation.headingPath
                      : undefined,
                }
              })
            : [],
          createdAt:
            typeof data.createdAt === 'string' ? data.createdAt : undefined,
        } satisfies RichMessageRespVO,
        activeConversation?.id,
      )
      if (
        realtimeMessageType === messageType.FILE ||
        realtimeMessageType === messageType.AUDIO ||
        realtimeMessageType === messageType.VIDEO
      ) {
        markConversationFileChanged(String(data.conversationId))
      }
      return
    }
    if (frame.type === 'CONVERSATION_UPDATED') {
      const data = getRealtimeData(frame.data)
      if (!data) {
        console.warn('忽略无效的会话更新 WebSocket 帧')
        return
      }
      const conversationId = String(data.conversationId || '')
      if (!conversationId) {
        return
      }
      const existingTimer =
        conversationRefreshTimerMapRef.current.get(conversationId)
      if (existingTimer) {
        clearTimeout(existingTimer)
      }
      conversationRefreshTimerMapRef.current.set(
        conversationId,
        setTimeout(() => {
          conversationRefreshTimerMapRef.current.delete(conversationId)
          void conversations.refreshConversationDetail(conversationId)
        }, 120),
      )
    }
  })

  useEffect(
    () => () => {
      conversationRefreshTimerMapRef.current.forEach(clearTimeout)
      conversationRefreshTimerMapRef.current.clear()
    },
    [],
  )

  const selectSection = (nextSection: Section): void => {
    const nextSelectedId = getAvailableSelectionId(
      nextSection,
      selectedIdBySection[nextSection],
      groupConversationList,
      friendConversationList,
      notifications.notificationList,
    )
    setIsMobileListOpen(true)
    if (nextSelectedId) {
      setSelectedIdBySection((current) => ({
        ...current,
        [nextSection]: nextSelectedId,
      }))
    }
    if (nextSection === 'groups' || nextSection === 'friends') {
      void conversations.openConversation(nextSelectedId)
    } else if (nextSection === 'notifications') {
      notifications.markNotificationRead(nextSelectedId)
    }
  }

  const selectConversation = (
    section: 'friends' | 'groups',
    conversationId: string,
  ): void => {
    setSelectedIdBySection((current) => ({
      ...current,
      [section]: conversationId,
    }))
    setIsMobileListOpen(false)
    void conversations.openConversation(conversationId)
  }

  const selectNotification = (notificationId: string): void => {
    setSelectedIdBySection((current) => ({
      ...current,
      notifications: notificationId,
    }))
    notifications.markNotificationRead(notificationId)
    setIsMobileListOpen(false)
  }

  const updateComposerDocument = (document: ComposerDocument): void => {
    if (!activeConversation) {
      return
    }
    setComposerDocumentByConversationId((current) => ({
      ...current,
      [activeConversation.id]: document,
    }))
  }

  const sendCurrentMessage = async (): Promise<void> => {
    const messageContent = composerDocument.content
    if (
      !activeConversation ||
      !messageContent.trim() ||
      activeConversation.friendRelationStatus === 'blocked'
    ) {
      return
    }

    try {
      await conversations.sendConversationMessage(
        activeConversation.id,
        messageType.TEXT,
        messageContent,
        composerDocument.mentionList,
      )
      setComposerDocumentByConversationId((current) => ({
        ...current,
        [activeConversation.id]: emptyComposerDocument,
      }))
    } catch {
      toast.error('消息发送失败，请稍后重试')
    }
  }

  /**
   * 发送独立 Emoji 消息.
   * @param emoji Emoji 内容
   * @return 发送流程
   */
  const sendEmojiMessage = async (emoji: string): Promise<void> => {
    if (
      !activeConversation ||
      !emoji ||
      activeConversation.friendRelationStatus === 'blocked'
    ) {
      return
    }
    try {
      await conversations.sendConversationMessage(
        activeConversation.id,
        messageType.EMOJI,
        emoji,
      )
    } catch {
      toast.error('表情发送失败，请稍后重试')
    }
  }

  /**
   * 按 MIME 类型确定附件消息类型.
   * @param file 本地附件
   * @return 附件消息类型
   */
  const getAttachmentMessageType = (file: File): MessageType => {
    const fileExtension = file.name.split('.').pop()?.toLowerCase() || ''
    if (
      file.type.startsWith('audio/') ||
      audioFileExtensionSet.has(fileExtension)
    ) {
      return messageType.AUDIO
    }
    if (
      file.type.startsWith('video/') ||
      videoFileExtensionSet.has(fileExtension)
    ) {
      return messageType.VIDEO
    }
    return messageType.FILE
  }

  /**
   * 按选择顺序上传并发送多个附件消息.
   * @param fileList 本地附件列表
   * @return 批量发送流程
   */
  const sendAttachmentList = async (fileList: File[]): Promise<void> => {
    if (
      !activeConversation ||
      fileList.length === 0 ||
      messageUploadBusyRef.current ||
      activeConversation.friendRelationStatus === 'blocked'
    ) {
      return
    }
    messageUploadBusyRef.current = true
    const conversationId = activeConversation.id
    let failedFileCount = 0

    for (const [fileIndex, file] of fileList.entries()) {
      setMessageUploadState({
        fileName: file.name,
        fileIndex: fileIndex + 1,
        fileCount: fileList.length,
        progress: 0,
      })
      try {
        const completedSession = await uploadConversationFile({
          conversationId,
          file,
          onProgress: (progress) =>
            setMessageUploadState({
              fileName: file.name,
              fileIndex: fileIndex + 1,
              fileCount: fileList.length,
              progress,
            }),
        })
        markConversationFileChanged(conversationId)
        await conversations.sendConversationMessage(
          conversationId,
          getAttachmentMessageType(file),
          completedSession.assetId,
        )
      } catch {
        failedFileCount += 1
      }
    }

    messageUploadBusyRef.current = false
    setMessageUploadState(null)
    if (failedFileCount > 0) {
      toast.error(`${failedFileCount} 个附件发送失败，请稍后重试`)
    }
  }

  const sendFriendRequest = async (
    input: AddFriendFormInput,
  ): Promise<void> => {
    try {
      await conversations.sendFriendRequest(input)
    } catch (error) {
      toast.error('好友申请发送失败，请稍后重试')
      throw error
    }
  }

  /**
   * 从服务端刷新创建群聊可选好友并统一处理失败提示.
   * @return 刷新流程
   */
  const refreshFriendOptionList = async (): Promise<void> => {
    try {
      await conversations.refreshFriendOptionList()
    } catch {
      toast.error('好友列表加载失败，请稍后重试')
    }
  }

  const createConversationGroup = async (
    input: CreateGroupFormInput,
  ): Promise<void> => {
    try {
      const conversationId = await conversations.createConversationGroup(input)
      if (conversationId) {
        setSelectedIdBySection((current) => ({
          ...current,
          groups: conversationId,
        }))
        setIsMobileListOpen(false)
        void conversations.openConversation(conversationId)
      }
    } catch (error) {
      toast.error('群聊创建失败，请稍后重试')
      throw error
    }
  }

  /**
   * 删除好友并迁移到下一位可用好友.
   * @param targetUserId 目标好友用户 ID
   * @return 是否删除成功
   */
  const deleteConversationFriend = async (
    targetUserId: string,
  ): Promise<boolean> => {
    const nextConversationList =
      await conversations.deleteConversationFriend(targetUserId)
    if (!nextConversationList) {
      return false
    }

    const nextFriendConversation = nextConversationList.find(
      (conversation) => conversation.kind === 'friend',
    )
    const nextConversationId = nextFriendConversation?.id || ''
    setSelectedIdBySection((current) => ({
      ...current,
      friends: nextConversationId,
    }))
    setIsDetailsSheetOpen(false)
    if (nextConversationId) {
      void conversations.openConversation(nextConversationId)
    }
    return true
  }

  /**
   * 群聊移除后迁移到下一项可用群聊.
   * @param nextConversationList 操作后的会话列表
   * @return void
   */
  const migrateAfterGroupRemoval = (
    nextConversationList: Conversation[],
  ): void => {
    const nextGroupConversation = nextConversationList.find(
      (conversation) => conversation.kind === 'group',
    )
    const nextConversationId = nextGroupConversation?.id || ''
    setSelectedIdBySection((current) => ({
      ...current,
      groups: nextConversationId,
    }))
    setIsDetailsSheetOpen(false)
    if (nextConversationId) {
      void conversations.openConversation(nextConversationId)
    }
  }

  /**
   * 退出群聊并迁移当前选择.
   * @param groupId 群聊 ID
   * @return 是否退出成功
   */
  const leaveConversationGroup = async (groupId: string): Promise<boolean> => {
    const nextConversationList =
      await conversations.leaveConversationGroup(groupId)
    if (!nextConversationList) {
      return false
    }
    migrateAfterGroupRemoval(nextConversationList)
    return true
  }

  /**
   * 解散群聊并迁移当前选择.
   * @param groupId 群聊 ID
   * @return 是否解散成功
   */
  const dissolveConversationGroup = async (
    groupId: string,
  ): Promise<boolean> => {
    const nextConversationList =
      await conversations.dissolveConversationGroup(groupId)
    if (!nextConversationList) {
      return false
    }
    migrateAfterGroupRemoval(nextConversationList)
    return true
  }

  const selectProfilePane = (profilePane: ProfilePane): void => {
    setSelectedIdBySection((current) => ({ ...current, profile: profilePane }))
  }

  const activeMessageCursor = activeConversation
    ? conversations.messageCursorByConversationId[activeConversation.id]
    : undefined

  return {
    activeSection,
    selectedIdBySection,
    notificationList: notifications.notificationList,
    groupConversationList,
    friendConversationList,
    friendOptionList: conversations.friendOptionList,
    activeConversation,
    activeNotification,
    unreadNotifications: notifications.notificationList.filter(
      (item) => item.unread,
    ).length,
    unreadFriends: friendConversationList.reduce(
      (count, item) => count + item.unreadCount,
      0,
    ),
    unreadGroups: groupConversationList.reduce(
      (count, item) => count + item.unreadCount,
      0,
    ),
    isDetailsOpen,
    isDetailsSheetOpen,
    isMobileListOpen,
    composerDocument,
    messageUploadState,
    conversationFileRefreshVersionByConversationId,
    profile: profileSettings.profile,
    isSavingProfile: profileSettings.isSavingProfile,
    activeProfilePane: selectedIdBySection.profile as ProfilePane,
    hasNextMessages: Boolean(activeMessageCursor?.hasNext),
    isLoadingNextMessages:
      conversations.loadingNextMessageConversationId === activeConversation?.id,
    processingFriendRelation: conversations.processingFriendRelation,
    processingGroupId: conversations.processingGroupId,
    processingGroupManagement: conversations.processingGroupManagement,
    selectSection,
    selectConversation,
    selectNotification,
    showMobileList: () => setIsMobileListOpen(true),
    toggleDetailsPanel: () => setIsDetailsOpen((isOpen) => !isOpen),
    setDetailsSheetOpen: setIsDetailsSheetOpen,
    updateComposerDocument,
    sendMessage: sendCurrentMessage,
    sendEmoji: (emoji: string) => void sendEmojiMessage(emoji),
    selectAttachmentList: (fileList: File[]) =>
      void sendAttachmentList(fileList),
    sendLocalFriendRequest: sendFriendRequest,
    refreshFriendOptionList,
    createLocalGroup: createConversationGroup,
    markConversationRead: conversations.markConversationRead,
    refreshConversationMessages: conversations.refreshConversationMessages,
    blockFriend: conversations.blockConversationFriend,
    unblockFriend: conversations.unblockConversationFriend,
    updateFriendRemark: conversations.updateConversationFriendRemark,
    updateGroupInformation: conversations.updateConversationGroupInformation,
    inviteGroupMembers: conversations.inviteConversationGroupMembers,
    kickGroupMember: conversations.kickConversationGroupMember,
    leaveGroup: leaveConversationGroup,
    dissolveGroup: dissolveConversationGroup,
    deleteFriend: deleteConversationFriend,
    loadNextMessages: () =>
      activeConversation
        ? void conversations.loadNextMessages(activeConversation.id)
        : undefined,
    showPendingFeature: (feature: string) =>
      toast.info(`${feature}功能即将开放`),
    saveProfile: profileSettings.saveProfile,
    saveAvatar: profileSettings.saveAvatar,
    savePassword: profileSettings.savePassword,
    setNotificationStatus: notifications.setNotificationStatus,
    processingFriendRequestId: notifications.processingFriendRequestId,
    handleFriendRequest: notifications.handleFriendRequest,
    toggleProfileFriendVerify: () =>
      void profileSettings.toggleProfileFriendVerify(),
    selectProfilePane,
    realtimeActions: {
      refreshConversations: conversations.refreshConversations,
      refreshConversationDetail: conversations.refreshConversationDetail,
      mergeRealtimeMessage: conversations.mergeRealtimeMessage,
      upsertFriendRequest: notifications.upsertFriendRequest,
    },
  }
}
