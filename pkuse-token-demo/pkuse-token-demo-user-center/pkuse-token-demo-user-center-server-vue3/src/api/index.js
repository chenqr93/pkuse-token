import request from '../utils/request'

/** 登录认证 */
export const authApi = {
  /** 账号密码登录（密码使用 SM2 公钥加密；需要滑块验证码时传 captchaId + captchaTrack） */
  login: (data) => request.post('/api/auth/login', data),
  /** 退出登录 */
  logout: () => request.post('/api/auth/logout'),
  /** 当前登录用户 */
  me: () => request.get('/api/auth/me'),
  /** 获取滑块验证码 */
  captcha: () => request.get('/api/auth/captcha'),
  /** 查询账号当前是否需要滑块验证码 */
  needCaptcha: (username) => request.get('/api/auth/need-captcha', { params: { username } }),
  /** 获取 SM2 公钥（国密，用于密码传输加密） */
  publicKey: () => request.get('/api/auth/public-key'),
  /** 新设备登录：发送短信验证码（参数为登录时返回的 verifyToken） */
  sendDeviceCode: (verifyToken) => request.post('/api/auth/device/send-code', { verifyToken }),
  /** 新设备登录：校验验证码并完成换绑登录 */
  verifyDevice: (verifyToken, code) => request.post('/api/auth/device/verify', { verifyToken, code }),
  /** 当前生效的密码规则（改密页面提示） */
  passwordRule: () => request.get('/api/auth/password-rule'),
  /** 修改当前登录用户的密码（原密码 + 新密码，SM2 密文） */
  changePassword: (data) => request.post('/api/auth/change-password', data)
}

/** 用户管理（仅系统管理员） */
export const userApi = {
  page: (params) => request.get('/api/user/page', { params }),
  detail: (id) => request.get(`/api/user/${id}`),
  create: (data) => request.post('/api/user', data),
  update: (data) => request.put('/api/user', data),
  remove: (id) => request.delete(`/api/user/${id}`),
  resetPassword: (id, password) =>
    request.put(`/api/user/${id}/password`, null, { params: { password } }),
  changeStatus: (id, status) =>
    request.put(`/api/user/${id}/status`, null, { params: { status } }),
  /** 查询用户已授权的应用标识 */
  listClients: (id) => request.get(`/api/user/${id}/clients`),
  /** 保存用户的应用授权（覆盖式，传全部已勾选的应用标识） */
  bindClients: (id, clientIds) => request.put(`/api/user/${id}/clients`, clientIds),
  /** 批量授权：{ userIds, clientIds, mode: append|cover } */
  batchBindClients: (data) => request.put('/api/user/clients/batch', data),
  /** 管理员在线解锁（清除密码错误锁定） */
  unlock: (id) => request.put(`/api/user/${id}/unlock`),
  /** 查询用户已绑定设备（设备锁 / 设备指纹） */
  listDevices: (id) => request.get(`/api/user/${id}/devices`),
  /** 解绑设备 */
  unbindDevice: (id, deviceId) =>
    request.delete(`/api/user/${id}/devices`, { params: { deviceId } })
}

/** 登录日志（仅系统管理员） */
export const loginLogApi = {
  page: (params) => request.get('/api/log/login/page', { params })
}

/** 操作日志（仅系统管理员） */
export const operLogApi = {
  page: (params) => request.get('/api/log/oper/page', { params })
}

/** 应用（Client）管理（仅系统管理员） */
export const clientApi = {
  page: (params) => request.get('/api/client/page', { params }),
  list: () => request.get('/api/client/list'),
  detail: (id) => request.get(`/api/client/${id}`),
  create: (data) => request.post('/api/client', data),
  update: (data) => request.put('/api/client', data),
  remove: (id) => request.delete(`/api/client/${id}`),
  changeStatus: (id, status) =>
    request.put(`/api/client/${id}/status`, null, { params: { status } })
}

/** 规则配置（仅系统管理员） */
export const configApi = {
  /** 配置项列表（按分组的 Map：密码规则 / 登录规则 / 日志规则） */
  list: () => request.get('/api/config/list'),
  /** 批量保存（{ values: { key: value } }） */
  save: (values) => request.put('/api/config', { values }),
  /** 恢复默认配置 */
  reset: () => request.post('/api/config/reset'),
  /** 当前密码规则（含系统默认密码，管理界面提示用） */
  passwordRule: () => request.get('/api/config/password-rule')
}

/** 工作台 */
export const dashboardApi = {
  /** 工作台总览（仅系统管理员） */
  overview: () => request.get('/api/dashboard/overview'),
  /** 我的应用入口（普通用户为已授权应用） */
  myApps: () => request.get('/api/dashboard/my-apps')
}
