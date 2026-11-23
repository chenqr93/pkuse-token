# zfw-auth-center（授权中心 / RBAC 权限管理中心）

面向多应用的统一权限管理中心：应用菜单管理、角色管理、用户授权、授权数据分发（签名开放接口 `/open/authz/*`：菜单树 / 权限码 / 角色编码）；应用/用户档案不落地（实时经用户中心开放接口查询）。
以 pkuse-token 框架 demo 授权中心为基础的正式开发工程（demo 保留在 pkuse-token 仓库作演示）。

## 架构约定

- 接口统一信封 `{"code":200,"msg":"ok","data":...}`；分页 `data={records,total,current,size}`；未登录 401、无权限 403（`/sso/*` 协议端点除外）
- 用户信息与角色拆分：用户信息通道不含任何角色；**控制台自身角色走本地 RBAC**（client=auth-center，种子见 `authcenter-web/src/main/resources/db/upgrade-v3-console-role.sql`，前端经 `GET /api/authz/my-roles` 获取）
- 各应用的角色/权限码由应用引 `pkuse-token-auth-center-client-spring-boot-starter`（cnb 私服）按客户端获取，`checkRole/checkPermission` 开箱即用
- 框架构件来自 cnb 私服（`https://maven.cnb.cool/pkuse/cloud-maven/-/packages/`，Maven settings 已配置）

## 构建

```bash
mvn clean package -DskipTests
```

## 模块

五层：`authcenter-common / authcenter-model / authcenter-service / authcenter-api / authcenter-web`
（启动类在 web 模块，dev 端口 9010；数据库脚本见 `authcenter-web/src/main/resources/db/`，默认 ticket 模式接入用户中心 SSO）。
