import {
  Ban,
  Check,
  CircleAlert,
  CircleDashed,
  LoaderCircle,
  MessageSquareText,
  Save,
  Search,
  Sparkles,
  Wrench,
} from 'lucide-react'

import type { AgentTraceStep } from '@/pages/home/model/agent-run'

/**
 * 渲染步骤类型对应的轻量图标.
 * @param props Agent 执行步骤
 * @return 步骤图标
 */
export const TraceStepIcon = ({ step }: { step: AgentTraceStep }) => {
  const iconClassName = 'size-4 shrink-0 text-current'
  if (step.status === 'RUNNING') {
    return (
      <LoaderCircle aria-hidden className={`${iconClassName} animate-spin`} />
    )
  }
  if (step.status === 'FAILED') {
    return <CircleAlert aria-hidden className={iconClassName} />
  }
  if (step.status === 'CANCELLED') {
    return <Ban aria-hidden className={iconClassName} />
  }
  if (step.status === 'SKIPPED') {
    return <CircleDashed aria-hidden className={iconClassName} />
  }
  if (step.stepType === 'MODEL_DECISION') {
    return <Sparkles aria-hidden className={iconClassName} />
  }
  if (step.stepType === 'CONTEXT_LOAD') {
    return <MessageSquareText aria-hidden className={iconClassName} />
  }
  if (step.stepType === 'RETRIEVAL' || step.stepType === 'RERANK') {
    return <Search aria-hidden className={iconClassName} />
  }
  if (step.stepType === 'MODEL_GENERATION') {
    return <Sparkles aria-hidden className={iconClassName} />
  }
  if (step.stepType === 'MESSAGE_PERSIST') {
    return <Save aria-hidden className={iconClassName} />
  }
  if (step.stepType === 'TOOL_CALL') {
    return <Wrench aria-hidden className={iconClassName} />
  }
  return <Check aria-hidden className={iconClassName} />
}
