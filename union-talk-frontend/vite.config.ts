/// <reference types="vitest/config" />

import path from 'node:path'

import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import { defineConfig, loadEnv } from 'vite'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const apiProxyTarget = env.API_PROXY_TARGET || 'http://localhost:13000'
  const webSocketProxyTarget = env.WS_PROXY_TARGET || 'ws://localhost:14001'

  return {
    plugins: [react(), tailwindcss()],
    build: {
      rolldownOptions: {
        output: {
          codeSplitting: {
            groups: [
              {
                name: 'react-vendor',
                test: /node_modules[\\/](react|react-dom|react-router)/,
                priority: 30,
              },
              {
                name: 'ui-vendor',
                test: /node_modules[\\/](radix-ui|lucide-react|motion)/,
                priority: 20,
              },
              {
                name: 'vendor',
                test: /node_modules/,
                maxSize: 400_000,
                priority: 10,
              },
            ],
          },
        },
      },
    },
    test: {
      setupFiles: ['./src/vitest-setup.ts'],
    },
    resolve: {
      alias: {
        '@': path.resolve(__dirname, './src'),
      },
    },
    server: {
      proxy: {
        '/api/v1/ws': {
          target: webSocketProxyTarget,
          changeOrigin: true,
          ws: true,
        },
        '/api/v1': {
          target: apiProxyTarget,
          changeOrigin: true,
        },
      },
    },
  }
})
