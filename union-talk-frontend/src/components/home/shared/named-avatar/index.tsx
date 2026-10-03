import {
  Avatar,
  AvatarFallback,
  AvatarImage,
} from '@/components/shadcn-ui/avatar'
import { getInitials } from '@/utils/avatar'

interface NamedAvatarProps {
  name: string
  avatarUrl?: string
  className?: string
  fallbackClassName?: string
  decorative?: boolean
}

/**
 * 展示名称对应的图片头像，并在图片不可用时回退到名称缩写.
 * @param props 命名头像属性
 * @return 命名头像
 */
export const NamedAvatar = ({
  name,
  avatarUrl,
  className,
  fallbackClassName,
  decorative = false,
}: NamedAvatarProps) => {
  const normalizedAvatarUrl = avatarUrl?.trim()

  return (
    <Avatar
      aria-hidden={decorative || undefined}
      className={className}
      data-testid="named-avatar"
    >
      {normalizedAvatarUrl ? (
        <AvatarImage
          src={normalizedAvatarUrl}
          alt={decorative ? '' : `${name}头像`}
        />
      ) : null}
      <AvatarFallback className={fallbackClassName}>
        {getInitials(name)}
      </AvatarFallback>
    </Avatar>
  )
}
