import { clsx, type ClassValue } from 'clsx'
import { twMerge } from 'tailwind-merge'

/**
 * 合并条件类名并消解 Tailwind 样式冲突.
 * @param inputs 条件类名列表
 * @return string 合并后的类名
 */
export const cn = (...inputs: ClassValue[]): string => twMerge(clsx(inputs))
