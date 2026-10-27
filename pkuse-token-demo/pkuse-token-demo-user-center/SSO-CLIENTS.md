# 四种对接模式客户端 Demo（SSO Client 1~5）

本目录下的 `client1`~`client4` 后端与 `client1-vue3`~`client4-vue3` 前端，分别演示 Pkuse-Token 单点登录的四种对接模式；
`client5` 是**模式三（跨域 + 跨 Redis）的第二实例**，演示同一模式下接入多个应用：

| 客户端 | 对接模式 | 域关系 | 会话存储 | ticket 校验方式 | 后端 | 前端 |
| --- | --- | --- | --- | --- | --- | --- |
| client1 | **模式一** | 同域（`localhost`，与认证中心共享 Cookie） | 同 Redis（认证中心的 db5） | 无需 ticket（`mode=simple`，直接共享会话） | `localhost:9001` | `localhost:5174` |
| client2 | **模式二** | 跨域（`127.0.0.1` vs `localhost`，Cookie 不共享） | 同 Redis（db5） | ticket **直连 Redis** 校验（`is-http=false`） | `127.0.0.1:9002` | `127.0.0.1:5175` |
| client3 | **模式三** | 跨域 | **独立 Redis**（db6） | ticket 通过 **HTTP 接口**向认证中心校验（`is-http=true`） | `127.0.0.1:9003` | `127.0.0.1:5176` |
| client4 | **NoSdk** | 跨域 | 独立会话（内存，不依赖 Redis） | 手动调用认证中心 `/sso/pushS`（`msgType=checkTicket`），签名自行实现 | `127.0.0.1:9004` | `127.0.0.1:5177` |
| client5 | **模式三（第二实例）** | 跨域 | **独立 Redis**（db7，独立于认证中心 db5 与 client3 db6） | 同 client3：HTTP 接口校验（`is-http=true`） | `127.0.0.1:9005` | `127.0.0.1:5178` |

> **关于"同域 / 跨域"**：本地演示用 `localhost` 与 `127.0.0.1` 模拟两个域（浏览器 Cookie 只按主机名区分，与端口无关）。
> 生产环境对应 `*.user-center.com`（同域，配置 `sa-token.cookie.domain=.user-center.com`）与 `client.xxx.com`（跨域）这样的真实域名。
> 本 demo 无需修改 hosts 文件。

## 一、四种模式的原理差异

```
                 Cookie 共享?   Redis 共享?   ticket 交换
模式一（同域同Redis）    是           是          ✗（直接读共享会话，mode=simple）
模式二（跨域同Redis）    否           是          ✓（客户端直连 Redis 读取 ticket）
模式三（跨域跨Redis）    否           否          ✓（HTTP 调认证中心校验 ticket）
NoSdk（跨域独立存储）    否           否          ✓（手动实现协议与签名）
```

- **模式一**：`client1` 与认证中心同为 `localhost`，浏览器上的 `satoken` Cookie 双方可见；二者又连着同一个 Redis，
  `StpUtil.isLogin()` 在客户端就能直接得到结果 —— 登录只需跳一次认证中心，**不产生 ticket**。
- **模式二**：`client2` 在 `127.0.0.1`（Cookie 拿不到），但和认证中心共用一个 Redis，
  回跳时携带的 ticket 缓存在 Redis 里，客户端**直接读取**完成校验。
- **模式三**：`client3` 用自己的 Redis（`192.168.1.75:6379 db6`），读不到认证中心写的 ticket，
  改为**通过 HTTP 接口**（`/sso/pushS` 的 `checkTicket` 消息）校验；单点注销依赖认证中心的**消息推送**。
- **模式三第二实例（client5）**：与 client3 完全相同的对接方式，但使用**另一个独立 Redis 库 db7**
  （认证中心 db5、client3 db6、client5 db7 互不相通），用于演示跨域跨 Redis 模式下**同时接入多个应用**：
  每个应用各自校验 ticket、各自维护本地会话，单点注销时由认证中心分别向 client3 / client5 推送 `logoutCall`。
- **NoSdk**：`client4` **不引入任何 Pkuse-Token 依赖**，用 SpringBoot Web + JDK HttpClient 手动实现：
  授权地址拼装、`checkTicket`/`signout`/`userinfo` 消息、MD5 参数签名（`timestamp` + `nonce` + `sign`）、
  本地会话（内存 `token → loginId`）、接收 `logoutCall` 推送并校验签名。

## 二、启动顺序

