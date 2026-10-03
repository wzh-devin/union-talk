import type {
  ConversationRespVO,
  MessageRespVO,
} from '@/services/generated/message/models'
import type {
  FriendRequestRespVO,
  FriendRespVO,
  GroupMemberRespVO,
  UserInfoRespVO,
} from '@/services/generated/user/models'
import type {
  Conversation,
  ConversationMember,
  FriendOption,
  Message,
  NotificationItem,
  ProfileSettings,
} from '@/pages/home/model/types'
import {
  getMessageType,
  messageType,
  type RichMessageRespVO,
} from '@/services/message/message-contract'

const conversationAccentList = [
  'bg-sky-700 text-white',
  'bg-emerald-700 text-white',
  'bg-amber-700 text-white',
  'bg-violet-700 text-white',
  'bg-teal-700 text-white',
  'bg-rose-700 text-white',
]

const getStableAccent = (id: string): string => {
  const accentIndex = Array.from(id).reduce(
    (total, character) => total + character.charCodeAt(0),
    0,
  )

  return conversationAccentList[accentIndex % conversationAccentList.length]
}

const getShortId = (id?: string): string => id?.slice(-4) || '未知'

const legacyApiDateTimePattern =
  /^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}(?:\.\d{1,3})?$/

/**
 * 将接口时间转换为可排序的时间戳.
 * @param value 接口时间
 * @return number 毫秒时间戳，缺失或非法时为负无穷
 */
const getApiTimeTimestamp = (value?: string): number => {
  if (!value) {
    return Number.NEGATIVE_INFINITY
  }

  const parsableValue = legacyApiDateTimePattern.test(value)
    ? value.replace(' ', 'T')
    : value
  const timestamp = Date.parse(parsableValue)

  return Number.isNaN(timestamp) ? Number.NEGATIVE_INFINITY : timestamp
}

/**
 * 按消息真实创建时间升序比较，兼容 HTTP 与 WebSocket 时间格式.
 * @param left 左侧消息
 * @param right 右侧消息
 * @return number 排序比较结果
 */
export const compareMessageCreatedAt = (
  left: { createdAt?: string },
  right: { createdAt?: string },
): number => {
  const leftTimestamp = getApiTimeTimestamp(left.createdAt)
  const rightTimestamp = getApiTimeTimestamp(right.createdAt)

  if (leftTimestamp === rightTimestamp) {
    return 0
  }

  return leftTimestamp < rightTimestamp ? -1 : 1
}

/**
 * 格式化接口时间为所有首页页面共享的完整中文时间文本.
 * @param value 接口时间
 * @return string 界面时间文本
 */
export const formatApiTime = (value?: string): string => {
  if (!value) {
    return '刚刚'
  }

  const timestamp = getApiTimeTimestamp(value)
  if (!Number.isFinite(timestamp)) {
    return value
  }
  const date = new Date(timestamp)

  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')

  return `${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日 ${hours}:${minutes}`
}

/**
 * 将消息响应映射为现有消息展示模型.
 * @param message 消息响应
 * @param currentUserId 当前用户 ID
 * @return Message 消息展示模型
 */
export const mapMessage = (
  message: MessageRespVO,
  currentUserId?: string,
): Message => {
  const richMessage = message as RichMessageRespVO
  const type = getMessageType(richMessage.type)
  const id =
    message.id ||
    `message-${message.conversationId || 'unknown'}-${message.createdAt || 'unknown'}`
  const isAgent =
    richMessage.senderType?.toUpperCase() === 'AGENT' ||
    Boolean(richMessage.agentRunId)
  const isSelf = Boolean(
    !isAgent && currentUserId && message.senderId === currentUserId,
  )
  const author = isAgent
    ? richMessage.senderAgent?.displayName || 'AI 助手'
    : isSelf
      ? '我'
      : message.senderUser?.username ||
        message.senderUser?.code ||
        `用户 ${getShortId(message.senderId)}`
  const body = richMessage.recalled
    ? '该消息已撤回'
    : type === messageType.TEXT || type === messageType.EMOJI
      ? richMessage.content || '[消息内容为空]'
      : richMessage.assetInfo?.name || '文件不可用'

  return {
    id,
    conversationId: message.conversationId,
    senderId: message.senderId,
    createdAt: message.createdAt,
    author,
    role: isAgent ? 'AI 助手' : isSelf ? '我' : '成员',
    time: formatApiTime(message.createdAt),
    body,
    type,
    assetInfo: richMessage.assetInfo,
    tone: isAgent ? 'agent' : isSelf ? 'self' : undefined,
    senderType: richMessage.senderType,
    agentRunId: richMessage.agentRunId,
    triggerMessageId: richMessage.triggerMessageId,
    agentName: richMessage.senderAgent?.displayName,
    citationList: richMessage.citationList,
    mentionList: richMessage.mentionList ?? [],
  }
}

