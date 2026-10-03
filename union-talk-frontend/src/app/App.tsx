import { RouterProvider } from 'react-router'

import { router } from '@/app/router'
import { Toaster } from '@/components/shadcn-ui/sonner'
import { RealtimeProvider } from '@/realtime/realtime-provider'

export default function App() {
  return (
    <>
      <RealtimeProvider>
        <RouterProvider router={router} />
      </RealtimeProvider>
      <Toaster position="top-center" richColors closeButton />
    </>
  )
}
