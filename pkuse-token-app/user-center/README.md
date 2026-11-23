# user-center（用户中心 / 统一认证中心）

SSO-Server：统一登录（SM2/SM3、滑块验证码、设备锁）、用户/组织/区划/应用管理、登录安全策略、开放注册表接口（`/open/registry/*`）。
以 pkuse-token 框架 demo 用户中心为基础的正式开发工程（demo 保留在 pkuse-token 仓库作演示）。

## 架构约定

- 接口统一信封 `{"code":200,"msg":"ok","data":...}`；分页 `data={records,total,current,size}`；未登录 401、无权限 403（`/sso/*` 协议端点除外）
- 用户信息与角色拆分：`userinfo` 消息 / `/api/auth/me` / 登录响应只含纯身份信息（id/username/nickname）；平台角色走 `GET /api/auth/my-roles`（本控制台专用），应用内角色由各应用经授权中心客户端 starter 按客户端获取
- 框架构件来自 cnb 私服（`https://maven.cnb.cool/pkuse/cloud-maven/-/packages/`，Maven settings 已配置）

## 构建

```bash
mvn clean package -DskipTests
```

## 模块

五层：`usercenter-common / usercenter-model / usercenter-service / usercenter-api / usercenter-web`
（启动类在 web 模块，dev 端口 9000；数据库脚本见 `usercenter-web/src/main/resources/db/`，KingbaseES + Redis db5）。

## 身份证号国密加密存储（SM4，upgrade-v16）

- **存储**：`uc_user.id_card` 存 SM4 密文（格式 `SM4: + hex(IV + 密文)`，CBC + PKCS7，随机 IV —— 同一身份证号每次加密结果不同）；`uc_user.id_card_hash` 存 SM3 摘要（带索引）。
- **精确查询**：`/open/registry/users/by-id-cards` 等按身份证匹配的场景，入参先取 SM3 摘要，再按 `id_card_hash` 等值 `IN` 查询（密文随机 IV 无法直接等值匹配）。
- **明文出口**：管理端（`/api/user/*`）与开放接口出口处由 Controller 用 `IdCardCipher` 解密回填明文；`from()` 静态工厂不携带密文出域；前端列表脱敏展示（前六后四）。
- **存量迁移**：启动时 `IdCardCryptoMigrator` 自动把明文行加密并补摘要（幂等，单行失败不阻断启动，读取侧兼容明文）；读取不带 `SM4:` 前缀的值原样返回，平滑过渡。
- **密钥**：`user-center.crypto.id-card-sm4-key`（各环境 yml 独立的 32 位十六进制 / 128 位密钥）。存储加密密钥**一经启用不可更换**（换密钥 = 历史密文不可解）；缺失或格式非法时服务启动即失败（fail-fast）。生产上线前必须替换 prod 占位密钥并移入受控配置管理。
