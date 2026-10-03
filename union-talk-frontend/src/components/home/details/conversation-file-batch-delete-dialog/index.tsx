import { LoaderCircle, Trash2 } from 'lucide-react'

import { Button } from '@/components/shadcn-ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogTitle,
} from '@/components/shadcn-ui/dialog'
import type {
  ConversationFileBatchResult,
  ConversationFileDeleteTarget,
} from '@/hooks/use-conversation-file-manager'

interface ConversationFileBatchDeleteDialogProps {
  nodeList: ConversationFileDeleteTarget[]
  isProcessing: boolean
  onClose: () => void
  onDelete: (
    nodeList: ConversationFileDeleteTarget[],
  ) => Promise<ConversationFileBatchResult>
  onComplete: (result: ConversationFileBatchResult) => void
}

/**
 * 确认删除混合选择的会话文件和目录.
 * @param props 删除目标、处理状态和结果回调
 * @return 批量删除确认弹窗
 */
export const ConversationFileBatchDeleteDialog = ({
  nodeList,
  isProcessing,
  onClose,
  onDelete,
  onComplete,
}: ConversationFileBatchDeleteDialogProps) => {
  if (nodeList.length === 0) {
    return null
  }

  const folderCount = nodeList.filter((node) => node.type === 'folder').length
  const fileCount = nodeList.length - folderCount
  const selectionDescription = [
    fileCount > 0 ? `${fileCount} 个文件` : '',
    folderCount > 0 ? `${folderCount} 个文件夹` : '',
  ]
    .filter(Boolean)
    .join('、')

  /**
   * 提交批量删除并把失败节点交回目录树保留选择.
   * @return void
   */
  const submitDelete = async (): Promise<void> => {
    const result = await onDelete(nodeList)
    onComplete(result)
    onClose()
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
        closeLabel="关闭批量删除确认"
        className="w-[min(400px,calc(100vw-32px))] p-5"
      >
        <DialogTitle className="pr-8 text-base font-semibold">
          删除所选 {nodeList.length} 项？
        </DialogTitle>
        <DialogDescription className="text-muted-foreground mt-1 text-xs leading-5">
          已选择 {selectionDescription}。删除后无法恢复。
        </DialogDescription>
        {folderCount > 0 ? (
          <p className="text-muted-foreground mt-3 text-xs leading-5">
            删除文件夹时，其中的子目录和文件也会被永久删除。
          </p>
        ) : null}
        <div className="mt-5 flex justify-end gap-2">
          <Button
            type="button"
            variant="outline"
            disabled={isProcessing}
            onClick={onClose}
          >
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
            ) : (
              <Trash2 aria-hidden />
            )}
            删除所选项
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  )
}
