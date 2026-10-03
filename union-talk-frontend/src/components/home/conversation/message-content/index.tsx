import { useCallback, useEffect, useState } from 'react'
import { Expand, FileX2, LoaderCircle, Play, RotateCcw } from 'lucide-react'

import { AudioMessagePlayer } from '@/components/home/conversation/audio-message-player'
import { MessageMentionText } from '@/components/home/conversation/message-mention-text'
import { ConversationFileIcon } from '@/components/home/details/conversation-file-icon'
import {
  ConversationFilePreviewDialog,
  type ConversationFilePreviewTarget,
} from '@/components/home/details/conversation-file-preview-dialog'
import { Button } from '@/components/shadcn-ui/button'
import type { Message } from '@/pages/home/model/types'
import {
  classifyConversationFile,
  formatConversationFileSize,
} from '@/pages/home/model/conversation-file'
import { previewConversationFile } from '@/services/file/conversation-file-preview'
import { messageType } from '@/services/message/message-contract'
import { cn } from '@/utils/class-name'

interface MessageContentProps {
  message: Message
  isSelf: boolean
}

interface AssetPreviewState {
  assetId: string
  previewUrl: string
  isLoading: boolean
  errorMessage: string
}

const emptyAssetPreviewState: AssetPreviewState = {
  assetId: '',
  previewUrl: '',
  isLoading: false,
  errorMessage: '',
}

/**
 * 按消息类型渲染文本、Emoji、音视频或文件内容.
 * @param props 消息内容属性
 * @return 消息内容
 */
