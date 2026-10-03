import { useCallback, useEffect, useRef, useState } from 'react'

import type { TreeViewElement } from '@/components/shadcn-ui/file-tree'
import { getFileList, getTree } from '@/services/generated/file'
import type {
  AssetFileRespVO,
  AssetFilePermissionRespVO,
  AssetFolderPermissionRespVO,
  AssetFolderTreeRespVO,
} from '@/services/generated/file/models'

interface ConversationFileNodeBase extends Omit<
  TreeViewElement,
  'children' | 'type'
> {
  resourceId: string
  parentResourceId?: string
  isRoot?: boolean
  fileExt?: string
  fileType?: string
  fileSize?: string
  mimeType?: string
  children?: ConversationFileNode[]
}

export interface ConversationFolderPermissions {
  canView: boolean
  canUpload: boolean
  canCreateFolder: boolean
  canRename: boolean
  canMove: boolean
  canDelete: boolean
  canAcceptMove: boolean
}

export interface ConversationAssetFilePermissions {
  canView: boolean
  canPreview: boolean
  canDownload: boolean
  canRename: boolean
  canMove: boolean
  canDelete: boolean
}

export interface ConversationFolderNode extends ConversationFileNodeBase {
  type: 'folder'
  folderType?: string
  ownerUserId?: string
  permissions: ConversationFolderPermissions
}

export interface ConversationAssetFileNode extends ConversationFileNodeBase {
  type: 'file'
  permissions: ConversationAssetFilePermissions
}

export type ConversationFileNode =
  ConversationFolderNode | ConversationAssetFileNode

interface ConversationFileTreeState {
  conversationId: string
  elementList: ConversationFileNode[]
  expandedFolderIdList: string[]
  isLoading: boolean
  errorMessage: string
}

interface ConversationFileTreeResult extends ConversationFileTreeState {
  refresh: () => void
  loadFolder: (folderId: string) => Promise<boolean>
  loadingFolderIdSet: Set<string>
  failedFolderIdSet: Set<string>
}

interface ConversationFolderLoadState {
  conversationId: string
  loadingFolderIdSet: Set<string>
  failedFolderIdSet: Set<string>
}

interface ConversationFolderCache {
  conversationId: string
  loadedFolderIdSet: Set<string>
  folderRequestMap: Map<string, Promise<boolean>>
}

const createLoadingFileTreeState = (
  conversationId: string,
): ConversationFileTreeState => ({
  conversationId,
  elementList: [],
  expandedFolderIdList: [],
  isLoading: true,
  errorMessage: '',
})

const createFolderLoadState = (
  conversationId: string,
): ConversationFolderLoadState => ({
  conversationId,
  loadingFolderIdSet: new Set(),
  failedFolderIdSet: new Set(),
})

const createFolderCache = (
  conversationId: string,
): ConversationFolderCache => ({
  conversationId,
  loadedFolderIdSet: new Set(),
  folderRequestMap: new Map(),
})

const isFolderTreeNode = (value: unknown): value is AssetFolderTreeRespVO =>
  typeof value === 'object' && value !== null && !Array.isArray(value)

/**
 * 将后端目录能力转换为严格布尔权限.
 * @param permissions 后端目录权限
 * @return 前端目录权限
 */
const mapFolderPermissions = (
  permissions?: AssetFolderPermissionRespVO,
): ConversationFolderPermissions => ({
  canView: permissions?.canView === true,
  canUpload: permissions?.canUpload === true,
  canCreateFolder: permissions?.canCreateFolder === true,
  canRename: permissions?.canRename === true,
  canMove: permissions?.canMove === true,
  canDelete: permissions?.canDelete === true,
  canAcceptMove: permissions?.canAcceptMove === true,
})

/**
 * 将后端文件能力转换为严格布尔权限.
 * @param permissions 后端文件权限
 * @return 前端文件权限
 */
const mapFilePermissions = (
  permissions?: AssetFilePermissionRespVO,
): ConversationAssetFilePermissions => ({
  canView: permissions?.canView === true,
  canPreview: permissions?.canPreview === true,
  canDownload: permissions?.canDownload === true,
  canRename: permissions?.canRename === true,
  canMove: permissions?.canMove === true,
  canDelete: permissions?.canDelete === true,
})

/**
 * 将生成接口中的 unknown 子目录转换为可遍历的目录节点.
 * @param folder 当前目录
 * @return 有效子目录列表
 */
const getChildFolderList = (
  folder: AssetFolderTreeRespVO,
): AssetFolderTreeRespVO[] =>
  (folder.childFolderList || [])
    .filter(isFolderTreeNode)
    .filter((childFolder) => childFolder.permissions?.canView === true)

/**
 * 收集根目录下所有拥有服务端 id 的子目录.
 * @param folder 当前目录
 * @return 扁平子目录列表
 */
const collectChildFolderList = (
  folder: AssetFolderTreeRespVO,
): AssetFolderTreeRespVO[] =>
  getChildFolderList(folder).flatMap((childFolder) => [
    childFolder,
    ...collectChildFolderList(childFolder),
  ])

