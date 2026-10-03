import type {
  MessageAgentCitation,
  MessageAssetInfo,
  MessageMention,
  MessageType,
} from '@/services/message/message-contract'

export type Section = 'notifications' | 'friends' | 'groups' | 'profile'
export type WorkspaceSection = Exclude<Section, 'profile'>
export type ConversationKind = 'friend' | 'group'
export type ConversationMemberRole = 'owner' | 'member'
export type FriendRelationStatus = 'normal' | 'blocked'
export type ProfilePane = 'account' | 'privacy' | 'devices'
export type NotificationStatus =
  '未读' | '已读' | '待处理' | '已处理' | '已同意' | '已拒绝'

export interface Message {
  id: string
  conversationId?: string
  senderId?: string
  createdAt?: string
  author: string
  role: string
  time: string
  body: string
  /** 历史本地消息未标注类型时按 TEXT 渲染。 */
  type?: MessageType
  assetInfo?: MessageAssetInfo
  tone?: 'self' | 'system' | 'agent'
  senderType?: string
  agentRunId?: string
  triggerMessageId?: string
  agentName?: string
  citationList?: MessageAgentCitation[]
  mentionList?: MessageMention[]
}

export interface ConversationMember {
  id: string
  name: string
  role: ConversationMemberRole
  avatarUrl?: string
}

export interface AddFriendFormInput {
  friendCode: string
  applyMessage: string
}

export interface CreateGroupFormInput {
  name: string
  description: string
  friendIdList: string[]
}

export interface FriendOption {
  id: string
  name: string
  accent: string
  avatarUrl?: string
}

export interface Conversation {
  id: string
  groupId?: string
  targetUserId?: string
  lastMessageId?: string
  ownerId?: string
  avatarUrl?: string
  groupMemberLimit?: number
  friendRelationStatus?: FriendRelationStatus
  friendUsername?: string
  friendRemark?: string
  kind: ConversationKind
  name: string
  description: string
  status: string
  lastMessage: string
  lastActive: string
  unreadCount: number
  memberList: ConversationMember[]
  tags: string[]
  accent: string
  messages: Message[]
  code: string
  relation: string
  isPinned: boolean
  isMuted: boolean
  isFavorite: boolean
}

export interface NotificationItem {
  id: string
  requestId?: string
  title: string
  source: string
  time: string
  summary: string
  body: string
  status: NotificationStatus
  unread: boolean
  category: string
  actionHint: string
}

export interface DeviceSummary {
  id: string
  name: string
  location: string
  lastActive: string
  isCurrent: boolean
}

export interface ProfileSettings {
  userId: string
  code: string
  displayName: string
  email: string
  avatarUrl: string
  bio: string
  needFriendVerify: boolean
  status: string
  lastLoginAt: string
  privacyLabel: string
  trustedDeviceList: DeviceSummary[]
}

export interface ProfilePaneOption {
  id: ProfilePane
  label: string
  description: string
}
