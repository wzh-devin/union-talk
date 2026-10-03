import {
  CheckCircle2,
  ChevronDown,
  CircleAlert,
  LoaderCircle,
} from 'lucide-react'
import { useEffect, useMemo, useRef } from 'react'

import { RunElapsedTitle } from '@/components/home/conversation/agent-run-elapsed-title'
import { AgentTraceContentBlockItem } from '@/components/home/conversation/agent-trace-content-block-item'
import { AgentTraceToolItem } from '@/components/home/conversation/agent-trace-tool-item'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogTitle,
} from '@/components/shadcn-ui/dialog'
import type { ConversationAgentController } from '@/hooks/use-conversation-agent'
import { useProgressiveText } from '@/hooks/use-progressive-text'
import {
  getAgentRunText,
  type AgentTraceTurn,
} from '@/pages/home/model/agent-run'

interface AgentTraceDialogProps {
  controller: Pick<
    ConversationAgentController,
    'traceRun' | 'trace' | 'snapshotByRunId' | 'isTraceLoading' | 'closeTrace'
  >
  requesterName: string
  answerContent?: string
}

/**
 * 格式化毫秒耗时.
 * @param durationMs 毫秒耗时
 * @return 毫秒或秒级文本
 */
const formatDuration = (durationMs: number | null): string => {
  if (durationMs === null || durationMs < 0) {
    return ''
  }
  if (durationMs < 1000) {
    return `${durationMs}ms`
  }
  const seconds = durationMs / 1000
  return `${Number.isInteger(seconds) ? seconds.toFixed(0) : seconds.toFixed(1)}s`
}

/**
 * 渲染由真实 Turn、Message、Content Block 和 Tool Call 组成的执行弹窗.
 * @param props 执行轨迹弹窗属性
 * @return Agent 执行详情弹窗
 */
