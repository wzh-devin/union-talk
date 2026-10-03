import * as React from 'react'
import { Switch as SwitchPrimitive } from 'radix-ui'

import { cn } from '@/utils/class-name'

/**
 * 渲染项目统一的可访问开关控件.
 * @param props Radix Switch 属性
 * @return 开关控件
 */
const Switch = ({
  className,
  ...props
}: React.ComponentProps<typeof SwitchPrimitive.Root>) => (
  <SwitchPrimitive.Root
    data-slot="switch"
    className={cn(
      "peer data-[state=checked]:bg-foreground data-[state=unchecked]:bg-input focus-visible:ring-ring/50 relative inline-flex h-4 w-7 shrink-0 cursor-pointer items-center rounded-full border border-transparent transition-colors before:absolute before:inset-x-0 before:-inset-y-2 before:content-[''] focus-visible:ring-3 disabled:cursor-not-allowed disabled:opacity-50",
      className,
    )}
    {...props}
  >
    <SwitchPrimitive.Thumb
      data-slot="switch-thumb"
      className="bg-background pointer-events-none block size-3 rounded-full shadow-sm transition-transform data-[state=checked]:translate-x-3 data-[state=unchecked]:translate-x-0"
    />
  </SwitchPrimitive.Root>
)

export { Switch }
