import type {
  AgentContentBlockType,
  AgentMessageSnapshot,
  AgentRunEvent,
  AgentRunSnapshot,
  AgentToolExecution,
} from '@/pages/home/model/agent-run'
import { isAgentRunTerminal } from '@/pages/home/model/agent-run'

export interface AgentRealtimeState {
  snapshotByRunId: Record<string, AgentRunSnapshot>
  disconnectedRunIdSet: ReadonlySet<string>
}

export type AgentRealtimeAction =
  | { type: 'snapshot_received'; snapshot: AgentRunSnapshot }
  | { type: 'agent_event_received'; event: AgentRunEvent }
  | { type: 'official_message_received'; runId: string; messageId: string }
  | { type: 'stream_disconnected'; runId: string }
  | { type: 'reset' }

export const initialAgentRealtimeState: AgentRealtimeState = {
  snapshotByRunId: {},
  disconnectedRunIdSet: new Set<string>(),
}

const providerBlockTypeMap: Partial<Record<string, AgentContentBlockType>> = {
  text_start: 'TEXT',
  text_delta: 'TEXT',
  text_end: 'TEXT',
  thinking_start: 'THINKING',
  thinking_delta: 'THINKING',
  thinking_end: 'THINKING',
  toolcall_start: 'TOOL_CALL',
  toolcall_delta: 'TOOL_CALL',
  toolcall_end: 'TOOL_CALL',
}

/**
 * 创建缺失的 partial assistant message.
 * @param snapshot 当前 Run 快照
 * @param event Agent Loop 事件
 * @return 当前事件对应的消息
 */
const ensureMessage = (
  snapshot: AgentRunSnapshot,
  event: AgentRunEvent,
): AgentMessageSnapshot | undefined => {
  if (!event.messageKey || event.turnNo === null) {
    return undefined
  }
  const existing = snapshot.messageList.find(
    (message) => message.messageKey === event.messageKey,
  )
  if (existing) {
    return existing
  }
  const message: AgentMessageSnapshot = {
    turnNo: event.turnNo,
    messageKey: event.messageKey,
    role: String(event.payload.role ?? 'assistant'),
    status: 'STREAMING',
    modelId: String(event.payload.modelId ?? ''),
    contentBlockList: [],
    stopReason: null,
    inputTokens: 0,
    outputTokens: 0,
  }
  snapshot.messageList.push(message)
  return message
}

/**
 * 将 message_update 增量归并到唯一 Content Block.
 * @param snapshot 当前可变快照副本
 * @param event Agent Loop 消息事件
 * @return 无返回值
 */
const applyMessageUpdate = (
  snapshot: AgentRunSnapshot,
  event: AgentRunEvent,
): void => {
  const message = ensureMessage(snapshot, event)
  const assistantEvent = event.payload.assistantMessageEvent
  if (
    !message ||
    !assistantEvent ||
    assistantEvent.contentIndex === undefined
  ) {
    return
  }
  const blockType = providerBlockTypeMap[assistantEvent.type]
  if (!blockType) {
    return
  }
  let block = message.contentBlockList.find(
    (item) => item.contentIndex === assistantEvent.contentIndex,
  )
  if (!block) {
    block = {
      contentIndex: assistantEvent.contentIndex,
      blockType,
      status: 'STREAMING',
      content: '',
      toolCallId: assistantEvent.toolCallId ?? null,
      toolName: assistantEvent.toolName ?? null,
    }
    message.contentBlockList.push(block)
  }
  if (assistantEvent.delta) {
    block.content += assistantEvent.delta
  }
  if (assistantEvent.type.endsWith('_end')) {
    block.status = 'COMPLETED'
  }
}

/**
 * 将工具执行事件归并到工具调用业务键.
 * @param snapshot 当前可变快照副本
 * @param event Agent Loop 工具事件
 * @return 无返回值
 */
const applyToolExecution = (
  snapshot: AgentRunSnapshot,
  event: AgentRunEvent,
): void => {
  const toolCallId = event.payload.toolCallId
  if (typeof toolCallId !== 'string' || event.turnNo === null) {
    return
  }
  let execution = snapshot.toolExecutionList.find(
    (item) => item.toolCallId === toolCallId,
  )
  if (!execution) {
    execution = {
      toolCallId,
      turnNo: event.turnNo,
      toolName: String(event.payload.toolName ?? ''),
      displayName: String(event.payload.displayName ?? ''),
      status: 'RUNNING',
      argumentsSummary: {},
      resultSummary: {},
      errorCode: null,
      errorMessage: null,
      durationMs: null,
    }
    snapshot.toolExecutionList.push(execution)
  }
  if (typeof event.payload.status === 'string') {
    execution.status = event.payload.status as AgentToolExecution['status']
  }
  if (
    typeof event.payload.argumentsSummary === 'object' &&
    event.payload.argumentsSummary !== null
  ) {
    execution.argumentsSummary = event.payload.argumentsSummary as Record<
      string,
      unknown
    >
  }
  if (
    typeof event.payload.resultSummary === 'object' &&
    event.payload.resultSummary !== null
  ) {
    execution.resultSummary = event.payload.resultSummary as Record<
      string,
      unknown
    >
  }
  execution.errorCode =
    typeof event.payload.errorCode === 'string' ? event.payload.errorCode : null
  execution.errorMessage =
    typeof event.payload.errorMessage === 'string'
      ? event.payload.errorMessage
      : null
  execution.durationMs =
    typeof event.payload.durationMs === 'number'
      ? event.payload.durationMs
      : null
}

