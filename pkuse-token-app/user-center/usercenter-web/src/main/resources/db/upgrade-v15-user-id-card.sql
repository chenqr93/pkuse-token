-- =====================================================================
--  用户中心 - 升级脚本：uc_user 增加身份证号
--  用途：业务应用（案卷评查）人员导入按身份证匹配用户
--  适用：已部署的用户中心库；新库直接执行 init.sql 即可
-- =====================================================================

ALTER TABLE uc_user ADD COLUMN IF NOT EXISTS id_card VARCHAR(32);
COMMENT ON COLUMN uc_user.id_card IS '身份证号（业务应用按身份证匹配人员）';

-- 可选：为历史用户补录身份证号（按实际数据维护）
-- UPDATE uc_user SET id_card = '130xxxxxxxxxxxxxxx' WHERE username = 'xxx';
