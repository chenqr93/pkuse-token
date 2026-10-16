-- =====================================================================
--  用户中心 - 升级脚本：国密密码存储（对应 init.sql v5）
--  内容：
--    1. 用户表增加 password_salt（随机盐，密码摘要改为 SM3(salt:明文)）
--    2. 历史数据兼容：盐为空的账号沿用 MD5 校验，首次登录成功后自动升级为 SM3
--  说明：SM2（传输加密）密钥为服务启动时生成，无需落库
-- =====================================================================

ALTER TABLE uc_user ADD COLUMN password_salt VARCHAR(64);

COMMENT ON COLUMN uc_user.password_salt IS '密码盐（国密 SM3 摘要使用；历史 MD5 数据此列为空，登录成功后自动升级）';
COMMENT ON COLUMN uc_user.password IS '密码摘要：SM3(password_salt + ":" + 明文)（历史数据为 MD5(明文)）';
