# Union Talk Frontend

Vite + React + TypeScript frontend foundation for Union Talk.

## Stack

- Vite
- React
- TypeScript
- React Router
- Axios
- Tailwind CSS
- shadcn/ui
- Magic UI
- ESLint
- Prettier
- lint-staged
- commitlint
- Husky

## Commands

```bash
pnpm install
pnpm dev
pnpm typecheck
pnpm lint
pnpm format:check
pnpm build
```

## Structure

```text
src/
  app/
  assets/
  components/
    common/
    magic-ui/
    shadcn-ui/
  hooks/
  lib/
  pages/
  services/
    http/
  styles/
```

## Conventions

- Use `@/*` for source imports.
- Put shadcn/ui components in `src/components/shadcn-ui`.
- Put Magic UI components in `src/components/magic-ui`.
- Put app-specific shared components in `src/components/common`.
- Configure API base URL with `VITE_API_BASE_URL`; use `/api/v1` in local development so Vite can proxy gateway requests via `API_PROXY_TARGET`.
