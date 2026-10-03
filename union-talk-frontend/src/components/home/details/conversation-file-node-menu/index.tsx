import {
  Download,
  Eye,
  FilePenLine,
  FolderInput,
  FolderPlus,
  Pencil,
  Trash2,
  Upload,
} from 'lucide-react'

import {
  ContextMenuContent,
  ContextMenuItem,
  ContextMenuSeparator,
} from '@/components/shadcn-ui/context-menu'
import type { ConversationFileNode } from '@/hooks/use-conversation-file-tree'
import type { ConversationFileNodeActions } from '@/components/home/details/conversation-file-tree/file-tree-utils'

interface ConversationFileNodeMenuProps {
  node: ConversationFileNode
  actions: ConversationFileNodeActions
  disabled: boolean
  isSelected: boolean
  selectedNodeCount: number
  canDeleteSelection: boolean
}

/**
 * 渲染根目录、普通目录和文件各自允许的右键操作.
 * @param props 节点、操作回调和禁用状态
 * @return 节点右键菜单
 */
export const ConversationFileNodeMenu = ({
  node,
  actions,
  disabled,
  isSelected,
  selectedNodeCount,
  canDeleteSelection,
}: ConversationFileNodeMenuProps) => {
  const isMultiSelection = isSelected && selectedNodeCount > 1
  const isBatchDelete = isMultiSelection && canDeleteSelection
  const canCreateFolder =
    node.type === 'folder' && node.permissions.canCreateFolder
  const canUpload = node.type === 'folder' && node.permissions.canUpload
  const canPreview = node.type === 'file' && node.permissions.canPreview
  const canDownload = node.type === 'file' && node.permissions.canDownload
  const canMove = !isMultiSelection && node.permissions.canMove
  const canRename = !isMultiSelection && node.permissions.canRename
  const canDelete = isMultiSelection
    ? canDeleteSelection
    : node.permissions.canDelete
  const hasPrimaryAction =
    canCreateFolder || canUpload || canPreview || canDownload
  const hasManageAction = canMove || canRename

  if (!hasPrimaryAction && !hasManageAction && !canDelete) {
    return null
  }

  return (
    <ContextMenuContent className="w-44">
      {canCreateFolder ? (
        <ContextMenuItem
          disabled={disabled}
          onSelect={() => actions.onCreateFolder(node)}
        >
          <FolderPlus aria-hidden />
          创建目录
        </ContextMenuItem>
      ) : null}
      {canUpload ? (
        <ContextMenuItem
          disabled={disabled}
          onSelect={() => actions.onUploadFile(node)}
        >
          <Upload aria-hidden />
          上传文件
        </ContextMenuItem>
      ) : null}
      {canPreview ? (
        <ContextMenuItem
          disabled={disabled}
          onSelect={() => actions.onPreview(node)}
        >
          <Eye aria-hidden />
          预览
        </ContextMenuItem>
      ) : null}
      {canDownload ? (
        <ContextMenuItem
          disabled={disabled}
          onSelect={() => actions.onDownload(node)}
        >
          <Download aria-hidden />
          下载
        </ContextMenuItem>
      ) : null}
      {hasPrimaryAction && (hasManageAction || canDelete) ? (
        <ContextMenuSeparator />
      ) : null}
      {canMove ? (
        <ContextMenuItem
          disabled={disabled}
          onSelect={() => actions.onMove(node)}
        >
          <FolderInput aria-hidden />
          移动到
        </ContextMenuItem>
      ) : null}
      {canRename ? (
        <ContextMenuItem
          disabled={disabled}
          onSelect={() => actions.onRename(node)}
        >
          {node.type === 'file' ? (
            <FilePenLine aria-hidden />
          ) : (
            <Pencil aria-hidden />
          )}
          重命名
        </ContextMenuItem>
      ) : null}
      {hasManageAction && canDelete ? <ContextMenuSeparator /> : null}
      {canDelete ? (
        <ContextMenuItem
          variant="destructive"
          disabled={disabled}
          onSelect={() =>
            isBatchDelete ? actions.onDeleteSelection() : actions.onDelete(node)
          }
        >
          <Trash2 aria-hidden />
          {isBatchDelete ? `删除所选 ${selectedNodeCount} 项` : '删除'}
        </ContextMenuItem>
      ) : null}
    </ContextMenuContent>
  )
}
