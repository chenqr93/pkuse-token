import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    // 开发环境代理：/api（管理接口）、/sso（统一登录协议地址）转发到用户中心后端
    proxy: {
      '/api': {
        target: 'http://localhost:9000',
        changeOrigin: true
      },
      '/sso': {
        target: 'http://localhost:9000',
        changeOrigin: true
      }
    }
  }
})
