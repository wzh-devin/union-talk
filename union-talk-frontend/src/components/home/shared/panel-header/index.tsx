import {
  ChevronLeft,
  Info,
  PanelRightClose,
  PanelRightOpen,
} from 'lucide-react'
import type { RefObject } from 'react'

import { Button } from '@/components/shadcn-ui/button'

interface PanelHeaderProps {
  title: string
  eyebrow: string
  isDetailsOpen: boolean
  backButtonRef?: RefObject<HTMLButtonElement | null>
  onBackToList: () => void
  onToggleDetails: () => void
  onOpenDetailsSheet: () => void
}

export const PanelHeader = ({
  title,
  eyebrow,
  isDetailsOpen,
  backButtonRef,
  onBackToList,
  onToggleDetails,
  onOpenDetailsSheet,
}: PanelHeaderProps) => (
  <header className="border-border flex h-14 shrink-0 items-center justify-between gap-3 border-b px-3 sm:px-4">
    <div className="flex min-w-0 items-center gap-2.5">
      <Button
        ref={backButtonRef}
        type="button"
        variant="ghost"
        size="icon-sm"
        aria-label="返回列表"
        className="text-muted-foreground hover:bg-muted hover:text-foreground sm:hidden"
        onClick={onBackToList}
      >
        <ChevronLeft />
      </Button>
      <div className="min-w-0">
        <h1 className="text-foreground truncate text-sm font-semibold tracking-normal sm:text-base">
          {title}
        </h1>
        <p className="text-muted-foreground truncate text-xs">{eyebrow}</p>
      </div>
    </div>
    <div className="flex items-center gap-1">
      <Button
        type="button"
        variant="ghost"
        size="icon-sm"
        aria-label="打开详情抽屉"
        className="text-muted-foreground hover:bg-muted hover:text-foreground xl:hidden"
        onClick={onOpenDetailsSheet}
      >
        <Info />
      </Button>
      <Button
        type="button"
        variant="ghost"
        size="icon-sm"
        aria-label={isDetailsOpen ? '隐藏详情栏' : '显示详情栏'}
        className="text-muted-foreground hover:bg-muted hover:text-foreground hidden xl:inline-flex"
        onClick={onToggleDetails}
      >
        {isDetailsOpen ? <PanelRightClose /> : <PanelRightOpen />}
      </Button>
    </div>
  </header>
)
