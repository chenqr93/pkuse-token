<template>
  <div class="page">
    <!-- 登录处理中 -->
    <div v-if="view === 'login'" class="center-box">
      <div class="spinner"></div>
      <p>{{ loginTip }}</p>
      <p v-if="errorMsg" class="error">{{ errorMsg }}</p>
    </div>

    <!-- 应用首页 -->
    <template v-else>
      <header class="header">
        <div class="brand">
          <span class="logo">SSO</span>
          <div>
            <div class="title">{{ config.clientName }}</div>
            <div class="subtitle">{{ config.modeLabel }}</div>
          </div>
        </div>
        <a class="link" :href="config.consoleUrl" target="_blank">用户中心（认证中心）↗</a>
      </header>

      <main class="main">
        <!-- 错误提示 -->
        <div v-if="errorMsg" class="alert-error">
          <span>操作失败：{{ errorMsg }}</span>
          <button class="btn ghost" @click="errorMsg = ''">关闭</button>
        </div>

        <!-- 登录状态 -->
        <section class="card">
          <div class="card-title">当前登录状态</div>
          <div class="status-row">
            <span class="badge" :class="isLogin ? 'badge-ok' : 'badge-off'">
              {{ isLogin ? '已登录' : '未登录' }}
            </span>
            <span v-if="isLogin" class="muted">loginId：{{ loginId }}</span>
          </div>
          <div v-if="token" class="muted mono token-line">
            token：{{ token.substring(0, 8) }}****{{ token.substring(token.length - 4) }}
          </div>
          <div class="muted tip">
            {{ isLogin ? '' : '点击下方「统一登录」跳转认证中心完成登录' }}
          </div>
        </section>

        <!-- 操作按钮 -->
        <section class="card">
          <div class="card-title">操作</div>
          <div class="btn-row">
            <button class="btn primary" @click="onLogin">
              {{ isLogin ? '重新登录' : '统一登录' }}
            </button>
            <button class="btn" :disabled="!isLogin" @click="onMyInfo">获取账号资料</button>
            <button class="btn" :disabled="!isLogin" @click="onLogoutByAlone">单应用注销</button>
            <button class="btn danger" :disabled="!isLogin" @click="onLogoutSlo">单点注销</button>
            <button class="btn ghost" @click="refresh">刷新状态</button>
          </div>
        </section>

        <!-- 授权中心：动态菜单与按钮级鉴权演示 -->
        <section class="card">
          <div class="card-title">
            授权中心 · 动态菜单与按钮权限
            <button class="btn ghost" style="margin-left: 8px" :disabled="!isLogin" @click="loadAuthz">
              重新拉取
            </button>
          </div>
          <template v-if="isLogin">
            <div v-if="authzError" class="alert-error" style="margin-bottom: 10px">
              <span>{{ authzError }}</span>
            </div>
            <p class="muted" style="margin: 6px 0">
              菜单树与权限码来自授权中心（角色绑定决定可见性）：zhangsan（user 角色）看不到「用户删除」按钮，
              admin 角色全部可见；隐藏页面（详情）不在导航中但其权限码仍生效。
            </p>

            <!-- 动态菜单（目录 → 页面，隐藏页不渲染） -->
            <div class="authz-menu">
              <div v-for="dir in authzMenus" :key="dir.id" class="authz-dir">
                <div class="authz-dir-name">📂 {{ dir.menuName }}</div>
                <div v-for="page in dir.children || []" :key="page.id" class="authz-page">
                  <span>📄 {{ page.menuName }}</span>
                  <span class="muted mono" style="font-size: 11px">{{ page.path }}</span>
                  <span v-if="page.perms" class="badge mono" style="font-size: 11px">{{ page.perms }}</span>
                </div>
              </div>
              <div v-if="!authzMenus.length" class="muted">
                当前用户在本应用无任何菜单权限（未分配角色或角色未绑定菜单）
              </div>
            </div>

            <!-- 权限码集合与按钮显隐 -->
            <div class="card-title" style="margin-top: 12px">我的权限码</div>
            <div class="perm-tags">
              <span v-for="perm in authzPerms" :key="perm" class="badge badge-ok mono">{{ perm }}</span>
              <span v-if="!authzPerms.length" class="muted">（空）</span>
            </div>

            <div class="btn-row" style="margin-top: 12px">
              <button
                class="btn"
                :disabled="!isLogin || !hasPerm('sys:user:add')"
                :title="hasPerm('sys:user:add') ? '' : '无权限码 sys:user:add'"
                @click="callDemo('add')"
              >新增（sys:user:add）</button>
              <button
                class="btn"
                :disabled="!isLogin || !hasPerm('sys:user:edit')"
                :title="hasPerm('sys:user:edit') ? '' : '无权限码 sys:user:edit'"
                @click="callDemo('edit')"
              >编辑（sys:user:edit）</button>
              <button
                class="btn danger"
                :disabled="!isLogin || !hasPerm('sys:user:del')"
                :title="hasPerm('sys:user:del') ? '' : '无权限码 sys:user:del'"
                @click="callDemo('del')"
              >删除（sys:user:del）</button>
            </div>
            <p v-if="demoMsg" class="muted" style="margin-top: 8px">接口返回：{{ demoMsg }}</p>
          </template>
          <p v-else class="muted">登录后展示本应用的动态菜单与按钮权限。</p>
        </section>

        <!-- 账号资料 -->
        <section v-if="userInfo" class="card">
          <div class="card-title">账号资料（来自认证中心 userinfo 消息）</div>
          <pre class="json">{{ JSON.stringify(userInfo, null, 2) }}</pre>
        </section>

        <!-- 模式说明 -->
        <section class="card tips">
          <div class="card-title">对接模式说明</div>
          <p>{{ config.modeDesc }}</p>
          <p class="muted mono">
            前端：http://localhost:{{ config.port }}　　后端：{{ config.apiBase }}
          </p>
        </section>
      </main>
    </template>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import config from './config.js'
