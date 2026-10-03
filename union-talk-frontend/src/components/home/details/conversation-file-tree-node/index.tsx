import { useCallback, type MouseEvent } from 'react'
import { useDraggable, useDroppable } from '@dnd-kit/react'
import { LoaderCircle } from 'lucide-react'

import { ConversationFileNodeMenu } from '@/components/home/details/conversation-file-node-menu'
import { ConversationFileIcon } from '@/components/home/details/conversation-file-icon'
import {
  ContextMenu,
  ContextMenuTrigger,
} from '@/components/shadcn-ui/context-menu'
import { File, Folder } from '@/components/shadcn-ui/file-tree'
import type { ConversationFileNode } from '@/hooks/use-conversation-file-tree'
import { cn } from '@/utils/class-name'
import {
  isValidFileMoveTarget,
  type ConversationFileNodeActions,
} from '@/components/home/details/conversation-file-tree/file-tree-utils'

interface ConversationFileTreeNodeProps {
  node: ConversationFileNode
  actions: ConversationFileNodeActions
  isProcessing: boolean
  selectedNodeIdSet: Set<string>
  loadingFolderIdSet: Set<string>
  failedFolderIdSet: Set<string>
  canDeleteSelection: boolean
  onSelectNode: (
    node: ConversationFileNode,
    event: MouseEvent<HTMLButtonElement>,
  ) => void
  onToggleFolder: (node: ConversationFileNode) => void
  onContextMenuNode: (node: ConversationFileNode) => void
}

/**
 * 渲染具备右键菜单和拖拽能力的单个会话文件树节点.
 * @param props 节点、菜单动作和处理状态
 * @return 文件或目录树节点
 */
export const ConversationFileTreeNode = ({
  node,
  actions,
  isProcessing,
  selectedNodeIdSet,
  loadingFolderIdSet,
  failedFolderIdSet,
  canDeleteSelection,
  onSelectNode,
  onToggleFolder,
  onContextMenuNode,
}: ConversationFileTreeNodeProps) => {
  const { ref: draggableRef, isDragging } = useDraggable({
    id: `drag:${node.id}`,
    data: { node },
    disabled: !node.permissions.canMove || isProcessing,
  })
  const { ref: droppableRef, isDropTarget } = useDroppable({
    id: `drop:${node.id}`,
    data: { node },
    disabled:
      node.type !== 'folder' || !node.permissions.canAcceptMove || isProcessing,
    accept: (source) =>
      isValidFileMoveTarget(source.data.node as ConversationFileNode, node),
  })
  const setFolderTriggerRef = useCallback(
    (element: HTMLButtonElement | null) => {
      draggableRef(element)
      droppableRef(element)
    },
    [draggableRef, droppableRef],
  )
  const isSelected = selectedNodeIdSet.has(node.id)

  if (node.type === 'folder') {
    const isFolderLoading = loadingFolderIdSet.has(node.resourceId)
    const isFolderLoadFailed = failedFolderIdSet.has(node.resourceId)

    return (
      <ContextMenu>
        <Folder
          value={node.id}
          element={node.name}
          triggerRef={setFolderTriggerRef}
          isSelect={isSelected}
          onTriggerSelect={(event) => {
            onToggleFolder(node)
            if (!node.isRoot) {
              onSelectNode(node, event)
            }
          }}
          onTriggerContextMenu={() => {
            if (!node.isRoot) {
              onContextMenuNode(node)
            }
          }}
          className={cn(
            'min-h-8 w-full px-2 py-1 text-left transition-colors',
            isDropTarget && 'bg-primary/10 text-primary ring-primary/25 ring-1',
            isDragging && 'opacity-40',
          )}
          renderTrigger={(trigger) => (
            <ContextMenuTrigger asChild>{trigger}</ContextMenuTrigger>
          )}
        >
          {(node.children || []).map((childNode) => (
            <ConversationFileTreeNode
              key={childNode.id}
              node={childNode}
              actions={actions}
              isProcessing={isProcessing}
              selectedNodeIdSet={selectedNodeIdSet}
              loadingFolderIdSet={loadingFolderIdSet}
              failedFolderIdSet={failedFolderIdSet}
              canDeleteSelection={canDeleteSelection}
              onSelectNode={onSelectNode}
              onToggleFolder={onToggleFolder}
              onContextMenuNode={onContextMenuNode}
            />
          ))}
          {isFolderLoading ? (
            <div
              role="status"
              className="text-muted-foreground flex min-h-8 items-center gap-2 px-2 text-xs"
            >
              <LoaderCircle aria-hidden className="size-3.5 animate-spin" />
              正在加载
            </div>
          ) : isFolderLoadFailed ? (
            <div
              role="alert"
              className="text-destructive min-h-8 px-2 text-xs leading-8"
            >
              加载失败，折叠后重试
            </div>
          ) : null}
        </Folder>
        <ConversationFileNodeMenu
          node={node}
          actions={actions}
          disabled={isProcessing}
          isSelected={isSelected}
          selectedNodeCount={selectedNodeIdSet.size}
          canDeleteSelection={canDeleteSelection}
        />
      </ContextMenu>
    )
  }

  return (
    <ContextMenu>
      <ContextMenuTrigger asChild>
        <File
          ref={draggableRef}
          value={node.id}
          fileIcon={<ConversationFileIcon file={node} />}
          isSelect={isSelected}
          className={cn(
            'min-h-8 w-full px-2 py-1 text-left transition-colors',
            isDragging && 'opacity-40',
          )}
          onDoubleClick={(event) => {
            event.preventDefault()
            if (node.permissions.canPreview) {
              actions.onPreview(node)
            }
          }}
          onClick={(event) => onSelectNode(node, event)}
          onContextMenu={() => onContextMenuNode(node)}
        >
          <span className="truncate">{node.name}</span>
        </File>
      </ContextMenuTrigger>
      <ConversationFileNodeMenu
        node={node}
        actions={actions}
        disabled={isProcessing}
        isSelected={isSelected}
        selectedNodeCount={selectedNodeIdSet.size}
        canDeleteSelection={canDeleteSelection}
      />
    </ContextMenu>
  )
}
