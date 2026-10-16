import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import clientConfig from './src/config.js'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    port: clientConfig.port,
    // 允许自定义 Host 访问（本地演示可用 127.0.0.1 / localhost）
    allowedHosts: true
  }
})
