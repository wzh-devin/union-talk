import { sendMessage } from '@/services/generated/message'
import type {
  MessageRespVO,
  SendMessageReqVO,
} from '@/services/generated/message/models'

export const messageType = {
  TEXT: 'TEXT',
  AUDIO: 'AUDIO',
  VIDEO: 'VIDEO',
  FILE: 'FILE',
  EMOJI: 'EMOJI',
} as const

export type MessageType = (typeof messageType)[keyof typeof messageType]

export const messageMentionType = {
  USER: 'USER',
  AGENT: 'AGENT',
  ALL: 'ALL',
} as const

export type MessageMentionType =
  (typeof messageMentionType)[keyof typeof messageMentionType]

export interface MessageMention {
  mentionType: MessageMentionType
  targetId?: string
  displayText: string
  startOffset: number
  length: number
}

export interface MessageAssetInfo {
  id?: string
  conversationId?: string
  name?: string
  fileExt?: string
  fileType?: string
  fileSize?: string
  mimeType?: string
}

export interface MessageAgentInfo {
  agentId?: string
  displayName?: string
}

export interface MessageAgentCitation {
  citationKey?: string
  sourceType?: string
  messageId?: string
  assetFileId?: string
  resourceVersion?: number
  chunkId?: string
  pageFrom?: number
  pageTo?: number
  headingPath?: string
}

export interface RichMessageRespVO extends Omit<
  MessageRespVO,
  'type' | 'mentionList'
> {
  type?: MessageType
  assetInfo?: MessageAssetInfo
  senderType?: string
  senderAgent?: MessageAgentInfo
  mentionList?: MessageMention[]
  agentRunId?: string
  triggerMessageId?: string
  citationList?: MessageAgentCitation[]
}

export interface SendTypedMessageInput {
  conversationId: string
  type: MessageType
  content: string
  quoteMsgId?: string
  mentionList?: MessageMention[]
}

const messageTypeList = Object.values(messageType)

/**
 * 将接口消息类型转换为受支持的业务消息类型.
 * @param value 接口消息类型
 * @return 支持的消息类型，缺失或未知时按文本处理
 */
export const getMessageType = (value?: string): MessageType =>
  messageTypeList.includes(value as MessageType)
    ? (value as MessageType)
    : messageType.TEXT

/**
 * 使用完整业务消息类型调用生成的发送接口.
 * @param input 消息发送参数
 * @return 消息响应
 */
export const sendTypedMessage = (
  input: SendTypedMessageInput,
): Promise<RichMessageRespVO> =>
  sendMessage(
    input as unknown as SendMessageReqVO,
  ) as Promise<RichMessageRespVO>
