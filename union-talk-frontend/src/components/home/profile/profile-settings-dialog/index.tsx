import { useRef } from 'react'

import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogTitle,
} from '@/components/shadcn-ui/dialog'
import { ProfileSettingsContent } from '@/components/home/profile/profile-settings-content'
import type {
  ProfilePane,
  ProfilePaneOption,
  ProfileSettings,
} from '@/pages/home/model/types'

interface ProfileSettingsDialogProps {
  profile: ProfileSettings
  profilePaneList: ProfilePaneOption[]
  activeProfilePane: ProfilePane
  isSavingProfile: boolean
  onOpenChange: (isOpen: boolean) => void
  onSelectProfilePane: (profilePane: ProfilePane) => void
  onSaveProfile: (profile: ProfileSettings) => Promise<void>
  onSaveAvatar: (file: File) => Promise<void>
  onSavePassword: (
    currentPassword: string,
    newPassword: string,
  ) => Promise<void>
  onToggleProfileFriendVerify: () => void
  onLogout: () => void
}

/**
 * 渲染路由驱动的个人配置弹窗.
 * @param props 个人配置弹窗属性
 * @return 个人配置弹窗
 */
export const ProfileSettingsDialog = ({
  profile,
  profilePaneList,
  activeProfilePane,
  isSavingProfile,
  onOpenChange,
  onSelectProfilePane,
  onSaveProfile,
  onSaveAvatar,
  onSavePassword,
  onToggleProfileFriendVerify,
  onLogout,
}: ProfileSettingsDialogProps) => {
  const dialogContentRef = useRef<HTMLDivElement>(null)

  return (
    <Dialog open onOpenChange={onOpenChange}>
      <DialogContent
        ref={dialogContentRef}
        closeLabel="关闭个人信息与设置"
        className="h-[calc(100svh-24px)] w-[calc(100vw-24px)] max-w-none overflow-hidden rounded-xl p-0 md:h-[min(76svh,760px)] md:w-[clamp(720px,68vw,1040px)]"
        onOpenAutoFocus={(event) => {
          event.preventDefault()
          dialogContentRef.current?.focus({ preventScroll: true })
        }}
      >
        <DialogTitle className="sr-only">账户</DialogTitle>
        <DialogDescription className="sr-only">
          管理账号资料、隐私状态和登录设备
        </DialogDescription>
        <ProfileSettingsContent
          profile={profile}
          profilePaneList={profilePaneList}
          activeProfilePane={activeProfilePane}
          isSavingProfile={isSavingProfile}
          onSelectProfilePane={onSelectProfilePane}
          onSaveProfile={onSaveProfile}
          onSaveAvatar={onSaveAvatar}
          onSavePassword={onSavePassword}
          onToggleProfileFriendVerify={onToggleProfileFriendVerify}
          onLogout={onLogout}
        />
      </DialogContent>
    </Dialog>
  )
}
