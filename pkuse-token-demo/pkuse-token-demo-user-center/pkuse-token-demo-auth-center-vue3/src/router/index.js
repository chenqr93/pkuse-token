import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getToken, isAdmin } from '../utils/auth'

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
        meta: { title: '概览', icon: 'Odometer' }
      },
      {
        path: 'menu',
        name: 'menu',
        component: () => import('../views/menu/index.vue'),
        meta: { title: '菜单管理', icon: 'Menu' }
      },
      {
        path: 'role',
        name: 'role',
        component: () => import('../views/role/index.vue'),
        meta: { title: '角色管理', icon: 'Avatar' }
      },
      {
        path: 'user',
        name: 'user',
        component: () => import('../views/user/index.vue'),
        meta: { title: '用户授权', icon: 'User' }
      },
      {
        path: 'log',
        name: 'log',
        component: () => import('../views/log/index.vue'),
        meta: { title: '操作日志', icon: 'Memo' }
      }
    ]
  },
  // 未匹配路由统一回概览
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 全局前置守卫：未登录跳登录中转页；控制台仅系统管理员可进入
router.beforeEach((to) => {
  document.title = (to.meta.title ? to.meta.title + ' - ' : '') + 'Pkuse-Token 授权中心'
  if (to.meta.public) {
    return true
  }
  if (!getToken()) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (!isAdmin()) {
    ElMessage.warning('授权中心控制台仅系统管理员可用')
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  return true
})

export default router
