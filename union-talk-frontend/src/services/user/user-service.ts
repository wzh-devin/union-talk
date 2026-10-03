import { request } from '@/services/http/request'
import type {
  ApiResult,
  CreateFriendGroupRequest,
  FriendGroupResponse,
  FriendRequestResponse,
  FriendResponse,
  HandleFriendRequestRequest,
  MoveFriendGroupRequest,
  SendFriendRequestRequest,
  UpdateFriendGroupRequest,
  UpdateFriendRemarkRequest,
  UpdateUserRequest,
  UserInfoResponse,
} from '@/services/user/types'

/**
 * 解包用户服务统一响应.
 * @param result API 统一响应
 * @return 响应业务数据
 */
function unwrapApiResult<T>(result: ApiResult<T>): T {
  if (!result.success) {
    throw new Error(result.errMsg || '请求失败')
  }

  return result.data
}

/**
 * 获取当前登录用户信息.
 * @return 当前用户资料
 */
export async function getCurrentUserInfo(): Promise<UserInfoResponse> {
  const result = await request<ApiResult<UserInfoResponse>>({
    method: 'GET',
    url: '/user/getCurrentUserInfo',
  })

  return unwrapApiResult(result)
}

/**
 * 根据用户编号查询用户信息.
 * @param code 用户编号
 * @return 用户资料
 */
export async function getUserInfoByCode(
  code: string,
): Promise<UserInfoResponse> {
  const result = await request<ApiResult<UserInfoResponse>>({
    method: 'GET',
    url: '/user/getUserInfo',
    params: { code },
  })

  return unwrapApiResult(result)
}

/**
 * 更新当前用户资料.
 * @param data 用户资料更新内容
 * @return 更新是否成功
 */
export async function updateUser(data: UpdateUserRequest): Promise<boolean> {
  const result = await request<ApiResult<boolean>>({
    method: 'PUT',
    url: '/user/updateUser',
    data,
  })

  return unwrapApiResult(result)
}

/**
 * 获取当前用户好友列表.
 * @return 好友列表
 */
export async function getFriendList(): Promise<FriendResponse[]> {
  const result = await request<ApiResult<FriendResponse[]>>({
    method: 'GET',
    url: '/user/userRelation/friend/list',
  })

  return unwrapApiResult(result)
}

/**
 * 发送好友申请.
 * @param data 好友申请内容
 * @return 发送是否成功
 */
export async function sendFriendRequest(
  data: SendFriendRequestRequest,
): Promise<boolean> {
  const result = await request<ApiResult<boolean>>({
    method: 'POST',
    url: '/user/friendRequest/send',
    data,
  })

  return unwrapApiResult(result)
}

/**
 * 处理收到的好友申请.
 * @param data 好友申请处理内容
 * @return 处理是否成功
 */
export async function handleFriendRequest(
  data: HandleFriendRequestRequest,
): Promise<boolean> {
  const result = await request<ApiResult<boolean>>({
    method: 'POST',
    url: '/user/friendRequest/handle',
    data,
  })

  return unwrapApiResult(result)
}

/**
 * 获取收到的好友申请列表.
 * @return 收到的好友申请列表
 */
export async function getReceivedFriendRequests(): Promise<
  FriendRequestResponse[]
> {
  const result = await request<ApiResult<FriendRequestResponse[]>>({
    method: 'GET',
    url: '/user/friendRequest/received/list',
  })

  return unwrapApiResult(result)
}

/**
 * 获取已发送的好友申请列表.
 * @return 已发送的好友申请列表
 */
export async function getSentFriendRequests(): Promise<
  FriendRequestResponse[]
> {
  const result = await request<ApiResult<FriendRequestResponse[]>>({
    method: 'GET',
    url: '/user/friendRequest/sent/list',
  })

  return unwrapApiResult(result)
}

/**
 * 获取好友分组列表.
 * @return 好友分组列表
 */
export async function getFriendGroups(): Promise<FriendGroupResponse[]> {
  const result = await request<ApiResult<FriendGroupResponse[]>>({
    method: 'GET',
    url: '/user/friendGroup/list',
  })

  return unwrapApiResult(result)
}

/**
 * 创建好友分组.
 * @param data 好友分组创建内容
 * @return 创建后的好友分组
 */
export async function createFriendGroup(
  data: CreateFriendGroupRequest,
): Promise<FriendGroupResponse> {
  const result = await request<ApiResult<FriendGroupResponse>>({
    method: 'POST',
    url: '/user/friendGroup/create',
    data,
  })

  return unwrapApiResult(result)
}

/**
 * 更新好友分组.
 * @param groupId 好友分组 ID
 * @param data 好友分组更新内容
 * @return 更新是否成功
 */
export async function updateFriendGroup(
  groupId: number,
  data: UpdateFriendGroupRequest,
): Promise<boolean> {
  const result = await request<ApiResult<boolean>>({
    method: 'PUT',
    url: `/user/friendGroup/update/${groupId}`,
    data,
  })

  return unwrapApiResult(result)
}

/**
 * 删除好友分组.
 * @param groupId 好友分组 ID
 * @return 删除是否成功
 */
export async function deleteFriendGroup(groupId: number): Promise<boolean> {
  const result = await request<ApiResult<boolean>>({
    method: 'DELETE',
    url: `/user/friendGroup/delete/${groupId}`,
  })

  return unwrapApiResult(result)
}

/**
 * 更新好友备注.
 * @param data 好友备注更新内容
 * @return 更新是否成功
 */
export async function updateFriendRemark(
  data: UpdateFriendRemarkRequest,
): Promise<boolean> {
  const result = await request<ApiResult<boolean>>({
    method: 'PUT',
    url: '/user/userRelation/remark',
    data,
  })

  return unwrapApiResult(result)
}

/**
 * 移动好友到指定分组.
 * @param data 好友分组移动内容
 * @return 移动是否成功
 */
export async function moveFriendToGroup(
  data: MoveFriendGroupRequest,
): Promise<boolean> {
  const result = await request<ApiResult<boolean>>({
    method: 'PUT',
    url: '/user/userRelation/moveGroup',
    data,
  })

  return unwrapApiResult(result)
}

/**
 * 拉黑好友.
 * @param targetId 目标用户 ID
 * @return 拉黑是否成功
 */
export async function blockFriend(targetId: number): Promise<boolean> {
  const result = await request<ApiResult<boolean>>({
    method: 'POST',
    url: `/user/userRelation/block/${targetId}`,
  })

  return unwrapApiResult(result)
}

/**
 * 解除好友拉黑.
 * @param targetId 目标用户 ID
 * @return 解除拉黑是否成功
 */
export async function unblockFriend(targetId: number): Promise<boolean> {
  const result = await request<ApiResult<boolean>>({
    method: 'POST',
    url: `/user/userRelation/unblock/${targetId}`,
  })

  return unwrapApiResult(result)
}

/**
 * 删除好友关系.
 * @param targetId 目标用户 ID
 * @return 删除是否成功
 */
export async function deleteFriend(targetId: number): Promise<boolean> {
  const result = await request<ApiResult<boolean>>({
    method: 'DELETE',
    url: `/user/userRelation/delete/${targetId}`,
  })

  return unwrapApiResult(result)
}
