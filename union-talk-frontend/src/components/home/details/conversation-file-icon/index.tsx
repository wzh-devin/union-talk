import {
  Archive,
  File,
  FileAudio,
  FileCode2,
  FileImage,
  FileSpreadsheet,
  FileText,
  FileVideo,
  Presentation,
  type LucideIcon,
} from 'lucide-react'

import {
  classifyConversationFile,
  type ConversationFileCategory,
  type ConversationFilePresentationInput,
} from '@/pages/home/model/conversation-file'
import { cn } from '@/utils/class-name'

interface ConversationFileIconProps {
  file: ConversationFilePresentationInput
  className?: string
}

const fileIconMap: Record<ConversationFileCategory, LucideIcon> = {
  image: FileImage,
  video: FileVideo,
  audio: FileAudio,
  pdf: FileText,
  document: FileText,
  spreadsheet: FileSpreadsheet,
  presentation: Presentation,
  text: FileCode2,
  archive: Archive,
  unknown: File,
}

const fileIconColorMap: Record<ConversationFileCategory, string> = {
  image: 'text-emerald-600 dark:text-emerald-400',
  video: 'text-violet-600 dark:text-violet-400',
  audio: 'text-fuchsia-600 dark:text-fuchsia-400',
  pdf: 'text-red-600 dark:text-red-400',
  document: 'text-blue-600 dark:text-blue-400',
  spreadsheet: 'text-green-600 dark:text-green-400',
  presentation: 'text-orange-600 dark:text-orange-400',
  text: 'text-sky-600 dark:text-sky-400',
  archive: 'text-amber-600 dark:text-amber-400',
  unknown: 'text-muted-foreground',
}

/**
 * 按文件类型渲染会话文件图标.
 * @param props 文件元数据和样式
 * @return 文件类型图标
 */
export const ConversationFileIcon = ({
  file,
  className,
}: ConversationFileIconProps) => {
  const category = classifyConversationFile(file)
  const Icon = fileIconMap[category]

  return (
    <Icon
      aria-hidden="true"
      data-file-category={category}
      className={cn('size-4 shrink-0', fileIconColorMap[category], className)}
    />
  )
}
