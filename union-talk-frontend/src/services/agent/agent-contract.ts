import type {
  AgentRun,
  AgentRunEvent,
  AgentRunSnapshot,
  AgentTraceStep,
  AgentTraceTurn,
} from '@/pages/home/model/agent-run'

export type AgentContextState =
  'NOT_CONFIGURED' | 'READY' | 'DISABLED' | 'CREDENTIAL_UNAVAILABLE'

export interface AgentDefinition {
  agentId: string
  agentVersion: number
  bindingVersion: number
  displayName: string
  modelId: string
  timeoutMs: number
  maxRetries: number
  systemPrompt: string
  enabled: boolean
  historyEnabled: boolean
  resourceEnabled: boolean
  maxContextTokens: number
  maxOutputTokens: number
  recentMessageTokens: number
  messageTopK: number
  resourceTopK: number
  temperature: number
  thinkingEnabled: boolean
  thinkingEffort: 'low' | 'medium' | 'high'
}

export interface AgentCredential {
  credentialId: string
  credentialVersion: number
  ownerUserId: string
  provider: string
  apiBase: string
  keyFingerprint: string
  status: string
  connectionTestStatus: string
  connectionTestError: string | null
}

export interface AgentContext {
  conversationId: string
  conversationType: string
  agentState: AgentContextState
  canView: boolean
  canInvoke: boolean
  canManageDefinition: boolean
  canManageCurrentCredential: boolean
  canReplaceCredential: boolean
  agent: AgentDefinition | null
  credential: AgentCredential | null
}

export type AgentDefinitionDraft = Omit<AgentDefinition, 'agentId'>

export interface AgentCredentialDraft {
  bindingVersion: number
  apiBase: string
  apiKey: string
}

export interface AgentTrace {
  runId: string
  turnList: AgentTraceTurn[]
  stepList: AgentTraceStep[]
}

export interface AgentRunPage {
  list: AgentRun[]
  hasNext: boolean
  nextCursorValue: string | null
  nextCursorId: string | null
}

export interface AgentRunPageQuery {
  pageSize: number
  cursorValue?: string | null
  cursorId?: string | null
}

export interface AgentSseEvent {
  eventId: string
  eventName: 'snapshot' | 'agent-event' | 'realtime-unavailable'
  payload: AgentRunSnapshot | AgentRunEvent | Record<string, unknown>
}

export type { AgentRun, AgentRunSnapshot }
