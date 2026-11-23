-- =====================================================================
--  用户中心 - 升级脚本：规则配置模块（对应 init.sql v7）
--  内容：
--    1. 系统配置表 uc_config（键值 + 分组/名称/类型/默认值，仅系统管理员可维护）
--    2. 用户表增加 must_change_password（初始密码/重置密码后需修改密码标记）
--  说明：
--    - 配置项的默认值由后端启动时自动写入（见 config/SystemConfig 与 UcConfigService），
--      这样后续版本新增配置项时无需再执行 SQL，启动即自动补齐；
--    - 分区频率（log.login.partition / log.oper.partition）变更后，
--      后端会立即按新频率续建分区（区间连续，不产生空档）。
-- =====================================================================

-- ------------------------- 1. 系统配置表 -------------------------
CREATE TABLE IF NOT EXISTS uc_config (
    id            BIGINT       NOT NULL,
    config_key    VARCHAR(64)  NOT NULL,
    config_value  VARCHAR(512),
    config_name   VARCHAR(128),
    config_group  VARCHAR(64),
    value_type    VARCHAR(16)  DEFAULT 'string',
    default_value VARCHAR(512),
    remark        VARCHAR(255),
    update_time   TIMESTAMP,
    CONSTRAINT pk_uc_config PRIMARY KEY (id)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_uc_config_key ON uc_config (config_key);

COMMENT ON TABLE  uc_config               IS '用户中心-规则配置表（键值对，仅系统管理员可维护；默认值由后端启动时补齐）';
COMMENT ON COLUMN uc_config.config_key    IS '配置键（如 password.min-length）';
COMMENT ON COLUMN uc_config.config_value  IS '配置值（统一按字符串存储，读取时按 value_type 转换）';
COMMENT ON COLUMN uc_config.config_name   IS '配置名称（界面展示）';
COMMENT ON COLUMN uc_config.config_group  IS '配置分组：密码规则 / 登录规则 / 日志规则';
COMMENT ON COLUMN uc_config.value_type    IS '值类型：string / int / bool / option';
COMMENT ON COLUMN uc_config.default_value IS '默认值（执行「恢复默认」时写回）';
COMMENT ON COLUMN uc_config.remark        IS '说明（界面展示）';
COMMENT ON COLUMN uc_config.update_time   IS '最后修改时间';

-- ------------------------- 2. 用户表：首次登录改密标记 -------------------------
ALTER TABLE uc_user ADD COLUMN must_change_password SMALLINT DEFAULT 0;

COMMENT ON COLUMN uc_user.must_change_password IS '是否需要修改密码：1=下次登录需强制修改（初始密码/管理员重置后置位，修改成功后清零）';
