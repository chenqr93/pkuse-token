# Pkuse-Token 授权中心示例（统一权限 / RBAC 管理中心）

基于 Pkuse-Token 的**授权中心** Demo：面向多应用的统一权限（RBAC）管理中心——**应用菜单管理、角色管理、用户授权、授权数据分发**，并作为用户中心的 SSO Client 完成登录（登录层与权限层解耦）。

> 功能设计见上级目录 [AUTH-CENTER-DESIGN.md](../AUTH-CENTER-DESIGN.md)（已评审定稿）。
> 应用接入方式见 [pkuse-token-auth-center-client-spring-boot-starter/README.md](../pkuse-token-auth-center-client-spring-boot-starter/README.md)（接入指南）。

```
pkuse-token-demo-auth-center-server/            # 本工程（聚合父 POM，分层五模块）
├── authcenter-common/                          #   通用层：注解 / 业务异常 / 审计父类 / 平台配置
├── authcenter-model/                           #   数据模型层：ac_* PO/BO/DTO/VO
├── authcenter-service/                         #   业务层：菜单/角色/绑定/操作日志 Service + 支撑组件
│                                               #     （应用注册表缓存 / 签名校验 / 用户中心对接双通道）
├── authcenter-api/                             #   对外接口层：控制台 / 开放分发 / SSO 双模式登录 Controller
└── authcenter-web/                             #   启动与装配（9010）：配置 / yml / db 脚本 / assembly
```

## 一、功能清单

| 模块 | 说明 |
| --- | --- |
| 概览 | 接入应用数/菜单数/角色数/已授权用户数统计（全本地），应用列表（注册表实时 + 缓存兜底）与「刷新应用」；**孤儿数据清理（V2）**：扫描/清理用户中心已删除应用/用户的本地残留 |
| 菜单管理 | 按应用隔离的目录/页面/按钮三层菜单树；类型字段约束（按钮必填权限码、目录不允许权限码）、应用内名称/权限码唯一、防环、删除级联清理角色绑定 |
| 角色管理 | 按应用隔离的角色 CRUD 与启禁；角色-菜单勾选（覆盖式、父子不联动）；删除级联清理两绑定表 |
| 用户授权 | 用户分页（实时取自用户中心）+ 本地角色摘要回填；分配角色弹窗按应用分组、入口授权边界提示；覆盖式保存 |
| 授权分发 | 开放接口 `/open/authz/menus`、`/open/authz/perms`、`/open/authz/roles`（MD5 签名 + 防重放；验签失败自动刷新注册表重试一次）；**只读本地库与缓存，用户中心宕机不影响分发**；roles 供各应用客户端 starter 按客户端获取本应用角色（应用以自身 client+secret 签名调用；用户信息通道不再携带角色，认证中心不调用本接口） |
| 授权变更推送（V2） | 菜单/角色/用户授权变更后，向「启用 + isPush=1」的应用推送缓存失效通知（`AuthzChangePusher` → 应用 starter 的 `/authz/evict` 签名端点），**秒级生效，无需等缓存 TTL**；推送失败仅告警（应用在 TTL 到期后自然刷新） |
| 操作审计 | `@OperLog` 切面自动写本地 `ac_oper_log`：操作人/模块/动作/请求参数摘要/**修改前·修改后数据快照（V2，MyBatis-Plus 变更捕获拦截器自动登记，不限实体类型）**/结果/耗时/IP/UA；**按月分区（V2，`ac_oper_log_yyyyMM` 自动创建）**；控制台可查（详情含红/绿底快照对比） |
| SSO 登录 | 作为用户中心 SSO Client，**双模式可配**：`ticket`（默认，模式二/三式，任意拓扑通用）/ `simple`（模式一式直通）；启动时校验模式与 token-name 组合 |

**设计要点**：应用档案与用户档案**均不落地**——应用清单/密钥以用户中心 `/open/registry/clients` 为唯一事实源（内存 + Redis 快照两级缓存，四时机刷新），用户数据实时经接口查询（simple=透传 / ticket=签名）；库中仅存授权关系（ac_menu / ac_role / ac_role_menu / ac_user_role / ac_oper_log）。

