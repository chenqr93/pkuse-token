-- =====================================================================
--  api-platform 初始化脚本：接口快速开发平台（magic-api）相关表 + 种子数据
--  数据库：KingbaseES（与用户中心 server 共用实例/库演示，可按需独立部署）
--  执行：ksql -h 192.168.1.77 -p 54321 -U DEV_USER_CENTER -d PKUSE_TOKEN_USER_CENTER -f init-api-platform.sql
-- =====================================================================

-- 1. magic-api 资源存储表（magic-api.resource.type=database, table-name=s_api_manage）
--    定制版 DatabaseResource 仅使用两列：file_path（接口/脚本路径）、file_content（内容 JSON）
CREATE TABLE IF NOT EXISTS s_api_manage (
    file_path    VARCHAR(255) NOT NULL,
    file_content TEXT,
    CONSTRAINT pk_s_api_manage PRIMARY KEY (file_path)
);

COMMENT ON TABLE  s_api_manage            IS 'magic-api-资源存储（接口/函数/任务等脚本的数据库持久化）';
COMMENT ON COLUMN s_api_manage.file_path  IS '资源路径（如 /fz-demo/hello）';
COMMENT ON COLUMN s_api_manage.file_content IS '资源内容（脚本与元信息 JSON）';

-- 2. 开放接口调用方登记表（动态管理模块，替代原 application.yml 的 magic-open.clients 静态配置）
CREATE TABLE IF NOT EXISTS s_open_client (
    id              VARCHAR(32)  NOT NULL,
    client_id       VARCHAR(64)  NOT NULL,
    client_name     VARCHAR(128),
    secret_key      VARCHAR(128) NOT NULL,
    timeout_seconds BIGINT       DEFAULT 7200,
    status          SMALLINT     DEFAULT 1,
    remark          VARCHAR(255),
    create_time     TIMESTAMP,
    update_time     TIMESTAMP,
    CONSTRAINT pk_s_open_client PRIMARY KEY (id)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_s_open_client_client_id ON s_open_client (client_id);

COMMENT ON TABLE  s_open_client                IS 'magic-api-开放接口调用方登记（动态管理：法治中台内部应用 / 外部第三方应用）';
COMMENT ON COLUMN s_open_client.id             IS '主键（32 位 UUID）';
COMMENT ON COLUMN s_open_client.client_id      IS '调用方标识（令牌签发后 loginId 为 open:{client_id}）';
COMMENT ON COLUMN s_open_client.client_name    IS '显示名';
COMMENT ON COLUMN s_open_client.secret_key     IS '签名密钥（/open/token 参数签名校验用，可经管理接口重置）';
COMMENT ON COLUMN s_open_client.timeout_seconds IS '签发令牌有效期（秒）';
COMMENT ON COLUMN s_open_client.status         IS '状态：0=禁用 / 1=启用（禁用/重置/删除时吊销全部已签发令牌）';
COMMENT ON COLUMN s_open_client.remark         IS '备注';

-- 3. 种子数据：原 yml 静态配置中的两个示例调用方（密钥保持不变，便于沿用既有联调参数）
INSERT INTO s_open_client (id, client_id, client_name, secret_key, timeout_seconds, status, remark, create_time, update_time) VALUES
  ('50001', 'fz-app-demo', '法治中台示例应用', 'Open-Fz-App-kQwIOrYvnXmSDkwEiFngrKidMcdrgKor', 7200, 1,
   '法治中台内部应用（服务间调用；用户级调用走统一登录）', now(), now()),
  ('50002', 'third-party-demo', '第三方示例应用', 'Open-Third-App-kQwIOrYvnXmSDkwEiFngrKidMcdrgKor', 1800, 1,
   '外部第三方应用（使用平台基于 pkuse-token 签发的令牌）', now(), now());

-- 4. magic-api 接口权限绑定（V2：接口级 RBAC；表 s_api_perm）
--    路径命中绑定时，拦截器按登录用户逐个校验 perms（来自授权中心菜单管理的权限码，逗号分隔需全部满足）
CREATE TABLE IF NOT EXISTS s_api_perm (
    id          VARCHAR(32)   NOT NULL,
    path        VARCHAR(255)  NOT NULL,
    method      VARCHAR(10)   DEFAULT '*',
    perms       VARCHAR(255)  NOT NULL,
    status      SMALLINT      DEFAULT 1,
    remark      VARCHAR(255),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    CONSTRAINT pk_s_api_perm PRIMARY KEY (id)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_s_api_perm_path ON s_api_perm (path, method);

COMMENT ON TABLE  s_api_perm          IS 'magic-api-接口权限绑定（V2 接口级 RBAC，权限码来自授权中心）';
COMMENT ON COLUMN s_api_perm.path     IS '接口请求路径（完整 URI）';
COMMENT ON COLUMN s_api_perm.method   IS '请求方法（GET/POST，* 表示全部）';
COMMENT ON COLUMN s_api_perm.perms    IS '所需权限码（逗号分隔，需全部满足；来自授权中心菜单管理）';
COMMENT ON COLUMN s_api_perm.status   IS '状态：1=启用 / 0=停用（停用后不校验）';

-- 示例绑定（需在 magic 编辑器创建同名接口后生效；绑定管理接口：GET/POST /magic/perms）
INSERT INTO s_api_perm (id, path, method, perms, status, remark, create_time) VALUES
  ('60001', '/api/v2/user/page', '*', 'sys:user:list', 1, '示例：用户分页接口绑定 sys:user:list（授权中心 sso-client5 应用需配置同名权限码菜单并绑定角色）', now());
