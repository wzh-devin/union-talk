import { CheckCheck } from 'lucide-react'

import { Separator } from '@/components/shadcn-ui/separator'
import { DetailAction } from '@/components/home/details/detail-action'
import { DetailGroup } from '@/components/home/details/detail-group'
import { getNotificationStatusAction } from '@/pages/home/model/notification-actions'
import type {
  NotificationItem,
  NotificationStatus,
} from '@/pages/home/model/types'

interface NotificationDetailsPanelProps {
  notification: NotificationItem
  onSetNotificationStatus: (
    notificationId: string,
    status: NotificationStatus,
  ) => void
}

/**
 * 展示通知内容并提供状态切换动作.
 * @param props 通知详情属性
 * @return ReactElement 通知详情面板
 */
export const NotificationDetailsPanel = ({
  notification,
  onSetNotificationStatus,
}: NotificationDetailsPanelProps) => {
  const isFriendRequest = notification.category === '好友申请'
  const isAccepted = notification.status === '已同意'
  const isRejected = notification.status === '已拒绝'
  const { actionLabel, nextStatus } = getNotificationStatusAction(
    notification.status,
  )

  if (isFriendRequest) {
    return (
      <div className="flex min-h-0 flex-1 flex-col p-4">
        <div
          data-testid="request-summary-header"
          className="border-border -mx-4 -mt-4 flex h-14 shrink-0 items-center border-b px-4"
        >
          <p className="text-foreground text-sm font-semibold">请求摘要</p>
        </div>
        <dl className="mt-4 space-y-5 text-xs">
          <div className="flex items-center justify-between gap-3">
            <dt className="text-muted-foreground">来源</dt>
            <dd className="text-foreground truncate font-medium">
              {notification.source}
            </dd>
          </div>
          <div className="flex items-center justify-between gap-3">
            <dt className="text-muted-foreground">收到时间</dt>
            <dd className="text-foreground shrink-0 font-medium">
              {notification.time}
            </dd>
          </div>
          <div className="flex items-center justify-between gap-3">
            <dt className="text-muted-foreground">当前状态</dt>
            <dd
              className={`rounded-md px-2 py-1 font-medium ${
                isAccepted
                  ? 'bg-emerald-50 text-emerald-700 dark:bg-emerald-950 dark:text-emerald-400'
                  : isRejected
                    ? 'bg-destructive/10 text-destructive'
                    : 'bg-muted text-foreground'
              }`}
            >
              {notification.status}
            </dd>
          </div>
        </dl>
      </div>
    )
  }

  return (
    <div className="flex min-h-0 flex-1 flex-col p-4">
      <p className="text-muted-foreground text-xs font-medium">通知详情</p>
      <p className="text-foreground mt-3 text-sm font-semibold">
        {notification.title}
      </p>
      <Separator className="my-4" />
      <DetailGroup
        title="基本信息"
        detailItemList={[
          ['分类', notification.category],
          ['来源', notification.source],
          ['时间', notification.time],
          ['状态', notification.status],
        ]}
      />
      <div className="bg-muted text-muted-foreground mt-5 rounded-lg p-3 text-xs leading-5">
        <p className="text-foreground">{notification.summary}</p>
        <p className="text-muted-foreground mt-2">{notification.body}</p>
      </div>
      <div className="border-border bg-muted/50 text-muted-foreground mt-5 rounded-lg border p-3 text-xs leading-5">
        {notification.actionHint}
      </div>
      <div className="mt-5 space-y-2">
        <DetailAction
          icon={<CheckCheck />}
          label={actionLabel}
          ariaLabel={`标记${notification.title}为${nextStatus}`}
          active={notification.status === '已处理'}
          onClick={() => onSetNotificationStatus(notification.id, nextStatus)}
        />
      </div>
    </div>
  )
}
