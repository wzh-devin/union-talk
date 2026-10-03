import {
  useEffect,
  useMemo,
  useRef,
  useState,
  type ChangeEvent,
  type MouseEvent,
} from 'react'
import {
  DragDropProvider,
  DragOverlay,
  type DragEndEvent,
} from '@dnd-kit/react'
import { FolderIcon, LoaderCircle } from 'lucide-react'

import { ConversationFileBatchDeleteDialog } from '@/components/home/details/conversation-file-batch-delete-dialog'
import { ConversationFileIcon } from '@/components/home/details/conversation-file-icon'
import { ConversationFilePreviewDialog } from '@/components/home/details/conversation-file-preview-dialog'
import {
  ConversationFileOperationDialog,
  type ConversationFileOperation,
} from '@/components/home/details/conversation-file-operation-dialog'
import { ConversationFileTreeNode } from '@/components/home/details/conversation-file-tree-node'
import { Tree } from '@/components/shadcn-ui/file-tree'
import { useConversationFileManager } from '@/hooks/use-conversation-file-manager'
import { useConversationFileTree } from '@/hooks/use-conversation-file-tree'
import type { ConversationFileNode } from '@/hooks/use-conversation-file-tree'
import {
  canDeleteConversationFileNodeList,
  flattenVisibleConversationFileNodeList,
  getConversationFileDeleteTargetList,
  getNextConversationFileSelection,
  isValidFileMoveTarget,
  type ConversationFileNodeActions,
} from './file-tree-utils'

interface ConversationFileTreeProps {
  conversationId: string
  fileRefreshVersion?: number
}

interface ConversationFileSelectionState {
  conversationId: string
  selectedNodeIdSet: Set<string>
  anchorNodeId?: string
}

interface ConversationFileExpansionState {
  conversationId: string
  isInitialized: boolean
  expandedFolderIdSet: Set<string>
}

/**
 * 展示会话共享文件的目录树.
 * @param props 文件树属性
 * @return ReactElement 文件目录树
 */
