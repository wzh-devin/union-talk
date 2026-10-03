import {
  useCallback,
  useEffect,
  useRef,
  useState,
  type KeyboardEvent as ReactKeyboardEvent,
  type PointerEvent as ReactPointerEvent,
  type RefObject,
} from 'react'

const DETAILS_PANEL_DEFAULT_WIDTH = 440
const DETAILS_PANEL_MIN_WIDTH = 280
const DETAILS_PANEL_HARD_MAX_WIDTH = 560
const RAIL_WIDTH = 56
const LIST_PANEL_WIDTH = 280
const MAIN_PANEL_MIN_WIDTH = 520

interface ResizeSession {
  startX: number
  startWidth: number
  pointerId?: number
}

const getInitialWorkspaceWidth = () =>
  typeof window === 'undefined'
    ? 1440 - RAIL_WIDTH
    : window.innerWidth - RAIL_WIDTH

/**
 * 计算详情栏在当前视口下的最大宽度.
 * @param workspaceWidth 工作区宽度
 * @return number 详情栏最大宽度
 */
const getDetailsPanelMaxWidth = (workspaceWidth: number): number => {
  const availableWidth =
    workspaceWidth - LIST_PANEL_WIDTH - MAIN_PANEL_MIN_WIDTH

  return Math.min(
    DETAILS_PANEL_HARD_MAX_WIDTH,
    Math.max(DETAILS_PANEL_MIN_WIDTH, availableWidth),
  )
}

const clampDetailsPanelWidth = (width: number, maxWidth: number): number =>
  Math.min(maxWidth, Math.max(DETAILS_PANEL_MIN_WIDTH, width))

/**
 * 管理首页桌面详情栏拖拽宽度.
 * @return 详情栏宽度、拖拽状态和调宽处理器
 */
