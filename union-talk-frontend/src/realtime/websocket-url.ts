interface WebSocketPageLocation {
  protocol: string
  host: string
}

/**
 * 解析当前部署环境使用的 WebSocket 地址.
 * @param configuredUrl 构建阶段显式配置的地址
 * @param pageLocation 浏览器当前页面地址
 * @return 可用于创建 WebSocket 的完整地址
 */
export const resolveWebSocketUrl = (
  configuredUrl: string | undefined,
  pageLocation: WebSocketPageLocation,
): string => {
  if (configuredUrl) {
    return configuredUrl
  }

  const socketProtocol = pageLocation.protocol === 'https:' ? 'wss:' : 'ws:'
  return `${socketProtocol}//${pageLocation.host}/api/v1/ws`
}
