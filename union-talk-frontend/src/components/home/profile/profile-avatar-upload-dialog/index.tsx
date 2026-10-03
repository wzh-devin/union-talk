import { useRef, useState, type ChangeEvent, type FormEvent } from 'react'
import { Upload } from 'lucide-react'

import {
  Avatar,
  AvatarFallback,
  AvatarImage,
} from '@/components/shadcn-ui/avatar'
import { Button } from '@/components/shadcn-ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogTitle,
} from '@/components/shadcn-ui/dialog'
import { Input } from '@/components/shadcn-ui/input'
import { getInitials } from '@/utils/avatar'
import type { ProfileSettings } from '@/pages/home/model/types'

interface ProfileAvatarUploadDialogProps {
  profile: ProfileSettings
  isSaving: boolean
  onOpenChange: (open: boolean) => void
  onUpload: (file: File) => Promise<void>
}

/**
 * 渲染头像图片上传弹窗.
 * @param props 头像上传弹窗属性
 * @return 头像上传弹窗
 */
export const ProfileAvatarUploadDialog = ({
  profile,
  isSaving,
  onOpenChange,
  onUpload,
}: ProfileAvatarUploadDialogProps) => {
  const [selectedFile, setSelectedFile] = useState<File | null>(null)
  const [errorMessage, setErrorMessage] = useState('')
  const fileInputRef = useRef<HTMLInputElement>(null)

  /**
   * 读取并校验用户选择的头像文件.
   * @param event 文件选择事件
   * @return void
   */
  const handleFileChange = (event: ChangeEvent<HTMLInputElement>): void => {
    const file = event.target.files?.[0] ?? null

    if (!file) {
      setSelectedFile(null)
      setErrorMessage('')
      return
    }

    if (!file.type.startsWith('image/')) {
      setSelectedFile(null)
      setErrorMessage('请选择图片文件')
      return
    }

    setSelectedFile(file)
    setErrorMessage('')
  }

  /**
   * 提交当前选择的头像文件.
   * @param event 表单提交事件
   * @return 上传流程
   */
  const handleSubmit = async (
    event: FormEvent<HTMLFormElement>,
  ): Promise<void> => {
    event.preventDefault()

    if (!selectedFile) {
      setErrorMessage('请选择头像文件')
      return
    }

    try {
      await onUpload(selectedFile)
      onOpenChange(false)
    } catch {
      // 业务错误由资料 hook 统一提示，弹窗保持打开方便用户重试。
    }
  }

  return (
    <Dialog open onOpenChange={onOpenChange}>
      <DialogContent
        closeLabel="关闭修改头像"
        className="w-[min(440px,calc(100vw-32px))] overflow-hidden rounded-xl p-0"
      >
        <div className="border-border border-b px-5 py-4 pr-12">
          <DialogTitle className="text-base font-semibold">
            修改头像
          </DialogTitle>
          <DialogDescription className="text-muted-foreground mt-1 text-sm leading-5">
            选择一张清晰图片作为新的个人头像。
          </DialogDescription>
        </div>
        <form noValidate onSubmit={handleSubmit}>
          <div className="grid gap-5 p-5">
            <div className="grid grid-cols-[72px_minmax(0,1fr)] items-center gap-4">
              <Avatar className="border-border size-18 border">
                {profile.avatarUrl ? (
                  <AvatarImage
                    src={profile.avatarUrl}
                    alt={profile.displayName}
                  />
                ) : null}
                <AvatarFallback className="bg-indigo-700 text-base text-white">
                  {getInitials(profile.displayName)}
                </AvatarFallback>
              </Avatar>
              <div className="min-w-0">
                <p className="text-foreground truncate text-sm font-semibold">
                  {profile.displayName}
                </p>
                <p className="text-muted-foreground mt-1 truncate text-xs leading-5">
                  {selectedFile?.name || '未选择图片'}
                </p>
                <p className="text-muted-foreground mt-0.5 text-xs leading-5">
                  支持常见图片格式，建议使用正方形图片。
                </p>
              </div>
            </div>

            <div className="border-border bg-muted/35 grid gap-3 rounded-lg border p-3 sm:grid-cols-[minmax(0,1fr)_auto] sm:items-center">
              <div className="min-w-0">
                <p className="text-foreground text-sm font-medium">头像文件</p>
                <p className="text-muted-foreground mt-1 truncate text-xs">
                  {selectedFile
                    ? `${Math.max(1, Math.round(selectedFile.size / 1024))} KB`
                    : '从本地选择一张图片后再上传'}
                </p>
              </div>
              <Button
                type="button"
                variant="outline"
                size="sm"
                className="justify-self-start sm:justify-self-end"
                onClick={() => fileInputRef.current?.click()}
              >
                选择图片
              </Button>
            </div>

            <Input
              ref={fileInputRef}
              type="file"
              accept="image/*"
              aria-label="头像文件"
              className="sr-only"
              onChange={handleFileChange}
            />

            {errorMessage ? (
              <p role="alert" className="text-destructive text-sm">
                {errorMessage}
              </p>
            ) : null}
          </div>
          <div className="border-border bg-muted/25 flex justify-end gap-2 border-t px-5 py-3">
            <Button
              type="button"
              variant="outline"
              onClick={() => onOpenChange(false)}
            >
              取消
            </Button>
            <Button type="submit" disabled={isSaving || !selectedFile}>
              <Upload data-icon="inline-start" />
              {isSaving ? '上传中...' : '上传'}
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  )
}
