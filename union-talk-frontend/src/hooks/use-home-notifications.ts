import { useCallback, useEffect, useState } from 'react'
import { toast } from 'sonner'

import {
  getReceivedRequestList,
  handleRequest,
} from '@/services/generated/user'
import { mapFriendRequestNotification } from '@/pages/home/model/api-adapters'
import type {
  NotificationItem,
  NotificationStatus,
} from '@/pages/home/model/types'
import type { FriendRequestRespVO } from '@/services/generated/user/models'

/** 管理好友申请通知的加载、阅读与服务端处理流程。 */
export const useHomeNotifications = (
  refreshConversations?: () => Promise<unknown>,
) => {
  const [notificationList, setNotificationList] = useState<NotificationItem[]>(
    [],
  )
  const [processingFriendRequestId, setProcessingFriendRequestId] = useState('')

  useEffect(() => {
    let isCurrent = true

    void getReceivedRequestList()
      .then((requestList) => {
        if (isCurrent) {
          setNotificationList(requestList.map(mapFriendRequestNotification))
        }
      })
      .catch(() => toast.error('好友申请加载失败，请稍后重试'))

    return () => {
      isCurrent = false
    }
  }, [])

  const markNotificationRead = useCallback((notificationId: string): void => {
    setNotificationList((currentList) =>
      currentList.map((notification) =>
        notification.id === notificationId
          ? {
              ...notification,
              status:
                notification.status === '未读' ? '已读' : notification.status,
              unread: false,
            }
          : notification,
      ),
    )
  }, [])

  const setNotificationStatus = useCallback(
    (notificationId: string, status: NotificationStatus): void => {
      setNotificationList((currentList) =>
        currentList.map((notification) =>
          notification.id === notificationId
            ? { ...notification, status, unread: status === '未读' }
            : notification,
        ),
      )
    },
    [],
  )

  /**
   * 调用服务端处理好友申请并同步通知状态.
   * @param notificationId 通知标识
   * @param accept 是否同意申请
   * @return 是否处理成功
   */
  const handleFriendRequest = useCallback(
    async (notificationId: string, accept: boolean): Promise<boolean> => {
      const notification = notificationList.find(
        (item) => item.id === notificationId,
      )

      if (!notification?.requestId) {
        toast.error('好友申请缺少有效标识，无法处理')
        return false
      }

      setProcessingFriendRequestId(notificationId)
      try {
        const isHandled = await handleRequest({
          requestId: notification.requestId,
          accept,
        })
        if (!isHandled) {
          throw new Error('好友申请处理失败')
        }

        const status: NotificationStatus = accept ? '已同意' : '已拒绝'
        setNotificationList((currentList) =>
          currentList.map((item) =>
            item.id === notificationId
              ? {
                  ...item,
                  status,
                  unread: false,
                  actionHint: accept
                    ? '你已同意该好友申请。'
                    : '你已拒绝该好友申请。',
                }
              : item,
          ),
        )
        toast.success(accept ? '已同意好友申请' : '已拒绝好友申请')

        if (accept && refreshConversations) {
          try {
            await refreshConversations()
          } catch {
            toast.error('好友与会话列表刷新失败，请稍后重试')
          }
        }

        return true
      } catch {
        toast.error('好友申请处理失败，请稍后重试')
        return false
      } finally {
        setProcessingFriendRequestId('')
      }
    },
    [notificationList, refreshConversations],
  )

  const upsertFriendRequest = useCallback(
    (request: FriendRequestRespVO): void => {
      const notification = mapFriendRequestNotification(request)

      setNotificationList((currentList) => [
        notification,
        ...currentList.filter((item) => item.id !== notification.id),
      ])
    },
    [],
  )

  return {
    notificationList,
    markNotificationRead,
    setNotificationStatus,
    processingFriendRequestId,
    handleFriendRequest,
    upsertFriendRequest,
  }
}
