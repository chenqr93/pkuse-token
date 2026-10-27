# 用户中心后端（pkuse-token-demo-user-center 分层多模块）

Pkuse-Token 用户中心示例的后端服务：**用户管理 + 应用（Client）管理 + 统一登录认证（SSO-Server）+ 登录/操作日志 + 规则配置**。

- 技术栈：SpringBoot 3.5.16 / JDK 17 / MyBatis-Plus 3.5.14（含内置插件：分页、数据变更捕获）/ KingbaseES / Druid 1.2.27 连接池 / Pkuse-Token 1.46.0 / Redisson / Spring AOP / Log4j2 2.24.3 + p6spy 3.9.1 + JavaMelody 2.6.0（日志与 SQL/运行监控，部门标准强制）
- 主键策略：MyBatis-Plus `ASSIGN_UUID`（32 位 UUID 字符串，String 主键）；**从旧版本（雪花 ID）升级需重建数据库**（列类型 BIGINT → VARCHAR(32)，数据不保留），新库直接执行 init.sql
- 端口：`9000`；数据库：`PKUSE_TOKEN_USER_CENTER`（表 `uc_user`、`uc_client`、`uc_user_client`、`uc_user_device`、`uc_login_log`、`uc_oper_log`、`uc_config`）
- 前端工程：`../pkuse-token-demo-user-center-server-vue3`
- 完整启动步骤与演示说明：见 `../README.md`

## 模块划分与目录结构

```
pkuse-token-demo-user-center-server/            # 聚合父 POM（${revision} + flatten 统一版本管理）
├── usercenter-common/                          # 通用层（零内部依赖叶子）
│   └── com.bdsoft.usercenter.common.{annotation,config,exception,pojo,util}  # pojo/：PageQueryDTO（分页基类）+ AbstractAuditPO（统一审计父类）
├── usercenter-model/                           # 数据模型层（域优先：com.bdsoft.usercenter.{域}.{po,bo,dto,vo}）
│   └── 以 auth 域为例：auth.po→UcUser（PO 对表）；auth.dto→LoginDTO 等 4 个认证参数；
│       auth.vo→LoginVO / PasswordRuleVO / DeviceCodeVO / DeviceNeedVerifyVO；auth.bo→3 个业务对象
│       其余域同理：user / client / authz / device / log / config / dashboard
│       （通用分页基类 PageQueryDTO 在 usercenter-common 模块：common.pojo.PageQueryDTO）
├── usercenter-service/                         # 业务层（域优先）
│   └── com.bdsoft.usercenter.{域}.
│       ├── service/            #   Service 接口
│       ├── service.impl/       #   Service 实现
│       ├── mapper/             #   Mapper
│       └── support/            #   域内支撑组件（log 域：OperLogAspect / 变更捕获拦截器+收集器 /
│                               #     分区维护器；auth 域：验证码缓存组件）；security/ 框架集成不变
├── usercenter-api/                             # 对外接口层（域优先：com.bdsoft.usercenter.{域}.controller）
│   └── auth/auth.controller → AuthController + SsoServerController（/sso/*）等
└── usercenter-web/                             # 启动与装配：启动类 / config 装配 ×5 /
                                               #   handler 全局异常 / application*.yml / db 脚本
```依赖方向：`web → api → service → model → common`，单向无环；Java 包名不跨模块拆分（无 split package）。

> **统一审计字段**：`common.pojo.AbstractAuditPO`（七字段：deleted / createBy·updateBy·deleteBy / createTime·updateTime·deleteTime），
> 五张业务表 PO（user / client / authz / device / config 域）继承；操作人由 MetaObjectHandler 自动填充
> （INSERT→创建人/时间，UPDATE→更新人/时间，取当前登录用户 ID，未登录场景留空）；`deleted` 启用逻辑删除
> （查询自动过滤已删除数据、删除转更新），五个唯一索引均为部分索引（WHERE deleted=0）以支持删除后重建同名数据；
> 追加型日志表（uc_login_log / uc_oper_log）不继承，仅保留自有 create_time（登录日志为分区键）。

> **业务域划分（域优先包结构）**：`com.bdsoft.usercenter.{域}.{层}`——七域：`auth` 统一登录认证（含验证码/在线用户/SSO 协议入口）、`user` 用户管理、`client` 应用管理、
> `authz` 用户-应用授权、`device` 设备管理、`log` 登录+操作日志（含切面/变更捕获/分区维护，见 log.support）、`config` 规则配置、`dashboard` 工作台聚合。
> 层后缀：`controller`（api 模块）、`service` + `service.impl`、`mapper`、`po`、`dto`、`vo`、`bo`（除公共分页基类 common.PageQueryDTO 在 common 模块外，均在 model 模块）。

## 分层与模型规范

