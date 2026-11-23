/**
 * 登录态工具：token / 当前用户信息 的本地存取
 *
 * 说明：
 *   1. token 存入 localStorage，接口请求时放入 satoken 请求头；
 *   2. 同时把 token 同步写入 Cookie，目的是浏览器整页跳转到认证中心
 *      /sso/auth 时自动携带（Pkuse-Token 默认从 Cookie 读取 token）；
 *      开发环境下前端与后端同为 localhost（Cookie 不区分端口），因此可行；
 *      生产环境推荐前后端同域部署（Nginx 反向代理），Cookie 天然共享。
 */
const TOKEN_KEY = 'user-center-token'
const USER_KEY = 'user-center-user'
const ROLES_KEY = 'user-center-roles'

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

/** 当前登录用户的平台角色（用户信息与角色拆分：角色经 /api/auth/my-roles 单独获取） */
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

/** 当前登录用户是否系统管理员（admin 可维护用户与应用） */
export function isAdmin() {
  return getStoredRoles().includes('admin')
}

/** 保存登录态（token + 用户信息 + 平台角色 + Cookie） */
export function saveLoginState(token, user, roles) {
  setToken(token)
  setStoredUser(user)
  setStoredRoles(roles)
  syncTokenCookie(token)
}

/** 清除登录态 */
export function clearLoginState() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
  localStorage.removeItem(ROLES_KEY)
  // 过期 cookie（后端退出登录时也会清理）
  document.cookie = 'satoken=; path=/; max-age=0'
}

/** 把 token 同步写入 Cookie，供浏览器整页跳转认证中心时携带 */
export function syncTokenCookie(token) {
  if (token) {
    document.cookie = `satoken=${token}; path=/; max-age=2592000`
  }
}
