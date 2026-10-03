import { useCallback, useState } from 'react'
import { toast } from 'sonner'

import { downloadConversationFile } from '@/services/file/conversation-file-download'
import { uploadConversationFile } from '@/services/file/conversation-file-upload'
import {
  create,
  delete1,
  delete2,
  move,
  move1,
  rename,
  rename1,
} from '@/services/generated/file'

type ConversationFileAction =
  | 'create-folder'
  | 'rename-folder'
  | 'move-folder'
  | 'delete-folder'
  | 'rename-file'
  | 'move-file'
  | 'delete-file'
  | 'upload-file'
  | 'download-file'
  | 'delete-batch'
  | 'upload-file-list'

export interface ConversationFileDeleteTarget {
  id: string
  resourceId: string
  type: 'folder' | 'file'
}

export interface ConversationFileBatchResult {
  successfulCount: number
  failedIdList: string[]
}

interface UseConversationFileManagerInput {
  conversationId: string
  refresh: () => void
}

interface ConversationFileManager {
  processingAction: ConversationFileAction | null
  createFolder: (parentId: string | undefined, name: string) => Promise<boolean>
  renameFolder: (folderId: string, name: string) => Promise<boolean>
  moveFolder: (folderId: string, targetParentId: string) => Promise<boolean>
  deleteFolder: (folderId: string) => Promise<boolean>
  renameFile: (fileId: string, name: string) => Promise<boolean>
  moveFile: (fileId: string, targetFolderId: string) => Promise<boolean>
  deleteFile: (fileId: string) => Promise<boolean>
  downloadFile: (fileId: string) => Promise<boolean>
  uploadFile: (folderId: string | undefined, file: File) => Promise<boolean>
  deleteNodeList: (
    targetList: ConversationFileDeleteTarget[],
  ) => Promise<ConversationFileBatchResult>
  uploadFileList: (
    folderId: string | undefined,
    fileList: File[],
  ) => Promise<ConversationFileBatchResult>
}

const getRequestErrorMessage = (error: unknown): string =>
  error instanceof Error ? error.message : '文件操作失败，请稍后重试'

/**
 * 管理会话目录和文件的变更、上传、提示与刷新流程.
 * @param input 会话标识和目录树刷新函数
 * @return 文件管理操作与处理状态
 */
