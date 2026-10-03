import { CheckCircle2, CircleAlert, LoaderCircle } from 'lucide-react'

import type { AgentRunStatus } from '@/pages/home/model/agent-run'
import { cn } from '@/utils/class-name'

interface AgentRunStatusBadgeProps {
  status: AgentRunStatus
}

const statusViewMap: Record<
  AgentRunStatus,
  { label: string; className: string }
> = {
  QUEUED: {
    label: '排队中',
    className: 'border-zinc-300 bg-zinc-50 text-zinc-600',
  },
  RUNNING: {
    label: '生成中',
    className: 'border-zinc-900 bg-zinc-950 text-white',
  },
  SUCCEEDED: {
    label: '已完成',
    className: 'border-emerald-200 bg-emerald-50 text-emerald-700',
  },
  FAILED: {
    label: '失败',
    className: 'border-red-200 bg-red-50 text-red-600',
  },
  CANCELLED: {
    label: '已取消',
    className: 'border-zinc-200 bg-zinc-100 text-zinc-500',
  },
}

/**
 * 渲染 Agent Run 状态徽标.
 * @param props Run 状态属性
 * @return 状态徽标
 */
export const AgentRunStatusBadge = ({ status }: AgentRunStatusBadgeProps) => {
  const normalizedStatus = status || 'QUEUED'
  const view = statusViewMap[normalizedStatus]
  return (
    <span
      className={cn(
        'inline-flex shrink-0 items-center gap-1 rounded-full border px-2 py-0.5 text-[11px] font-medium',
        view.className,
      )}
    >
      {normalizedStatus === 'RUNNING' ? (
        <LoaderCircle aria-hidden className="size-3 animate-spin" />
      ) : normalizedStatus === 'SUCCEEDED' ? (
        <CheckCircle2 aria-hidden className="size-3" />
      ) : normalizedStatus === 'FAILED' ? (
        <CircleAlert aria-hidden className="size-3" />
      ) : (
        <span aria-hidden className="size-1.5 rounded-full bg-current" />
      )}
      {view.label}
    </span>
  )
}
