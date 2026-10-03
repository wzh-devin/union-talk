import { useEffect, useState } from 'react'

import type { AgentRun } from '@/pages/home/model/agent-run'

const TRACE_REFRESH_INTERVAL_MS = 1_000

/**
 * 计算 Agent Run 从开始到指定时间的耗时.
 * @param run Agent Run 生命周期信息
 * @param currentTime 当前时间戳
 * @return 秒级耗时或等待提示
 */
const getRunElapsedLabel = (run: AgentRun, currentTime: number): string => {
  const startedAt = run.startedAt ?? run.queuedAt
  if (!startedAt) {
    return '等待中'
  }
  const startedAtMs = Date.parse(startedAt)
  const completedAtMs = run.completedAt
    ? Date.parse(run.completedAt)
    : currentTime
  const durationMs = completedAtMs - startedAtMs
  if (!Number.isFinite(durationMs) || durationMs < 0) {
    return '—'
  }
  const seconds = durationMs / 1000
  return `${Number.isInteger(seconds) ? seconds.toFixed(0) : seconds.toFixed(1)}s`
}

/**
 * 按秒刷新并渲染 Run 的处理耗时标题.
 * @param props 当前 Agent Run
 * @return 运行状态与耗时标题
 */
export const RunElapsedTitle = ({ run }: { run: AgentRun }) => {
  const [currentTime, setCurrentTime] = useState(() => Date.now())

  useEffect(() => {
    if (run.completedAt) {
      return
    }
    const refreshTimer = window.setInterval(
      () => setCurrentTime(Date.now()),
      TRACE_REFRESH_INTERVAL_MS,
    )
    return () => window.clearInterval(refreshTimer)
  }, [run.completedAt])

  if (run.status === 'FAILED') {
    return <>处理失败 · {getRunElapsedLabel(run, currentTime)}</>
  }
  if (run.status === 'CANCELLED') {
    return <>已取消 · {getRunElapsedLabel(run, currentTime)}</>
  }
  if (run.status === 'SUCCEEDED') {
    return <>已完成 · {getRunElapsedLabel(run, currentTime)}</>
  }
  return <>进行中 · {getRunElapsedLabel(run, currentTime)}</>
}
