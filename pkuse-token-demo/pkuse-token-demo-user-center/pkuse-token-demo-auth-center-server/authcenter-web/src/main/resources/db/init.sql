-- =====================================================================
--  Pkuse-Token 授权中心示例 - 数据库初始化脚本（人大金仓 KingbaseES）
--  数据库：PKUSE_TOKEN_AUTH_CENTER（独立库，与用户中心库同实例不同库，生产可分机部署）
--
--  建库：
--    ksql -h 192.168.1.77 -p 54321 -U DEV_USER_CENTER -d postgres
--         -c 'CREATE DATABASE "PKUSE_TOKEN_AUTH_CENTER" WITH OWNER = "DEV_USER_CENTER" ENCODING = '"'"'UTF8'"'"';'
--  建表：本脚本执行一次即可
--
--  设计要点（见 AUTH-CENTER-DESIGN.md）：
--    仅存「授权关系」：菜单 / 角色 / 角色菜单 / 用户角色 / 操作日志；
--    应用档案与用户档案不落地——client_id / user_id 均为用户中心侧的外部引用，不建外键。
-- =====================================================================

-- ------------------------- 1. 菜单表（按应用隔离） -------------------------
CREATE TABLE IF NOT EXISTS ac_menu (
    id          VARCHAR(32)      NOT NULL,
    client_id   VARCHAR(64)      NOT NULL,
    parent_id   VARCHAR(32)      NOT NULL DEFAULT '0',
    menu_name   VARCHAR(50)      NOT NULL,
    menu_type   SMALLINT         NOT NULL,
    path        VARCHAR(200),
    component   VARCHAR(200),
    perms       VARCHAR(100),
    icon        VARCHAR(100),
    sort        INT              NOT NULL DEFAULT 0,
    status      SMALLINT         NOT NULL DEFAULT 0,
    visible     SMALLINT         NOT NULL DEFAULT 0,
    remark      VARCHAR(200),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted     INT              NOT NULL DEFAULT 0,
    create_by   VARCHAR(32),
    update_by   VARCHAR(32),
    delete_by   VARCHAR(32),
    delete_time TIMESTAMP,
    CONSTRAINT pk_ac_menu PRIMARY KEY (id)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_ac_menu_name ON ac_menu (client_id, menu_name) WHERE deleted = 0;
CREATE UNIQUE INDEX IF NOT EXISTS uk_ac_menu_perms ON ac_menu (client_id, perms) WHERE deleted = 0 AND perms IS NOT NULL AND perms <> '';
CREATE INDEX IF NOT EXISTS idx_ac_menu_client ON ac_menu (client_id);
CREATE INDEX IF NOT EXISTS idx_ac_menu_parent ON ac_menu (parent_id);

COMMENT ON TABLE  ac_menu                IS '授权中心-菜单表（按应用隔离：目录/页面/按钮三层）';
COMMENT ON COLUMN ac_menu.client_id      IS '归属应用标识（用户中心应用注册表引用，不建外键）';
COMMENT ON COLUMN ac_menu.parent_id      IS '父菜单 ID（顶级为 0）';
COMMENT ON COLUMN ac_menu.menu_name      IS '菜单名称（应用内唯一）';
COMMENT ON COLUMN ac_menu.menu_type      IS '菜单类型：0=目录 1=页面 2=按钮';
COMMENT ON COLUMN ac_menu.path           IS '路由地址（目录/页面）';
COMMENT ON COLUMN ac_menu.component      IS '前端组件路径（页面）';
COMMENT ON COLUMN ac_menu.perms          IS '权限标识（按钮必填，应用内唯一，如 sys:user:add）';
COMMENT ON COLUMN ac_menu.icon           IS '图标';
COMMENT ON COLUMN ac_menu.sort           IS '同级排序（升序）';
COMMENT ON COLUMN ac_menu.status         IS '状态：0=启用 1=禁用（禁用后不参与分发）';
COMMENT ON COLUMN ac_menu.visible        IS '显示状态：0=显示 1=隐藏（隐藏不进分发树，权限码仍参与鉴权）';

-- ------------------------- 2. 角色表（按应用隔离） -------------------------
CREATE TABLE IF NOT EXISTS ac_role (
    id          VARCHAR(32)      NOT NULL,
    client_id   VARCHAR(64)      NOT NULL,
    role_code   VARCHAR(50)      NOT NULL,
    role_name   VARCHAR(50)      NOT NULL,
    status      SMALLINT         NOT NULL DEFAULT 0,
    remark      VARCHAR(200),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted     INT              NOT NULL DEFAULT 0,
    create_by   VARCHAR(32),
    update_by   VARCHAR(32),
    delete_by   VARCHAR(32),
    delete_time TIMESTAMP,
    CONSTRAINT pk_ac_role PRIMARY KEY (id)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_ac_role_code ON ac_role (client_id, role_code) WHERE deleted = 0;

COMMENT ON TABLE  ac_role           IS '授权中心-角色表（应用内角色，非平台角色）';
COMMENT ON COLUMN ac_role.client_id IS '归属应用标识（用户中心应用注册表引用，不建外键）';
COMMENT ON COLUMN ac_role.role_code IS '角色编码（应用内唯一，字母数字下划线）';
COMMENT ON COLUMN ac_role.role_name IS '角色名称';
COMMENT ON COLUMN ac_role.status    IS '状态：0=启用 1=禁用（禁用后不参与分发，授权关系保留）';

-- ------------------------- 3. 角色-菜单绑定表 -------------------------
CREATE TABLE IF NOT EXISTS ac_role_menu (
    id          VARCHAR(32)      NOT NULL,
    role_id     VARCHAR(32)      NOT NULL,
    menu_id     VARCHAR(32)      NOT NULL,
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted     INT              NOT NULL DEFAULT 0,
    create_by   VARCHAR(32),
    update_by   VARCHAR(32),
    delete_by   VARCHAR(32),
    delete_time TIMESTAMP,
    CONSTRAINT pk_ac_role_menu PRIMARY KEY (id)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_ac_role_menu ON ac_role_menu (role_id, menu_id) WHERE deleted = 0;

COMMENT ON TABLE ac_role_menu IS '授权中心-角色-菜单绑定表';

-- ------------------------- 4. 用户-角色绑定表 -------------------------
CREATE TABLE IF NOT EXISTS ac_user_role (
    id          VARCHAR(32)      NOT NULL,
    user_id     VARCHAR(32)      NOT NULL,
    role_id     VARCHAR(32)      NOT NULL,
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted     INT              NOT NULL DEFAULT 0,
    create_by   VARCHAR(32),
    update_by   VARCHAR(32),
    delete_by   VARCHAR(32),
    delete_time TIMESTAMP,
    CONSTRAINT pk_ac_user_role PRIMARY KEY (id)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_ac_user_role ON ac_user_role (user_id, role_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_ac_user_role_role ON ac_user_role (role_id);

COMMENT ON TABLE  ac_user_role      IS '授权中心-用户-角色绑定表';
COMMENT ON COLUMN ac_user_role.user_id IS '用户中心用户 ID（外部引用，不建外键）';

-- ------------------------- 5. 操作日志表（按月分表：ac_oper_log_yyyyMM） -------------------------
-- 父表仅作分区路由：数据按 create_time 落到月分区；分区由后端启动时与每日 00:05 自动创建（AcLogPartitionMaintainer）
CREATE TABLE IF NOT EXISTS ac_oper_log (
    id             VARCHAR(32)   NOT NULL,
    oper_user_id   VARCHAR(32),
    oper_user_name VARCHAR(50),
    module         VARCHAR(50),
    action         VARCHAR(50),
    method         VARCHAR(200),
    params         VARCHAR(2000),
    before_data    VARCHAR(2000),
    after_data     VARCHAR(2000),
    success        SMALLINT,
    message        VARCHAR(500),
    cost_ms        BIGINT,
    ip             VARCHAR(64),
    ua             VARCHAR(500),
    create_time    TIMESTAMP     NOT NULL,
    CONSTRAINT pk_ac_oper_log PRIMARY KEY (id, create_time)
) PARTITION BY RANGE (create_time);

COMMENT ON TABLE  ac_oper_log            IS '授权中心-操作日志表（@OperLog 切面自动写入；参数摘要 + 修改前/后数据快照，按 create_time 按月分表）';
COMMENT ON COLUMN ac_oper_log.oper_user_name IS '操作人账号（冗余存储，用户中心删除用户后日志仍可读）';
COMMENT ON COLUMN ac_oper_log.before_data   IS '修改前数据快照（JSON：[{table,operation,rows:[...]}]，由变更捕获拦截器自动登记）';
COMMENT ON COLUMN ac_oper_log.after_data    IS '修改后数据快照（新增=写入数据含主键、更新=修改后、删除为空）';

-- =====================================================================
--  种子数据：为 sso-client3 预置演示菜单 / 角色 / 用户绑定
--  （应用清单本身无种子：启动后从用户中心注册表自动拉取）
-- =====================================================================

-- 菜单：系统管理（目录）→ 用户管理（页面）→ 新增/编辑/删除（按钮）；演示中心 → 关于 + 隐藏详情页
INSERT INTO ac_menu (id, client_id, parent_id, menu_name, menu_type, path, component, perms, icon, sort, status, visible, create_time, create_by) VALUES
  ('m1001', 'sso-client3', '0',     '系统管理',   0, '/sys',        NULL,                 NULL,           'Setting', 10, 0, 0, now(), '10001'),
  ('m1002', 'sso-client3', 'm1001', '用户管理',   1, '/sys/user',   'sys/user/index',     'sys:user:list', NULL,      10, 0, 0, now(), '10001'),
  ('m1003', 'sso-client3', 'm1002', '用户新增',   2, NULL,          NULL,                 'sys:user:add',  NULL,      10, 0, 0, now(), '10001'),
  ('m1004', 'sso-client3', 'm1002', '用户编辑',   2, NULL,          NULL,                 'sys:user:edit', NULL,      20, 0, 0, now(), '10001'),
  ('m1005', 'sso-client3', 'm1002', '用户删除',   2, NULL,          NULL,                 'sys:user:del',  NULL,      30, 0, 0, now(), '10001'),
  ('m1006', 'sso-client3', '0',     '演示中心',   0, '/demo',       NULL,                 NULL,           'Star',    20, 0, 0, now(), '10001'),
  ('m1007', 'sso-client3', 'm1006', '关于',       1, '/demo/about', 'demo/about/index',   NULL,           NULL,      10, 0, 0, now(), '10001'),
  ('m1008', 'sso-client3', 'm1006', '详情(隐藏)', 1, '/demo/detail','demo/detail/index',  'demo:detail',   NULL,      20, 0, 1, now(), '10001');

-- 角色：admin（全部菜单）/ user（无删除按钮；含隐藏页的权限码用于鉴权演示）
INSERT INTO ac_role (id, client_id, role_code, role_name, status, remark, create_time, create_by) VALUES
  ('r3001', 'sso-client3', 'admin', '管理员', 0, '全部菜单与按钮', now(), '10001'),
  ('r3002', 'sso-client3', 'user',  '普通用户', 0, '无删除按钮，隐藏页不显示但可鉴权', now(), '10001');

-- 角色-菜单绑定：admin 全量；user 排除删除按钮（m1005）
INSERT INTO ac_role_menu (id, role_id, menu_id, create_time, create_by) VALUES
  ('b4001', 'r3001', 'm1001', now(), '10001'),
  ('b4002', 'r3001', 'm1002', now(), '10001'),
  ('b4003', 'r3001', 'm1003', now(), '10001'),
  ('b4004', 'r3001', 'm1004', now(), '10001'),
  ('b4005', 'r3001', 'm1005', now(), '10001'),
  ('b4006', 'r3001', 'm1006', now(), '10001'),
  ('b4007', 'r3001', 'm1007', now(), '10001'),
  ('b4008', 'r3001', 'm1008', now(), '10001'),
  ('b4009', 'r3002', 'm1001', now(), '10001'),
  ('b4010', 'r3002', 'm1002', now(), '10001'),
  ('b4011', 'r3002', 'm1003', now(), '10001'),
  ('b4012', 'r3002', 'm1004', now(), '10001'),
  ('b4013', 'r3002', 'm1006', now(), '10001'),
  ('b4014', 'r3002', 'm1007', now(), '10001'),
  ('b4015', 'r3002', 'm1008', now(), '10001');

-- 用户-角色绑定：zhangsan（用户中心种子用户 ID 10002）→ sso-client3 普通用户角色
-- （应用内角色与平台角色无关：admin 是否能进某应用由该应用自己的角色绑定决定）
INSERT INTO ac_user_role (id, user_id, role_id, create_time, create_by) VALUES
  ('u5001', '10002', 'r3002', now(), '10001');

-- 控制台自身角色（认证/授权职责拆分后，管理端角色走本地 RBAC，client=auth-center，
-- 与 upgrade-v3-console-role.sql 一致；管理员角色绑定用户中心平台管理员 10001）
INSERT INTO ac_role (id, client_id, role_code, role_name, status, remark, create_time, create_by) VALUES
  ('r9001', 'auth-center', 'admin', '控制台管理员', 0, '授权中心控制台自身管理角色（本地 RBAC，种子数据）', now(), '10001');

INSERT INTO ac_user_role (id, user_id, role_id, create_time, create_by) VALUES
  ('u9001', '10001', 'r9001', now(), '10001');
