import { ConversationWorkspace } from '@/components/home/conversation/conversation-workspace'
import { NotificationWorkspace } from '@/components/home/notification/notification-workspace'
import type { WorkspaceSection } from '@/pages/home/model/types'

interface HomeWorkspaceBackgroundProps {
  section: WorkspaceSection
}

/**
 * 渲染个人设置弹窗下方的来源工作区.
 * @param props 来源工作区属性
 * @return 来源工作区
 */
export const HomeWorkspaceBackground = ({
  section,
}: HomeWorkspaceBackgroundProps) =>
  section === 'notifications' ? (
    <NotificationWorkspace />
  ) : (
    <ConversationWorkspace section={section} />
  )
