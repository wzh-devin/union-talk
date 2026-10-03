import type { JSONContent } from '@tiptap/core'

import type {
  MessageMention,
  MessageMentionType,
} from '@/services/message/message-contract'

export interface ComposerDocument {
  editorContent: JSONContent
  content: string
  mentionList: MessageMention[]
}

export const emptyComposerDocument: ComposerDocument = {
  editorContent: {
    type: 'doc',
    content: [{ type: 'paragraph' }],
  },
  content: '',
  mentionList: [],
}

/**
 * 将 Tiptap 文档序列化为 Message Service 正文和结构化提及列表.
 * @param editorContent Tiptap 编辑器文档
 * @return ComposerDocument 可直接发送的消息草稿
 */
export const serializeComposerDocument = (
  editorContent: JSONContent,
): ComposerDocument => {
  let content = ''
  const mentionList: MessageMention[] = []
  const paragraphList = editorContent.content ?? []

  paragraphList.forEach((paragraph, paragraphIndex) => {
    if (paragraphIndex > 0) {
      content += '\n'
    }
    ;(paragraph.content ?? []).forEach((node) => {
      if (node.type === 'text') {
        content += node.text ?? ''
        return
      }
      if (node.type === 'hardBreak') {
        content += '\n'
        return
      }
      if (node.type !== 'mention') {
        return
      }
      const mentionType = String(node.attrs?.mentionType) as MessageMentionType
      const targetId = String(node.attrs?.id ?? '')
      const displayText = `@${String(node.attrs?.label ?? '')}`
      mentionList.push({
        mentionType,
        ...(targetId ? { targetId } : {}),
        displayText,
        startOffset: content.length,
        length: displayText.length,
      })
      content += displayText
    })
  })

  return { editorContent, content, mentionList }
}

/**
 * 判断草稿是否包含指定类型的结构化提及.
 * @param document 消息草稿
 * @param mentionType 提及类型
 * @return boolean 是否存在目标提及
 */
export const hasComposerMentionType = (
  document: ComposerDocument,
  mentionType: MessageMentionType,
): boolean =>
  document.mentionList.some((mention) => mention.mentionType === mentionType)
