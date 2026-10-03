import type { LoginResponse } from '@/services/auth/types'

const authSessionStorageKey = 'union-talk.auth'
const authSessionListenerSet = new Set<() => void>()

/**
 * 通知认证会话订阅者重新读取会话.
 * @return void
 */
const notifyAuthSessionChange = (): void => {
  authSessionListenerSet.forEach((listener) => listener())
}

/**
 * 订阅认证会话变化.
 * @param listener 会话变化监听器
 * @return () => void 取消订阅函数
 */
export const subscribeAuthSession = (listener: () => void): (() => void) => {
  authSessionListenerSet.add(listener)

  return () => authSessionListenerSet.delete(listener)
}

const getAuthStorage = (): Storage | null =>
  typeof localStorage === 'undefined' ? null : localStorage

/**
 * 持久化登录会话.
 * @param user 登录响应用户信息
 * @return 无返回值
 */
export const persistAuthSession = (user: LoginResponse): void => {
  getAuthStorage()?.setItem(
    authSessionStorageKey,
    JSON.stringify({
      token: user.token,
      userId: user.userId,
      username: user.username,
      email: user.email,
      avatarUrl: user.avatarUrl,
    }),
  )
  notifyAuthSessionChange()
}

/**
 * 读取已持久化的登录 token.
 * @return 登录 token，未登录或数据异常时为空字符串
 */
export const getPersistedAuthToken = (): string => {
  const rawSession = getAuthStorage()?.getItem(authSessionStorageKey)

  if (!rawSession) {
    return ''
  }

  try {
    const session = JSON.parse(rawSession) as { token?: unknown }

    return typeof session.token === 'string' ? session.token : ''
  } catch {
    return ''
  }
}

/**
 * 清理本地登录会话.
 * @return 无返回值
 */
export const clearAuthSession = (): void => {
  getAuthStorage()?.removeItem(authSessionStorageKey)
  notifyAuthSessionChange()
}
