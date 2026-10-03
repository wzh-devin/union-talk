export interface MemberAvatar {
  imageUrl: string
  altText: string
}

const memberAvatarAccentList = [
  'bg-sky-700 text-white',
  'bg-emerald-700 text-white',
  'bg-purple-700 text-white',
  'bg-rose-700 text-white',
  'bg-amber-700 text-white',
  'bg-teal-700 text-white',
]

const memberAvatarColorList = [
  '#0369a1',
  '#047857',
  '#7e22ce',
  '#be123c',
  '#b45309',
  '#0f766e',
]

/**
 * 获取用户名称缩写.
 * @param name 用户名称
 * @return string 用户名称缩写
 */
export const getInitials = (name: string): string => {
  const trimmedName = name.trim()

  if (/^[\u4e00-\u9fa5]/.test(trimmedName)) {
    return trimmedName.slice(0, 1)
  }

  return trimmedName
    .split(/\s+/)
    .map((part) => part[0])
    .join('')
    .slice(0, 2)
    .toUpperCase()
}

/**
 * 获取成员头像占位色.
 * @param index 成员序号
 * @return string 头像占位样式
 */
export const getMemberAvatarAccent = (index: number): string =>
  memberAvatarAccentList[index % memberAvatarAccentList.length]

/**
 * 生成不依赖外部图片的纯色 SVG 头像地址.
 * @param member 成员名称
 * @param index 成员序号
 * @return string SVG data URL
 */
const getMemberAvatarDataUri = (member: string, index: number): string => {
  const color = memberAvatarColorList[index % memberAvatarColorList.length]
  const initials = getInitials(member)
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="40" height="40" viewBox="0 0 40 40"><rect width="40" height="40" rx="20" fill="${color}"/><text x="50%" y="54%" text-anchor="middle" dominant-baseline="middle" font-family="Arial, sans-serif" font-size="16" font-weight="700" fill="#fff">${initials}</text></svg>`

  return `data:image/svg+xml;charset=utf-8,${encodeURIComponent(svg)}`
}

/**
 * 将成员名称转换为本地头像数据.
 * @param memberPreviewList 成员名称列表
 * @return MemberAvatar[] 本地头像数据列表
 */
export const getMemberAvatarList = (
  memberPreviewList: string[],
): MemberAvatar[] =>
  memberPreviewList.map((member, index) => ({
    imageUrl: getMemberAvatarDataUri(member, index),
    altText: member,
  }))
