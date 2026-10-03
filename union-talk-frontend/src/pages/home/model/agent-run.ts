export type AgentRunStatus =
  'QUEUED' | 'RUNNING' | 'SUCCEEDED' | 'FAILED' | 'CANCELLED'

export type AgentRunStage =
  'PREPARING' | 'RETRIEVING' | 'TOOL_RUNNING' | 'GENERATING' | 'FINALIZING'

export type AgentContentBlockType = 'TEXT' | 'THINKING' | 'TOOL_CALL'
export type AgentContentBlockStatus =
  'STREAMING' | 'COMPLETED' | 'FAILED' | 'INTERRUPTED'
export type AgentMessageStatus = AgentContentBlockStatus
export type AgentToolExecutionStatus =
  'RUNNING' | 'SUCCEEDED' | 'FAILED' | 'CANCELLED' | 'INTERRUPTED'

export type AgentEventType =
  | 'agent_start'
  | 'turn_start'
  | 'message_start'
  | 'message_update'
  | 'message_end'
  | 'tool_execution_start'
  | 'tool_execution_update'
  | 'tool_execution_end'
  | 'turn_end'
  | 'agent_end'

export type AgentProviderEventType =
  | 'start'
  | 'text_start'
  | 'text_delta'
  | 'text_end'
  | 'thinking_start'
  | 'thinking_delta'
  | 'thinking_end'
  | 'toolcall_start'
  | 'toolcall_delta'
  | 'toolcall_end'
  | 'done'
  | 'error'

export interface AgentCitation {
  citationKey: string
  sourceType: string
  messageId?: string | null
  assetFileId?: string | null
  resourceVersion?: number | null
  chunkId?: string | null
  pageFrom?: number | null
  pageTo?: number | null
  headingPath?: string | null
}

export interface AgentRun {
  runId: string
  conversationId: string
  triggerMessageId: string
  requesterUserId: string
  bindingId: string
  bindingVersion: number
  agentId: string
  agentVersion: number
  credentialId: string
  credentialVersion: number
  credentialOwnerUserId: string
  modelId: string
  status: AgentRunStatus
  stage: AgentRunStage
  answerMessageId: string | null
  lastEventSequence: number
  errorCode: string | null
  errorMessage: string | null
  queuedAt: string | null
  startedAt: string | null
  completedAt: string | null
}

export interface AgentContentBlock {
  contentIndex: number
  blockType: AgentContentBlockType
  status: AgentContentBlockStatus
  content: string
  toolCallId: string | null
  toolName: string | null
}

export interface AgentMessageSnapshot {
  turnNo: number
  messageKey: string
  role: string
  status: AgentMessageStatus
  modelId: string
  contentBlockList: AgentContentBlock[]
  stopReason: string | null
  inputTokens: number
  outputTokens: number
}

export interface AgentToolExecution {
  toolCallId: string
  turnNo: number
  toolName: string
  displayName: string
  status: AgentToolExecutionStatus
  argumentsSummary: Record<string, unknown>
  resultSummary: Record<string, unknown>
  errorCode: string | null
  errorMessage: string | null
  durationMs: number | null
}

export interface AgentRunSnapshot {
  schemaVersion: number
  runId: string
  conversationId: string
  triggerMessageId: string
  requesterUserId: string
  cardKey: string
  status: AgentRunStatus
  stage: AgentRunStage
  currentTurnNo: number | null
  messageList: AgentMessageSnapshot[]
  toolExecutionList: AgentToolExecution[]
  citationList: AgentCitation[]
  lastSequence: number
  answerMessageId: string | null
  errorCode: string | null
  errorMessage: string | null
  updatedAt?: string
}

export interface AgentAssistantMessageEvent {
  type: AgentProviderEventType
  contentIndex?: number
  delta?: string
  toolCallId?: string
  toolName?: string
}

