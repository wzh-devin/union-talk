import { Camera, Pencil, Search, UserRound } from 'lucide-react'

import {
  Avatar,
  AvatarFallback,
  AvatarImage,
} from '@/components/shadcn-ui/avatar'
import { Input } from '@/components/shadcn-ui/input'
import { cn } from '@/utils/class-name'
import { getInitials } from '@/utils/avatar'
import type {
  ProfilePane,
  ProfilePaneOption,
  ProfileSettings,
} from '@/pages/home/model/types'

interface ProfileSettingsSidebarProps {
  profile: ProfileSettings
  profilePaneList: ProfilePaneOption[]
  activeProfilePane: ProfilePane
  onSelectProfilePane: (pane: ProfilePane) => void
  onEditAvatar: () => void
  onEditDisplayName: () => void
}

/**
 * 渲染个人设置侧栏与头像入口.
 * @param props 个人设置侧栏属性
 * @return 个人设置侧栏
 */
export const ProfileSettingsSidebar = ({
  profile,
  profilePaneList,
  activeProfilePane,
  onSelectProfilePane,
  onEditAvatar,
  onEditDisplayName,
}: ProfileSettingsSidebarProps) => (
  <aside
    className="border-border bg-muted/50 md:bg-muted/35 flex min-h-0 flex-col gap-3 border-b px-3 py-3 md:border-r md:border-b-0 md:py-4"
    data-testid="profile-settings-sidebar"
  >
    <div className="flex items-center gap-2.5 px-1">
      <button
        type="button"
        aria-label="修改头像"
        className="group/avatar focus-visible:ring-ring/50 relative shrink-0 rounded-full outline-none focus-visible:ring-3"
        onClick={onEditAvatar}
      >
        <Avatar className="border-border size-11 border">
          {profile.avatarUrl ? (
            <AvatarImage src={profile.avatarUrl} alt={profile.displayName} />
          ) : null}
          <AvatarFallback className="bg-indigo-700 text-white">
            {getInitials(profile.displayName)}
          </AvatarFallback>
        </Avatar>
        <span className="bg-background text-muted-foreground border-border group-hover/avatar:text-foreground absolute -right-0.5 -bottom-0.5 flex size-5 items-center justify-center rounded-full border shadow-sm transition">
          <Camera className="size-3" />
        </span>
      </button>
      <div className="min-w-0">
        <p className="text-foreground truncate text-sm font-semibold">
          {profile.displayName}
        </p>
        <button
          type="button"
          className="text-muted-foreground hover:text-foreground mt-0.5 flex items-center gap-1 text-xs transition"
          onClick={onEditDisplayName}
        >
          编辑个人资料
          <Pencil className="size-3" />
        </button>
      </div>
    </div>

    <div className="relative">
      <Search className="text-muted-foreground pointer-events-none absolute top-1/2 left-2.5 size-4 -translate-y-1/2" />
      <Input
        aria-label="搜索设置"
        placeholder="搜索"
        className="border-border bg-background text-foreground placeholder:text-muted-foreground h-9 rounded-md pl-8"
      />
    </div>

    <button
      type="button"
      aria-label="账户"
      aria-pressed="true"
      className="bg-muted text-foreground hidden min-h-9 items-center gap-2 rounded-md px-2.5 py-2 text-left text-sm font-medium md:flex"
      onClick={() => onSelectProfilePane('account')}
    >
      <UserRound className="size-4" />
      账户
    </button>

    <nav
      aria-label="个人设置导航"
      className="grid grid-cols-3 gap-1 md:ml-4 md:flex md:flex-col md:border-l md:pl-3"
    >
      {profilePaneList.map((pane) => (
        <button
          key={pane.id}
          type="button"
          aria-label={pane.label}
          aria-pressed={pane.id === activeProfilePane}
          className={cn(
            'text-muted-foreground hover:bg-muted hover:text-foreground relative min-h-8 rounded-md px-2 py-1.5 text-left text-sm transition',
            pane.id === activeProfilePane &&
              'text-foreground bg-muted/70 md:before:bg-foreground font-medium md:before:absolute md:before:inset-y-1 md:before:-left-[13px] md:before:w-px',
          )}
          onClick={() => onSelectProfilePane(pane.id)}
        >
          <span className="block truncate">{pane.label}</span>
        </button>
      ))}
    </nav>
  </aside>
)
