import { Link, Outlet, useLocation } from 'react-router'

export function AppShell() {
  const location = useLocation()

  if (location.pathname === '/') {
    return <Outlet />
  }

  return (
    <div className="bg-background text-foreground min-h-svh">
      <header className="border-border border-b">
        <div className="mx-auto flex h-14 w-full max-w-6xl items-center justify-between px-4">
          <Link to="/" className="text-sm font-semibold tracking-normal">
            Union Talk
          </Link>
          <nav
            aria-label="Primary navigation"
            className="flex items-center gap-1"
          >
            <Link
              to="/auth"
              className="hover:bg-muted text-muted-foreground hover:text-foreground rounded-md px-3 py-1.5 text-sm transition"
            >
              登录 / 注册
            </Link>
          </nav>
        </div>
      </header>
      <main className="mx-auto w-full max-w-6xl px-4 py-10">
        <Outlet />
      </main>
    </div>
  )
}
