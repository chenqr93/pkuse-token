-- =====================================================================
--  用户中心 - 升级脚本：身份证号国密（SM4）加密存储
--  内容：
--    1. uc_user.id_card 扩列 VARCHAR(128)：存放 SM4 密文（格式 SM4: + hex(IV + 密文)）
--    2. 新增 uc_user.id_card_hash（SM3 摘要，64 位十六进制）：身份证精确匹配（IN 查询）走本列
--    3. 存量明文数据无需 SQL 处理：应用启动时由 IdCardCryptoMigrator 自动加密迁移
--  说明：
--    SM4 密钥为部署级配置（user-center.crypto.id-card-sm4-key，见各环境 yml），
--    换密钥后历史密文将无法解密，请独立生成并妥善保管；
--    密文带随机 IV（同一身份证号每次加密结果不同），等值查询统一走 id_card_hash。
-- =====================================================================

ALTER TABLE uc_user ALTER COLUMN id_card TYPE VARCHAR(128);

ALTER TABLE uc_user ADD COLUMN IF NOT EXISTS id_card_hash VARCHAR(64);

CREATE INDEX IF NOT EXISTS idx_uc_user_id_card_hash ON uc_user (id_card_hash) WHERE id_card_hash IS NOT NULL;

COMMENT ON COLUMN uc_user.id_card IS '身份证号 SM4 密文（SM4: + hex(IV + 密文)，随机 IV 国密存储加密；存量明文由应用启动时自动迁移）';
COMMENT ON COLUMN uc_user.id_card_hash IS '身份证号 SM3 摘要（64 位十六进制，身份证精确匹配索引，不存明文）';
