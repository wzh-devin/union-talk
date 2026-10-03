import { Ban, ChevronRight, LoaderCircle, Pencil, Trash2 } from 'lucide-react'
import { useState, type FormEvent } from 'react'

import { Button } from '@/components/shadcn-ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogTitle,
} from '@/components/shadcn-ui/dialog'
import { Input } from '@/components/shadcn-ui/input'
import { Switch } from '@/components/shadcn-ui/switch'
import type { FriendRelationAction } from '@/hooks/use-home-conversations'

interface FriendRelationSettingsProps {
  friendName: string
  friendRemark: string
  targetUserId: string
  isBlocked: boolean
  processingAction?: FriendRelationAction
  onBlockFriend: (targetUserId: string) => Promise<boolean>
  onUnblockFriend: (targetUserId: string) => Promise<boolean>
  onDeleteFriend: (targetUserId: string) => Promise<boolean>
  onUpdateFriendRemark: (
    targetUserId: string,
    remark: string,
  ) => Promise<boolean>
}

type ConfirmationAction = Extract<FriendRelationAction, 'block' | 'delete'>

/**
 * 渲染好友关系设置与危险操作确认.
 * @param props 好友关系设置属性
 * @return 好友关系设置
 */
export const FriendRelationSettings = ({
  friendName,
  friendRemark,
  targetUserId,
  isBlocked,
  processingAction,
  onBlockFriend,
  onUnblockFriend,
  onDeleteFriend,
  onUpdateFriendRemark,
}: FriendRelationSettingsProps) => {
  const [confirmationAction, setConfirmationAction] =
    useState<ConfirmationAction | null>(null)
  const [isRemarkDialogOpen, setIsRemarkDialogOpen] = useState(false)
  const [remarkDraft, setRemarkDraft] = useState(friendRemark)
  const isProcessing = Boolean(processingAction)
  const isDeleteConfirmation = confirmationAction === 'delete'

  /**
   * 提交当前好友关系确认动作.
   * @return 操作完成
   */
  const confirmAction = async (): Promise<void> => {
    let isSuccessful = false
    if (confirmationAction === 'block') {
      isSuccessful = await onBlockFriend(targetUserId)
    } else if (confirmationAction === 'delete') {
      isSuccessful = await onDeleteFriend(targetUserId)
    }
    if (isSuccessful) {
      setConfirmationAction(null)
    }
  }

  /**
   * 切换好友备注编辑弹层并同步最新备注草稿.
   * @param isOpen 下一弹层状态
   * @return void
   */
  const handleRemarkDialogOpenChange = (isOpen: boolean): void => {
    if (isOpen) {
      setRemarkDraft(friendRemark)
    }
    setIsRemarkDialogOpen(isOpen)
  }

  /**
   * 提交好友备注，空内容用于清除当前备注.
   * @param event 表单提交事件
   * @return 保存流程
   */
  const handleRemarkSubmit = async (
    event: FormEvent<HTMLFormElement>,
  ): Promise<void> => {
    event.preventDefault()
    const isSuccessful = await onUpdateFriendRemark(
      targetUserId,
      remarkDraft.trim(),
    )
    if (isSuccessful) {
      setIsRemarkDialogOpen(false)
    }
  }

  return (
    <section aria-labelledby="friend-relation-settings-title">
      <p
        id="friend-relation-settings-title"
        className="text-muted-foreground mb-2 text-xs font-medium"
      >
        关系设置
      </p>
      <div className="space-y-1" data-testid="friend-relation-settings-list">
        <button
          type="button"
          aria-label="修改好友备注"
          className="hover:bg-muted/70 focus-visible:ring-ring -mx-2 flex min-h-11 w-[calc(100%+1rem)] items-center gap-2.5 rounded-lg px-2 py-1.5 text-left transition-colors outline-none focus-visible:ring-2 disabled:pointer-events-none disabled:opacity-50"
          disabled={isProcessing}
          onClick={() => handleRemarkDialogOpenChange(true)}
        >
          <Pencil
            aria-hidden
            className="text-muted-foreground size-3.5 shrink-0"
          />
          <span className="min-w-0 flex-1">
            <span className="text-foreground block text-xs leading-4 font-medium">
              好友备注
            </span>
            <span className="text-muted-foreground block truncate text-[11px] leading-4">
              {friendRemark || '未设置'}
            </span>
          </span>
          <ChevronRight
            aria-hidden
            className="text-muted-foreground size-3.5 shrink-0"
          />
        </button>
        <div
          className="-mx-2 flex min-h-10 items-center gap-2.5 rounded-lg px-2 py-1"
          data-testid="friend-block-row"
        >
          <Ban
            aria-hidden
            className="text-muted-foreground size-3.5 shrink-0"
          />
          <div className="min-w-0 flex-1">
            <p className="text-foreground text-xs leading-4 font-medium">
              拉黑好友
            </p>
            <p className="text-muted-foreground text-[11px] leading-4">
              {isBlocked ? '已停止接收对方消息' : '停止接收对方消息'}
            </p>
          </div>
          <Switch
            aria-label={`拉黑${friendName}`}
            checked={isBlocked}
            disabled={isProcessing}
            onCheckedChange={(isChecked) => {
              if (isChecked) {
                setConfirmationAction('block')
                return
              }
              void onUnblockFriend(targetUserId)
            }}
          />
        </div>
        <button
          type="button"
          className="text-destructive hover:bg-destructive/5 focus-visible:ring-destructive/20 -mx-2 flex min-h-9 w-[calc(100%+1rem)] items-center gap-2.5 rounded-lg px-2 py-2 text-left text-xs font-medium transition-colors outline-none focus-visible:ring-2 disabled:pointer-events-none disabled:opacity-50"
          disabled={isProcessing}
          onClick={() => setConfirmationAction('delete')}
        >
          <Trash2 aria-hidden className="size-3.5 shrink-0" />
          <span className="flex-1">删除好友</span>
          <ChevronRight aria-hidden className="size-3.5 opacity-60" />
        </button>
      </div>

      <Dialog
        open={isRemarkDialogOpen}
        onOpenChange={handleRemarkDialogOpenChange}
      >
        <DialogContent
          closeLabel="关闭修改好友备注"
          className="w-[min(360px,calc(100vw-32px))] p-5"
        >
          <DialogTitle className="pr-8 text-base font-semibold">
            修改好友备注
          </DialogTitle>
          <DialogDescription className="text-muted-foreground mt-1 text-xs leading-5">
            设置一个更容易识别的名字
          </DialogDescription>
          <form className="mt-4" noValidate onSubmit={handleRemarkSubmit}>
            <label className="grid gap-2 text-xs font-medium">
              好友备注
              <Input
                autoFocus
                aria-label="好友备注"
                value={remarkDraft}
                maxLength={64}
                placeholder={friendName}
                disabled={isProcessing}
                onChange={(event) => setRemarkDraft(event.target.value)}
              />
            </label>
            <p className="text-muted-foreground mt-1.5 text-right text-[11px] tabular-nums">
              {remarkDraft.length} / 64
            </p>
            <div className="mt-4 flex justify-end gap-2">
              <Button
                type="button"
                variant="outline"
                disabled={isProcessing}
                onClick={() => handleRemarkDialogOpenChange(false)}
              >
                取消
              </Button>
              <Button type="submit" disabled={isProcessing}>
                {processingAction === 'remark' ? (
                  <LoaderCircle aria-hidden className="animate-spin" />
                ) : null}
                保存
              </Button>
            </div>
          </form>
        </DialogContent>
      </Dialog>

      <Dialog
        open={confirmationAction !== null}
        onOpenChange={(isOpen) => {
          if (!isOpen) {
            setConfirmationAction(null)
          }
        }}
      >
        <DialogContent className="w-[min(360px,calc(100vw-32px))] p-5">
          <DialogTitle className="pr-8 text-base font-semibold">
            {isDeleteConfirmation
              ? `删除${friendName}？`
              : `拉黑${friendName}？`}
          </DialogTitle>
          <DialogDescription className="text-muted-foreground mt-1 text-xs leading-5">
            {isDeleteConfirmation
              ? '删除后，对方将从好友列表移除。历史消息不会被清除。'
              : '拉黑后将停止接收对方消息，你可以随时取消拉黑。'}
          </DialogDescription>
          <div className="mt-5 flex justify-end gap-2">
            <Button
              type="button"
              variant="outline"
              disabled={isProcessing}
              onClick={() => setConfirmationAction(null)}
            >
              取消
            </Button>
            <Button
              type="button"
              variant={isDeleteConfirmation ? 'destructive' : 'default'}
              disabled={isProcessing}
              onClick={() => void confirmAction()}
            >
              {processingAction === confirmationAction ? (
                <LoaderCircle aria-hidden className="animate-spin" />
              ) : null}
              {isDeleteConfirmation ? '删除好友' : '确认拉黑'}
            </Button>
          </div>
        </DialogContent>
      </Dialog>
    </section>
  )
}
