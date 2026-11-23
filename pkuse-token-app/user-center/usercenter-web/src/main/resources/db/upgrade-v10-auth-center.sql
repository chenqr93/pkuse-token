-- =====================================================================
--  用户中心 - 升级脚本：注册授权中心（auth-center）应用 + 开放注册表接口配套
--  适用：已按 init.sql / upgrade-v2 ~ upgrade-v9 部署过的数据库
--
--  授权中心（RBAC 权限管理中心）作为 SSO Client 接入用户中心：
--    后端 localhost:9010        前端 localhost:5180
--    默认 ticket 模式（跨域 + 独立会话 Redis db8 亦可部署；同域时共享 Cookie 需切 simple 模式）
--    凭证 client=auth-center 同时用于 /open/registry/* 开放接口的签名调用
-- =====================================================================

-- 注册授权中心应用（is_push=1：ticket 模式下接收认证中心 logoutCall 单点注销推送；simple 模式下推送无副作用）
INSERT INTO uc_client (id, client_id, client_name, allow_url, entry_url, secret_key, is_push, push_url, is_slo, status, remark, create_time, update_time)
VALUES ('20006', 'auth-center', '授权中心', '*', 'http://localhost:5180/login',
        'SSO-AC-kQwIOrYvnXmSDkwEiFngrKidMcdrgKor', 1, 'http://localhost:9010/sso/pushC', 1, 1,
        '授权中心（RBAC：应用菜单/角色/用户授权），默认 ticket 模式接入', now(), now());

-- 授权中心管理控制台仅系统管理员使用；如需给普通用户开通入口（不推荐），可绑定：
-- INSERT INTO uc_user_client (id, user_id, client_id, create_time) VALUES ('30006', '10002', 'auth-center', now());
