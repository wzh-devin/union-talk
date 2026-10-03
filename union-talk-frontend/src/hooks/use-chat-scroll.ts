import {
  useCallback,
  useEffect,
  useLayoutEffect,
  useRef,
  useState,
  type RefObject,
  type UIEvent,
} from 'react'

import type { Message } from '@/pages/home/model/types'

const LOAD_NEXT_THRESHOLD = 64
const NEAR_BOTTOM_THRESHOLD = 96

interface ChatScrollState {
  conversationId: string
  messageList: Message[]
  isNearBottom: boolean
  newReceivedMessageIdSet: Set<string>
  unseenMessageCount: number
  scrollRequestId: number
  scrollBehavior: ScrollBehavior
}

interface PaginationSnapshot {
  conversationId: string
  scrollHeight: number
  scrollTop: number
}

interface UseChatScrollOptions {
  conversationId: string
  messageList: Message[]
  hasNextMessages: boolean
  isLoadingNextMessages: boolean
  onLoadNextMessages?: () => void
  onLatestReceivedMessageViewed?: (
    conversationId: string,
    messageId: string,
  ) => void
}

interface UseChatScrollResult {
  messageEndRef: RefObject<HTMLDivElement | null>
  newReceivedMessageIdSet: Set<string>
  unseenMessageCount: number
  handleScrollCapture: (event: UIEvent<HTMLElement>) => void
  scrollToLatest: () => void
}

/**
 * 获取符合用户动态效果偏好的滚动方式.
 * @return ScrollBehavior 消息滚动方式
 */
const getPreferredScrollBehavior = (): ScrollBehavior => {
  if (
    typeof window !== 'undefined' &&
    window.matchMedia?.('(prefers-reduced-motion: reduce)').matches
  ) {
    return 'auto'
  }
  return 'smooth'
}

/**
 * 根据新的消息列表计算跟随、动画与未查看消息状态.
 * @param currentState 当前聊天滚动状态
 * @param conversationId 当前会话编号
 * @param messageList 当前消息列表
 * @return ChatScrollState 下一聊天滚动状态
 */
const getNextChatScrollState = (
  currentState: ChatScrollState,
  conversationId: string,
  messageList: Message[],
): ChatScrollState => {
  if (currentState.conversationId !== conversationId) {
    return {
      conversationId,
      messageList,
      isNearBottom: true,
      newReceivedMessageIdSet: new Set(),
      unseenMessageCount: 0,
      scrollRequestId: currentState.scrollRequestId + 1,
      scrollBehavior: 'auto',
    }
  }

  const previousLatestMessageId = currentState.messageList.at(-1)?.id
  const latestMessageId = messageList.at(-1)?.id
  if (!previousLatestMessageId && latestMessageId) {
    return {
      ...currentState,
      messageList,
      isNearBottom: true,
      unseenMessageCount: 0,
      scrollRequestId: currentState.scrollRequestId + 1,
      scrollBehavior: 'auto',
    }
  }

  const previousLatestIndex = previousLatestMessageId
    ? messageList.findIndex((message) => message.id === previousLatestMessageId)
    : -1
  const appendedMessageList =
    previousLatestIndex >= 0 ? messageList.slice(previousLatestIndex + 1) : []
  if (appendedMessageList.length === 0) {
    return { ...currentState, messageList }
  }

  const appendedReceivedMessageList = appendedMessageList.filter(
    (message) => message.tone !== 'self',
  )
  const newReceivedMessageIdSet = new Set(currentState.newReceivedMessageIdSet)
  appendedReceivedMessageList.forEach((message) =>
    newReceivedMessageIdSet.add(message.id),
  )
  const hasSentMessage = appendedMessageList.some(
    (message) => message.tone === 'self',
  )
  if (currentState.isNearBottom || hasSentMessage) {
    return {
      ...currentState,
      messageList,
      isNearBottom: true,
      newReceivedMessageIdSet,
      unseenMessageCount: 0,
      scrollRequestId: currentState.scrollRequestId + 1,
      scrollBehavior: 'smooth',
    }
  }

  return {
    ...currentState,
    messageList,
    newReceivedMessageIdSet,
    unseenMessageCount:
      currentState.unseenMessageCount + appendedReceivedMessageList.length,
  }
}

/**
 * 管理聊天消息跟随、游标分页位置与未查看消息状态.
 * @param options 聊天滚动配置
 * @return UseChatScrollResult 聊天滚动状态与操作
 */
