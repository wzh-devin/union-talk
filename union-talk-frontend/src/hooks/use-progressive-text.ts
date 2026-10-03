import { useEffect, useRef, useState } from 'react'

import { AGENT_TEXT_REVEAL_INTERVAL_MILLISECONDS } from '@/services/agent/agent-constant'

interface UseProgressiveTextInput {
  text: string
  enabled: boolean
}

/**
 * 获取目标正文中尚未展示的第一个 Unicode 字符.
 * @param visibleText 当前已展示文本
 * @param targetText 服务端已接收的完整文本
 * @return 下一个待展示字符；没有剩余字符时返回空字符串
 */
const getNextCharacter = (visibleText: string, targetText: string): string => {
  if (!targetText.startsWith(visibleText)) {
    return ''
  }
  return Array.from(targetText.slice(visibleText.length))[0] ?? ''
}

/**
 * 将服务端 SSE 正文限制为固定速率的逐字输出.
 * @param input 目标文本与渐进输出开关
 * @return 当前应展示的文本
 */
export const useProgressiveText = ({
  text,
  enabled,
}: UseProgressiveTextInput): string => {
  const initialVisibleText = enabled ? '' : text
  const [visibleText, setVisibleText] = useState(initialVisibleText)
  const [isProgressiveSessionActive, setIsProgressiveSessionActive] =
    useState(enabled)
  const visibleTextRef = useRef(initialVisibleText)
  const nextRevealAtRef = useRef(0)

  useEffect(() => {
    if (enabled && !isProgressiveSessionActive) {
      const activateTimerId = window.setTimeout(() => {
        visibleTextRef.current = ''
        nextRevealAtRef.current = 0
        setVisibleText('')
        setIsProgressiveSessionActive(true)
      }, 0)
      return () => window.clearTimeout(activateTimerId)
    }
    if (!enabled && !isProgressiveSessionActive) {
      return
    }

    if (!text.startsWith(visibleTextRef.current)) {
      const resetTimerId = window.setTimeout(() => {
        visibleTextRef.current = ''
        nextRevealAtRef.current = 0
        setVisibleText('')
      }, 0)
      return () => window.clearTimeout(resetTimerId)
    }

    const nextCharacter = getNextCharacter(visibleTextRef.current, text)
    if (!nextCharacter) {
      if (!enabled) {
        const completeTimerId = window.setTimeout(
          () => setIsProgressiveSessionActive(false),
          0,
        )
        return () => window.clearTimeout(completeTimerId)
      }
      return
    }

    const delayMilliseconds = Math.max(0, nextRevealAtRef.current - Date.now())
    const revealTimerId = window.setTimeout(() => {
      const nextVisibleText = `${visibleTextRef.current}${nextCharacter}`
      visibleTextRef.current = nextVisibleText
      nextRevealAtRef.current =
        Date.now() + AGENT_TEXT_REVEAL_INTERVAL_MILLISECONDS
      setVisibleText(nextVisibleText)
    }, delayMilliseconds)
    return () => window.clearTimeout(revealTimerId)
  }, [enabled, isProgressiveSessionActive, text, visibleText])

  if (!enabled && !isProgressiveSessionActive) {
    return text
  }
  if (enabled && !isProgressiveSessionActive) {
    return ''
  }
  if (!text.startsWith(visibleText)) {
    return ''
  }
  return visibleText
}
