-- =====================================================================
--  Pkuse-Token 用户中心示例 - 数据库初始化脚本（人大金仓 KingbaseES）
--  数据库：PKUSE_TOKEN_USER_CENTER
--  说明：首次部署时执行一次即可（ksql -f init.sql 或 数据库客户端直接执行）
--
--  注意：KingbaseES 保留了 sys_ 前缀（sys_catalog 下存在系统视图 sys_user 等），
--       因此业务表不要使用 sys_ 前缀，否则 unqualified 名称会被解析到系统对象。
-- =====================================================================

-- ------------------------- 1. 用户表 -------------------------
CREATE TABLE IF NOT EXISTS uc_user (
    id              VARCHAR(32)       NOT NULL,
    username        VARCHAR(64)  NOT NULL,
    password        VARCHAR(64)  NOT NULL,
    password_salt   VARCHAR(64),
    nickname        VARCHAR(64),
    email           VARCHAR(128),
    phone           VARCHAR(32),
    role            VARCHAR(32)  DEFAULT 'user',
    status          SMALLINT     DEFAULT 1,
    remark          VARCHAR(255),
    last_login_time TIMESTAMP,
    login_fail_count INT         DEFAULT 0,
    locked_until    TIMESTAMP,
    lock_reason     VARCHAR(255),
    must_change_password SMALLINT DEFAULT 0,
    create_time     TIMESTAMP,
    update_time     TIMESTAMP,
    deleted         INT          DEFAULT 0 NOT NULL,
    create_by       VARCHAR(32),
    update_by       VARCHAR(32),
    delete_by       VARCHAR(32),
    delete_time     TIMESTAMP,
    CONSTRAINT pk_uc_user PRIMARY KEY (id)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_uc_user_username ON uc_user (username) WHERE deleted = 0;

COMMENT ON TABLE  uc_user                 IS '用户中心-用户表';
COMMENT ON COLUMN uc_user.id              IS '主键';
COMMENT ON COLUMN uc_user.username        IS '登录账号';
COMMENT ON COLUMN uc_user.password        IS '密码摘要：SM3(password_salt + ":" + 明文)（历史数据为 MD5(明文)）';
COMMENT ON COLUMN uc_user.password_salt   IS '密码盐（国密 SM3 摘要使用；历史 MD5 数据此列为空，登录成功后自动升级）';
COMMENT ON COLUMN uc_user.nickname        IS '昵称';
COMMENT ON COLUMN uc_user.email           IS '邮箱';
COMMENT ON COLUMN uc_user.phone           IS '手机号';
COMMENT ON COLUMN uc_user.role            IS '角色：admin=系统管理员 / user=普通用户';
COMMENT ON COLUMN uc_user.status          IS '状态：0=禁用 / 1=启用';
COMMENT ON COLUMN uc_user.remark          IS '备注';
COMMENT ON COLUMN uc_user.last_login_time IS '最后登录时间';
COMMENT ON COLUMN uc_user.login_fail_count IS '连续登录失败次数';
COMMENT ON COLUMN uc_user.locked_until     IS '锁定截止时间（为空表示未锁定）';
COMMENT ON COLUMN uc_user.lock_reason      IS '锁定原因（如：连续密码错误 5 次）';
COMMENT ON COLUMN uc_user.must_change_password IS '是否需要修改密码：1=下次登录需强制修改（初始密码/管理员重置后置位，修改成功后清零）';
COMMENT ON COLUMN uc_user.create_time     IS '创建时间';
COMMENT ON COLUMN uc_user.update_time     IS '更新时间';

-- ------------------------- 2.1 登录日志表（按月分表） -------------------------
-- 父表仅作分区路由：数据按 create_time 落到月分区 uc_login_log_yyyyMM
-- 月分区由后端启动时与每日定时任务自动创建（见 LoginLogService#ensurePartition）
CREATE TABLE IF NOT EXISTS uc_login_log (
    id          VARCHAR(32)       NOT NULL,
    username    VARCHAR(64),
    user_id     VARCHAR(32),
    login_type  VARCHAR(16)  DEFAULT 'login',
    result      VARCHAR(24)  NOT NULL,
    msg         VARCHAR(255),
    ip          VARCHAR(64),
    user_agent  VARCHAR(512),
    os          VARCHAR(64),
    browser     VARCHAR(64),
    device_id   VARCHAR(128),
    create_time TIMESTAMP    NOT NULL,
    CONSTRAINT pk_uc_login_log PRIMARY KEY (id, create_time)
) PARTITION BY RANGE (create_time);

COMMENT ON TABLE  uc_login_log             IS '用户中心-登录日志表（按 create_time 按月分表）';
COMMENT ON COLUMN uc_login_log.login_type  IS '类型：login=登录 / logout=注销';
COMMENT ON COLUMN uc_login_log.result      IS '结果：success / fail / locked / captcha_fail / device_lock / disabled / no_user';
COMMENT ON COLUMN uc_login_log.device_id   IS '设备指纹（客户端上报）';
COMMENT ON COLUMN uc_login_log.create_time IS '发生时间（分区键）';

-- ------------------------- 2.2 操作日志表（管理动作审计，按月分表） -------------------------
-- 父表仅作分区路由：数据按 create_time 落到月分区 uc_oper_log_yyyyMM
-- 月分区由后端启动时与每日定时任务自动创建（见 LogPartitionMaintainer）
CREATE TABLE IF NOT EXISTS uc_oper_log (
    id          VARCHAR(32)       NOT NULL,
    user_id     VARCHAR(32),
    username    VARCHAR(64),
    module      VARCHAR(64),
    action      VARCHAR(64),
    method      VARCHAR(255),
    params      VARCHAR(2000),
    before_data VARCHAR(2000),
    after_data  VARCHAR(2000),
    result      VARCHAR(16)  DEFAULT 'success',
    msg         VARCHAR(500),
    cost_time   BIGINT,
    ip          VARCHAR(64),
    user_agent  VARCHAR(512),
    os          VARCHAR(64),
    browser     VARCHAR(64),
    create_time TIMESTAMP    NOT NULL,
    CONSTRAINT pk_uc_oper_log PRIMARY KEY (id, create_time)
) PARTITION BY RANGE (create_time);

COMMENT ON TABLE  uc_oper_log             IS '用户中心-操作日志表（管理动作审计，按 create_time 按月分表）';
COMMENT ON COLUMN uc_oper_log.user_id     IS '操作人用户 ID';
COMMENT ON COLUMN uc_oper_log.username    IS '操作人账号';
COMMENT ON COLUMN uc_oper_log.module      IS '模块：用户管理 / 应用管理 / 授权管理 / 设备管理 / 登录安全 / 规则配置 / 账号安全';
COMMENT ON COLUMN uc_oper_log.action      IS '动作：新增用户 / 修改应用 / 批量授权 ...';
COMMENT ON COLUMN uc_oper_log.method      IS '请求方法（HTTP 方法 + 路径）';
COMMENT ON COLUMN uc_oper_log.params      IS '请求参数摘要（JSON，密码等敏感字段已脱敏）';
COMMENT ON COLUMN uc_oper_log.before_data IS '修改前数据快照（JSON，更新/删除时记录；敏感字段已脱敏）';
COMMENT ON COLUMN uc_oper_log.after_data  IS '修改后数据快照（JSON，新增/更新时记录；新增含生成的主键）';
COMMENT ON COLUMN uc_oper_log.result      IS '结果：success=成功 / fail=失败';
COMMENT ON COLUMN uc_oper_log.msg         IS '说明信息（失败时为异常信息）';
COMMENT ON COLUMN uc_oper_log.cost_time   IS '耗时（毫秒）';
COMMENT ON COLUMN uc_oper_log.create_time IS '发生时间（分区键）';

-- ------------------------- 2.3 用户设备表（设备锁 / 设备指纹） -------------------------
CREATE TABLE IF NOT EXISTS uc_user_device (
    id               VARCHAR(32)       NOT NULL,
    user_id          VARCHAR(32)       NOT NULL,
    device_id        VARCHAR(128) NOT NULL,
    user_agent       VARCHAR(512),
    os               VARCHAR(64),
    browser          VARCHAR(64),
    last_login_ip    VARCHAR(64),
    first_login_time TIMESTAMP,
    last_login_time  TIMESTAMP,
    deleted          INT          DEFAULT 0 NOT NULL,
    create_by        VARCHAR(32),
    create_time      TIMESTAMP,
    update_by        VARCHAR(32),
    update_time      TIMESTAMP,
    delete_by        VARCHAR(32),
    delete_time      TIMESTAMP,
    CONSTRAINT pk_uc_user_device PRIMARY KEY (id)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_uc_user_device ON uc_user_device (user_id, device_id) WHERE deleted = 0;

COMMENT ON TABLE  uc_user_device           IS '用户中心-用户设备表（设备锁/设备指纹，记录账号已绑定设备）';
COMMENT ON COLUMN uc_user_device.device_id IS '设备指纹（客户端按浏览器环境生成）';

-- ------------------------- 2. 应用（SSO Client）表 -------------------------
CREATE TABLE IF NOT EXISTS uc_client (
    id          VARCHAR(32)       NOT NULL,
    client_id   VARCHAR(64)  NOT NULL,
    client_name VARCHAR(128) NOT NULL,
    allow_url   VARCHAR(512) DEFAULT '',
    entry_url   VARCHAR(255),
    secret_key  VARCHAR(128),
    is_push     SMALLINT     DEFAULT 0,
    push_url    VARCHAR(255) DEFAULT '/sso/pushC',
    is_slo      SMALLINT     DEFAULT 1,
    status      SMALLINT     DEFAULT 1,
    remark      VARCHAR(255),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted     INT          DEFAULT 0 NOT NULL,
    create_by   VARCHAR(32),
    update_by   VARCHAR(32),
    delete_by   VARCHAR(32),
    delete_time TIMESTAMP,
    CONSTRAINT pk_uc_client PRIMARY KEY (id)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_uc_client_client_id ON uc_client (client_id) WHERE deleted = 0;

COMMENT ON TABLE  uc_client             IS '用户中心-接入应用（SSO Client）表';
COMMENT ON COLUMN uc_client.id          IS '主键';
COMMENT ON COLUMN uc_client.client_id   IS '应用标识（SSO client 参数）';
COMMENT ON COLUMN uc_client.client_name IS '应用名称';
COMMENT ON COLUMN uc_client.allow_url   IS '允许的授权回调地址，多个用逗号分隔，* 表示全部放行';
COMMENT ON COLUMN uc_client.entry_url   IS '应用入口地址（工作台点击进入应用时跳转的目标，通常为应用的 /sso/login?back=首页）';
COMMENT ON COLUMN uc_client.secret_key  IS '接口调用秘钥（SSO 参数签名/推送校验使用）';
COMMENT ON COLUMN uc_client.is_push     IS '是否接收消息推送：0=否 / 1=是';
COMMENT ON COLUMN uc_client.push_url    IS '消息推送地址（is_push=1 时生效）';
COMMENT ON COLUMN uc_client.is_slo      IS '是否参与单点注销：0=否 / 1=是';
COMMENT ON COLUMN uc_client.status      IS '状态：0=禁用 / 1=启用';
COMMENT ON COLUMN uc_client.remark      IS '备注';
COMMENT ON COLUMN uc_client.create_time IS '创建时间';
COMMENT ON COLUMN uc_client.update_time IS '更新时间';

-- ------------------------- 3. 用户-应用授权绑定表 -------------------------
CREATE TABLE IF NOT EXISTS uc_user_client (
    id          VARCHAR(32)      NOT NULL,
    user_id     VARCHAR(32)      NOT NULL,
    client_id   VARCHAR(64) NOT NULL,
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted     INT          DEFAULT 0 NOT NULL,
    create_by   VARCHAR(32),
    update_by   VARCHAR(32),
    delete_by   VARCHAR(32),
    delete_time TIMESTAMP,
    CONSTRAINT pk_uc_user_client PRIMARY KEY (id)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_uc_user_client ON uc_user_client (user_id, client_id) WHERE deleted = 0;

COMMENT ON TABLE  uc_user_client             IS '用户中心-用户与应用授权绑定表（普通用户可访问的应用）';
COMMENT ON COLUMN uc_user_client.id          IS '主键';
COMMENT ON COLUMN uc_user_client.user_id     IS '用户 ID（uc_user.id）';
COMMENT ON COLUMN uc_user_client.client_id   IS '应用标识（uc_client.client_id）';
COMMENT ON COLUMN uc_user_client.create_time IS '授权时间';

-- ------------------------- 4. 规则配置表 -------------------------
-- 键值对存储平台规则（密码规则 / 登录规则 / 日志规则），仅系统管理员可维护；
-- 配置项默认值由后端启动时自动写入（见 config/SystemConfig 与 UcConfigService），
-- 后续版本新增配置项时无需执行 SQL，启动即自动补齐。
CREATE TABLE IF NOT EXISTS uc_config (
    id            VARCHAR(32)       NOT NULL,
    config_key    VARCHAR(64)  NOT NULL,
    config_value  VARCHAR(512),
    config_name   VARCHAR(128),
    config_group  VARCHAR(64),
    value_type    VARCHAR(16)  DEFAULT 'string',
    default_value VARCHAR(512),
    remark        VARCHAR(255),
    create_by     VARCHAR(32),
    create_time   TIMESTAMP,
    update_by     VARCHAR(32),
    update_time   TIMESTAMP,
    deleted       INT          DEFAULT 0 NOT NULL,
    delete_by     VARCHAR(32),
    delete_time   TIMESTAMP,
    CONSTRAINT pk_uc_config PRIMARY KEY (id)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_uc_config_key ON uc_config (config_key) WHERE deleted = 0;

COMMENT ON TABLE  uc_config               IS '用户中心-规则配置表（键值对，仅系统管理员可维护；默认值由后端启动时补齐）';
COMMENT ON COLUMN uc_config.config_key    IS '配置键（如 password.min-length）';
COMMENT ON COLUMN uc_config.config_value  IS '配置值（统一按字符串存储，读取时按 value_type 转换）';
COMMENT ON COLUMN uc_config.config_name   IS '配置名称（界面展示）';
COMMENT ON COLUMN uc_config.config_group  IS '配置分组：密码规则 / 登录规则 / 日志规则';
COMMENT ON COLUMN uc_config.value_type    IS '值类型：string / int / bool / option';
COMMENT ON COLUMN uc_config.default_value IS '默认值（执行「恢复默认」时写回）';
COMMENT ON COLUMN uc_config.remark        IS '说明（界面展示）';
COMMENT ON COLUMN uc_config.update_time   IS '最后修改时间';

-- ------------------------- 5. 初始化数据 -------------------------
-- 用户：密码均为 123456（MD5: e10adc3949ba59abbe56e057f20f883e）
INSERT INTO uc_user (id, username, password, nickname, email, phone, role, status, remark, create_time, update_time) VALUES
  ('10001', 'admin',    'e10adc3949ba59abbe56e057f20f883e', '超级管理员', 'admin@bdsoft.com',  '13800000001', 'admin', 1, '内置管理员账号（系统账号，不参与用户管理）', now(), now()),
  ('10002', 'zhangsan', 'e10adc3949ba59abbe56e057f20f883e', '张三',       'zhangsan@bdsoft.com', '13800000002', 'user',  1, '演示用户（已授权：客户端1、客户端2）', now(), now()),
  ('10003', 'lisi',     'e10adc3949ba59abbe56e057f20f883e', '李四',       'lisi@bdsoft.com',     '13800000003', 'user',  0, '演示用户（已禁用；已授权：客户端3）', now(), now());

-- 应用：对应四种对接模式的客户端 Demo（client1~client4，client5 为模式三第二实例）
-- 约定：认证中心 localhost:9000；client1 同域（localhost）；client2/3/4/5 跨域（127.0.0.1）
INSERT INTO uc_client (id, client_id, client_name, allow_url, entry_url, secret_key, is_push, push_url, is_slo, status, remark, create_time, update_time) VALUES
  ('20001', 'sso-client1', '单点登录客户端1', '*', 'http://localhost:5174/login', 'SSO-C1-kQwIOrYvnXmSDkwEiFngrKidMcdrgKor', 0, '/sso/pushC', 1, 1, '模式一：同域（localhost）+ 同 Redis（共享会话）', now(), now()),
  ('20002', 'sso-client2', '单点登录客户端2', '*', 'http://127.0.0.1:5175/login', 'SSO-C2-kQwIOrYvnXmSDkwEiFngrKidMcdrgKor', 0, '/sso/pushC', 1, 1, '模式二：跨域（127.0.0.1）+ 同 Redis（ticket 直连 Redis 校验）', now(), now()),
  ('20003', 'sso-client3', '单点登录客户端3', '*', 'http://127.0.0.1:5176/login', 'SSO-C3-kQwIOrYvnXmSDkwEiFngrKidMcdrgKor', 1, 'http://127.0.0.1:9003/sso/pushC', 1, 1, '模式三：跨域 + 独立 Redis（HTTP 接口校验 ticket）', now(), now()),
  ('20004', 'sso-client4', '单点登录客户端4（NoSdk）', '*', 'http://127.0.0.1:5177/login', 'SSO-C4-kQwIOrYvnXmSDkwEiFngrKidMcdrgKor', 1, 'http://127.0.0.1:9004/sso/pushC', 1, 1, 'NoSdk 对接示例（手动实现 SSO 协议与签名，不依赖 Pkuse-Token 客户端 SDK）', now(), now()),
  ('20005', 'sso-client5', '单点登录客户端5', '*', 'http://127.0.0.1:5178/login', 'SSO-C5-kQwIOrYvnXmSDkwEiFngrKidMcdrgKor', 1, 'http://127.0.0.1:9005/sso/pushC', 1, 1, '模式三第二实例：跨域 + 独立 Redis db7（HTTP 接口校验 ticket）', now(), now());

-- 用户-应用授权绑定（admin 为系统管理员，可访问全部应用，无需绑定）
INSERT INTO uc_user_client (id, user_id, client_id, create_time) VALUES
  ('30001', '10002', 'sso-client1', now()),
  ('30002', '10002', 'sso-client2', now()),
  ('30003', '10003', 'sso-client3', now());