## 二、技术栈

SpringBoot 3.5.16 / JDK 17 / MyBatis-Plus 3.5.14 / KingbaseES（**独立库 PKUSE_TOKEN_AUTH_CENTER**）/ Pkuse-Token 1.46.0（SSO-Client，默认 ticket 模式 + 自有会话 Redis db8）/ Log4j2 + p6spy + JavaMelody（部门标准）

前端（`../pkuse-token-demo-auth-center-vue3`，端口 5180）：Vue 3.4 + Vite 4 + Element Plus 2.8 + Axios。

## 三、快速开始

### 1. 前置

- 用户中心后端（9000）已启动、前端（5173）可登录 admin（授权中心登录/数据源都依赖它）；
- 已构建客户端 starter：`cd ../pkuse-token-auth-center-client-spring-boot-starter && mvn -DskipTests install`（client3 接入需要）。

### 2. 建库与初始化

```bash
# 建独立库（与用户中心库同实例不同库）
ksql -h 192.168.1.77 -p 54321 -U DEV_USER_CENTER -d postgres \
     -c 'CREATE DATABASE "PKUSE_TOKEN_AUTH_CENTER" WITH OWNER = "DEV_USER_CENTER" ENCODING = '"'"'UTF8'"'"';'

# 建表 + 演示种子（sso-client3 的菜单/角色/用户绑定）
ksql -h 192.168.1.77 -p 54321 -U DEV_USER_CENTER -d PKUSE_TOKEN_AUTH_CENTER \
     -f authcenter-web/src/main/resources/db/init.sql

# 用户中心库登记 auth-center 应用（旧库执行；新库 init.sql 已内置）
ksql -h 192.168.1.77 -p 54321 -U DEV_USER_CENTER -d PKUSE_TOKEN_USER_CENTER \
     -f ../pkuse-token-demo-user-center-server/usercenter-web/src/main/resources/db/upgrade-v10-auth-center.sql
```

### 3. 启动后端（9010）

```bash
mvn -DskipTests package
java -jar authcenter-web/target/authcenter-web-0.0.1-SNAPSHOT-exec.jar
# 或开发热启动：mvn -DskipTests install && mvn -f authcenter-web/pom.xml spring-boot:run
```

启动日志应看到：`SSO 对接模式：ticket（token-name=authcenter-token）`、`应用注册表已刷新：6 个应用`。

### 4. 启动前端（5180）

```bash
cd ../pkuse-token-demo-auth-center-vue3 && npm install && npm run dev
```

### 5. 演示流程

1. **登录**：打开 <http://localhost:5180> → 自动跳用户中心统一登录 → `admin / 123456` → 回到授权中心概览；
2. **概览**：查看统计与 5+1 个应用（含 auth-center 自身），点「刷新应用」验证注册表同步；
3. **菜单管理**：选「客户端3」查看种子菜单（系统管理→用户管理→按钮；演示中心→关于/隐藏详情）；
4. **角色管理**：客户端3 的 `admin`（全菜单）与 `user`（无删除按钮），点「菜单权限」调整勾选；
5. **用户授权**：给 zhangsan 分配/调整角色（注意「未开通入口」提示）；
6. **分发演示（client3）**：启动 client3（9003/5176）→ `zhangsan / 123456` 登录 → 首页「授权中心 · 动态菜单与按钮权限」：
   - zhangsan（user 角色）看不到「用户删除」按钮、点其它受保护接口正常；
   - 换 admin 登录 client3 → 全部按钮可用（客户端侧叠加平台 admin 全量逻辑）；
   - **停掉用户中心** → client3 仍能拉取权限（分发独立性）；
7. **操作日志**：以上管理动作全部留痕，支持筛选与参数详情查看。

## 四、SSO 模式切换（ticket ↔ simple）

默认 `ticket`（任意拓扑通用：跨域 + 独立 Redis 亦可部署）。若与用户中心**同域 + 同 Redis**，可切 `simple`（免 ticket 直通），三处联动修改（见 `application-dev.yml` 注释）：