export const ConversationFileTree = ({
  conversationId,
  fileRefreshVersion = 0,
}: ConversationFileTreeProps) => {
  const {
    elementList,
    expandedFolderIdList,
    isLoading,
    errorMessage,
    refresh,
    loadFolder,
    loadingFolderIdSet,
    failedFolderIdSet,
  } = useConversationFileTree(conversationId)
  const manager = useConversationFileManager({ conversationId, refresh })
  const externalRefreshStateRef = useRef({
    conversationId,
    version: fileRefreshVersion,
  })
  const [operation, setOperation] = useState<ConversationFileOperation | null>(
    null,
  )
  const [uploadTarget, setUploadTarget] = useState<ConversationFileNode | null>(
    null,
  )
  const [previewFile, setPreviewFile] = useState<ConversationFileNode | null>(
    null,
  )
  const [selectionState, setSelectionState] =
    useState<ConversationFileSelectionState>(() => ({
      conversationId,
      selectedNodeIdSet: new Set(),
    }))
  const [expansionState, setExpansionState] =
    useState<ConversationFileExpansionState>(() => ({
      conversationId,
      isInitialized: expandedFolderIdList.length > 0,
      expandedFolderIdSet: new Set(expandedFolderIdList),
    }))
  const [isBatchDeleteOpen, setIsBatchDeleteOpen] = useState(false)
  const fileInputRef = useRef<HTMLInputElement>(null)
  const rootNode = elementList[0]
  const isProcessing = manager.processingAction !== null

  useEffect(() => {
    const previousState = externalRefreshStateRef.current
    if (previousState.conversationId !== conversationId) {
      externalRefreshStateRef.current = {
        conversationId,
        version: fileRefreshVersion,
      }
      return
    }
    if (previousState.version === fileRefreshVersion) {
      return
    }

    externalRefreshStateRef.current = {
      conversationId,
      version: fileRefreshVersion,
    }
    const refreshTimer = setTimeout(refresh, 120)
    return () => clearTimeout(refreshTimer)
  }, [conversationId, fileRefreshVersion, refresh])

  const availableNodeIdSet = useMemo(() => {
    const nodeIdSet = new Set<string>()
    const appendNodeId = (node: ConversationFileNode): void => {
      nodeIdSet.add(node.id)
      node.children?.forEach(appendNodeId)
    }

    elementList.forEach(appendNodeId)
    return nodeIdSet
  }, [elementList])
  const selectedNodeIdSet = useMemo(() => {
    if (selectionState.conversationId !== conversationId) {
      return new Set<string>()
    }

    return new Set(
      Array.from(selectionState.selectedNodeIdSet).filter((nodeId) =>
        availableNodeIdSet.has(nodeId),
      ),
    )
  }, [availableNodeIdSet, conversationId, selectionState])
  const selectionAnchorNodeId =
    selectionState.conversationId === conversationId
      ? selectionState.anchorNodeId
      : undefined
  const expandedFolderIdSet = useMemo(
    () =>
      expansionState.conversationId === conversationId &&
      expansionState.isInitialized
        ? expansionState.expandedFolderIdSet
        : new Set(expandedFolderIdList),
    [conversationId, expandedFolderIdList, expansionState],
  )
  const visibleNodeList = useMemo(
    () =>
      flattenVisibleConversationFileNodeList(elementList, expandedFolderIdSet),
    [elementList, expandedFolderIdSet],
  )
  const deleteTargetList = useMemo(
    () => getConversationFileDeleteTargetList(elementList, selectedNodeIdSet),
    [elementList, selectedNodeIdSet],
  )
  const canDeleteSelection =
    deleteTargetList.length > 1 &&
    canDeleteConversationFileNodeList(deleteTargetList)
  const actions = useMemo<ConversationFileNodeActions>(
    () => ({
      onPreview: (node) => setPreviewFile(node),
      onDownload: (node) => void manager.downloadFile(node.resourceId),
      onCreateFolder: (node) => setOperation({ type: 'create', node }),
      onUploadFile: (node) => {
        setUploadTarget(node)
        window.setTimeout(() => fileInputRef.current?.click())
      },
      onMove: (node) => setOperation({ type: 'move', node }),
      onRename: (node) => setOperation({ type: 'rename', node }),
      onDelete: (node) => setOperation({ type: 'delete', node }),
      onDeleteSelection: () => setIsBatchDeleteOpen(true),
    }),
    [manager],
  )

  /**
   * 根据鼠标修饰键更新文件树选择集合.
   * @param node 被点击的文件或目录节点
   * @param event 节点点击事件
   * @return void
   */
  const handleSelectNode = (
    node: ConversationFileNode,
    event: MouseEvent<HTMLButtonElement>,
  ): void => {
    const nextSelection = getNextConversationFileSelection({
      selectedNodeIdSet,
      anchorNodeId: selectionAnchorNodeId,
      targetNodeId: node.id,
      orderedSelectableNodeIdList: visibleNodeList.map(
        (visibleNode) => visibleNode.id,
      ),
      isRangeSelection: event.shiftKey,
      isToggleSelection: event.metaKey || event.ctrlKey,
    })

    setSelectionState({ conversationId, ...nextSelection })
  }

  /**
   * 同步目录展开状态供范围选择计算当前可见顺序.
   * @param node 被切换的目录节点
   * @return void
   */
  const handleToggleFolder = (node: ConversationFileNode): void => {
    const nextExpandedFolderIdSet = new Set(expandedFolderIdSet)
    const isExpanded = nextExpandedFolderIdSet.has(node.id)

    if (isExpanded) {
      nextExpandedFolderIdSet.delete(node.id)
    } else {
      nextExpandedFolderIdSet.add(node.id)
    }
    setExpansionState({
      conversationId,
      isInitialized: true,
      expandedFolderIdSet: nextExpandedFolderIdSet,
    })

    if (!isExpanded && !node.isRoot) {
      void loadFolder(node.resourceId)
    }
  }

  /**
   * 右键未选节点时收敛为单选，右键已选节点时保留整组选择.
   * @param node 右键目标节点
   * @return void
   */
  const handleContextMenuNode = (node: ConversationFileNode): void => {
    if (selectedNodeIdSet.has(node.id)) {
      return
    }

    setSelectionState({
      conversationId,
      selectedNodeIdSet: new Set([node.id]),
      anchorNodeId: node.id,
    })
  }

  /**
   * 批量删除完成后只保留失败节点的选择状态.
   * @param result 批量删除结果
   * @return void
   */
  const handleBatchDeleteComplete = (result: {
    failedIdList: string[]
  }): void => {
    setSelectionState({
      conversationId,
      selectedNodeIdSet: new Set(result.failedIdList),
      anchorNodeId: result.failedIdList[0],
    })
  }

  const moveNode = async (
    sourceNode: ConversationFileNode,
    targetNode: ConversationFileNode,
  ): Promise<void> => {
    if (!isValidFileMoveTarget(sourceNode, targetNode)) {
      return
    }

    if (sourceNode.type === 'folder') {
      await manager.moveFolder(sourceNode.resourceId, targetNode.resourceId)
      return
    }

    await manager.moveFile(sourceNode.resourceId, targetNode.resourceId)
  }

  const handleDragEnd = (event: DragEndEvent): void => {
    if (event.canceled) {
      return
    }

    const sourceNode = event.operation.source?.data.node as
      ConversationFileNode | undefined
    const targetNode = event.operation.target?.data.node as
      ConversationFileNode | undefined

    if (sourceNode && targetNode) {
      void moveNode(sourceNode, targetNode)
    }
  }

  const handleFileChange = (event: ChangeEvent<HTMLInputElement>): void => {
    const fileList = Array.from(event.target.files || [])
    event.target.value = ''

    if (fileList.length === 0 || !uploadTarget) {
      return
    }

    void manager.uploadFileList(uploadTarget.resourceId, fileList)
  }

  if (isLoading) {
    return (
      <div
        className="text-muted-foreground flex h-full min-h-48 items-center justify-center gap-2 text-sm"
        role="status"
      >
        <LoaderCircle className="size-4 animate-spin" aria-hidden="true" />
        正在加载文件
      </div>
    )
  }

  if (errorMessage) {
    return (
      <div
        className="text-muted-foreground flex h-full min-h-48 items-center justify-center px-6 text-center text-sm"
        role="alert"
      >
        {errorMessage}
      </div>
    )
  }

  return (
    <>
      <DragDropProvider onDragEnd={handleDragEnd}>
        <div className="flex h-full min-h-48 flex-col">
          <Tree
            key={conversationId}
            aria-label="会话文件目录"
            initialExpandedItems={expandedFolderIdList}
            className="min-h-0 flex-1"
          >
            {elementList.map((node) => (
              <ConversationFileTreeNode
                key={node.id}
                node={node}
                actions={actions}
                isProcessing={isProcessing}
                selectedNodeIdSet={selectedNodeIdSet}
                loadingFolderIdSet={loadingFolderIdSet}
                failedFolderIdSet={failedFolderIdSet}
                canDeleteSelection={canDeleteSelection}
                onSelectNode={handleSelectNode}
                onToggleFolder={handleToggleFolder}
                onContextMenuNode={handleContextMenuNode}
              />
            ))}
          </Tree>
        </div>
        <DragOverlay dropAnimation={{ duration: 140, easing: 'ease-out' }}>
          {(source) => {
            const node = source.data.node as ConversationFileNode

            return (
              <div className="bg-popover text-popover-foreground flex max-w-56 items-center gap-2 rounded-md px-3 py-2 text-sm shadow-lg ring-1 ring-black/10">
                {node.type === 'folder' ? (
                  <FolderIcon aria-hidden className="size-4" />
                ) : (
                  <ConversationFileIcon file={node} />
                )}
                <span className="truncate">{node.name}</span>
              </div>
            )
          }}
        </DragOverlay>
      </DragDropProvider>

      <input
        ref={fileInputRef}
        type="file"
        multiple
        aria-label={`上传到${uploadTarget?.name || '当前目录'}`}
        className="sr-only"
        disabled={isProcessing}
        onChange={handleFileChange}
      />

      <ConversationFileOperationDialog
        key={operation ? `${operation.type}:${operation.node.id}` : 'closed'}
        operation={operation}
        rootNode={rootNode}
        isProcessing={isProcessing}
        onClose={() => setOperation(null)}
        onCreateFolder={manager.createFolder}
        onRenameFolder={manager.renameFolder}
        onMoveFolder={manager.moveFolder}
        onDeleteFolder={manager.deleteFolder}
        onRenameFile={manager.renameFile}
        onMoveFile={manager.moveFile}
        onDeleteFile={manager.deleteFile}
      />

      <ConversationFilePreviewDialog
        file={previewFile}
        onClose={() => setPreviewFile(null)}
        onDownload={
          previewFile?.type === 'file' && previewFile.permissions.canDownload
            ? manager.downloadFile
            : undefined
        }
        isDownloading={manager.processingAction === 'download-file'}
      />

      <ConversationFileBatchDeleteDialog
        nodeList={isBatchDeleteOpen ? deleteTargetList : []}
        isProcessing={isProcessing}
        onClose={() => setIsBatchDeleteOpen(false)}
        onDelete={manager.deleteNodeList}
        onComplete={handleBatchDeleteComplete}
      />
    </>
  )
}
