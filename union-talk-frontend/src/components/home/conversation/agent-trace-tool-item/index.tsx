import { ChevronDown, CircleAlert, LoaderCircle, Wrench } from 'lucide-react'

import type { AgentToolExecution } from '@/pages/home/model/agent-run'

interface AgentTraceToolItemProps {
  execution: AgentToolExecution
}

const detailLabelMap: Record<string, string> = {
  query: '检索内容',
  scope: '检索范围',
  resultCount: '命中数量',
  sourceCount: '来源数量',
  resourceId: '资源 ID',
  resourceIds: '资源 ID',
  participantIds: '参与者',
  timeFrom: '开始时间',
  timeTo: '结束时间',
  evidenceCount: '证据数量',
  status: '状态',
  message: '说明',
}

/**
 * 将工具摘要字段名转换为成员可读标签.
 * @param key 工具摘要字段名
 * @return 中文标签或分词后的字段名
 */
const formatDetailLabel = (key: string): string =>
  detailLabelMap[key] ?? key.replace(/([a-z])([A-Z])/gu, '$1 $2')

/**
 * 将工具摘要值格式化为紧凑文本.
 * @param value 工具参数或结果值
 * @return 可读文本
 */
const formatDetailValue = (value: unknown): string => {
  if (Array.isArray(value)) {
    return value.map(String).join('、')
  }
  if (typeof value === 'object' && value !== null) {
    return JSON.stringify(value, null, 2)
  }
  return String(value ?? '')
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
 * 渲染工具调用的成员可见摘要.
 * @param props 工具执行记录
 * @return 可展开的工具活动
 */
export const AgentTraceToolItem = ({ execution }: AgentTraceToolItemProps) => {
  const isRunning = execution.status === 'RUNNING'
  const summaryEntryList = [
    ...Object.entries(execution.argumentsSummary),
    ...Object.entries(execution.resultSummary),
  ]
  return (
    <li className="grid grid-cols-[28px_minmax(0,1fr)] gap-3">
      <span className="bg-muted text-muted-foreground flex size-7 items-center justify-center rounded-full">
        {isRunning ? (
          <LoaderCircle aria-hidden className="size-4 animate-spin" />
        ) : execution.status === 'FAILED' ? (
          <CircleAlert aria-hidden className="text-destructive size-4" />
        ) : (
          <Wrench aria-hidden className="size-4" />
        )}
      </span>
      <div className="min-w-0">
        <div className="flex items-start gap-3">
          <div className="min-w-0 flex-1">
            <h5 className="truncate text-sm font-medium">
              {execution.displayName || execution.toolName}
            </h5>
            {execution.errorMessage ? (
              <p className="text-destructive mt-1 text-sm">
                {execution.errorMessage}
              </p>
            ) : null}
          </div>
          <span className="text-muted-foreground shrink-0 text-xs tabular-nums">
            {formatDuration(execution.durationMs) ||
              (isRunning ? '进行中' : '')}
          </span>
        </div>
        {summaryEntryList.length > 0 ? (
          <details className="group/tool mt-2">
            <summary className="text-muted-foreground hover:text-foreground inline-flex cursor-pointer list-none items-center gap-1 py-1 text-xs [&::-webkit-details-marker]:hidden">
              查看调用详情
              <ChevronDown className="size-3.5 transition-transform group-open/tool:rotate-180" />
            </summary>
            <dl className="border-border/70 bg-muted/30 mt-2 grid gap-3 rounded-lg border p-3 text-xs">
              {summaryEntryList.map(([key, value], index) => (
                <div
                  key={`${key}:${index}`}
                  className="grid grid-cols-[88px_minmax(0,1fr)] gap-3"
                >
                  <dt className="text-muted-foreground">
                    {formatDetailLabel(key)}
                  </dt>
                  <dd className="min-w-0 break-words whitespace-pre-wrap">
                    {formatDetailValue(value)}
                  </dd>
                </div>
              ))}
            </dl>
          </details>
        ) : null}
      </div>
    </li>
  )
}
