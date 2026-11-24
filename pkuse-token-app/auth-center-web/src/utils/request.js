import axios from 'axios'
import { message } from 'ant-design-vue'
import router from '../router'
import { getToken, clearLoginState } from './auth'

/**
 * axios 封装
 *
 * <pre>
 *   后端统一返回结构： { code, msg, data }
 *     code = 200  成功（拦截器直接返回 res，业务代码取 res.data）
 *     code = 401  未登录（清除登录态并跳登录中转页，由 SSO 重新登录）
 *     code = 403  无权限（控制台仅系统管理员可用）
 * </pre>
 */
const request = axios.create({
  baseURL: '/authcenter',
  timeout: 15000
})

// 请求拦截：自动携带 token（两种 SSO 模式的请求头名都带上，未登录时为空）
request.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers['authcenter-token'] = token
    config.headers['satoken'] = token
  }
  return config
})

// 响应拦截：统一处理 code
request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res && res.code === 200) {
      return res
    }
    if (res && res.code === 401) {
      clearLoginState()
      const current = router.currentRoute
      if (current.path !== '/login') {
        router.replace({ path: '/login', query: { redirect: current.fullPath } })
      }
      return Promise.reject(withCode(new Error(res.msg || '未登录'), res.code))
    }
    const msg = (res && res.msg) || '请求失败'
    message.error(msg)
    return Promise.reject(withCode(new Error(msg), res && res.code, res && res.data))
  },
  (error) => {
    message.error('网络异常：' + (error.message || ''))
    return Promise.reject(error)
  }
)

/** 把后端的业务状态码与附加数据挂到 Error 上，便于调用方区分处理 */
function withCode(error, code, data) {
  error.code = code
  error.data = data
  return error
}

export default request
