export interface ApiResult<T> {
  success: boolean
  errCode?: number
  errMsg?: string
  data: T
}

export type UserStatus = 'NORMAL' | 'BLOCKED' | 'DELETED'

export interface UserInfoResponse {
  uid?: string
  code?: string
  username?: string
  email?: string
  avatarUrl?: string
  bio?: string
  needFriendVerify?: boolean
  status?: UserStatus
  lastLoginAt?: string
}

export interface UpdateUserRequest {
  username?: string
  avatarUrl?: string
  bio?: string
  needFriendVerify?: boolean
}

export interface FriendResponse {
  userId?: number
  code?: string
  username?: string
  avatarUrl?: string
  remark?: string
  friendGroupId?: number
  status?: string
}

export interface FriendRequestResponse {
  id?: number
  fromUserId?: number
  fromUsername?: string
  fromAvatarUrl?: string
  applyMsg?: string
  status?: string
  createdAt?: string
}

export interface SendFriendRequestRequest {
  toUserCode: string
  applyMsg?: string
}

export interface HandleFriendRequestRequest {
  requestId: number
  accept: boolean
  friendGroupId?: number
  remark?: string
}

export interface FriendGroupResponse {
  id?: number
  name?: string
  sortOrder?: number
}

export interface CreateFriendGroupRequest {
  name: string
  sortOrder?: number
}

export interface UpdateFriendGroupRequest {
  name?: string
  sortOrder?: number
}

export interface UpdateFriendRemarkRequest {
  targetId: number
  remark: string
}

export interface MoveFriendGroupRequest {
  targetId: number
  friendGroupId: number
}
