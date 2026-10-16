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
import { ssoApi, setToken, clearToken, getToken } from './api.js'

const view = ref('home')
const loginTip = ref('正在跳转统一登录…')
const errorMsg = ref('')
const isLogin = ref(false)
const loginId = ref(null)
const token = ref(getToken())
const userInfo = ref(null)

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
    isLogin.value = !!res.isLogin
    loginId.value = res.loginId
    token.value = res.token || getToken()
    if (!isLogin.value) {
      userInfo.value = null
    }
  } catch (e) {
    errorMsg.value = '请求失败：' + e.message
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
