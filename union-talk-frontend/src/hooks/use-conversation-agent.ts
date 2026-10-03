import { useCallback, useEffect, useReducer, useRef, useState } from 'react'
import { toast } from 'sonner'

import type {
  AgentContext,
  AgentCredentialDraft,
  AgentDefinitionDraft,
  AgentRun,
  AgentRunSnapshot,
} from '@/services/agent/agent-contract'
import {
  createAgentCredentialDraft,
  createDefaultAgentDefinitionDraft,
  getAgentRunSnapshot,
  getAgentRunTrace,
  getConversationAgentContext,
  pageConversationAgentRuns,
  replaceConversationAgentCredential,
  revokeConversationAgentCredential,
  saveConversationAgentDefinition,
  testConversationAgentCredential,
  toAgentDefinitionDraft,
} from '@/services/agent/agent-service'
import { AGENT_RUN_PAGE_SIZE } from '@/services/agent/agent-constant'
import { connectAgentRunStream } from '@/services/agent/agent-stream'
import {
  isAgentRunTerminal,
  type AgentRunEvent,
} from '@/pages/home/model/agent-run'
import {
  agentRealtimeReducer,
  initialAgentRealtimeState,
} from '@/pages/home/model/agent-realtime-reducer'
import type { AgentTrace } from '@/services/agent/agent-contract'
import type { Conversation } from '@/pages/home/model/types'

export interface AgentFocusRequest {
  targetKey: string
  requestId: number
}

interface UseConversationAgentInput {
  conversation?: Conversation
  onRefreshConversationMessages?: (
    conversationId: string,
    answerMessageId: string,
  ) => Promise<boolean>
}

interface AgentRunCursor {
  hasNext: boolean
  nextCursorValue: string | null
  nextCursorId: string | null
}

const emptyRunCursor: AgentRunCursor = {
  hasNext: false,
  nextCursorValue: null,
  nextCursorId: null,
}

const TRACE_REFRESH_INTERVAL_MS = 800

/**
 * 按主列表顺序合并 Agent Run，并使用主列表中的最新状态.
 * @param primaryRunList 优先保留顺序和内容的 Run 列表
 * @param secondaryRunList 用于补充缺失记录的 Run 列表
 * @return 去重后的 Agent Run 列表
 */
const mergeAgentRunList = (
  primaryRunList: AgentRun[],
  secondaryRunList: AgentRun[],
): AgentRun[] => {
  const runMap = new Map<string, AgentRun>()
  primaryRunList.forEach((run) => runMap.set(run.runId, run))
  secondaryRunList.forEach((run) => {
    if (!runMap.has(run.runId)) {
      runMap.set(run.runId, run)
    }
  })
  return Array.from(runMap.values())
}

/**
 * 管理当前会话 Agent 配置、运行记录、SSE 卡片与执行轨迹.
 * @param input 当前会话和管理权限
 * @return Agent 页面与消息卡片共享的交互状态
 */
