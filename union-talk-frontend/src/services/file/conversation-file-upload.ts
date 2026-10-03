import { cancel, complete, init, uploadChunk } from '@/services/generated/file'
import type { UploadSessionRespVO } from '@/services/generated/file/models'

export const conversationFileChunkSize = 5 * 1024 * 1024
export const conversationFileUploadConcurrency = 6

interface UploadConversationFileInput {
  conversationId: string
  folderId?: string
  file: File
  onProgress?: (progress: number) => void
}

/**
 * 将本地文件按固定大小分片上传到指定会话目录.
 * @param input 文件、目标目录和进度回调
 * @return 上传完成流程
 */
export const uploadConversationFile = async ({
  conversationId,
  folderId,
  file,
  onProgress,
}: UploadConversationFileInput): Promise<
  UploadSessionRespVO & { assetId: string }
> => {
  if (file.size <= 0) {
    throw new Error('不能上传空文件')
  }

  const session = await init({
    conversationId,
    folderId,
    fileName: file.name,
    fileSize: String(file.size),
    chunkSize: String(conversationFileChunkSize),
    mimeType: file.type || 'application/octet-stream',
  })
  const sessionId = session.sessionId

  if (!sessionId) {
    throw new Error('上传任务初始化失败')
  }

  const chunkCount = Math.ceil(file.size / conversationFileChunkSize)

  try {
    let nextChunkIndex = 0
    let uploadedChunkCount = 0
    let hasUploadError = false
    let firstUploadError: unknown

    /**
     * 持续领取并上传下一个分片，直到任务完成或任一分片失败.
     * @return 当前上传 Worker 完成
     */
    const uploadNextChunk = async (): Promise<void> => {
      while (!hasUploadError) {
        const chunkIndex = nextChunkIndex
        nextChunkIndex += 1

        if (chunkIndex >= chunkCount) {
          return
        }

        const start = chunkIndex * conversationFileChunkSize
        const chunk = file.slice(start, start + conversationFileChunkSize)

        try {
          await uploadChunk(sessionId, chunkIndex, { file: chunk }, undefined, {
            headers: { 'Content-Type': 'multipart/form-data' },
          })
          uploadedChunkCount += 1
          onProgress?.(Math.round((uploadedChunkCount / chunkCount) * 100))
        } catch (error) {
          if (!hasUploadError) {
            firstUploadError = error
          }
          hasUploadError = true
        }
      }
    }

    const uploadWorkerCount = Math.min(
      conversationFileUploadConcurrency,
      chunkCount,
    )
    await Promise.all(
      Array.from({ length: uploadWorkerCount }, () => uploadNextChunk()),
    )

    if (hasUploadError) {
      throw firstUploadError
    }

    const completedSession = await complete(sessionId)
    if (!completedSession.assetId) {
      throw new Error('上传完成后未返回资产文件 ID')
    }
    return completedSession as UploadSessionRespVO & { assetId: string }
  } catch (error) {
    await cancel(sessionId).catch(() => undefined)
    throw error
  }
}
