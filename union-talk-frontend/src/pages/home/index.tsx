import { useRef } from 'react'
import { Outlet, useLocation, useNavigate } from 'react-router'

import { TooltipProvider } from '@/components/shadcn-ui/tooltip'
import { useHomeDetailsPanelResize } from '@/hooks/use-home-details-panel-resize'
import type { HomeRouteContextValue } from '@/hooks/use-home-route-context'
import { useHomeWorkspace } from '@/hooks/use-home-workspace'
import { RailNavigation } from '@/components/home/navigation/rail-navigation'
import {
  defaultHomeSection,
  getHomeProfileRouteState,
  getHomeSectionFromPath,
  getHomeWorkspaceSectionFromPath,
  homeRouteMap,
} from '@/pages/home/model/home-routes'
import type { Section } from '@/pages/home/model/types'

/**
 * 渲染 Home 共享布局并向子页面提供工作台上下文.
 * @return Home 共享布局
 */
export const HomePage = () => {
  const location = useLocation()
  const navigate = useNavigate()
  const activeSection = getHomeSectionFromPath(location.pathname)
  const profileRouteState = getHomeProfileRouteState(location.state)
  const workspaceSection =
    activeSection === 'profile'
      ? getHomeWorkspaceSectionFromPath(
          profileRouteState?.backgroundPath ?? homeRouteMap[defaultHomeSection],
        )
      : activeSection
  const workspace = useHomeWorkspace(workspaceSection)
  const detailsPanelWorkspaceRef = useRef<HTMLDivElement>(null)
  const detailsPanel = useHomeDetailsPanelResize(detailsPanelWorkspaceRef)

  /**
   * 准备目标分区状态并进入对应子路由.
   * @param nextSection 目标 Home 分区
   * @return void
   */
  const selectSection = (nextSection: Section): void => {
    workspace.selectSection(nextSection)
    if (nextSection === 'profile') {
      void navigate(homeRouteMap.profile, {
        state: { backgroundPath: homeRouteMap[workspaceSection] },
      })
      return
    }

    void navigate(homeRouteMap[nextSection])
  }

  /**
   * 关闭个人设置并恢复来源工作区路由.
   * @return void
   */
  const closeProfileSettings = (): void => {
    if (profileRouteState) {
      void navigate(-1)
      return
    }

    void navigate(homeRouteMap[defaultHomeSection], { replace: true })
  }

  const routeContext = {
    workspace,
    detailsPanel,
    workspaceSection,
    closeProfileSettings,
  } satisfies HomeRouteContextValue

  return (
    <TooltipProvider delayDuration={150}>
      <main className="bg-background text-foreground h-svh overflow-hidden">
        <div className="flex h-full min-w-0 overflow-hidden">
          <RailNavigation
            profile={workspace.profile}
            activeSection={activeSection}
            unreadNotifications={workspace.unreadNotifications}
            unreadFriends={workspace.unreadFriends}
            unreadGroups={workspace.unreadGroups}
            onSelectSection={selectSection}
            onShowPendingFeature={workspace.showPendingFeature}
          />

          <div ref={detailsPanelWorkspaceRef} className="min-w-0 flex-1">
            <Outlet context={routeContext} />
          </div>
        </div>
      </main>
    </TooltipProvider>
  )
}
