# agent.md — 通用项目开发规范（供 AI Agent 与开发者遵循）

> 本规范提炼自 `pkuse-token-demo-user-center-server` 分层多模块工程，适用于后续同风格的
> SpringBoot 后端项目。新建项目时将本文件复制到工程根目录（可重命名为 `AGENTS.md` 以便
> AI 工具自动加载），并把 `{app}` 占位符替换为实际应用名。

---

## 1. 技术栈基线

| 项 | 约定 |
| --- | --- |
| JDK | 17 |
| 框架 | Spring Boot 3.5.x（parent：`spring-boot-starter-parent`） |
| ORM | MyBatis-Plus 3.5.14 + `mybatis-plus-jsqlparser`（3.5.9+ 分页/插件必需） |
| 数据库 | 人大金仓 KingbaseES（`kingbase8` 驱动，兼容 PostgreSQL 方言）；连接池 Druid（`druid-spring-boot-3-starter`） |
| 认证/SSO | Pkuse-Token（`pkuse-token-spring-boot3-starter` + `pkuse-token-sso` + `pkuse-token-redisson-spring-boot-starter`） |
| AOP | spring-boot-starter-aop（操作日志切面等基础设施） |
| 日志与监控 | **部门 Java Web 开发标准强制**：Log4j2（Boot 3.5.16 管理，2.24.3 ≥ 2.22.1）、p6spy 3.9.1（SQL 监控，≥ 1.9.0）、JavaMelody 2.6.0（运行监控，≥ 2.5.0）；**禁用默认 Logback** |
| 注意 | **不使用 Lombok**；POJO 手写 getter/setter/toString |

## 2. 模块结构（分层依赖单向无环）

```
{app}-server/                  # 聚合父 POM（packaging=pom，${revision} + flatten）
├── {app}-common/              # 通用层（零内部依赖叶子）：annotation / config / exception / pojo / util
├── {app}-model/               # 数据模型层：{域}.{po,bo,dto,vo}
├── {app}-service/             # 业务层：Service 接口 + impl + Mapper（含自定义 XML）+ support 域内支撑
├── {app}-api/                 # 对外接口层：{域}.controller（入参 DTO、出参 VO）
└── {app}-web/                 # 启动与装配：启动类 / config / handler / application*.yml / db 脚本（可执行 jar）
```

- 依赖方向：`web → api → service → model → common`，**单向无环**，禁止反向依赖与跨层依赖。
- Java 包名不跨模块拆分（无 split package）；模块划分即依赖边界。
- 各模块职责：
  - **common**：自定义注解、`BizException` 业务异常、工具类、分页基类 `PageQueryDTO`、审计父类 `AbstractAuditPO`、平台配置定义（`@ConfigurationProperties` 类）。除注解级依赖（如 `mybatis-plus-annotation`、`bcprov`）外不引重依赖。
  - **model**：纯数据模型，**零逻辑**（仅允许 `VO.from(Entity)` 转换工厂与常量）。
  - **service**：`XxxService extends IService<Entity>` + `XxxServiceImpl extends ServiceImpl<XxxMapper, Entity>`；`XxxMapper extends BaseMapper<Entity>`；域内支撑组件放 `{域}.support`（切面/拦截器/缓存组件）。
  - **api**：仅 Controller，轻量编排与 DTO/VO 转换，**不写业务规则**。
  - **web**：启动类 `@SpringBootApplication`、`config/` 装配（MyBatis-Plus、Sa-Token、验证码、HttpTemplate 等）、`handler/GlobalExceptionHandler`、`application*.yml`、`log4j2-spring.xml` + `spy.properties`（日志与 SQL 监控）、`db/` 脚本、`assembly/` 部署描述。

## 3. Maven 规范

- **版本管理**：父 POM 用 `${revision}`（如 `0.0.1-SNAPSHOT`）+ `flatten-maven-plugin`
  （`flattenMode=resolveCiFriendliesOnly`，`updatePomFile=true`）统一版本；子模块版本一律 `${revision}`。
- **依赖声明**：所有第三方版本号**只在聚合父 POM 的 `<properties>` + `<dependencyManagement>` 声明一次**；
  子模块引依赖不写版本号。版本由 spring-boot-starter-parent 管理的（spring 全家桶）不重复声明。
  新增依赖必须先进父 POM `dependencyManagement`。
