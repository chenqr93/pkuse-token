# Pkuse-Token 用户中心示例（统一登录 / SSO 认证中心）

基于 **Pkuse-Token** 的用户中心 Demo：包含 **登录认证、用户管理、应用（Client）管理、工作台**，并作为 **SSO 认证中心（SSO-Server）** 为各接入应用提供统一登录能力。

```
pkuse-token-demo-user-center/
├── pkuse-token-demo-user-center-server/        # 用户中心后端（分层多模块，聚合父 POM 在本目录）
│   ├── usercenter-common/                      #   通用层：注解 / 业务异常 / 工具 / 平台配置定义（零内部依赖叶子）
│   ├── usercenter-model/                       #   数据模型层：域优先 com.bdsoft.usercenter.{域}.{po,bo,dto,vo}
│   ├── usercenter-service/                     #   业务层：域优先 {域}.service(.impl) / {域}.mapper / {域}.support
│   ├── usercenter-api/                         #   对外接口层：域优先 com.bdsoft.usercenter.{域}.controller
│   └── usercenter-web/                         #   启动与装配：启动类 / Spring 配置 / application.yml / db 脚本
├── pkuse-token-demo-user-center-server-vue3/   # 用户中心前端（Vue3 + Vite + Element Plus）
├── SSO-CLIENTS.md                              # ★ 四种对接模式客户端 Demo 说明（模式一/二/三/NoSdk）
├── pkuse-token-demo-user-center-client1/       # 客户端1 后端：模式一（同域 + 同 Redis）
├── pkuse-token-demo-user-center-client1-vue3/  # 客户端1 前端（Vue3）
├── pkuse-token-demo-user-center-client2/       # 客户端2 后端：模式二（跨域 + 同 Redis）
├── pkuse-token-demo-user-center-client2-vue3/  # 客户端2 前端（Vue3）
├── pkuse-token-demo-user-center-client3/       # 客户端3 后端：模式三（跨域 + 跨 Redis）
├── pkuse-token-demo-user-center-client3-vue3/  # 客户端3 前端（Vue3）
├── pkuse-token-demo-user-center-client4/       # 客户端4 后端：NoSdk（不依赖 SDK，手动实现协议）
└── pkuse-token-demo-user-center-client4-vue3/  # 客户端4 前端（Vue3）
```

## 一、功能清单

| 模块 | 说明 |
| --- | --- |
| 统一登录页 | 账号密码登录；支持被应用重定向唤起（登录后自动跳回应用）；密码 **SM2 国密加密传输**、**滑块验证码**、**新设备验证** |
| 工作台 | **系统管理员**：用户/应用统计、**当前在线人数**（持有有效登录会话的账号数，退出/被踢/删除账号即时下降）、已接入应用、认证中心信息；**普通用户**：只有「我的应用」，即已授权应用入口 |
| 用户管理 | 用户增删改查、启用/禁用、重置密码、**授权应用**（用户-应用绑定，支持**多选用户批量授权**：追加/覆盖两种模式）、**登录锁定与在线解锁**、**设备管理（解绑）**；内置管理员 `admin` 为系统账号，**不出现在用户列表中**（统计口径同步排除） |
| 应用管理 | 接入应用（SSO Client）增删改查、启用/禁用、回调地址/入口地址/密钥配置，**变更即时同步到认证中心，无需重启** |
| 登录日志 | 全量登录/注销日志（成功、密码错误、锁定、验证码错误、设备锁拦截、设备验证/换绑），**按月份表存储**（KingbaseES 声明式分区，自动建表），支持账号/结果/时间范围查询 |
| 操作日志 | 平台管理动作审计：新增/修改/删除用户、重置密码、启用禁用、授权与批量授权、在线解锁、解绑设备，应用的新增/修改/启用禁用/删除等；记录操作人、模块、动作、请求方法、**请求参数摘要**、**修改前/修改后数据快照**（新增=传参+写入数据、更新=传参+修改前+修改后、删除=传参+删除前数据；敏感字段脱敏，由 MyBatis-Plus 拦截器自动捕获、不限定实体类型）、结果与说明、耗时、IP/UA，**成功与失败都记录**；同样**按月份表存储**（`uc_oper_log_yyyyMM`），支持操作人/模块/动作/结果/时间范围筛选与参数详情查看 |
| 规则配置 | **仅系统管理员**：在线维护平台规则——**密码复杂度**（最小长度/大小写/数字/特殊字符）、**首次登录强制修改密码**、**设备验证（设备锁）开关**、**踢掉同类型设备在线账号**（PC/移动端/小程序同类型互踢）、**滑块验证码开关与触发阈值**、**系统默认密码**、**失败锁定策略**（阈值/窗口/时长）、**登录日志与操作日志的分区频率**（按月/按周/按天）；保存后**即时生效**（无需重启），支持一键恢复默认 |
| 登录安全 | **密码国密 SM3 摘要**（随机盐，历史 MD5 无感升级）；**连续密码错误锁定 + 管理员在线解锁**；**滑块验证码**（tianai-captcha）；**设备锁 + 设备指纹**（首次登录绑定，更换设备需手机验证码验证后自动换绑）；**首次登录强制修改密码**；**多端在线与同类型设备互踢**（同一账号可分别在 PC / 移动端 / 小程序在线，「规则配置 - 踢掉同类型设备在线账号」开启后同类型新登录会顶掉该类型旧会话）；以上策略均可在「规则配置」中在线调整 |
| 客户端对接 Demo | 四种对接模式：模式一（同域+同Redis）、模式二（跨域+同Redis）、模式三（跨域+跨Redis）、NoSdk（手动实现协议）——详见 [SSO-CLIENTS.md](SSO-CLIENTS.md) |
| 权限模型 | **系统管理员**（role=admin）：可维护用户与应用、可进入全部应用；**普通用户**：只能进入工作台，且**只能进入已授权应用**（工作台入口与统一登录两处校验） |

