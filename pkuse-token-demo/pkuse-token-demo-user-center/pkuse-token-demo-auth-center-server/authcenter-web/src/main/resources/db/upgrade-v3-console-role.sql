-- ============================================================
--  授权中心 - 升级脚本 v3：控制台角色自管理（认证/授权职责拆分）
-- ============================================================
--  背景：用户信息通道（SSO userinfo 消息 / /api/auth/me）不再返回角色，
--  授权中心控制台自身的管理端角色（/api/authz/** 拦截器 checkRole("admin")）
--  改为本地 RBAC 查询：控制台自身作为一个 client（client_id=auth-center，
--  与 sa-token.sso-client.client 一致）查 ac_user_role + ac_role。
--
--  本脚本为控制台种子 admin 角色并绑定用户中心平台管理员（10001）。
--  注意：存量部署必须执行本脚本，否则平台管理员登录控制台后无角色（管理接口 403）。

-- 控制台自身的管理员角色
INSERT INTO ac_role (id, client_id, role_code, role_name, status, remark, create_time, create_by) VALUES
  ('r9001', 'auth-center', 'admin', '控制台管理员', 0, '授权中心控制台自身管理角色（本地 RBAC，种子数据）', now(), '10001');

-- 平台管理员（用户中心内置账号 10001）绑定控制台管理员角色
INSERT INTO ac_user_role (id, user_id, role_id, create_time, create_by) VALUES
  ('u9001', '10001', 'r9001', now(), '10001');
