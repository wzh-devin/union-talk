import { useState, type FormEvent } from 'react'

import { Button } from '@/components/shadcn-ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogTitle,
} from '@/components/shadcn-ui/dialog'
import { Input } from '@/components/shadcn-ui/input'
import { getPasswordError } from '@/pages/home/model/profile-settings'

interface PasswordEditDialogProps {
  isSaving: boolean
  onOpenChange: (open: boolean) => void
  onSave: (currentPassword: string, newPassword: string) => Promise<void>
}

/**
 * 渲染并校验本地密码编辑弹窗.
 * @param props 密码编辑属性
 * @return 密码编辑弹窗
 */
export const PasswordEditDialog = ({
  isSaving,
  onOpenChange,
  onSave,
}: PasswordEditDialogProps) => {
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [errorMessage, setErrorMessage] = useState('')

  /**
   * 校验并保存本地密码.
   * @param event 表单提交事件
   * @return 保存流程
   */
  const handleSubmit = async (
    event: FormEvent<HTMLFormElement>,
  ): Promise<void> => {
    event.preventDefault()
    const nextErrorMessage = getPasswordError({
      currentPassword,
      newPassword,
      confirmPassword,
    })

    if (nextErrorMessage) {
      setErrorMessage(nextErrorMessage)
      return
    }

    try {
      await onSave(currentPassword, newPassword)
      onOpenChange(false)
    } catch {
      // 错误提示由统一业务 hook 展示，保留弹窗供用户修正。
    }
  }

  return (
    <Dialog open onOpenChange={onOpenChange}>
      <DialogContent
        closeLabel="关闭修改密码"
        className="w-[min(440px,calc(100vw-32px))] overflow-hidden rounded-xl p-0"
      >
        <div className="border-border border-b px-5 py-4 pr-12">
          <DialogTitle className="text-lg font-semibold">修改密码</DialogTitle>
          <DialogDescription className="text-muted-foreground mt-1 text-sm">
            验证当前密码后设置新的登录密码。
          </DialogDescription>
        </div>
        <form className="space-y-4 p-5" onSubmit={handleSubmit}>
          <label className="grid gap-2 text-sm font-medium">
            当前密码
            <Input
              type="password"
              value={currentPassword}
              aria-label="当前密码"
              autoComplete="current-password"
              onChange={(event) => {
                setCurrentPassword(event.target.value)
                setErrorMessage('')
              }}
            />
          </label>
          <label className="grid gap-2 text-sm font-medium">
            新密码
            <Input
              type="password"
              value={newPassword}
              aria-label="新密码"
              autoComplete="new-password"
              onChange={(event) => {
                setNewPassword(event.target.value)
                setErrorMessage('')
              }}
            />
          </label>
          <label className="grid gap-2 text-sm font-medium">
            确认新密码
            <Input
              type="password"
              value={confirmPassword}
              aria-label="确认新密码"
              autoComplete="new-password"
              onChange={(event) => {
                setConfirmPassword(event.target.value)
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
              onClick={() => onOpenChange(false)}
            >
              取消
            </Button>
            <Button type="submit" disabled={isSaving}>
              {isSaving ? '保存中...' : '保存'}
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  )
}
