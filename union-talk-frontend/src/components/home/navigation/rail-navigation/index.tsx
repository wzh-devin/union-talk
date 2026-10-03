import { Bell, MessageCircle, Plus, Sparkles, Users } from 'lucide-react'

import {
  Avatar,
  AvatarFallback,
  AvatarImage,
} from '@/components/shadcn-ui/avatar'
import { Button } from '@/components/shadcn-ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuGroup,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/shadcn-ui/dropdown-menu'
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from '@/components/shadcn-ui/tooltip'
import { cn } from '@/utils/class-name'
import { getInitials } from '@/utils/avatar'
import { RailButton } from '@/components/home/navigation/rail-button'
import type { ProfileSettings, Section } from '@/pages/home/model/types'

interface RailNavigationProps {
  profile: ProfileSettings
  activeSection: Section
  unreadNotifications: number
  unreadFriends: number
  unreadGroups: number
  onSelectSection: (section: Section) => void
  onShowPendingFeature: (feature: string) => void
}

export const RailNavigation = ({
  profile,
  activeSection,
  unreadNotifications,
  unreadFriends,
  unreadGroups,
  onSelectSection,
  onShowPendingFeature,
}: RailNavigationProps) => (
  <nav
    aria-label="主导航"
    className="border-border bg-background flex w-14 shrink-0 flex-col items-center border-r py-3"
  >
    <div className="flex flex-1 flex-col items-center gap-2">
      <RailButton
        label="通知"
        active={activeSection === 'notifications'}
        unreadCount={unreadNotifications}
        onClick={() => onSelectSection('notifications')}
      >
        <Bell />
      </RailButton>
      <RailButton
        label="好友"
        active={activeSection === 'friends'}
        unreadCount={unreadFriends}
        onClick={() => onSelectSection('friends')}
      >
        <Users />
      </RailButton>
      <RailButton
        label="群聊"
        active={activeSection === 'groups'}
        unreadCount={unreadGroups}
        onClick={() => onSelectSection('groups')}
      >
        <MessageCircle />
      </RailButton>

      <DropdownMenu>
        <Tooltip>
          <TooltipTrigger asChild>
            <DropdownMenuTrigger asChild>
              <Button
                type="button"
                variant="ghost"
                size="icon-lg"
                aria-label="打开新增菜单"
                className="border-border text-muted-foreground hover:bg-muted hover:text-foreground relative mt-1 rounded-xl border border-dashed bg-transparent"
              >
                <Plus className="size-4" />
              </Button>
            </DropdownMenuTrigger>
          </TooltipTrigger>
          <TooltipContent side="right">新增</TooltipContent>
        </Tooltip>
        <DropdownMenuContent side="right" align="start">
          <DropdownMenuGroup>
            <DropdownMenuItem onSelect={() => onShowPendingFeature('新功能')}>
              <Sparkles />
              新功能占位
            </DropdownMenuItem>
          </DropdownMenuGroup>
        </DropdownMenuContent>
      </DropdownMenu>
    </div>

    <Tooltip>
      <TooltipTrigger asChild>
        <Button
          type="button"
          variant="ghost"
          size="icon-lg"
          aria-label="打开个人信息设置"
          aria-pressed={activeSection === 'profile'}
          aria-current={activeSection === 'profile' ? 'page' : undefined}
          className={cn(
            'hover:bg-muted rounded-xl p-0',
            activeSection === 'profile' &&
              'bg-muted-foreground/15 hover:bg-muted-foreground/15',
          )}
          onClick={() => onSelectSection('profile')}
        >
          <Avatar className="border-border size-9 border">
            {profile.avatarUrl ? (
              <AvatarImage src={profile.avatarUrl} alt={profile.displayName} />
            ) : null}
            <AvatarFallback className="bg-indigo-700 text-white">
              {getInitials(profile.displayName)}
            </AvatarFallback>
          </Avatar>
        </Button>
      </TooltipTrigger>
      <TooltipContent side="right">个人信息</TooltipContent>
    </Tooltip>
  </nav>
)