/**
 * 将消息转换为会话列表摘要.
 * @param message 消息响应
 * @return 会话摘要
 */
export const formatMessageSummary = (message?: RichMessageRespVO): string => {
  if (!message) {
    return '暂无消息'
  }
  if (message.recalled) {
    return '该消息已撤回'
  }
  const type = getMessageType(message.type)
  if (type === messageType.AUDIO) {
    return `[音频] ${message.assetInfo?.name || '文件不可用'}`
  }
  if (type === messageType.VIDEO) {
    return `[视频] ${message.assetInfo?.name || '文件不可用'}`
  }
  if (type === messageType.FILE) {
    return `[文件] ${message.assetInfo?.name || '文件不可用'}`
  }
  return message.content || '暂无消息'
}

/**
 * 对消息响应排序并按消息 ID 去重.
 * @param messageList 消息响应列表
 * @param currentUserId 当前用户 ID
 * @return Message[] 消息展示列表
 */
export const mapMessageList = (
  messageList: MessageRespVO[],
  currentUserId?: string,
): Message[] => {
  const messageMap = new Map<string, Message>()

  messageList
    .slice()
    .sort(compareMessageCreatedAt)
    .map((message) => mapMessage(message, currentUserId))
    .forEach((message) => {
      if (!messageMap.has(message.id)) {
        messageMap.set(message.id, message)
      }
    })

  return Array.from(messageMap.values())
}

/**
 * 将会话响应映射为现有会话展示模型.
 * @param conversation 会话响应
 * @param friend 好友关系信息
 * @param currentUserId 当前用户 ID
 * @param currentUserAvatarUrl 当前用户头像地址
 * @return Conversation 会话展示模型
 */
export const mapConversation = (
  conversation: ConversationRespVO,
  friend?: FriendRespVO,
  currentUserId?: string,
  currentUserAvatarUrl?: string,
): Conversation => {
  const kind = conversation.type?.toUpperCase().includes('GROUP')
    ? 'group'
    : 'friend'
  const id =
    conversation.conversationId ||
    conversation.groupInfo?.groupId ||
    conversation.targetUser?.userId ||
    'unknown-conversation'
  const groupId = conversation.groupId || conversation.groupInfo?.groupId
  const targetUserId = conversation.targetUser?.userId
  const friendRelationStatus =
    friend?.status?.toUpperCase() === 'BLOCKED' ? 'blocked' : 'normal'
  const friendUsername =
    conversation.targetUser?.username ||
    conversation.targetUser?.code ||
    `好友 ${getShortId(targetUserId)}`
  const friendRemark = friend?.remark || ''
  const friendName = friendRemark || friendUsername
  const friendAvatarUrl =
    conversation.targetUser?.avatarUrl || friend?.avatarUrl || undefined
  const groupName =
    conversation.groupInfo?.name || `群聊 ${getShortId(groupId || id)}`
  const name = kind === 'group' ? groupName : friendName
  const privateMemberList: ConversationMember[] =
    kind === 'friend'
      ? [
          {
            id: currentUserId || 'current-user',
            name: '我',
            role: 'member',
            ...(currentUserAvatarUrl
              ? { avatarUrl: currentUserAvatarUrl }
              : {}),
          },
          {
            id: targetUserId || `${id}-target`,
            name: friendName,
            role: 'member',
            ...(friendAvatarUrl ? { avatarUrl: friendAvatarUrl } : {}),
          },
        ]
      : []

  return {
    id,
    groupId,
    targetUserId,
    lastMessageId: conversation.lastMsgId,
    ownerId: conversation.groupInfo?.ownerId,
    friendRelationStatus: kind === 'friend' ? friendRelationStatus : undefined,
    friendUsername: kind === 'friend' ? friendUsername : undefined,
    friendRemark: kind === 'friend' ? friendRemark : undefined,
    avatarUrl:
      kind === 'group' ? conversation.groupInfo?.avatarUrl : friendAvatarUrl,
    kind,
    name,
    description:
      kind === 'group'
        ? conversation.groupInfo?.description || '暂无群聊描述'
        : conversation.targetUser?.code
          ? `用户 ${conversation.targetUser.code}`
          : '好友会话',
    status:
      (kind === 'group'
        ? conversation.groupInfo?.status
        : conversation.targetUser?.status || friend?.status) || '正常',
    lastMessage: formatMessageSummary(
      conversation.lastMessage as RichMessageRespVO | undefined,
    ),
    lastActive: formatApiTime(
      conversation.lastMsgAt ||
        conversation.lastMessage?.createdAt ||
        conversation.updatedAt,
    ),
    unreadCount: conversation.unreadCount || 0,
    memberList: privateMemberList,
    tags: [],
    accent: getStableAccent(id),
    messages: [],
    code:
      kind === 'group'
        ? groupId || `GROUP-${getShortId(id)}`
        : conversation.targetUser?.code || `USER-${getShortId(targetUserId)}`,
    relation: kind === 'group' ? '群聊' : '好友',
    isPinned: Boolean(conversation.isPinned),
    isMuted: Boolean(conversation.isMuted),
    isFavorite: false,
  }
}

