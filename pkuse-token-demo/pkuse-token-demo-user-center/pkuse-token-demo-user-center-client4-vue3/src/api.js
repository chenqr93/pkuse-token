import config from './config.js'

/**
 * 本应用 SSO 接口封装
 *
 * <p> token 存于 localStorage，请求时放入 satoken 请求头； </p>
 * <p> 模式一（同域）场景下会同时携带 Cookie（credentials: include），会话由共享 Cookie + 共享 Redis 决定。 </p>
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

async function request(path) {
  const headers = {}
  const token = getToken()
  if (token) {
    headers['satoken'] = token
  }
  const response = await fetch(config.apiBase + path, {
    method: 'GET',
    headers,
    credentials: 'include'
  })
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
