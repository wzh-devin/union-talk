import type { ReactNode } from 'react'

import { Button } from '@/components/shadcn-ui/button'

interface AccountSettingRowProps {
  label: string
  value: ReactNode
  actionLabel: string
  actionText?: string
  onAction: () => void
}

export const AccountSettingRow = ({
  label,
  value,
  actionLabel,
  actionText = '编辑',
  onAction,
}: AccountSettingRowProps) => (
  <div
    role="group"
    aria-label={`${label}设置`}
    className="grid min-h-16 grid-cols-[minmax(0,1fr)_auto] items-center gap-x-4 gap-y-1 py-3 sm:grid-cols-[132px_minmax(0,1fr)_88px]"
  >
    <p className="text-foreground text-sm font-medium">{label}</p>
    <div className="text-muted-foreground col-start-1 row-start-2 min-w-0 text-sm break-words sm:col-start-2 sm:row-start-1 sm:text-right">
      {value}
    </div>
    <Button
      type="button"
      variant="outline"
      size="lg"
      aria-label={actionLabel}
      className="col-start-2 row-span-2 row-start-1 min-w-20 justify-self-end sm:col-start-3 sm:row-span-1"
      onClick={onAction}
    >
      {actionText}
    </Button>
  </div>
)
