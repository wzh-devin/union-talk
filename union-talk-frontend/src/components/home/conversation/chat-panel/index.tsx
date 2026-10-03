import { ArrowDown, LoaderCircle, Paperclip, Send } from 'lucide-react'
import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import type { ChangeEvent, FormEvent, RefObject } from 'react'
import { toast } from 'sonner'

import { Button } from '@/components/shadcn-ui/button'
import { ScrollArea } from '@/components/shadcn-ui/scroll-area'
import { AgentMessageCard } from '@/components/home/conversation/agent-message-card'
import { AgentTraceDialog } from '@/components/home/conversation/agent-trace-dialog'
import { MessageBubble } from '@/components/home/conversation/message-bubble'
import { MessageEmojiMenu } from '@/components/home/conversation/message-emoji-menu'
import { StructuredMessageComposer } from '@/components/home/conversation/structured-message-composer'
import { PanelHeader } from '@/components/home/shared/panel-header'
import { useChatScroll } from '@/hooks/use-chat-scroll'
import type { ConversationAgentController } from '@/hooks/use-conversation-agent'
import type { AgentRunSnapshot } from '@/pages/home/model/agent-run'
import type { ComposerDocument } from '@/pages/home/model/composer-document'
import type { Conversation } from '@/pages/home/model/types'
import { messageMentionType } from '@/services/message/message-contract'
import { cn } from '@/utils/class-name'

interface ChatPanelProps {
  conversation: Conversation
  currentUserName: string
  isDetailsOpen: boolean
  composerDocument: ComposerDocument
  backButtonRef?: RefObject<HTMLButtonElement | null>
  onBackToList: () => void
  onToggleDetails: () => void
  onOpenDetailsSheet: () => void
  onComposerChange: (document: ComposerDocument) => void
  onSendMessage: () => void
  onSendEmoji: (emoji: string) => void
  onSelectAttachmentList: (fileList: File[]) => void
  uploadState?: {
    fileName: string
    fileIndex: number
    fileCount: number
    progress: number
  }
  hasNextMessages?: boolean
  isLoadingNextMessages?: boolean
  onLoadNextMessages?: () => void
  onLatestReceivedMessageViewed?: (
    conversationId: string,
    messageId: string,
  ) => void
  agentController?: Pick<
    ConversationAgentController,
    | 'snapshotByRunId'
    | 'context'
    | 'runList'
    | 'focusRequest'
    | 'traceRun'
    | 'trace'
    | 'isTraceLoading'
    | 'openTrace'
    | 'closeTrace'
  >
}

const emptyAgentController: NonNullable<ChatPanelProps['agentController']> = {
  context: null,
  snapshotByRunId: {},
  runList: [],
  focusRequest: null,
  traceRun: null,
  trace: null,
  isTraceLoading: false,
  openTrace: async () => undefined,
  closeTrace: () => undefined,
}

/**
 * 渲染会话消息列表与消息输入区.
 * @param props 会话面板属性
 * @return 会话面板
 */
