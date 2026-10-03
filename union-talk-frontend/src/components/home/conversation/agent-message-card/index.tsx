import {
  CheckCircle2,
  CircleAlert,
  LoaderCircle,
  Sparkles,
  Wrench,
} from 'lucide-react'
import { AnimatePresence, motion, useReducedMotion } from 'motion/react'
import { useEffect, useRef } from 'react'

import { MENTION_TEXT_CLASS_NAME } from '@/components/home/conversation/mention-style'
import { useProgressiveText } from '@/hooks/use-progressive-text'
import {
  getActiveAgentTool,
  getAgentRunText,
  getAgentRunThinking,
  type AgentRunSnapshot,
  type AgentRunStatus,
} from '@/pages/home/model/agent-run'
import { cn } from '@/utils/class-name'

interface AgentMessageCardProps {
  snapshot: AgentRunSnapshot
  requesterName: string
  createdAtLabel?: string
  isHighlighted?: boolean
  isLiveSnapshot?: boolean
  onProgressiveRevealComplete?: (runId: string) => void
  onOpenTrace: (runId: string) => void
}

const statusViewMap: Record<
  AgentRunStatus,
  { label: string; className: string }
> = {
  QUEUED: { label: '排队中', className: 'bg-zinc-100 text-zinc-600' },
  RUNNING: { label: '执行中', className: 'bg-zinc-100 text-zinc-700' },
  SUCCEEDED: { label: '已完成', className: 'bg-emerald-50 text-emerald-700' },
  FAILED: { label: '失败', className: 'bg-red-50 text-red-600' },
  CANCELLED: { label: '已取消', className: 'bg-zinc-100 text-zinc-500' },
}

interface AgentActivityView {
  key: string
  label: string
  kind: 'thinking' | 'tool'
}

/**
 * 从真实 Content Block 和 Tool Execution 推导当前活动.
 * @param snapshot 当前 Agent Run 快照
 * @return 当前真实活动；没有活动时返回空
 */
const getCurrentActivity = (
  snapshot: AgentRunSnapshot,
): AgentActivityView | null => {
  const tool = getActiveAgentTool(snapshot)
  if (tool) {
    return {
      key: `tool:${tool.toolCallId}`,
      label: tool.displayName || tool.toolName,
      kind: 'tool',
    }
  }
  const thinking = getAgentRunThinking(snapshot).trim()
  if (!thinking) {
    return null
  }
  const latestLine = thinking.split(/\r?\n/u).filter(Boolean).at(-1) ?? thinking
  return {
    key: `thinking:${latestLine}`,
    label: latestLine,
    kind: 'thinking',
  }
}

/**
 * 渲染同一 Run 原位更新的流式 AI 消息卡片.
 * @param props Agent 卡片属性
 * @return 可打开执行过程的消息卡片
 */
