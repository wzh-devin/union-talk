import { download as requestFileDownload } from '@/services/generated/file'

/**
 * 获取文件下载地址并交给浏览器直连对象存储.
 * @param fileId 文件标识
 * @return 下载触发完成
 */
export const downloadConversationFile = async (
  fileId: string,
): Promise<void> => {
  const downloadUrl = await requestFileDownload(fileId)

  if (!downloadUrl) {
    throw new Error('文件下载地址获取失败')
  }

  const link = document.createElement('a')
  link.href = downloadUrl
  link.rel = 'noreferrer'
  link.click()
}
