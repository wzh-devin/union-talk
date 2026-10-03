import { Badge } from '@/components/shadcn-ui/badge'
import { ScrollArea } from '@/components/shadcn-ui/scroll-area'
import { Separator } from '@/components/shadcn-ui/separator'
import { FriendRequestNotificationPanel } from '@/components/home/notification/friend-request-notification-panel'
import { PanelHeader } from '@/components/home/shared/panel-header'
import type { NotificationItem } from '@/pages/home/model/types'

interface NotificationPanelProps {
  notification: NotificationItem
  isDetailsOpen: boolean
  processingFriendRequestId: string
  onBackToList: () => void
  onToggleDetails: () => void
  onOpenDetailsSheet: () => void
  onHandleFriendRequest: (
    notificationId: string,
    accept: boolean,
  ) => void | Promise<unknown>
}

/**
 * 根据通知分类渲染普通通知或好友申请内容.
 * @param props 通知内容属性
 * @return 对应通知内容
 */
export const NotificationPanel = ({
  notification,
  isDetailsOpen,
  processingFriendRequestId,
  onBackToList,
  onToggleDetails,
  onOpenDetailsSheet,
  onHandleFriendRequest,
}: NotificationPanelProps) => {
  if (notification.category === '好友申请') {
    return (
      <FriendRequestNotificationPanel
        notification={notification}
        isDetailsOpen={isDetailsOpen}
        processingFriendRequestId={processingFriendRequestId}
        onBackToList={onBackToList}
        onToggleDetails={onToggleDetails}
        onOpenDetailsSheet={onOpenDetailsSheet}
        onHandleFriendRequest={onHandleFriendRequest}
      />
    )
  }

  return (
    <>
      <PanelHeader
        title={notification.title}
        eyebrow={`${notification.source} · ${notification.time}`}
        isDetailsOpen={isDetailsOpen}
        onBackToList={onBackToList}
        onToggleDetails={onToggleDetails}
        onOpenDetailsSheet={onOpenDetailsSheet}
      />
      <ScrollArea className="min-h-0 flex-1">
        <div className="mx-auto w-full max-w-3xl px-4 py-8">
          <div className="mb-5 flex items-center gap-2">
            <Badge className="bg-primary text-primary-foreground">
              {notification.status}
            </Badge>
            <Badge
              variant="outline"
              className="border-border text-muted-foreground"
            >
              通知详情
            </Badge>
          </div>
          <p className="text-foreground text-xl font-semibold tracking-normal">
            {notification.title}
          </p>
          <p className="text-muted-foreground mt-3 max-w-2xl text-sm leading-6">
            {notification.summary}
          </p>
          <Separator className="my-6" />
          <p className="text-foreground max-w-2xl text-sm leading-7">
            {notification.body}
          </p>
        </div>
      </ScrollArea>
    </>
  )
}
