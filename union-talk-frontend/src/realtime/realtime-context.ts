import { createContext } from 'react'

import type { RealtimeFrame } from '@/realtime/websocket-client'

export interface RealtimeContextValue {
  subscribe: (listener: (frame: RealtimeFrame) => void) => () => void
}

export const RealtimeContext = createContext<RealtimeContextValue | null>(null)