export const useConversationFileManager = ({
  conversationId,
  refresh,
}: UseConversationFileManagerInput): ConversationFileManager => {
  const [processingAction, setProcessingAction] =
    useState<ConversationFileAction | null>(null)

  const runMutation = useCallback(
    async (
      action: ConversationFileAction,
      request: () => Promise<unknown>,
      successMessage: string,
    ): Promise<boolean> => {
      setProcessingAction(action)

      try {
        await request()
        refresh()
        toast.success(successMessage)
        return true
      } catch (error) {
        refresh()
        toast.error(getRequestErrorMessage(error))
        return false
      } finally {
        setProcessingAction(null)
      }
    },
    [refresh],
  )

  const createFolder = useCallback(
    (parentId: string | undefined, name: string) =>
      runMutation(
        'create-folder',
        () => create({ conversationId, parentId, name }),
        '目录已创建',
      ),
    [conversationId, runMutation],
  )

  const renameFolder = useCallback(
    (folderId: string, name: string) =>
      runMutation(
        'rename-folder',
        () => rename(folderId, { name }),
        '目录已重命名',
      ),
    [runMutation],
  )

  const moveFolder = useCallback(
    (folderId: string, targetParentId: string) =>
      runMutation(
        'move-folder',
        () => move(folderId, { targetParentId }),
        '目录已移动',
      ),
    [runMutation],
  )

  const deleteFolder = useCallback(
    (folderId: string) =>
      runMutation('delete-folder', () => delete1(folderId), '目录已删除'),
    [runMutation],
  )

  const renameFile = useCallback(
    (fileId: string, name: string) =>
      runMutation(
        'rename-file',
        () => rename1(fileId, { name }),
        '文件已重命名',
      ),
    [runMutation],
  )

  const moveFile = useCallback(
    (fileId: string, targetFolderId: string) =>
      runMutation(
        'move-file',
        () => move1(fileId, { targetFolderId }),
        '文件已移动',
      ),
    [runMutation],
  )

  const deleteFile = useCallback(
    (fileId: string) =>
      runMutation('delete-file', () => delete2(fileId), '文件已删除'),
    [runMutation],
  )

  /**
   * 获取文件下载地址并触发浏览器直连下载.
   * @param fileId 文件标识
   * @return 是否成功触发下载
   */
  const downloadFile = useCallback(async (fileId: string): Promise<boolean> => {
    setProcessingAction('download-file')

    try {
      await downloadConversationFile(fileId)
      return true
    } catch (error) {
      toast.error(getRequestErrorMessage(error))
      return false
    } finally {
      setProcessingAction(null)
    }
  }, [])

  /**
   * 按选择顺序上传一批文件并汇总失败文件.
   * @param folderId 上传目标目录标识
   * @param fileList 待上传文件列表
   * @return 批量上传结果
   */
  const uploadFileList = useCallback(
    async (
      folderId: string | undefined,
      fileList: File[],
    ): Promise<ConversationFileBatchResult> => {
      if (fileList.length === 0) {
        return { successfulCount: 0, failedIdList: [] }
      }

      setProcessingAction('upload-file-list')
      const isSingleFile = fileList.length === 1
      const initialMessage = isSingleFile
        ? `正在上传 ${fileList[0].name}（0%）`
        : `正在上传 1/${fileList.length}：${fileList[0].name}（0%）`
      const toastId = toast.loading(initialMessage)
      const failedIdList: string[] = []

      try {
        for (const [fileIndex, file] of fileList.entries()) {
          try {
            await uploadConversationFile({
              conversationId,
              folderId,
              file,
              onProgress: (progress) => {
                const totalProgress = Math.round(
                  ((fileIndex + progress / 100) / fileList.length) * 100,
                )
                const progressMessage = isSingleFile
                  ? `正在上传 ${file.name}（${totalProgress}%）`
                  : `正在上传 ${fileIndex + 1}/${fileList.length}：${file.name}（${totalProgress}%）`
                toast.loading(progressMessage, { id: toastId })
              },
            })
          } catch {
            failedIdList.push(file.name)
          }
        }

        const successfulCount = fileList.length - failedIdList.length
        refresh()

        if (failedIdList.length > 0) {
          toast.error(
            `已上传 ${successfulCount} 个文件，${failedIdList.length} 个失败`,
            { id: toastId },
          )
        } else {
          toast.success(
            isSingleFile ? '文件已上传' : `${successfulCount} 个文件已上传`,
            { id: toastId },
          )
        }

        return { successfulCount, failedIdList }
      } finally {
        setProcessingAction(null)
      }
    },
    [conversationId, refresh],
  )

  /**
   * 保留单文件上传调用并复用批量上传流程.
   * @param folderId 上传目标目录标识
   * @param file 待上传文件
   * @return 是否上传成功
   */
  const uploadFile = useCallback(
    async (folderId: string | undefined, file: File): Promise<boolean> =>
      (await uploadFileList(folderId, [file])).successfulCount === 1,
    [uploadFileList],
  )

  /**
   * 以最多六个并发请求删除混合文件和目录目标.
   * @param targetList 批量删除目标列表
   * @return 批量删除结果
   */
  const deleteNodeList = useCallback(
    async (
      targetList: ConversationFileDeleteTarget[],
    ): Promise<ConversationFileBatchResult> => {
      if (targetList.length === 0) {
        return { successfulCount: 0, failedIdList: [] }
      }

      setProcessingAction('delete-batch')
      const failedNodeIdSet = new Set<string>()
      let nextTargetIndex = 0

      try {
        const deleteNextTarget = async (): Promise<void> => {
          while (nextTargetIndex < targetList.length) {
            const target = targetList[nextTargetIndex]
            nextTargetIndex += 1

            try {
              if (target.type === 'folder') {
                await delete1(target.resourceId)
              } else {
                await delete2(target.resourceId)
              }
            } catch {
              failedNodeIdSet.add(target.id)
            }
          }
        }
        const workerCount = Math.min(6, targetList.length)

        await Promise.all(
          Array.from({ length: workerCount }, () => deleteNextTarget()),
        )

        const failedIdList = targetList
          .filter((target) => failedNodeIdSet.has(target.id))
          .map((target) => target.id)
        const successfulCount = targetList.length - failedIdList.length

        refresh()
        if (failedIdList.length > 0) {
          toast.error(
            `已删除 ${successfulCount} 项，${failedIdList.length} 项失败`,
          )
        } else {
          toast.success(`已删除 ${successfulCount} 项`)
        }

        return { successfulCount, failedIdList }
      } finally {
        setProcessingAction(null)
      }
    },
    [refresh],
  )

  return {
    processingAction,
    createFolder,
    renameFolder,
    moveFolder,
    deleteFolder,
    renameFile,
    moveFile,
    deleteFile,
    downloadFile,
    uploadFile,
    deleteNodeList,
    uploadFileList,
  }
}
