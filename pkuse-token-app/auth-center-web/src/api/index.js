import request from '../utils/request'

/** SSO 登录态（登录中转页使用） */
export const ssoApi = {
  /** 登录态查询：data = { isLogin, loginId, token, ssoMode, user: {id,username,nickname} }（纯身份，不含角色） */
  isLogin: () => request.get('/sso/isLogin')
}

/** 应用（注册表实时 + 本地授权统计回填） */
export const appApi = {
  list: () => request.get('/api/authz/app/list'),
  refresh: () => request.post('/api/authz/app/refresh')
}

/** 概览与当前用户 */
export const overviewApi = {
  overview: () => request.get('/api/authz/overview'),
  me: () => request.get('/api/authz/me'),
  /** 当前登录用户的控制台角色（本地 RBAC：client=auth-center；用户信息与角色拆分后单独获取） */
  myRoles: () => request.get('/api/authz/my-roles')
}

/** 菜单管理 */
export const menuApi = {
  tree: (clientId) => request.get('/api/authz/menu/tree', { params: { clientId } }),
  create: (data) => request.post('/api/authz/menu', data),
  update: (data) => request.put('/api/authz/menu', data),
  remove: (id) => request.delete(`/api/authz/menu/${id}`),
  changeStatus: (id, status) =>
    request.put(`/api/authz/menu/${id}/status`, null, { params: { status } })
}

/** 角色管理 */
export const roleApi = {
  page: (params) => request.get('/api/authz/role/page', { params }),
  create: (data) => request.post('/api/authz/role', data),
  update: (data) => request.put('/api/authz/role', data),
  remove: (id) => request.delete(`/api/authz/role/${id}`),
  changeStatus: (id, status) =>
    request.put(`/api/authz/role/${id}/status`, null, { params: { status } }),
  menuIds: (id) => request.get(`/api/authz/role/${id}/menuIds`),
  bindMenus: (id, menuIds) => request.put(`/api/authz/role/${id}/menus`, menuIds)
}

/** 用户授权（用户档案实时取自用户中心） */
export const userGrantApi = {
  page: (params) => request.get('/api/authz/user/page', { params }),
  roles: (id) => request.get(`/api/authz/user/${id}/roles`),
  bindRoles: (id, roleIds) => request.put(`/api/authz/user/${id}/roles`, roleIds)
}

/** 操作日志 */
export const operLogApi = {
  page: (params) => request.get('/api/authz/log/oper/page', { params })
}

/** 孤儿数据清理（V2） */
export const cleanupApi = {
  scan: () => request.get('/api/authz/cleanup/scan'),
  execute: () => request.post('/api/authz/cleanup/execute')
}
