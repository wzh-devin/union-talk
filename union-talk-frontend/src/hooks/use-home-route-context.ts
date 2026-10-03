import { useOutletContext } from 'react-router'

import type { useHomeDetailsPanelResize } from '@/hooks/use-home-details-panel-resize'
import type { useHomeWorkspace } from '@/hooks/use-home-workspace'
import type { WorkspaceSection } from '@/pages/home/model/types'

export interface HomeRouteContextValue {
  workspace: ReturnType<typeof useHomeWorkspace>
  detailsPanel: ReturnType<typeof useHomeDetailsPanelResize>
  workspaceSection: WorkspaceSection
  closeProfileSettings: () => void
}

/**
 * 获取 Home 子页面共享的工作台上下文.
 * @return Home 工作台路由上下文
 */
export const useHomeRouteContext = (): HomeRouteContextValue =>
  useOutletContext<HomeRouteContextValue>()
