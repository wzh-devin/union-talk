import { Smile } from 'lucide-react'

import { Button } from '@/components/shadcn-ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/shadcn-ui/dropdown-menu'

interface MessageEmojiMenuProps {
  disabled?: boolean
  onSelectEmoji: (emoji: string) => void
}

const commonEmojiList = [
  '😀',
  '😂',
  '🥰',
  '😍',
  '🤔',
  '👏',
  '👍',
  '🎉',
  '❤️',
  '🔥',
  '😭',
  '🙏',
] as const

/**
 * 展示常用 Emoji 并在选择后立即发送.
 * @param props 禁用状态和选择回调
 * @return Emoji 菜单
 */
export const MessageEmojiMenu = ({
  disabled = false,
  onSelectEmoji,
}: MessageEmojiMenuProps) => (
  <DropdownMenu>
    <DropdownMenuTrigger asChild>
      <Button
        type="button"
        variant="ghost"
        size="icon-sm"
        aria-label="添加表情"
        disabled={disabled}
        className="text-muted-foreground hover:bg-muted hover:text-foreground"
      >
        <Smile />
      </Button>
    </DropdownMenuTrigger>
    <DropdownMenuContent
      side="top"
      align="start"
      className="grid min-w-0 grid-cols-6 gap-1 p-2"
    >
      {commonEmojiList.map((emoji) => (
        <DropdownMenuItem
          key={emoji}
          aria-label={`发送表情 ${emoji}`}
          className="grid size-9 place-items-center p-0 text-xl"
          onSelect={() => onSelectEmoji(emoji)}
        >
          {emoji}
        </DropdownMenuItem>
      ))}
    </DropdownMenuContent>
  </DropdownMenu>
)
