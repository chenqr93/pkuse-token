# 客户端1 前端（模式一：同域 + 同 Redis）

SSO 客户端示例1 的前端页面（Vue3 + Vite，独立部署）。

- 前端端口：`5174`；后端：`http://localhost:9001`（模式一：与认证中心同为 localhost）
- 页面：
  - `/` 首页：显示登录状态、账号资料，提供统一登录 / 单应用注销 / 单点注销操作
  - `/login` 登录处理页：跳转后端 `/sso/login`，由后端完成与认证中心的交互（同域 Cookie + 共享 Redis，无需 ticket）
- 各客户端差异集中配置在 `src/config.js`

```bash
npm install
npm run dev     # http://localhost:5174
```

> 完整演示步骤见上级目录 README：《四种对接模式客户端 Demo》。