export const AgentMessageCard = ({
  snapshot,
  requesterName,
  createdAtLabel = '刚刚',
  isHighlighted = false,
  isLiveSnapshot = false,
  onProgressiveRevealComplete,
  onOpenTrace,
}: AgentMessageCardProps) => {
  const status = snapshot.status || 'RUNNING'
  const statusView = statusViewMap[status]
  const isRunning = status === 'QUEUED' || status === 'RUNNING'
  const outputViewportRef = useRef<HTMLDivElement>(null)
  const notifiedRunIdRef = useRef('')
  const shouldReduceMotion = useReducedMotion()
  const targetText = getAgentRunText(snapshot)
  const progressiveContent = useProgressiveText({
    text: targetText,
    enabled: isRunning || isLiveSnapshot,
  })
  const isProgressivelyRevealing =
    Boolean(targetText) && progressiveContent !== targetText
  const content =
    progressiveContent ||
    (status === 'FAILED' && snapshot.errorMessage ? snapshot.errorMessage : '')
  const activity = isRunning ? getCurrentActivity(snapshot) : null
  const isOutputActive =
    Boolean(content) && (isRunning || isProgressivelyRevealing)

  useEffect(() => {
    if (
      isRunning ||
      !isLiveSnapshot ||
      progressiveContent !== targetText ||
      notifiedRunIdRef.current === snapshot.runId
    ) {
      return
    }
    notifiedRunIdRef.current = snapshot.runId
    onProgressiveRevealComplete?.(snapshot.runId)
  }, [
    isLiveSnapshot,
    isRunning,
    onProgressiveRevealComplete,
    progressiveContent,
    snapshot.runId,
    targetText,
  ])

  useEffect(() => {
    if (!content || !outputViewportRef.current) {
      return
    }
    const animationFrameId = window.requestAnimationFrame(() => {
      const viewport = outputViewportRef.current
      if (!viewport) {
        return
      }
      if (typeof viewport.scrollTo === 'function') {
        viewport.scrollTo({ top: viewport.scrollHeight, behavior: 'smooth' })
      } else {
        viewport.scrollTop = viewport.scrollHeight
      }
    })
    return () => window.cancelAnimationFrame(animationFrameId)
  }, [content])

  return (
    <article className="flex gap-3">
      <span className="mt-1 grid size-9 shrink-0 place-items-center rounded-full bg-black text-[11px] font-semibold text-white">
        AI
      </span>
      <div className="w-fit max-w-[min(680px,85%)] min-w-0">
        <div className="text-muted-foreground mb-1 flex items-center gap-2 text-xs">
          <span className="text-foreground font-medium">AI 助手</span>
          <span
            className={cn(
              'inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-[11px]',
              statusView.className,
            )}
          >
            {isRunning ? (
              <LoaderCircle aria-hidden className="size-3 animate-spin" />
            ) : status === 'SUCCEEDED' ? (
              <CheckCircle2 aria-hidden className="size-3" />
            ) : status === 'FAILED' ? (
              <CircleAlert aria-hidden className="size-3" />
            ) : (
              <Sparkles aria-hidden className="size-3" />
            )}
            {statusView.label}
          </span>
          <span>{createdAtLabel}</span>
        </div>
        <div
          role="button"
          tabIndex={0}
          aria-label={`查看 Agent ${snapshot.runId} 执行过程`}
          className={cn(
            'bg-background w-full overflow-hidden rounded-xl border text-left shadow-sm transition focus-visible:ring-2 focus-visible:ring-blue-400 focus-visible:ring-offset-2 focus-visible:outline-none',
            isRunning ? 'border-blue-400' : 'border-border',
            isHighlighted &&
              'ring-offset-background ring-2 ring-blue-400 ring-offset-2',
          )}
          onClick={() => onOpenTrace(snapshot.runId)}
          onKeyDown={(event) => {
            if (event.key !== 'Enter' && event.key !== ' ') {
              return
            }
            event.preventDefault()
            onOpenTrace(snapshot.runId)
          }}
        >
          <p className="px-4 pt-3 text-sm font-medium">
            <span data-mention-type="USER" className={MENTION_TEXT_CLASS_NAME}>
              @{requesterName}
            </span>
          </p>
          <div className="px-4 pb-3">
            {activity ? (
              <div
                aria-live="polite"
                className="relative mt-2 h-5 overflow-hidden"
              >
                <AnimatePresence initial={false}>
                  <motion.div
                    key={activity.key}
                    className="text-muted-foreground absolute inset-x-0 flex items-center gap-1.5 text-xs"
                    initial={shouldReduceMotion ? false : { opacity: 0, y: 8 }}
                    animate={{ opacity: 1, y: 0 }}
                    exit={
                      shouldReduceMotion
                        ? { opacity: 0 }
                        : { opacity: 0, y: -8 }
                    }
                    transition={{
                      duration: shouldReduceMotion ? 0 : 0.18,
                      ease: [0.25, 1, 0.5, 1],
                    }}
                  >
                    {activity.kind === 'tool' ? (
                      <Wrench aria-hidden className="size-3.5" />
                    ) : (
                      <LoaderCircle
                        aria-hidden
                        className="size-3.5 animate-spin motion-reduce:animate-none"
                      />
                    )}
                    <span className="truncate">{activity.label}</span>
                  </motion.div>
                </AnimatePresence>
              </div>
            ) : null}

            {content ? (
              <div
                ref={outputViewportRef}
                aria-label="AI 回复输出"
                aria-live={isOutputActive ? 'polite' : 'off'}
                aria-relevant="additions text"
                className="mt-2 max-h-56 [scrollbar-width:thin] overflow-y-auto overscroll-contain scroll-smooth motion-reduce:scroll-auto"
              >
                <p className="text-foreground text-sm leading-6 whitespace-pre-wrap">
                  {content}
                  {isOutputActive ? (
                    <span
                      aria-hidden
                      className="ml-1 inline-block h-4 w-0.5 animate-pulse bg-current align-middle motion-reduce:animate-none"
                    />
                  ) : null}
                </p>
              </div>
            ) : null}
          </div>
        </div>
      </div>
    </article>
  )
}
