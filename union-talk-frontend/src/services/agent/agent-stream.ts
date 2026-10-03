import { getPersistedAuthToken } from '@/services/auth/session-service'
import type { AgentSseEvent } from '@/services/agent/agent-contract'

interface ConnectAgentRunStreamInput {
  runId: string
  signal: AbortSignal
  lastEventId?: string
  onEvent: (event: AgentSseEvent) => void
}

const getAuthorizationValue = (): string => {
  return getPersistedAuthToken()
}

/**
 * 解析一个完整 SSE 数据块.
 * @param block SSE 数据块
 * @return Agent SSE 事件，注释或非法数据返回空
 */
export const parseAgentSseBlock = (block: string): AgentSseEvent | null => {
  if (!block.trim() || block.trimStart().startsWith(':')) {
    return null
  }

  let eventId = ''
  let eventName = ''
  const dataLineList: string[] = []
  block.split(/\r?\n/u).forEach((line) => {
    if (line.startsWith('id:')) {
      eventId = line.slice(3).trim()
    } else if (line.startsWith('event:')) {
      eventName = line.slice(6).trim()
    } else if (line.startsWith('data:')) {
      dataLineList.push(line.slice(5).trimStart())
    }
  })

  if (
    !['snapshot', 'agent-event', 'realtime-unavailable'].includes(eventName) ||
    dataLineList.length === 0
  ) {
    return null
  }

  try {
    return {
      eventId,
      eventName: eventName as AgentSseEvent['eventName'],
      payload: JSON.parse(dataLineList.join('\n')) as AgentSseEvent['payload'],
    }
  } catch {
    return null
  }
}

/**
 * 连接单个 Agent Run 的鉴权 SSE 流.
 * @param input 流连接参数
 * @return SSE 流关闭后的异步流程
 */
export const connectAgentRunStream = async (
  input: ConnectAgentRunStreamInput,
): Promise<void> => {
  const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || '/api/v1'
  const streamUrl = new URL(
    `${apiBaseUrl.replace(/\/$/u, '')}/agent/runs/${input.runId}/events`,
    globalThis.location?.origin || 'http://localhost',
  )
  const authorizationValue = getAuthorizationValue()
  const response = await fetch(streamUrl, {
    headers: {
      Accept: 'text/event-stream',
      ...(authorizationValue ? { Authorization: authorizationValue } : {}),
      ...(input.lastEventId ? { 'Last-Event-ID': input.lastEventId } : {}),
    },
    signal: input.signal,
  })

  if (!response.ok || !response.body) {
    throw new Error(`Agent SSE 连接失败: ${response.status}`)
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let pendingText = ''
  while (true) {
    const { done, value } = await reader.read()
    pendingText += decoder
      .decode(value, { stream: !done })
      .replace(/\r\n/gu, '\n')
    let separatorIndex = pendingText.indexOf('\n\n')
    while (separatorIndex >= 0) {
      const block = pendingText.slice(0, separatorIndex)
      pendingText = pendingText.slice(separatorIndex + 2)
      const event = parseAgentSseBlock(block)
      if (event) {
        input.onEvent(event)
      }
      separatorIndex = pendingText.indexOf('\n\n')
    }
    if (done) {
      break
    }
  }
}
