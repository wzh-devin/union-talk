export interface TraceDetailItem {
  id: string
  label: string
  value: string
}

/**
 * 渲染步骤输入或结果摘要.
 * @param props 摘要名称与键值列表
 * @return 摘要定义列表
 */
export const TraceDetailSection = ({
  label,
  detailList,
}: {
  label: string
  detailList: TraceDetailItem[]
}) => {
  if (detailList.length === 0) {
    return null
  }
  return (
    <div className="grid gap-2 sm:grid-cols-[72px_1fr]">
      <p className="text-muted-foreground text-xs leading-6">{label}</p>
      <dl className="grid min-w-0 gap-2">
        {detailList.map((detail) => (
          <div
            key={detail.id}
            className="grid min-w-0 gap-1 text-xs leading-5 sm:grid-cols-[88px_minmax(0,1fr)] sm:gap-3"
          >
            <dt className="text-muted-foreground">{detail.label}</dt>
            <dd className="text-foreground min-w-0 break-words">
              {detail.value}
            </dd>
          </div>
        ))}
      </dl>
    </div>
  )
}
