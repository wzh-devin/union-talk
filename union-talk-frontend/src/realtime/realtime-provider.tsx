import {
  useEffect,
  useMemo,
  useRef,
  useSyncExternalStore,
  type ReactNode,
} from 'react'

import { RealtimeContext } from '@/realtime/realtime-context'
import {
  clearAuthSession,
  getPersistedAuthToken,
  subscribeAuthSession,
} from '@/services/auth/session-service'
import {
  UnionTalkWebSocketClient,
  type RealtimeFrame,
} from '@/realtime/websocket-client'
import { resolveWebSocketUrl } from '@/realtime/websocket-url'

const deviceStorageKey = 'union-talk.device-id'

const getDeviceId = (): string => {
  const existingId = localStorage.getItem(deviceStorageKey)
  if (existingId) {
    return existingId
  }
  const deviceId =
    globalThis.crypto?.randomUUID?.() ||
    `web-${Date.now()}-${Math.random().toString(36).slice(2)}`
  localStorage.setItem(deviceStorageKey, deviceId)
  return deviceId
}

/** 在应用单页范围内共享一个 WebSocket 连接。 */
export const RealtimeProvider = ({ children }: { children: ReactNode }) => {
  const token = useSyncExternalStore(
    subscribeAuthSession,
    getPersistedAuthToken,
    () => '',
  )
  const listenerSetRef = useRef(new Set<(frame: RealtimeFrame) => void>())

  const contextValue = useMemo(
    () => ({
      subscribe: (listener: (frame: RealtimeFrame) => void) => {
        listenerSetRef.current.add(listener)
        return () => listenerSetRef.current.delete(listener)
      },
    }),
    [],
  )

  useEffect(() => {
    if (!token) {
      return
    }
    const client = new UnionTalkWebSocketClient({
      url: resolveWebSocketUrl(import.meta.env.VITE_WS_URL, window.location),
      getToken: () => token,
      getDeviceId,
      onFrame: (frame) =>
        listenerSetRef.current.forEach((listener) => {
          try {
            listener(frame)
          } catch (error) {
            console.warn('忽略 WebSocket 业务帧处理异常', error)
          }
        }),
      onAuthError: clearAuthSession,
    })
    client.start()
    return () => client.stop()
  }, [token])

  return (
    <RealtimeContext.Provider value={contextValue}>
      {children}
    </RealtimeContext.Provider>
  )
}
