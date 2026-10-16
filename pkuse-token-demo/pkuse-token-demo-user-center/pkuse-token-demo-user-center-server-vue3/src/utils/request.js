import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'
import { getToken, clearLoginState } from './auth'

/**
 * axios 封装
 *
 * <pre>
 *   后端统一返回结构： { code, msg, data }
 *     code = 200  成功（拦截器直接返回 res，业务代码取 res.data）
 *     code = 401  未登录（清除登录态并跳转统一登录页）
 *     其他        业务失败（弹出 msg 提示）
 * </pre>
 */
const request = axios.create({
  baseURL: '/',
  timeout: 15000
})

// 请求拦截：自动携带 token
request.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
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
      ElMessage.warning(res.msg || '登录已过期，请重新登录')
      const current = router.currentRoute.value
      if (current.path !== '/login') {
        router.replace({ path: '/login', query: { redirect: current.fullPath } })
      }
      return Promise.reject(withCode(new Error(res.msg || '未登录'), res.code))
    }
    // 登录安全相关状态码由调用方（登录页）自行处理提示：421=新设备待验证 / 423=账号锁定 / 428=需要验证码
    const silentCodes = [421, 423, 428]
    if (res && silentCodes.includes(res.code)) {
      return Promise.reject(withCode(new Error(res.msg || '登录失败'), res.code, res.data))
    }
    // 426：需先修改初始密码（规则配置 - 首次登录强制修改密码）→ 引导到修改密码页
    if (res && res.code === 426) {
      ElMessage.warning(res.msg || '请先修改初始密码')
      const current = router.currentRoute.value
      if (current.path !== '/password') {
        router.replace({ path: '/password', query: { force: '1' } })
      }
      return Promise.reject(withCode(new Error(res.msg || '请先修改初始密码'), res.code))
    }
    const msg = (res && res.msg) || '请求失败'
    ElMessage.error(msg)
    return Promise.reject(withCode(new Error(msg), res && res.code, res && res.data))
  },
  (error) => {
    ElMessage.error('网络异常：' + (error.message || ''))
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
