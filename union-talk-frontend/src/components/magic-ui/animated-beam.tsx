import { motion } from 'motion/react'
import { useEffect, useId, useState, type RefObject } from 'react'

import { cn } from '@/utils/class-name'

export interface AnimatedBeamProps {
  className?: string
  direction?: 'horizontal' | 'vertical'
  containerRef: RefObject<HTMLElement | null>
  fromRef: RefObject<HTMLElement | null>
  toRef: RefObject<HTMLElement | null>
  curvature?: number
  reverse?: boolean
  pathColor?: string
  pathWidth?: number
  pathOpacity?: number
  gradientStartColor?: string
  gradientStopColor?: string
  delay?: number
  duration?: number
  repeat?: number
  repeatDelay?: number
  startXOffset?: number
  startYOffset?: number
  endXOffset?: number
  endYOffset?: number
}

export const AnimatedBeam = ({
  className,
  direction = 'horizontal',
  containerRef,
  fromRef,
  toRef,
  curvature = 0,
  reverse = false,
  duration = 5,
  delay = 0,
  pathColor = 'gray',
  pathWidth = 2,
  pathOpacity = 0.2,
  gradientStartColor = '#ffaa40',
  gradientStopColor = '#9c40ff',
  repeat = Infinity,
  repeatDelay = 0,
  startXOffset = 0,
  startYOffset = 0,
  endXOffset = 0,
  endYOffset = 0,
}: AnimatedBeamProps) => {
  const id = useId()
  const [pathD, setPathD] = useState('')
  const [svgDimensions, setSvgDimensions] = useState({ width: 0, height: 0 })
  const gradientCoordinates =
    direction === 'vertical'
      ? reverse
        ? {
            x1: ['0%', '0%'],
            x2: ['0%', '0%'],
            y1: ['90%', '-10%'],
            y2: ['100%', '0%'],
          }
        : {
            x1: ['0%', '0%'],
            x2: ['0%', '0%'],
            y1: ['10%', '110%'],
            y2: ['0%', '100%'],
          }
      : reverse
        ? {
            x1: ['90%', '-10%'],
            x2: ['100%', '0%'],
            y1: ['0%', '0%'],
            y2: ['0%', '0%'],
          }
        : {
            x1: ['10%', '110%'],
            x2: ['0%', '100%'],
            y1: ['0%', '0%'],
            y2: ['0%', '0%'],
          }

  useEffect(() => {
    const updatePath = () => {
      if (!containerRef.current || !fromRef.current || !toRef.current) {
        return
      }

      const containerRect = containerRef.current.getBoundingClientRect()
      const sourceRect = fromRef.current.getBoundingClientRect()
      const targetRect = toRef.current.getBoundingClientRect()
      const startX =
        sourceRect.left -
        containerRect.left +
        sourceRect.width / 2 +
        startXOffset
      const startY =
        sourceRect.top -
        containerRect.top +
        sourceRect.height / 2 +
        startYOffset
      const endX =
        targetRect.left - containerRect.left + targetRect.width / 2 + endXOffset
      const endY =
        targetRect.top - containerRect.top + targetRect.height / 2 + endYOffset

      setSvgDimensions({
        width: containerRect.width,
        height: containerRect.height,
      })
      setPathD(
        `M ${startX},${startY} Q ${(startX + endX) / 2},${
          startY - curvature
        } ${endX},${endY}`,
      )
    }

    const resizeObserver = new ResizeObserver(updatePath)

    if (containerRef.current) {
      resizeObserver.observe(containerRef.current)
    }
    updatePath()

    return () => resizeObserver.disconnect()
  }, [
    containerRef,
    curvature,
    endXOffset,
    endYOffset,
    fromRef,
    startXOffset,
    startYOffset,
    toRef,
  ])

  return (
    <svg
      aria-hidden="true"
      data-direction={direction}
      fill="none"
      width={svgDimensions.width}
      height={svgDimensions.height}
      xmlns="http://www.w3.org/2000/svg"
      className={cn(
        'pointer-events-none absolute top-0 left-0 transform-gpu stroke-2',
        className,
      )}
      viewBox={`0 0 ${svgDimensions.width} ${svgDimensions.height}`}
    >
      <path
        d={pathD}
        stroke={pathColor}
        strokeWidth={pathWidth}
        strokeOpacity={pathOpacity}
        strokeLinecap="round"
      />
      <path
        d={pathD}
        strokeWidth={pathWidth}
        stroke={`url(#${id})`}
        strokeOpacity="1"
        strokeLinecap="round"
      />
      <defs>
        <motion.linearGradient
          className="transform-gpu"
          id={id}
          gradientUnits="userSpaceOnUse"
          initial={{ x1: '0%', x2: '0%', y1: '0%', y2: '0%' }}
          animate={gradientCoordinates}
          transition={{
            delay,
            duration,
            ease: [0.16, 1, 0.3, 1],
            repeat,
            repeatDelay,
          }}
        >
          <stop stopColor={gradientStartColor} stopOpacity="0" />
          <stop stopColor={gradientStartColor} />
          <stop offset="32.5%" stopColor={gradientStopColor} />
          <stop offset="100%" stopColor={gradientStopColor} stopOpacity="0" />
        </motion.linearGradient>
      </defs>
    </svg>
  )
}
