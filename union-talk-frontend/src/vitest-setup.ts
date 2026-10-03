class ResizeObserverMock implements ResizeObserver {
  /**
   * 创建测试环境的 ResizeObserver 替身.
   * @param callback 尺寸变化回调
   */
  constructor(callback: ResizeObserverCallback) {
    void callback
  }

  /**
   * 注册观察目标，测试环境无需执行尺寸计算.
   * @param target 观察目标
   * @param options 观察选项
   * @return void
   */
  observe(target: Element, options?: ResizeObserverOptions): void {
    void target
    void options
  }

  /**
   * 取消观察目标.
   * @param target 观察目标
   * @return void
   */
  unobserve(target: Element): void {
    void target
  }

  /** 释放所有观察目标. */
  disconnect(): void {}
}

if (!globalThis.ResizeObserver) {
  globalThis.ResizeObserver = ResizeObserverMock
}

/**
 * 创建供富文本编辑器定位光标使用的零尺寸矩形.
 * @return DOMRect 零尺寸矩形
 */
const createZeroDomRect = (): DOMRect => new DOMRect(0, 0, 0, 0)

/**
 * 创建供富文本编辑器读取选区范围使用的矩形列表.
 * @return DOMRectList 仅包含零尺寸矩形的列表
 */
const createDomRectList = (): DOMRectList => {
  const rect = createZeroDomRect()
  return Object.assign([rect], {
    item: (index: number) => (index === 0 ? rect : null),
  }) as unknown as DOMRectList
}

if (typeof document !== 'undefined' && typeof Range !== 'undefined') {
  if (!document.elementFromPoint) {
    document.elementFromPoint = () => document.body
  }

  if (!Range.prototype.getBoundingClientRect) {
    Range.prototype.getBoundingClientRect = createZeroDomRect
  }

  if (!Range.prototype.getClientRects) {
    Range.prototype.getClientRects = createDomRectList
  }
}
