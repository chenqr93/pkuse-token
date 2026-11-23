-- =====================================================================
--  用户中心 - 升级脚本：用户与应用授权绑定（对应 init.sql v2）
--  适用：已按 init.sql 旧版本部署过 uc_user / uc_client 的数据库
--  说明：全新部署直接执行 init.sql 即可，无需本脚本
-- =====================================================================

-- 1. 应用表增加「应用入口地址」列（工作台点击进入应用时跳转的目标地址）
ALTER TABLE uc_client ADD COLUMN entry_url VARCHAR(255);
COMMENT ON COLUMN uc_client.entry_url IS '应用入口地址（工作台点击进入应用时跳转的目标，通常为应用的 /sso/login?back=首页）';

-- 2. 应用入口地址初始化（对应 client1/client2/client3 脚手架）
UPDATE uc_client SET entry_url = 'http://localhost:9001/sso/login?back=http://localhost:9001/' WHERE client_id = 'sso-client1';
UPDATE uc_client SET entry_url = 'http://localhost:9002/sso/login?back=http://localhost:9002/' WHERE client_id = 'sso-client2';
UPDATE uc_client SET entry_url = 'http://localhost:9003/sso/login?back=http://localhost:9003/' WHERE client_id = 'sso-client3';

-- 3. 用户-应用授权绑定表
CREATE TABLE IF NOT EXISTS uc_user_client (
    id          BIGINT      NOT NULL,
    user_id     BIGINT      NOT NULL,
    client_id   VARCHAR(64) NOT NULL,
    create_time TIMESTAMP,
    CONSTRAINT pk_uc_user_client PRIMARY KEY (id)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_uc_user_client ON uc_user_client (user_id, client_id);

COMMENT ON TABLE  uc_user_client             IS '用户中心-用户与应用授权绑定表（普通用户可访问的应用）';
COMMENT ON COLUMN uc_user_client.id          IS '主键';
COMMENT ON COLUMN uc_user_client.user_id     IS '用户 ID（uc_user.id）';
COMMENT ON COLUMN uc_user_client.client_id   IS '应用标识（uc_client.client_id）';
COMMENT ON COLUMN uc_user_client.create_time IS '授权时间';

-- 4. 演示授权数据（admin 为系统管理员，可访问全部应用，无需绑定）
INSERT INTO uc_user_client (id, user_id, client_id, create_time) VALUES
  (30001, 10002, 'sso-client1', now()),
  (30002, 10002, 'sso-client2', now()),
  (30003, 10003, 'sso-client3', now());

-- 5. 内置管理员说明更新
UPDATE uc_user SET remark = '内置管理员账号（系统账号，不参与用户管理）' WHERE id = 10001;
UPDATE uc_user SET remark = '演示用户（已授权：客户端1、客户端2）' WHERE id = 10002;
UPDATE uc_user SET remark = '演示用户（已禁用；已授权：客户端3）' WHERE id = 10003;