```
① 认证中心：用户中心后端(9000) + 用户中心前端(5173)   ← 见上级 README
② 五个客户端后端：client1(9001)、client2(9002)、client3(9003)、client4(9004)、client5(9005)
③ 五个客户端前端：client1-vue3(5174) ~ client4-vue3(5177)、client5-vue3(5178)
```

```bash
# ① 认证中心（首次需执行 db/init.sql + upgrade-v2/upgrade-v3 脚本；旧库接入 client5 需追加 upgrade-v9-client5.sql）
cd pkuse-token-demo-user-center-server && mvn -DskipTests package && java -jar usercenter-web/target/usercenter-web-0.0.1-SNAPSHOT.jar
cd pkuse-token-demo-user-center-server-vue3 && npm run dev

# ② 五个客户端后端（分层多模块：common / model / service / api / web，与认证中心 server 同风格；可分别启动；内存受限时加 -Xmx256m 等参数）
#    开发热启动：先在聚合根目录 install 各模块，再进入 web 模块 spring-boot:run；
#    或 mvn -DskipTests package 后 java -jar clientN-web/target/clientN-web-0.0.1-SNAPSHOT.jar
cd pkuse-token-demo-user-center-client1 && mvn -DskipTests install && mvn -f client1-web/pom.xml spring-boot:run
cd pkuse-token-demo-user-center-client2 && mvn -DskipTests install && mvn -f client2-web/pom.xml spring-boot:run
cd pkuse-token-demo-user-center-client3 && mvn -DskipTests install && mvn -f client3-web/pom.xml spring-boot:run
cd pkuse-token-demo-user-center-client4 && mvn -DskipTests install && mvn -f client4-web/pom.xml spring-boot:run
cd pkuse-token-demo-user-center-client5 && mvn -DskipTests install && mvn -f client5-web/pom.xml spring-boot:run

# ③ 五个客户端前端
cd pkuse-token-demo-user-center-client1-vue3 && npm install && npm run dev
cd pkuse-token-demo-user-center-client2-vue3 && npm install && npm run dev
cd pkuse-token-demo-user-center-client3-vue3 && npm install && npm run dev
cd pkuse-token-demo-user-center-client4-vue3 && npm install && npm run dev
cd pkuse-token-demo-user-center-client5-vue3 && npm install && npm run dev
```

## 三、演示流程

### 1. 从认证中心（用户中心）进入各应用

`admin / 123456` 登录 <http://localhost:5173> → 工作台「我的应用」点击任一应用卡片
（或「已接入应用」表格的「进入」按钮）→ 浏览器跳到对应客户端 → 完成统一登录并落到客户端首页。

> 卡片的目标地址来自「应用管理」中的**应用入口地址**（如 `http://127.0.0.1:5175/login`）。

### 2. 直接访问各客户端

| 客户端 | 打开地址 | 现象 |
| --- | --- | --- |
| client1（模式一） | <http://localhost:5174> | 未登录 → 点「统一登录」→ 认证中心（如需先登录）→ 直接回到本应用（无 ticket） |
| client2（模式二） | <http://127.0.0.1:5175> | → 认证中心 → 带 ticket 回跳 → 客户端直连 Redis 校验 → 已登录 |
| client3（模式三） | <http://127.0.0.1:5176> | → 认证中心 → 带 ticket 回跳 → HTTP 校验 ticket → 已登录 |
| client4（NoSdk） | <http://127.0.0.1:5177> | → 认证中心 → 带 ticket 回跳 → 手动调 `checkTicket` → 已登录 |
| client5（模式三第二实例） | <http://127.0.0.1:5178> | → 认证中心 → 带 ticket 回跳 → HTTP 校验 ticket → 已登录 |

各客户端首页可查看登录状态、`loginId`、token，并可：

- **获取账号资料**：客户端后端 → 认证中心 `userinfo` 消息（认证中心返回 `id/username/nickname/role`）；
- **单应用注销**：只退出本应用，不影响其它端；
- **单点注销**：通知认证中心注销**当前设备**在各端（含其它客户端）的会话——
  同一账号在**其它设备 / 其它浏览器**上的登录不受影响（按登录设备 `deviceId` 精确下线）；
  需要全端下线时在注销地址显式携带 `singleDeviceIdLogout=false`；
- **重新登录 / 刷新状态**。

### 3. 单点注销（当前设备全端下线）验证

在任意客户端（例如 client4）点击「单点注销」，随后检查四个客户端与认证中心：
**同一浏览器**里登录的各端全部下线；**其它浏览器 / 其它设备**登录的同账号会话保持在线。

