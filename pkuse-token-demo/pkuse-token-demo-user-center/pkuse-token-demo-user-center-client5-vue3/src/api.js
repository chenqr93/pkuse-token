import config from './config.js'

/**
 * 本应用后端接口封装
 *
 * <p> token 存于 localStorage，请求时放入 satoken 请求头； </p>
 * <p> 跨域场景（前端 5178 / 后端 9005）同时携带 Cookie（credentials: include）。 </p>
 */
const TOKEN_KEY = `sso-client-token-${config.port}`

export function getToken() {
  return localStorage.getItem(TOKEN_KEY) || ''
}

export function setToken(token) {
  localStorage.setItem(TOKEN_KEY, token)
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY)
}

/**
 * 通用请求
 *
 * @param path    接口路径
 * @param options { method: 'GET'|'POST'|'PUT'|'DELETE', params: {k:v} }
 *                GET 时 params 拼查询串；其余方法以表单编码放入请求体（后端按普通参数绑定）
 */
async function request(path, options = {}) {
  const headers = {}
  const token = getToken()
  if (token) {
    headers['satoken'] = token
  }
  const method = options.method || 'GET'
  if (options.params && method === 'GET') {
    path += '?' + new URLSearchParams(options.params).toString()
  }
  const init = {
    method,
    headers,
    credentials: 'include'
  }
  if (options.params && method !== 'GET') {
    headers['Content-Type'] = 'application/x-www-form-urlencoded; charset=UTF-8'
    init.body = new URLSearchParams(options.params).toString()
  }
  const response = await fetch(config.apiBase + path, init)
  const text = await response.text()
  try {
    return JSON.parse(text)
  } catch (e) {
    return { code: 500, msg: text }
  }
}

export const ssoApi = {
  /** 当前是否登录 */
  isLogin: () => request('/sso/isLogin'),
  /** 认证中心授权地址（前端跳转过去完成统一登录） */
  getSsoAuthUrl: (clientLoginUrl) =>
    request(`/sso/getSsoAuthUrl?clientLoginUrl=${encodeURIComponent(clientLoginUrl)}`),
  /** 用 ticket 换取本应用 token */
  doLoginByTicket: (ticket) => request(`/sso/doLoginByTicket?ticket=${encodeURIComponent(ticket)}`),
  /** 账号资料（本应用后端 → 认证中心 userinfo 消息） */
  myInfo: () => request('/sso/myInfo'),
  /** 单应用注销（只退出本应用） */
  logoutByAlone: () => request('/sso/logoutByAlone'),
  /**
   * 单点注销地址（浏览器整页跳转，认证中心注销后回到 back）
   *
   * 整页跳转不携带自定义请求头，因此把 token 通过 satoken 参数带过去（后端从参数读取）。
   */
  logoutUrl: (back) => {
    const token = getToken()
    const suffix = token ? `&satoken=${encodeURIComponent(token)}` : ''
    return `${config.apiBase}/sso/logout?back=${encodeURIComponent(back)}${suffix}`
  }
}

/**
 * magic-api 开放接口调用方管理（需 pkuse-token 登录；变更即时生效）
 */
export const openClientApi = {
  /** 调用方列表（密钥脱敏） */
  list: () => request('/open/clients'),
  /** 新登记调用方（密钥自动生成，明文仅响应返回一次） */
  create: (data) => request('/open/clients', { method: 'POST', params: data }),
  /** 修改显示名 / 令牌有效期 / 备注 */
  update: (id, data) => request(`/open/clients/${id}`, { method: 'PUT', params: data }),
  /** 启用 / 禁用（0=禁用 / 1=启用；禁用时吊销其全部令牌） */
  updateStatus: (id, status) => request(`/open/clients/${id}/status`, { method: 'PUT', params: { status } }),
  /** 重置密钥（新密钥仅响应返回一次，旧令牌全部吊销） */
  resetSecret: (id) => request(`/open/clients/${id}/secret`, { method: 'PUT' }),
  /** 删除调用方（并吊销其全部令牌） */
  remove: (id) => request(`/open/clients/${id}`, { method: 'DELETE' })
}