> 调用链：Controller(api) → Service 接口(service) → ServiceImpl(service) → Mapper(service)；
> 通用 CRUD 复用 MyBatis-Plus 的 `IService` / `ServiceImpl`，业务方法在接口声明、实现类编写。

> 模型规范：**Entity 对表、DTO 管传输、VO 管展示、BO 管业务**。
> Entity 仅在 Mapper / Service 层内部使用，不直接进出接口层；
> 请求参数统一用 DTO、响应统一用 VO（Controller 转换）；BO 仅 Service 层内部使用。

## 自定义 SQL 的 XML 约定（多数据库兼容）

- XML 与 Mapper 接口**同模块**存放：`usercenter-service/src/main/resources/mapper/`；
- 扫描路径：`mybatis-plus.mapper-locations: classpath*:mapper/**/*.xml`（同时覆盖依赖 jar 内 XML）；
- 多数据库（KingbaseES / 达梦 DM / MySQL）用 MyBatis `databaseId` 机制：按方言建子目录
  （`mapper/kingbase/`、`mapper/dm/`、`mapper/mysql/`）或同一文件内用 `databaseId` 属性区分，
  不带 `databaseId` 的语句作为默认实现；当前库的方言由 `MybatisPlusConfig` 中的
  `VendorDatabaseIdProvider` 按 JDBC 产品名自动解析，无需手工配置。

## 构建与运行

```bash
cd pkuse-token-demo-user-center-server
mvn -DskipTests package                 # 一次构建 5 个模块；-P dev|test|uat|prod 选择打包环境（默认 dev）
```

**方式一：assembly 部署包（推荐；应用瘦包在根目录，bin/conf/lib 分离，Linux 解压即部署）**

```
usercenter-web/target/usercenter-web-0.0.1-SNAPSHOT-assembly/
└── pkuse-token-demo-user-center-server-0.0.1-SNAPSHOT/
    ├── usercenter-web-*.jar   # 本应用瘦包（位于根目录）
    ├── bin/                   # start.bat / start.sh / stop.bat / stop.sh
    ├── conf/                  # application.yml + application-{env}.yml（外置配置）
    ├── lib/                   # 全部依赖 jar
    └── logs/                  # 运行时生成：GC 日志、OOM 堆转储
```

conf 中携带的环境配置由打包时的 Maven profile 决定（默认 dev），

**Linux 部署（上传 tar.gz 解压后运行 start.sh 即可）**：

```bash
tar -xzf usercenter-web-0.0.1-SNAPSHOT-assembly.tar.gz
cd pkuse-token-demo-user-center-server-0.0.1-SNAPSHOT
./bin/start.sh                                  # 前台启动
nohup ./bin/start.sh > logs/stdout.log 2>&1 &   # 后台启动
./bin/stop.sh                                   # 停止
./bin/start.sh --server.port=9001               # 可追加任意启动参数
```

Windows 下运行 `bin\start.bat` / `bin\stop.bat`，同样支持追加参数。

启动脚本内置 JVM 参数（可在 bin/start.sh 中调整）：`-Xms512m -Xmx1024m -XX:MaxMetaspaceSize=256m`、
OOM 自动转储堆快照到 `logs/`、GC 日志（JDK17 统一日志 `-Xlog:gc*`，滚动 5 个文件、单个 20M）→ `logs/gc-*.log`。

修改 conf 下的 application*.yml 后重启即生效，无需重新打包；
运行时可用 `--spring.profiles.active=xxx` 切换环境（conf 中需存在对应 application-xxx.yml）。

同目录另有 `usercenter-web-0.0.1-SNAPSHOT-assembly.zip` / `.tar.gz`，内容与目录形态一致，用于上传部署。

**方式二：单文件 fat 包**（按打包时的 profile 激活环境）

```bash
java -jar usercenter-web/target/usercenter-web-0.0.1-SNAPSHOT-exec.jar
```

**方式三：开发热启动**（需先把各模块安装到本地仓库）

```bash
mvn -DskipTests install && mvn -f usercenter-web/pom.xml spring-boot:run
```

## 权限模型

| 角色 | 可访问范围 |
| --- | --- |
| `admin`（系统管理员） | 全部接口与应用（工作台总览、用户管理、应用管理、登录日志、操作日志、规则配置、进入全部应用） |
| `user`（普通用户） | 工作台（`/api/dashboard/my-apps`）、修改密码（`/api/auth/change-password`）；仅能进入已授权应用（`uc_user_client` 绑定） |

- 管理接口（`/api/user/**`、`/api/client/**`、`/api/log/**`、`/api/config/**`、`/api/dashboard/overview`）由拦截器执行
  `StpUtil.checkRole("admin")`，普通用户返回 `code=403`；前端同步隐藏菜单并拦截路由。
