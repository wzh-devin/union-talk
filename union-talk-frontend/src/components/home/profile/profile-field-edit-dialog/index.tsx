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
import {
  editableProfileFieldMeta,
  getProfileFieldError,
  type EditableProfileField,
} from '@/pages/home/model/profile-settings'

interface ProfileFieldEditDialogProps {
  field: EditableProfileField
  value: string
  isSaving: boolean
  onOpenChange: (open: boolean) => void
  onSave: (value: string) => Promise<void>
}

/**
 * 渲染并校验单个资料字段编辑弹窗.
 * @param props 资料字段编辑属性
 * @return 资料字段编辑弹窗
 */
export const ProfileFieldEditDialog = ({
  field,
  value,
  isSaving,
  onOpenChange,
  onSave,
}: ProfileFieldEditDialogProps) => {
  const fieldMeta = editableProfileFieldMeta[field]
  const [draftValue, setDraftValue] = useState(value)
  const [errorMessage, setErrorMessage] = useState('')

  /**
   * 校验并保存当前资料字段.
   * @param event 表单提交事件
   * @return 保存流程
   */
  const handleSubmit = async (
    event: FormEvent<HTMLFormElement>,
  ): Promise<void> => {
    event.preventDefault()
    const nextValue = field === 'bio' ? draftValue : draftValue.trim()
    const nextErrorMessage = getProfileFieldError(field, nextValue)

    if (nextErrorMessage) {
      setErrorMessage(nextErrorMessage)
      return
    }

    try {
      await onSave(nextValue)
      onOpenChange(false)
    } catch {
      // 错误提示由统一业务 hook 展示，保留弹窗供用户修正。
    }
  }

  return (
    <Dialog open onOpenChange={onOpenChange}>
      <DialogContent
        closeLabel={`关闭编辑${fieldMeta.label}`}
        className="w-[min(440px,calc(100vw-32px))] overflow-hidden rounded-xl p-0"
      >
        <div className="border-border border-b px-5 py-4 pr-12">
          <DialogTitle className="text-lg font-semibold">
            编辑{fieldMeta.label}
          </DialogTitle>
          <DialogDescription className="text-muted-foreground mt-1 text-sm">
            {fieldMeta.description}
          </DialogDescription>
        </div>
        <form className="space-y-4 p-5" noValidate onSubmit={handleSubmit}>
          <label className="grid gap-2 text-sm font-medium">
            {fieldMeta.label}
            {fieldMeta.multiline ? (
              <Textarea
                value={draftValue}
                aria-label={fieldMeta.label}
                className="min-h-24"
                onChange={(event) => {
                  setDraftValue(event.target.value)
                  setErrorMessage('')
                }}
              />
            ) : (
              <Input
                type={fieldMeta.inputType}
                value={draftValue}
                aria-label={fieldMeta.label}
                autoComplete={field === 'email' ? 'email' : 'name'}
                onChange={(event) => {
                  setDraftValue(event.target.value)
                  setErrorMessage('')
                }}
              />
            )}
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
