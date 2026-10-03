export type RealtimeFrameType =
  | 'CONNECT_ACK'
  | 'PONG'
  | 'FRIEND_REQUEST_CREATED'
  | 'FRIEND_REQUEST_ACCEPTED'
  | 'MESSAGE_CREATED'
  | 'CONVERSATION_UPDATED'
  | 'ERROR'

export interface RealtimeFrame<T = unknown> {
  type: RealtimeFrameType
  requestId?: string
  success?: boolean
  code?: number
  message?: string
  timestamp?: number
  data?: T
}

interface WebSocketClientOptions {
  url: string
  getToken: () => string
  getDeviceId: () => string
  onFrame: (frame: RealtimeFrame) => void
  onAuthError?: () => void
  socketFactory?: (url: string) => WebSocket
  random?: () => number
}

const supportedFrameTypeSet = new Set<RealtimeFrameType>([
  'CONNECT_ACK',
  'PONG',
  'FRIEND_REQUEST_CREATED',
  'FRIEND_REQUEST_ACCEPTED',
  'MESSAGE_CREATED',
  'CONVERSATION_UPDATED',
  'ERROR',
])

const createRequestId = (): string =>
  globalThis.crypto?.randomUUID?.() ||
  `web-${Date.now()}-${Math.random().toString(36).slice(2)}`

const parseFrame = (rawValue: string): RealtimeFrame | null => {
  try {
    const value = JSON.parse(rawValue) as Record<string, unknown>
    if (
      !value ||
      typeof value !== 'object' ||
      typeof value.type !== 'string' ||
      !supportedFrameTypeSet.has(value.type as RealtimeFrameType)
    ) {
      console.warn('忽略未知 WebSocket 帧', value)
      return null
    }
    return value as unknown as RealtimeFrame
  } catch {
    console.warn('忽略无法解析的 WebSocket 帧')
    return null
  }
}

/** 维护当前页面唯一的 Union Talk WebSocket 连接。 */
export class UnionTalkWebSocketClient {
  private readonly options: WebSocketClientOptions
  private socket: WebSocket | null = null
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null
  private heartbeatTimer: ReturnType<typeof setInterval> | null = null
  private pendingPingRequestId = ''
  private reconnectAttempt = 0
  private isEnabled = false

  constructor(options: WebSocketClientOptions) {
    this.options = options
  }

  start(): void {
    if (this.isEnabled || !this.options.getToken()) {
      return
    }
    this.isEnabled = true
    this.connect()
  }

  stop(): void {
    this.isEnabled = false
    this.clearTimers()
    const socket = this.socket
    this.socket = null
    socket?.close()
  }

  private connect(): void {
    const token = this.options.getToken()
    if (!this.isEnabled || !token) {
      return
    }

    const url = new URL(this.options.url)
    url.searchParams.set('token', token)
    url.searchParams.set('deviceId', this.options.getDeviceId())
    url.searchParams.set('deviceType', 'WEB')
    const socketFactory =
      this.options.socketFactory ||
      ((nextUrl: string) => new WebSocket(nextUrl))
    const socket = socketFactory(url.toString())
    this.socket = socket

    socket.onopen = () => {
      if (socket !== this.socket) {
        return
      }
      this.reconnectAttempt = 0
      this.startHeartbeat()
    }
    socket.onmessage = (event) => this.handleMessage(event.data)
    socket.onclose = () => {
      if (socket !== this.socket) {
        return
      }
      this.socket = null
      this.stopHeartbeat()
      this.scheduleReconnect()
    }
    socket.onerror = () => socket.close()
  }

  private handleMessage(rawValue: unknown): void {
    if (typeof rawValue !== 'string') {
      console.warn('忽略非文本 WebSocket 帧')
      return
    }
    const frame = parseFrame(rawValue)
    if (!frame) {
      return
    }
    if (frame.type === 'PONG') {
      if (
        this.pendingPingRequestId &&
        frame.requestId === this.pendingPingRequestId
      ) {
        this.pendingPingRequestId = ''
      } else {
        console.warn('忽略不匹配的 WebSocket PONG')
      }
      return
    }
    if (frame.type === 'ERROR' && frame.code === 401) {
      this.isEnabled = false
      this.clearTimers()
      this.options.onAuthError?.()
      const socket = this.socket
      this.socket = null
      socket?.close()
      return
    }
    if (frame.type !== 'CONNECT_ACK' && frame.type !== 'ERROR') {
      this.options.onFrame(frame)
    }
  }

  private startHeartbeat(): void {
    this.stopHeartbeat()
    this.heartbeatTimer = setInterval(() => {
      if (!this.socket || this.socket.readyState !== 1) {
        return
      }
      if (this.pendingPingRequestId) {
        this.socket.close()
        return
      }
      const requestId = createRequestId()
      this.pendingPingRequestId = requestId
      this.socket.send(
        JSON.stringify({
          type: 'PING',
          requestId,
          data: { clientTime: Date.now() },
        }),
      )
    }, 20_000)
  }

  private stopHeartbeat(): void {
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer)
      this.heartbeatTimer = null
    }
    this.pendingPingRequestId = ''
  }

  private scheduleReconnect(): void {
    if (!this.isEnabled || !this.options.getToken()) {
      return
    }
    const baseDelay = Math.min(1_000 * 2 ** this.reconnectAttempt, 30_000)
    const random = this.options.random || Math.random
    const delay = Math.round(baseDelay * (0.8 + random() * 0.4))
    this.reconnectAttempt += 1
    this.reconnectTimer = setTimeout(() => this.connect(), delay)
  }

  private clearTimers(): void {
    this.stopHeartbeat()
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
  }
}
