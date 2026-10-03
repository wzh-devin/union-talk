import type { Section, WorkspaceSection } from '@/pages/home/model/types'

export interface HomeProfileRouteState {
  backgroundPath: string
}

export const homeRouteMap: Record<Section, string> = {
  notifications: '/home/notifications',
  friends: '/home/friends',
  groups: '/home/groups',
  profile: '/home/profile',
}

export const defaultHomeSection: WorkspaceSection = 'notifications'

const homeSectionList: Section[] = [
  'notifications',
  'friends',
  'groups',
  'profile',
]

/**
 * 根据首页路径解析当前工作区分区.
 * @param pathname 当前路径
 * @return Section 首页工作区分区
 */
export const getHomeSectionFromPath = (pathname: string): Section =>
  homeSectionList.find((section) => {
    const sectionPath = homeRouteMap[section]

    return pathname === sectionPath || pathname.startsWith(`${sectionPath}/`)
  }) ?? defaultHomeSection

/**
 * 将路径解析为可渲染的 Home 工作区分区.
 * @param pathname 当前路径
 * @return WorkspaceSection Home 工作区分区
 */
export const getHomeWorkspaceSectionFromPath = (
  pathname: string,
): WorkspaceSection => {
  const section = getHomeSectionFromPath(pathname)

  return section === 'profile' ? defaultHomeSection : section
}

/**
 * 从路由状态读取 Profile 弹窗的来源工作区.
 * @param state React Router location state
 * @return HomeProfileRouteState | null 有效来源状态
 */
export const getHomeProfileRouteState = (
  state: unknown,
): HomeProfileRouteState | null => {
  if (!state || typeof state !== 'object' || !('backgroundPath' in state)) {
    return null
  }

  const { backgroundPath } = state

  if (typeof backgroundPath !== 'string') {
    return null
  }

  const section = getHomeSectionFromPath(backgroundPath)

  if (section === 'profile' || homeRouteMap[section] !== backgroundPath) {
    return null
  }

  return { backgroundPath }
}
