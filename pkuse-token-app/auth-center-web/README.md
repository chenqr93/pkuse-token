# 授权中心前端（auth-center-web）

统一权限（RBAC）管理控制台：概览 / 菜单管理 / 角色管理 / 用户授权 / 操作日志。

- 技术栈：Vue 2.7 + Vue CLI 4.5（webpack 4） + Vue Router 3 + Axios + Ant Design Vue 1.6
- 开发端口：`5180`（`/authcenter/api`、`/authcenter/sso` 代理到 `http://localhost:9010`）
- 生产路径：`/authcenter/`
- 主题变量见 `src/styles/tokens.less`

## 启动

```bash
npm install --legacy-peer-deps
npm run serve
NODE_OPTIONS=--openssl-legacy-provider npm run build
```

> Node 17+ 构建需 `NODE_OPTIONS=--openssl-legacy-provider`。

## SSO

登录由用户中心统一完成。进入控制台时校验本地 `authcenter-token` 会话与用户中心当前 SSO 用户是否一致，
不一致时通过 `/authcenter/sso/switch` 重新换取 ticket（窗口重新聚焦时也会校验）；用户中心已注销时回到统一登录页；当前用户未授权 `auth-center` 应用时展示无权限提示，避免后端返回原始 JSON。
