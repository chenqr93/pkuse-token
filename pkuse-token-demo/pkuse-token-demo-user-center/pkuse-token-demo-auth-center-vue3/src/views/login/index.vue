<template>
  <div class="login-transit">
    <el-card class="transit-card">
      <div class="transit-title">
        <el-icon :size="26" class="logo-icon"><Key /></el-icon>
        <span>Pkuse-Token 授权中心</span>
      </div>
      <el-alert
        v-if="error"
        :title="error"
        type="error"
        show-icon
        :closable="false"
        class="transit-tip"
      />
      <div v-else class="transit-tip">
        <el-icon class="is-loading"><Loading /></el-icon>
        {{ loadingText }}
      </div>
      <div class="transit-desc">
        登录由用户中心统一完成（SSO，当前模式：<el-tag size="small">{{ ssoModeText }}</el-tag>），
        登录成功后自动返回本控制台。
      </div>
      <div class="transit-actions">
        <el-button type="primary" :loading="!error" @click="retry">重新检测登录态</el-button>
        <el-button @click="goUserCenter">返回用户中心</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ssoApi, overviewApi as authApi } from '../../api'
import { clearLoginState, saveLoginState } from '../../utils/auth'

/** 用户中心前端地址（登录后非管理员回跳目标） */
const USER_CENTER_HOME = 'http://localhost:5173/workbench'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const error = ref('')
const ssoModeText = ref('检测中')

const loadingText = ref('正在检测登录态，即将跳转用户中心统一登录…')

/** 解析登录后的目标地址（SSO 回跳保留） */
function targetPath() {
  const redirect = route.query.redirect
  return redirect && String(redirect).startsWith('/') ? String(redirect) : '/dashboard'
}

/** 整页跳转后端 /sso/login，由其 302 到用户中心统一登录页（登录后携带 ticket/会话回到本页） */
function redirectToSsoLogin() {
  const back = window.location.origin + import.meta.env.BASE_URL + 'login?redirect=' + encodeURIComponent(targetPath())
  window.location.href = '/sso/login?back=' + encodeURIComponent(back)
}

async function check() {
  loading.value = true
  error.value = ''
  try {
    const res = await ssoApi.isLogin()
    // isLogin 统一信封返回：业务字段（isLogin/token/user/ssoMode）在 data 内
    const data = res.data || {}
    ssoModeText.value = data.ssoMode || '-'
    if (data.isLogin) {
      if (!data.token) {
        // 已登录但未取到 token（极端情况），刷新重试
        window.location.reload()
        return
      }
      // 用户信息不再含角色，控制台管理端角色改走本地 RBAC（/api/authz/my-roles）
      const rolesRes = await authApi.myRoles().catch(() => null)
      const roles = rolesRes?.data || []
      saveLoginState(data.token, data.user || null, data.ssoMode || '', roles)
      if (!roles.includes('admin')) {
        error.value = '当前账号没有控制台管理员角色，无法进入授权中心控制台'
        loadingText.value = ''
        return
      }
      router.replace(targetPath())
      return
    }
    // 未登录：清掉本地残留的旧 token。会话可能已被踢/失效，而请求头里带着的过期 token
    // 会遮蔽浏览器里仍然有效的 authcenter-token Cookie（sa-token 读取顺序：header > cookie），
    // 不清掉的话 /sso/login 回跳后 isLogin 永远 false，卡死在登录中转页。
    clearLoginState()
    redirectToSsoLogin()
  } catch (e) {
    // /sso/isLogin 不可用（后端未启动等）
    error.value = '登录态检测失败：' + (e.message || '服务不可用')
    loadingText.value = ''
  } finally {
    loading.value = false
  }
}

function retry() {
  check()
}

function goUserCenter() {
  window.location.href = USER_CENTER_HOME
}

onMounted(check)
</script>

<style scoped>
.login-transit {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4effc 100%);
}

.transit-card {
  width: 420px;
  text-align: center;
}

.transit-title {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 18px;
}

.logo-icon {
  color: #409eff;
}

.transit-tip {
  margin: 12px 0;
  color: #606266;
}

.transit-desc {
  color: #909399;
  font-size: 12px;
  line-height: 1.8;
  margin-bottom: 16px;
}

.transit-actions {
  display: flex;
  justify-content: center;
  gap: 8px;
}
</style>
