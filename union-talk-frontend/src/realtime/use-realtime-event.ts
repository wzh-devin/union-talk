import { useContext, useEffect, useRef } from 'react'

import { RealtimeContext } from '@/realtime/realtime-context'
import type { RealtimeFrame } from '@/realtime/websocket-client'

/** 订阅共享连接推送的业务帧。 */
export const useRealtimeEvent = (
  handler: (frame: RealtimeFrame) => void,
): void => {
  const context = useContext(RealtimeContext)
  const handlerRef = useRef(handler)

  useEffect(() => {
    handlerRef.current = handler
  }, [handler])

  useEffect(() => {
    if (!context) {
      return
    }
    return context.subscribe((frame) => handlerRef.current(frame))
  }, [context])
}
