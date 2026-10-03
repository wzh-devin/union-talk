import { useHomeRouteContext } from '@/hooks/use-home-route-context'
import { DetailsPanel } from '@/components/home/details/details-panel'
import { ListPanel } from '@/components/home/list/list-panel'
import { NotificationPanel } from '@/components/home/notification/notification-panel'
import { WorkspaceSurface } from '@/components/home/shared/workspace-surface'

/**
 * 编排通知路由页面的工作台内容.
 * @return 通知工作台
 */
export const NotificationWorkspace = () => {
  const { workspace, detailsPanel } = useHomeRouteContext()
  const notification = workspace.activeNotification
  const detailsContent = notification ? (
    <DetailsPanel
      kind="notification"
      notification={notification}
      onSetNotificationStatus={workspace.setNotificationStatus}
    />
  ) : null

  return (
    <WorkspaceSurface
      section="notifications"
      listContent={
        <ListPanel
          kind="notification"
          activeSection="notifications"
          notificationList={workspace.notificationList}
          selectedId={workspace.selectedIdBySection.notifications}
          onSelectNotification={workspace.selectNotification}
        />
      }
      mainContent={
        notification ? (
          <NotificationPanel
            notification={notification}
            isDetailsOpen={workspace.isDetailsOpen}
            processingFriendRequestId={workspace.processingFriendRequestId}
            onBackToList={workspace.showMobileList}
            onToggleDetails={workspace.toggleDetailsPanel}
            onOpenDetailsSheet={() => workspace.setDetailsSheetOpen(true)}
            onHandleFriendRequest={workspace.handleFriendRequest}
          />
        ) : null
      }
      detailsContent={detailsContent}
      isMobileListOpen={workspace.isMobileListOpen}
      isDetailsOpen={workspace.isDetailsOpen}
      isDetailsSheetOpen={workspace.isDetailsSheetOpen}
      detailsPanelWidth={detailsPanel.detailsPanelWidth}
      detailsPanelMaxWidth={detailsPanel.detailsPanelMaxWidth}
      isResizingDetailsPanel={detailsPanel.isResizingDetailsPanel}
      onDetailsSheetOpenChange={workspace.setDetailsSheetOpen}
      onBeginDetailsPanelResize={detailsPanel.beginDetailsPanelResize}
      onDetailsPanelResizeKeyDown={detailsPanel.handleDetailsPanelResizeKeyDown}
      onResetDetailsPanelWidth={detailsPanel.resetDetailsPanelWidth}
    />
  )
}
