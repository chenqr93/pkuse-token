-- =====================================================================
--  用户中心 - 升级脚本：注册客户端5（模式三：跨域 + 跨 Redis，client3 的第二实例）
--  适用：已按 init.sql / upgrade-v2 ~ upgrade-v8 部署过的数据库
--
--  域与端口约定（本地用 localhost / 127.0.0.1 模拟两个域）：
--    认证中心：localhost:9000        （前端 localhost:5173）
--    client5：127.0.0.1:9005         （跨域，模式三）      前端 127.0.0.1:5178
--    client5 使用独立 Redis 库 db7（认证中心 db5、client3 db6），ticket 通过 HTTP 接口校验
-- =====================================================================

-- 新增客户端5（模式三：跨域跨 Redis）：通过消息推送接收单点注销通知
INSERT INTO uc_client (id, client_id, client_name, allow_url, entry_url, secret_key, is_push, push_url, is_slo, status, remark, create_time, update_time)
VALUES ('20005', 'sso-client5', '单点登录客户端5', '*', 'http://127.0.0.1:5178/login',
        'SSO-C5-kQwIOrYvnXmSDkwEiFngrKidMcdrgKor', 1, 'http://127.0.0.1:9005/sso/pushC', 1, 1,
        '模式三第二实例：跨域 + 独立 Redis db7（HTTP 接口校验 ticket）', now(), now());

-- 普通用户演示授权（可选）：给 zhangsan 开通客户端5，管理员 admin 无需绑定即可进入全部应用
-- INSERT INTO uc_user_client (id, user_id, client_id, create_time) VALUES ('30005', '10002', 'sso-client5', now());