## 二、技术栈

| 端 | 技术 |
| --- | --- |
| 后端 | SpringBoot 3.5.16 / JDK 17 / MyBatis-Plus 3.5.14 / 人大金仓 KingbaseES（kingbase8 驱动）/ Pkuse-Token 1.46.0（SSO-Server）/ Redisson（会话共享）/ tianai-captcha 1.5.3（滑块验证码）/ BouncyCastle 1.81（国密 SM2/SM3）/ Spring AOP（操作日志切面） |
| 前端 | Vue 3.4 + Vite 4 + Vue Router 4 + Axios + Element Plus 2.8 + sm-crypto（国密 SM2） |
| 存储 | KingbaseES：`uc_user`（用户）、`uc_client`（接入应用）、`uc_user_client`（授权绑定）、`uc_user_device`（设备指纹）、`uc_login_log`（登录日志，按月分表）、`uc_oper_log`（操作日志，按月分表）、`uc_config`（规则配置）；Redis：登录会话（token）、失败计数、验证码/验证凭证 |

## 三、快速开始

### 1. 前置条件

- JDK 17
- Node.js 16+（本项目在 Node 16.20 下验证通过）
- KingbaseES 实例（本示例默认连接 `192.168.1.77:54321`，可按需修改）
- Redis 实例（本示例默认连接 `192.168.1.75:6379`，可按需修改）

### 2. 初始化数据库

```bash
# 在 Kingbase 中创建数据库（如不存在）
ksql -h 192.168.1.77 -p 54321 -U DEV_USER_CENTER -d postgres \
     -c 'CREATE DATABASE "PKUSE_TOKEN_USER_CENTER" WITH OWNER = "DEV_USER_CENTER" ENCODING = '"'"'UTF8'"'"';'

# 执行初始化脚本（建表 + 种子数据）
ksql -h 192.168.1.77 -p 54321 -U DEV_USER_CENTER -d PKUSE_TOKEN_USER_CENTER \
     -f pkuse-token-demo-user-center-server/usercenter-web/src/main/resources/db/init.sql
```

> **注意：主键已从雪花 ID 切换为 32 位 UUID 字符串（VARCHAR(32)），旧版本升级需重建本库（数据不保留）；新库直接执行本脚本。**
>
> 脚本会创建 `uc_user`、`uc_client`、`uc_user_client`、`uc_user_device`、`uc_login_log`（登录日志，分区父表）、
> `uc_oper_log`（操作日志，分区父表）、`uc_config`（规则配置）等表，
> 并写入演示数据（3 个用户、4 个应用、3 条授权关系）；日志的月分表由后端启动时/每日定时自动创建。
> 「规则配置」中的 17 项配置默认值同样由后端启动时自动写入（后续版本新增配置项无需执行 SQL）。
>
> 已按旧版本部署过的库，按顺序执行增量脚本即可：
> ```bash
> cd pkuse-token-demo-user-center-server/usercenter-web/src/main/resources/db
> for f in upgrade-v2-authorization.sql upgrade-v3-clients.sql upgrade-v4-login-security.sql upgrade-v5-sm-crypto.sql upgrade-v6-oper-log.sql upgrade-v7-config.sql; do
>   ksql -h 192.168.1.77 -p 54321 -U DEV_USER_CENTER -d PKUSE_TOKEN_USER_CENTER -f $f
> done
> ```
>
> **注意**：KingbaseES 保留了 `sys_` 前缀（`sys_catalog` 下存在系统视图 `sys_user` 等），
> 业务表不要使用 `sys_` 前缀，否则 unqualified 表名会解析到系统对象上。

### 3. 修改配置

编辑 `pkuse-token-demo-user-center-server/usercenter-web/src/main/resources/application.yml`：

