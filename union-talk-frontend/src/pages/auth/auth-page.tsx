import { AuthFormSwitcher } from '@/components/auth/auth-form-switcher'
import { BorderBeam } from '@/components/magic-ui/border-beam'
import { useAuthForm } from '@/hooks/use-auth-form'

/**
 * 渲染认证页面外壳.
 * @return 认证页面
 */
export const AuthPage = () => {
  const authForm = useAuthForm()

  return (
    <main className="bg-background min-h-svh p-2 sm:p-5">
      <section className="border-border bg-background grid min-h-[calc(100svh-1rem)] place-items-center overflow-hidden rounded-xl border px-4 py-12 sm:min-h-[calc(100svh-2.5rem)]">
        <div className="border-border bg-card text-card-foreground relative w-full max-w-[350px] overflow-hidden rounded-xl border p-6 shadow-sm sm:max-w-[360px]">
          <BorderBeam
            size={86}
            duration={7}
            colorFrom="#111111"
            colorTo="#d4d4d4"
            borderWidth={1.25}
          />

          <div className="relative z-10">
            <AuthFormSwitcher authForm={authForm} />
          </div>
        </div>
      </section>
    </main>
  )
}
