import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/suggest': 'http://localhost:8080',
      '/search': 'http://localhost:8080',
      '/trending': 'http://localhost:8080',
      '/api': 'http://localhost:8080',
    },
  },
  build: {
    rollupOptions: {
      input: {
        main: './index.html',
      },
    },
  },
})