```yaml
spring:
  datasource:                      # 数据源：Kingbase
    url: jdbc:kingbase8://192.168.1.77:54321/PKUSE_TOKEN_USER_CENTER
    username: DEV_USER_CENTER
    password: Beid@s0ft
  data:
    redis:                         # 会话存储：Redis（各接入应用共享）
      host: 192.168.1.75
      port: 6379
      database: 5
      password: bds0ft@2025

user-center:
  frontend-base-url: http://localhost:8090   # 统一登录页地址（未登录访问 /sso/auth 时跳转到这里）
  # 登录安全：仅保留部署级参数；失败计数窗口/锁定阈值/验证码策略/设备验证开关/默认密码/密码复杂度/
  #          日志分区频率等运行期规则已迁移到「规则配置」模块（表 uc_config，管理员在线维护，即时生效）
  login:
    max-devices: 1                   # 允许绑定的设备数
    device-lock-exempt-admin: false  # 内置管理员是否豁免设备锁
    device-verify-minutes: 5         # 新设备短信验证码有效期（分钟）
    device-verify-show-code: true    # 演示模式：验证码随响应返回（生产必须 false）

captcha:
  expire:
    default: 120000              # 滑块验证码有效期（毫秒）
```

### 4. 启动后端（端口 8091）

```bash
cd pkuse-token-demo-user-center-server
mvn -DskipTests package                 # 一次构建 5 个模块；-P dev|test|uat|prod 选择打包环境（默认 dev）
cd usercenter-web/target/usercenter-web-0.0.1-SNAPSHOT-assembly/pkuse-token-demo-user-center-server-0.0.1-SNAPSHOT
bin\start.bat                          # Windows 启动（应用瘦包在根目录，bin/ conf/ lib/，conf 为外置配置）
# Linux/macOS：./bin/start.sh（内置 JVM/GC 日志参数）；停止：bin\stop.bat 或 ./bin/stop.sh

# 开发热启动（需先把各模块安装到本地仓库）
mvn -DskipTests install && mvn -f usercenter-web/pom.xml spring-boot:run
```

启动日志中应看到 SSO 客户端同步信息：

```
SSO 客户端配置已同步，当前生效应用：[sso-client1, sso-client2, sso-client3]
```

### 5. 启动前端（端口 8090）

```bash
cd pkuse-token-demo-user-center-server-vue3
npm install
npm run dev
```

浏览器访问 <http://localhost:8090>（开发环境已配置 `/api`、`/sso` 代理到 `http://localhost:8091`）。

### 6. 演示账号

| 账号 | 密码 | 角色 | 说明 |
| --- | --- | --- | --- |
| admin | 123456 | 系统管理员 | 内置管理员（不参与用户管理、不可删除/禁用）；可维护用户与应用，可进入全部应用 |
| zhangsan | 123456 | 普通用户 | 已授权：客户端1、客户端2（工作台只显示这两个入口） |
| lisi | 123456 | 普通用户 | 初始为禁用状态（用于演示禁用拦截）；已授权：客户端3 |

## 四、演示流程

### 管理员视角（admin / 123456）

1. **统一登录**：打开 <http://localhost:8090> → 登录 → 进入工作台。
2. **工作台**：查看用户/应用统计与**当前在线人数**（首位红色卡片；基于 Redis 登录会话统计，
   退出登录、被踢下线、删除用户会即时下降；删除用户时其全部在线会话同步注销）、
   已接入应用列表（含「已授权用户」数与同步状态）、认证中心配置、我的应用入口（管理员为全部启用应用）。
3. **用户管理**：新增用户（不填密码默认 `123456`）、编辑、启用/禁用、重置密码、删除。
   - 内置管理员 `admin` 是系统账号，不出现在用户列表中；直接调用接口操作它也会被拦截。
   - 其他保护逻辑：不能删除/禁用当前登录账号；用禁用账号登录会被拦截（提示「该账号已被禁用」）。
   - **授权应用**：点击某用户的「授权应用」按钮，勾选其可访问的应用并保存（列表中的「已授权应用」列即时更新）。
   - **批量授权**：勾选用户列表左侧的多选框（可多选）→ 点击工具栏「批量授权（N）」→ 在弹窗中选择应用与授权模式：
     **追加授权**（在原有基础上新增，已授权的自动跳过）或 **覆盖授权**（把所选用户的应用授权重置为勾选集合，不勾选则清空授权）。
4. **应用管理**：新增/编辑应用、启用/禁用、删除；配置**回调地址**（授权白名单）与**应用入口**（工作台进入应用的目标地址，通常是应用的 `/sso/login?back=首页`）。
   - 新增应用后工作台的「已注册应用」即时增加（同步到 SSO 认证中心）。
   - **禁用某个应用后，该应用再发起统一登录会被认证中心拒绝**（`未能获取应用信息`）。
5. **测试普通用户视角**：退出后用 `zhangsan / 123456` 登录（或另开无痕窗口）。

### 普通用户视角（zhangsan / 123456）