export const ChatPanel = ({
  conversation,
  currentUserName,
  isDetailsOpen,
  composerDocument,
  backButtonRef,
  onBackToList,
  onToggleDetails,
  onOpenDetailsSheet,
  onComposerChange,
  onSendMessage,
  onSendEmoji,
  onSelectAttachmentList,
  uploadState,
  hasNextMessages = false,
  isLoadingNextMessages = false,
  onLoadNextMessages,
  onLatestReceivedMessageViewed,
  agentController = emptyAgentController,
}: ChatPanelProps) => {
  const attachmentInputRef = useRef<HTMLInputElement>(null)
  const messageElementMapRef = useRef(new Map<string, HTMLElement>())
  const [highlightedTargetKey, setHighlightedTargetKey] = useState('')
  const [
    progressiveRevealCompletedRunIdSet,
    setProgressiveRevealCompletedRunIdSet,
  ] = useState<Set<string>>(() => new Set())
  const {
    messageEndRef,
    newReceivedMessageIdSet,
    unseenMessageCount,
    handleScrollCapture,
    scrollToLatest,
  } = useChatScroll({
    conversationId: conversation.id,
    messageList: conversation.messages,
    hasNextMessages,
    isLoadingNextMessages,
    onLoadNextMessages,
    onLatestReceivedMessageViewed,
  })
  const unseenMessageLabel =
    unseenMessageCount > 99 ? '99+' : String(unseenMessageCount)
  const isFriendBlocked =
    conversation.kind === 'friend' &&
    conversation.friendRelationStatus === 'blocked'
  const isUploadingAttachment = Boolean(uploadState)
  const mentionOptionList = useMemo(
    () => [
      ...(agentController.context?.canInvoke && agentController.context.agent
        ? [
            {
              id: agentController.context.agent.agentId,
              label: 'AI',
              description:
                agentController.context.agent.displayName || '会话 AI 助手',
              mentionType: messageMentionType.AGENT,
            },
          ]
        : []),
      ...conversation.memberList.map((member) => ({
        id: member.id,
        label: member.name,
        description: member.role === 'owner' ? '群主' : '成员',
        mentionType: messageMentionType.USER,
      })),
    ],
    [agentController.context, conversation.memberList],
  )

  /**
   * 标记实时卡片已播放完全部正文，允许切换为正式消息.
   * @param runId 已完成逐字播放的 Agent Run ID
   * @return 无返回值
   */
  const completeProgressiveReveal = useCallback((runId: string): void => {
    setProgressiveRevealCompletedRunIdSet((currentSet) => {
      if (currentSet.has(runId)) {
        return currentSet
      }
      const nextSet = new Set(currentSet)
      nextSet.add(runId)
      return nextSet
    })
  }, [])

  const persistedAgentRunIdSet = useMemo(
    () =>
      new Set(
        conversation.messages
          .map((message) => message.agentRunId)
          .filter((runId): runId is string => Boolean(runId)),
      ),
    [conversation.messages],
  )
  const temporarySnapshotList = useMemo(() => {
    const snapshotMap = new Map<string, AgentRunSnapshot>()
    Object.values(agentController.snapshotByRunId).forEach((snapshot) => {
      if (
        !snapshot?.runId ||
        !snapshot.triggerMessageId ||
        (persistedAgentRunIdSet.has(snapshot.runId) &&
          progressiveRevealCompletedRunIdSet.has(snapshot.runId))
      ) {
        return
      }
      snapshotMap.set(snapshot.runId, snapshot)
    })
    return [...snapshotMap.values()]
  }, [
    agentController.snapshotByRunId,
    persistedAgentRunIdSet,
    progressiveRevealCompletedRunIdSet,
  ])
  const snapshotListByTriggerId = useMemo(() => {
    const snapshotMap = new Map<string, AgentRunSnapshot[]>()
    temporarySnapshotList.forEach((snapshot) => {
      const currentList = snapshotMap.get(snapshot.triggerMessageId) ?? []
      snapshotMap.set(snapshot.triggerMessageId, [...currentList, snapshot])
    })
    return snapshotMap
  }, [temporarySnapshotList])
  const knownTriggerMessageIdSet = new Set(
    conversation.messages.map((message) => message.id),
  )
  const detachedSnapshotList = temporarySnapshotList.filter(
    (snapshot) => !knownTriggerMessageIdSet.has(snapshot.triggerMessageId),
  )
  const traceRequesterName =
    conversation.memberList.find(
      (member) => member.id === agentController.traceRun?.requesterUserId,
    )?.name || '会话成员'
  const traceAnswerContent = agentController.traceRun
    ? (conversation.messages.find(
        (message) =>
          message.id === agentController.traceRun?.answerMessageId ||
          message.agentRunId === agentController.traceRun?.runId,
      )?.body ?? '')
    : ''

  useEffect(() => {
    const focusRequest = agentController.focusRequest
    if (!focusRequest) {
      return
    }
    const targetElement = messageElementMapRef.current.get(
      focusRequest.targetKey,
    )
    if (!targetElement) {
      toast.error('对应消息尚未加载')
      return
    }

    targetElement.scrollIntoView({ behavior: 'smooth', block: 'center' })
    setHighlightedTargetKey(focusRequest.targetKey)
    toast.success('已定位到对应消息')
    const highlightTimer = setTimeout(() => setHighlightedTargetKey(''), 1800)
    return () => clearTimeout(highlightTimer)
  }, [agentController.focusRequest])

  /**
   * 为消息或临时卡片维护可滚动定位的 DOM 引用.
   * @param key 消息 ID 或 cardKey
   * @param element 当前 DOM 元素
   * @return void
   */
  const registerMessageElement = (
    key: string,
    element: HTMLDivElement | null,
  ): void => {
    if (element) {
      messageElementMapRef.current.set(key, element)
    } else {
      messageElementMapRef.current.delete(key)
    }
  }

  /**
   * 查询 Run 的请求用户名称.
   * @param requesterUserId 请求用户 ID
   * @return 请求用户名称
   */
  const getRequesterName = (requesterUserId: string): string =>
    conversation.memberList.find((member) => member.id === requesterUserId)
      ?.name ||
    (requesterUserId ? `用户 ${requesterUserId.slice(-4)}` : '会话成员')

  /**
   * 将正式 Agent 消息转换为已完成的卡片快照.
   * @param message Agent 正式消息
   * @return 已完成 Agent 卡片快照
   */
  const getPersistedAgentSnapshot = (
    message: Conversation['messages'][number],
  ): AgentRunSnapshot => {
    const run = agentController.runList.find(
      (item) => item.runId === message.agentRunId,
    )
    const replyMention = message.mentionList?.find(
      (mention) =>
        mention.mentionType === messageMentionType.USER &&
        mention.startOffset === 0,
    )
    const answerContent = replyMention
      ? message.body.slice(
          replyMention.length +
            (message.body.charAt(replyMention.length) === '\n' ? 1 : 0),
        )
      : message.body
    return {
      schemaVersion: 2,
      runId: message.agentRunId || message.id,
      conversationId: message.conversationId || conversation.id,
      triggerMessageId: message.triggerMessageId || run?.triggerMessageId || '',
      requesterUserId:
        run?.requesterUserId ||
        message.mentionList?.find(
          (mention) => mention.mentionType === messageMentionType.USER,
        )?.targetId ||
        '',
      cardKey: `agent-run:${message.agentRunId || message.id}`,
      status: 'SUCCEEDED',
      stage: 'FINALIZING',
      currentTurnNo: 1,
      messageList: [
        {
          turnNo: 1,
          messageKey: `assistant:${message.agentRunId || message.id}`,
          role: 'assistant',
          status: 'COMPLETED',
          modelId: run?.modelId || '',
          contentBlockList: [
            {
              contentIndex: 0,
              blockType: 'TEXT',
              status: 'COMPLETED',
              content: answerContent,
              toolCallId: null,
              toolName: null,
            },
          ],
          stopReason: 'stop',
          inputTokens: 0,
          outputTokens: 0,
        },
      ],
      toolExecutionList: [],
      lastSequence: run?.lastEventSequence || 0,
      answerMessageId: message.id,
      citationList: [],
      errorCode: null,
      errorMessage: null,
    }
  }

  /**
   * 提交当前消息输入.
   * @param event 表单提交事件
   * @return void
   */
  const handleSubmit = (event: FormEvent<HTMLFormElement>): void => {
    event.preventDefault()
    if (isFriendBlocked) {
      return
    }
    onSendMessage()
  }

  /**
   * 提交本次选择的全部附件并重置文件输入.
   * @param event 文件输入变化事件
   * @return void
   */
  const handleAttachmentChange = (
    event: ChangeEvent<HTMLInputElement>,
  ): void => {
    const fileList = Array.from(event.target.files || [])
    event.target.value = ''
    if (fileList.length > 0) {
      onSelectAttachmentList(fileList)
    }
  }

  return (
    <>
      <PanelHeader
        title={conversation.name}
        eyebrow={
          conversation.kind === 'group'
            ? `${conversation.memberList.length} 位成员`
            : conversation.status
        }
        isDetailsOpen={isDetailsOpen}
        backButtonRef={backButtonRef}
        onBackToList={onBackToList}
        onToggleDetails={onToggleDetails}
        onOpenDetailsSheet={onOpenDetailsSheet}
      />
      <div className="relative min-h-0 flex-1">
        <ScrollArea className="h-full" onScrollCapture={handleScrollCapture}>
          <div
            data-testid="chat-message-list"
            className="flex w-full flex-col gap-5 px-[clamp(16px,3vw,40px)] py-6"
          >
            {conversation.messages.map((message) => {
              const snapshotList = snapshotListByTriggerId.get(message.id) ?? []
              const run = message.agentRunId
                ? agentController.runList.find(
                    (item) => item.runId === message.agentRunId,
                  )
                : undefined
              const isAgentMessage = message.tone === 'agent'
              const shouldDeferPersistedAgentMessage = Boolean(
                isAgentMessage &&
                message.agentRunId &&
                agentController.snapshotByRunId[message.agentRunId] &&
                !progressiveRevealCompletedRunIdSet.has(message.agentRunId),
              )
              const persistedSnapshot =
                isAgentMessage && !shouldDeferPersistedAgentMessage
                  ? getPersistedAgentSnapshot(message)
                  : null
              const requesterName = run
                ? getRequesterName(run.requesterUserId)
                : getRequesterName(
                    message.mentionList?.find(
                      (mention) =>
                        mention.mentionType === messageMentionType.USER,
                    )?.targetId || '',
                  )

              return (
                <div key={message.id} className="contents">
                  {shouldDeferPersistedAgentMessage ? null : (
                    <div
                      ref={(element) =>
                        registerMessageElement(message.id, element)
                      }
                      className={cn(
                        'scroll-m-16 rounded-xl transition-shadow',
                        highlightedTargetKey === message.id &&
                          !isAgentMessage &&
                          'ring-offset-background ring-2 ring-blue-400 ring-offset-4',
                      )}
                    >
                      {persistedSnapshot ? (
                        <AgentMessageCard
                          snapshot={persistedSnapshot}
                          requesterName={requesterName}
                          createdAtLabel={message.time}
                          isHighlighted={highlightedTargetKey === message.id}
                          onOpenTrace={(runId) =>
                            void agentController.openTrace(runId)
                          }
                        />
                      ) : (
                        <MessageBubble
                          message={message}
                          currentUserName={currentUserName}
                          animateEntrance={newReceivedMessageIdSet.has(
                            message.id,
                          )}
                        />
                      )}
                    </div>
                  )}
                  {snapshotList.map((snapshot) => (
                    <div
                      key={snapshot.runId}
                      ref={(element) =>
                        registerMessageElement(snapshot.cardKey, element)
                      }
                      className="scroll-m-16"
                    >
                      <AgentMessageCard
                        snapshot={snapshot}
                        requesterName={getRequesterName(
                          snapshot.requesterUserId,
                        )}
                        isHighlighted={
                          highlightedTargetKey === snapshot.cardKey
                        }
                        isLiveSnapshot
                        onProgressiveRevealComplete={completeProgressiveReveal}
                        onOpenTrace={(runId) =>
                          void agentController.openTrace(runId)
                        }
                      />
                    </div>
                  ))}
                </div>
              )
            })}
            {detachedSnapshotList.map((snapshot) => (
              <div
                key={snapshot.runId}
                ref={(element) =>
                  registerMessageElement(snapshot.cardKey, element)
                }
                className="scroll-m-16"
              >
                <AgentMessageCard
                  snapshot={snapshot}
                  requesterName={getRequesterName(snapshot.requesterUserId)}
                  isHighlighted={highlightedTargetKey === snapshot.cardKey}
                  isLiveSnapshot
                  onProgressiveRevealComplete={completeProgressiveReveal}
                  onOpenTrace={(runId) => void agentController.openTrace(runId)}
                />
              </div>
            ))}
            <div ref={messageEndRef} aria-hidden className="h-px w-full" />
          </div>
        </ScrollArea>
        {isLoadingNextMessages ? (
          <div
            role="status"
            aria-label="正在加载消息"
            className="pointer-events-none absolute inset-0 z-10 flex items-center justify-center"
          >
            <div className="border-border bg-background/95 text-muted-foreground flex items-center gap-2 rounded-full border px-3 py-1.5 text-xs shadow-sm backdrop-blur-sm">
              <LoaderCircle
                aria-hidden
                className="size-4 animate-spin motion-reduce:animate-none"
              />
              <span>加载消息</span>
            </div>
          </div>
        ) : null}
        {unseenMessageCount > 0 ? (
          <Button
            type="button"
            variant="outline"
            size="sm"
            aria-live="polite"
            aria-label={`查看 ${unseenMessageCount} 条新消息`}
            className="bg-background/95 absolute right-4 bottom-4 z-20 rounded-full shadow-md backdrop-blur-sm"
            onClick={scrollToLatest}
          >
            <ArrowDown aria-hidden />
            {unseenMessageLabel} 条新消息
          </Button>
        ) : null}
      </div>
      <form
        className="border-border bg-background border-t px-3 py-3"
        onSubmit={handleSubmit}
      >
        <input
          ref={attachmentInputRef}
          type="file"
          multiple
          aria-label="选择附件文件"
          className="sr-only"
          onChange={handleAttachmentChange}
        />
        <div
          data-testid="chat-composer-shell"
          className="border-border bg-background relative w-full rounded-2xl border p-2 shadow-sm"
        >
          <StructuredMessageComposer
            document={composerDocument}
            mentionOptionList={mentionOptionList}
            placeholder={
              isFriendBlocked
                ? '已拉黑该好友，取消拉黑后可继续发送消息'
                : '输入消息，按 Enter 发送'
            }
            disabled={isFriendBlocked}
            onChange={onComposerChange}
            onSend={onSendMessage}
          />
          <div className="border-border flex items-center justify-between border-t pt-2">
            <div className="flex shrink-0 items-center gap-1">
              <Button
                type="button"
                variant="ghost"
                size="icon-sm"
                aria-label="添加附件"
                disabled={isFriendBlocked || isUploadingAttachment}
                className="text-muted-foreground hover:bg-muted hover:text-foreground"
                onClick={() => attachmentInputRef.current?.click()}
              >
                <Paperclip />
              </Button>
              <MessageEmojiMenu
                disabled={isFriendBlocked || isUploadingAttachment}
                onSelectEmoji={onSendEmoji}
              />
            </div>
            <Button
              type="submit"
              aria-label="发送消息"
              size="sm"
              disabled={
                isFriendBlocked || composerDocument.content.trim().length === 0
              }
            >
              <Send />
              发送
            </Button>
          </div>
          {uploadState ? (
            <p
              role="status"
              className="text-muted-foreground mt-2 truncate text-xs"
            >
              正在上传 {uploadState.fileIndex}/{uploadState.fileCount} ·{' '}
              {uploadState.fileName} · {uploadState.progress}%
            </p>
          ) : null}
        </div>
      </form>
      <AgentTraceDialog
        controller={agentController}
        requesterName={traceRequesterName}
        answerContent={traceAnswerContent}
      />
    </>
  )
}