/**
 * 将文件响应转换为 FileTree 文件节点.
 * @param file 文件响应
 * @param fallbackId 无文件 id 时的稳定回退标识
 * @return FileTree 文件节点
 */
const mapFileElement = (
  file: AssetFileRespVO,
  fallbackId: string,
  parentResourceId: string,
): ConversationAssetFileNode => {
  const resourceId = file.id || fallbackId

  return {
    id: `file:${resourceId}`,
    resourceId,
    parentResourceId,
    name: file.name || '未命名文件',
    type: 'file',
    fileExt: file.fileExt,
    fileType: file.fileType,
    fileSize: file.fileSize,
    mimeType: file.mimeType,
    permissions: mapFilePermissions(file.permissions),
  }
}

/**
 * 将目录响应和按目录分组的文件转换为 FileTree 目录节点.
 * @param folder 目录响应
 * @param fileListMap 目录 id 对应的文件列表
 * @param path 当前目录路径，用于生成回退标识
 * @param expandedFolderIdList 默认展开目录标识集合
 * @return FileTree 目录节点
 */
const mapFolderElement = (
  folder: AssetFolderTreeRespVO,
  fileListMap: Map<string, AssetFileRespVO[]>,
  path: string,
  expandedFolderIdList: string[],
  isRoot = false,
): ConversationFolderNode => {
  const folderKey = folder.id || path
  const elementId = `folder:${folderKey}`
  const childFolderElementList = getChildFolderList(folder).map(
    (childFolder, index) =>
      mapFolderElement(
        childFolder,
        fileListMap,
        `${path}.${index}`,
        expandedFolderIdList,
      ),
  )
  const fileElementList = (fileListMap.get(folder.id || '') || [])
    .filter((file) => file.permissions?.canView === true)
    .map((file, index) =>
      mapFileElement(file, `${folderKey}.${index}`, folderKey),
    )

  if (isRoot) {
    expandedFolderIdList.push(elementId)
  }

  return {
    id: elementId,
    resourceId: folderKey,
    parentResourceId: folder.parentId,
    name: folder.name || '未命名目录',
    type: 'folder',
    isRoot,
    folderType: folder.folderType,
    ownerUserId: folder.ownerUserId,
    permissions: mapFolderPermissions(folder.permissions),
    children: [...childFolderElementList, ...fileElementList],
  }
}

/**
 * 使用指定目录的最新文件替换文件树中的旧文件节点.
 * @param nodeList 当前文件树节点列表
 * @param folderId 目标目录标识
 * @param fileList 最新文件列表
 * @return 更新后的文件树节点列表
 */
const replaceFolderFileList = (
  nodeList: ConversationFileNode[],
  folderId: string,
  fileList: AssetFileRespVO[],
): ConversationFileNode[] =>
  nodeList.map((node) => {
    if (node.type !== 'folder') {
      return node
    }

    if (node.resourceId === folderId) {
      const childFolderList = (node.children || []).filter(
        (childNode) => childNode.type === 'folder',
      )

      return {
        ...node,
        children: [
          ...childFolderList,
          ...fileList
            .filter((file) => file.permissions?.canView === true)
            .map((file, index) =>
              mapFileElement(file, `${folderId}.${index}`, folderId),
            ),
        ],
      }
    }

    return {
      ...node,
      children: replaceFolderFileList(node.children || [], folderId, fileList),
    }
  })

/**
 * 按需加载指定会话的目录树和用户展开目录下的文件.
 * @param conversationId 会话标识
 * @return FileTree 数据和请求状态
 */
