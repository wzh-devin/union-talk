import { ChevronRight, LoaderCircle, Pencil } from 'lucide-react'
import { useState, type FormEvent } from 'react'

import { Button } from '@/components/shadcn-ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogTitle,
} from '@/components/shadcn-ui/dialog'
import { Input } from '@/components/shadcn-ui/input'
import { Textarea } from '@/components/shadcn-ui/textarea'
import { GroupMemberManagementSettings } from '@/components/home/details/group-member-management-settings'
import type { ProcessingGroupManagement } from '@/hooks/use-home-conversations'
import {
  getGroupInformationFormError,
  MAX_GROUP_DESCRIPTION_LENGTH,
  MAX_GROUP_MEMBER_COUNT,
  MAX_GROUP_NAME_LENGTH,
  MIN_GROUP_MEMBER_COUNT,
  type GroupInformationFormInput,
} from '@/pages/home/model/group-information'
import type { ConversationMember, FriendOption } from '@/pages/home/model/types'

interface GroupInformationSettingsProps {
  groupId: string
  groupName: string
  description: string
  memberLimit: number
  currentMemberCount: number
  isSaving: boolean
  canEdit?: boolean
  memberList?: ConversationMember[]
  friendOptionList?: FriendOption[]
  processingGroupManagement?: ProcessingGroupManagement | null
  onUpdateGroupInformation: (
    groupId: string,
    input: GroupInformationFormInput,
  ) => Promise<boolean>
  onInviteGroupMembers?: (
    groupId: string,
    userIdList: string[],
  ) => Promise<boolean>
  onLeaveGroup?: (groupId: string) => Promise<boolean>
  onDissolveGroup?: (groupId: string) => Promise<boolean>
}

/**
 * 渲染群主可用的群聊信息编辑入口与弹层.
 * @param props 群聊信息设置属性
 * @return 群聊信息设置
 */
