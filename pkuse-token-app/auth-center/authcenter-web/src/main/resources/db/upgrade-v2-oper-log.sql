-- =====================================================================
--  授权中心 - 升级脚本：操作日志表 V2（快照字段 + 按月分区）
--  适用：已按 V1 版 init.sql 部署过的数据库（ac_oper_log 为普通表）
--  内容：重命名旧表 → 创建分区父表 → 迁移存量数据（按 create_time 落入对应分区，
--        分区由后端启动时自动创建；执行本脚本后需重启一次授权中心）
-- =====================================================================

-- 1. 旧表重命名（保留存量数据）
ALTER TABLE ac_oper_log RENAME TO ac_oper_log_v1_backup;

-- 2. 创建分区父表（含快照字段）
CREATE TABLE ac_oper_log (
    id             VARCHAR(36)   NOT NULL,
    oper_user_id   VARCHAR(36),
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

COMMENT ON TABLE  ac_oper_log IS '授权中心-操作日志表（按月分表，V2 起含修改前/后数据快照）';
COMMENT ON COLUMN ac_oper_log.before_data IS '修改前数据快照（JSON）';
COMMENT ON COLUMN ac_oper_log.after_data  IS '修改后数据快照（JSON）';

-- 3. 存量数据迁移（create_time 为空的旧数据补当前时间；分区由后端启动时自动补齐后，
--    本脚本仅在分区存在后由后端重启完成，未落分区的数据保留在备份表）
-- 说明：KingbaseES 分区表要求写入时目标分区已存在，故迁移放在后端首次启动
--      （AcLogPartitionMaintainer 已创建分区）后手工执行下方语句：
-- INSERT INTO ac_oper_log (id, oper_user_id, oper_user_name, module, action, method, params, success, message, cost_ms, ip, ua, create_time)
-- SELECT id, oper_user_id, oper_user_name, module, action, method, params, success, message, cost_ms, ip, ua,
--        COALESCE(create_time, now()) FROM ac_oper_log_v1_backup;
-- DROP TABLE ac_oper_log_v1_backup;
