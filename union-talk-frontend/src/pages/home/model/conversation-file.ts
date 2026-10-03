export type ConversationFileCategory =
  | 'image'
  | 'video'
  | 'audio'
  | 'pdf'
  | 'document'
  | 'spreadsheet'
  | 'presentation'
  | 'text'
  | 'archive'
  | 'unknown'

export interface ConversationFilePresentationInput {
  fileExt?: string
  fileType?: string
  mimeType?: string
}

interface FileExtensionCategory {
  category: ConversationFileCategory
  extensionList: readonly string[]
}

const fileExtensionCategoryList: readonly FileExtensionCategory[] = [
  {
    category: 'image',
    extensionList: ['png', 'jpg', 'jpeg', 'gif', 'webp', 'svg', 'bmp'],
  },
  {
    category: 'video',
    extensionList: ['mp4', 'webm', 'mov', 'm4v', 'ogv'],
  },
  {
    category: 'audio',
    extensionList: ['mp3', 'wav', 'm4a', 'aac', 'ogg', 'flac'],
  },
  { category: 'pdf', extensionList: ['pdf'] },
  {
    category: 'document',
    extensionList: ['doc', 'docx', 'odt', 'rtf'],
  },
  {
    category: 'spreadsheet',
    extensionList: ['xls', 'xlsx', 'csv', 'ods'],
  },
  {
    category: 'presentation',
    extensionList: ['ppt', 'pptx', 'odp'],
  },
  {
    category: 'text',
    extensionList: [
      'txt',
      'md',
      'json',
      'xml',
      'yaml',
      'yml',
      'js',
      'jsx',
      'ts',
      'tsx',
      'css',
      'html',
      'java',
      'kt',
      'py',
      'go',
      'rs',
      'sql',
      'sh',
    ],
  },
  {
    category: 'archive',
    extensionList: ['zip', 'rar', '7z', 'tar', 'gz', 'bz2', 'xz'],
  },
]

const exactMimeTypeCategoryMap = new Map<string, ConversationFileCategory>([
  ['application/pdf', 'pdf'],
  ['text/csv', 'spreadsheet'],
  ['application/json', 'text'],
  ['application/xml', 'text'],
  ['application/javascript', 'text'],
  ['application/x-yaml', 'text'],
  ['application/msword', 'document'],
  [
    'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    'document',
  ],
  ['application/vnd.ms-excel', 'spreadsheet'],
  [
    'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    'spreadsheet',
  ],
  ['application/vnd.ms-powerpoint', 'presentation'],
  [
    'application/vnd.openxmlformats-officedocument.presentationml.presentation',
    'presentation',
  ],
  ['application/zip', 'archive'],
  ['application/x-7z-compressed', 'archive'],
  ['application/vnd.rar', 'archive'],
  ['application/x-rar-compressed', 'archive'],
  ['application/x-tar', 'archive'],
  ['application/gzip', 'archive'],
])

const fileSizeUnitList = ['B', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB'] as const

/**
 * 根据 MIME 类型和扩展名判断会话文件展示类型.
 * @param input 文件类型元数据
 * @return 文件展示类型
 */
export const classifyConversationFile = (
  input: ConversationFilePresentationInput,
): ConversationFileCategory => {
  const mimeType = input.mimeType?.trim().toLowerCase() || ''
  const exactCategory = exactMimeTypeCategoryMap.get(mimeType)

  if (exactCategory) {
    return exactCategory
  }
  if (mimeType.startsWith('image/')) {
    return 'image'
  }
  if (mimeType.startsWith('video/')) {
    return 'video'
  }
  if (mimeType.startsWith('audio/')) {
    return 'audio'
  }
  if (mimeType.startsWith('text/')) {
    return 'text'
  }

  const fileExt = input.fileExt?.replace(/^\./, '').trim().toLowerCase() || ''
  return (
    fileExtensionCategoryList.find(({ extensionList }) =>
      extensionList.includes(fileExt),
    )?.category || 'unknown'
  )
}

/**
 * 将后端字符串文件大小转换为易读文本.
 * @param fileSize 文件字节数
 * @return 易读文件大小
 */
export const formatConversationFileSize = (fileSize?: string): string => {
  if (!fileSize || !/^\d+$/.test(fileSize)) {
    return '未知大小'
  }

  const byteSize = BigInt(fileSize)
  let unitIndex = 0
  let unitDivisor = 1n

  while (
    unitIndex < fileSizeUnitList.length - 1 &&
    byteSize >= unitDivisor * 1024n
  ) {
    unitIndex += 1
    unitDivisor *= 1024n
  }

  if (unitIndex === 0) {
    return `${byteSize} ${fileSizeUnitList[unitIndex]}`
  }

  const sizeTenths = (byteSize * 10n) / unitDivisor
  const wholeSize = sizeTenths / 10n
  const decimalSize = sizeTenths % 10n
  return `${wholeSize}${decimalSize ? `.${decimalSize}` : ''} ${fileSizeUnitList[unitIndex]}`
}
