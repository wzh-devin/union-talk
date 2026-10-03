import type { JSONContent } from '@tiptap/core'
import Document from '@tiptap/extension-document'
import HardBreak from '@tiptap/extension-hard-break'
import Mention from '@tiptap/extension-mention'
import Paragraph from '@tiptap/extension-paragraph'
import Placeholder from '@tiptap/extension-placeholder'
import Text from '@tiptap/extension-text'
import { EditorContent, useEditor } from '@tiptap/react'
import type {
  SuggestionKeyDownProps,
  SuggestionProps,
} from '@tiptap/suggestion'
import { Bot } from 'lucide-react'
import { useEffect, useState } from 'react'

import { MENTION_TEXT_CLASS_NAME } from '@/components/home/conversation/mention-style'
import type { ComposerDocument } from '@/pages/home/model/composer-document'
import { serializeComposerDocument } from '@/pages/home/model/composer-document'
import type { MessageMentionType } from '@/services/message/message-contract'
import { getInitials } from '@/utils/avatar'
import { cn } from '@/utils/class-name'

export interface MentionOption {
  id: string
  label: string
  description: string
  mentionType: MessageMentionType
}

interface MentionAttributes {
  id: string
  label: string
  mentionType: MessageMentionType
}

interface SuggestionView {
  props: SuggestionProps<MentionOption, MentionAttributes>
  selectedIndex: number
}

interface StructuredMessageComposerProps {
  document: ComposerDocument
  mentionOptionList: MentionOption[]
  placeholder: string
  disabled: boolean
  onChange: (document: ComposerDocument) => void
  onSend: () => void
}

const StructuredMention = Mention.extend({
  addAttributes() {
    return {
      ...this.parent?.(),
      mentionType: {
        default: null,
        parseHTML: (element) => element.getAttribute('data-mention-type'),
        renderHTML: (attributes) => ({
          'data-mention-type': attributes.mentionType,
        }),
      },
    }
  },
})

/**
 * 比较两个 Tiptap 文档是否一致.
 * @param left 左侧文档
 * @param right 右侧文档
 * @return boolean 是否一致
 */
const isSameEditorContent = (left: JSONContent, right: JSONContent): boolean =>
  JSON.stringify(left) === JSON.stringify(right)

/**
 * 渲染带原子 Mention 节点的消息输入编辑器.
 * @param props 结构化输入器属性
 * @return 结构化消息输入器
 */
