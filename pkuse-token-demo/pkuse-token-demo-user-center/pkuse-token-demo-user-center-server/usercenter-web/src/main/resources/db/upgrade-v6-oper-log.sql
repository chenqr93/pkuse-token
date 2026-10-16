-- =====================================================================
--  用户中心 - 升级脚本：操作日志（对应 init.sql v6）
--  内容：操作日志表 uc_oper_log（按月分表，与登录日志一致）
--        记录管理动作：操作人、模块、动作、请求、参数摘要、结果、耗时、IP/UA 等
--  说明：月分表由后端启动时/每日定时自动创建（与 uc_login_log 相同机制）
-- =====================================================================

CREATE TABLE IF NOT EXISTS uc_oper_log (
    id          BIGINT       NOT NULL,
    user_id     BIGINT,
    username    VARCHAR(64),
    module      VARCHAR(64),
    action      VARCHAR(64),
    method      VARCHAR(255),
    params      VARCHAR(2000),
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
COMMENT ON COLUMN uc_oper_log.module      IS '模块：用户管理 / 应用管理 / 授权 / 设备 / 登录安全';
COMMENT ON COLUMN uc_oper_log.action      IS '动作：新增用户 / 修改应用 / 批量授权 ...';
COMMENT ON COLUMN uc_oper_log.method      IS '请求方法（HTTP 方法 + 路径）';
COMMENT ON COLUMN uc_oper_log.params      IS '请求参数摘要（JSON，密码等敏感字段已脱敏）';
COMMENT ON COLUMN uc_oper_log.result      IS '结果：success=成功 / fail=失败';
COMMENT ON COLUMN uc_oper_log.msg         IS '说明信息（失败时为异常信息）';
COMMENT ON COLUMN uc_oper_log.cost_time   IS '耗时（毫秒）';
COMMENT ON COLUMN uc_oper_log.create_time IS '发生时间（分区键）';