export const useConversationFileTree = (
  conversationId: string,
): ConversationFileTreeResult => {
  const [state, setState] = useState<ConversationFileTreeState>(() =>
    createLoadingFileTreeState(conversationId),
  )
  const [folderLoadState, setFolderLoadState] =
    useState<ConversationFolderLoadState>(() =>
      createFolderLoadState(conversationId),
    )
  const folderCacheRef = useRef<ConversationFolderCache>(
    createFolderCache(conversationId),
  )
  const [requestVersion, setRequestVersion] = useState(0)

  const refresh = useCallback(() => {
    setRequestVersion((currentVersion) => currentVersion + 1)
  }, [])

  /**
   * 首次展开普通目录时加载文件，并复用进行中或已完成的请求.
   * @param folderId 目标目录标识
   * @return 是否加载成功
   */
  const loadFolder = useCallback(
    (folderId: string): Promise<boolean> => {
      let folderCache = folderCacheRef.current

      if (folderCache.conversationId !== conversationId) {
        folderCache = createFolderCache(conversationId)
        folderCacheRef.current = folderCache
      }

      if (folderCache.loadedFolderIdSet.has(folderId)) {
        return Promise.resolve(true)
      }

      const activeRequest = folderCache.folderRequestMap.get(folderId)
      if (activeRequest) {
        return activeRequest
      }

      setFolderLoadState((currentState) => {
        if (currentState.conversationId !== conversationId) {
          return {
            conversationId,
            loadingFolderIdSet: new Set([folderId]),
            failedFolderIdSet: new Set(),
          }
        }

        const failedFolderIdSet = new Set(currentState.failedFolderIdSet)
        failedFolderIdSet.delete(folderId)

        return {
          ...currentState,
          loadingFolderIdSet: new Set(currentState.loadingFolderIdSet).add(
            folderId,
          ),
          failedFolderIdSet,
        }
      })

      const request = getFileList({ conversationId, folderId })
        .then((fileList) => {
          if (folderCacheRef.current !== folderCache) {
            return false
          }

          folderCache.loadedFolderIdSet.add(folderId)
          setState((currentState) => {
            if (currentState.conversationId !== conversationId) {
              return currentState
            }

            return {
              ...currentState,
              elementList: replaceFolderFileList(
                currentState.elementList,
                folderId,
                fileList,
              ),
            }
          })
          return true
        })
        .catch(() => {
          if (folderCacheRef.current === folderCache) {
            setFolderLoadState((currentState) => {
              if (currentState.conversationId !== conversationId) {
                return currentState
              }

              return {
                ...currentState,
                failedFolderIdSet: new Set(currentState.failedFolderIdSet).add(
                  folderId,
                ),
              }
            })
          }
          return false
        })
        .finally(() => {
          if (folderCacheRef.current !== folderCache) {
            return
          }

          folderCache.folderRequestMap.delete(folderId)
          setFolderLoadState((currentState) => {
            if (currentState.conversationId !== conversationId) {
              return currentState
            }

            const loadingFolderIdSet = new Set(currentState.loadingFolderIdSet)
            loadingFolderIdSet.delete(folderId)

            return { ...currentState, loadingFolderIdSet }
          })
        })

      folderCache.folderRequestMap.set(folderId, request)
      return request
    },
    [conversationId],
  )

  useEffect(() => {
    let isCurrentRequest = true
    let folderCache = folderCacheRef.current

    if (folderCache.conversationId !== conversationId) {
      folderCache = createFolderCache(conversationId)
      folderCacheRef.current = folderCache
    }

    void Promise.all([
      getTree({ conversationId }),
      getFileList({ conversationId }),
    ])
      .then(async ([rootFolder, rootFileList]) => {
        if (rootFolder.permissions?.canView !== true) {
          if (isCurrentRequest && folderCacheRef.current === folderCache) {
            folderCache.loadedFolderIdSet = new Set()
            setState({
              conversationId,
              elementList: [],
              expandedFolderIdList: [],
              isLoading: false,
              errorMessage: '',
            })
            setFolderLoadState(createFolderLoadState(conversationId))
          }
          return
        }

        const childFolderIdSet = new Set(
          collectChildFolderList(rootFolder)
            .map((folder) => folder.id)
            .filter((folderId): folderId is string => Boolean(folderId)),
        )
        const loadedFolderIdList = Array.from(
          folderCache.loadedFolderIdSet,
        ).filter((folderId) => childFolderIdSet.has(folderId))
        const loadedFolderResultList = await Promise.allSettled(
          loadedFolderIdList.map((folderId) =>
            getFileList({ conversationId, folderId }),
          ),
        )
        const fileListMap = new Map<string, AssetFileRespVO[]>([
          [rootFolder.id || '', rootFileList],
        ])
        const nextLoadedFolderIdSet = new Set<string>()
        const failedFolderIdSet = new Set<string>()

        loadedFolderResultList.forEach((result, index) => {
          const folderId = loadedFolderIdList[index]
          if (result.status === 'fulfilled') {
            fileListMap.set(folderId, result.value)
            nextLoadedFolderIdSet.add(folderId)
          } else {
            failedFolderIdSet.add(folderId)
          }
        })
        const expandedFolderIdList: string[] = []
        const rootElement = mapFolderElement(
          {
            ...rootFolder,
            name: rootFolder.name || '群文件',
          },
          fileListMap,
          `root:${conversationId}`,
          expandedFolderIdList,
          true,
        )

        if (isCurrentRequest && folderCacheRef.current === folderCache) {
          folderCache.loadedFolderIdSet = nextLoadedFolderIdSet
          setState({
            conversationId,
            elementList: [rootElement],
            expandedFolderIdList,
            isLoading: false,
            errorMessage: '',
          })
          setFolderLoadState({
            conversationId,
            loadingFolderIdSet: new Set(),
            failedFolderIdSet,
          })
        }
      })
      .catch(() => {
        if (isCurrentRequest && folderCacheRef.current === folderCache) {
          setState({
            conversationId,
            elementList: [],
            expandedFolderIdList: [],
            isLoading: false,
            errorMessage: '文件列表加载失败，请稍后重试',
          })
          setFolderLoadState(createFolderLoadState(conversationId))
        }
      })

    return () => {
      isCurrentRequest = false
    }
  }, [conversationId, requestVersion])

  const currentState =
    state.conversationId === conversationId
      ? state
      : createLoadingFileTreeState(conversationId)
  const currentFolderLoadState =
    folderLoadState.conversationId === conversationId
      ? folderLoadState
      : createFolderLoadState(conversationId)

  return {
    ...currentState,
    ...currentFolderLoadState,
    refresh,
    loadFolder,
  }
}
