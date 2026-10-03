import type { ConversationFileNode } from '@/hooks/use-conversation-file-tree'

export interface ConversationFileNodeActions {
  onPreview: (node: ConversationFileNode) => void
  onDownload: (node: ConversationFileNode) => void
  onCreateFolder: (node: ConversationFileNode) => void
  onUploadFile: (node: ConversationFileNode) => void
  onMove: (node: ConversationFileNode) => void
  onRename: (node: ConversationFileNode) => void
  onDelete: (node: ConversationFileNode) => void
  onDeleteSelection: () => void
}

interface ConversationFileSelectionInput {
  selectedNodeIdSet: Set<string>
  anchorNodeId?: string
  targetNodeId: string
  orderedSelectableNodeIdList: string[]
  isRangeSelection: boolean
  isToggleSelection: boolean
}

interface ConversationFileSelectionResult {
  selectedNodeIdSet: Set<string>
  anchorNodeId: string
}

/**
 * 根据点击修饰键计算目录树的下一组选中节点.
 * @param input 当前选择、锚点、可见顺序和修饰键状态
 * @return 下一组选中节点和范围选择锚点
 */
export const getNextConversationFileSelection = ({
  selectedNodeIdSet,
  anchorNodeId,
  targetNodeId,
  orderedSelectableNodeIdList,
  isRangeSelection,
  isToggleSelection,
}: ConversationFileSelectionInput): ConversationFileSelectionResult => {
  if (!orderedSelectableNodeIdList.includes(targetNodeId)) {
    return {
      selectedNodeIdSet: new Set(selectedNodeIdSet),
      anchorNodeId: anchorNodeId || targetNodeId,
    }
  }

  const anchorIndex = anchorNodeId
    ? orderedSelectableNodeIdList.indexOf(anchorNodeId)
    : -1
  const targetIndex = orderedSelectableNodeIdList.indexOf(targetNodeId)

  if (isRangeSelection && anchorIndex >= 0) {
    const nextSelectedNodeIdSet = isToggleSelection
      ? new Set(selectedNodeIdSet)
      : new Set<string>()
    const rangeStart = Math.min(anchorIndex, targetIndex)
    const rangeEnd = Math.max(anchorIndex, targetIndex)

    orderedSelectableNodeIdList
      .slice(rangeStart, rangeEnd + 1)
      .forEach((nodeId) => nextSelectedNodeIdSet.add(nodeId))

    return {
      selectedNodeIdSet: nextSelectedNodeIdSet,
      anchorNodeId: anchorNodeId || targetNodeId,
    }
  }

  if (isToggleSelection) {
    const nextSelectedNodeIdSet = new Set(selectedNodeIdSet)

    if (nextSelectedNodeIdSet.has(targetNodeId)) {
      nextSelectedNodeIdSet.delete(targetNodeId)
    } else {
      nextSelectedNodeIdSet.add(targetNodeId)
    }

    return {
      selectedNodeIdSet: nextSelectedNodeIdSet,
      anchorNodeId: targetNodeId,
    }
  }

  return {
    selectedNodeIdSet: new Set([targetNodeId]),
    anchorNodeId: targetNodeId,
  }
}

/**
 * 按当前展开状态返回可见且可选择的目录树节点.
 * @param rootNodeList 根目录节点列表
 * @param expandedFolderIdSet 已展开目录节点标识集合
 * @return 当前可见节点列表
 */
export const flattenVisibleConversationFileNodeList = (
  rootNodeList: ConversationFileNode[],
  expandedFolderIdSet: Set<string>,
): ConversationFileNode[] => {
  const visibleNodeList: ConversationFileNode[] = []
  const appendVisibleNode = (node: ConversationFileNode): void => {
    if (!node.isRoot) {
      visibleNodeList.push(node)
    }

    if (
      node.type === 'folder' &&
      (node.isRoot || expandedFolderIdSet.has(node.id))
    ) {
      node.children?.forEach(appendVisibleNode)
    }
  }

  rootNodeList.forEach(appendVisibleNode)
  return visibleNodeList
}

