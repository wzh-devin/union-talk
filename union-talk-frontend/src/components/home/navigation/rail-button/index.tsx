import type { ReactElement } from 'react'

import { Button } from '@/components/shadcn-ui/button'
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from '@/components/shadcn-ui/tooltip'
import { cn } from '@/utils/class-name'

interface RailButtonProps {
  label: string
  active: boolean
  unreadCount: number
  children: ReactElement
  onClick: () => void
}

export const RailButton = ({
  label,
  active,
  unreadCount,
  children,
  onClick,
}: RailButtonProps) => (
  <Tooltip>
    <TooltipTrigger asChild>
      <Button
        type="button"
        variant="ghost"
        size="icon-lg"
        aria-label={label}
        aria-pressed={active}
        aria-current={active ? 'page' : undefined}
        className={cn(
          'text-muted-foreground hover:bg-muted hover:text-foreground relative rounded-xl',
          active &&
            'bg-muted-foreground/15 text-foreground hover:bg-muted-foreground/15 hover:text-foreground',
        )}
        onClick={onClick}
      >
        {children}
        {unreadCount > 0 ? (
          <span className="bg-primary text-primary-foreground ring-background absolute -top-0.5 -right-0.5 grid min-w-4 place-items-center rounded-full px-1 text-[10px] font-semibold ring-2">
            {unreadCount}
          </span>
        ) : null}
      </Button>
    </TooltipTrigger>
    <TooltipContent side="right">{label}</TooltipContent>
  </Tooltip>
)