- **Netty 对齐**：父 POM import `netty-bom` 统一 Netty 版本（pkuse-token-redisson 排除了 netty-codec，web 模块需显式补齐 `io.netty:netty-codec`）。
- **环境 Profile**：`dev`（默认）/ `test` / `uat` / `prod`，各自定义 `profiles.active` 属性，
  用于资源过滤替换 `application.yml` 中的 `@profiles.active@`，并决定 assembly 部署包 conf/ 携带的环境配置。
- **打包（web 模块）**：
  - `spring-boot-maven-plugin` 配置 `<classifier>exec</classifier>`：主产物为瘦包（供 assembly 装配），另出 `*-exec.jar` fat 包（单文件运行）。
  - `maven-assembly-plugin`（descriptor：`src/main/assembly/assembly.xml`）：装配 `bin/`（启停脚本 bat+sh）、`conf/`（外置配置）、`lib/`（依赖）、应用瘦包在根目录。
- **常用命令**：
  ```bash
  mvn -DskipTests package                 # 全量打包（-P dev|test|uat|prod 选环境，默认 dev）
  mvn -DskipTests install                 # 安装到本地仓库（热启动前置）
  mvn -f {app}-web/pom.xml spring-boot:run  # 开发热启动（需先 install）
  ```

## 4. 包结构与命名（域优先）

- 包结构：`com.bdsoft.{app}.{域}.{层}`。层后缀固定：
  `controller`（api 模块）、`service` + `service.impl`、`mapper`、`support`（service 模块）、
  `po` / `bo` / `dto` / `vo`（model 模块）；公共的放 common 模块对应包。
- 业务域按功能划分（如 auth 认证 / user 用户 / client 应用 / authz 授权 / device 设备 / log 日志 / config 配置 / dashboard 聚合）。
- 类命名约定：
  - 表实体：`Uc` 前缀 + 业务名（如 `UcUser`、`UcClient`），对应表 `uc_xxx`（前缀可按应用调整，但表名与 PO 名保持一致映射）。
  - Service 接口 `XxxService`、实现 `XxxServiceImpl`、Mapper `XxxMapper`、Controller `XxxController`。
  - 传输对象：入参 `XxxDTO` / `XxxSaveDTO` / `XxxQueryDTO`，出参 `XxxVO`，业务中间对象 `XxxBO`。
- **常量**：枚举值/魔法值定义为 `public static final` 常量，放在对应 PO（如 `UcUser.STATUS_ENABLED`）或 Service 接口（如 `UcUserService.BUILT_IN_ADMIN_ID`）上，禁止裸写魔法值。

## 5. 数据模型规范

- **口诀：Entity 对表、DTO 管传输、VO 管展示、BO 管业务。**
  - Entity（PO）仅在 Mapper / Service 层内部流转，**不进出接口层**；
  - Controller 入参一律 DTO、出参一律 VO（Controller 完成转换）；
  - BO 仅 Service 层内部使用。
- **VO 必须提供 `public static XxxVO from(Entity e)` 静态转换工厂**（null 安全，排除 password 等敏感字段）。
- **审计父类**：需要审计的 PO 继承 `common.pojo.AbstractAuditPO`（七字段：`deleted` / `createBy·updateBy·deleteBy` / `createTime·updateTime·deleteTime`）：
  - `deleted` 用 `@TableLogic(value="0", delval="1")` 逻辑删除：查询自动过滤，删除转 UPDATE；
  - `createBy/updateBy/createTime/updateTime` 由 MyBatis-Plus `MetaObjectHandler` 自动填充（操作人取当前登录用户 ID，未登录场景留空）；
  - `deleteBy/deleteTime` 在业务删除场景**手工设置**（逻辑删除只置 deleted，不填这两列）；
  - 追加型日志表不继承审计父类（仅保留自有 create_time）。
- 主键：MyBatis-Plus `ASSIGN_UUID`（32 位 UUID 字符串，`@TableId(type = IdType.ASSIGN_UUID)` + `String` 主键），跨库通用、前端无精度丢失。
- POJO 规约：属性一律**包装类型**（Integer/Long），不设默认值；序列化实现 `Serializable` 并声明 `serialVersionUID = 1L`；
  时间字段用 `java.time.LocalDateTime`；手写 getter/setter 与 `toString()`（不用 Lombok）。

## 6. 统一响应与异常

### 6.1 PkuseResult（统一响应包装，来自 SDK）

