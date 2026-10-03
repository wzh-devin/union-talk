import { LogOut } from 'lucide-react'

import { Button } from '@/components/shadcn-ui/button'
import { Separator } from '@/components/shadcn-ui/separator'
import { AccountSettingRow } from '@/components/home/profile/account-setting-row'
import type { EditableProfileField } from '@/pages/home/model/profile-settings'
import type { ProfileSettings } from '@/pages/home/model/types'

interface AccountSettingsPaneProps {
  profile: ProfileSettings
  onEditProfileField: (field: EditableProfileField) => void
  onEditPassword: () => void
  onLogout: () => void
}

export const AccountSettingsPane = ({
  profile,
  onEditProfileField,
  onEditPassword,
  onLogout,
}: AccountSettingsPaneProps) => (
  <div>
    <h2 className="text-foreground text-2xl font-semibold tracking-normal">
      账号信息
    </h2>
    <div className="mt-4">
      <AccountSettingRow
        label="Code"
        value={profile.code}
        actionLabel="复制 Code"
        actionText="复制"
        onAction={() => void navigator.clipboard.writeText(profile.code)}
      />
      <AccountSettingRow
        label="显示名称"
        value={profile.displayName}
        actionLabel="编辑显示名称"
        onAction={() => onEditProfileField('displayName')}
      />
      <AccountSettingRow
        label="邮箱"
        value={profile.email}
        actionLabel="编辑邮箱"
        onAction={() => onEditProfileField('email')}
      />
      <AccountSettingRow
        label="个人签名"
        value={profile.bio || '未设置'}
        actionLabel="编辑个人签名"
        onAction={() => onEditProfileField('bio')}
      />
    </div>

    <Separator className="my-8" />

    <h2 className="text-foreground text-2xl font-semibold tracking-normal">
      密码和安全中心
    </h2>
    <div className="mt-4">
      <AccountSettingRow
        label="密码"
        value="••••••••"
        actionLabel="修改密码"
        actionText="修改"
        onAction={onEditPassword}
      />
    </div>

    <Separator className="my-8" />

    <section
      aria-labelledby="profile-settings-logout-title"
      className="grid gap-4 sm:grid-cols-[minmax(0,1fr)_auto] sm:items-center"
    >
      <div className="min-w-0">
        <h2
          id="profile-settings-logout-title"
          className="text-foreground text-2xl font-semibold tracking-normal"
        >
          退出登录
        </h2>
        <p className="text-muted-foreground mt-1.5 text-sm leading-5">
          退出当前账号后，需要重新登录
        </p>
      </div>
      <Button
        type="button"
        variant="destructive"
        size="lg"
        className="justify-self-start sm:justify-self-end"
        onClick={onLogout}
      >
        <LogOut data-icon="inline-start" />
        退出登录
      </Button>
    </section>
  </div>
)
