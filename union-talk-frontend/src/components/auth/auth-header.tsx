export interface AuthHeaderProps {
  title: string
  description: string
}

/**
 * 渲染认证表单标题.
 * @param props 标题文案属性
 * @return 认证表单标题
 */
export const AuthHeader = ({ title, description }: AuthHeaderProps) => (
  <div className="space-y-1.5">
    <h1 className="text-foreground text-base font-semibold tracking-normal">
      {title}
    </h1>
    <p className="text-muted-foreground text-sm leading-5">{description}</p>
  </div>
)
