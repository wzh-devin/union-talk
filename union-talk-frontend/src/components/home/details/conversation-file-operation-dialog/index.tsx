import { useState, type FormEvent } from 'react'
import { Check, FileIcon, FolderIcon, LoaderCircle } from 'lucide-react'

import { Button } from '@/components/shadcn-ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogTitle,
} from '@/components/shadcn-ui/dialog'
import { Input } from '@/components/shadcn-ui/input'
import type { ConversationFileNode } from '@/hooks/use-conversation-file-tree'
import { cn } from '@/utils/class-name'
import {
  flattenFolderList,
  getEditableFileName,
  isValidFileMoveTarget,
} from '@/components/home/details/conversation-file-tree/file-tree-utils'

export type ConversationFileOperation = {
  type: 'create' | 'rename' | 'move' | 'delete'
  node: ConversationFileNode
}

interface ConversationFileOperationDialogProps {
  operation: ConversationFileOperation | null
  rootNode?: ConversationFileNode
  isProcessing: boolean
  onClose: () => void
  onCreateFolder: (
    parentId: string | undefined,
    name: string,
  ) => Promise<boolean>
  onRenameFolder: (folderId: string, name: string) => Promise<boolean>
  onMoveFolder: (folderId: string, targetParentId: string) => Promise<boolean>
  onDeleteFolder: (folderId: string) => Promise<boolean>
  onRenameFile: (fileId: string, name: string) => Promise<boolean>
  onMoveFile: (fileId: string, targetFolderId: string) => Promise<boolean>
  onDeleteFile: (fileId: string) => Promise<boolean>
}

/**
 * 渲染会话目录和文件的创建、重命名、移动与删除弹窗.
 * @param props 当前操作、目录树和文件管理回调
 * @return 文件操作弹窗
 */
