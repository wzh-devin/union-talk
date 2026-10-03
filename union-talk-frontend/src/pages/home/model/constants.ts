import type { ProfilePaneOption, Section } from '@/pages/home/model/types'

export interface SectionMeta {
  label: string
  listTitle: string
  searchPlaceholder: string
}

export const sectionMetaMap: Record<Section, SectionMeta> = {
  notifications: {
    label: '通知',
    listTitle: '通知列表',
    searchPlaceholder: '搜索通知',
  },
  friends: {
    label: '好友',
    listTitle: '好友列表',
    searchPlaceholder: '搜索好友',
  },
  groups: {
    label: '群聊',
    listTitle: '群聊列表',
    searchPlaceholder: '搜索群聊',
  },
  profile: {
    label: '个人信息',
    listTitle: '个人设置',
    searchPlaceholder: '搜索设置',
  },
}

export const profilePaneList: ProfilePaneOption[] = [
  {
    id: 'account',
    label: '账号信息',
    description: '名称、邮箱和个人签名',
  },
  {
    id: 'privacy',
    label: '隐私状态',
    description: '在线状态与资料可见性',
  },
  {
    id: 'devices',
    label: '登录设备',
    description: '当前设备与安全提醒',
  },
]

export const defaultSelectedIdBySection: Record<Section, string> = {
  notifications: 'notice-release',
  friends: 'friend-lin',
  groups: 'group-product',
  profile: 'account',
}