export const MessageContent = ({ message, isSelf }: MessageContentProps) => {
  const [assetPreviewState, setAssetPreviewState] = useState<AssetPreviewState>(
    emptyAssetPreviewState,
  )
  const [previewFile, setPreviewFile] =
    useState<ConversationFilePreviewTarget | null>(null)
  const assetInfo = message.assetInfo
  const currentMessageType = message.type || messageType.TEXT
  const assetId = assetInfo?.id || ''
  const assetName = assetInfo?.name || message.body
  const hasCurrentPreview = assetPreviewState.assetId === assetId
  const previewUrl = hasCurrentPreview ? assetPreviewState.previewUrl : ''
  const isLoading = hasCurrentPreview && assetPreviewState.isLoading
  const errorMessage = hasCurrentPreview ? assetPreviewState.errorMessage : ''
  const fileCategory = classifyConversationFile(assetInfo || {})
  const bubbleClassName = cn(
    'w-fit max-w-full rounded-xl px-3.5 py-2.5 text-left text-sm leading-6 break-words whitespace-pre-wrap',
    isSelf
      ? 'bg-primary text-primary-foreground ml-auto'
      : 'bg-muted text-foreground',
  )

  /**
   * 按需获取当前音视频的临时预览地址.
   * @return 预览加载流程
   */
  const loadAssetPreview = useCallback(async (): Promise<void> => {
    if (!assetId || isLoading) {
      return
    }
    setAssetPreviewState({
      assetId,
      previewUrl: '',
      isLoading: true,
      errorMessage: '',
    })
    try {
      const nextPreviewUrl = await previewConversationFile(assetId)
      setAssetPreviewState({
        assetId,
        previewUrl: nextPreviewUrl,
        isLoading: false,
        errorMessage: '',
      })
    } catch {
      setAssetPreviewState({
        assetId,
        previewUrl: '',
        isLoading: false,
        errorMessage: '预览加载失败',
      })
    }
  }, [assetId, isLoading])

  useEffect(() => {
    if (
      currentMessageType === messageType.FILE &&
      fileCategory === 'image' &&
      assetId &&
      !previewUrl &&
      !errorMessage
    ) {
      const previewTimer = setTimeout(() => void loadAssetPreview(), 0)
      return () => clearTimeout(previewTimer)
    }
    return undefined
  }, [
    assetId,
    currentMessageType,
    errorMessage,
    fileCategory,
    loadAssetPreview,
    previewUrl,
  ])

  if (currentMessageType === messageType.EMOJI) {
    return (
      <div
        data-message-type={currentMessageType}
        className={cn(
          'w-fit bg-transparent py-1 text-left text-4xl leading-none',
          isSelf && 'ml-auto',
        )}
      >
        {message.body}
      </div>
    )
  }

  if (currentMessageType === messageType.TEXT) {
    return (
      <div data-message-type={currentMessageType} className={bubbleClassName}>
        <MessageMentionText
          content={message.body}
          mentionList={message.mentionList ?? []}
        />
      </div>
    )
  }

  if (!assetInfo || !assetId) {
    return (
      <div
        data-message-type={currentMessageType}
        className={cn(bubbleClassName, 'flex items-center gap-2')}
      >
        <FileX2 aria-hidden className="size-4" />
        文件不可用
      </div>
    )
  }

  if (currentMessageType === messageType.FILE && fileCategory === 'image') {
    return (
      <>
        {previewUrl ? (
          <button
            type="button"
            data-message-type={currentMessageType}
            aria-label={`预览图片 ${assetName}`}
            className={cn(
              'group border-border bg-muted relative block max-w-[min(460px,75vw)] overflow-hidden rounded-2xl border text-left shadow-sm',
              isSelf && 'ml-auto',
            )}
            onClick={() =>
              setPreviewFile({
                resourceId: assetId,
                name: assetName,
                fileExt: assetInfo.fileExt,
                fileType: assetInfo.fileType,
                fileSize: assetInfo.fileSize,
                mimeType: assetInfo.mimeType,
              })
            }
          >
            <img
              src={previewUrl}
              alt={assetName}
              className="max-h-80 min-h-36 w-full object-cover"
              onError={() =>
                setAssetPreviewState({
                  assetId,
                  previewUrl: '',
                  isLoading: false,
                  errorMessage: '图片加载失败',
                })
              }
            />
            <span className="bg-background/88 text-foreground absolute top-2 right-2 grid size-8 place-items-center rounded-full opacity-0 shadow-sm transition group-hover:opacity-100 group-focus-visible:opacity-100">
              <Expand aria-hidden className="size-4" />
            </span>
          </button>
        ) : (
          <Button
            type="button"
            variant="secondary"
            aria-label={`${errorMessage ? '重试图片 ' : '加载图片 '}${assetName}`}
            className={cn(
              'h-36 w-[min(360px,75vw)] rounded-2xl',
              isSelf && 'ml-auto',
            )}
            onClick={() => void loadAssetPreview()}
          >
            {isLoading ? (
              <LoaderCircle aria-hidden className="animate-spin" />
            ) : (
              <RotateCcw aria-hidden />
            )}
            {errorMessage || '正在加载图片'}
          </Button>
        )}
        <ConversationFilePreviewDialog
          file={previewFile}
          onClose={() => setPreviewFile(null)}
        />
      </>
    )
  }

  if (currentMessageType === messageType.AUDIO) {
    return (
      <div data-message-type={currentMessageType}>
        <AudioMessagePlayer
          assetName={assetName}
          isSelf={isSelf}
          previewUrl={previewUrl}
          isLoading={isLoading}
          errorMessage={errorMessage}
          onLoadPreview={() => void loadAssetPreview()}
        />
      </div>
    )
  }

  if (currentMessageType === messageType.VIDEO) {
    return (
      <div data-message-type={currentMessageType}>
        {previewUrl ? (
          <video
            src={previewUrl}
            aria-label={`${assetName}视频消息`}
            controls
            className="max-h-80 w-[min(420px,75vw)] rounded-xl bg-black"
          />
        ) : (
          <button
            type="button"
            aria-label={`${errorMessage ? '重试 ' : '播放 '}${assetName}`}
            className="group relative flex h-44 w-[min(360px,75vw)] items-center justify-center overflow-hidden rounded-xl bg-zinc-900 text-white"
            onClick={() => void loadAssetPreview()}
          >
            <span className="absolute inset-x-0 bottom-0 truncate bg-black/55 px-3 py-2 text-left text-xs">
              {errorMessage || assetName}
            </span>
            {isLoading ? (
              <LoaderCircle aria-hidden className="size-8 animate-spin" />
            ) : errorMessage ? (
              <RotateCcw aria-hidden className="size-8" />
            ) : (
              <span className="grid size-12 place-items-center rounded-full bg-white/18 transition group-hover:bg-white/28">
                <Play aria-hidden className="size-6 fill-current" />
              </span>
            )}
          </button>
        )}
      </div>
    )
  }

  return (
    <>
      <button
        type="button"
        data-message-type={currentMessageType}
        aria-label={`预览 ${assetName}`}
        className={cn(
          'flex w-[min(340px,75vw)] items-center gap-3 rounded-xl px-3.5 py-3 text-left transition',
          isSelf
            ? 'bg-primary text-primary-foreground ml-auto hover:opacity-90'
            : 'bg-muted text-foreground hover:bg-muted/75',
        )}
        onClick={() =>
          setPreviewFile({
            resourceId: assetId,
            name: assetName,
            fileExt: assetInfo.fileExt,
            fileType: assetInfo.fileType,
            fileSize: assetInfo.fileSize,
            mimeType: assetInfo.mimeType,
          })
        }
      >
        <span className="bg-background/80 grid size-10 shrink-0 place-items-center rounded-lg">
          <ConversationFileIcon file={assetInfo} className="size-5" />
        </span>
        <span className="min-w-0 flex-1">
          <span className="block truncate text-sm font-medium">
            {assetName}
          </span>
          <span className="block text-xs opacity-70">
            {formatConversationFileSize(assetInfo.fileSize)}
          </span>
        </span>
      </button>
      <ConversationFilePreviewDialog
        file={previewFile}
        onClose={() => setPreviewFile(null)}
      />
    </>
  )
}
