interface DetailGroupProps {
  title: string
  detailItemList: Array<[string, string]>
}

export const DetailGroup = ({ title, detailItemList }: DetailGroupProps) => (
  <div>
    <p className="text-muted-foreground mb-3 text-xs font-medium">{title}</p>
    <div className="space-y-2">
      {detailItemList.map(([label, value]) => (
        <div
          key={label}
          data-testid="detail-group-row"
          className="grid grid-cols-[72px_minmax(0,1fr)] items-start gap-x-4 text-xs leading-5"
        >
          <span className="text-muted-foreground">{label}</span>
          <span className="text-foreground min-w-0 text-left break-words">
            {value}
          </span>
        </div>
      ))}
    </div>
  </div>
)