export const GroupInformationSettings = ({
  groupId,
  groupName,
  description,
  memberLimit,
  currentMemberCount,
  isSaving,
  canEdit = true,
  memberList = [],
  friendOptionList = [],
  processingGroupManagement = null,
  onUpdateGroupInformation,
  onInviteGroupMembers,
  onLeaveGroup,
  onDissolveGroup,
}: GroupInformationSettingsProps) => {
  const [isOpen, setIsOpen] = useState(false)
  const [nameDraft, setNameDraft] = useState(groupName)
  const [descriptionDraft, setDescriptionDraft] = useState(description)
  const [memberLimitDraft, setMemberLimitDraft] = useState(String(memberLimit))
  const [errorMessage, setErrorMessage] = useState('')
  const minimumMemberLimit = Math.max(
    MIN_GROUP_MEMBER_COUNT,
    currentMemberCount,
  )

  /**
   * 切换群聊信息弹层并从当前群聊重新初始化草稿.
   * @param nextIsOpen 下一弹层状态
   * @return void
   */
  const handleOpenChange = (nextIsOpen: boolean): void => {
    if (nextIsOpen) {
      setNameDraft(groupName)
      setDescriptionDraft(description === '暂无群聊描述' ? '' : description)
      setMemberLimitDraft(String(memberLimit))
      setErrorMessage('')
    }
    setIsOpen(nextIsOpen)
  }

  /**
   * 校验并提交群聊信息.
   * @param event 表单提交事件
   * @return 保存流程
   */
  const handleSubmit = async (
    event: FormEvent<HTMLFormElement>,
  ): Promise<void> => {
    event.preventDefault()
    const input: GroupInformationFormInput = {
      name: nameDraft.trim(),
      description: descriptionDraft.trim(),
      memberLimit:
        memberLimitDraft.trim() === '' ? Number.NaN : Number(memberLimitDraft),
    }
    const nextErrorMessage = getGroupInformationFormError(
      input,
      currentMemberCount,
    )

    if (nextErrorMessage) {
      setErrorMessage(nextErrorMessage)
      return
    }

    const isSuccessful = await onUpdateGroupInformation(groupId, input)
    if (isSuccessful) {
      setIsOpen(false)
    }
  }

  return (
    <section aria-labelledby="group-information-settings-title">
      <p
        id="group-information-settings-title"
        className="text-muted-foreground mb-2 text-xs font-medium"
      >
        群聊设置
      </p>
      <div className="space-y-1">
        {canEdit ? (
          <button
            type="button"
            aria-label="编辑群聊信息"
            className="hover:bg-muted/70 focus-visible:ring-ring -mx-2 flex min-h-11 w-[calc(100%+1rem)] items-center gap-2.5 rounded-lg px-2 py-1.5 text-left transition-colors outline-none focus-visible:ring-2 disabled:pointer-events-none disabled:opacity-50"
            disabled={isSaving}
            onClick={() => handleOpenChange(true)}
          >
            <Pencil
              aria-hidden
              className="text-muted-foreground size-3.5 shrink-0"
            />
            <span className="min-w-0 flex-1">
              <span className="text-foreground block text-xs leading-4 font-medium">
                编辑群聊信息
              </span>
              <span className="text-muted-foreground block truncate text-[11px] leading-4">
                名称、描述与人数上限
              </span>
            </span>
            <ChevronRight
              aria-hidden
              className="text-muted-foreground size-3.5 shrink-0"
            />
          </button>
        ) : null}
        {onInviteGroupMembers && onLeaveGroup && onDissolveGroup ? (
          <GroupMemberManagementSettings
            groupId={groupId}
            groupName={groupName}
            memberList={memberList}
            memberLimit={memberLimit}
            friendOptionList={friendOptionList}
            isOwner={canEdit}
            processingGroupManagement={processingGroupManagement}
            onInviteGroupMembers={onInviteGroupMembers}
            onLeaveGroup={onLeaveGroup}
            onDissolveGroup={onDissolveGroup}
          />
        ) : null}
      </div>

      <Dialog open={isOpen} onOpenChange={handleOpenChange}>
        <DialogContent
          closeLabel="关闭编辑群聊信息"
          className="w-[min(420px,calc(100vw-32px))] overflow-hidden rounded-xl p-0"
        >
          <div className="border-border border-b px-5 py-4 pr-12">
            <DialogTitle className="text-base font-semibold">
              编辑群聊信息
            </DialogTitle>
            <DialogDescription className="text-muted-foreground mt-1 text-xs leading-5">
              更新群聊名称、描述和可容纳的成员人数
            </DialogDescription>
          </div>
          <form className="space-y-4 p-5" noValidate onSubmit={handleSubmit}>
            <label className="grid gap-2 text-xs font-medium">
              群聊名称
              <Input
                value={nameDraft}
                aria-label="群聊名称"
                aria-invalid={errorMessage.includes('群聊名称')}
                maxLength={MAX_GROUP_NAME_LENGTH}
                disabled={isSaving}
                onChange={(event) => {
                  setNameDraft(event.target.value)
                  setErrorMessage('')
                }}
              />
            </label>
            <label className="grid gap-2 text-xs font-medium">
              群聊描述
              <Textarea
                value={descriptionDraft}
                aria-label="群聊描述"
                aria-invalid={errorMessage.includes('群聊描述')}
                maxLength={MAX_GROUP_DESCRIPTION_LENGTH}
                className="min-h-20"
                placeholder="暂无群聊描述"
                disabled={isSaving}
                onChange={(event) => {
                  setDescriptionDraft(event.target.value)
                  setErrorMessage('')
                }}
              />
            </label>
            <label className="grid gap-2 text-xs font-medium">
              人数上限
              <Input
                type="number"
                value={memberLimitDraft}
                aria-label="人数上限"
                aria-invalid={errorMessage.includes('人数上限')}
                min={minimumMemberLimit}
                max={MAX_GROUP_MEMBER_COUNT}
                step={1}
                disabled={isSaving}
                onChange={(event) => {
                  setMemberLimitDraft(event.target.value)
                  setErrorMessage('')
                }}
              />
              <span className="text-muted-foreground text-[11px] font-normal">
                当前 {currentMemberCount} 人，可设置为 {minimumMemberLimit}～100
                人
              </span>
            </label>
            {errorMessage ? (
              <p role="alert" className="text-destructive text-xs">
                {errorMessage}
              </p>
            ) : null}
            <div className="flex justify-end gap-2 pt-1">
              <Button
                type="button"
                variant="outline"
                disabled={isSaving}
                onClick={() => handleOpenChange(false)}
              >
                取消
              </Button>
              <Button type="submit" disabled={isSaving}>
                {isSaving ? (
                  <LoaderCircle aria-hidden className="animate-spin" />
                ) : null}
                保存
              </Button>
            </div>
          </form>
        </DialogContent>
      </Dialog>
    </section>
  )
}
