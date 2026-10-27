# 客户端5 前端（模式三：跨域 + 跨 Redis + magic-api 快速开发平台）

SSO 客户端示例5 的前端页面（Vue3 + Vite，独立部署）。与客户端3 相同的**模式三**对接（第二实例），
并集成了 **magic-api 接口快速开发平台**的开放调用方管理。

- 前端端口：`5178`；后端：`http://127.0.0.1:9005`（模式三：跨域，独立 Redis db7）
- 页面：
  - `/` 首页
    - SSO 演示：登录状态、统一登录 / 单应用注销 / 单点注销、账号资料
    - **开放接口调用方管理**（登录后显示）：登记 / 编辑 / 启禁用 / 重置密钥 / 删除
      法治中台内部应用与外部第三方应用（对应后端 `/open/clients` 管理接口，库表 `s_open_client`）；
      新登记与重置的密钥**仅展示一次**，需及时下发给调用方
  - `/login` 登录处理页：取认证中心授权地址跳转过去，带 ticket 回跳后调后端 `/sso/doLoginByTicket` 换取本应用 token
- magic-api 在线编辑器：`http://127.0.0.1:9005/api-manage/console`（鉴权方式见后端 application.yml）
- 各客户端差异集中配置在 `src/config.js`

```bash
npm install
npm run dev     # http://127.0.0.1:5178
```

> 完整演示步骤见上级目录 SSO-CLIENTS.md：《四种对接模式客户端 Demo》。