/**
 * 将好友申请映射为现有通知展示模型.
 * @param request 好友申请响应
 * @return NotificationItem 通知展示模型
 */
export const mapFriendRequestNotification = (
  request: FriendRequestRespVO,
): NotificationItem => {
  const id =
    request.id ||
    `friend-request-${request.fromUserId || 'unknown'}-${request.createdAt || 'unknown'}`
  const serverStatus = request.status?.toUpperCase()
  const status: NotificationItem['status'] =
    serverStatus === 'PENDING'
      ? '待处理'
      : serverStatus === 'ACCEPTED'
        ? '已同意'
        : serverStatus === 'REJECTED'
          ? '已拒绝'
          : '已处理'
  const source =
    request.fromUsername || `用户 ${getShortId(request.fromUserId)}`
  const applyMessage = request.applyMsg || '对方希望添加你为好友。'

  return {
    id,
    requestId: request.id,
    title: '好友申请',
    source,
    time: formatApiTime(request.createdAt),
    summary: applyMessage,
    body: applyMessage,
    status,
    unread: status === '待处理',
    category: '好友申请',
    actionHint:
      status === '待处理'
        ? '请选择同意或拒绝该好友申请。'
        : status === '已同意'
          ? '你已同意该好友申请。'
          : status === '已拒绝'
            ? '你已拒绝该好友申请。'
            : '该好友申请已处理。',
  }
}

/**
 * 将当前用户响应映射为个人设置模型.
 * @param user 当前用户响应
 * @return ProfileSettings 个人设置模型
 */
export const mapProfileSettings = (user: UserInfoRespVO): ProfileSettings => ({
  userId: user.uid || '',
  code: user.code || '',
  displayName: user.username || '未命名用户',
  email: user.email || '',
  avatarUrl: user.avatarUrl || '',
  bio: user.bio || '',
  needFriendVerify: Boolean(user.needFriendVerify),
  status: user.status === 'NORMAL' ? '正常' : user.status || '未知',
  lastLoginAt: formatApiTime(user.lastLoginAt),
  privacyLabel: '仅好友可见',
  trustedDeviceList: [],
})

/**
 * 将好友列表映射为创建群聊的选项.
 * @param friendList 好友列表
 * @param currentUserId 当前用户 ID
 * @return FriendOption[] 好友选项列表
 */
export const mapFriendOptionList = (
  friendList: FriendRespVO[],
  currentUserId?: string,
): FriendOption[] => {
  const friendOptionMap = new Map<string, FriendOption>()

  friendList.forEach((friend) => {
    const friendId = friend.userId || ''
    if (
      !friendId ||
      friendId === currentUserId ||
      friendOptionMap.has(friendId)
    ) {
      return
    }

    friendOptionMap.set(friendId, {
      id: friendId,
      name: friend.remark || friend.username || friend.code || '未命名好友',
      accent: getStableAccent(friendId),
      ...(friend.avatarUrl ? { avatarUrl: friend.avatarUrl } : {}),
    })
  })

  return Array.from(friendOptionMap.values())
}

/**
 * 将群成员响应映射为详情栏成员模型.
 * @param memberList 群成员列表
 * @param ownerId 群主 ID
 * @return ConversationMember[] 群成员展示列表
 */
export const mapGroupMemberList = (
  memberList: GroupMemberRespVO[],
  ownerId?: string,
): ConversationMember[] =>
  memberList
    .filter((member) => Boolean(member.userId))
    .map((member) => ({
      id: member.userId || '',
      name: member.nickname || member.username || '未命名成员',
      role:
        member.role?.toUpperCase() === 'OWNER' || member.userId === ownerId
          ? 'owner'
          : 'member',
      ...(member.avatarUrl ? { avatarUrl: member.avatarUrl } : {}),
    }))
