import { useEffect, useRef, useState } from 'react'
import {
  AudioWaveform,
  LoaderCircle,
  Pause,
  Play,
  RotateCcw,
} from 'lucide-react'

import { Button } from '@/components/shadcn-ui/button'
import { cn } from '@/utils/class-name'

interface AudioMessagePlayerProps {
  assetName: string
  isSelf: boolean
  previewUrl: string
  isLoading: boolean
  errorMessage: string
  onLoadPreview: () => void
}

/**
 * 将媒体秒数转换为分秒文本.
 * @param durationSeconds 媒体秒数
 * @return 分秒文本
 */
const formatMediaDuration = (durationSeconds: number): string => {
  if (!Number.isFinite(durationSeconds) || durationSeconds <= 0) {
    return '00:00'
  }
  const roundedDuration = Math.round(durationSeconds)
  const minute = Math.floor(roundedDuration / 60)
  const second = roundedDuration % 60
  return `${String(minute).padStart(2, '0')}:${String(second).padStart(2, '0')}`
}

/**
 * 渲染轻量语音消息播放器并保留真实媒体播放能力.
 * @param props 音频资源与预览加载状态
 * @return 语音消息播放器
 */
export const AudioMessagePlayer = ({
  assetName,
  isSelf,
  previewUrl,
  isLoading,
  errorMessage,
  onLoadPreview,
}: AudioMessagePlayerProps) => {
  const audioRef = useRef<HTMLAudioElement>(null)
  const pendingPlayRef = useRef(false)
  const [isPlaying, setIsPlaying] = useState(false)
  const [durationSeconds, setDurationSeconds] = useState(0)
  const [currentSeconds, setCurrentSeconds] = useState(0)
  const progress = durationSeconds
    ? Math.min(100, Math.round((currentSeconds / durationSeconds) * 100))
    : 0

  useEffect(() => {
    const audio = audioRef.current
    if (!previewUrl || !audio || !pendingPlayRef.current) {
      return
    }
    pendingPlayRef.current = false
    void audio
      .play()
      .then(() => setIsPlaying(true))
      .catch(() => setIsPlaying(false))
  }, [previewUrl])

  /**
   * 加载音频预览或切换当前播放状态.
   * @return 播放控制流程
   */
  const togglePlayback = async (): Promise<void> => {
    if (isLoading) {
      return
    }
    if (!previewUrl) {
      pendingPlayRef.current = true
      onLoadPreview()
      return
    }

    const audio = audioRef.current
    if (!audio) {
      return
    }
    if (isPlaying) {
      audio.pause()
      setIsPlaying(false)
      return
    }
    try {
      await audio.play()
      setIsPlaying(true)
    } catch {
      setIsPlaying(false)
    }
  }

  return (
    <div
      data-testid="audio-message-player"
      className={cn(
        'flex w-[min(360px,75vw)] items-center gap-2 rounded-full border px-2.5 py-2 shadow-sm',
        isSelf
          ? 'border-emerald-200 bg-emerald-50 text-emerald-950'
          : 'border-border bg-muted/70 text-foreground',
      )}
    >
      <Button
        type="button"
        variant="ghost"
        size="icon-sm"
        aria-label={`${isPlaying ? '暂停' : errorMessage ? '重试' : '播放'} ${assetName}`}
        disabled={isLoading}
        className={cn(
          'size-8 rounded-full',
          isSelf
            ? 'bg-emerald-600 text-white hover:bg-emerald-700 hover:text-white'
            : 'bg-background hover:bg-background/75',
        )}
        onClick={() => void togglePlayback()}
      >
        {isLoading ? (
          <LoaderCircle aria-hidden className="animate-spin" />
        ) : errorMessage ? (
          <RotateCcw aria-hidden />
        ) : isPlaying ? (
          <Pause aria-hidden className="fill-current" />
        ) : (
          <Play aria-hidden className="fill-current" />
        )}
      </Button>

      <AudioWaveform
        aria-hidden
        className={cn(
          'size-5 shrink-0',
          isSelf ? 'text-emerald-700' : 'text-muted-foreground',
        )}
      />
      <span
        role="progressbar"
        aria-label={`${assetName}播放进度`}
        aria-valuemin={0}
        aria-valuemax={100}
        aria-valuenow={progress}
        className="bg-foreground/15 relative h-px min-w-14 flex-1 overflow-hidden rounded-full"
      >
        <span
          className={cn(
            'absolute inset-y-0 left-0 rounded-full transition-[width]',
            isSelf ? 'bg-emerald-700' : 'bg-foreground',
          )}
          style={{ width: `${progress}%` }}
        />
      </span>
      <span className="min-w-11 font-mono text-xs tabular-nums">
        {formatMediaDuration(durationSeconds)}
      </span>
      <span className="text-muted-foreground rounded-full px-1.5 text-xs">
        1×
      </span>

      {previewUrl ? (
        <audio
          ref={audioRef}
          src={previewUrl}
          aria-label={`${assetName}音频消息`}
          className="hidden"
          onLoadedMetadata={(event) =>
            setDurationSeconds(event.currentTarget.duration)
          }
          onTimeUpdate={(event) =>
            setCurrentSeconds(event.currentTarget.currentTime)
          }
          onEnded={() => {
            setIsPlaying(false)
            setCurrentSeconds(0)
          }}
          onError={() => setIsPlaying(false)}
        />
      ) : null}
    </div>
  )
}