- 统一返回类为 **`com.bdsoft.pkusetoken.util.PkuseResult`**，由 **Pkuse-Token SDK 提供**
  （`pkuse-token-spring-boot3-starter` 传递引入 `pkuse-token-core`，其内部经
  `pkuse-token-jakarta-servlet → pkuse-token-core` 带入）。
- **各项目直接使用 SDK 的 PkuseResult，不要自行实现、不要从其他工程拷贝借用类。**
- 用法：
  ```java
  return PkuseResult.ok();                      // 成功（无提示）
  return PkuseResult.ok("新增成功");             // 成功 + 提示
  return PkuseResult.ok().setData(vo);          // 成功 + data
  return PkuseResult.ok().set("total", x).set("current", y).setData(records);  // 附加键值（链式）
  return PkuseResult.error("系统异常：" + msg);  // 失败（code=500）
  return PkuseResult.get(code, msg, data);      // 指定业务码
  ```
- code 语义（前端按 code 判断）：`200` 成功 / `401` 未登录（跳统一登录页）/ `403` 无权限 / `500` 业务失败或系统异常；
  业务扩展码：`421` 新设备需验证 / `423` 账号锁定 / `426` 需先修改密码 / `428` 需要验证码。

### 6.2 BizException（业务异常）

- Service 层抛 `common.exception.BizException`（默认 code=500，可携带业务码与附加 data），
  全局异常处理器统一转换为 PkuseResult；**业务校验失败一律抛 BizException，不返回 null/布尔约定**。
- 常用模式（不存在即抛）：
  ```java
  public UcUser getByIdOrThrow(String id) {
      UcUser user = (id == null ? null : this.getById(id));
      if (user == null) { throw new BizException("用户不存在"); }
      return user;
  }
  ```

### 6.3 GlobalExceptionHandler（web 模块必备）

`@RestControllerAdvice` 统一转换，缺一不可：

| 异常 | 返回 |
| --- | --- |
| `NotLoginException`（SDK） | `PkuseResult.get(CODE_NOT_LOGIN, "未登录或登录已过期…", null)` → 401 |
| `NotRoleException` / `NotPermissionException`（SDK） | `PkuseResult.get(CODE_NOT_PERMISSION, "无权限…", null)` → 403 |
| `BizException` | `PkuseResult.get(e.getCode(), e.getMessage(), e.getData())` |
| `PkuseTokenException`（SDK 框架异常） | `PkuseResult.error(e.getMessage())` |
| `Exception`（兜底） | `log.error` 记录堆栈 + `PkuseResult.error("系统异常：" + e.getMessage())` |

## 7. 接口层（api）规范

- RESTful 风格：`GET /api/{域}/page` 分页、`GET /api/{域}/{id}` 详情、`POST` 新增、`PUT` 修改、`DELETE /{id}` 删除；子资源/动作用路径后缀（如 `PUT /{id}/status`、`PUT /{id}/password`）。
- 类上 `@RestController + @RequestMapping("/api/{域}")`；方法上单行 Javadoc 说明用途。
- **管理动作必须标注 `@OperLog(module = "xxx管理", action = "yyy")`**（注解定义在 common，切面在 service 域 support 中自动记录，写日志异常不影响业务）。
- 分页标准写法：查询 DTO 继承 `PageQueryDTO`（含 `currentOrDefault()/sizeOrDefault()` 规范化），
  Service 返回 `Page<Entity>`，Controller 转 `List<VO>` 后用
  `PkuseResult.ok().set("total", ...).set("current", ...).set("size", ...).setData(records)` 返回。
- 列表页补充统计字段（计数等）**批量查询后内存回填**，禁止逐行查询（N+1）。
- 权限：管理接口由拦截器统一 `StpUtil.checkRole("admin")`（web 模块 config 装配），Controller 内不散写权限判断。
- Service 注入用 `jakarta.annotation.Resource` 字段注入。

## 8. 业务层（service）规范

- 接口 `extends IService<Entity>`，业务方法**在接口声明**（带 Javadoc），实现类 `@Service` 继承 `ServiceImpl<Mapper, Entity>`。
- 参数校验在 Service 层**显式 if + BizException**（校验优先于任何写操作）；唯一性校验记得排除自身（更新场景）。
- 查询构造用 `LambdaQueryWrapper` + 条件重载（`query.getStatus() != null` / `hasText(...)`），条件为空即不加该过滤；排序显式声明。
- **置空字段必须用 `LambdaUpdateWrapper.set(field, null)`**（`updateById` 忽略 null 字段）：
  ```java
  this.update(new LambdaUpdateWrapper<UcUser>()
          .eq(UcUser::getId, id)
          .set(UcUser::getLockedUntil, null)
          .set(UcUser::getUpdateTime, LocalDateTime.now()));
  ```
