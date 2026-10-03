import { defineConfig } from 'orval'

const gatewayUrl = process.env.OPENAPI_GATEWAY_URL || 'http://localhost:13006'
const transformerPath = './openapi/openapi-transformer.ts'
const mutatorPath = './src/services/http/generated-request.ts'

/**
 * 创建 Union Talk 微服务的 Orval 生成配置.
 * @param serviceKey 微服务标识
 * @param baseUrl Gateway 业务前缀
 * @return Orval 微服务配置
 */
const createServiceConfig = (serviceKey: string, baseUrl: string) => ({
  input: {
    target:
      process.env[`OPENAPI_${serviceKey.toUpperCase()}_URL`] ||
      `${gatewayUrl}/api/v1/v3/api-docs`,
    override: {
      transformer: transformerPath,
    },
  },
  output: {
    target: `./src/services/generated/${serviceKey}/endpoints.ts`,
    schemas: `./src/services/generated/${serviceKey}/models`,
    client: 'axios-functions' as const,
    mode: 'tags-split' as const,
    clean: true,
    baseUrl,
    indexFiles: true,
    tagsSplitDeduplication: true,
    override: {
      mutator: {
        path: mutatorPath,
        name: 'generatedRequest',
      },
    },
  },
})

const authConfig = createServiceConfig('auth', '')
authConfig.input.target =
  process.env.OPENAPI_AUTH_URL || 'http://localhost:13006/api/v1/v3/api-docs'

export default defineConfig({
  auth: authConfig,
  user: createServiceConfig('user', '/user'),
  message: createServiceConfig('message', '/message'),
  file: createServiceConfig('file', '/file'),
})
