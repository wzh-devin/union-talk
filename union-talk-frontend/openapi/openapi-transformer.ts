type JsonRecord = Record<string, unknown>

const httpMethodSet = new Set([
  'get',
  'post',
  'put',
  'patch',
  'delete',
  'head',
  'options',
  'trace',
])

const isRecord = (value: unknown): value is JsonRecord =>
  typeof value === 'object' && value !== null && !Array.isArray(value)

/**
 * 将后端按字符串序列化的大数字契约改写为字符串类型.
 * @param value OpenAPI 节点
 * @return unknown 改写后的 OpenAPI 节点
 */
const transformBigNumberSchema = (value: unknown): unknown => {
  if (Array.isArray(value)) {
    return value.map(transformBigNumberSchema)
  }
  if (!isRecord(value)) {
    return value
  }

  const transformedValue = Object.fromEntries(
    Object.entries(value).map(([key, child]) => [
      key,
      transformBigNumberSchema(child),
    ]),
  )
  const type = transformedValue.type
  const format = transformedValue.format
  const isSerializedInteger =
    type === 'integer' && (format === undefined || format === 'int64')
  const isSerializedDecimal =
    type === 'number' &&
    (format === undefined || (format !== 'float' && format !== 'double'))

  if (isSerializedInteger || isSerializedDecimal) {
    transformedValue.type = 'string'
    delete transformedValue.format
  }

  return transformedValue
}

/**
 * 读取 ApiResult schema 中的业务数据契约.
 * @param schemaName ApiResult schema 名称
 * @param schemaMap OpenAPI schema 映射
 * @return JsonRecord 业务数据契约
 */
const getApiResultDataSchema = (
  schemaName: string,
  schemaMap: JsonRecord,
): JsonRecord => {
  const apiResultSchema = schemaMap[schemaName]
  const properties = isRecord(apiResultSchema)
    ? apiResultSchema.properties
    : undefined
  const dataSchema = isRecord(properties) ? properties.data : undefined

  if (!isRecord(dataSchema)) {
    throw new Error(`${schemaName} 缺少 data 契约`)
  }

  return dataSchema
}

/**
 * 校验 operationId 并解包接口成功响应.
 * @param schema OpenAPI 文档
 * @return void
 */
const transformOperations = (schema: JsonRecord): void => {
  const pathMap = isRecord(schema.paths) ? schema.paths : {}
  const components = isRecord(schema.components) ? schema.components : {}
  const schemaMap = isRecord(components.schemas) ? components.schemas : {}
  const operationIdSet = new Set<string>()

  Object.entries(pathMap).forEach(([path, pathValue]) => {
    if (!isRecord(pathValue)) {
      return
    }

    Object.entries(pathValue).forEach(([method, operationValue]) => {
      if (!httpMethodSet.has(method) || !isRecord(operationValue)) {
        return
      }

      const operationId = operationValue.operationId
      if (typeof operationId !== 'string' || operationId.length === 0) {
        throw new Error(`${method.toUpperCase()} 接口缺少 operationId`)
      }
      if (operationIdSet.has(operationId)) {
        throw new Error(`重复 operationId: ${operationId}`)
      }
      operationIdSet.add(operationId)

      const existingParameterList = [
        ...(Array.isArray(pathValue.parameters) ? pathValue.parameters : []),
        ...(Array.isArray(operationValue.parameters)
          ? operationValue.parameters
          : []),
      ]
      const existingPathParameterNameSet = new Set(
        existingParameterList
          .filter(
            (parameter): parameter is JsonRecord =>
              isRecord(parameter) && parameter.in === 'path',
          )
          .map((parameter) => parameter.name)
          .filter((name): name is string => typeof name === 'string'),
      )
      const missingParameterList = Array.from(
        path.matchAll(/\{([^}]+)\}/g),
        (match) => match[1],
      )
        .filter((name) => !existingPathParameterNameSet.has(name))
        .map((name) => ({
          name,
          in: 'path',
          required: true,
          schema: { type: 'string' },
        }))

      if (missingParameterList.length > 0) {
        operationValue.parameters = [
          ...(Array.isArray(operationValue.parameters)
            ? operationValue.parameters
            : []),
          ...missingParameterList,
        ]
      }

      const responseMap = isRecord(operationValue.responses)
        ? operationValue.responses
        : {}
      Object.values(responseMap).forEach((responseValue) => {
        const contentMap =
          isRecord(responseValue) && isRecord(responseValue.content)
            ? responseValue.content
            : {}

        Object.values(contentMap).forEach((mediaValue) => {
          if (!isRecord(mediaValue) || !isRecord(mediaValue.schema)) {
            return
          }

          const schemaReference = mediaValue.schema.$ref
          if (
            typeof schemaReference !== 'string' ||
            !schemaReference.startsWith('#/components/schemas/ApiResult')
          ) {
            throw new Error(`${operationId} 响应未使用 ApiResult`)
          }
          const schemaName = schemaReference.split('/').at(-1)
          if (!schemaName) {
            return
          }
          mediaValue.schema = getApiResultDataSchema(schemaName, schemaMap)
        })
      })
    })
  })
}

/**
 * 将后端 Swagger 转换为前端可直接生成 service 的契约.
 * @param schema 原始 OpenAPI 文档
 * @return T 转换后的 OpenAPI 文档
 */
export const transformOpenApi = <T>(schema: T): T => {
  const transformedSchema = transformBigNumberSchema(structuredClone(schema))

  if (!isRecord(transformedSchema)) {
    throw new Error('OpenAPI 文档格式无效')
  }

  transformOperations(transformedSchema)
  return transformedSchema as T
}

export default transformOpenApi