export const useHomeDetailsPanelResize = (
  workspaceElementRef?: RefObject<HTMLDivElement | null>,
) => {
  const [detailsPanelMaxWidth, setDetailsPanelMaxWidth] = useState(() =>
    getDetailsPanelMaxWidth(getInitialWorkspaceWidth()),
  )
  const [preferredDetailsPanelWidth, setPreferredDetailsPanelWidth] = useState(
    DETAILS_PANEL_DEFAULT_WIDTH,
  )
  const [isResizingDetailsPanel, setIsResizingDetailsPanel] = useState(false)
  const resizeSessionRef = useRef<ResizeSession | null>(null)
  const detailsPanelWidth = clampDetailsPanelWidth(
    preferredDetailsPanelWidth,
    detailsPanelMaxWidth,
  )

  /**
   * 根据真实工作区宽度同步详情栏上限.
   * @param workspaceWidth 工作区宽度
   * @return void
   */
  const syncDetailsPanelMaxWidth = useCallback(
    (workspaceWidth: number): void => {
      if (workspaceWidth <= 0) {
        return
      }
      setDetailsPanelMaxWidth(getDetailsPanelMaxWidth(workspaceWidth))
    },
    [],
  )

  useEffect(() => {
    const element = workspaceElementRef?.current

    if (!element) {
      return
    }

    syncDetailsPanelMaxWidth(element.clientWidth)
    if (typeof ResizeObserver === 'undefined') {
      return
    }

    const resizeObserver = new ResizeObserver((entryList) => {
      const workspaceWidth =
        entryList[0]?.contentRect.width ?? element.clientWidth
      syncDetailsPanelMaxWidth(workspaceWidth)
    })
    resizeObserver.observe(element)

    return () => resizeObserver.disconnect()
  }, [syncDetailsPanelMaxWidth, workspaceElementRef])

  useEffect(() => {
    const handleWindowResize = () => {
      syncDetailsPanelMaxWidth(
        workspaceElementRef?.current?.clientWidth ?? getInitialWorkspaceWidth(),
      )
    }

    window.addEventListener('resize', handleWindowResize)

    return () => window.removeEventListener('resize', handleWindowResize)
  }, [syncDetailsPanelMaxWidth, workspaceElementRef])

  useEffect(() => {
    if (!isResizingDetailsPanel) {
      return
    }

    const handlePointerMove = (event: PointerEvent) => {
      const resizeSession = resizeSessionRef.current

      if (!resizeSession) {
        return
      }
      if (
        resizeSession.pointerId !== undefined &&
        event.pointerId !== resizeSession.pointerId
      ) {
        return
      }

      setPreferredDetailsPanelWidth(
        clampDetailsPanelWidth(
          resizeSession.startWidth + resizeSession.startX - event.clientX,
          detailsPanelMaxWidth,
        ),
      )
    }

    const finishResize = (event: PointerEvent) => {
      const resizeSession = resizeSessionRef.current

      if (
        resizeSession?.pointerId !== undefined &&
        event.pointerId !== resizeSession.pointerId
      ) {
        return
      }

      resizeSessionRef.current = null
      setIsResizingDetailsPanel(false)
    }

    window.addEventListener('pointermove', handlePointerMove)
    window.addEventListener('pointerup', finishResize)
    window.addEventListener('pointercancel', finishResize)

    return () => {
      window.removeEventListener('pointermove', handlePointerMove)
      window.removeEventListener('pointerup', finishResize)
      window.removeEventListener('pointercancel', finishResize)
    }
  }, [detailsPanelMaxWidth, isResizingDetailsPanel])

  useEffect(() => {
    if (!isResizingDetailsPanel) {
      return
    }

    const originalCursor = document.body.style.cursor
    const originalUserSelect = document.body.style.userSelect

    document.body.style.cursor = 'col-resize'
    document.body.style.userSelect = 'none'

    return () => {
      document.body.style.cursor = originalCursor
      document.body.style.userSelect = originalUserSelect
    }
  }, [isResizingDetailsPanel])

  /**
   * 开始详情栏指针拖拽.
   * @param event 指针事件
   * @return void
   */
  const beginDetailsPanelResize = useCallback(
    (event: ReactPointerEvent<HTMLElement>): void => {
      if (event.button > 0) {
        return
      }

      event.preventDefault()
      event.currentTarget.setPointerCapture?.(event.pointerId)
      resizeSessionRef.current = {
        startX: event.clientX,
        startWidth: detailsPanelWidth,
        pointerId:
          typeof event.pointerId === 'number' ? event.pointerId : undefined,
      }
      setIsResizingDetailsPanel(true)
    },
    [detailsPanelWidth],
  )

  /**
   * 按增量调整详情栏宽度并限制边界.
   * @param delta 宽度增量
   * @return void
   */
  const resizeDetailsPanelBy = useCallback(
    (delta: number): void => {
      setPreferredDetailsPanelWidth((currentWidth) =>
        clampDetailsPanelWidth(
          clampDetailsPanelWidth(currentWidth, detailsPanelMaxWidth) + delta,
          detailsPanelMaxWidth,
        ),
      )
    },
    [detailsPanelMaxWidth],
  )

  /**
   * 重置详情栏默认宽度.
   * @return void
   */
  const resetDetailsPanelWidth = useCallback((): void => {
    setPreferredDetailsPanelWidth(DETAILS_PANEL_DEFAULT_WIDTH)
  }, [])

  /**
   * 处理详情栏拖拽条的键盘调宽.
   * @param event 键盘事件
   * @return void
   */
  const handleDetailsPanelResizeKeyDown = useCallback(
    (event: ReactKeyboardEvent<HTMLDivElement>): void => {
      if (event.key === 'ArrowLeft') {
        event.preventDefault()
        resizeDetailsPanelBy(24)
        return
      }
      if (event.key === 'ArrowRight') {
        event.preventDefault()
        resizeDetailsPanelBy(-24)
        return
      }
      if (event.key === 'Home') {
        event.preventDefault()
        resizeDetailsPanelBy(-detailsPanelWidth)
        return
      }
      if (event.key === 'End') {
        event.preventDefault()
        resizeDetailsPanelBy(detailsPanelMaxWidth - detailsPanelWidth)
      }
    },
    [detailsPanelMaxWidth, detailsPanelWidth, resizeDetailsPanelBy],
  )

  return {
    detailsPanelWidth,
    detailsPanelMaxWidth,
    isResizingDetailsPanel,
    beginDetailsPanelResize,
    handleDetailsPanelResizeKeyDown,
    resizeDetailsPanelBy,
    resetDetailsPanelWidth,
  }
}
