import { useHomeRouteContext } from '@/hooks/use-home-route-context'
import { ProfileSettingsDialog } from '@/components/home/profile/profile-settings-dialog'
import { HomeWorkspaceBackground } from '@/components/home/shared/home-workspace-background'
import { profilePaneList } from '@/pages/home/model/constants'
import { clearAuthSession } from '@/services/auth/session-service'

/**
 * 组合独立路由下的个人设置页面.
 * @return 个人设置页面
 */
export const ProfilePage = () => {
  const { workspace, workspaceSection, closeProfileSettings } =
    useHomeRouteContext()

  /**
   * 退出当前本地登录会话.
   * @return 无返回值
   */
  const handleLogout = (): void => {
    clearAuthSession()
  }

  return (
    <>
      <HomeWorkspaceBackground section={workspaceSection} />
      <ProfileSettingsDialog
        profile={workspace.profile}
        profilePaneList={profilePaneList}
        activeProfilePane={workspace.activeProfilePane}
        isSavingProfile={workspace.isSavingProfile}
        onOpenChange={(isOpen) => {
          if (!isOpen) {
            closeProfileSettings()
          }
        }}
        onSelectProfilePane={workspace.selectProfilePane}
        onSaveProfile={workspace.saveProfile}
        onSaveAvatar={workspace.saveAvatar}
        onSavePassword={workspace.savePassword}
        onToggleProfileFriendVerify={workspace.toggleProfileFriendVerify}
        onLogout={handleLogout}
      />
    </>
  )
}