1. `auth-center.sso.integration-mode: simple`；
2. `sa-token.token-name: satoken`（共享 Cookie 前提；ticket 模式必须独立命名防 Cookie 覆盖——启动时校验，配错快速失败）；
3. 打开 `sa-token.alone-redisson`（指向用户中心会话 Redis db5），同时用户查询自动切为「管理员会话透传」通道。

## 五、接口清单（摘要）

| 类别 | 路径 | 说明 |
| --- | --- | --- |
| SSO | `/sso/isLogin`、`/sso/login`、`/sso/loginByTicket`、`/sso/logout`、`/sso/*` | 双模式登录/注销/推送 |
| 控制台 | `/api/authz/app/list`、`/api/authz/app/refresh` | 应用列表（注册表实时+统计回填）、手动刷新 |
| 控制台 | `/api/authz/menu/**`、`/api/authz/role/**`、`/api/authz/user/**` | 菜单/角色/用户授权管理（登录 + admin） |
| 控制台 | `/api/authz/overview`、`/api/authz/me`（纯身份不含角色）、`/api/authz/my-roles`（本地 RBAC 控制台角色）、`/api/authz/log/oper/page` | 概览 / 当前用户 / 角色 / 操作日志 |
| 控制台 | `/api/authz/cleanup/scan`、`/api/authz/cleanup/execute` | 孤儿数据扫描/清理（V2） |
| 开放 | `GET /open/authz/menus?client=&loginId=&timestamp=&nonce=&sign=` | 用户可见菜单树（签名，全本地） |
| 开放 | `GET /open/authz/perms?client=&loginId=&timestamp=&nonce=&sign=` | 用户权限码集合（签名，全本地） |
| 开放 | `GET /open/authz/roles?client=&loginId=&timestamp=&nonce=&sign=` | 用户角色编码集合（绑定且启用的角色，按编码排序；无绑定空数组）。**调用方**：各应用后端（授权中心客户端 starter，以自身 client+secret 签名，client 即角色归属应用）；用户信息与角色拆分后，角色一律按客户端经本接口获取 |

## 六、V2 增量（相对首版）

| 特性 | 说明 | 验证要点 |
| --- | --- | --- |
| 授权变更推送 | 变更后推送 `authzChanged` 失效通知到开启推送的应用（注册表含 isPush/pushUrl，失效端点由 starter 自动暴露） | 菜单/角色/用户授权变更后，应用日志出现「权限缓存已全部失效」 |
| 操作日志快照 | 修改前/修改后数据自动捕获（新增=写入数据含主键、更新=前后对比、删除=删除前数据） | 操作日志详情页红/绿底快照 |
| 操作日志分区 | `ac_oper_log` 按 create_time 按月分区（启动+每日 00:05 自动创建当前/下一月） | 启动日志「操作日志分区已创建」；旧库执行 `db/upgrade-v2-oper-log.sql` |
| 孤儿数据清理 | 概览页「扫描/清理」：用户中心已删除应用/用户的本地残留 | 扫描列出孤儿 → 清理 → 复扫为空 |
| client5 magic 接口级 RBAC | `s_api_perm` 表把 magic 接口绑定到权限码，拦截器按登录用户校验（starter 提供权限数据）；绑定管理 `GET/POST/DELETE /magic/perms` | 见 client5 的 `db/init-client5.sql` 与 `PkuseTokenRequestInterceptor` |

## 七、常见问题

| 现象 | 处理 |
| --- | --- |
| 启动报 token-name 与模式不匹配 | 按第四节检查三处联动配置（快速失败属预期保护） |
| 应用列表为空 / 新增菜单提示应用未启用 | 用户中心未启动或注册表未拉到；恢复后点「刷新应用」；首次部署必须成功拉取一次 |
| 登录后提示没有控制台管理员角色 | 授权中心控制台角色走**本地 RBAC**（client=auth-center，种子见 `db/upgrade-v3-console-role.sql`）：需在该 client 下绑定 admin 角色；平台角色 uc_user.role 不再用于控制台门禁 |
| 用户授权页提示「用户中心服务不可用」 | 降级预期：仅影响用户查询，菜单/角色/分发不受影响 |
| client3 拉权限 403 | 密钥不一致：确认 yml 与用户中心登记一致，并在概览页「刷新应用」 |
