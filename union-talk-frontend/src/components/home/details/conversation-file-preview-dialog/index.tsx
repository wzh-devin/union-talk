import { useEffect, useState } from 'react'
import { Download, ExternalLink, LoaderCircle, RotateCcw } from 'lucide-react'

import { ConversationFileIcon } from '@/components/home/details/conversation-file-icon'
import { Button } from '@/components/shadcn-ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogTitle,
} from '@/components/shadcn-ui/dialog'
import {
  classifyConversationFile,
  formatConversationFileSize,
} from '@/pages/home/model/conversation-file'
import { previewConversationFile } from '@/services/file/conversation-file-preview'

export interface ConversationFilePreviewTarget {
  resourceId: string
  name: string
  fileExt?: string
  fileType?: string
  fileSize?: string
  mimeType?: string
}

interface ConversationFilePreviewDialogProps {
  file: ConversationFilePreviewTarget | null
  onClose: () => void
  onDownload?: (fileId: string) => Promise<boolean>
  isDownloading?: boolean
}

interface PreviewRequestState {
  requestKey: string
  previewUrl: string
  errorMessage: string
  hasMediaError: boolean
}

/**
 * 展示会话文件的临时在线预览.
 * @param props 当前文件和关闭回调
 * @return 文件预览弹窗
 */