export const StructuredMessageComposer = ({
  document,
  mentionOptionList,
  placeholder,
  disabled,
  onChange,
  onSend,
}: StructuredMessageComposerProps) => {
  const [suggestionView, setSuggestionView] = useState<SuggestionView | null>(
    null,
  )

  const editor = useEditor(
    {
      immediatelyRender: false,
      content: document.editorContent,
      extensions: [
        Document,
        Paragraph,
        Text,
        HardBreak,
        Placeholder.configure({ placeholder }),
        StructuredMention.configure({
          deleteTriggerWithBackspace: true,
          renderText: ({ node }) => `@${String(node.attrs.label ?? '')}`,
          renderHTML: ({ node }) => [
            'span',
            {
              'data-type': 'mention',
              'data-id': node.attrs.id,
              'data-label': node.attrs.label,
              'data-mention-type': node.attrs.mentionType,
              class: MENTION_TEXT_CLASS_NAME,
            },
            `@${String(node.attrs.label ?? '')}`,
          ],
          suggestion: {
            char: '@',
            items: ({ query }) => {
              const keyword = query.trim().toLocaleLowerCase()
              return mentionOptionList.filter(
                (option) =>
                  !keyword ||
                  option.label.toLocaleLowerCase().includes(keyword) ||
                  option.description.toLocaleLowerCase().includes(keyword),
              )
            },
            render: () => {
              let selectedIndex = 0
              let currentProps:
                SuggestionProps<MentionOption, MentionAttributes> | undefined
              const present = (
                props: SuggestionProps<MentionOption, MentionAttributes>,
              ): void => {
                currentProps = props
                setSuggestionView({ props, selectedIndex })
              }
              return {
                onStart: present,
                onUpdate: (props) => {
                  selectedIndex = Math.min(
                    selectedIndex,
                    Math.max(props.items.length - 1, 0),
                  )
                  present(props)
                },
                onKeyDown: ({ event }: SuggestionKeyDownProps): boolean => {
                  if (!currentProps) {
                    return false
                  }
                  if (event.key === 'ArrowUp' || event.key === 'ArrowDown') {
                    if (currentProps.items.length === 0) {
                      return true
                    }
                    event.preventDefault()
                    const direction = event.key === 'ArrowUp' ? -1 : 1
                    selectedIndex =
                      (selectedIndex + direction + currentProps.items.length) %
                      currentProps.items.length
                    present(currentProps)
                    return true
                  }
                  if (event.key === 'Enter') {
                    const selectedOption = currentProps.items[selectedIndex]
                    if (selectedOption) {
                      event.preventDefault()
                      currentProps.command(selectedOption)
                    }
                    return true
                  }
                  if (event.key === 'Escape') {
                    currentProps = undefined
                    setSuggestionView(null)
                    return true
                  }
                  return false
                },
                onExit: () => {
                  currentProps = undefined
                  setSuggestionView(null)
                },
              }
            },
          },
        }),
      ],
      editorProps: {
        attributes: {
          role: 'textbox',
          'aria-label': '消息输入框',
          'aria-multiline': 'true',
          'aria-placeholder': placeholder,
          class:
            'min-h-16 max-h-40 overflow-y-auto px-2 py-2 text-sm leading-6 text-foreground outline-none whitespace-pre-wrap [&_p]:min-h-6 [&_.is-editor-empty:first-child]:before:pointer-events-none [&_.is-editor-empty:first-child]:before:float-left [&_.is-editor-empty:first-child]:before:h-0 [&_.is-editor-empty:first-child]:before:text-muted-foreground [&_.is-editor-empty:first-child]:before:content-[attr(data-placeholder)]',
        },
      },
    },
    [mentionOptionList, placeholder],
  )

  useEffect(() => {
    if (!editor) {
      return
    }
    const handleUpdate = (): void => {
      onChange(serializeComposerDocument(editor.getJSON()))
    }
    editor.on('update', handleUpdate)
    return () => {
      editor.off('update', handleUpdate)
    }
  }, [editor, onChange])

  useEffect(() => {
    editor?.setEditable(!disabled)
  }, [disabled, editor])

  useEffect(() => {
    if (
      editor &&
      !isSameEditorContent(editor.getJSON(), document.editorContent)
    ) {
      editor.commands.setContent(document.editorContent, {
        emitUpdate: false,
      })
    }
  }, [document.editorContent, editor])

  return (
    <div className="relative">
      {suggestionView && suggestionView.props.items.length > 0 ? (
        <div className="border-border bg-popover absolute bottom-[calc(100%+8px)] left-0 z-30 w-56 overflow-hidden rounded-xl border p-1 shadow-lg">
          <ul aria-label="可提及对象">
            {suggestionView.props.items.map((option, index) => (
              <li key={`${option.mentionType}:${option.id}`}>
                <button
                  type="button"
                  className={cn(
                    'flex w-full items-center gap-2 rounded-lg px-2 py-2 text-left',
                    index === suggestionView.selectedIndex
                      ? 'bg-accent text-accent-foreground'
                      : 'hover:bg-accent/60',
                  )}
                  onMouseDown={(event) => event.preventDefault()}
                  onClick={() => suggestionView.props.command(option)}
                >
                  <span
                    className={cn(
                      'grid size-7 shrink-0 place-items-center rounded-full text-[10px] font-semibold text-white',
                      option.mentionType === 'AGENT'
                        ? 'bg-black'
                        : 'bg-cyan-700',
                    )}
                  >
                    {option.mentionType === 'AGENT' ? (
                      <Bot aria-hidden className="size-3.5" />
                    ) : (
                      getInitials(option.label)
                    )}
                  </span>
                  <span className="min-w-0">
                    <span className="block truncate text-xs font-medium">
                      {option.mentionType === 'AGENT'
                        ? 'AI 助手'
                        : option.label}
                    </span>
                    <span className="text-muted-foreground block text-[11px]">
                      {option.description}
                    </span>
                  </span>
                </button>
              </li>
            ))}
          </ul>
        </div>
      ) : null}
      <EditorContent
        editor={editor}
        onKeyDownCapture={(event) => {
          if (
            event.key === 'Enter' &&
            !event.shiftKey &&
            !disabled &&
            !suggestionView
          ) {
            event.preventDefault()
            onSend()
          }
        }}
      />
    </div>
  )
}
