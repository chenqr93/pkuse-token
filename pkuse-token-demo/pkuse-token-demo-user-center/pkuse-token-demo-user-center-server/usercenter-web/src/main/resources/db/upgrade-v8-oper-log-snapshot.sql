-- =====================================================================
--  用户中心 - 升级脚本：操作日志记录变更前后数据（对应 init.sql v8）
--  内容：操作日志表 uc_oper_log 增加 before_data（修改前数据）/ after_data（修改后数据）
--  说明：
--    - 新增数据：记录 传参（params）+ 修改后数据（after_data，含生成的 ID）
--    - 更新数据：记录 传参 + 修改前数据（before_data）+ 修改后数据（after_data）
--    - 删除数据：记录 传参 + 修改前数据（删除前的完整记录）
--    - 列加在分区父表上，自动传播到已有/新建的月分区；敏感字段（密码等）脱敏后存储
-- =====================================================================

ALTER TABLE uc_oper_log ADD COLUMN before_data VARCHAR(2000);
ALTER TABLE uc_oper_log ADD COLUMN after_data  VARCHAR(2000);

COMMENT ON COLUMN uc_oper_log.before_data IS '修改前数据快照（JSON，更新/删除时记录；敏感字段已脱敏）';
COMMENT ON COLUMN uc_oper_log.after_data  IS '修改后数据快照（JSON，新增/更新时记录；新增含生成的主键）';
