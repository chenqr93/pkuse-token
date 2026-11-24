import Vue from 'vue'
import VueRouter from 'vue-router'
import { message } from 'ant-design-vue'
import {
  getToken,
  isAdmin,
  getStoredUser,
  clearLoginState,
  fetchUserCenterCurrentUser,
  fetchUserCenterApps
} from '../utils/auth'

Vue.use(VueRouter)

/**
 * 路由表（授权中心控制台，仅系统管理员）
 *
 * /login      登录中转页（未登录 → 整页跳用户中心统一登录，SSO 回跳后自动进入）
 * /dashboard  概览（统计卡片 + 应用列表/注册表）
 * /menu       菜单管理（按应用）
 * /role       角色管理（按应用，含角色-菜单勾选）
 * /user       用户授权（用户-角色分配，用户数据来自用户中心）
 * /log        操作日志（本地审计）
 */
const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/login/index.vue'),
    meta: { public: true, title: '登录' }
  },
  {
    path: '/',
    component: () => import('../layout/BasicLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'dashboard',
        component: () => import('../views/dashboard/index.vue'),
        meta: { title: '概览', icon: 'dashboard' }
      },
      {
        path: 'menu',
        name: 'menu',
        component: () => import('../views/menu/index.vue'),
        meta: { title: '菜单管理', icon: 'menu' }
      },
      {
        path: 'role',
        name: 'role',
        component: () => import('../views/role/index.vue'),
        meta: { title: '角色管理', icon: 'team' }
      },
      {
        path: 'user',
        name: 'user',
        component: () => import('../views/user/index.vue'),
        meta: { title: '用户授权', icon: 'user' }
      },
      {
        path: 'log',
        name: 'log',
        component: () => import('../views/log/index.vue'),
        meta: { title: '操作日志', icon: 'file-text' }
      }
    ]
  },
  // 未匹配路由统一回概览
  { path: '*', redirect: '/dashboard' }
]

const router = new VueRouter({
  mode: 'history',
  base: process.env.BASE_URL,
  routes
})

// 全局前置守卫：未登录跳登录中转页；控制台仅系统管理员可进入
let lastIdentityCheckAt = 0
let identityChecking = false

/**
 * 校验授权中心本地会话与当前浏览器 SSO 用户是否一致。
 *
 * <p> 授权中心使用独立的 authcenter-token 会话；当用户在同一浏览器先登录用户中心/业务应用，
 * 再打开授权中心时，本地可能残留上一个管理员会话。这里读取用户中心当前 SSO 用户，
 * 发现不一致则清理本地授权中心登录态，并通过 /sso/switch 重新换取当前用户的 ticket。 </p>
 */
export async function ensureSsoIdentity(force = false) {
  const now = Date.now()
  if (!force && now - lastIdentityCheckAt < 5000) {
    return true
  }
  if (identityChecking) {
    return true
  }
  const stored = getStoredUser()
  if (!stored) {
    return true
  }
  identityChecking = true
  lastIdentityCheckAt = now
  try {
    const ssoUser = await fetchUserCenterCurrentUser()
    if (ssoUser && ssoUser.unauthenticated) {
      // 用户中心已注销（单点注销推送后）：清本地会话并回到统一登录页
      clearLoginState()
      window.location.href = '/usercenter/login?redirect=' + encodeURIComponent(window.location.href)
      return false
    }
    if (!ssoUser || !ssoUser.id) {
      return true
    }
    if (String(ssoUser.id) === String(stored.id)) {
      return true
    }
    // 当前用户没有授权中心应用权限时，先回到中转页给出友好提示，避免后端 ticket 校验返回原始 JSON
    const apps = await fetchUserCenterApps()
    clearLoginState()
    if (apps && !apps.some((app) => app && app.clientId === 'auth-center')) {
      window.location.href = '/authcenter/login?denied=1'
      return false
    }
    const back = window.location.origin + window.location.pathname + window.location.search
    window.location.href = '/authcenter/sso/switch?back=' + encodeURIComponent(back)
    return false
  } finally {
    identityChecking = false
  }
}

router.beforeEach(async (to, from, next) => {
  document.title = (to.meta.title ? to.meta.title + ' - ' : '') + 'Pkuse-Token 授权中心'
  // simple 共享会话：登录态由用户中心 satoken Cookie 承载，接口 401 由响应拦截器统一引导跳登录
  if (!(await ensureSsoIdentity())) {
    next(false)
    return
  }
  if (to.meta.public) {
    next()
    return
  }
  if (!getToken()) {
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }
  next()
})

export default router
