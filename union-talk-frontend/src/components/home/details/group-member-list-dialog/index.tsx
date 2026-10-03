import { LoaderCircle, Search, UserMinus } from 'lucide-react'
import { useState, type ReactElement } from 'react'

import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogTitle,
  DialogTrigger,
} from '@/components/shadcn-ui/dialog'
import { Badge } from '@/components/shadcn-ui/badge'
import { Button } from '@/components/shadcn-ui/button'
import { Input } from '@/components/shadcn-ui/input'
import { ScrollArea } from '@/components/shadcn-ui/scroll-area'
import { NamedAvatar } from '@/components/home/shared/named-avatar'
import type { ProcessingGroupManagement } from '@/hooks/use-home-conversations'
import { MIN_GROUP_MEMBER_COUNT } from '@/pages/home/model/group-information'
import type { ConversationMember } from '@/pages/home/model/types'
import { getMemberAvatarAccent } from '@/utils/avatar'

interface GroupMemberListDialogProps {
  groupId?: string
  groupName: string
  memberList: ConversationMember[]
  currentUserId?: string
  canKickMembers?: boolean
  processingGroupManagement?: ProcessingGroupManagement | null
  onKickGroupMember?: (
    groupId: string,
    targetUserId: string,
  ) => Promise<boolean>
  trigger: ReactElement
}

/**
 * 展示支持搜索的完整群成员列表.
 * @param props 群成员弹窗属性
 * @return 群成员列表弹窗
 */
export const GroupMemberListDialog = ({
  groupId = '',
  groupName,
  memberList,
  currentUserId = '',
  canKickMembers = false,
  processingGroupManagement = null,
  onKickGroupMember,
  trigger,
}: GroupMemberListDialogProps) => {
  const [isOpen, setIsOpen] = useState(false)
  const [searchValue, setSearchValue] = useState('')
  const [confirmationMember, setConfirmationMember] =
    useState<ConversationMember | null>(null)
  const normalizedSearchValue = searchValue.trim().toLocaleLowerCase()
  const filteredMemberList = memberList.filter((member) =>
    member.name.toLocaleLowerCase().includes(normalizedSearchValue),
  )
  const memberIndexMap = new Map(
    memberList.map((member, index) => [member.id, index]),
  )
  const isGroupProcessing = processingGroupManagement?.groupId === groupId
  const isKickProcessing =
    isGroupProcessing && processingGroupManagement.action === 'kick'

  /**
   * 切换弹窗，并在关闭时清空成员搜索条件.
   * @param nextIsOpen 下一弹窗状态
   * @return void
   */
  const handleOpenChange = (nextIsOpen: boolean): void => {
    setIsOpen(nextIsOpen)
    if (!nextIsOpen) {
      setSearchValue('')
      setConfirmationMember(null)
    }
  }

  /**
   * 确认将选中成员移出群聊.
   * @return 移出流程
   */
  const handleKickConfirmation = async (): Promise<void> => {
    if (!confirmationMember || !onKickGroupMember) {
      return
    }
    const isSuccessful = await onKickGroupMember(groupId, confirmationMember.id)
    if (isSuccessful) {
      setConfirmationMember(null)
    }
  }

  return (
    <Dialog open={isOpen} onOpenChange={handleOpenChange}>
      <DialogTrigger asChild>{trigger}</DialogTrigger>
      <DialogContent
        closeLabel="关闭"
        className="flex max-h-[calc(100svh-2rem)] w-[calc(100vw-2rem)] max-w-md flex-col overflow-hidden rounded-xl p-0"
      >
        <div className="border-border border-b px-5 py-4 pr-12">
          <DialogTitle className="text-lg font-semibold">群成员</DialogTitle>
          <DialogDescription className="text-muted-foreground mt-1 text-sm">
            {groupName} · {memberList.length} 位成员
          </DialogDescription>
        </div>

        <div className="px-5 pt-4">
          <div className="relative">
            <Search
              aria-hidden="true"
              className="text-muted-foreground pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2"
            />
            <Input
              type="search"
              value={searchValue}
              aria-label="搜索群成员"
              placeholder="搜索成员"
              className="pl-9"
              onChange={(event) => setSearchValue(event.target.value)}
            />
          </div>
        </div>

        <ScrollArea
          className="h-[min(420px,55vh)]"
          data-testid="group-member-scroll-area"
        >
          {filteredMemberList.length > 0 ? (
            <ul className="space-y-1 p-3" aria-label={`${groupName}成员列表`}>
              {filteredMemberList.map((member) => {
                const memberIndex = memberIndexMap.get(member.id) ?? 0

                return (
                  <li
                    key={member.id}
                    className="flex items-center gap-3 rounded-lg px-2 py-2"
                  >
                    <NamedAvatar
                      name={member.name}
                      avatarUrl={member.avatarUrl}
                      className="border-border size-9 border"
                      fallbackClassName={getMemberAvatarAccent(memberIndex)}
                      decorative
                    />
                    <span className="text-foreground min-w-0 flex-1 truncate text-sm font-medium">
                      {member.name}
                    </span>
                    {member.role === 'owner' ? (
                      <Badge variant="secondary">群主</Badge>
                    ) : null}
                    {canKickMembers &&
                    onKickGroupMember &&
                    member.role !== 'owner' &&
                    member.id !== currentUserId ? (
                      <Button
                        type="button"
                        variant="ghost"
                        size="sm"
                        aria-label={`移出${member.name}`}
                        className="text-destructive hover:bg-destructive/5 hover:text-destructive h-7 px-2 text-xs"
                        disabled={
                          isGroupProcessing ||
                          memberList.length <= MIN_GROUP_MEMBER_COUNT
                        }
                        onClick={() => setConfirmationMember(member)}
                      >
                        {isKickProcessing &&
                        processingGroupManagement.targetUserId === member.id ? (
                          <LoaderCircle aria-hidden className="animate-spin" />
                        ) : (
                          <UserMinus aria-hidden />
                        )}
                        移出
                      </Button>
                    ) : null}
                  </li>
                )
              })}
            </ul>
          ) : (
            <div className="text-muted-foreground flex h-full items-center justify-center px-5 text-sm">
              未找到相关成员
            </div>
          )}
        </ScrollArea>
      </DialogContent>
      <Dialog
        open={confirmationMember !== null}
        onOpenChange={(nextIsOpen) => {
          if (!nextIsOpen) {
            setConfirmationMember(null)
          }
        }}
      >
        <DialogContent className="w-[min(380px,calc(100vw-32px))] p-5">
          <DialogTitle className="pr-8 text-base font-semibold">
            移出{confirmationMember?.name}？
          </DialogTitle>
          <DialogDescription className="text-muted-foreground mt-1 text-xs leading-5">
            移出后，该成员将无法继续参与群聊。历史消息不会被清除。
          </DialogDescription>
          <div className="mt-5 flex justify-end gap-2">
            <Button
              type="button"
              variant="outline"
              disabled={isKickProcessing}
              onClick={() => setConfirmationMember(null)}
            >
              取消
            </Button>
            <Button
              type="button"
              variant="destructive"
              disabled={isKickProcessing}
              onClick={() => void handleKickConfirmation()}
            >
              {isKickProcessing ? (
                <LoaderCircle aria-hidden className="animate-spin" />
              ) : null}
              移出群聊
            </Button>
          </div>
        </DialogContent>
      </Dialog>
    </Dialog>
  )
}
