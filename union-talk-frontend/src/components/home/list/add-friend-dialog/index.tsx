import { useState, type FormEvent, type RefObject } from 'react'

import { Button } from '@/components/shadcn-ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogTitle,
} from '@/components/shadcn-ui/dialog'
import { Input } from '@/components/shadcn-ui/input'
import { Textarea } from '@/components/shadcn-ui/textarea'
import { getAddFriendFormError } from '@/pages/home/model/conversation-create'
import type { AddFriendFormInput } from '@/pages/home/model/types'

interface AddFriendDialogProps {
  open: boolean
  returnFocusRef: RefObject<HTMLButtonElement | null>
  onOpenChange: (isOpen: boolean) => void
  onSubmit: (input: AddFriendFormInput) => void | Promise<void>
}

/**
 * 渲染并校验本地好友申请弹窗.
 * @param props 添加好友弹窗属性
 * @return 添加好友弹窗
 */
export const AddFriendDialog = ({
  open,
  returnFocusRef,
  onOpenChange,
  onSubmit,
}: AddFriendDialogProps) => {
  const [friendCode, setFriendCode] = useState('')
  const [applyMessage, setApplyMessage] = useState('')
  const [errorMessage, setErrorMessage] = useState('')

  /**
   * 切换弹窗并在关闭时重置表单.
   * @param nextIsOpen 下一弹窗状态
   * @return void
   */
  const handleOpenChange = (nextIsOpen: boolean): void => {
    if (!nextIsOpen) {
      setFriendCode('')
      setApplyMessage('')
      setErrorMessage('')
    }
    onOpenChange(nextIsOpen)
  }

  /**
   * 校验并提交本地好友申请.
   * @param event 表单提交事件
   * @return void
   */
  const handleSubmit = async (
    event: FormEvent<HTMLFormElement>,
  ): Promise<void> => {
    event.preventDefault()
    const input: AddFriendFormInput = {
      friendCode: friendCode.trim(),
      applyMessage: applyMessage.trim(),
    }
    const nextErrorMessage = getAddFriendFormError(input)

    if (nextErrorMessage) {
      setErrorMessage(nextErrorMessage)
      return
    }

    try {
      await onSubmit(input)
      handleOpenChange(false)
    } catch {
      // 业务层已展示失败提示，保留用户输入供重试。
    }
  }

  const isFriendCodeInvalid = errorMessage.includes('Code')
  const isApplyMessageInvalid = errorMessage.includes('申请简介')

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent
        closeLabel="关闭添加好友"
        className="w-[min(440px,calc(100vw-32px))] overflow-hidden rounded-xl p-0"
        onCloseAutoFocus={(event) => {
          event.preventDefault()
          returnFocusRef.current?.focus()
        }}
      >
        <div className="border-border border-b px-5 py-4 pr-12">
          <DialogTitle className="text-lg font-semibold">添加好友</DialogTitle>
          <DialogDescription className="text-muted-foreground mt-1 text-sm">
            输入好友 Code，并说明你的添加原因。
          </DialogDescription>
        </div>

        <form className="space-y-4 p-5" noValidate onSubmit={handleSubmit}>
          <label className="grid gap-2 text-sm font-medium">
            好友 Code
            <Input
              value={friendCode}
              aria-label="好友 Code"
              aria-invalid={isFriendCodeInvalid}
              maxLength={32}
              placeholder="请输入好友 Code"
              onChange={(event) => {
                setFriendCode(event.target.value)
                setErrorMessage('')
              }}
            />
          </label>

          <label className="grid gap-2 text-sm font-medium">
            申请简介
            <Textarea
              value={applyMessage}
              aria-label="申请简介"
              aria-invalid={isApplyMessageInvalid}
              maxLength={200}
              className="min-h-24"
              placeholder="介绍一下你是谁或添加原因"
              onChange={(event) => {
                setApplyMessage(event.target.value)
                setErrorMessage('')
              }}
            />
          </label>

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
            <Button type="submit">发送申请</Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  )
}