- 逻辑删除前先显式填 `deleteBy/deleteTime`（同上 Wrapper），再 `removeById`。
- 级联清理：删除主实体时同步清理关联表数据与会话（如 `StpUtil.logout(id)`），防在线统计残留。
- 字符串判空统一用私有静态 `hasText(String)`（`text != null && !text.isBlank()`）。

## 9. 持久层规范

- 通用 CRUD 复用 MyBatis-Plus `IService` / `BaseMapper`，**不手写简单 SQL**。
- 自定义 SQL 的 XML 与 Mapper 接口**同模块**存放：`{app}-service/src/main/resources/mapper/`；
  扫描路径 `mybatis-plus.mapper-locations: classpath*:mapper/**/*.xml`。
- **多数据库方言兼容**（KingbaseES / 达梦 / MySQL）：按方言建子目录（`mapper/kingbase/`、`mapper/dm/`、`mapper/mysql/`）
  或同一文件内用 `databaseId` 属性区分；不带 `databaseId` 的语句为默认实现；
  当前方言由 web 模块 `MybatisPlusConfig` 的 `VendorDatabaseIdProvider` 按 JDBC 产品名自动解析。
- `mybatis-plus.configuration.map-underscore-to-camel-case: true`；全局 `id-type: assign_uuid`。
- 大数据量日志表按时间分区（`_yyyyMM` / `_yyyyww` / `_yyyyMMdd`），由 support 组件（分区维护器）在启动/定时/配置变更时自动建分区。

## 10. 日志与监控规范（部门 Java Web 开发标准强制）

三项强制基线：**Log4j2 ≥ 2.22.1、p6spy ≥ 1.9.0、JavaMelody ≥ 2.5.0**。新项目必须按下述方式集成，不得使用默认 Logback。三项版本集中在聚合父 POM 的 `<properties>` 声明（log4j2 版本由 spring-boot-starter-parent 管理无需声明）。

### 10.1 Log4j2（日志实现）

- web 模块引入 `spring-boot-starter-log4j2`（版本由 spring-boot-starter-parent 管理，Boot 3.5.16 → 2.24.3，满足 ≥ 2.22.1）；
- **所有会传递引入 Boot starter 的依赖，声明时必须排除 `spring-boot-starter-logging`**——今后新增任何 `spring-boot-starter-*` 依赖都要带上该 exclusion（示例：starter-web、starter-validation、starter-aop）：
  ```xml
  <exclusions>
      <exclusion>
          <groupId>org.springframework.boot</groupId>
          <artifactId>spring-boot-starter-logging</artifactId>
      </exclusion>
  </exclusions>
  ```
- 验证方式（期望无任何输出）：
  ```bash
  mvn dependency:tree -Dincludes=ch.qos.logback
  ```
- 配置文件固定为 `src/main/resources/log4j2-spring.xml`（带 `-spring` 后缀，Spring Boot 提前加载并支持 `<SpringProfile>`）：
  Console + RollingFile（`logs/app.log`，按天 + 100MB 滚动，保留 30 个）双通道；固定保留 `p6spy` logger（INFO）；
- 代码统一 SLF4J API（`private static final Logger log = LoggerFactory.getLogger(Xxx.class);`），不直接依赖 log4j API，不使用 @Slf4j。

### 10.2 p6spy（SQL 监控）

- 父 POM 声明版本并 dependencyManagement，web 模块引入 `p6spy:p6spy`；数据源接入三要素：
  1. url 加 `jdbc:p6spy:` 前缀（如 `jdbc:p6spy:kingbase8://host:port/db`，各 `application-{env}.yml`）；
  2. `driver-class-name: com.p6spy.engine.spy.P6SpyDriver`；
  3. `src/main/resources/spy.properties` 中 `driverlist=` 真实驱动（如 `com.kingbase8.Driver`）。
