import { useRef, useState, type FormEvent, type RefObject } from 'react'

import { Avatar, AvatarFallback } from '@/components/shadcn-ui/avatar'
import { Button } from '@/components/shadcn-ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogTitle,
} from '@/components/shadcn-ui/dialog'
import { Input } from '@/components/shadcn-ui/input'
import { ScrollArea } from '@/components/shadcn-ui/scroll-area'
import { Textarea } from '@/components/shadcn-ui/textarea'
import {
  getCreateGroupFormError,
  isGroupFriendSelectionDisabled,
} from '@/pages/home/model/conversation-create'
import type {
  CreateGroupFormInput,
  FriendOption,
} from '@/pages/home/model/types'
import { getInitials } from '@/utils/avatar'

interface CreateGroupDialogProps {
  open: boolean
  returnFocusRef: RefObject<HTMLButtonElement | null>
  successFocusRef: RefObject<HTMLButtonElement | null>
  friendConversationList: FriendOption[]
  onOpenChange: (isOpen: boolean) => void
  onSubmit: (input: CreateGroupFormInput) => void | Promise<void>
}

/**
 * 渲染并校验本地群聊创建弹窗.
 * @param props 创建群聊弹窗属性
 * @return 创建群聊弹窗
 */
export const CreateGroupDialog = ({
  open,
  returnFocusRef,
  successFocusRef,
  friendConversationList,
  onOpenChange,
  onSubmit,
}: CreateGroupDialogProps) => {
  const didSubmitSuccessfullyRef = useRef(false)
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [selectedFriendIdSet, setSelectedFriendIdSet] = useState<Set<string>>(
    new Set(),
  )
  const [errorMessage, setErrorMessage] = useState('')

  /**
   * 切换弹窗并在关闭时重置群聊表单.
   * @param nextIsOpen 下一弹窗状态
   * @return void
   */
  const handleOpenChange = (nextIsOpen: boolean): void => {
    if (nextIsOpen) {
      didSubmitSuccessfullyRef.current = false
    }
    if (!nextIsOpen) {
      setName('')
      setDescription('')
      setSelectedFriendIdSet(new Set())
      setErrorMessage('')
    }
    onOpenChange(nextIsOpen)
  }

  /**
   * 切换待邀请好友的选择状态.
   * @param friendId 好友会话 ID
   * @param isSelected 是否选中
   * @return void
   */
  const handleFriendSelectionChange = (
    friendId: string,
    isSelected: boolean,
  ): void => {
    setSelectedFriendIdSet((currentFriendIdSet) => {
      const nextFriendIdSet = new Set(currentFriendIdSet)

      if (isSelected) {
        nextFriendIdSet.add(friendId)
      } else {
        nextFriendIdSet.delete(friendId)
      }
      return nextFriendIdSet
    })
    setErrorMessage('')
  }

  /**
   * 校验并创建本地群聊.
   * @param event 表单提交事件
   * @return void
   */
  const handleSubmit = async (
    event: FormEvent<HTMLFormElement>,
  ): Promise<void> => {
    event.preventDefault()
    const input: CreateGroupFormInput = {
      name: name.trim(),
      description: description.trim(),
      friendIdList: [...selectedFriendIdSet],
    }
    const nextErrorMessage = getCreateGroupFormError(input)

    if (nextErrorMessage) {
      setErrorMessage(nextErrorMessage)
      return
    }

    try {
      await onSubmit(input)
      didSubmitSuccessfullyRef.current = true
      handleOpenChange(false)
    } catch {
      // 业务层已展示失败提示，保留用户输入供重试。
    }
  }

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent
        closeLabel="关闭创建群聊"
        className="flex max-h-[calc(100svh-32px)] w-[min(520px,calc(100vw-32px))] flex-col overflow-hidden rounded-xl p-0"
        onCloseAutoFocus={(event) => {
          event.preventDefault()
          const successFocusTarget = successFocusRef.current
          const isSuccessFocusTargetVisible =
            didSubmitSuccessfullyRef.current &&
            successFocusTarget !== null &&
            successFocusTarget.getClientRects().length > 0

          if (isSuccessFocusTargetVisible) {
            successFocusTarget.focus()
          } else {
            returnFocusRef.current?.focus()
          }
          didSubmitSuccessfullyRef.current = false
        }}
      >
        <div className="border-border border-b px-5 py-4 pr-12">
          <DialogTitle className="text-lg font-semibold">创建群聊</DialogTitle>
          <DialogDescription className="text-muted-foreground mt-1 text-sm">
            填写群聊信息，并选择要邀请的好友。
          </DialogDescription>
        </div>

        <form
          className="flex min-h-0 flex-1 flex-col gap-4 p-5"
          noValidate
          onSubmit={handleSubmit}
        >
          <label className="grid gap-2 text-sm font-medium">
            群聊名称
            <Input
              value={name}
              aria-label="群聊名称"
              aria-invalid={errorMessage.includes('群聊名称')}
              maxLength={30}
              placeholder="请输入群聊名称"
              onChange={(event) => {
                setName(event.target.value)
                setErrorMessage('')
              }}
            />
          </label>

          <label className="grid gap-2 text-sm font-medium">
            群聊简介
            <Textarea
              value={description}
              aria-label="群聊简介"
              aria-invalid={errorMessage.includes('群聊简介')}
              maxLength={200}
              className="min-h-20"
              placeholder="介绍群聊主题或用途"
              onChange={(event) => {
                setDescription(event.target.value)
                setErrorMessage('')
              }}
            />
          </label>

          <fieldset
            aria-invalid={errorMessage === '请至少选择两位好友'}
            className="grid min-h-0 gap-2"
          >
            <legend className="sr-only">选择好友</legend>
            <div className="flex items-center justify-between gap-3">
              <span aria-hidden="true" className="text-sm font-medium">
                选择好友
              </span>
              <span className="text-muted-foreground text-xs">
                已选择 {selectedFriendIdSet.size} / 99 人
              </span>
            </div>
            <p className="text-muted-foreground text-[11px]">
              最多选择 99 位好友，加上你共 100 人
            </p>
            <ScrollArea className="border-border h-48 rounded-lg border">
              <div className="space-y-1 p-2">
                {friendConversationList.map((conversation) => (
                  <label
                    key={conversation.id}
                    className="hover:bg-muted flex items-center gap-3 rounded-lg px-2 py-2 text-sm transition-colors"
                  >
                    <input
                      type="checkbox"
                      checked={selectedFriendIdSet.has(conversation.id)}
                      disabled={isGroupFriendSelectionDisabled(
                        selectedFriendIdSet.size,
                        selectedFriendIdSet.has(conversation.id),
                      )}
                      aria-label={`选择${conversation.name}`}
                      className="accent-foreground size-4 shrink-0 disabled:cursor-not-allowed disabled:opacity-50"
                      onChange={(event) =>
                        handleFriendSelectionChange(
                          conversation.id,
                          event.target.checked,
                        )
                      }
                    />
                    <Avatar className="border-border size-9 border">
                      <AvatarFallback className={conversation.accent}>
                        {getInitials(conversation.name)}
                      </AvatarFallback>
                    </Avatar>
                    <span className="text-foreground min-w-0 flex-1 truncate font-medium">
                      {conversation.name}
                    </span>
                  </label>
                ))}
              </div>
            </ScrollArea>
          </fieldset>

          {errorMessage ? (
            <p role="alert" className="text-destructive text-sm">
              {errorMessage}
            </p>
          ) : null}

          <div className="flex justify-end gap-2 pt-1">
            <Button
              type="button"
              variant="outline"
              onClick={() => handleOpenChange(false)}
            >
              取消
            </Button>
            <Button type="submit">创建群聊</Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  )
}
