/**
 * 登录态工具：token / 当前用户信息 的本地存取
 *
 * 说明：
 *   1. 登录走用户中心统一登录页（SSO），本前端无登录表单；/login 为登录中转页；
 *   2. token（ticket 模式为授权中心本地会话 authcenter-token）存 localStorage，
 *      接口请求放入 authcenter-token 请求头（与后端 sa-token.token-name 一致）；
 *      simple 模式下后端沿用 satoken，此处经 /sso/isLogin 返回的 token 自动适配，
 *      请求头同时携带两个名字以保证两种模式都可用（未登录时为空，无副作用）；
 *   3. 用户信息（id/username/nickname）来自 /sso/isLogin 返回的 user（后端缓存），
 *      不含角色——控制台管理端角色经 /api/authz/my-roles（本地 RBAC）单独获取。
 */
const TOKEN_KEY = 'auth-center-token'
const USER_KEY = 'auth-center-user'
const SSO_MODE_KEY = 'auth-center-sso-mode'
const ROLES_KEY = 'auth-center-roles'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY) || ''
}

export function setToken(token) {
  localStorage.setItem(TOKEN_KEY, token)
}

export function getStoredUser() {
  try {
    return JSON.parse(localStorage.getItem(USER_KEY) || 'null')
  } catch (e) {
    return null
  }
}

export function setStoredUser(user) {
  localStorage.setItem(USER_KEY, JSON.stringify(user || null))
}

export function getSsoMode() {
  return localStorage.getItem(SSO_MODE_KEY) || ''
}

export function setSsoMode(mode) {
  localStorage.setItem(SSO_MODE_KEY, mode || '')
}

/** 当前登录用户的控制台角色（本地 RBAC：client=auth-center） */
export function getStoredRoles() {
  try {
    return JSON.parse(localStorage.getItem(ROLES_KEY) || '[]')
  } catch (e) {
    return []
  }
}

export function setStoredRoles(roles) {
  localStorage.setItem(ROLES_KEY, JSON.stringify(roles || []))
}

/** 当前登录用户是否控制台管理员（授权中心控制台仅 admin 可用） */
export function isAdmin() {
  return getStoredRoles().includes('admin')
}

/** 保存登录态（token + 用户信息 + SSO 模式 + 控制台角色） */
export function saveLoginState(token, user, ssoMode, roles) {
  setToken(token)
  setStoredUser(user)
  setSsoMode(ssoMode)
  setStoredRoles(roles)
}

/** 清除登录态 */
export function clearLoginState() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
  localStorage.removeItem(SSO_MODE_KEY)
  localStorage.removeItem(ROLES_KEY)
}