export interface AgentRunEvent {
  schemaVersion: number
  eventId: string
  runId: string
  conversationId: string
  triggerMessageId: string
  requesterUserId: string
  sequence: number
  type: AgentEventType
  turnNo: number | null
  messageKey: string | null
  payload: Record<string, unknown> & {
    assistantMessageEvent?: AgentAssistantMessageEvent
  }
  status: AgentRunStatus
  stage: AgentRunStage
  occurredAt: string
}

export interface AgentTraceMessage {
  messageKey: string
  role: string
  status: AgentMessageStatus
  modelId: string
  stopReason: string | null
  inputTokens: number
  outputTokens: number
  contentBlockList: AgentContentBlock[]
}

export interface AgentTraceTurn {
  turnNo: number
  status: string
  stopReason: string | null
  durationMs: number | null
  messageList: AgentTraceMessage[]
  toolCallList: AgentToolExecution[]
}

export interface AgentTraceStep {
  stepId: string
  sequence: number
  stepType: string
  stepCode: string
  displayName: string
  status: string
  visibility: string
  inputSummary: Record<string, unknown>
  outputSummary: Record<string, unknown>
  errorCode: string | null
  errorMessage: string | null
  startedAt: string | null
  finishedAt: string | null
  durationMs: number | null
}

/**
 * 获取最后一条 Assistant Message 的指定内容块.
 * @param snapshot 当前 Agent Run 快照
 * @param blockType 内容块类型
 * @return 最后命中的内容块；不存在时返回空
 */
export const getLatestAgentContentBlock = (
  snapshot: AgentRunSnapshot,
  blockType: AgentContentBlockType,
): AgentContentBlock | undefined =>
  snapshot.messageList
    .toSorted((left, right) => left.turnNo - right.turnNo)
    .flatMap((message) => message.contentBlockList)
    .filter((block) => block.blockType === blockType)
    .at(-1)

/**
 * 获取当前 Run 的最终或流式正文.
 * @param snapshot 当前 Agent Run 快照
 * @return 最新 TEXT 内容块正文
 */
export const getAgentRunText = (snapshot: AgentRunSnapshot): string =>
  getLatestAgentContentBlock(snapshot, 'TEXT')?.content ?? ''

/**
 * 获取当前 Run 最新的真实 Thinking 正文.
 * @param snapshot 当前 Agent Run 快照
 * @return 最新 THINKING 内容块正文
 */
export const getAgentRunThinking = (snapshot: AgentRunSnapshot): string =>
  getLatestAgentContentBlock(snapshot, 'THINKING')?.content ?? ''

/**
 * 获取当前正在执行的真实工具活动.
 * @param snapshot 当前 Agent Run 快照
 * @return 最近一个运行中的工具执行
 */
export const getActiveAgentTool = (
  snapshot: AgentRunSnapshot,
): AgentToolExecution | undefined =>
  snapshot.toolExecutionList
    .filter((execution) => execution.status === 'RUNNING')
    .at(-1)

/**
 * 计算 Agent Run 的界面耗时.
 * @param run 包含生命周期时间的 Run
 * @return 秒级耗时或等待状态
 */
export const getAgentRunDurationLabel = (
  run: Pick<AgentRun, 'queuedAt' | 'startedAt' | 'completedAt'>,
): string => {
  const startValue = run.startedAt ?? run.queuedAt
  if (!startValue) {
    return '等待中'
  }
  if (!run.completedAt) {
    return '进行中'
  }

  const durationMilliseconds =
    Date.parse(run.completedAt) - Date.parse(startValue)
  if (!Number.isFinite(durationMilliseconds) || durationMilliseconds < 0) {
    return '—'
  }

  const seconds = durationMilliseconds / 1000
  return `${Number.isInteger(seconds) ? seconds.toFixed(0) : seconds.toFixed(1)}s`
}

/**
 * 判断 Run 是否已经到达终态.
 * @param status Agent Run 状态
 * @return 是否为成功、失败或取消
 */
export const isAgentRunTerminal = (status: AgentRunStatus): boolean =>
  status === 'SUCCEEDED' || status === 'FAILED' || status === 'CANCELLED'
