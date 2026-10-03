import {
  BrainCircuit,
  ChevronDown,
  LoaderCircle,
  MessageSquareText,
} from 'lucide-react'

import type { AgentContentBlock } from '@/pages/home/model/agent-run'

interface AgentTraceContentBlockItemProps {
  block: AgentContentBlock
}

/**
 * 渲染 Thinking 或 Text 内容块详情.
 * @param props 内容块属性
 * @return 可展开的内容块活动
 */
export const AgentTraceContentBlockItem = ({
  block,
}: AgentTraceContentBlockItemProps) => {
  const isThinking = block.blockType === 'THINKING'
  return (
    <li className="grid grid-cols-[28px_minmax(0,1fr)] gap-3">
      <span className="bg-muted text-muted-foreground flex size-7 items-center justify-center rounded-full">
        {block.status === 'STREAMING' ? (
          <LoaderCircle aria-hidden className="size-4 animate-spin" />
        ) : isThinking ? (
          <BrainCircuit aria-hidden className="size-4" />
        ) : (
          <MessageSquareText aria-hidden className="size-4" />
        )}
      </span>
      <div className="min-w-0 pt-0.5">
        <h5 className="text-sm font-medium">
          {isThinking ? '思考过程' : '生成回复'}
        </h5>
        {block.content ? (
          <details className="group/block mt-1.5">
            <summary className="text-muted-foreground hover:text-foreground inline-flex cursor-pointer list-none items-center gap-1 py-1 text-xs [&::-webkit-details-marker]:hidden">
              查看内容
              <ChevronDown className="size-3.5 transition-transform group-open/block:rotate-180" />
            </summary>
            <p className="bg-muted/30 mt-2 max-h-64 overflow-y-auto rounded-lg px-3 py-2.5 text-sm leading-6 whitespace-pre-wrap">
              {block.content}
            </p>
          </details>
        ) : null}
      </div>
    </li>
  )
}
