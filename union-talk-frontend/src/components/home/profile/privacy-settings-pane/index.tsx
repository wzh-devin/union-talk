import type { ProfileSettings } from '@/pages/home/model/types'

interface PrivacySettingsPaneProps {
  profile: ProfileSettings
  onToggleProfileFriendVerify: () => void
}

export const PrivacySettingsPane = ({
  profile,
  onToggleProfileFriendVerify,
}: PrivacySettingsPaneProps) => (
  <div>
    <h2 className="text-foreground text-2xl font-semibold tracking-normal">
      隐私状态
    </h2>
    <p className="text-muted-foreground mt-1.5 text-sm leading-5">
      管理在线状态、资料可见性和好友验证。
    </p>
    <dl className="mt-6" data-testid="privacy-settings-list">
      {[
        ['当前状态', profile.status],
        ['资料可见', profile.privacyLabel],
        ['上次登录', profile.lastLoginAt],
      ].map(([label, value]) => (
        <div
          key={label}
          className="grid gap-1 py-3 sm:grid-cols-[112px_minmax(0,1fr)] sm:gap-4"
        >
          <dt className="text-muted-foreground text-sm">{label}</dt>
          <dd className="text-foreground text-sm font-medium">{value}</dd>
        </div>
      ))}
    </dl>
    <label
      className="text-foreground mt-1 flex items-center justify-between gap-4 py-4 text-sm font-semibold"
      data-testid="privacy-friend-verify-row"
    >
      <span>
        添加好友需要验证
        <span className="text-muted-foreground mt-1 block text-xs font-normal">
          关闭后，其他用户可以直接添加你为好友。
        </span>
      </span>
      <input
        type="checkbox"
        aria-label="添加好友需要验证"
        checked={profile.needFriendVerify}
        className="size-4 accent-black"
        onChange={onToggleProfileFriendVerify}
      />
    </label>
  </div>
)