export const useChatScroll = ({
  conversationId,
  messageList,
  hasNextMessages,
  isLoadingNextMessages,
  onLoadNextMessages,
  onLatestReceivedMessageViewed,
}: UseChatScrollOptions): UseChatScrollResult => {
  const messageEndRef = useRef<HTMLDivElement>(null)
  const viewportRef = useRef<HTMLElement>(null)
  const previousConversationIdRef = useRef(conversationId)
  const previousFirstMessageIdRef = useRef(messageList.at(0)?.id)
  const paginationSnapshotRef = useRef<PaginationSnapshot>(null)
  const isPaginationRequestPendingRef = useRef(false)
  const hasObservedPaginationLoadingRef = useRef(false)
  const isNearBottomRef = useRef(true)
  const initialMediaSettledConversationIdRef = useRef('')
  const [isDocumentVisible, setIsDocumentVisible] = useState(
    () =>
      typeof document === 'undefined' || document.visibilityState === 'visible',
  )
  const [chatScrollState, setChatScrollState] = useState<ChatScrollState>(
    () => ({
      conversationId,
      messageList,
      isNearBottom: true,
      newReceivedMessageIdSet: new Set(),
      unseenMessageCount: 0,
      scrollRequestId: 1,
      scrollBehavior: 'auto',
    }),
  )

  if (
    chatScrollState.conversationId !== conversationId ||
    chatScrollState.messageList !== messageList
  ) {
    setChatScrollState((currentState) =>
      getNextChatScrollState(currentState, conversationId, messageList),
    )
  }

  /**
   * 将消息尾部滚动到视口内.
   * @param behavior 指定滚动方式
   * @return void
   */
  const scrollMessageEndIntoView = useCallback(
    (behavior: ScrollBehavior): void => {
      messageEndRef.current?.scrollIntoView?.({ behavior, block: 'end' })
    },
    [],
  )

  /**
   * 前往最新消息并清除未查看计数.
   * @return void
   */
  const scrollToLatest = useCallback((): void => {
    isNearBottomRef.current = true
    setChatScrollState((currentState) => ({
      ...currentState,
      isNearBottom: true,
      unseenMessageCount: 0,
      scrollRequestId: currentState.scrollRequestId + 1,
      scrollBehavior: 'smooth',
    }))
  }, [])

  useLayoutEffect(() => {
    isNearBottomRef.current = chatScrollState.isNearBottom
  }, [chatScrollState.isNearBottom])

  useEffect(() => {
    const handleVisibilityChange = (): void => {
      setIsDocumentVisible(document.visibilityState === 'visible')
    }
    document.addEventListener('visibilitychange', handleVisibilityChange)
    return () =>
      document.removeEventListener('visibilitychange', handleVisibilityChange)
  }, [])

  const latestMessage = messageList.at(-1)
  useEffect(() => {
    if (
      !onLatestReceivedMessageViewed ||
      !isDocumentVisible ||
      !chatScrollState.isNearBottom ||
      !latestMessage?.id ||
      latestMessage.tone === 'self'
    ) {
      return
    }
    onLatestReceivedMessageViewed(conversationId, latestMessage.id)
  }, [
    chatScrollState.isNearBottom,
    conversationId,
    isDocumentVisible,
    latestMessage?.id,
    latestMessage?.tone,
    onLatestReceivedMessageViewed,
  ])

  useLayoutEffect(() => {
    const isConversationChanged =
      previousConversationIdRef.current !== conversationId
    const currentMessageIdList = messageList.map((message) => message.id)
    const previousFirstMessageIndex = previousFirstMessageIdRef.current
      ? currentMessageIdList.indexOf(previousFirstMessageIdRef.current)
      : -1
    const paginationSnapshot = paginationSnapshotRef.current

    if (
      !isConversationChanged &&
      paginationSnapshot &&
      paginationSnapshot.conversationId === conversationId &&
      previousFirstMessageIndex > 0 &&
      viewportRef.current
    ) {
      const addedScrollHeight =
        viewportRef.current.scrollHeight - paginationSnapshot.scrollHeight
      viewportRef.current.scrollTop =
        paginationSnapshot.scrollTop + Math.max(0, addedScrollHeight)
      paginationSnapshotRef.current = null
    }

    if (isConversationChanged) {
      paginationSnapshotRef.current = null
      isPaginationRequestPendingRef.current = false
      hasObservedPaginationLoadingRef.current = false
    }

    previousConversationIdRef.current = conversationId
    previousFirstMessageIdRef.current = currentMessageIdList.at(0)
  }, [conversationId, messageList])

  useLayoutEffect(() => {
    const behavior =
      chatScrollState.scrollBehavior === 'smooth'
        ? getPreferredScrollBehavior()
        : 'auto'
    scrollMessageEndIntoView(behavior)
  }, [
    chatScrollState.scrollBehavior,
    chatScrollState.scrollRequestId,
    scrollMessageEndIntoView,
  ])

  useLayoutEffect(() => {
    const messageListElement = messageEndRef.current?.parentElement
    if (!messageListElement || typeof ResizeObserver === 'undefined') {
      return
    }

    const resizeObserver = new ResizeObserver(() => {
      if (isNearBottomRef.current && paginationSnapshotRef.current === null) {
        scrollMessageEndIntoView('auto')
      }
    })
    resizeObserver.observe(messageListElement)
    return () => resizeObserver.disconnect()
  }, [scrollMessageEndIntoView])

  useLayoutEffect(() => {
    const messageListElement = messageEndRef.current?.parentElement
    if (
      !messageListElement ||
      messageList.length === 0 ||
      initialMediaSettledConversationIdRef.current === conversationId
    ) {
      return
    }

    const mediaElementList = Array.from(
      messageListElement.querySelectorAll<HTMLImageElement | HTMLVideoElement>(
        'img, video',
      ),
    )
    const pendingMediaElementList = mediaElementList.filter((element) =>
      element instanceof HTMLImageElement
        ? !element.complete
        : element.readyState < HTMLMediaElement.HAVE_METADATA,
    )

    if (pendingMediaElementList.length === 0) {
      initialMediaSettledConversationIdRef.current = conversationId
      scrollMessageEndIntoView('auto')
      return
    }

    let pendingMediaCount = pendingMediaElementList.length
    let animationFrameId = 0
    const handleMediaSettled = (): void => {
      pendingMediaCount -= 1
      if (pendingMediaCount > 0) {
        return
      }
      initialMediaSettledConversationIdRef.current = conversationId
      animationFrameId = window.requestAnimationFrame(() =>
        scrollMessageEndIntoView('auto'),
      )
    }

    pendingMediaElementList.forEach((element) => {
      element.addEventListener('error', handleMediaSettled, { once: true })
      if (element instanceof HTMLImageElement) {
        element.addEventListener('load', handleMediaSettled, { once: true })
      } else {
        element.addEventListener('loadedmetadata', handleMediaSettled, {
          once: true,
        })
      }
    })

    return () => {
      pendingMediaElementList.forEach((element) => {
        element.removeEventListener('error', handleMediaSettled)
        if (element instanceof HTMLImageElement) {
          element.removeEventListener('load', handleMediaSettled)
        } else {
          element.removeEventListener('loadedmetadata', handleMediaSettled)
        }
      })
      if (animationFrameId) {
        window.cancelAnimationFrame(animationFrameId)
      }
    }
  }, [conversationId, messageList, scrollMessageEndIntoView])

  useLayoutEffect(() => {
    if (isLoadingNextMessages) {
      hasObservedPaginationLoadingRef.current = true
      return
    }
    if (hasObservedPaginationLoadingRef.current) {
      hasObservedPaginationLoadingRef.current = false
      isPaginationRequestPendingRef.current = false
      paginationSnapshotRef.current = null
    }
  }, [isLoadingNextMessages])

  /**
   * 同步滚动位置并在接近顶部时请求下一页消息.
   * @param event 消息视口滚动事件
   * @return void
   */
  const handleScrollCapture = useCallback(
    (event: UIEvent<HTMLElement>): void => {
      const viewport = event.target as HTMLElement
      viewportRef.current = viewport
      const bottomDistance =
        viewport.scrollHeight - viewport.scrollTop - viewport.clientHeight
      const isNearBottom = bottomDistance <= NEAR_BOTTOM_THRESHOLD
      isNearBottomRef.current = isNearBottom
      setChatScrollState((currentState) => {
        const unseenMessageCount = isNearBottom
          ? 0
          : currentState.unseenMessageCount
        if (
          currentState.isNearBottom === isNearBottom &&
          currentState.unseenMessageCount === unseenMessageCount
        ) {
          return currentState
        }
        return { ...currentState, isNearBottom, unseenMessageCount }
      })

      if (
        viewport.scrollTop > LOAD_NEXT_THRESHOLD ||
        !hasNextMessages ||
        isLoadingNextMessages ||
        isPaginationRequestPendingRef.current ||
        !onLoadNextMessages
      ) {
        return
      }

      paginationSnapshotRef.current = {
        conversationId,
        scrollHeight: viewport.scrollHeight,
        scrollTop: viewport.scrollTop,
      }
      isPaginationRequestPendingRef.current = true
      onLoadNextMessages()
    },
    [
      conversationId,
      hasNextMessages,
      isLoadingNextMessages,
      onLoadNextMessages,
    ],
  )

  return {
    messageEndRef,
    newReceivedMessageIdSet: chatScrollState.newReceivedMessageIdSet,
    unseenMessageCount: chatScrollState.unseenMessageCount,
    handleScrollCapture,
    scrollToLatest,
  }
}
