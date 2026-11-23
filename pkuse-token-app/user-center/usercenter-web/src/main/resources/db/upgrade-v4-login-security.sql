-- =====================================================================
--  用户中心 - 升级脚本：登录安全能力（对应 init.sql v4）
--  内容：
--    1. 用户表增加「密码错误锁定」相关字段
--    2. 登录日志表（按月分表：KingbaseES 声明式分区，父表 uc_login_log + 月分区 uc_login_log_yyyymm）
--    3. 用户设备表（设备锁 / 设备指纹）
--  适用：已部署过旧版本的数据库；全新部署执行 init.sql 即可（本脚本内容已并入）
-- =====================================================================

-- ------------------------- 1. 用户表：锁定相关字段 -------------------------
ALTER TABLE uc_user ADD COLUMN login_fail_count INT DEFAULT 0;
ALTER TABLE uc_user ADD COLUMN locked_until TIMESTAMP;
ALTER TABLE uc_user ADD COLUMN lock_reason VARCHAR(255);

COMMENT ON COLUMN uc_user.login_fail_count IS '连续登录失败次数';
COMMENT ON COLUMN uc_user.locked_until     IS '锁定截止时间（为空表示未锁定）';
COMMENT ON COLUMN uc_user.lock_reason      IS '锁定原因（如：连续密码错误 5 次）';

-- ------------------------- 2. 登录日志表（按月分表） -------------------------
-- 父表：不存数据，仅作分区路由与查询入口；数据按 create_time 落到各月分区
CREATE TABLE IF NOT EXISTS uc_login_log (
    id          BIGINT       NOT NULL,
    username    VARCHAR(64),
    user_id     BIGINT,
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
COMMENT ON COLUMN uc_login_log.result      IS '结果：success=成功 / fail=密码错误 / locked=账号锁定 / captcha_fail=验证码错误 / device_lock=设备锁拦截 / disabled=账号禁用 / no_user=账号不存在';
COMMENT ON COLUMN uc_login_log.device_id   IS '设备指纹（客户端上报）';
COMMENT ON COLUMN uc_login_log.create_time IS '发生时间（分区键）';

-- ------------------------- 3. 用户设备表（设备锁 / 设备指纹） -------------------------
CREATE TABLE IF NOT EXISTS uc_user_device (
    id               BIGINT       NOT NULL,
    user_id          BIGINT       NOT NULL,
    device_id        VARCHAR(128) NOT NULL,
    user_agent       VARCHAR(512),
    os               VARCHAR(64),
    browser          VARCHAR(64),
    last_login_ip    VARCHAR(64),
    first_login_time TIMESTAMP,
    last_login_time  TIMESTAMP,
    CONSTRAINT pk_uc_user_device PRIMARY KEY (id)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_uc_user_device ON uc_user_device (user_id, device_id);

COMMENT ON TABLE  uc_user_device                  IS '用户中心-用户设备表（设备锁/设备指纹，记录账号已绑定的设备）';
COMMENT ON COLUMN uc_user_device.device_id        IS '设备指纹（客户端按浏览器环境生成）';
COMMENT ON COLUMN uc_user_device.first_login_time IS '首次登录时间（即设备绑定时间）';
COMMENT ON COLUMN uc_user_device.last_login_time  IS '最近登录时间';
