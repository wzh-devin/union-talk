import { useState } from 'react'

import { ScrollArea } from '@/components/shadcn-ui/scroll-area'
import { AccountSettingsPane } from '@/components/home/profile/account-settings-pane'
import { DevicesSettingsPane } from '@/components/home/profile/devices-settings-pane'
import { PasswordEditDialog } from '@/components/home/profile/password-edit-dialog'
import { ProfileAvatarUploadDialog } from '@/components/home/profile/profile-avatar-upload-dialog'
import { PrivacySettingsPane } from '@/components/home/profile/privacy-settings-pane'
import { ProfileFieldEditDialog } from '@/components/home/profile/profile-field-edit-dialog'
import { ProfileSettingsSidebar } from '@/components/home/profile/profile-settings-sidebar'
import type { EditableProfileField } from '@/pages/home/model/profile-settings'
import type {
  ProfilePane,
  ProfilePaneOption,
  ProfileSettings,
} from '@/pages/home/model/types'

interface ProfileSettingsContentProps {
  profile: ProfileSettings
  profilePaneList: ProfilePaneOption[]
  activeProfilePane: ProfilePane
  isSavingProfile: boolean
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
 * 渲染可嵌入路由页面的个人设置内容.
 * @param props 个人设置属性
 * @return 个人设置内容
 */
export const ProfileSettingsContent = ({
  profile,
  profilePaneList,
  activeProfilePane,
  isSavingProfile,
  onSelectProfilePane,
  onSaveProfile,
  onSaveAvatar,
  onSavePassword,
  onToggleProfileFriendVerify,
  onLogout,
}: ProfileSettingsContentProps) => {
  const [activeEditableField, setActiveEditableField] =
    useState<EditableProfileField | null>(null)
  const [isAvatarDialogOpen, setIsAvatarDialogOpen] = useState(false)
  const [isPasswordDialogOpen, setIsPasswordDialogOpen] = useState(false)

  /**
   * 保存单个可编辑资料字段.
   * @param field 资料字段
   * @param value 下一字段值
   * @return 保存流程
   */
  const saveEditableField = async (
    field: EditableProfileField,
    value: string,
  ): Promise<void> => {
    await onSaveProfile({
      ...profile,
      [field]: value,
    })
  }

  /**
   * 保存服务端密码.
   * @param currentPassword 当前密码
   * @param newPassword 新密码
   * @return 保存流程
   */
  const savePassword = async (
    currentPassword: string,
    newPassword: string,
  ): Promise<void> => onSavePassword(currentPassword, newPassword)

  return (
    <section aria-label="个人信息与设置" className="h-full min-h-0 min-w-0">
      <div
        className="grid h-full min-h-0 min-w-0 grid-rows-[auto_minmax(0,1fr)] md:grid-cols-[clamp(200px,20vw,240px)_minmax(0,1fr)] md:grid-rows-1"
        data-testid="profile-settings-dialog-shell"
      >
        <ProfileSettingsSidebar
          profile={profile}
          profilePaneList={profilePaneList}
          activeProfilePane={activeProfilePane}
          onSelectProfilePane={onSelectProfilePane}
          onEditAvatar={() => setIsAvatarDialogOpen(true)}
          onEditDisplayName={() => setActiveEditableField('displayName')}
        />

        <div
          className="bg-background flex min-h-0 min-w-0 flex-col"
          data-testid="profile-settings-content"
        >
          <header className="border-border flex min-h-14 shrink-0 items-center border-b px-5 py-3 md:px-7">
            <div>
              <h1 className="text-base font-semibold tracking-normal">账户</h1>
              <p className="sr-only">管理账号资料、隐私状态和登录设备</p>
            </div>
          </header>

          <ScrollArea className="min-h-0 flex-1">
            <div
              className="mx-auto w-full max-w-3xl px-5 py-7 md:px-[clamp(24px,3vw,32px)] md:py-[clamp(24px,4vh,40px)]"
              data-testid="profile-settings-pane"
            >
              {activeProfilePane === 'account' ? (
                <AccountSettingsPane
                  profile={profile}
                  onEditProfileField={setActiveEditableField}
                  onEditPassword={() => setIsPasswordDialogOpen(true)}
                  onLogout={onLogout}
                />
              ) : null}

              {activeProfilePane === 'privacy' ? (
                <PrivacySettingsPane
                  profile={profile}
                  onToggleProfileFriendVerify={onToggleProfileFriendVerify}
                />
              ) : null}

              {activeProfilePane === 'devices' ? (
                <DevicesSettingsPane profile={profile} />
              ) : null}
            </div>
          </ScrollArea>
        </div>
      </div>

      {activeEditableField ? (
        <ProfileFieldEditDialog
          key={activeEditableField}
          field={activeEditableField}
          value={profile[activeEditableField]}
          isSaving={isSavingProfile}
          onOpenChange={(open) => {
            if (!open) {
              setActiveEditableField(null)
            }
          }}
          onSave={(value) => saveEditableField(activeEditableField, value)}
        />
      ) : null}

      {isAvatarDialogOpen ? (
        <ProfileAvatarUploadDialog
          profile={profile}
          isSaving={isSavingProfile}
          onOpenChange={setIsAvatarDialogOpen}
          onUpload={onSaveAvatar}
        />
      ) : null}

      {isPasswordDialogOpen ? (
        <PasswordEditDialog
          isSaving={isSavingProfile}
          onOpenChange={setIsPasswordDialogOpen}
          onSave={savePassword}
        />
      ) : null}
    </section>
  )
}
