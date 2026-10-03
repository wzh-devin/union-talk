import type { NotificationStatus } from '@/pages/home/model/types'

export interface NotificationStatusAction {
  actionLabel: string
  nextStatus: NotificationStatus
}

/**
 * 获取通知状态按钮的下一步动作.
 * @param status 当前通知状态
 * @return NotificationStatusAction 下一状态及按钮文案
 */
export const getNotificationStatusAction = (
  status: NotificationStatus,
): NotificationStatusAction =>
  status === '已处理'
    ? { actionLabel: '标记为未读', nextStatus: '未读' }
    : { actionLabel: '标记为已处理', nextStatus: '已处理' }
