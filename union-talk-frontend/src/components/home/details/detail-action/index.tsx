import type { ReactElement } from 'react'

import { cn } from '@/utils/class-name'

interface DetailActionProps {
  icon: ReactElement
  label: string
  ariaLabel: string
  active?: boolean
  onClick: () => void
}

export const DetailAction = ({
  icon,
  label,
  ariaLabel,
  active = false,
  onClick,
}: DetailActionProps) => (
  <button
    type="button"
    aria-label={ariaLabel}
    aria-pressed={active}
    className={cn(
      'flex w-full items-center gap-2 rounded-lg px-3 py-2 text-left text-xs transition',
      active
        ? 'bg-primary text-primary-foreground hover:bg-primary/90'
        : 'bg-muted text-foreground hover:bg-muted/80',
    )}
    onClick={onClick}
  >
    {icon}
    {label}
  </button>
)
