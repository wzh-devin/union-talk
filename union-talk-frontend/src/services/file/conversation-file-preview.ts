import { preview } from '@/services/generated/file'

/**
 * 获取指定会话文件的临时预览地址.
 * @param fileId 文件标识
 * @return 两小时有效的预签名地址
 */
export const previewConversationFile = (fileId: string): Promise<string> =>
  preview(fileId)
