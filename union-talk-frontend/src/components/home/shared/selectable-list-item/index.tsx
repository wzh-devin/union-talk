import type { ButtonHTMLAttributes } from 'react'

import { BorderBeam } from '@/components/magic-ui/border-beam'
import { cn } from '@/utils/class-name'

interface SelectableListItemProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  active: boolean
}

/**
 * 统一好友、群聊与通知列表项的选中外观.
 * @param props 原生按钮属性与选中状态
 * @return 具有共享选中态的列表按钮
 */
export const SelectableListItem = ({
  active,
  children,
  className,
  type = 'button',
  ...buttonProps
}: SelectableListItemProps) => (
  <button
    {...buttonProps}
    type={type}
    data-slot="selectable-list-item"
    aria-pressed={active}
    className={cn(
      'relative w-full overflow-hidden rounded-lg px-3 py-2 text-left transition',
      active ? 'bg-background ring-border shadow-sm ring-1' : 'hover:bg-muted',
      className,
    )}
  >
    {active ? (
      <BorderBeam
        size={42}
        duration={8}
        colorFrom="#111111"
        colorTo="#d4d4d4"
        borderWidth={1}
        className="motion-reduce:hidden"
      />
    ) : null}
    {children}
  </button>
)