1. 登录后左侧**只有「工作台」**（用户管理、应用管理菜单不可见）；直接访问 `/user`、`/client` 会被路由守卫拦回工作台，后端接口也会返回 `code=403 无权限访问`。
2. 工作台只显示**已授权的应用入口**（zhangsan 为客户端1、客户端2）；点击卡片 → 跳转认证中心 `/sso/auth` → 下发 ticket → 回到应用（`http://localhost:9001/sso/login?back=...&ticket=xxx`，客户端未启动时页面连接失败属预期）。
3. 未授权的应用（如 `sso-client3`）无法进入：直接访问认证中心授权地址会被拒绝（`您没有该应用的访问权限，请联系管理员开通`）。
4. 在「用户管理」中给 zhangsan 勾选客户端3 后，其工作台入口与登录授权**即时生效**（无需重新登录）。

### 登录安全演示（登录日志 / 锁定解锁 / 验证码 / 设备锁）

1. **密码国密加密**：登录页会用 `/api/auth/public-key` 的 SM2 公钥加密密码后提交（浏览器请求体里是密文，页面底部可看到本机设备指纹）。
2. **滑块验证码**：登录页默认需要验证码——点击「点击验证」按钮弹出滑块界面（拖动拼图与缺口对齐），验证通过后按钮变为绿色「验证成功，请点击登录」，
   再点「登 录」提交；**滑块通过不会自动提交**（便于先改密码再登录）。验证码为一次性，任何一次登录请求后（无论成功失败）都会重置为「点击验证」，
   再次点击会取一张新的验证码。
3. **连续错误锁定 + 在线解锁**：连续输错 5 次密码后账号被锁定 30 分钟（提示"可联系管理员在线解锁"）；
   管理员在「用户管理」中点击该用户的 **解锁** 即可立即恢复（列表会显示红色「已锁定」标签）。
4. **设备锁与设备指纹**：首次登录会自动把当前浏览器指纹绑定到账号；换一台设备/浏览器登录时会提示
   **"检测到新设备登录"**，需用账号绑定手机号接收验证码（演示环境验证码直接显示在页面上）完成验证，
   验证通过后**自动解绑原设备并绑定新设备**；管理员也可在「用户管理 - 设备」中查看设备指纹并解绑。
5. **登录日志**：管理员菜单「登录日志」可查看全部登录/注销记录（结果包含登录成功、密码错误、账号锁定、验证码错误、
   设备锁拦截、设备验证、设备换绑），支持账号、结果、时间范围筛选；日志按月分表存储（`uc_login_log_yyyyMM`）。
6. **操作日志（管理审计）**：管理员菜单「操作日志」记录平台管理动作——用户的新增/修改/删除/重置密码/启用禁用、
   单条与批量授权、在线解锁、解绑设备，以及应用的新增/修改/启用禁用/删除等；每条日志含操作人、模块、动作、
   请求方法（如 `PUT /api/user`）、**请求参数摘要**、**修改前 / 修改后数据**、结果与说明、耗时、IP 与设备信息。
   变更快照规则：**新增=传参+写入数据（含生成的主键）、更新=传参+修改前数据+修改后数据、删除=传参+删除前数据**
   （删除用户会同时带上其授权关系清理记录）；密码等敏感字段脱敏为 `***`。
   **操作失败也会记录**（例如新增重名用户 → 结果「失败」并附失败原因），支持操作人/模块/动作/结果/时间范围筛选，
   点「详情」可查看格式化的传参与变更前后数据（修改前红底 / 修改后绿底）。
7. **统一登录（SSO）全链路**：在浏览器地址栏直接访问下面的授权地址（模拟应用跳转）：

   ```
   http://localhost:8091/sso/auth?client=sso-client1&redirect=http://localhost:9001/sso/login&mode=ticket
   ```

   - 未登录时：认证中心 302 跳转到统一登录页，页面提示「应用 sso-client1 正在请求统一登录」；
   - 登录后：自动跳回认证中心 → 认证中心下发 ticket → 302 跳转到
     `http://localhost:9001/sso/login?ticket=xxx`（9001 端口未启动应用时页面会抛出连接失败，属预期现象）。
   - **禁用某个应用后，该应用再发起统一登录会被认证中心拒绝**（`未能获取应用信息`）。

### 规则配置演示（仅系统管理员）

1. 进入菜单「**规则配置**」：三个分组（密码规则 / 登录规则 / 日志规则）共 17 项配置，每项含名称、当前值、
   取值范围与说明；「保存配置」仅提交改动项，「恢复默认」一键还原（需确认）。
2. **密码复杂度**：开启「启用密码复杂度校验」并设置「系统默认密码」后保存（示例：`Bdsoft@2026`）。
   此时新增用户填弱密码（如 `123456`）、重置密码、用户自助改密都会被拒绝，并提示具体原因
   （长度不足 / 缺大写 / 缺数字 …）；**系统默认密码本身也必须满足复杂度要求**，否则保存会被拒绝。
3. **首次登录强制修改密码**：开启后，新增用户与被重置密码的用户**下次登录会先跳到「修改密码」页**
   （页面提示密码规则，改密前访问其他接口返回 `code=426`），改密成功后自动进入工作台。
   用户列表中这类账号会显示黄色「需改密」标签。管理员与普通用户都可通过右上角下拉「修改密码」自助改密。
