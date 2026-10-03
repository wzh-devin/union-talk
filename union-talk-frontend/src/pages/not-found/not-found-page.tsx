import { Link } from 'react-router'

import { Button } from '@/components/shadcn-ui/button'

export function NotFoundPage() {
  return (
    <section className="grid min-h-[calc(100svh-9.5rem)] place-items-center">
      <div className="flex max-w-md flex-col items-center gap-4 text-center">
        <p className="text-muted-foreground text-sm font-medium">404</p>
        <h1 className="text-2xl font-semibold tracking-normal">
          Page not found
        </h1>
        <Button asChild>
          <Link to="/">Back home</Link>
        </Button>
      </div>
    </section>
  )
}
