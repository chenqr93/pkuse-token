# Pkuse-Token

**面向 Spring Boot 3 的轻量级 Java 权限认证框架，单点登录（SSO）是一等公民。**

Pkuse-Token 由 [Sa-Token v1.46.0](https://sa-token.com) 改造而来（构件坐标 `com.bdsoft`、包名 `com.bdsoft.pkusetoken`），在此基础上完成了平台化收敛：

- **支持 Spring Boot 3 与 4**（基线分别为 3.5.16/Spring 6.2.19、4.1.1/Spring 7.0.9），Java 17 基线，编译开启 `-parameters`；
- **单线存储选型 Redisson**，SSO 全家桶演示与存储层完全对齐；
- **模块高度精简**：reactor 24 个模块、10 个插件，SB2 历史包袱与无用模块已清除。

---

## 一、总体架构

框架分为四层，自上而下逐层依赖，每层职责单一：

```
┌─────────────────────────────────────────────────────────────────┐
│  应用层（你的 Spring Boot 3 项目）                                  │
│      引入 pkuse-token-spring-boot3-starter + 按需引入插件           │
└────────────────────────────┬────────────────────────────────────┘
                             │
┌────────────────────────────▼────────────────────────────────────┐
│  ① Starter 层 —— spring-boot3-starter / spring-boot4-starter   │
│      只做一件事：把下层组装成一个开箱即用的 starter                    │
└────────────────────────────┬────────────────────────────────────┘
                             │
┌────────────────────────────▼────────────────────────────────────┐
│  ② Web 运行时层 —— spring-boot-webmvc-v3v4-common                │
│      过滤器 / 拦截器 / 请求上下文（让框架跑在 HTTP 上）                │
└──────────────┬─────────────────────────────────┬────────────────┘
               │                                 │
┌──────────────▼──────────────┐   ┌──────────────▼────────────────┐
│  ③ Servlet 模型层            │   │  ④ Bean 装配层                │
│  jakarta-servlet            │   │  webmvc-reactor-v3v4-common │
│  request/response/storage   │   │  yml 配置 → Bean → 静态门面     │
└──────────────┬──────────────┘   └──────────────┬────────────────┘
               │                                 │
┌──────────────▼─────────────────────────────────▼────────────────┐
│  ⑤ 核心引擎层 —— core（零第三方依赖）                               │
│      PkuseManager / StpLogic / PkuseSession / PkuseTokenDao / 路由匹配      │
└────────────────────────────┬────────────────────────────────────┘
                             │ 可选插件（按需引入，条件装配）
┌────────────────────────────▼────────────────────────────────────┐
│  插件层：sso(+sign) ｜ oauth2(+jwt) ｜ redisson 存储线 ｜ spring-aop │
└─────────────────────────────────────────────────────────────────┘
```

## 二、模块清单与依赖关系

### 2.1 版本管理层（4 个模块）

| 模块 | 作用 |
|---|---|
| `pkuse-token-bom` | **自家构件版本清单**：管理全部 com.bdsoft 构件的版本（`${revision}` CI 友好版本） |
| `pkuse-token-dependencies` | **三方版本目录**：jackson2 / redisson / hutool-jwt / jakarta-servlet-api 等 |
| `pkuse-token-special-dependencies` | 聚合器，当前只含 ↓ |
| `pkuse-token-spring-boot3-dependencies` | **Spring 系版本清单**：spring-boot 3.5.x / spring 6.2.x 的 starter 与核心库 |

版本传递链：`根 pom → import dependencies（继承 bom）→ 各层 import spring-boot3-dependencies`。三层各管一摊，升级互不干扰。

### 2.2 核心与 Starter 链（6 个模块）

```
pkuse-token-spring-boot3-starter        ← 应用唯一需要引入的依赖
 ├── pkuse-token-spring-boot-webmvc-v3v4-common     （Web 运行时桥）
 │    ├── pkuse-token-jakarta-servlet               （Servlet 上下文模型）
 │    └── pkuse-token-spring-boot-webmvc-reactor-v3v4-common  （Bean 装配层）
 │         └── pkuse-token-core                     （核心引擎，零依赖）
 └── pkuse-token-jackson                            （默认 JSON 模板）
```

| 模块 | 职责 |
|---|---|
| `pkuse-token-core` | **框架心脏**：登录认证、会话、权限、路由匹配、全局门面 `PkuseManager`。零第三方依赖，内置默认 JSON / HTTP 实现 |
| `pkuse-token-jakarta-servlet` | 请求三件套 `PkuseRequestForServlet` / `PkuseResponseForServlet` / `PkuseStorageForServlet` 与上下文工具（jakarta 命名空间） |
| `...webmvc-reactor-v3v4-common` | **Bean 装配层**：`@ConfigurationProperties` 绑定 `sa-token.*` 配置并注入静态门面；对 sso / oauth2 / sign 插件做条件装配（`@ConditionalOnClass` + optional 依赖） |
| `...webmvc-v3v4-common` | **Web 运行时桥**：注册上下文过滤器、防火墙、CORS、`PkuseInterceptor`（注解鉴权 + 路由拦截），SB3 自动配置入口 `PkuseTokenContextRegister` |
| `pkuse-token-spring-boot3-starter` | 入口打包：带入 Web 链 + 默认 JSON 引擎，零代码接入 |

> 模块名中的 `v3v4` 表示同时服务 Spring Boot 3 与 4 两个大版本（SB2 已退出，原名中的 v2 已于改名时去除）。

### 2.3 插件层（9 个模块）

| 插件 | 依赖 | 说明 |
|---|---|---|
| `pkuse-token-sso` | core、**sign** | **单点登录（核心诉求）**：认证中心 / 三种客户端对接模式，纯 Java 库零 Spring 依赖 |
| `pkuse-token-sign` | core | API 签名校验（sso 强制依赖） |
| `pkuse-token-oauth2` | core、jwt、sign | OAuth2 授权服务器四种模式 |
| `pkuse-token-jwt` | core | JWT 令牌风格（Simple / Mixin / Stateless） |
| `pkuse-token-jackson` | core | 默认 JSON 序列化模板 |
| `pkuse-token-redisson` | core | `PkuseTokenDao` 的 Redisson 实现（共享会话存储） |
| `pkuse-token-redisson-spring-boot-starter` | redisson | Redisson DAO 的 Spring Boot 自动装配 |
| `pkuse-token-alone-redisson` | redisson | 为 token 配置独立 Redis 连接（`sa-token.alone-redisson.config`，Redisson 原生 YAML） |
| `pkuse-token-spring-aop` | core | 非 Controller 方法上的注解鉴权（AOP 切面） |

SSO 多端共享会话的最小依赖组合：`spring-boot3-starter + sso + redisson-spring-boot-starter`。

### 2.4 演示与测试（不在主 reactor 内，独立聚合）

```
pkuse-token-demo/     16 个工程，全部运行在 com.bdsoft 构件（SB 3.5.16 / SB 4.1.1）+ Redisson 线
├── pkuse-token-demo-sso/          SSO 家族：server + 模式一/二/三客户端及变体（nosdk/resdk/anon）
├── pkuse-token-demo-oauth2/       OAuth2 授权码模式 server + client
├── pkuse-token-demo-remember-me/  记住我
├── pkuse-token-demo-device-lock/  设备锁定
├── pkuse-token-demo-jwt/          JWT 三模式
└── springboot3-redis / alone-redisson-sb3   存储基线

pkuse-token-test/     5 个模块，187 个用例
├── pkuse-token-springboot-test    147 用例：真实 Spring 上下文 + MockMvc 集成测试
├── pkuse-token-jwt-test           26 用例：JWT 三模式
├── pkuse-token-json-test          7 用例：JSON 引擎与反序列化安全
├── pkuse-token-jackson3-test      5 用例：Jackson 3 模板
└── pkuse-token-easy-test          2 用例：Jackson 3 冒烟
```

## 三、架构设计原理

1. **核心零依赖、能力插件化**：`core` 不依赖任何第三方库（JSON / HTTP 都有内置默认实现），所有增强能力（SSO、OAuth2、存储、JWT）都是独立插件，按需引入、条件装配，不引零开销。
2. **协议库与 Spring 解耦**：`sso` / `oauth2` 等插件是纯 Java 库，不认识 Spring；Spring 装配统一收敛在 starter 链的 Bean 装配层。这样插件可以脱离容器单测，也便于未来支持其他运行时。
3. **条件装配 + 可覆盖**：装配层用 `@ConditionalOnClass`（引了插件才装配对应 Bean）、`@ConditionalOnMissingBean`（业务方注册自己的模板即可覆盖默认行为，如自定义 `PkuseSsoServerTemplate`）。
4. **optional 依赖抑制传递**：Bean 装配层对 sso / oauth2 / sign 声明为 `optional`，依赖不会强加给应用，只有显式引入才参与装配。
5. **三层版本管理**：自家构件（bom）、三方库（dependencies）、Spring 系（spring-boot3-dependencies）各一层 import，版本治理互不牵连。
6. **双注册机制**：Spring Boot 自动配置走 `AutoConfiguration.imports`；插件发现走 SPI 文件 `META-INF/pkusetoken/com.bdsoft.pkusetoken.plugin.PkuseTokenPlugin`。
7. **静态门面 + 容器注入双轨**：核心以 `PkuseManager` 等静态门面提供极简 API，Spring 环境下由装配层把容器 Bean 注入门面，两种风格互通。

## 四、源码阅读指南

### 推荐阅读顺序（由入口到深处）

**第 1 站：跑起来看效果**
- `pkuse-token-demo/pkuse-token-demo-sso/`：先看 `pkuse-token-demo-sso-server`（认证中心）和 `pkuse-token-demo-sso3-client`（模式三客户端，生产最常用），对照 `application.yml` 里的 `sa-token.sso-server.*` / `sso-client.*` 配置理解三种模式差异。

**第 2 站：Spring Boot 集成链（框架如何接入容器）**
- 入口文件：`pkuse-token-spring-boot-webmvc-v3v4-common` 的 `META-INF/spring/...AutoConfiguration.imports` → `PkuseTokenContextRegister`（过滤器 / 拦截器注册）
- 再看 Bean 装配层：`PkuseBeanRegister` / `PkuseBeanInject`（配置如何变成 Bean 并注入门面）、`sso/PkuseSsoBeanRegister`（SSO 配置绑定）

**第 3 站：核心引擎**
- `com.bdsoft.pkusetoken.PkuseManager`：全局门面，一切从这里出发
- `com.bdsoft.pkusetoken.stp.StpLogic` + `StpUtil`：登录模型（login / logout / kickout / 会话）
- `com.bdsoft.pkusetoken.session.PkuseSession`：会话模型
- `com.bdsoft.pkusetoken.dao.PkuseTokenDao`：存储接口（默认内存实现，Redisson 插件替换之）

**第 4 站：SSO 协议（核心诉求）**
- `com.bdsoft.pkusetoken.sso.processor.PkuseSsoServerProcessor` / `PkuseSsoClientProcessor`：请求处理入口（`/sso/auth`、`/sso/check-ticket` 等端点逻辑）
- `com.bdsoft.pkusetoken.sso.template.PkuseSsoServerTemplate` / `PkuseSsoClientTemplate`：协议模板

**第 5 站：存储线**
- `pkuse-token-redisson` 的 `PkuseTokenDaoForRedisson`：`PkuseTokenDao` 的分布式实现

### 关键类速查

| 想了解 | 看哪里 |
|---|---|
| 全局配置与组件 | `core` → `PkuseManager` |
| 登录 / 权限 API | `core` → `StpUtil`、`StpLogic` |
| 注解鉴权 | `spring-aop` 插件 + Web 层 `PkuseInterceptor` |
| 路由拦截 | `core` → `PkuseRouter` |
| SSO 三种模式 | `sso` 插件 + demo-sso 家族 |
| 会话存储替换 | `PkuseTokenDao` 接口及 redisson 实现 |
| yml 配置项绑定 | Bean 装配层 `PkuseBeanRegister` / `PkuseSsoBeanRegister` |

## 五、构建与测试

```bash
# 环境：JDK 17+
# 主工程（24 个模块）
mvn clean install -DskipTests

# 单元测试与集成测试（187 用例）
cd pkuse-token-test && mvn clean test

# 运行 SSO 演示（需本地 Redis）
cd pkuse-token-demo/pkuse-token-demo-sso/pkuse-token-demo-sso-server && mvn spring-boot:run
```

## 六、与上游 Sa-Token 的关系

- 由 sa-token v1.46.0 fork 改造：坐标 `cn.dev33 → com.bdsoft`，包名 `cn.dev33.satoken → com.bdsoft.pkusetoken`；
- 合并上游修复时，先对补丁做一次翻译再应用：
  ```bash
  perl -pe 's/cn\.dev33/com.bdsoft/g; s/com\.bdsoft\.satoken/com.bdsoft.pkusetoken/g; s/\bSa(?=[A-Z])/Pkuse/g' upstream.patch > translated.patch && git apply translated.patch
  ```
- 本平台不承诺跟随上游版本演进，架构与选型以本仓库为准。
