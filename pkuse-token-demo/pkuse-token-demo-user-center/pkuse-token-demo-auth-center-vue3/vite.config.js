import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5180,
    // 开发环境代理：/api（控制台接口）、/sso（SSO 协议地址）转发到授权中心后端（9010）
    proxy: {
      '/api': {
        target: 'http://localhost:9010',
        changeOrigin: true
      },
      '/sso': {
        target: 'http://localhost:9010',
        changeOrigin: true
      }
    }
  }
})