- 开启「首次登录强制修改密码」后，「需改密」账号访问 `/api/**`（登录、改密、查询密码规则等除外）返回 `code=426`。
- 统一登录入口 `/sso/auth` 校验：管理员放行全部应用，普通用户需已授权，否则拒绝下放 ticket。

## 登录安全机制（国密 / 锁定 / 验证码 / 设备锁）

| 能力 | 实现要点 |
| --- | --- |
| 密码国密加密 | 前端用 `SM2` 公钥（`/api/auth/public-key`）加密密码，服务端私钥解密（C1C3C2）；存储为 `SM3(salt:明文)`（随机盐，历史 MD5 无感升级）；新增/重置/改密按「规则配置 - 密码规则」校验复杂度 |
| 登录日志 + 分表 | `uc_login_log` 按 `create_time` **分区**（频率可配：按月 `_yyyyMM` / 按周 `_yyyyww` / 按天 `_yyyyMMdd`），启动、每日定时与配置变更时自动维护；写入父表由数据库路由 |
| 密码错误锁定 | Redis 计数（窗口 `login.fail-window-seconds`）→ 达 `login.max-fail-count` 写 `locked_until`/`lock_reason`；管理员「用户管理 - 解锁」在线解锁 |
| 滑块验证码 | tianai-captcha（core 手动装配）；策略取自「规则配置」：`login.captcha-enabled` 总开关 + `login.captcha-fail-threshold`（0=每次登录/ N=连续失败 N 次后）；**校验数据一次性**（失败需换一张） |
| 设备锁 + 设备指纹 | 前端指纹（UA/屏幕/时区/Canvas + 浏览器内随机盐 → SHA-256）随登录上报；「规则配置 - 启用设备验证」总开关；首次登录绑定；换设备返回 `421` → 手机验证码验证 → **自动解绑原设备并换绑新设备**；管理员可在线解绑 |
| 多端在线 + 同类型互踢 | 设备类型（`pc` / `mobile` / `mini`，登录上报或按 UA 推断）随会话写入终端信息；「规则配置 - 踢掉同类型设备在线账号」开启后，同类型新登录顶掉该类型旧会话（`CURR_DEVICE_TYPE`），其它类型不受影响；关闭则各类型均可多端并存 |
| 首次登录改密 | 「规则配置 - 首次登录强制修改密码」开启后，新增/被重置密码的用户下次登录先进入改密页（`code=426` 拦截其他接口）；改密清标记 |

> 演示模式说明：`user-center.login.device-verify-show-code=true` 时新设备验证码随响应返回（无短信通道时便于演示），
> **生产环境必须关闭**；`captcha.expire.default` 控制验证码有效期（毫秒）。

## 操作日志（管理动作审计）

| 能力 | 实现要点 |
| --- | --- |
| 埋点方式 | 管理接口方法标注 `@OperLog(module = "用户管理", action = "新增用户")`（仅声明模块/动作），`OperLogAspect`（`@Around`）自动记录成功与失败，**写日志异常不影响业务返回** |
| 记录内容 | 操作人（`StpUtil` 会话 + `uc_user` 反查账号）、模块、动作、请求方法（如 `POST /api/user`）、请求参数摘要、**修改前/修改后数据**、结果与说明、耗时、IP、UA、OS、浏览器 |
| 变更快照 | **由 MyBatis-Plus 拦截器自动捕获，无需声明实体**：`OperChangeCaptureInterceptor`（InnerInterceptor）在 `@OperLog` 方法执行期间把所有 INSERT/UPDATE/DELETE 登记到请求级 `OperRecordCollector`——更新=修改前数据（SQL 执行前按同一 WHERE 查询）+修改后数据（方法结束后重查）、新增=写入数据（含生成主键）、删除=删除前数据；**一个方法更新多张表/多个实体全部覆盖**（删除用户同时记录其授权清理）。借鉴 MP 官方 `DataChangeRecorderInnerInterceptor` 技术（该插件已 `@Deprecated` 且仅输出 diff 字符串），自行实现结构化快照 |
| 参数脱敏 | 与 MVC 共用 Jackson 序列化（含 `LocalDateTime`）；键名含 `password`/`salt`/`secret`/`token`/`captcha` 的字段值替换为 `***`（布尔开关保留原值），整体截断至 2000 字符 |
| 覆盖范围 | 用户：新增/修改/重置密码/启用禁用/删除、授权应用、批量授权、在线解锁、解绑设备；应用：新增/修改/启用禁用/删除；规则配置：修改/恢复默认；账号安全：修改密码 |
| 分表存储 | `uc_oper_log` 按 `create_time` **按月分区**（`uc_oper_log_yyyyMM`），与登录日志共用 `LogPartitionMaintainer` 自动建表；`before_data`/`after_data` 列已传播到已有月分区（见 `upgrade-v8-oper-log-snapshot.sql`） |
| 查询接口 | `GET /api/log/oper/page`（操作人/模块/动作/结果/时间范围，跨月分区；仅系统管理员） |

