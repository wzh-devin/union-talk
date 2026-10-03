import { useSyncExternalStore } from 'react'
import { Navigate, Outlet } from 'react-router'

import {
  getPersistedAuthToken,
  subscribeAuthSession,
} from '@/services/auth/session-service'

/** 保护需要登录会话的路由，并响应运行时 401 清理会话事件。 */
export const RequireAuth = () => {
  const token = useSyncExternalStore(
    subscribeAuthSession,
    getPersistedAuthToken,
    () => '',
  )

  return token ? <Outlet /> : <Navigate to="/auth" replace />
}