import { ssoApi, authzApi, setToken, clearToken, getToken } from './api.js'

const view = ref('home')
const loginTip = ref('正在跳转统一登录…')
const errorMsg = ref('')
const isLogin = ref(false)
const loginId = ref(null)
const token = ref(getToken())
const userInfo = ref(null)

// 授权中心：动态菜单 / 权限码 / 演示结果
const authzMenus = ref([])
const authzPerms = ref([])
const authzError = ref('')
const demoMsg = ref('')

onMounted(async () => {
  // 统一登录回跳页面：/login（可能携带 ticket）
  if (window.location.pathname.startsWith('/login')) {
    await handleLoginPage()
    return
  }
  await refresh()
})

/** 登录页：驱动统一登录流程 */
async function handleLoginPage() {
  view.value = 'login'
  const params = new URLSearchParams(window.location.search)
  const ticket = params.get('ticket')

  try {
    if (config.loginMode === 'navigate') {
      // 模式一：直接跳本应用后端登录入口（同域 Cookie + 共享 Redis，无需 ticket）
      loginTip.value = '正在跳转认证中心统一登录…'
      window.location.href = `${config.apiBase}/sso/login?back=${encodeURIComponent(window.location.origin + '/')}`
      return
    }

    if (ticket) {
      // 从认证中心带 ticket 回跳：换取本应用 token
      loginTip.value = '正在校验 ticket 并登录…'
      const res = await ssoApi.doLoginByTicket(ticket)
      if (res.code === 200) {
        setToken(res.data)
        window.location.replace('/')
        return
      }
      errorMsg.value = res.msg || '登录失败'
      view.value = 'home'
      return
    }

    // 未登录：取认证中心授权地址并跳转
    loginTip.value = '正在跳转认证中心统一登录…'
    const res = await ssoApi.getSsoAuthUrl(window.location.origin + '/login')
    if (res.code === 200 && res.data) {
      window.location.href = res.data
      return
    }
    errorMsg.value = res.msg || '获取认证中心地址失败'
    view.value = 'home'
  } catch (e) {
    errorMsg.value = '请求失败：' + e.message
    view.value = 'home'
  }
}

/** 刷新登录状态 */
async function refresh() {
  try {
    const res = await ssoApi.isLogin()
    // isLogin 统一信封返回：业务字段在 data 内（data={isLogin,loginId,token}）
    isLogin.value = !!res.data?.isLogin
    loginId.value = res.data?.loginId
    token.value = res.data?.token || getToken()
    if (!isLogin.value) {
      userInfo.value = null
      authzMenus.value = []
      authzPerms.value = []
    } else if (!authzMenus.value.length) {
      loadAuthz()
    }
  } catch (e) {
    errorMsg.value = '请求失败：' + e.message
  }
}

/** 拉取授权中心分发的菜单树与权限码（starter 暴露的 /authz/*，直接返回数组） */
async function loadAuthz() {
  authzError.value = ''
  demoMsg.value = ''
  try {
    const [menusRes, permsRes] = await Promise.all([authzApi.menus(), authzApi.perms()])
    if (Array.isArray(menusRes)) {
      authzMenus.value = menusRes
    } else if (menusRes.code === 200) {
      authzMenus.value = menusRes.data || []
    } else {
      authzError.value = '菜单拉取失败：' + (menusRes.msg || menusRes.code || '')
    }
    if (Array.isArray(permsRes)) {
      authzPerms.value = permsRes
    } else if (permsRes.code === 200) {
      authzPerms.value = permsRes.data || []
    } else {
      authzError.value = '权限码拉取失败：' + (permsRes.msg || permsRes.code || '')
    }
  } catch (e) {
    authzError.value = '授权中心接口请求失败：' + e.message
  }
}

/** 是否拥有权限码（按钮显隐） */
function hasPerm(perm) {
  return authzPerms.value.includes(perm)
}

/** 调用受保护演示接口（后端 checkPermission；前端隐藏 + 后端 403 双重防线） */
async function callDemo(action) {
  demoMsg.value = ''
  const res = action === 'del' ? await authzApi.demoUserDelete() : await authzApi.demoUserEdit()
  if (res.code === 200) {
    demoMsg.value = '✅ ' + res.msg
  } else {
    demoMsg.value = `❌ code=${res.code} ${res.msg}`
  }
}

function onLogin() {
  window.location.href = '/login'
}

async function onMyInfo() {
  errorMsg.value = ''
  const res = await ssoApi.myInfo()
  if (res && res.code !== 200) {
    errorMsg.value = res.msg || '获取账号资料失败'
    return
  }
  userInfo.value = res
}

async function onLogoutByAlone() {
  await ssoApi.logoutByAlone()
  clearToken()
  await refresh()
}

function onLogoutSlo() {
  // 单点注销：跳应用后端 /sso/logout（会通知认证中心，注销后回到本页）
  window.location.href = ssoApi.logoutUrl(window.location.origin + '/')
}
</script>
