<template>
  <div class="login-transit">
    <div class="transit-card">
      <div class="transit-title">
        <a-icon type="key" class="logo-icon" />
        <span>Pkuse-Token 授权中心</span>
      </div>

      <a-alert
        v-if="error"
        :message="error"
        type="error"
        show-icon
        :closable="false"
        class="transit-tip"
      />
      <div v-else class="transit-tip transit-loading">
        <a-icon type="loading" spin />
        <span>{{ loadingText }}</span>
      </div>

      <div class="transit-desc">
        登录由用户中心统一完成（SSO，当前模式：<a-tag class="pd-tag-blue">{{ ssoModeText }}</a-tag>），
        登录成功后自动返回本控制台。
      </div>

      <div class="transit-actions">
        <a-button v-if="!denied" type="primary" :loading="!error" @click="retry">重新检测登录态</a-button>
        <a-button @click="goUserCenter">返回用户中心</a-button>
      </div>
    </div>
  </div>
</template>

<script>
import { overviewApi as authApi } from '../../api'
import { clearLoginState, saveLoginState } from '../../utils/auth'

/** 用户中心前端地址（与部署路径一致，支持本地/生产同构） */
const USER_CENTER_HOME = window.location.origin + '/usercenter/workbench'

export default {
  name: 'LoginTransit',
  data() {
    return {
      loading: true,
      denied: false,
      error: '',
      ssoModeText: '检测中',
      loadingText: '正在检测登录态，即将跳转用户中心统一登录…'
    }
  },
  mounted() {
    if (this.$route.query.denied === '1') {
      this.denied = true
      this.loading = false
      this.loadingText = ''
      this.error = '当前账号没有授权中心控制台访问权限，请联系管理员开通'
      return
    }
    this.check()
  },
  methods: {
    /** 解析登录后的目标地址（SSO 回跳保留） */
    targetPath() {
      const redirect = this.$route.query.redirect
      return redirect && String(redirect).startsWith('/') ? String(redirect) : '/dashboard'
    },
    /** 整页跳用户中心统一认证入口（未登录由用户中心引导到统一登录页，登录后 302 带回） */
    redirectToSsoAuth() {
      const back =
        window.location.origin +
        process.env.BASE_URL +
        'dashboard'
      const ucBase = process.env.VUE_APP_UC_BASE || window.location.origin
      window.location.href =
        ucBase +
        '/sso/auth?client=auth-center&redirect=' +
        encodeURIComponent(back)
    },
    async check() {
      this.loading = true
      this.error = ''
      try {
        // simple 共享会话：登录态由用户中心 satoken Cookie 承载（客户端零 SSO 端点）——
        // 探测一个需登录接口：200 已登录（存角色后进工作台）；401 未登录跳统一登录
        const res = await fetch('/api/authz/my-roles', { credentials: 'include' })
        if (res.status === 401) {
          this.redirectToSsoAuth()
          return
        }
        const body = await res.json()
        if (!body || body.code !== 200) {
          this.error = '登录态检测失败：' + ((body && body.msg) || '服务不可用')
          this.loadingText = ''
          return
        }
        const roles = body.data || []
        // 本地登记登录态标记（simple 模式无独立 token，占位值供路由守卫 getToken() 判断）
        saveLoginState('simple-cookie', null, '', roles)
        if (!roles.includes('admin')) {
          this.error = '当前账号没有控制台管理员角色，无法进入授权中心控制台'
          this.loadingText = ''
          return
        }
        this.$router.replace(this.targetPath())
        return
      } catch (e) {
        this.error = '登录态检测失败：服务不可用，请确认授权中心后端已启动'
        this.loadingText = ''
      } finally {
        this.loading = false
      }
    },
    retry() {
      this.check()
    },
    goUserCenter() {
      window.location.href = USER_CENTER_HOME
    }
  }
}
</script>

<style lang="less" scoped>
@import '../../styles/tokens.less';

.login-transit {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, @pd-color-page-bg 0%, @pd-color-card-tint 100%);
}

.transit-card {
  width: 420px;
  padding: @pd-card-padding;
  text-align: center;
  background: @pd-color-white;
  border-radius: @pd-card-radius;
  box-shadow: @pd-shadow-popup;
}

.transit-title {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: @pd-space-sm;
  margin-bottom: 18px;
  font-size: 18px;
  font-weight: 600;
  color: @pd-color-text;
}

.logo-icon {
  color: @pd-btn-primary;
  font-size: 26px;
}

.transit-tip {
  margin: @pd-space-md 0;
  color: @pd-color-text-secondary;
}

.transit-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: @pd-space-sm;
}

.transit-desc {
  margin-bottom: @pd-space-lg;
  color: @pd-color-text-muted;
  font-size: 12px;
  line-height: 1.8;
}

.transit-actions {
  display: flex;
  justify-content: center;
  gap: @pd-space-sm;
}
</style>
