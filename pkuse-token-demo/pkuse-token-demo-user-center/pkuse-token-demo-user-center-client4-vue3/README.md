# 客户端4 前端（NoSdk：手动实现协议）

SSO 客户端示例4 的前端页面（Vue3 + Vite，独立部署）。

- 前端端口：`5177`；后端：`http://127.0.0.1:9004`（NoSdk：跨域，本地内存会话）
- 页面：
  - `/` 首页：显示登录状态、账号资料，提供统一登录 / 单应用注销 / 单点注销操作
  - `/login` 登录处理页：取认证中心授权地址跳转过去，带 ticket 回跳后调后端 `/sso/doLoginByTicket`（后端手动调认证中心 checkTicket）换取本地 token
- 各客户端差异集中配置在 `src/config.js`

```bash
npm install
npm run dev     # http://127.0.0.1:5177
```

> 完整演示步骤见上级目录 README：《四种对接模式客户端 Demo》。
