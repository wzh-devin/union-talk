import { Avatar, AvatarFallback } from '@/components/shadcn-ui/avatar'
import { MessageContent } from '@/components/home/conversation/message-content'
import { cn } from '@/utils/class-name'
import type { Message } from '@/pages/home/model/types'
import { getInitials } from '@/utils/avatar'

interface MessageBubbleProps {
  message: Message
  currentUserName: string
  animateEntrance?: boolean
}

/**
 * 渲染单条会话消息并展示对应发送者信息.
 * @param props 消息气泡属性
 * @return 消息气泡
 */
export const MessageBubble = ({
  message,
  currentUserName,
  animateEntrance = false,
}: MessageBubbleProps) => {
  const isSelf = message.tone === 'self'
  const displayAuthor = isSelf ? currentUserName : message.author

  return (
    <article
      className={cn(
        'flex gap-3',
        isSelf && 'justify-end',
        animateEntrance &&
          !isSelf &&
          'animate-message-in motion-reduce:animate-none',
      )}
    >
      {!isSelf ? (
        <Avatar className="border-border mt-1 size-9 border">
          <AvatarFallback className="bg-cyan-700 text-white">
            {getInitials(message.author)}
          </AvatarFallback>
        </Avatar>
      ) : null}
      <div className={cn('max-w-[min(680px,85%)]', isSelf && 'text-right')}>
        <div
          className={cn(
            'mb-1 flex items-center gap-2 text-xs',
            isSelf
              ? 'text-muted-foreground justify-end'
              : 'text-muted-foreground',
          )}
        >
          <span className="font-medium">{displayAuthor}</span>
          {!isSelf && message.role ? <span>{message.role}</span> : null}
          <span>{message.time}</span>
        </div>
        <MessageContent message={message} isSelf={isSelf} />
      </div>
    </article>
  )
}
