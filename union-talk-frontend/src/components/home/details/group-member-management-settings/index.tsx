import {
  ChevronRight,
  LoaderCircle,
  LogOut,
  Search,
  Trash2,
  UserPlus,
} from 'lucide-react'
import { useState, type FormEvent } from 'react'

import { Button } from '@/components/shadcn-ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogTitle,
} from '@/components/shadcn-ui/dialog'
import { Input } from '@/components/shadcn-ui/input'
import { ScrollArea } from '@/components/shadcn-ui/scroll-area'
import type { ProcessingGroupManagement } from '@/hooks/use-home-conversations'
import {
  getGroupInviteError,
  getInvitableFriendList,
  getRemainingGroupCapacity,
} from '@/pages/home/model/group-member-management'
import { MIN_GROUP_MEMBER_COUNT } from '@/pages/home/model/group-information'
import type { ConversationMember, FriendOption } from '@/pages/home/model/types'

interface GroupMemberManagementSettingsProps {
  groupId: string
  groupName: string
  memberList: ConversationMember[]
  memberLimit: number
  friendOptionList: FriendOption[]
  isOwner: boolean
  processingGroupManagement: ProcessingGroupManagement | null
  onInviteGroupMembers: (
    groupId: string,
    userIdList: string[],
  ) => Promise<boolean>
  onLeaveGroup: (groupId: string) => Promise<boolean>
  onDissolveGroup: (groupId: string) => Promise<boolean>
}

type ConfirmationAction = 'leave' | 'dissolve'

/**
 * 渲染群聊邀请、退出与解散操作.
 * @param props 群成员管理设置属性
 * @return 群成员管理设置
 */