export const useConversationAgent = ({
  conversation,
  onRefreshConversationMessages,
}: UseConversationAgentInput) => {
  const [context, setContext] = useState<AgentContext | null>(null)
  const [definitionDraft, setDefinitionDraft] = useState<AgentDefinitionDraft>(
    createDefaultAgentDefinitionDraft,
  )
  const [credentialDraft, setCredentialDraft] = useState<AgentCredentialDraft>(
    createAgentCredentialDraft,
  )
  const [isContextLoading, setIsContextLoading] = useState(false)
  const [isDefinitionSaving, setIsDefinitionSaving] = useState(false)
  const [isCredentialSaving, setIsCredentialSaving] = useState(false)
  const [isConnectionTesting, setIsConnectionTesting] = useState(false)
  const [connectionLatency, setConnectionLatency] = useState<number | null>(
    null,
  )
  const [connectionError, setConnectionError] = useState('')
  const [runList, setRunList] = useState<AgentRun[]>([])
  const [realtimeState, dispatchRealtime] = useReducer(
    agentRealtimeReducer,
    initialAgentRealtimeState,
  )
  const snapshotByRunId = realtimeState.snapshotByRunId
  const [isRunListLoading, setIsRunListLoading] = useState(false)
  const [isLoadingNextRunList, setIsLoadingNextRunList] = useState(false)
  const [runCursor, setRunCursor] = useState<AgentRunCursor>(emptyRunCursor)
  const [focusRequest, setFocusRequest] = useState<AgentFocusRequest | null>(
    null,
  )
  const [traceRun, setTraceRun] = useState<AgentRun | null>(null)
  const [trace, setTrace] = useState<AgentTrace | null>(null)
  const [isTraceLoading, setIsTraceLoading] = useState(false)
  const streamControllerMapRef = useRef(new Map<string, AbortController>())
  const streamCursorMapRef = useRef(new Map<string, string>())
  const runLoadErrorShownRef = useRef(false)
  const runListInitializedRef = useRef(false)
  const refreshRequestPendingRef = useRef(false)
  const nextRunRequestPendingRef = useRef(false)
  const activeRunIdSetRef = useRef(new Set<string>())
  const terminalMessageSyncRunIdSetRef = useRef(new Set<string>())
  const traceRunIdRef = useRef('')
  const traceRefreshPendingRef = useRef(false)
  const conversationId = conversation?.id ?? ''
  const conversationIdRef = useRef(conversationId)
  const messageIdSetRef = useRef(
    new Set(conversation?.messages.map((message) => message.id) ?? []),
  )

  useEffect(() => {
    conversationIdRef.current = conversationId
  }, [conversationId])

  useEffect(() => {
    messageIdSetRef.current = new Set(
      conversation?.messages.map((message) => message.id) ?? [],
    )
  }, [conversation?.messages])

  useEffect(() => {
    conversation?.messages.forEach((message) => {
      if (!message.agentRunId) {
        return
      }
      dispatchRealtime({
        type: 'official_message_received',
        runId: message.agentRunId,
        messageId: message.id,
      })
    })
  }, [conversation?.messages])

  /**
   * Run 到达终态后补拉正式消息，避免 WebSocket 丢失时残留临时卡片.
   * @param runId Agent Run ID
   * @param triggerMessageId 触发 Agent 的会话消息 ID
   * @param answerMessageId 已落库的正式回复消息 ID
   * @return void
   */
  const synchronizeTerminalAnswerMessage = useCallback(
    (
      runId: string,
      triggerMessageId: string,
      answerMessageId: string | null,
    ): void => {
      const isCurrentConversationRun =
        activeRunIdSetRef.current.has(runId) ||
        messageIdSetRef.current.has(triggerMessageId)
      if (!answerMessageId || !isCurrentConversationRun) {
        return
      }
      if (messageIdSetRef.current.has(answerMessageId)) {
        activeRunIdSetRef.current.delete(runId)
        terminalMessageSyncRunIdSetRef.current.add(runId)
        return
      }
      if (
        !conversationId ||
        !onRefreshConversationMessages ||
        terminalMessageSyncRunIdSetRef.current.has(runId)
      ) {
        return
      }

      terminalMessageSyncRunIdSetRef.current.add(runId)
      void onRefreshConversationMessages(conversationId, answerMessageId)
        .then((isAnswerMessageSynchronized) => {
          if (isAnswerMessageSynchronized) {
            activeRunIdSetRef.current.delete(runId)
            return
          }
          terminalMessageSyncRunIdSetRef.current.delete(runId)
        })
        .catch(() => {
          terminalMessageSyncRunIdSetRef.current.delete(runId)
        })
    },
    [conversationId, onRefreshConversationMessages],
  )

  /**
   * 从服务端刷新当前会话执行记录及活动卡片快照.
   * @return 刷新流程
   */
  const refreshRunList = useCallback(async (): Promise<void> => {
    if (!conversationId || refreshRequestPendingRef.current) {
      return
    }
    const requestedConversationId = conversationId
    refreshRequestPendingRef.current = true
    setIsRunListLoading(true)
    try {
      const page = await pageConversationAgentRuns(conversationId, {
        pageSize: AGENT_RUN_PAGE_SIZE,
      })
      if (conversationIdRef.current !== requestedConversationId) {
        return
      }
      page.list.forEach((run) => {
        if (isAgentRunTerminal(run.status)) {
          synchronizeTerminalAnswerMessage(
            run.runId,
            run.triggerMessageId,
            run.answerMessageId,
          )
        } else {
          activeRunIdSetRef.current.add(run.runId)
        }
      })
      setRunList((currentList) => mergeAgentRunList(page.list, currentList))
      if (!runListInitializedRef.current) {
        runListInitializedRef.current = true
        setRunCursor({
          hasNext: page.hasNext,
          nextCursorValue: page.nextCursorValue,
          nextCursorId: page.nextCursorId,
        })
      }
      const snapshotRunList = page.list.filter((run) => {
        if (!isAgentRunTerminal(run.status)) {
          return true
        }
        const isCurrentConversationRun =
          activeRunIdSetRef.current.has(run.runId) ||
          messageIdSetRef.current.has(run.triggerMessageId)
        const isAnswerMessageMissing =
          !run.answerMessageId ||
          !messageIdSetRef.current.has(run.answerMessageId)
        return isCurrentConversationRun && isAnswerMessageMissing
      })
      const snapshotResultList = await Promise.allSettled(
        snapshotRunList.map((run) => getAgentRunSnapshot(run.runId)),
      )
      snapshotResultList.forEach((result, index) => {
        if (
          result.status === 'fulfilled' &&
          isAgentRunTerminal(result.value.status)
        ) {
          synchronizeTerminalAnswerMessage(
            snapshotRunList[index].runId,
            snapshotRunList[index].triggerMessageId,
            result.value.answerMessageId,
          )
        }
      })
      snapshotResultList.forEach((result, index) => {
        if (result.status !== 'fulfilled') {
          return
        }
        const run = snapshotRunList[index]
        dispatchRealtime({
          type: 'snapshot_received',
          snapshot: isAgentRunTerminal(run.status)
            ? {
                ...result.value,
                status: run.status,
                stage: run.stage,
                answerMessageId:
                  run.answerMessageId ?? result.value.answerMessageId,
                errorCode: run.errorCode ?? result.value.errorCode,
                errorMessage: run.errorMessage ?? result.value.errorMessage,
              }
            : result.value,
        })
      })
      runLoadErrorShownRef.current = false
    } catch {
      if (
        conversationIdRef.current === requestedConversationId &&
        !runLoadErrorShownRef.current
      ) {
        runLoadErrorShownRef.current = true
        toast.error('Agent 执行记录加载失败')
      }
    } finally {
      refreshRequestPendingRef.current = false
      if (conversationIdRef.current === requestedConversationId) {
        setIsRunListLoading(false)
      }
    }
  }, [conversationId, synchronizeTerminalAnswerMessage])

  /**
   * 使用当前双游标追加更早的 Agent Run.
   * @return 下一页加载流程
   */
  const loadNextRunList = useCallback(async (): Promise<void> => {
    if (
      !conversationId ||
      !runCursor.hasNext ||
      !runCursor.nextCursorValue ||
      !runCursor.nextCursorId ||
      nextRunRequestPendingRef.current
    ) {
      return
    }
    const requestedConversationId = conversationId
    nextRunRequestPendingRef.current = true
    setIsLoadingNextRunList(true)
    try {
      const page = await pageConversationAgentRuns(conversationId, {
        pageSize: AGENT_RUN_PAGE_SIZE,
        cursorValue: runCursor.nextCursorValue,
        cursorId: runCursor.nextCursorId,
      })
      if (conversationIdRef.current !== requestedConversationId) {
        return
      }
      setRunList((currentList) => mergeAgentRunList(currentList, page.list))
      setRunCursor({
        hasNext: page.hasNext,
        nextCursorValue: page.nextCursorValue,
        nextCursorId: page.nextCursorId,
      })
    } catch {
      if (conversationIdRef.current === requestedConversationId) {
        toast.error('更多 Agent 执行记录加载失败')
      }
    } finally {
      nextRunRequestPendingRef.current = false
      if (conversationIdRef.current === requestedConversationId) {
        setIsLoadingNextRunList(false)
      }
    }
  }, [conversationId, runCursor])

  useEffect(() => {
    const streamControllerMap = streamControllerMapRef.current
    streamControllerMap.forEach((controller) => controller.abort())
    streamControllerMap.clear()
    streamCursorMapRef.current.clear()
    runLoadErrorShownRef.current = false
    runListInitializedRef.current = false
    refreshRequestPendingRef.current = false
    nextRunRequestPendingRef.current = false
    activeRunIdSetRef.current.clear()
    terminalMessageSyncRunIdSetRef.current.clear()
    traceRunIdRef.current = ''
    traceRefreshPendingRef.current = false
    const resetTimer = setTimeout(() => {
      setRunList([])
      dispatchRealtime({ type: 'reset' })
      setRunCursor(emptyRunCursor)
      setIsLoadingNextRunList(false)
      setTraceRun(null)
      setTrace(null)
      setFocusRequest(null)
      setIsRunListLoading(Boolean(conversationId))
    }, 0)
    if (!conversationId) {
      return () => clearTimeout(resetTimer)
    }

    const initialLoadTimer = setTimeout(() => void refreshRunList(), 0)
    const refreshTimer = setInterval(() => void refreshRunList(), 5000)
    return () => {
      clearTimeout(resetTimer)
      clearTimeout(initialLoadTimer)
      clearInterval(refreshTimer)
      streamControllerMap.forEach((controller) => controller.abort())
      streamControllerMap.clear()
    }
  }, [conversationId, refreshRunList])

  useEffect(() => {
    if (!conversationId) {
      const resetTimer = setTimeout(() => {
        setContext(null)
        setDefinitionDraft(createDefaultAgentDefinitionDraft())
        setCredentialDraft(createAgentCredentialDraft())
        setIsContextLoading(false)
      }, 0)
      return () => clearTimeout(resetTimer)
    }

    let isCurrent = true
    const loadingTimer = setTimeout(() => setIsContextLoading(true), 0)
    void getConversationAgentContext(conversationId)
      .then((nextContext) => {
        if (!isCurrent) {
          return
        }
        setContext(nextContext)
        setDefinitionDraft(
          nextContext.agent
            ? toAgentDefinitionDraft(nextContext.agent)
            : createDefaultAgentDefinitionDraft(),
        )
        setCredentialDraft(
          createAgentCredentialDraft(
            nextContext.agent?.bindingVersion ?? 0,
            nextContext.credential?.apiBase,
          ),
        )
      })
      .catch(() => {
        if (isCurrent) {
          toast.error('Agent 上下文加载失败')
        }
      })
      .finally(() => {
        if (isCurrent) {
          setIsContextLoading(false)
        }
      })

    return () => {
      isCurrent = false
      clearTimeout(loadingTimer)
    }
  }, [conversationId])

  useEffect(() => {
    runList.forEach((run) => {
      if (
        isAgentRunTerminal(run.status) ||
        streamControllerMapRef.current.has(run.runId)
      ) {
        return
      }

      const controller = new AbortController()
      streamControllerMapRef.current.set(run.runId, controller)
      void connectAgentRunStream({
        runId: run.runId,
        signal: controller.signal,
        lastEventId: streamCursorMapRef.current.get(run.runId),
        onEvent: (event) => {
          if (event.eventId) {
            streamCursorMapRef.current.set(run.runId, event.eventId)
          }
          if (event.eventName === 'snapshot') {
            const snapshot = event.payload as AgentRunSnapshot
            dispatchRealtime({ type: 'snapshot_received', snapshot })
            setRunList((currentList) =>
              currentList.map((currentRun) =>
                currentRun.runId === run.runId
                  ? {
                      ...currentRun,
                      status: snapshot.status,
                      stage: snapshot.stage,
                      answerMessageId:
                        snapshot.answerMessageId ?? currentRun.answerMessageId,
                      errorCode: snapshot.errorCode ?? currentRun.errorCode,
                      errorMessage:
                        snapshot.errorMessage ?? currentRun.errorMessage,
                    }
                  : currentRun,
              ),
            )
            if (isAgentRunTerminal(snapshot.status)) {
              synchronizeTerminalAnswerMessage(
                run.runId,
                snapshot.triggerMessageId,
                snapshot.answerMessageId,
              )
              setTimeout(() => void refreshRunList(), 500)
            }
            return
          }
          if (event.eventName !== 'agent-event') {
            return
          }

          const update = event.payload as AgentRunEvent
          dispatchRealtime({ type: 'agent_event_received', event: update })
          setRunList((currentList) =>
            currentList.map((currentRun) =>
              currentRun.runId === run.runId
                ? {
                    ...currentRun,
                    status: update.status,
                    stage: update.stage,
                    answerMessageId:
                      (typeof update.payload.answerMessageId === 'string'
                        ? update.payload.answerMessageId
                        : null) ?? currentRun.answerMessageId,
                    errorCode:
                      (typeof update.payload.errorCode === 'string'
                        ? update.payload.errorCode
                        : null) ?? currentRun.errorCode,
                    errorMessage:
                      (typeof update.payload.errorMessage === 'string'
                        ? update.payload.errorMessage
                        : null) ?? currentRun.errorMessage,
                  }
                : currentRun,
            ),
          )
          if (update.status && isAgentRunTerminal(update.status)) {
            synchronizeTerminalAnswerMessage(
              run.runId,
              run.triggerMessageId,
              typeof update.payload.answerMessageId === 'string'
                ? update.payload.answerMessageId
                : null,
            )
            setTimeout(() => void refreshRunList(), 500)
          }
        },
      })
        .catch(() => undefined)
        .finally(() => {
          streamControllerMapRef.current.delete(run.runId)
          dispatchRealtime({ type: 'stream_disconnected', runId: run.runId })
        })
    })
  }, [refreshRunList, runList, synchronizeTerminalAnswerMessage])

  /**
   * 保存 Agent 定义新版本且不改变当前凭证所有权.
   * @param draft 待保存 Agent 定义草稿
   * @param successMessage 保存成功提示
   * @return 是否保存成功
   */
  const persistDefinitionDraft = useCallback(
    async (
      draft: AgentDefinitionDraft,
      successMessage?: string,
    ): Promise<AgentContext | null> => {
      if (!conversationId || !context?.canManageDefinition) {
        return null
      }
      setIsDefinitionSaving(true)
      try {
        const nextContext = await saveConversationAgentDefinition(
          conversationId,
          draft,
        )
        setContext(nextContext)
        setDefinitionDraft(
          nextContext.agent
            ? toAgentDefinitionDraft(nextContext.agent)
            : createDefaultAgentDefinitionDraft(),
        )
        setCredentialDraft((current) => ({
          ...current,
          bindingVersion: nextContext.agent?.bindingVersion ?? 0,
        }))
        if (successMessage) {
          toast.success(successMessage)
        }
        return nextContext
      } catch {
        toast.error('Agent 定义保存失败')
        return null
      } finally {
        setIsDefinitionSaving(false)
      }
    },
    [context?.canManageDefinition, conversationId],
  )

  /**
   * 切换并立即保存当前会话 Agent 启用状态.
   * @param enabled 是否启用 Agent
   * @return 是否保存成功
   */
  const setAgentEnabled = useCallback(
    async (enabled: boolean): Promise<boolean> => {
      if (isDefinitionSaving || enabled === definitionDraft.enabled) {
        return enabled === definitionDraft.enabled
      }

      const previousDraft = definitionDraft
      const nextDraft = { ...definitionDraft, enabled }
      setDefinitionDraft(nextDraft)
      const savedContext = await persistDefinitionDraft(
        nextDraft,
        enabled ? 'Agent 已启用' : 'Agent 已停用',
      )
      if (!savedContext && conversationIdRef.current === conversationId) {
        setDefinitionDraft(previousDraft)
      }
      return Boolean(savedContext)
    },
    [
      conversationId,
      definitionDraft,
      isDefinitionSaving,
      persistDefinitionDraft,
    ],
  )

  /**
   * 通过单一入口保存 Agent 定义，并在输入新 Key 时测试和替换凭证.
   * @return 配置保存是否成功
   */
  const saveConfiguration = useCallback(async (): Promise<boolean> => {
    const hasCredentialChange = Boolean(credentialDraft.apiKey.trim())
    let savedContext = context

    if (context?.canManageDefinition) {
      savedContext = await persistDefinitionDraft(
        definitionDraft,
        hasCredentialChange ? undefined : '配置已保存',
      )
      if (!savedContext) {
        return false
      }
    }

    if (!hasCredentialChange) {
      return Boolean(context?.canManageDefinition)
    }
    if (
      !conversationId ||
      !context?.canReplaceCredential ||
      !savedContext?.agent
    ) {
      toast.error('当前用户无法更新 Agent 凭证')
      return false
    }

    setIsConnectionTesting(true)
    setIsCredentialSaving(true)
    setConnectionError('')
    try {
      const startedAt = performance.now()
      const nextContext = await replaceConversationAgentCredential(
        conversationId,
        {
          ...credentialDraft,
          bindingVersion: savedContext.agent.bindingVersion,
        },
      )
      setContext(nextContext)
      setCredentialDraft(
        createAgentCredentialDraft(
          nextContext.agent?.bindingVersion ?? 0,
          nextContext.credential?.apiBase,
        ),
      )
      setDefinitionDraft((current) => ({
        ...current,
        bindingVersion: nextContext.agent?.bindingVersion ?? 0,
      }))
      setConnectionLatency(
        Math.max(1, Math.round(performance.now() - startedAt)),
      )
      toast.success(
        context.canManageDefinition ? '配置与凭证已保存' : '凭证已保存',
      )
      return true
    } catch {
      setConnectionLatency(null)
      setConnectionError('连接失败')
      toast.error('凭证测试失败，配置定义已保存，当前凭证未改变')
      return false
    } finally {
      setIsConnectionTesting(false)
      setIsCredentialSaving(false)
    }
  }, [
    context,
    conversationId,
    credentialDraft,
    definitionDraft,
    persistDefinitionDraft,
  ])

  /**
   * 测试当前用户拥有的已保存凭证.
   * @return 当前凭证连接测试流程
   */
  const testCurrentCredential = useCallback(async (): Promise<void> => {
    if (!conversationId || !context?.canManageCurrentCredential) {
      return
    }
    setIsConnectionTesting(true)
    setConnectionError('')
    try {
      const startedAt = performance.now()
      await testConversationAgentCredential(conversationId)
      setConnectionLatency(
        Math.max(1, Math.round(performance.now() - startedAt)),
      )
      toast.success('当前凭证连接正常')
    } catch {
      setConnectionLatency(null)
      setConnectionError('连接失败')
      toast.error('当前凭证连接测试失败')
    } finally {
      setIsConnectionTesting(false)
    }
  }, [context?.canManageCurrentCredential, conversationId])

  /**
   * 撤销当前用户拥有的 Provider 凭证.
   * @return 凭证撤销流程
   */
  const revokeCredential = useCallback(async (): Promise<void> => {
    if (!conversationId || !context?.canManageCurrentCredential) {
      return
    }
    setIsCredentialSaving(true)
    try {
      const nextContext =
        await revokeConversationAgentCredential(conversationId)
      setContext(nextContext)
      setCredentialDraft(
        createAgentCredentialDraft(nextContext.agent?.bindingVersion ?? 0),
      )
      setDefinitionDraft((current) => ({
        ...current,
        bindingVersion: nextContext.agent?.bindingVersion ?? 0,
      }))
      setConnectionLatency(null)
      setConnectionError('')
      toast.success('当前凭证已撤销')
    } catch {
      toast.error('当前凭证撤销失败')
    } finally {
      setIsCredentialSaving(false)
    }
  }, [context?.canManageCurrentCredential, conversationId])

  /**
   * 请求消息列表定位指定 Run 对应消息或临时卡片.
   * @param run 待定位 Agent Run
   * @return void
   */
  const focusRun = useCallback(
    (run: AgentRun): void => {
      const hasAnswerMessage = Boolean(
        run.answerMessageId &&
        conversation?.messages.some(
          (message) => message.id === run.answerMessageId,
        ),
      )
      setFocusRequest({
        targetKey: hasAnswerMessage
          ? String(run.answerMessageId)
          : `agent-run:${run.runId}`,
        requestId: Date.now(),
      })
    },
    [conversation?.messages],
  )

  /**
   * 获取指定 Run 最新的动态执行事件列表.
   * @param runId Agent Run ID
   * @param showError 是否向用户提示加载失败
   * @return 轨迹刷新流程
   */
  const refreshTrace = useCallback(
    async (runId: string, showError: boolean): Promise<void> => {
      if (traceRefreshPendingRef.current) {
        return
      }
      traceRefreshPendingRef.current = true
      try {
        const trace = await getAgentRunTrace(runId)
        if (traceRunIdRef.current === runId) {
          setTrace(trace)
        }
      } catch {
        if (showError && traceRunIdRef.current === runId) {
          toast.error('Agent 执行过程加载失败')
        }
      } finally {
        traceRefreshPendingRef.current = false
      }
    },
    [],
  )

  /**
   * 打开并加载指定 Run 的执行轨迹.
   * @param runId Agent Run ID
   * @return 轨迹加载流程
   */
  const openTrace = useCallback(
    async (runId: string): Promise<void> => {
      const nextRun = runList.find((run) => run.runId === runId)
      if (!nextRun) {
        return
      }
      traceRunIdRef.current = runId
      setTraceRun(nextRun)
      setTrace(null)
      setIsTraceLoading(true)
      try {
        await refreshTrace(runId, true)
      } finally {
        if (traceRunIdRef.current === runId) {
          setIsTraceLoading(false)
        }
      }
    },
    [refreshTrace, runList],
  )

  const presentedTraceRun = traceRun
    ? (runList.find((run) => run.runId === traceRun.runId) ?? traceRun)
    : null

  useEffect(() => {
    if (!presentedTraceRun || isAgentRunTerminal(presentedTraceRun.status)) {
      return
    }
    const runId = presentedTraceRun.runId
    const refreshTimer = window.setInterval(
      () => void refreshTrace(runId, false),
      TRACE_REFRESH_INTERVAL_MS,
    )
    return () => window.clearInterval(refreshTimer)
  }, [presentedTraceRun, refreshTrace])

  const closeTrace = useCallback((): void => {
    traceRunIdRef.current = ''
    setTraceRun(null)
    setTrace(null)
    setIsTraceLoading(false)
  }, [])

  return {
    context,
    definitionDraft,
    setDefinitionDraft,
    credentialDraft,
    setCredentialDraft,
    isContextLoading,
    isDefinitionSaving,
    isCredentialSaving,
    isConnectionTesting,
    connectionLatency,
    connectionError,
    runList,
    snapshotByRunId,
    isRunListLoading,
    isLoadingNextRunList,
    hasNextRunList: runCursor.hasNext,
    focusRequest,
    traceRun: presentedTraceRun,
    trace,
    isTraceLoading,
    setAgentEnabled,
    saveConfiguration,
    testCurrentCredential,
    revokeCredential,
    refreshRunList,
    loadNextRunList,
    focusRun,
    openTrace,
    closeTrace,
  }
}

export type ConversationAgentController = ReturnType<
  typeof useConversationAgent
>
