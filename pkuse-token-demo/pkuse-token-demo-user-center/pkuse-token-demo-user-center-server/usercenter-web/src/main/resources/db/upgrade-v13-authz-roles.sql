-- =====================================================================
--  用户中心 - 升级脚本：userinfo 应用角色（认证中心 ↔ 授权中心联动）
--  适用：已按 init.sql / upgrade-v2 ~ upgrade-v12 部署过的数据库
--
--  SSO userinfo 的 role 字段改为「该用户在发起方应用下授权中心分配的启用角色编码数组」
--  （未分配 / 授权中心不可用时为空数组，不再回退平台角色 admin/user）。认证中心以目标应用
--  自身在 uc_client 登记的密钥（client 即角色归属应用）签名调用 /open/authz/roles 获取；
--  本脚本注册的 user-center 行为认证中心服务身份（预留），非本链路必需。
-- =====================================================================

-- 注册用户中心（认证中心）服务身份：与 auth-center 行对称，仅用于服务间开放接口验签，
-- 非接入应用（不用于 SSO 登录）；授权中心验签失败会自动刷新注册表，插入后即时生效。
INSERT INTO uc_client (id, client_id, client_name, allow_url, entry_url, secret_key, is_push, push_url, is_slo, status, remark, create_time, update_time)
VALUES ('20007', 'user-center', '用户中心（认证中心）', '*', NULL, 'SSO-UC-kQwIOrYvnXmSDkwEiFngrKidMcdrgKor', 0, NULL, 0, 1,
        '认证中心服务身份（服务间开放接口调用预留）；userinfo 应用角色按目标应用自身身份签名调用，不使用本行', now(), now());
