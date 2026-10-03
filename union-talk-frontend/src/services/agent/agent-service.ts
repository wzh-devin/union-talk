import type {
  AgentContext,
  AgentCredentialDraft,
  AgentDefinition,
  AgentDefinitionDraft,
  AgentRunPage,
  AgentRunPageQuery,
  AgentRunSnapshot,
  AgentTrace,
} from '@/services/agent/agent-contract'
import { AGENT_RUN_PAGE_SIZE } from '@/services/agent/agent-constant'
import { generatedRequest } from '@/services/http/generated-request'

const defaultAgentDefinitionDraft: AgentDefinitionDraft = {
  agentVersion: 0,
  bindingVersion: 0,
  modelId: 'deepseek-chat',
  timeoutMs: 60_000,
  maxRetries: 2,
  displayName: 'AI 助手',
  systemPrompt: '',
  enabled: true,
  historyEnabled: true,
  resourceEnabled: true,
  maxContextTokens: 32_000,
  maxOutputTokens: 4096,
  recentMessageTokens: 6000,
  messageTopK: 20,
  resourceTopK: 30,
  temperature: 0.2,
  thinkingEnabled: false,
  thinkingEffort: 'medium',
}

/**
 * 创建尚未落库的 Agent 定义默认草稿.
 * @return 默认 Agent 定义草稿
 */
export const createDefaultAgentDefinitionDraft = (): AgentDefinitionDraft => ({
  ...defaultAgentDefinitionDraft,
})

/**
 * 创建尚未提交的 DeepSeek 凭证草稿.
 * @param bindingVersion 当前会话绑定版本
 * @param apiBase 已保存或默认 Provider 地址
 * @return 不包含历史 API Key 明文的凭证草稿
 */
export const createAgentCredentialDraft = (
  bindingVersion = 0,
  apiBase = 'https://api.deepseek.com',
): AgentCredentialDraft => ({
  bindingVersion,
  apiBase,
  apiKey: '',
})

/**
 * 将 Agent 定义响应转换为可编辑草稿.
 * @param agent Agent 定义响应
 * @return Agent 定义草稿
 */
export const toAgentDefinitionDraft = (
  agent: AgentDefinition,
): AgentDefinitionDraft => ({ ...agent })

/**
 * 查询当前成员可见的会话 Agent 上下文.
 * @param conversationId 会话 ID
 * @return Agent 状态、定义、凭证摘要和服务端能力
 */
export const getConversationAgentContext = async (
  conversationId: string,
): Promise<AgentContext> =>
  generatedRequest<AgentContext>({
    url: `/agent/conversations/${conversationId}/context`,
    method: 'GET',
  })

/**
 * 保存 Agent 定义新版本且不改变当前凭证所有权.
 * @param conversationId 会话 ID
 * @param draft Agent 定义草稿
 * @return 保存后的 Agent 上下文
 */
export const saveConversationAgentDefinition = async (
  conversationId: string,
  draft: AgentDefinitionDraft,
): Promise<AgentContext> =>
  generatedRequest<AgentContext>({
    url: `/agent/conversations/${conversationId}/definition`,
    method: 'PUT',
    data: draft,
  })

/**
 * 测试完整 API Key 并替换会话当前凭证赞助者.
 * @param conversationId 会话 ID
 * @param draft 新 Provider 凭证草稿
 * @return 替换后的 Agent 上下文
 */
export const replaceConversationAgentCredential = async (
  conversationId: string,
  draft: AgentCredentialDraft,
): Promise<AgentContext> =>
  generatedRequest<AgentContext>({
    url: `/agent/conversations/${conversationId}/credential`,
    method: 'PUT',
    data: draft,
  })

/**
 * 测试当前用户拥有的已保存凭证.
 * @param conversationId 会话 ID
 * @return 无返回值
 */
export const testConversationAgentCredential = async (
  conversationId: string,
): Promise<void> => {
  await generatedRequest<null>({
    url: `/agent/conversations/${conversationId}/credential/test`,
    method: 'POST',
  })
}

/**
 * 撤销当前用户拥有的会话 Agent 凭证.
 * @param conversationId 会话 ID
 * @return 撤销后的 Agent 上下文
 */
export const revokeConversationAgentCredential = async (
  conversationId: string,
): Promise<AgentContext> =>
  generatedRequest<AgentContext>({
    url: `/agent/conversations/${conversationId}/credential`,
    method: 'DELETE',
  })

/**
 * 按双游标查询会话 Agent 执行记录.
 * @param conversationId 会话 ID
 * @param query 游标分页参数
 * @return 与 Java CursorPageResult 同构的 Agent Run 游标页
 */
export const pageConversationAgentRuns = async (
  conversationId: string,
  query: AgentRunPageQuery = { pageSize: AGENT_RUN_PAGE_SIZE },
): Promise<AgentRunPage> => {
  const page = await generatedRequest<Partial<AgentRunPage>>({
    url: `/agent/conversations/${conversationId}/runs`,
    method: 'GET',
    params: query,
  })
  return {
    list: page.list ?? [],
    hasNext: Boolean(page.hasNext),
    nextCursorValue: page.nextCursorValue ?? null,
    nextCursorId: page.nextCursorId ?? null,
  }
}

/**
 * 查询单个 Agent Run 卡片快照.
 * @param runId Agent Run ID
 * @return 卡片快照
 */
export const getAgentRunSnapshot = async (
  runId: string,
): Promise<AgentRunSnapshot> =>
  generatedRequest<AgentRunSnapshot>({
    url: `/agent/runs/${runId}/snapshot`,
    method: 'GET',
  })

/**
 * 查询单个 Agent Run 的结构化执行轨迹.
 * @param runId Agent Run ID
 * @return 脱敏执行轨迹
 */
export const getAgentRunTrace = async (runId: string): Promise<AgentTrace> =>
  generatedRequest<AgentTrace>({
    url: `/agent/runs/${runId}/trace`,
    method: 'GET',
  })
