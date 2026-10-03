import type { ReactNode } from 'react'

import { MENTION_TEXT_CLASS_NAME } from '@/components/home/conversation/mention-style'
import type { MessageMention } from '@/services/message/message-contract'

interface MessageMentionTextProps {
  content: string
  mentionList: MessageMention[]
}

/**
 * 按后端 offsets 渲染消息正文中的结构化 Mention.
 * @param props 消息正文和提及列表
 * @return 带独立 Mention 样式的消息正文
 */
export const MessageMentionText = ({
  content,
  mentionList,
}: MessageMentionTextProps) => {
  if (mentionList.length === 0) {
    return content
  }
  const contentNodeList: ReactNode[] = []
  let cursor = 0

  mentionList
    .slice()
    .sort((left, right) => left.startOffset - right.startOffset)
    .forEach((mention, index) => {
      const endOffset = mention.startOffset + mention.length
      const isValid =
        mention.startOffset >= cursor &&
        mention.length > 0 &&
        endOffset <= content.length &&
        content.slice(mention.startOffset, endOffset) === mention.displayText
      if (!isValid) {
        return
      }
      if (mention.startOffset > cursor) {
        contentNodeList.push(content.slice(cursor, mention.startOffset))
      }
      contentNodeList.push(
        <span
          key={`${mention.mentionType}:${mention.targetId ?? 'all'}:${mention.startOffset}:${index}`}
          data-mention-type={mention.mentionType}
          data-mention-target-id={mention.targetId}
          className={MENTION_TEXT_CLASS_NAME}
        >
          {mention.displayText}
        </span>,
      )
      cursor = endOffset
    })

  if (cursor < content.length) {
    contentNodeList.push(content.slice(cursor))
  }

  return <>{contentNodeList}</>
}