export const ConversationFileOperationDialog = ({
  operation,
  rootNode,
  isProcessing,
  onClose,
  onCreateFolder,
  onRenameFolder,
  onMoveFolder,
  onDeleteFolder,
  onRenameFile,
  onMoveFile,
  onDeleteFile,
}: ConversationFileOperationDialogProps) => {
  const [nameDraft, setNameDraft] = useState(() =>
    operation?.type === 'rename'
      ? operation.node.type === 'file'
        ? getEditableFileName(operation.node)
        : operation.node.name
      : '',
  )
  const [selectedFolderId, setSelectedFolderId] = useState('')
  const [errorMessage, setErrorMessage] = useState('')

  if (!operation) {
    return null
  }

  const { node, type } = operation
  const extension =
    node.type === 'file' && node.fileExt ? `.${node.fileExt}` : ''
  const isNameOperation = type === 'create' || type === 'rename'
  const title =
    type === 'create'
      ? '创建目录'
      : type === 'rename'
        ? node.type === 'file'
          ? '重命名文件'
          : '重命名目录'
        : type === 'move'
          ? `移动${node.name}`
          : `删除${node.name}？`

  const submitName = async (
    event: FormEvent<HTMLFormElement>,
  ): Promise<void> => {
    event.preventDefault()
    const nextName = nameDraft.trim()

    if (!nextName) {
      setErrorMessage(type === 'create' ? '请输入目录名称' : '请输入新名称')
      return
    }

    const isSuccessful =
      type === 'create'
        ? await onCreateFolder(node.resourceId, nextName)
        : node.type === 'folder'
          ? await onRenameFolder(node.resourceId, nextName)
          : await onRenameFile(node.resourceId, `${nextName}${extension}`)

    if (isSuccessful) {
      onClose()
    }
  }

  const submitMove = async (): Promise<void> => {
    if (!selectedFolderId) {
      setErrorMessage('请选择目标目录')
      return
    }

    const isSuccessful =
      node.type === 'folder'
        ? await onMoveFolder(node.resourceId, selectedFolderId)
        : await onMoveFile(node.resourceId, selectedFolderId)

    if (isSuccessful) {
      onClose()
    }
  }

  const submitDelete = async (): Promise<void> => {
    const isSuccessful =
      node.type === 'folder'
        ? await onDeleteFolder(node.resourceId)
        : await onDeleteFile(node.resourceId)

    if (isSuccessful) {
      onClose()
    }
  }

  return (
    <Dialog
      open
      onOpenChange={(isOpen) => {
        if (!isOpen && !isProcessing) {
          onClose()
        }
      }}
    >
      <DialogContent
        closeLabel={`关闭${title}`}
        className="w-[min(380px,calc(100vw-32px))] p-5"
      >
        <DialogTitle className="pr-8 text-base font-semibold">
          {title}
        </DialogTitle>
        <DialogDescription className="text-muted-foreground mt-1 text-xs leading-5">
          {type === 'create'
            ? `在${node.isRoot ? '根目录' : node.name}中创建新目录。`
            : type === 'rename'
              ? node.type === 'file'
                ? '文件扩展名保持不变。'
                : '输入新的目录名称。'
              : type === 'move'
                ? '选择要移动到的目标目录。'
                : node.type === 'folder'
                  ? '删除后，其中的子目录和文件也会被永久删除。'
                  : '删除后，该文件将无法恢复。'}
        </DialogDescription>

        {isNameOperation ? (
          <form className="mt-4" noValidate onSubmit={submitName}>
            <label className="grid gap-2 text-xs font-medium">
              {type === 'create'
                ? '目录名称'
                : node.type === 'file'
                  ? '文件名称'
                  : '目录名称'}
              <span className="border-input focus-within:border-ring focus-within:ring-ring/50 flex h-9 rounded-lg border focus-within:ring-3">
                <Input
                  autoFocus
                  aria-label={
                    type === 'create'
                      ? '目录名称'
                      : node.type === 'file'
                        ? '文件名称'
                        : '目录名称'
                  }
                  value={nameDraft}
                  maxLength={
                    node.type === 'file' ? 255 - extension.length : 100
                  }
                  className="h-full min-w-0 flex-1 border-0 focus-visible:ring-0"
                  disabled={isProcessing}
                  onChange={(event) => {
                    setNameDraft(event.target.value)
                    setErrorMessage('')
                  }}
                />
                {extension ? (
                  <span className="text-muted-foreground flex items-center pr-3 text-sm">
                    {extension}
                  </span>
                ) : null}
              </span>
            </label>
            {errorMessage ? (
              <p role="alert" className="text-destructive mt-2 text-xs">
                {errorMessage}
              </p>
            ) : null}
            <div className="mt-5 flex justify-end gap-2">
              <Button type="button" variant="outline" onClick={onClose}>
                取消
              </Button>
              <Button type="submit" disabled={isProcessing}>
                {isProcessing ? (
                  <LoaderCircle aria-hidden className="animate-spin" />
                ) : null}
                {type === 'create' ? '创建' : '保存'}
              </Button>
            </div>
          </form>
        ) : null}

        {type === 'move' && rootNode ? (
          <div className="mt-4">
            <div className="max-h-64 space-y-0.5 overflow-y-auto py-1">
              {flattenFolderList(rootNode).map(
                ({ node: folderNode, depth }) => {
                  const isValid = isValidFileMoveTarget(node, folderNode)
                  const isSelected = selectedFolderId === folderNode.resourceId

                  return (
                    <button
                      key={folderNode.id}
                      type="button"
                      aria-label={`选择${folderNode.isRoot ? '根目录' : folderNode.name}`}
                      disabled={!isValid || isProcessing}
                      className={cn(
                        'hover:bg-muted focus-visible:ring-ring flex min-h-9 w-full items-center gap-2 rounded-md px-2 text-left text-sm outline-none focus-visible:ring-2 disabled:opacity-40',
                        isSelected && 'bg-muted',
                      )}
                      style={{ paddingLeft: `${8 + depth * 18}px` }}
                      onClick={() => {
                        setSelectedFolderId(folderNode.resourceId)
                        setErrorMessage('')
                      }}
                    >
                      <FolderIcon aria-hidden className="size-4 shrink-0" />
                      <span className="min-w-0 flex-1 truncate">
                        {folderNode.isRoot ? '根目录' : folderNode.name}
                      </span>
                      {isSelected ? (
                        <Check aria-hidden className="size-4" />
                      ) : null}
                    </button>
                  )
                },
              )}
            </div>
            {errorMessage ? (
              <p role="alert" className="text-destructive mt-2 text-xs">
                {errorMessage}
              </p>
            ) : null}
            <div className="mt-5 flex justify-end gap-2">
              <Button type="button" variant="outline" onClick={onClose}>
                取消
              </Button>
              <Button
                type="button"
                disabled={isProcessing}
                onClick={() => void submitMove()}
              >
                {isProcessing ? (
                  <LoaderCircle aria-hidden className="animate-spin" />
                ) : null}
                移动
              </Button>
            </div>
          </div>
        ) : null}

        {type === 'delete' ? (
          <div className="mt-5 flex justify-end gap-2">
            <Button type="button" variant="outline" onClick={onClose}>
              取消
            </Button>
            <Button
              type="button"
              variant="destructive"
              disabled={isProcessing}
              onClick={() => void submitDelete()}
            >
              {isProcessing ? (
                <LoaderCircle aria-hidden className="animate-spin" />
              ) : node.type === 'file' ? (
                <FileIcon aria-hidden />
              ) : (
                <FolderIcon aria-hidden />
              )}
              {node.type === 'folder' ? '删除目录' : '删除文件'}
            </Button>
          </div>
        ) : null}
      </DialogContent>
    </Dialog>
  )
}