4. **设备验证（设备锁）开关**：关闭后任意设备都能直接登录（不再发送手机验证码、不再绑定设备指纹）；
   重新开启后恢复「首次登录绑定 + 换设备短信验证」策略。
5. **滑块验证码开关**：关闭后登录页不再显示「点击验证」按钮，登录接口也不再要求验证码；
   「验证码触发阈值」支持 0=每次登录都需验证、N=连续密码错误 N 次后才需要。
6. **失败锁定策略**：连续密码错误阈值 / 计数窗口 / 锁定时长均可在页面调整，即时生效
   （管理员仍可在「用户管理」中在线解锁）。
7. **日志分区频率**：登录日志、操作日志可分别设置为**按月 / 按周 / 按天**分区。修改后后端立即按新频率
   **续建**分区（已有分区保持不变、区间连续，不产生空档），例如按天分区会生成
   `uc_login_log_20261011`、`uc_login_log_20261012` …；按周分区生成 `uc_login_log_202641`（ISO 周）。
   注意：分区创建以「区间不重叠」为前提，因此新频率从**现有分区的终点**开始生效（例如已有按月分区覆盖到 12-01 时，
   按天分区将从 12-01 起创建）。

## 五、统一登录（SSO）接入说明

### 5.1 认证中心协议地址

| 地址 | 说明 |
| --- | --- |
| `/sso/auth` | 单点登录授权地址（应用重定向到这里发起登录） |
| `/sso/doLogin` | 账号密码登录接口（POST：`name`、`pwd`），返回 token |
| `/sso/signout` | 单点注销地址 |
| `/sso/pushS` | 接收应用端推送的消息 |

### 5.2 应用接入步骤

1. 在「应用管理」中新增应用，填写：
   - **应用标识**（`client_id`）：如 `sso-client1`，需与应用侧 `sa-token.sso-client.client` 一致；
   - **回调地址**：应用允许的回调地址，`*` 表示放行全部，多个用英文逗号分隔；
   - **接口秘钥**：与 `sa-token.sso-client.secret-key` 一致（用于参数签名/推送校验）。
2. 应用侧引入 `pkuse-token-spring-boot3-starter` + `pkuse-token-sso`（参考本目录下 `client1`~`client3` 脚手架的 `pom.xml`、`application.yml`，以及 `pkuse-token-demo-sso` 下的完整 client 示例）：

   ```yaml
   sa-token:
     sso-client:
       client: sso-client1                     # 与应用管理中的「应用标识」一致
       server-url: http://localhost:8091       # 认证中心地址
   ```

   ```java
   // 应用侧登录入口：接收 ticket 并换回会话
   @RequestMapping("/sso/login")
   public Object login(String ticket) {
       Object loginId = StpUtil.loginByTicket(ticket);
       return PkuseResult.ok();
   }
   ```

3. 应用跳转授权地址即可完成统一登录：

   ```
   http://localhost:8091/sso/auth?client=sso-client1&redirect={应用回调地址}&mode=ticket
   ```

### 5.3 前端登录页与认证中心的跳转时序

```
应用(9001)               认证中心(8091)                统一登录页(8090)
   │  /sso/auth?client=..&redirect=..  │                       │
   ├──────────────────────────────────▶ │  未登录                │
   │                                    │  302 ─────────────────▶│ /login?redirect={原始授权地址}
   │                                    │                       │ 用户输入账号密码
   │                                    │  POST /api/auth/login ◀┤
   │                                    │  ◀── token + Cookie ───┤
   │                                    │  ◀── 整页跳回原始授权地址 ┤
   │  已登录：下发 ticket 并 302 跳回      │                       │
   │ ◀──────────────────────────────────┤                       │
   │  /sso/login?ticket=xxx 换回会话      │                       │
```

> **登录态如何跨系统传递**：登录接口除了返回 `token`，框架还会自动写入 `satoken` Cookie。
> 前端登录成功后将 token 存入 `localStorage`（请求头 `satoken` 用于 XHR）并同步写入 Cookie
> （浏览器整页跳转 `/sso/auth` 时自动携带）。开发环境前端 8090 与后端 8091 同为 `localhost`
> （Cookie 不区分端口），因此可行；生产环境推荐前后端同域部署（Nginx 反向代理）。

