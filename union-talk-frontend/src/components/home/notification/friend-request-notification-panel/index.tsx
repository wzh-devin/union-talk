import { Check, Clock3, MessageCircle, X } from 'lucide-react'
import { useRef } from 'react'

import { AnimatedBeam } from '@/components/magic-ui/animated-beam'
import { Avatar, AvatarFallback } from '@/components/shadcn-ui/avatar'
import { Button } from '@/components/shadcn-ui/button'
import { ScrollArea } from '@/components/shadcn-ui/scroll-area'
import { PanelHeader } from '@/components/home/shared/panel-header'
import type { NotificationItem } from '@/pages/home/model/types'
import { getInitials } from '@/utils/avatar'

interface FriendRequestNotificationPanelProps {
  notification: NotificationItem
  isDetailsOpen: boolean
  processingFriendRequestId: string
  onBackToList: () => void
  onToggleDetails: () => void
  onOpenDetailsSheet: () => void
  onHandleFriendRequest: (
    notificationId: string,
    accept: boolean,
  ) => void | Promise<unknown>
}

/**
 * 渲染好友申请详情与服务端处理动作.
 * @param props 好友申请属性
 * @return 好友申请内容
 */
export const FriendRequestNotificationPanel = ({
  notification,
  isDetailsOpen,
  processingFriendRequestId,
  onBackToList,
  onToggleDetails,
  onOpenDetailsSheet,
  onHandleFriendRequest,
}: FriendRequestNotificationPanelProps) => {
  const isPending = notification.status === '待处理'
  const isAccepted = notification.status === '已同意'
  const isRejected = notification.status === '已拒绝'
  const isProcessing = processingFriendRequestId === notification.id
  const isActionDisabled = isProcessing || !notification.requestId
  const timelineRef = useRef<HTMLElement>(null)
  const requesterNodeRef = useRef<HTMLDivElement>(null)
  const messageNodeRef = useRef<HTMLDivElement>(null)
  const resultNodeRef = useRef<HTMLDivElement>(null)

  return (
    <>
      <PanelHeader
        title={notification.title}
        eyebrow="请求处理时间线"
        isDetailsOpen={isDetailsOpen}
        onBackToList={onBackToList}
        onToggleDetails={onToggleDetails}
        onOpenDetailsSheet={onOpenDetailsSheet}
      />
      <ScrollArea className="min-h-0 flex-1">
        <div className="w-full px-[clamp(16px,3vw,40px)] py-8 sm:py-10">
          <section
            ref={timelineRef}
            aria-label="好友申请处理时间线"
            className="relative w-full max-w-5xl"
          >
            <AnimatedBeam
              direction="vertical"
              containerRef={timelineRef}
              fromRef={requesterNodeRef}
              toRef={messageNodeRef}
              duration={0.65}
              repeat={0}
              pathColor="var(--border)"
              pathWidth={1.5}
              pathOpacity={0.9}
              gradientStartColor="#7c3aed"
              gradientStopColor="#71717a"
              className="z-0 motion-reduce:[&>path:nth-of-type(2)]:hidden"
            />

            {!isPending ? (
              <div data-testid="friend-request-resolution-beam">
                <AnimatedBeam
                  key={notification.status}
                  direction="vertical"
                  containerRef={timelineRef}
                  fromRef={messageNodeRef}
                  toRef={resultNodeRef}
                  duration={0.8}
                  repeat={0}
                  pathColor="var(--border)"
                  pathWidth={1.5}
                  pathOpacity={0.9}
                  gradientStartColor={isRejected ? '#fca5a5' : '#a7f3d0'}
                  gradientStopColor={isRejected ? '#dc2626' : '#047857'}
                  className="z-0 motion-reduce:[&>path:nth-of-type(2)]:hidden"
                />
              </div>
            ) : null}

            <div className="relative z-10 grid grid-cols-[136px_40px_minmax(0,1fr)] items-start gap-x-3 sm:grid-cols-[144px_48px_minmax(0,1fr)] sm:gap-x-5">
              <span aria-hidden="true" />
              <div ref={requesterNodeRef} className="relative z-10">
                <Avatar className="border-border size-10 border sm:size-12">
                  <AvatarFallback className="bg-violet-700 text-white">
                    {getInitials(notification.source)}
                  </AvatarFallback>
                </Avatar>
              </div>
              <div className="min-w-0 pt-0.5">
                <h2 className="text-foreground truncate text-base font-semibold tracking-normal sm:text-lg">
                  {notification.source}
                </h2>
                <p className="text-muted-foreground mt-1 text-xs sm:text-sm">
                  发起好友申请
                </p>
              </div>
            </div>

            <div
              data-testid="friend-request-message-row"
              className="relative z-10 mt-9 grid grid-cols-[136px_40px_minmax(0,1fr)] items-start gap-x-3 sm:mt-10 sm:grid-cols-[144px_48px_minmax(0,1fr)] sm:gap-x-5"
            >
              <time
                data-testid="timeline-time"
                className="text-muted-foreground pt-3 text-right text-[11px] leading-4 whitespace-nowrap tabular-nums sm:text-xs"
              >
                {notification.time}
              </time>
              <div
                ref={messageNodeRef}
                data-testid="timeline-node"
                className="border-border bg-background text-foreground relative z-10 flex size-10 items-center justify-center rounded-full border sm:size-12"
              >
                <MessageCircle className="size-4 sm:size-5" />
              </div>
              <div data-testid="timeline-content" className="min-w-0 pt-2.5">
                <h3 className="text-foreground text-sm font-semibold sm:text-base">
                  申请消息
                </h3>
                <p className="text-foreground mt-2 text-sm leading-6">
                  {notification.body || notification.summary}
                </p>
              </div>
            </div>

            <div
              data-testid="friend-request-result-row"
              className="relative z-10 mt-9 grid grid-cols-[136px_40px_minmax(0,1fr)] items-start gap-x-3 sm:mt-10 sm:grid-cols-[144px_48px_minmax(0,1fr)] sm:gap-x-5"
            >
              <time
                data-testid="timeline-time"
                className="text-muted-foreground pt-3 text-right text-[11px] leading-4 whitespace-nowrap tabular-nums sm:text-xs"
              >
                {notification.time}
              </time>
              <div
                ref={resultNodeRef}
                data-testid="friend-request-result-node"
                data-state={isPending ? 'pending' : 'resolved'}
                className={`bg-background relative z-10 flex size-10 items-center justify-center rounded-full border transition-[border-color,color,box-shadow,transform] duration-200 ease-[cubic-bezier(0.25,1,0.5,1)] motion-reduce:transition-none sm:size-12 ${
                  isAccepted
                    ? 'border-emerald-200 text-emerald-700 shadow-sm dark:border-emerald-900 dark:text-emerald-400'
                    : isRejected
                      ? 'border-destructive/30 text-destructive'
                      : 'border-border text-muted-foreground'
                }`}
              >
                {isAccepted ? (
                  <Check className="size-5" />
                ) : isRejected ? (
                  <X className="size-5" />
                ) : (
                  <Clock3 className="size-5" />
                )}
              </div>
              <div data-testid="timeline-content" className="min-w-0 pt-2.5">
                <h3 className="text-foreground text-sm font-semibold sm:text-base">
                  {notification.status}
                </h3>
                <p className="text-muted-foreground mt-2 text-sm leading-6">
                  {notification.actionHint}
                </p>

                {isPending ? (
                  <div className="mt-5 flex flex-wrap gap-2">
                    <Button
                      type="button"
                      aria-label="同意"
                      className="min-w-24"
                      disabled={isActionDisabled}
                      onClick={() =>
                        void onHandleFriendRequest(notification.id, true)
                      }
                    >
                      <Check />
                      同意
                    </Button>
                    <Button
                      type="button"
                      variant="destructive"
                      aria-label="拒绝"
                      className="min-w-24"
                      disabled={isActionDisabled}
                      onClick={() =>
                        void onHandleFriendRequest(notification.id, false)
                      }
                    >
                      <X />
                      拒绝
                    </Button>
                  </div>
                ) : null}
              </div>
            </div>
          </section>
        </div>
      </ScrollArea>
    </>
  )
}