/**
 * 收敛批量删除目标，过滤已选目录下重复选择的后代节点.
 * @param rootNodeList 根目录节点列表
 * @param selectedNodeIdSet 已选择节点标识集合
 * @return 需要发送删除请求的节点列表
 */
export const getConversationFileDeleteTargetList = (
  rootNodeList: ConversationFileNode[],
  selectedNodeIdSet: Set<string>,
): ConversationFileNode[] => {
  const allNodeList: ConversationFileNode[] = []
  const appendNode = (node: ConversationFileNode): void => {
    allNodeList.push(node)
    node.children?.forEach(appendNode)
  }

  rootNodeList.forEach(appendNode)
  const resourceNodeMap = new Map(
    allNodeList.map((node) => [node.resourceId, node]),
  )

  return allNodeList.filter((node) => {
    if (node.isRoot || !selectedNodeIdSet.has(node.id)) {
      return false
    }

    let parentResourceId = node.parentResourceId
    const visitedParentResourceIdSet = new Set<string>()
    while (
      parentResourceId &&
      !visitedParentResourceIdSet.has(parentResourceId)
    ) {
      visitedParentResourceIdSet.add(parentResourceId)
      const parentNode = resourceNodeMap.get(parentResourceId)

      if (!parentNode) {
        break
      }
      if (
        parentNode.type === 'folder' &&
        selectedNodeIdSet.has(parentNode.id)
      ) {
        return false
      }
      parentResourceId = parentNode.parentResourceId
    }

    return true
  })
}

/**
 * 判断文件或目录是否可以移动到指定目录.
 * @param source 待移动节点
 * @param target 目标目录
 * @return 是否为有效移动目标
 */
export const isValidFileMoveTarget = (
  source: ConversationFileNode,
  target: ConversationFileNode,
): boolean => {
  if (
    !source.permissions.canMove ||
    target.type !== 'folder' ||
    !target.permissions.canAcceptMove ||
    source.isRoot
  ) {
    return false
  }

  if (
    source.resourceId === target.resourceId ||
    source.parentResourceId === target.resourceId
  ) {
    return false
  }

  if (source.type === 'file') {
    return true
  }

  const containsTarget = (node: ConversationFileNode): boolean =>
    (node.children || []).some(
      (childNode) =>
        childNode.type === 'folder' &&
        (childNode.resourceId === target.resourceId ||
          containsTarget(childNode)),
    )

  return !containsTarget(source)
}

/**
 * 判断一组文件树目标是否全部允许删除.
 * @param nodeList 已收敛的删除目标列表
 * @return 是否允许整组批量删除
 */
export const canDeleteConversationFileNodeList = (
  nodeList: ConversationFileNode[],
): boolean =>
  nodeList.length > 0 && nodeList.every((node) => node.permissions.canDelete)

/**
 * 获取文件重命名时允许编辑的主文件名.
 * @param node 文件节点
 * @return 不包含固定扩展名的文件名
 */
export const getEditableFileName = (node: ConversationFileNode): string => {
  if (!node.fileExt) {
    return node.name
  }

  const suffix = `.${node.fileExt}`
  return node.name.toLowerCase().endsWith(suffix.toLowerCase())
    ? node.name.slice(0, -suffix.length)
    : node.name
}

/**
 * 展开目录树中所有目录供目标选择使用.
 * @param rootNode 根目录
 * @return 按树形顺序排列的目录和层级
 */
export const flattenFolderList = (
  rootNode: ConversationFileNode,
): Array<{ node: ConversationFileNode; depth: number }> => {
  const folderList: Array<{ node: ConversationFileNode; depth: number }> = []

  const appendFolder = (node: ConversationFileNode, depth: number): void => {
    if (node.type !== 'folder') {
      return
    }

    folderList.push({ node, depth })
    for (const childNode of node.children || []) {
      appendFolder(childNode, depth + 1)
    }
  }

  appendFolder(rootNode, 0)
  return folderList
}
