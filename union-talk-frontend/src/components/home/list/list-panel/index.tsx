import { Search } from 'lucide-react'
import type { ReactNode } from 'react'

import { Input } from '@/components/shadcn-ui/input'
import { ScrollArea } from '@/components/shadcn-ui/scroll-area'
import { ConversationListItem } from '@/components/home/list/conversation-list-item'
import { NotificationListItem } from '@/components/home/list/notification-list-item'
import { sectionMetaMap } from '@/pages/home/model/constants'
import type { Conversation, NotificationItem } from '@/pages/home/model/types'

interface ConversationListPanelProps {
  kind: 'conversation'
  activeSection: 'friends' | 'groups'
  conversationList: Conversation[]
  selectedId: string
  toolbarAction?: ReactNode
  onSelectConversation: (conversationId: string) => void
}

interface NotificationListPanelProps {
  kind: 'notification'
  activeSection: 'notifications'
  notificationList: NotificationItem[]
  selectedId: string
  toolbarAction?: ReactNode
  onSelectNotification: (notificationId: string) => void
}

type ListPanelProps = ConversationListPanelProps | NotificationListPanelProps

/**
 * 根据当前领域渲染会话或通知列表.
 * @param props 列表领域属性
 * @return 对应领域列表
 */
export const ListPanel = (props: ListPanelProps) => {
  const meta = sectionMetaMap[props.activeSection]

  return (
    <>
      <div
        className="border-border flex h-14 shrink-0 items-center gap-2 border-b px-3"
        data-testid="list-panel-toolbar"
      >
        <div className="relative min-w-0 flex-1">
          <Search className="text-muted-foreground pointer-events-none absolute top-1/2 left-2.5 size-4 -translate-y-1/2" />
          <Input
            aria-label={meta.searchPlaceholder}
            placeholder={meta.searchPlaceholder}
            className="border-border bg-background text-foreground placeholder:text-muted-foreground h-8 pl-8"
          />
        </div>
        {props.toolbarAction}
      </div>

      <ScrollArea className="min-h-0 flex-1">
        <div className="space-y-1 p-2">
          {props.kind === 'notification'
            ? props.notificationList.map((notification) => (
                <NotificationListItem
                  key={notification.id}
                  notification={notification}
                  active={notification.id === props.selectedId}
                  onSelect={() => props.onSelectNotification(notification.id)}
                />
              ))
            : props.conversationList.map((conversation) => (
                <ConversationListItem
                  key={conversation.id}
                  conversation={conversation}
                  active={conversation.id === props.selectedId}
                  onSelect={() => props.onSelectConversation(conversation.id)}
                />
              ))}
        </div>
      </ScrollArea>
    </>
  )
}