/**
 * 按单调 sequence 将 V2 Agent Loop 事件物化为快照.
 * @param currentSnapshot 当前快照
 * @param event 新增 Agent 事件
 * @return 幂等归并后的快照
 */
export const applyAgentRunEvent = (
  currentSnapshot: AgentRunSnapshot,
  event: AgentRunEvent,
): AgentRunSnapshot => {
  if (
    currentSnapshot.runId !== event.runId ||
    event.sequence <= currentSnapshot.lastSequence ||
    (isAgentRunTerminal(currentSnapshot.status) &&
      !isAgentRunTerminal(event.status))
  ) {
    return currentSnapshot
  }
  const snapshot: AgentRunSnapshot = structuredClone(currentSnapshot)
  snapshot.status = event.status
  snapshot.stage = event.stage
  snapshot.lastSequence = event.sequence
  snapshot.currentTurnNo = event.turnNo ?? snapshot.currentTurnNo
  snapshot.updatedAt = event.occurredAt

  if (event.type === 'message_start') {
    ensureMessage(snapshot, event)
  } else if (event.type === 'message_update') {
    applyMessageUpdate(snapshot, event)
  } else if (event.type === 'message_end') {
    const message = ensureMessage(snapshot, event)
    if (message) {
      message.status = String(
        event.payload.status ?? 'COMPLETED',
      ) as AgentMessageSnapshot['status']
      message.stopReason =
        typeof event.payload.stopReason === 'string'
          ? event.payload.stopReason
          : null
      const usage = event.payload.usage
      if (typeof usage === 'object' && usage !== null) {
        const typedUsage = usage as Record<string, unknown>
        message.inputTokens = Number(typedUsage.inputTokens ?? 0)
        message.outputTokens = Number(typedUsage.outputTokens ?? 0)
      }
    }
  } else if (event.type.startsWith('tool_execution_')) {
    applyToolExecution(snapshot, event)
  } else if (event.type === 'agent_end') {
    snapshot.answerMessageId =
      typeof event.payload.answerMessageId === 'string'
        ? event.payload.answerMessageId
        : snapshot.answerMessageId
    snapshot.errorCode =
      typeof event.payload.errorCode === 'string'
        ? event.payload.errorCode
        : null
    snapshot.errorMessage =
      typeof event.payload.errorMessage === 'string'
        ? event.payload.errorMessage
        : null
  }
  if (Array.isArray(event.payload.citationList)) {
    snapshot.citationList = event.payload
      .citationList as AgentRunSnapshot['citationList']
  }
  return snapshot
}

/**
 * 归并快照、增量、正式消息与断线状态.
 * @param state 当前 Agent 实时状态
 * @param action 实时状态动作
 * @return 下一状态
 */
export const agentRealtimeReducer = (
  state: AgentRealtimeState,
  action: AgentRealtimeAction,
): AgentRealtimeState => {
  if (action.type === 'reset') {
    return initialAgentRealtimeState
  }
  if (action.type === 'snapshot_received') {
    const current = state.snapshotByRunId[action.snapshot.runId]
    const snapshot =
      current && current.lastSequence > action.snapshot.lastSequence
        ? current
        : action.snapshot
    const disconnectedRunIdSet = new Set(state.disconnectedRunIdSet)
    disconnectedRunIdSet.delete(action.snapshot.runId)
    return {
      snapshotByRunId: {
        ...state.snapshotByRunId,
        [action.snapshot.runId]: snapshot,
      },
      disconnectedRunIdSet,
    }
  }
  if (action.type === 'agent_event_received') {
    const current = state.snapshotByRunId[action.event.runId]
    if (!current) {
      return state
    }
    const disconnectedRunIdSet = new Set(state.disconnectedRunIdSet)
    disconnectedRunIdSet.delete(action.event.runId)
    return {
      snapshotByRunId: {
        ...state.snapshotByRunId,
        [action.event.runId]: applyAgentRunEvent(current, action.event),
      },
      disconnectedRunIdSet,
    }
  }
  if (action.type === 'official_message_received') {
    const current = state.snapshotByRunId[action.runId]
    if (!current) {
      return state
    }
    return {
      ...state,
      snapshotByRunId: {
        ...state.snapshotByRunId,
        [action.runId]: {
          ...current,
          status: 'SUCCEEDED',
          stage: 'FINALIZING',
          answerMessageId: action.messageId,
        },
      },
    }
  }
  const disconnectedRunIdSet = new Set(state.disconnectedRunIdSet)
  disconnectedRunIdSet.add(action.runId)
  return { ...state, disconnectedRunIdSet }
}
