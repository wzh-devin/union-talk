import { Laptop } from 'lucide-react'

import { Badge } from '@/components/shadcn-ui/badge'
import type { ProfileSettings } from '@/pages/home/model/types'

interface DevicesSettingsPaneProps {
  profile: ProfileSettings
}

export const DevicesSettingsPane = ({ profile }: DevicesSettingsPaneProps) => (
  <div>
    <h2 className="text-foreground text-2xl font-semibold tracking-normal">
      登录设备
    </h2>
    <p className="text-muted-foreground mt-1.5 text-sm leading-5">
      查看当前会话内的可信设备摘要。
    </p>
    <div className="mt-6" data-testid="trusted-device-list">
      {profile.trustedDeviceList.map((device) => (
        <div
          key={device.id}
          className="flex items-center justify-between gap-4 py-3.5"
        >
          <div className="flex min-w-0 items-center gap-3">
            <div className="bg-muted text-muted-foreground flex size-9 shrink-0 items-center justify-center rounded-md">
              <Laptop className="size-4" />
            </div>
            <div className="min-w-0">
              <p className="text-foreground truncate text-sm font-semibold">
                {device.name}
              </p>
              <p className="text-muted-foreground mt-0.5 truncate text-xs">
                {device.location} · {device.lastActive}
              </p>
            </div>
          </div>
          {device.isCurrent ? (
            <Badge className="bg-primary text-primary-foreground">当前</Badge>
          ) : null}
        </div>
      ))}
    </div>
  </div>
)