| 端 | 结果 | 下线机制 |
| --- | --- | --- |
| client1 / client2 | 当前浏览器内未登录 | 与认证中心共享 Redis，认证中心按 `deviceId` 注销后共享会话即刻失效 |
| client3 | 当前浏览器内未登录 | 认证中心推送 `logoutCall`（含 `deviceId`）→ 客户端 `/sso/pushC` 按设备清除本地会话 |
| client5 | 当前浏览器内未登录 | 同 client3：认证中心推送 `logoutCall` → `/sso/pushC` 清除 db7 中的本地会话 |
| client4 | 当前浏览器内未登录 | NoSdk：本地通知认证中心 `signout`（携带 `deviceId`）+ 清除内存会话；接收推送时按 `loginId + deviceId` 清会话 |
| 认证中心 | 当前浏览器内未登录 | 按设备注销账号会话 |

## 四、关键配置（每个客户端的差异点）

| 配置 | client1 | client2 | client3 | client4（NoSdk） | client5 |
| --- | --- | --- | --- | --- | --- |
| `sa-token.sso-client.client` | `sso-client1` | `sso-client2` | `sso-client3` | —（`sso-client.client-id: sso-client4`） | `sso-client5` |
| `sa-token.sso-client.server-url` | `http://localhost:9000` | 同左 | 同左 | `sso-client.server-url` | 同左 |
| `sa-token.sso-client.is-http` | —（不用 ticket） | `false`（直连 Redis） | **`true`**（HTTP 校验） | —（手动实现） | **`true`**（HTTP 校验） |
| 会话存储 | `sa-token.alone-redisson` → db5 | `alone-redisson` → db5 | `spring.data.redis` → **db6** | 内存（`LocalSessionStore`） | `spring.data.redis` → **db7** |
| 认证中心推送 | 不需要（共享会话） | 不需要（共享会话） | **需要**（`is_push=1`，`push_url=/sso/pushC`） | **需要**（同左） | **需要**（同左） |

> 应用侧的应用标识、回调地址、密钥需与用户中心「应用管理」中的登记一致（本 demo 已通过 `db/upgrade-v3-clients.sql` 初始化）。

## 五、注意事项（踩坑记录）

1. **`PkuseHttpTemplate` 必须自行提供**：凡需要 SDK 主动发起 HTTP 调用的场景（模式三的 ticket 校验、
   单点注销、`userinfo` 消息、认证中心向应用推送），框架默认实现都会抛
   `NotImplException: HTTP 请求处理器未实现`。因此：
   - client1 / client2 / client3 都提供了 `config/HttpTemplateConfig`（JDK HttpClient 实现）；
   - **认证中心（用户中心后端）也需要**——否则单点注销时向模式三/NoSdk 应用推送 `logoutCall` 会失败，
     表现为「本端下线了，跨 Redis 的应用没下线」；
   - 官方 demo 里该实现用的是 Forest（`forest-spring-boot-starter`），本 demo 改用 JDK HttpClient，少一个依赖。
2. **模式一必须"同域"**：若客户端与认证中心不同域，浏览器不会携带认证中心的 `satoken` Cookie，
   模式一即退化为"未登录"。本地用同一 `localhost` 主机名即可。
3. **跨域客户端必须走 ticket**：`127.0.0.1` 与 `localhost` 是不同站点，跨站 XHR 不会带 Cookie（SameSite 限制），
   因此模式二/三/NoSdk 的前端统一采用「ticket 换 token + `satoken` 请求头」的方式，而不是依赖 Cookie。
4. **整页跳转不带自定义请求头**：单点注销是浏览器跳转（非 XHR），所以前端把 token 作为
   `satoken` 参数拼在注销地址上（后端从请求参数读取），否则 NoSdk 客户端拿不到会话信息。
5. **跨域调试需要 CORS + `credentials`**：各客户端后端均放开了对应前端端口的跨域访问
   （`WebCorsConfig`），前端 `fetch` 使用 `credentials: 'include'`。
6. **内存占用**：一台机器同时跑 6 个后端 + 6 个前端（Node）时内存较紧张，
   可给客户端加 `-Xmx256m -XX:MaxMetaspaceSize=192m -XX:TieredStopAtLevel=1`。
7. **`alone-redisson` 一旦引入必须配置**（`sa-token.alone-redisson.config`），否则启动直接报
   「请配置 sa-token.alone-redisson.config 或 file」；模式三不需要它（用 `spring.data.redis`）。
8. **client5 与 client3 是同模式的不同应用**：二者代码结构一致，差异只在
   端口（9005/5178）、应用标识（`sso-client5`）、密钥与 Redis 库（db7）；
   旧库接入 client5 需执行 `db/upgrade-v9-client5.sql` 注册应用（新库 init.sql 已内置）。