## 六、接口清单

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/auth/login` | 登录（返回 token + 用户信息） |
| POST | `/api/auth/logout` | 退出登录 |
| GET | `/api/auth/me` | 当前登录用户 |
| GET | `/api/user/page` | 用户分页（`current`、`size`、`username`、`nickname`、`status`；含「已授权应用数」） |
| POST/PUT | `/api/user` | 新增/修改用户 |
| PUT | `/api/user/{id}/status` | 启用/禁用用户 |
| PUT | `/api/user/{id}/password` | 重置密码 |
| DELETE | `/api/user/{id}` | 删除用户（同步清理其应用授权） |
| GET | `/api/user/{id}/clients` | 查询用户已授权的应用标识 |
| PUT | `/api/user/{id}/clients` | 保存用户的应用授权（覆盖式，请求体为应用标识数组） |
| PUT | `/api/user/clients/batch` | **批量授权**：为多个用户授予多个应用（`{userIds, clientIds, mode: append\|cover}`） |
| GET | `/api/client/page` | 应用分页（`current`、`size`、`clientId`、`clientName`、`status`；含「已授权用户数」） |
| GET | `/api/client/list` | 全部应用 |
| POST/PUT | `/api/client` | 新增/修改应用（保存后自动同步到 SSO；应用标识变更时迁移用户授权） |
| PUT | `/api/client/{id}/status` | 启用/禁用应用 |
| DELETE | `/api/client/{id}` | 删除应用（同步清理用户授权） |
| GET | `/api/dashboard/overview` | 工作台总览（**仅系统管理员**） |
| GET | `/api/dashboard/my-apps` | 我的应用入口（普通用户为已授权应用；管理员为全部启用应用） |
| GET | `/api/auth/public-key` | SM2 公钥（前端加密登录密码，国密） |
| GET | `/api/auth/captcha` | 获取滑块验证码（tianai-captcha） |
| GET | `/api/auth/need-captcha` | 查询账号当前是否需要滑块验证码 |
| POST | `/api/auth/device/send-code` | 新设备登录：发送手机验证码（参数 verifyToken） |
| POST | `/api/auth/device/verify` | 新设备登录：校验验证码并换绑登录（解绑原设备、绑定新设备） |
| PUT | `/api/user/{id}/unlock` | 管理员在线解锁（清除密码错误锁定与失败计数） |
| GET | `/api/user/{id}/devices` | 查询用户已绑定设备（设备指纹） |
| DELETE | `/api/user/{id}/devices` | 解绑设备 |
| GET | `/api/log/login/page` | 登录日志分页查询（账号/结果/时间范围，跨月分区，**仅系统管理员**） |
| GET | `/api/log/oper/page` | 操作日志分页查询（操作人/模块/动作/结果/时间范围，跨月分区，**仅系统管理员**） |
| GET | `/api/config/list` | 规则配置项列表（按分组：密码规则/登录规则/日志规则，含默认值与可选值，**仅系统管理员**） |
| PUT | `/api/config` | 批量保存规则配置（`{values:{key:value}}`，校验通过后即时生效，**仅系统管理员**） |
| POST | `/api/config/reset` | 恢复默认配置（**仅系统管理员**） |
| GET | `/api/config/password-rule` | 当前密码规则 + 系统默认密码（管理界面提示用，**仅系统管理员**） |
| GET | `/api/auth/password-rule` | 当前生效的密码规则（登录用户可读，修改密码页提示） |
| POST | `/api/auth/change-password` | 修改当前登录用户的密码（原密码+新密码，SM2 密文；成功后清除「需改密」标记） |
| GET/POST | `/sso/auth`、`/sso/doLogin`、`/sso/signout` | SSO 标准协议地址 |

所有 `/api/**` 管理接口除登录/登出外均需登录（请求头 `satoken: {token}`），未登录返回 `code=401`；
其中 `/api/user/**`、`/api/client/**`、`/api/log/**`、`/api/config/**`、`/api/dashboard/overview` 仅系统管理员（role=admin）可访问，
普通用户返回 `code=403`。状态码约定：`421`=新设备需验证、`423`=账号锁定、`426`=需先修改初始密码、`428`=需要滑块验证码。

## 七、设计说明与注意事项

1. **应用名单以数据库为准**：`SsoClientRegistry` 在应用启动完成后把 `uc_client` 表中启用的应用
   同步到 Pkuse-Token 的 SSO-Server 全局客户端配置，每次增删改后重新同步并即时生效；
   yml 中通过 `sa-token.sso-server.clients` 配置的应用作为补充（同名时数据库覆盖，禁用则移除）。
2. **角色权限模型**：角色取自 `uc_user.role`，由 `security/StpInterfaceImpl` 提供给 Pkuse-Token；
   拦截器对管理接口执行 `StpUtil.checkRole("admin")`（前端同步隐藏菜单与路由，双重校验）。
   角色变更即时生效（框架未缓存角色列表）。
3. **用户-应用授权绑定**：`uc_user_client` 表以「用户 ID + 应用标识」为唯一键；
   普通用户工作台只展示已授权应用入口，进入应用与统一登录（`/sso/auth`）两处都会校验授权
   （管理员不受限）；用户删除、应用删除会同步清理授权，应用标识变更会迁移授权。
4. **雪花 ID 与前端精度**：主键为 19 位雪花 ID，超出 JS `Number` 安全整数范围，
   实体/`UserVO` 的 `id` 通过 `@JsonSerialize(using = ToStringSerializer.class)` 序列化为字符串，
   避免前端编辑/删除时 ID 错位。
5. **权限签名校验**：演示环境 `sa-token.sso-server.is-check-sign=false`（跳过 SSO 参数签名校验），
   便于直接联调；**生产环境必须为 true**，并保证应用侧密钥与「应用管理」中一致。
6. **密码与国密**：密码存储为 `SM3(password_salt + ":" + 明文)`（bcprov，随机盐）；历史 MD5 数据在首次登录成功后自动升级；
   前端登录时用 `/api/auth/public-key` 的 SM2 公钥加密密码（C1C3C2），服务端私钥解密（`encrypted=true` 标识）。
   演示环境 SM2 密钥对为启动时随机生成（重启后旧密文失效），生产环境应从密钥管理/配置中心加载固定密钥对。
7. **日志月分表（频率可配）**：`uc_login_log`（登录日志）与 `uc_oper_log`（操作日志）均为 KingbaseES 声明式分区表
   （按 `create_time` 范围分区），分区频率可在「规则配置」中按表设置（按月 `_yyyyMM` / 按周 `_yyyyww` / 按天 `_yyyyMMdd`）。
   分区由 `LogPartitionMaintainer` 在启动时、每日 00:05、以及规则配置变更后自动维护（覆盖「当前周期 + 下一周期」），
   写入日志前还有一次内存兜底检查；频率变更时从数据库现有分区的最大终点**续建**，保证区间连续（不重叠、不留空档）。
8. **失败锁定与在线解锁**：连续失败计数存于 Redis（窗口 `fail-window-seconds`），达到 `max-fail-count` 写入账号锁定状态
   （`locked_until`/`lock_reason`，用户列表显示「已锁定」标签）；管理员在用户管理点「解锁」即清除锁定与计数。
9. **滑块验证码**：tianai-captcha（core 手动装配，避开基于 SpringBoot2 的 starter）；
   策略 `captcha-after-fail`：**0=每次登录都需要（本 demo 默认）**、N=连续失败 N 次后需要、-1=从不。
   交互为「点击验证 → 弹窗拖动滑块 → 验证成功后手动点登录」（不自动提交，失败后自动重置为待验证状态）；
   **验证码校验数据为一次性**，每次校验（无论成功与否）都会消费，因此前端每次打开弹窗都会重新获取一张。
   SSO 协议登录接口 `/sso/doLogin`（面向第三方应用登录页，由应用签名/票据保护）不受该策略限制。
10. **服务端需要 HTTP 模板**：引擎默认的 `PkuseHttpTemplate` 会抛「HTTP 请求处理器未实现」，
    凡涉及 SDK 主动发 HTTP 的场景（模式三校验 ticket、单点注销推送等）都需应用提供实现（本项目用 JDK HttpClient）。
11. **设备锁与设备指纹**：前端按浏览器环境（UA/屏幕/时区/Canvas 等）计算 SHA-256 设备指纹随登录上报；
    **首次登录自动绑定**；已绑定设备直接放行；**更换设备**时返回 `421` 并下发验证凭证，
   前端引导用户用**绑定手机号接收短信验证码**完成验证，验证通过后**自动解绑原设备、绑定新设备**（自助换绑），
    管理员也可在「用户管理 - 设备」中在线解绑。接口/SSO 调用未上报指纹时，服务端按 User-Agent 派生设备标识。
12. **Redis 会话共享**：token 存于 Redis（`PkuseTokenDaoForRedisson`），应用与认证中心使用同一 Redis
    可实现 SSO 模式一/二的会话同步。
13. **依赖提示**：`pkuse-token-redisson-spring-boot-starter` 的传递依赖中排除了 `netty-codec`，
   应用需自行补齐（本示例在 `pom.xml` 中引入了 `io.netty:netty-codec` 并通过 `netty-bom` 统一版本）。
14. **分层规范**：Controller（`@Resource` 注入）→ Service 接口 → ServiceImpl（`ServiceImpl<Mapper, Entity>`）
   → Mapper；实体服务接口继承 `IService<T>`，通用 CRUD 复用 MyBatis-Plus 能力，
   业务方法在接口中声明、在 `service/impl` 下实现。
15. **操作日志埋点与变更快照**：Spring AOP + MyBatis-Plus 拦截器两级配合 ——
   ① 在管理接口方法上标注 `@OperLog(module = "用户管理", action = "新增用户")`（只声明模块/动作两个属性），
   `OperLogAspect`（`@Around`）自动记录操作人（`StpUtil` 会话）、请求方法/参数、结果、耗时与 IP/UA；
   ② **修改前 / 修改后数据不需要任何声明**：方法执行期间，自定义 MyBatis-Plus 内置拦截器
   `OperChangeCaptureInterceptor` 把该方法内所有落库的 INSERT/UPDATE/DELETE 自动登记到请求级
   `OperRecordCollector`（借鉴 MP 官方 `DataChangeRecorderInnerInterceptor` 的「WHERE 改写为 SELECT +
   DefaultParameterHandler 绑定」技术；官方插件已标记 `@Deprecated` 且输出为 `旧值->新值` diff 字符串，
   故自行实现结构化快照）——**不限定实体类型，一个方法更新多张表 / 多个实体同样覆盖**：
   更新记录「传参 + 修改前数据 + 修改后数据」（修改前在 SQL 执行前按同一 WHERE 查询、修改后在该方法结束时重查）、
   新增记录「传参 + 写入数据（含生成的主键）」、删除记录「传参 + 删除前数据」（删除用户会同时带上其授权清理记录）。
   序列化用与 MVC 共用的 Jackson，键名包含 `password`/`salt`/`secret`/`token`/`captcha` 的字段值脱敏为 `***`
   （布尔开关保留原值）、超长截断（2000 字符）；**写日志失败不影响业务主流程**。
   实现细节：登录/SSO 等未标注 `@OperLog` 的链路零开销；授权与配置保存路径改用逐条写入
   （`saveBatch`/`updateBatchById` 的 BATCH 执行器同一 SQL 只 prepare 一次，会漏掉后续语句的变更捕获）。
   需要在 pom 中引入 `spring-boot-starter-aop`（拦截器属于 MyBatis-Plus 内置插件体系，`mybatis-plus-jsqlparser` 已在依赖中）。
16. **规则配置模块**：配置以键值对存于 `uc_config`（17 项，分「密码规则 / 登录规则 / 日志规则」三组），
   配置项定义（键、默认值、名称、分组、类型、取值范围、说明）集中在 `config/SystemConfigItems`：
   后端启动时**自动补齐缺失配置项并同步元数据**（后续版本新增配置项无需执行 SQL），随后加载为
   `SystemConfig` 内存快照（`volatile` 引用整体替换，读侧无锁）；保存配置时先做类型/范围/选项与
   **跨字段校验**（系统默认密码必须满足密码复杂度），落库后刷新快照并发布 `SystemConfigChangedEvent`，
   通知日志分区维护器等组件即时生效。
17. **「需修改初始密码」拦截**：开启「首次登录强制修改密码」后，新增用户与被重置密码的用户在
   `uc_user.must_change_password` 上置 1；登录成功时该标记写入 Redis（随登录态有效期），
   拦截器对除登录/改密/查询规则等必要接口外的 `/api/**` 返回 `code=426`；
   用户改密成功后清库标记与 Redis 标记。前端登录页据此自动跳转「修改密码」页（`/password?force=1`），
   并保留原始跳转目标（如 SSO 回跳地址）在改密完成后继续。

## 八、常见问题（FAQ）

| 现象 | 原因与处理 |
| --- | --- |
| 启动报 `ClassNotFoundException: io.netty.handler.codec.MessageToMessageDecoder` | 缺少 `netty-codec`（见「注意事项 13」），在 pom 中补齐即可 |
| 建表后 `COMMENT ON TABLE sys_user` 报「不是一个表或物化视图」 | Kingbase 中 `sys_user` 被解析到系统视图，业务表请避开 `sys_` 前缀 |
| 编辑/删除时提示「用户不存在」 | 雪花 ID 前端精度丢失，确认 `id` 已按字符串返回 |
| 登录页登录后仍跳回登录页 | Cookie 未写入（跨域/跨站被拦截），确认前端与后端同域或已按 5.3 说明处理 |
| 应用发起统一登录提示「未能获取应用信息」 | 该应用未在「应用管理」中登记或被禁用 |
| 应用发起统一登录提示「您没有该应用的访问权限」 | 当前登录用户未被授权该应用；在「用户管理 - 授权应用」中开通（管理员不受限） |
| 普通用户看不到「用户管理 / 应用管理」菜单 | 符合预期：管理功能仅系统管理员可用（后端返回 403） |
| 验证码生成报「随机获取资源错误，store中资源为空」 | 未给 ResourceStore 添加背景图；`CaptchaConfig` 已内置 jar 里的背景图，自建时需 `addResource(SLIDER, ...)` |
| 滑块滑动位置正确但校验仍失败 | tianai 的验证码校验数据为**一次性**（每次校验即消费）；前端每次失败后需重新获取验证码（登录页已自动处理） |
| 换设备登录提示「检测到新设备登录」 | 设备锁生效：用绑定手机号验证码完成验证即可自助换绑，或由管理员在「用户管理 - 设备」中解绑；也可在「规则配置」中关闭「启用设备验证」 |
| 登录提示「请先修改初始密码后再使用系统」（code=426） | 已开启「首次登录强制修改密码」且该账号仍在用初始/重置密码：先到「修改密码」页完成改密（右上角下拉也可进入） |
| 修改了「规则配置」但没生效 | 配置保存后即时生效（内存快照整体替换）；若改的是**分区频率**，请留意新频率从现有分区终点续建（不重叠），不会立即出现新分区 |
| 登录后看不到「规则配置」菜单 | 符合预期：规则配置仅系统管理员可用（后端 `/api/config/**` 返回 403） |
| 登录提示「密码解密失败」 | 服务重启后 SM2 密钥对会重新生成，旧页面上缓存的公钥已失效；刷新页面（重新取公钥）后重试 |
| Token 在服务重启后仍有效 | 符合预期：token 存于 Redis，重启不丢失会话 |
