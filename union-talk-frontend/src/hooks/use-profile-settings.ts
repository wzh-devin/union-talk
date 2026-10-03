import { useCallback, useEffect, useRef, useState } from 'react'
import { toast } from 'sonner'

import {
  getCurrentUserInfo,
  resetPassword,
  updateUser,
  uploadAvatar,
} from '@/services/generated/user'
import { mapProfileSettings } from '@/pages/home/model/api-adapters'
import type { ProfileSettings } from '@/pages/home/model/types'

const emptyProfile: ProfileSettings = {
  userId: '',
  code: '',
  displayName: '未命名用户',
  email: '',
  avatarUrl: '',
  bio: '',
  needFriendVerify: true,
  status: '未知',
  lastLoginAt: '-',
  privacyLabel: '仅好友可见',
  trustedDeviceList: [],
}

/** 管理个人资料的服务端加载、保存与失败回滚。 */
export const useProfileSettings = () => {
  const [profile, setProfile] = useState<ProfileSettings>(emptyProfile)
  const [isSavingProfile, setIsSavingProfile] = useState(false)
  const requestVersionRef = useRef(0)

  useEffect(() => {
    const requestVersion = ++requestVersionRef.current

    void getCurrentUserInfo()
      .then((user) => {
        if (requestVersion === requestVersionRef.current) {
          setProfile(mapProfileSettings(user))
        }
      })
      .catch(() => toast.error('个人资料加载失败，请稍后重试'))

    return () => {
      requestVersionRef.current += 1
    }
  }, [])

  const saveProfile = useCallback(
    async (nextProfile: ProfileSettings): Promise<void> => {
      const previousProfile = profile
      const hasServerChanges =
        nextProfile.displayName !== previousProfile.displayName ||
        nextProfile.bio !== previousProfile.bio

      setIsSavingProfile(true)
      setProfile(nextProfile)
      try {
        if (hasServerChanges) {
          await updateUser({
            username: nextProfile.displayName,
            bio: nextProfile.bio,
          })
        }
        toast.success('个人信息已保存')
      } catch (error) {
        setProfile(previousProfile)
        toast.error('个人信息保存失败，请稍后重试')
        throw error
      } finally {
        setIsSavingProfile(false)
      }
    },
    [profile],
  )

  const savePassword = useCallback(
    async (currentPassword: string, newPassword: string): Promise<void> => {
      setIsSavingProfile(true)
      try {
        await resetPassword({ currentPassword, newPassword })
        toast.success('密码已更新')
      } catch (error) {
        toast.error('密码更新失败，请检查当前密码')
        throw error
      } finally {
        setIsSavingProfile(false)
      }
    },
    [],
  )

  const toggleProfileFriendVerify = useCallback(async (): Promise<void> => {
    const previousProfile = profile
    const needFriendVerify = !previousProfile.needFriendVerify

    setProfile({ ...previousProfile, needFriendVerify })
    try {
      await updateUser({ needFriendVerify })
    } catch {
      setProfile(previousProfile)
      toast.error('好友验证设置更新失败')
    }
  }, [profile])

  /**
   * 上传头像文件并将返回地址写入当前用户资料.
   * @param file 头像图片文件
   * @return 保存流程
   */
  const saveAvatar = useCallback(
    async (file: File): Promise<void> => {
      const previousProfile = profile

      setIsSavingProfile(true)
      try {
        const avatarUrl = await uploadAvatar(
          { file },
          { headers: { 'Content-Type': 'multipart/form-data' } },
        )
        const nextProfile = { ...previousProfile, avatarUrl }
        setProfile(nextProfile)
        await updateUser({ avatarUrl })
        toast.success('头像已更新')
      } catch (error) {
        setProfile(previousProfile)
        toast.error('头像更新失败，请稍后重试')
        throw error
      } finally {
        setIsSavingProfile(false)
      }
    },
    [profile],
  )

  return {
    profile,
    isSavingProfile,
    saveProfile,
    saveAvatar,
    savePassword,
    toggleProfileFriendVerify,
  }
}