export const AgentTraceDialog = ({
  controller,
  requesterName,
  answerContent = '',
}: AgentTraceDialogProps) => {
  const run = controller.traceRun
  const isOpen = run !== null
  const streamPreviewRef = useRef<HTMLDivElement>(null)
  const snapshot = run ? controller.snapshotByRunId[run.runId] : undefined
  const isRunning = run?.status === 'QUEUED' || run?.status === 'RUNNING'
  const snapshotText = snapshot ? getAgentRunText(snapshot) : ''
  const resolvedAnswerContent = answerContent || snapshotText
  const visibleAnswerContent = useProgressiveText({
    text: resolvedAnswerContent,
    enabled: Boolean(isRunning),
  })
  const turnList = useMemo<AgentTraceTurn[]>(() => {
    if (controller.trace?.turnList.length) {
      return controller.trace.turnList
    }
    if (!snapshot) {
      return []
    }
    const turnNoSet = new Set([
      ...snapshot.messageList.map((message) => message.turnNo),
      ...snapshot.toolExecutionList.map((execution) => execution.turnNo),
    ])
    return [...turnNoSet]
      .toSorted((left, right) => left - right)
      .map((turnNo) => ({
        turnNo,
        status: isRunning ? 'RUNNING' : 'SUCCEEDED',
        stopReason: null,
        durationMs: null,
        messageList: snapshot.messageList.filter(
          (message) => message.turnNo === turnNo,
        ),
        toolCallList: snapshot.toolExecutionList.filter(
          (execution) => execution.turnNo === turnNo,
        ),
      }))
  }, [controller.trace?.turnList, isRunning, snapshot])

  useEffect(() => {
    if (!visibleAnswerContent || !streamPreviewRef.current) {
      return
    }
    streamPreviewRef.current.scrollTop = streamPreviewRef.current.scrollHeight
  }, [visibleAnswerContent])

  return (
    <Dialog
      open={isOpen}
      onOpenChange={(nextIsOpen) => {
        if (!nextIsOpen) {
          controller.closeTrace()
        }
      }}
    >
      <DialogContent
        closeLabel="关闭 Agent 执行过程"
        overlayClassName="bg-black/10 backdrop-blur-[1px]"
        className="flex max-h-[min(800px,calc(100svh-32px))] w-[min(780px,calc(100vw-32px))] flex-col overflow-hidden rounded-xl border-black/10 p-0 shadow-sm"
      >
        {run ? (
          <>
            <header className="border-border/70 flex min-h-16 items-center gap-4 border-b px-6 py-4 pr-14">
              <DialogTitle className="shrink-0 text-base font-medium tracking-tight">
                <RunElapsedTitle key={run.runId} run={run} />
              </DialogTitle>
              <DialogDescription className="text-muted-foreground ml-auto flex min-w-0 items-center gap-2 truncate text-xs">
                <span className="shrink-0">回复 @{requesterName}</span>
                <span aria-hidden>·</span>
                <span className="truncate font-mono">AR-{run.runId}</span>
              </DialogDescription>
            </header>

            <div className="min-h-0 flex-1 [scrollbar-width:thin] overflow-y-auto overscroll-contain">
              <div className="mx-auto max-w-[680px] px-6 py-6 sm:px-8">
                {controller.isTraceLoading ? (
                  <div
                    role="status"
                    className="text-muted-foreground flex min-h-64 items-center justify-center gap-2 text-sm"
                  >
                    <LoaderCircle aria-hidden className="size-4 animate-spin" />
                    正在读取执行过程
                  </div>
                ) : (
                  <>
                    <section aria-label="Agent 最终回复">
                      <div className="flex items-center gap-2">
                        {isRunning ? (
                          <LoaderCircle
                            aria-hidden
                            className="text-muted-foreground size-4 animate-spin"
                          />
                        ) : run.status === 'SUCCEEDED' ? (
                          <CheckCircle2
                            aria-hidden
                            className="size-4 text-emerald-600"
                          />
                        ) : run.status === 'FAILED' ? (
                          <CircleAlert
                            aria-hidden
                            className="text-destructive size-4"
                          />
                        ) : null}
                        <h3 className="text-sm font-medium">
                          {isRunning ? '回复生成中' : '最终回复'}
                        </h3>
                      </div>
                      {visibleAnswerContent ? (
                        <div
                          ref={streamPreviewRef}
                          aria-live={isRunning ? 'polite' : 'off'}
                          className="border-border/70 bg-muted/25 mt-3 max-h-80 [scrollbar-width:thin] overflow-y-auto rounded-xl border px-5 py-4 text-sm leading-7 whitespace-pre-wrap"
                        >
                          {visibleAnswerContent}
                        </div>
                      ) : run.errorMessage ? (
                        <p className="bg-destructive/5 text-destructive mt-3 rounded-xl px-4 py-3 text-sm">
                          {run.errorMessage}
                        </p>
                      ) : null}
                    </section>

                    {turnList.length > 0 ? (
                      <details
                        key={`${run.runId}:${isRunning ? 'running' : 'terminal'}`}
                        open={isRunning || undefined}
                        className="group border-border/70 mt-6 border-t pt-1"
                      >
                        <summary className="hover:bg-muted/50 -mx-2 flex cursor-pointer list-none items-center gap-2 rounded-lg px-2 py-3 text-sm [&::-webkit-details-marker]:hidden">
                          <span className="font-medium">思考与执行详情</span>
                          <span className="text-muted-foreground text-xs">
                            {turnList.length} 个 Turn
                          </span>
                          <ChevronDown className="text-muted-foreground ml-auto size-4 transition-transform group-open:rotate-180" />
                        </summary>
                        <div className="space-y-6 pt-3 pb-2">
                          {turnList.map((turn) => {
                            const blockList = turn.messageList.flatMap(
                              (message) =>
                                message.contentBlockList.filter(
                                  (block) => block.blockType !== 'TOOL_CALL',
                                ),
                            )
                            return (
                              <section
                                key={turn.turnNo}
                                aria-label={`Turn ${turn.turnNo}`}
                              >
                                <div className="mb-3 flex items-center gap-2 text-xs">
                                  <span className="font-medium">
                                    Turn {turn.turnNo}
                                  </span>
                                  <span className="text-muted-foreground">
                                    {turn.status}
                                  </span>
                                  <span className="text-muted-foreground ml-auto tabular-nums">
                                    {formatDuration(turn.durationMs)}
                                  </span>
                                </div>
                                <ol className="space-y-5">
                                  {blockList.map((block) => (
                                    <AgentTraceContentBlockItem
                                      key={`${turn.turnNo}:${block.contentIndex}:${block.blockType}`}
                                      block={block}
                                    />
                                  ))}
                                  {turn.toolCallList.map((execution) => (
                                    <AgentTraceToolItem
                                      key={execution.toolCallId}
                                      execution={execution}
                                    />
                                  ))}
                                </ol>
                              </section>
                            )
                          })}
                        </div>
                      </details>
                    ) : null}
                  </>
                )}
              </div>
            </div>
          </>
        ) : null}
      </DialogContent>
    </Dialog>
  )
}