> 该能力依赖 `spring-boot-starter-aop`（本工程 pom 已引入）；拦截器属于 MyBatis-Plus 内置插件体系
> （`mybatis-plus-jsqlparser` 已在依赖中）。授权保存/配置保存等路径使用逐条写入而非
> `saveBatch`/`updateBatchById`（BATCH 执行器同一 SQL 只 prepare 一次，会漏掉后续语句的变更捕获）。

## 规则配置（平台规则在线维护，仅系统管理员）

| 能力 | 实现要点 |
| --- | --- |
| 配置存储 | 键值对表 `uc_config`（键 / 值 / 名称 / 分组 / 类型 / 默认值 / 说明）；默认值由 `SystemConfigItems` 定义，**启动时自动补齐缺失项并同步元数据**（新增配置项无需 SQL） |
| 内存快照 | `SystemConfig` 为不可变快照，`UcConfigService#get()` 通过 `volatile` 引用整体替换，读侧无锁；保存后刷新并发布 `SystemConfigChangedEvent` |
| 配置分组 | **密码规则**（复杂度开关、最小长度、大小写/数字/特殊字符、系统默认密码）、**登录规则**（首次登录强制改密、设备验证开关、验证码开关与阈值、锁定阈值/窗口/时长）、**日志规则**（登录/操作日志分区频率） |
| 校验 | 类型/取值范围/选项校验 + **跨字段校验**（系统默认密码必须满足密码复杂度）；未知配置项、非法取值直接拒绝并提示原因 |
| 生效点 | 登录（验证码/设备验证/锁定）、用户新增与重置密码（默认密码/复杂度/需改密标记）、自助改密、日志分区维护 |
| 查询接口 | `GET /api/config/list`、`PUT /api/config`、`POST /api/config/reset`（均仅系统管理员，写操作计入操作日志） |

## 注意：必须提供 PkuseHttpTemplate（HTTP 请求处理器）

认证中心向应用推送消息（单点注销 `logoutCall` 等）需要 SDK 主动发起 HTTP 请求，
框架默认实现会抛 `NotImplException: HTTP 请求处理器未实现`，因此本项目提供了
[`config/HttpTemplateConfig`](src/main/java/com/bdsoft/usercenter/config/HttpTemplateConfig.java)（JDK HttpClient 实现）。
若缺少该实现，会出现「本端已注销，但跨 Redis 的应用（模式三 / NoSdk）没有下线」的现象。

## 配置项（application.yml）

| 配置 | 说明 |
| --- | --- |
| `spring.datasource.*` | Kingbase 数据源（`jdbc:p6spy:kingbase8://host:port/PKUSE_TOKEN_USER_CENTER`；p6spy 代理驱动做 SQL 监控，真实驱动与日志格式见 `spy.properties`，SQL 输出到 Log4j2 的 `p6spy` logger） |
| `spring.data.redis.*` | Redis 会话存储（应用与认证中心共享） |
| `sa-token.sso-server.*` | SSO-Server 配置（ticket 有效期、home-route、签名校验开关等） |
| `javamelody.*` | 运行监控开关（`/monitoring` 监控页；部门标准强制） |
| `user-center.frontend-base-url` | 统一登录页地址（未登录访问 `/sso/auth` 时跳转） |
| `user-center.login.*` | 部署级登录参数（绑定设备数、内置管理员豁免设备锁、新设备验证码有效期/是否回显） |
| 运行期规则 | 失败计数窗口、锁定阈值、验证码策略、设备验证开关、系统默认密码、密码复杂度、日志分区频率 → 存于 `uc_config`，在「规则配置」页面维护，**即时生效** |

## 启动

```bash
# 初始化数据库（首次）
ksql -h 192.168.1.77 -p 54321 -U DEV_USER_CENTER -d PKUSE_TOKEN_USER_CENTER \
     -f src/main/resources/db/init.sql

# 已按旧版本部署过的库：按顺序执行增量脚本（授权表/入口地址、客户端列、登录安全列、国密列、操作日志表、规则配置表）
cd src/main/resources/db
for f in upgrade-v2-authorization.sql upgrade-v3-clients.sql upgrade-v4-login-security.sql upgrade-v5-sm-crypto.sql upgrade-v6-oper-log.sql upgrade-v7-config.sql; do
  ksql -h 192.168.1.77 -p 54321 -U DEV_USER_CENTER -d PKUSE_TOKEN_USER_CENTER -f $f
done

# 启动
mvn spring-boot:run
# 或打包运行
mvn -DskipTests package && java -jar target/pkuse-token-demo-user-center-server-0.0.1-SNAPSHOT.jar
```
