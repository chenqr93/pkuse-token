-- =====================================================================
--  用户中心 - 升级脚本：注册四种对接模式的演示客户端（对应 init.sql v3）
--  适用：已按 init.sql / upgrade-v2 部署过的数据库
--
--  域与端口约定（本地用 localhost / 127.0.0.1 模拟两个域）：
--    认证中心：localhost:9000        （前端 localhost:5173）
--    client1：localhost:9001         （同域，模式一）      前端 localhost:5174
--    client2：127.0.0.1:9002         （跨域，模式二）      前端 127.0.0.1:5175
--    client3：127.0.0.1:9003         （跨域，模式三）      前端 127.0.0.1:5176
--    client4：127.0.0.1:9004         （跨域，NoSdk）       前端 127.0.0.1:5177
-- =====================================================================

-- 1. 三个已有客户端：更新应用入口地址（工作台「进入应用」的目标地址）
UPDATE uc_client SET entry_url = 'http://localhost:5174/login'   WHERE client_id = 'sso-client1';
UPDATE uc_client SET entry_url = 'http://127.0.0.1:5175/login'   WHERE client_id = 'sso-client2';
UPDATE uc_client SET entry_url = 'http://127.0.0.1:5176/login'   WHERE client_id = 'sso-client3';

-- 2. 客户端3（模式三：跨域跨 Redis）：通过消息推送接收单点注销通知
UPDATE uc_client SET is_push = 1, push_url = 'http://127.0.0.1:9003/sso/pushC' WHERE client_id = 'sso-client3';

-- 3. 新增客户端4（NoSdk：不依赖客户端 SDK，手动实现协议）
INSERT INTO uc_client (id, client_id, client_name, allow_url, entry_url, secret_key, is_push, push_url, is_slo, status, remark, create_time, update_time)
VALUES (20004, 'sso-client4', '单点登录客户端4（NoSdk）', '*', 'http://127.0.0.1:5177/login',
        'SSO-C4-kQwIOrYvnXmSDkwEiFngrKidMcdrgKor', 1, 'http://127.0.0.1:9004/sso/pushC', 1, 1,
        'NoSdk 对接示例（手动实现 SSO 协议与签名，不依赖 Pkuse-Token 客户端 SDK）', now(), now());

-- 4. 更新备注（说明各客户端采用的对接模式）
UPDATE uc_client SET remark = '模式一：同域（localhost）+ 同 Redis（共享会话）' WHERE client_id = 'sso-client1';
UPDATE uc_client SET remark = '模式二：跨域（127.0.0.1）+ 同 Redis（ticket 直连 Redis 校验）' WHERE client_id = 'sso-client2';
UPDATE uc_client SET remark = '模式三：跨域 + 独立 Redis（HTTP 接口校验 ticket）' WHERE client_id = 'sso-client3';