export const ConversationFilePreviewDialog = ({
  file,
  onClose,
  onDownload,
  isDownloading = false,
}: ConversationFilePreviewDialogProps) => {
  const [requestVersion, setRequestVersion] = useState(0)
  const [previewRequestState, setPreviewRequestState] =
    useState<PreviewRequestState>({
      requestKey: '',
      previewUrl: '',
      errorMessage: '',
      hasMediaError: false,
    })
  const fileId = file?.resourceId || ''
  const requestKey = `${fileId}:${requestVersion}`

  useEffect(() => {
    let isCurrentRequest = true

    if (!fileId) {
      return () => {
        isCurrentRequest = false
      }
    }

    void previewConversationFile(fileId)
      .then((url) => {
        if (isCurrentRequest) {
          setPreviewRequestState({
            requestKey,
            previewUrl: url,
            errorMessage: '',
            hasMediaError: false,
          })
        }
      })
      .catch(() => {
        if (isCurrentRequest) {
          setPreviewRequestState({
            requestKey,
            previewUrl: '',
            errorMessage: '文件预览加载失败，请稍后重试',
            hasMediaError: false,
          })
        }
      })

    return () => {
      isCurrentRequest = false
    }
  }, [fileId, requestKey])

  if (!file) {
    return null
  }

  const category = classifyConversationFile(file)
  const supportsInlinePreview = [
    'image',
    'video',
    'audio',
    'pdf',
    'text',
  ].includes(category)
  const fileTypeLabel =
    file.mimeType || file.fileExt?.toUpperCase() || '未知类型'
  const isCurrentPreview = previewRequestState.requestKey === requestKey
  const previewUrl = isCurrentPreview ? previewRequestState.previewUrl : ''
  const errorMessage = isCurrentPreview ? previewRequestState.errorMessage : ''
  const hasMediaError = isCurrentPreview
    ? previewRequestState.hasMediaError
    : false
  const isLoading = !previewUrl && !errorMessage
  const showFallback =
    Boolean(previewUrl) && (!supportsInlinePreview || hasMediaError)

  /**
   * 将浏览器媒体解码失败切换为文件打开降级态.
   * @return void
   */
  const handleMediaError = (): void => {
    setPreviewRequestState((currentState) =>
      currentState.requestKey === requestKey
        ? { ...currentState, hasMediaError: true }
        : currentState,
    )
  }

  return (
    <Dialog
      open
      onOpenChange={(isOpen) => {
        if (!isOpen) {
          onClose()
        }
      }}
    >
      <DialogContent
        closeLabel="关闭文件预览"
        className="flex h-[min(82svh,760px)] w-[min(960px,calc(100vw-32px))] max-w-none flex-col overflow-hidden p-0 sm:max-w-none"
      >
        <div className="border-border flex min-h-16 shrink-0 items-center gap-3 border-b px-5 pr-14">
          <ConversationFileIcon file={file} className="size-5" />
          <div className="min-w-0 flex-1">
            <DialogTitle className="truncate text-base font-semibold">
              {file.name}
            </DialogTitle>
            <DialogDescription className="text-muted-foreground mt-0.5 truncate text-xs">
              {formatConversationFileSize(file.fileSize)} · {fileTypeLabel}
            </DialogDescription>
          </div>
          {onDownload ? (
            <Button
              type="button"
              variant="outline"
              aria-label={isDownloading ? '正在下载文件' : '下载文件'}
              disabled={isDownloading}
              onClick={() => void onDownload(file.resourceId)}
            >
              {isDownloading ? (
                <LoaderCircle aria-hidden className="animate-spin" />
              ) : (
                <Download aria-hidden />
              )}
              下载
            </Button>
          ) : null}
        </div>

        <div className="bg-muted/20 relative flex min-h-0 flex-1 items-center justify-center overflow-hidden p-4">
          {isLoading ? (
            <div
              role="status"
              aria-label="正在加载预览"
              className="text-muted-foreground flex items-center gap-2 text-sm"
            >
              <LoaderCircle
                aria-hidden="true"
                className="size-4 animate-spin"
              />
              正在加载预览
            </div>
          ) : null}

          {errorMessage ? (
            <div
              role="alert"
              className="flex flex-col items-center gap-4 text-center"
            >
              <div>
                <p className="font-medium">{errorMessage}</p>
                <p className="text-muted-foreground mt-1 text-sm">
                  请检查网络连接后重新尝试
                </p>
              </div>
              <Button
                type="button"
                variant="outline"
                onClick={() =>
                  setRequestVersion((currentVersion) => currentVersion + 1)
                }
              >
                <RotateCcw aria-hidden="true" />
                重试
              </Button>
            </div>
          ) : null}

          {previewUrl && !showFallback && category === 'image' ? (
            <img
              src={previewUrl}
              alt={file.name}
              className="max-h-full max-w-full object-contain"
              onError={handleMediaError}
            />
          ) : null}

          {previewUrl && !showFallback && category === 'video' ? (
            <video
              src={previewUrl}
              aria-label={`${file.name}视频预览`}
              controls
              className="max-h-full max-w-full"
              onError={handleMediaError}
            />
          ) : null}

          {previewUrl && !showFallback && category === 'audio' ? (
            <audio
              src={previewUrl}
              aria-label={`${file.name}音频预览`}
              controls
              className="w-full max-w-xl"
              onError={handleMediaError}
            />
          ) : null}

          {previewUrl &&
          !showFallback &&
          (category === 'pdf' || category === 'text') ? (
            <iframe
              src={previewUrl}
              title={`${file.name}预览`}
              className="bg-background size-full border-0"
              onError={handleMediaError}
            />
          ) : null}

          {showFallback ? (
            <div className="flex max-w-sm flex-col items-center text-center">
              <ConversationFileIcon file={file} className="size-12" />
              <p className="mt-5 text-base font-medium">
                {hasMediaError
                  ? '当前浏览器无法预览此文件'
                  : '暂不支持在线预览'}
              </p>
              <p className="text-muted-foreground mt-1 text-sm">
                可以使用本机支持的应用打开或下载文件
              </p>
              {!onDownload ? (
                <Button asChild className="mt-5">
                  <a href={previewUrl} target="_blank" rel="noreferrer">
                    <ExternalLink aria-hidden="true" />
                    在新窗口打开
                  </a>
                </Button>
              ) : null}
            </div>
          ) : null}
        </div>
      </DialogContent>
    </Dialog>
  )
}
