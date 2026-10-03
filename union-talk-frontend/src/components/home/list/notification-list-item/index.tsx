import { Badge } from '@/components/shadcn-ui/badge'
import { SelectableListItem } from '@/components/home/shared/selectable-list-item'
import type { NotificationItem } from '@/pages/home/model/types'
import { cn } from '@/utils/class-name'

interface NotificationListItemProps {
  notification: NotificationItem
  active: boolean
  onSelect: () => void
}

export const NotificationListItem = ({
  notification,
  active,
  onSelect,
}: NotificationListItemProps) => (
  <SelectableListItem
    aria-label={notification.title}
    active={active}
    onClick={onSelect}
  >
    <div className="relative z-10 flex items-start gap-2.5">
      <span className="border-border bg-background mt-0.5 grid size-8 shrink-0 place-items-center rounded-full border">
        <span
          className={cn(
            'size-2 rounded-full',
            notification.unread ? 'bg-primary' : 'bg-muted-foreground',
          )}
        />
      </span>
      <div className="min-w-0 flex-1">
        <div className="flex items-center justify-between gap-2">
          <span className="text-foreground truncate text-sm font-medium">
            {notification.title}
          </span>
          <span className="text-muted-foreground shrink-0 text-[11px] whitespace-nowrap">
            {notification.time}
          </span>
        </div>
        <p className="text-muted-foreground mt-0.5 line-clamp-1 text-xs leading-4">
          {notification.summary}
        </p>
        <div className="mt-1.5 flex items-center justify-between gap-2">
          <span className="text-muted-foreground truncate text-[11px]">
            {notification.source}
          </span>
          <Badge
            variant={notification.status === '待处理' ? 'destructive' : 'muted'}
            className="bg-muted text-muted-foreground h-5 border-0"
          >
            {notification.status}
          </Badge>
        </div>
      </div>
    </div>
  </SelectableListItem>
)
