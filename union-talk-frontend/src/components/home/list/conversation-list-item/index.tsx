import { Badge } from '@/components/shadcn-ui/badge'
import { NamedAvatar } from '@/components/home/shared/named-avatar'
import { SelectableListItem } from '@/components/home/shared/selectable-list-item'
import type { Conversation } from '@/pages/home/model/types'

interface ConversationListItemProps {
  conversation: Conversation
  active: boolean
  onSelect: () => void
}

export const ConversationListItem = ({
  conversation,
  active,
  onSelect,
}: ConversationListItemProps) => (
  <SelectableListItem
    aria-label={conversation.name}
    active={active}
    onClick={onSelect}
  >
    <div className="relative z-10 flex items-start gap-2.5">
      <NamedAvatar
        name={conversation.name}
        avatarUrl={conversation.avatarUrl}
        className="border-border mt-0.5 size-8 border"
        fallbackClassName={conversation.accent}
      />
      <div className="min-w-0 flex-1">
        <div className="flex items-center justify-between gap-2">
          <span className="text-foreground truncate text-sm font-medium">
            {conversation.name}
          </span>
          <span className="text-muted-foreground shrink-0 text-[11px]">
            {conversation.lastActive}
          </span>
        </div>
        <p className="text-muted-foreground mt-0.5 line-clamp-1 text-xs leading-4">
          {conversation.lastMessage}
        </p>
        <div className="mt-1.5 flex items-center justify-between gap-2">
          <span className="text-muted-foreground truncate text-[11px]">
            {conversation.status}
          </span>
          {conversation.unreadCount > 0 ? (
            <Badge className="bg-primary text-primary-foreground h-5 min-w-5 rounded-full px-1">
              {conversation.unreadCount}
            </Badge>
          ) : null}
        </div>
      </div>
    </div>
  </SelectableListItem>
)
