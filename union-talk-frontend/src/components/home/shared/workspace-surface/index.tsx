import type {
  KeyboardEventHandler,
  PointerEventHandler,
  ReactNode,
} from 'react'

import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetTitle,
} from '@/components/shadcn-ui/sheet'
import { cn } from '@/utils/class-name'
import { sectionMetaMap } from '@/pages/home/model/constants'
import type { Section } from '@/pages/home/model/types'

interface WorkspaceSurfaceProps {
  section: Exclude<Section, 'profile'>
  listContent: ReactNode
  mainContent: ReactNode
  detailsContent: ReactNode
  isMobileListOpen: boolean
  isDetailsOpen: boolean
  isDetailsSheetOpen: boolean
  detailsPanelWidth: number
  detailsPanelMaxWidth: number
  isResizingDetailsPanel: boolean
  onDetailsSheetOpenChange: (isOpen: boolean) => void
  onBeginDetailsPanelResize: PointerEventHandler<HTMLDivElement>
  onDetailsPanelResizeKeyDown: KeyboardEventHandler<HTMLDivElement>
  onResetDetailsPanelWidth: () => void
}

/**
 * 渲染会话和通知页面共用的列表、内容与详情三栏容器.
 * @param props 工作台三栏容器属性
 * @return 工作台三栏容器
 */
export const WorkspaceSurface = ({
  section,
  listContent,
  mainContent,
  detailsContent,
  isMobileListOpen,
  isDetailsOpen,
  isDetailsSheetOpen,
  detailsPanelWidth,
  detailsPanelMaxWidth,
  isResizingDetailsPanel,
  onDetailsSheetOpenChange,
  onBeginDetailsPanelResize,
  onDetailsPanelResizeKeyDown,
  onResetDetailsPanelWidth,
}: WorkspaceSurfaceProps) => (
  <div className="flex h-full min-w-0 overflow-hidden">
    <section
      className={cn(
        'border-border bg-muted/40 w-[min(280px,calc(100vw-56px))] shrink-0 border-r sm:flex sm:w-[280px]',
        isMobileListOpen ? 'flex' : 'hidden',
        'flex-col',
      )}
      aria-label={sectionMetaMap[section].listTitle}
    >
      {listContent}
    </section>

    <section
      className={cn(
        'bg-background min-w-0 flex-1 flex-col sm:flex',
        isMobileListOpen ? 'hidden' : 'flex',
      )}
      aria-label="主内容"
    >
      {mainContent}
    </section>

    {detailsContent ? (
      <aside
        role="complementary"
        aria-label="详情栏"
        aria-hidden={!isDetailsOpen}
        inert={!isDetailsOpen}
        className={cn(
          'bg-card hidden min-w-0 shrink-0 overflow-hidden border-l xl:flex',
          isResizingDetailsPanel
            ? 'transition-none select-none'
            : 'transition-[width,opacity,transform,border-color] duration-200 ease-[cubic-bezier(0.25,1,0.5,1)] motion-reduce:transition-none',
          isDetailsOpen
            ? 'border-border translate-x-0 opacity-100'
            : 'pointer-events-none translate-x-2 border-transparent opacity-0',
        )}
        style={{ width: isDetailsOpen ? `${detailsPanelWidth}px` : '0px' }}
      >
        <div
          data-testid="desktop-details-panel-content"
          className="relative flex h-full min-w-0 shrink-0"
          style={{ width: `${detailsPanelWidth}px` }}
        >
          <div
            role="separator"
            aria-label="调整详情栏宽度"
            aria-orientation="vertical"
            aria-valuemin={280}
            aria-valuemax={detailsPanelMaxWidth}
            aria-valuenow={detailsPanelWidth}
            tabIndex={0}
            className="focus-visible:bg-foreground/15 hover:bg-foreground/10 absolute inset-y-0 left-0 z-20 w-2 -translate-x-1 cursor-col-resize touch-none transition outline-none"
            onPointerDown={onBeginDetailsPanelResize}
            onKeyDown={onDetailsPanelResizeKeyDown}
            onDoubleClick={onResetDetailsPanelWidth}
          />
          <div
            data-testid="desktop-details-panel-body"
            className="flex h-full min-w-0 flex-1 [&>*]:w-full [&>*]:min-w-0"
          >
            {detailsContent}
          </div>
        </div>
      </aside>
    ) : null}

    <Sheet open={isDetailsSheetOpen} onOpenChange={onDetailsSheetOpenChange}>
      <SheetContent
        side="right"
        className="border-border bg-card text-card-foreground p-0"
      >
        <SheetTitle className="sr-only">详情栏</SheetTitle>
        <SheetDescription className="sr-only">
          当前选中对象的上下文信息
        </SheetDescription>
        {detailsContent}
      </SheetContent>
    </Sheet>
  </div>
)
