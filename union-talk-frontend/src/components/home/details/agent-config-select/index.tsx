import { Field, FieldLabel } from '@/components/shadcn-ui/field'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/shadcn-ui/select'

interface AgentConfigSelectOption {
  value: string
  label: string
}

interface AgentConfigSelectProps {
  id: string
  label: string
  value: string
  optionList: readonly AgentConfigSelectOption[]
  onValueChange: (value: string) => void
}

/**
 * 渲染 Agent 配置统一下拉控件.
 * @param props 下拉字段、选项和变更事件
 * @return Agent 配置下拉字段
 */
export const AgentConfigSelect = ({
  id,
  label,
  value,
  optionList,
  onValueChange,
}: AgentConfigSelectProps) => (
  <Field className="gap-1.5">
    <FieldLabel htmlFor={id} className="text-xs font-medium">
      {label}
    </FieldLabel>
    <Select value={value} onValueChange={onValueChange}>
      <SelectTrigger
        id={id}
        aria-label={label}
        data-agent-control
        data-agent-config-select
        size="lg"
        className="w-full min-w-0 px-3 text-sm font-normal"
      >
        <SelectValue />
      </SelectTrigger>
      <SelectContent
        position="popper"
        align="start"
        className="w-(--radix-select-trigger-width) rounded-[10px] p-1"
      >
        {optionList.map((option) => (
          <SelectItem
            key={option.value}
            value={option.value}
            className="min-h-9 px-2.5 py-1.5 text-sm font-normal"
          >
            {option.label}
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  </Field>
)