- SQL 日志经 `Slf4JLogger` 输出到 Log4j2（logger 名 `p6spy`），格式 `CustomLineFormat`（时间 | 耗时 | 连接 | SQL）；
- 慢 SQL 检测常开：`outagedetection=true` + `outagedetectioninterval=3000`（毫秒）；
- **不再使用 mybatis `log-impl` 打印 SQL**（dev 环境也不配 StdOutImpl，SQL 统一走 p6spy）。

### 10.3 JavaMelody（运行监控）

- web 模块引入 `net.bull.javamelody:javamelody-spring-boot-starter`（强制依赖仅 javamelody-core + starter-aop，quartz/actuator 等均为 optional 不必排除）；
- 公共 `application.yml` 声明 `javamelody.enabled: true`；启动后监控页 `http://localhost:{port}/monitoring`（HTTP 请求/SQL/内存/线程/会话）；
- 生产环境保持开启，通过网关/防火墙限制 `/monitoring` 的访问来源。

## 11. 配置文件规范（web 模块）

- `application.yml`：各环境**通用**配置 + 注释块说明；环境差异（端口/数据源/Redis/SQL 日志/SSO 地址）放 `application-{env}.yml`（dev/test/uat/prod 四份齐全）。
- `spring.profiles.active: @profiles.active@` 由 Maven profile 资源过滤替换；运行时可被 `--spring.profiles.active=xxx` 覆盖。
- 敏感信息（数据源账号密码等）只进环境配置文件，不进公共配置；演示用开关（如验证码回显）必须注明「生产环境必须关闭」。
- 配置分区原则：**部署级参数**走 yml；**运行期可调规则**（阈值/开关/策略）入库（键值对配置表），管理端在线维护、即时生效，默认值由代码常量类统一定义并在启动时自动补齐。

## 12. 数据库脚本规范（web 模块 `db/`）

- `init.sql`：全量初始化脚本（建表 + 索引 + 内置数据），新库直接执行。
- `upgrade-vN-xxx.sql`：增量升级脚本，按版本序号命名、按序执行；表结构变更必须配套脚本。
- 约定：审计七字段齐全；唯一索引用**部分索引**（`WHERE deleted=0`）以支持删除后重建同名数据；
  字符串主键 `VARCHAR(32)`；SQL 兼容 KingbaseES（PostgreSQL 方言）。

## 13. 编码风格

- **注释**：中文 Javadoc。类头说明职责与要点（用 `<p>` 分段），方法/字段用单行 `/** */`；
  注释解释「为什么/业务约束」，不复述代码；框架集成类必须写明「缺了会怎样」（如缺 HttpTemplateConfig 的后果）。
- **日志**：`private static final Logger log = LoggerFactory.getLogger(Xxx.class);`（SLF4J，不用 @Slf4j）。
- **依赖注入**：`jakarta.annotation.Resource` 字段注入（配置类可用构造器注入 + `@Autowired` 方法）。
- **导入顺序**：框架/SDK → 本项目 → jakarta → java 标准库，字母序，不用通配符 `.*`。
- 禁止事项：不引 Lombok；不让 Entity 进出 Controller；不让 api 直接用 Mapper；不跨层依赖；
  不写裸魔法值；不从其他工程拷贝借用类（依赖一律来自父 POM 管理的坐标）；
  不使用 Logback（日志一律 Log4j2）；不用 mybatis log-impl 打印 SQL（一律 p6spy）。

## 14. AI Agent 工作流程

新增/修改业务功能时按此顺序操作：

1. **建表/改表**：先写 `db/` 脚本（init.sql 同步更新 + 增量 upgrade-vN）；
2. **model**：PO（继承审计父类）→ DTO（入参）→ VO（出参，含 `from()` 工厂）→ BO（如需）；
3. **service**：Mapper（`extends BaseMapper`，复杂 SQL 配 XML）→ Service 接口（方法 + Javadoc + 常量）→ ServiceImpl（校验/业务/级联）；
4. **api**：Controller（RESTful + `@OperLog` + DTO 入/VO 出 + PkuseResult 返回）；
5. **web**：如需新装配则加 config 类；涉及权限的在拦截器规则中补路径；
6. **自检**：`mvn -DskipTests compile`（至少 compile）通过；`mvn dependency:tree -Dincludes=ch.qos.logback` 无输出（无 Logback 残留）；
   确认依赖方向未破坏、无借用类、无魔法值；
7. **文档**：更新模块 README（新接口、新配置项、新表）。

修改已有代码时：保持既有风格（即使有更优写法），先读同包相邻文件再动手；错误信息、注释、日志全部使用中文。