export const GroupMemberManagementSettings = ({
  groupId,
  groupName,
  memberList,
  memberLimit,
  friendOptionList,
  isOwner,
  processingGroupManagement,
  onInviteGroupMembers,
  onLeaveGroup,
  onDissolveGroup,
}: GroupMemberManagementSettingsProps) => {
  const [isInviteOpen, setIsInviteOpen] = useState(false)
  const [searchValue, setSearchValue] = useState('')
  const [selectedUserIdSet, setSelectedUserIdSet] = useState<Set<string>>(
    new Set(),
  )
  const [inviteError, setInviteError] = useState('')
  const [confirmationAction, setConfirmationAction] =
    useState<ConfirmationAction | null>(null)
  const invitableFriendList = getInvitableFriendList(
    friendOptionList,
    memberList,
  )
  const remainingCapacity = getRemainingGroupCapacity(
    memberList.length,
    memberLimit,
  )
  const normalizedSearchValue = searchValue.trim().toLocaleLowerCase()
  const filteredFriendList = invitableFriendList.filter((friend) =>
    friend.name.toLocaleLowerCase().includes(normalizedSearchValue),
  )
  const isProcessing = processingGroupManagement?.groupId === groupId
  const isInviteProcessing =
    isProcessing && processingGroupManagement.action === 'invite'
  const isConfirmationProcessing =
    isProcessing && processingGroupManagement.action === confirmationAction
  const cannotLeave = memberList.length <= MIN_GROUP_MEMBER_COUNT

  /**
   * 切换邀请弹窗并重置临时选择.
   * @param nextIsOpen 下一弹窗状态
   * @return void
   */
  const handleInviteOpenChange = (nextIsOpen: boolean): void => {
    setIsInviteOpen(nextIsOpen)
    if (!nextIsOpen) {
      setSearchValue('')
      setSelectedUserIdSet(new Set())
      setInviteError('')
    }
  }

  /**
   * 切换好友邀请选择.
   * @param userId 好友用户 ID
   * @param isSelected 是否选择
   * @return void
   */
  const handleSelectionChange = (userId: string, isSelected: boolean): void => {
    setSelectedUserIdSet((currentSet) => {
      const nextSet = new Set(currentSet)
      if (isSelected) {
        nextSet.add(userId)
      } else {
        nextSet.delete(userId)
      }
      return nextSet
    })
    setInviteError('')
  }

  /**
   * 提交邀请成员请求.
   * @param event 表单提交事件
   * @return 邀请流程
   */
  const handleInviteSubmit = async (
    event: FormEvent<HTMLFormElement>,
  ): Promise<void> => {
    event.preventDefault()
    const selectedUserIdList = Array.from(selectedUserIdSet)
    const nextError = getGroupInviteError(selectedUserIdList, remainingCapacity)
    if (nextError) {
      setInviteError(nextError)
      return
    }
    const isSuccessful = await onInviteGroupMembers(groupId, selectedUserIdList)
    if (isSuccessful) {
      handleInviteOpenChange(false)
    }
  }

  /**
   * 提交退出或解散群聊操作.
   * @return 操作流程
   */
  const handleConfirmation = async (): Promise<void> => {
    const isSuccessful =
      confirmationAction === 'dissolve'
        ? await onDissolveGroup(groupId)
        : await onLeaveGroup(groupId)
    if (isSuccessful) {
      setConfirmationAction(null)
    }
  }

  return (
    <>
      <button
        type="button"
        aria-label="邀请新成员"
        className="hover:bg-muted/70 focus-visible:ring-ring -mx-2 flex min-h-11 w-[calc(100%+1rem)] items-center gap-2.5 rounded-lg px-2 py-1.5 text-left transition-colors outline-none focus-visible:ring-2 disabled:pointer-events-none disabled:opacity-50"
        disabled={isProcessing || remainingCapacity === 0}
        onClick={() => setIsInviteOpen(true)}
      >
        <UserPlus
          aria-hidden
          className="text-muted-foreground size-3.5 shrink-0"
        />
        <span className="min-w-0 flex-1">
          <span className="text-foreground block text-xs leading-4 font-medium">
            邀请新成员
          </span>
          <span className="text-muted-foreground block truncate text-[11px] leading-4">
            {remainingCapacity === 0
              ? '群聊人数已达上限'
              : `还可邀请 ${remainingCapacity} 人`}
          </span>
        </span>
        <ChevronRight
          aria-hidden
          className="text-muted-foreground size-3.5 shrink-0"
        />
      </button>

      <button
        type="button"
        aria-label={isOwner ? '解散群聊' : '退出群聊'}
        className="text-destructive hover:bg-destructive/5 focus-visible:ring-destructive/20 -mx-2 flex min-h-9 w-[calc(100%+1rem)] items-center gap-2.5 rounded-lg px-2 py-2 text-left text-xs font-medium transition-colors outline-none focus-visible:ring-2 disabled:pointer-events-none disabled:opacity-50"
        disabled={isProcessing || (!isOwner && cannotLeave)}
        onClick={() => setConfirmationAction(isOwner ? 'dissolve' : 'leave')}
      >
        {isOwner ? (
          <Trash2 aria-hidden className="size-3.5 shrink-0" />
        ) : (
          <LogOut aria-hidden className="size-3.5 shrink-0" />
        )}
        <span className="min-w-0 flex-1">
          <span className="block">{isOwner ? '解散群聊' : '退出群聊'}</span>
          {!isOwner && cannotLeave ? (
            <span className="text-muted-foreground block text-[11px] font-normal">
              群聊成员不能少于 3 人
            </span>
          ) : null}
        </span>
        <ChevronRight aria-hidden className="size-3.5 opacity-60" />
      </button>

      <Dialog open={isInviteOpen} onOpenChange={handleInviteOpenChange}>
        <DialogContent className="flex max-h-[calc(100svh-2rem)] w-[calc(100vw-2rem)] max-w-md flex-col overflow-hidden rounded-xl p-0">
          <div className="border-border border-b px-5 py-4 pr-12">
            <DialogTitle className="text-base font-semibold">
              邀请新成员
            </DialogTitle>
            <DialogDescription className="text-muted-foreground mt-1 text-xs">
              当前 {memberList.length} / {memberLimit} 人，还可邀请{' '}
              {remainingCapacity} 人
            </DialogDescription>
          </div>
          <form
            className="flex min-h-0 flex-1 flex-col"
            noValidate
            onSubmit={handleInviteSubmit}
          >
            <div className="px-5 pt-4">
              <div className="relative">
                <Search
                  aria-hidden
                  className="text-muted-foreground pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2"
                />
                <Input
                  type="search"
                  value={searchValue}
                  aria-label="搜索可邀请好友"
                  placeholder="搜索好友"
                  className="pl-9"
                  disabled={isInviteProcessing}
                  onChange={(event) => setSearchValue(event.target.value)}
                />
              </div>
            </div>
            <ScrollArea className="h-[min(360px,48vh)]">
              {filteredFriendList.length > 0 ? (
                <ul className="space-y-1 p-3" aria-label="可邀请好友列表">
                  {filteredFriendList.map((friend) => {
                    const isSelected = selectedUserIdSet.has(friend.id)
                    const isSelectionDisabled =
                      !isSelected && selectedUserIdSet.size >= remainingCapacity

                    return (
                      <li key={friend.id}>
                        <label className="hover:bg-muted/70 flex cursor-pointer items-center gap-3 rounded-lg px-2 py-2">
                          <input
                            type="checkbox"
                            checked={isSelected}
                            aria-label={`选择${friend.name}`}
                            className="accent-foreground size-4 shrink-0"
                            disabled={isInviteProcessing || isSelectionDisabled}
                            onChange={(event) =>
                              handleSelectionChange(
                                friend.id,
                                event.target.checked,
                              )
                            }
                          />
                          <span className="text-foreground min-w-0 flex-1 truncate text-sm font-medium">
                            {friend.name}
                          </span>
                        </label>
                      </li>
                    )
                  })}
                </ul>
              ) : (
                <div className="text-muted-foreground flex h-full min-h-32 items-center justify-center px-5 text-sm">
                  没有可邀请的好友
                </div>
              )}
            </ScrollArea>
            <div className="border-border border-t px-5 py-4">
              {inviteError ? (
                <p role="alert" className="text-destructive mb-3 text-xs">
                  {inviteError}
                </p>
              ) : null}
              <div className="flex items-center justify-between gap-3">
                <span className="text-muted-foreground text-xs">
                  已选择 {selectedUserIdSet.size} 人
                </span>
                <div className="flex gap-2">
                  <Button
                    type="button"
                    variant="outline"
                    disabled={isInviteProcessing}
                    onClick={() => handleInviteOpenChange(false)}
                  >
                    取消
                  </Button>
                  <Button type="submit" disabled={isInviteProcessing}>
                    {isInviteProcessing ? (
                      <LoaderCircle aria-hidden className="animate-spin" />
                    ) : null}
                    邀请 {selectedUserIdSet.size} 人
                  </Button>
                </div>
              </div>
            </div>
          </form>
        </DialogContent>
      </Dialog>

      <Dialog
        open={confirmationAction !== null}
        onOpenChange={(nextIsOpen) => {
          if (!nextIsOpen) {
            setConfirmationAction(null)
          }
        }}
      >
        <DialogContent className="w-[min(380px,calc(100vw-32px))] p-5">
          <DialogTitle className="pr-8 text-base font-semibold">
            {confirmationAction === 'dissolve'
              ? `解散${groupName}？`
              : '退出群聊？'}
          </DialogTitle>
          <DialogDescription className="text-muted-foreground mt-1 text-xs leading-5">
            {confirmationAction === 'dissolve'
              ? '解散后，所有成员都将无法继续使用该群聊。'
              : '退出后，该群聊将从当前列表移除。'}
          </DialogDescription>
          <div className="mt-5 flex justify-end gap-2">
            <Button
              type="button"
              variant="outline"
              disabled={isConfirmationProcessing}
              onClick={() => setConfirmationAction(null)}
            >
              取消
            </Button>
            <Button
              type="button"
              variant="destructive"
              disabled={isConfirmationProcessing}
              onClick={() => void handleConfirmation()}
            >
              {isConfirmationProcessing ? (
                <LoaderCircle aria-hidden className="animate-spin" />
              ) : null}
              {confirmationAction === 'dissolve' ? '确认解散' : '确认退出'}
            </Button>
          </div>
        </DialogContent>
      </Dialog>
    </>
  )
}
