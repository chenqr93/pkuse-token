import Vue from 'vue'
import VueRouter from 'vue-router'
import { message } from 'ant-design-vue'
import { getToken, isAdmin } from '../utils/auth'

Vue.use(VueRouter)

/**
 * 路由表
 *
 * /login      统一登录页（认证中心登录入口，支持 SSO redirect 回跳）
 * /workbench  工作台（所有登录用户可见；普通用户仅展示已授权应用入口）
 * /user       用户管理（仅系统管理员）
 * /org        组织架构管理（仅系统管理员）
 * /region     区划管理（仅系统管理员）
 * /client     应用管理（仅系统管理员）
 * /config     规则配置（仅系统管理员）
 * /password   修改密码（所有登录用户；开启「首次登录强制修改密码」时由登录页引导进入）
 */
const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/login/index.vue'),
    meta: { public: true, title: '统一登录' }
  },
  {
    path: '/',
    component: () => import('../layout/BasicLayout.vue'),
    redirect: '/workbench',
    children: [
      {
        path: 'workbench',
        name: 'workbench',
        component: () => import('../views/workbench/index.vue'),
        meta: { title: '工作台', icon: 'dashboard' }
      },
      {
        path: 'user',
        name: 'user',
        component: () => import('../views/user/index.vue'),
        meta: { title: '用户管理', icon: 'user', admin: true }
      },
      {
        path: 'org',
        name: 'org',
        component: () => import('../views/org/index.vue'),
        meta: { title: '组织架构', icon: 'apartment', admin: true }
      },
      {
        path: 'region',
        name: 'region',
        component: () => import('../views/region/index.vue'),
        meta: { title: '区划管理', icon: 'environment', admin: true }
      },
      {
        path: 'client',
        name: 'client',
        component: () => import('../views/client/index.vue'),
        meta: { title: '应用管理', icon: 'appstore', admin: true }
      },
      {
        path: 'log',
        name: 'log',
        component: () => import('../views/log/index.vue'),
        meta: { title: '登录日志', icon: 'file-text', admin: true }
      },
      {
        path: 'operlog',
        name: 'operlog',
        component: () => import('../views/operlog/index.vue'),
        meta: { title: '操作日志', icon: 'profile', admin: true }
      },
      {
        path: 'config',
        name: 'config',
        component: () => import('../views/config/index.vue'),
        meta: { title: '规则配置', icon: 'setting', admin: true }
      },
      {
        path: 'password',
        name: 'password',
        component: () => import('../views/password/index.vue'),
        meta: { title: '修改密码', icon: 'lock' }
      }
    ]
  },
  // 未匹配路由统一回工作台
  { path: '*', redirect: '/workbench' }
]

const router = new VueRouter({
  mode: 'history',
  base: process.env.BASE_URL,
  routes
})

// 全局前置守卫：未登录跳转统一登录页；管理页面仅系统管理员可进入
router.beforeEach((to, from, next) => {
  document.title = (to.meta.title ? to.meta.title + ' - ' : '') + 'Pkuse-Token 用户中心'
  if (to.meta.public) {
    next()
    return
  }
  if (!getToken()) {
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }
  if (to.meta.admin && !isAdmin()) {
    message.warning('该功能仅系统管理员可用')
    next({ path: '/workbench' })
    return
  }
  next()
})

export default router
