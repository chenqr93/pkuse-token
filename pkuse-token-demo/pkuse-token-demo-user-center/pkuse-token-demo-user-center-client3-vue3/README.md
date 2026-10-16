# 客户端3 前端（模式三：跨域 + 跨 Redis）

SSO 客户端示例3 的前端页面（Vue3 + Vite，独立部署）。

- 前端端口：`5176`；后端：`http://127.0.0.1:9003`（模式三：跨域，独立 Redis）
- 页面：
  - `/` 首页：显示登录状态、账号资料，提供统一登录 / 单应用注销 / 单点注销操作
  - `/login` 登录处理页：取认证中心授权地址跳转过去，带 ticket 回跳后调后端 `/sso/doLoginByTicket` 换取本应用 token
- 各客户端差异集中配置在 `src/config.js`

```bash
npm install
npm run dev     # http://127.0.0.1:5176
```

> 完整演示步骤见上级目录 README：《四种对接模式客户端 Demo》。
