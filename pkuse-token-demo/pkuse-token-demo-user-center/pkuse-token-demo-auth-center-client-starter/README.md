# 应用接入指南（授权中心客户端）

> 各应用接入授权中心的两种方式：**方式 A 引入 starter（推荐，3 步接入）**；方式 B 手动接入（NoSdk 应用）。
> 登录层仍走用户中心 SSO（模式一/二/三/NoSdk 不变），权限层走授权中心——**两层解耦**。

## 一、前置：在用户中心登记应用

1. 用户中心「应用管理」新增（或确认已有）你的应用，拿到：
   - **应用标识**（client_id，如 `sso-client3`）；
   - **接口密钥**（secret_key）——同时用于 SSO 协议与授权中心签名；
2. 用户中心会把应用清单（含密钥）同步到授权中心注册表（启动/每 5 分钟/手动刷新），**密钥轮换后最迟 5 分钟生效，或在授权中心概览页点「刷新应用」立即生效**。

## 二、方式 A：引入 starter（推荐）

### 第 1 步：加依赖

```xml
<dependency>
    <groupId>com.bdsoft</groupId>
    <artifactId>pkuse-token-demo-auth-center-client-starter</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

（本地构建：`cd pkuse-token-demo-auth-center-client-starter && mvn -DskipTests install`）

### 第 2 步：写配置

```yaml
auth-center:
  client:
    base-url: http://localhost:9010    # 授权中心地址
    client: sso-client3                # 本应用标识（与用户中心应用管理一致）
    secret-key: SSO-C3-xxxx            # 接口密钥（与用户中心应用管理一致）
    cache-ttl-seconds: 60              # 权限缓存有效期（秒）
    max-stale-seconds: 1800            # 授权中心故障时的陈旧兜底窗口（秒）
    expose-endpoints: true             # 暴露 /authz/menus、/authz/perms 给前端
    path-prefix: /authz                # 端点路径前缀
```

### 第 3 步：使用（登录流程零改动）

starter 自动提供：

| 能力 | 用法 |
| --- | --- |
| **权限码鉴权** | `StpUtil.checkPermission("sys:user:del")` / `@PkuseCheckPermission("sys:user:del")` 开箱即用（starter 自动注册 StpInterface，权限码来自授权中心并按 loginId 缓存；应用已有自定义 StpInterface 时自动退避，角色维度留给应用） |
| **前端动态菜单** | `expose-endpoints: true` 时自动暴露 `GET /authz/menus`（当前登录用户可见菜单树）与 `GET /authz/perms`（权限码集合），前端据此渲染导航与控制按钮显隐 |
| **手动取数** | 注入 `AuthzClientService`：`getMenus(loginId)` / `getPerms(loginId)` / `warmUp(loginId)`（登录成功后预热，可选） |

参考实现：`pkuse-token-demo-user-center-client3`（后端 `AuthzDemoController` + 前端 `App.vue` 的「授权中心 · 动态菜单与按钮权限」区块）。

### 缓存与降级策略（务必了解）

- TTL（默认 60s）内直接命中本地缓存；
- 授权中心故障时，**30 分钟内（`max-stale-seconds`）沿用陈旧权限**（可用性优先；注意窗口内已在授权中心回收的权限仍生效）；
- 超窗 **fail-closed 返回空集合并告警**（安全优先：无菜单、受控接口 403）。

## 三、方式 B：手动接入（NoSdk 应用，如 client4）

按以下契约自行实现（签名算法与 SSO 协议、client5 开放接口同风格）：

### 接口契约

```
GET {授权中心}/open/authz/menus?client={client}&loginId={loginId}&timestamp={ms}&nonce={随机串}&sign={签名}
    → { code:200, data:[ {id, parentId, menuName, menuType, path, component, perms, icon, sort, children:[...]}, ... ] }

GET {授权中心}/open/authz/perms?client=..&loginId=..&timestamp=..&nonce=..&sign=..
    → { code:200, data:["sys:user:add", "sys:user:edit", ...] }
```

- `menuType`：0=目录 1=页面 2=按钮（menus 返回中已过滤按钮与隐藏页；perms 含隐藏页权限码）；
- 用户无角色时 menus/perms 均为空数组；
- 验签失败/重放/应用未启用统一返回 `code=403`。

### 签名算法

```
① 参与签名参数：client、loginId、timestamp、nonce（按 key 字典序升序拼接 k=v&k=v）
② 末尾追加 &key={secret_key}，整体取 MD5（小写）作为 sign
③ timestamp 为毫秒时间戳（15 分钟窗口），nonce 为一次性随机串（防重放）
```

示例（Java 片段，完整实现可参考 starter 的 `AuthCenterSignUtil`）：

```java
String signSource = "client=" + client
        + "&loginId=" + loginId
        + "&nonce=" + nonce
        + "&timestamp=" + timestamp;   // 已按字典序：client < loginId < nonce < timestamp
String sign = md5(signSource + "&key=" + secretKey);
String url = baseUrl + "/open/authz/perms?client=" + client + "&loginId=" + loginId
        + "&timestamp=" + timestamp + "&nonce=" + nonce + "&sign=" + sign;
```

### 建议的缓存策略

登录成功后拉取并缓存（60s~5min TTL）；拉取失败时短时间内沿用陈旧数据、超时回退为空集合并记录告警（与 starter 行为一致）。

## 四、常见问题

| 现象 | 处理 |
| --- | --- |
| menus/perms 返回空数组 | 该用户未分配角色、角色未绑定菜单，或角色/菜单被禁用；到授权中心「角色管理」绑定 |
| 调用返回 403「签名校验失败」 | 检查 client/secret-key 与用户中心登记是否一致；密钥轮换后先在授权中心概览页「刷新应用」 |
| 授权中心停机后按钮全灰/接口 403 | 已超出陈旧兜底窗口（fail-closed）；恢复授权中心即可，应用登录态不受影响 |
| starter 未生效 | 确认 `auth-center.client.enabled` 未被置为 false，且 base-url/client/secret-key 配置正确 |
