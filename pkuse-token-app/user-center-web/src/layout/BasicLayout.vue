<template>
  <div class="app-shell">
    <!-- 顶部导航 -->
    <header class="shell-header">
      <div class="brand">
        <span class="brand-logo"><a-icon type="safety-certificate" /></span>
        <span class="brand-name">PKUSE</span>
        <span class="brand-divider">|</span>
        <span class="brand-product">统一用户中心</span>
      </div>

      <nav class="top-nav">
        <div
          v-for="group in menuGroups"
          :key="group.key"
          :class="['top-nav-item', { active: group.key === activeGroupKey }]"
          @click="selectGroup(group)">
          <a-icon :type="group.icon" />
          <span>{{ group.title }}</span>
        </div>
      </nav>

      <div class="header-right">
        <a-dropdown :trigger="['click']">
          <span class="user-info">
            <a-avatar :size="28" class="avatar">{{ avatarText }}</a-avatar>
            <span class="username">{{ displayName }}</span>
            <a-icon type="down" />
          </span>
          <a-menu slot="overlay" @click="onUserMenuClick">
            <a-menu-item key="profile"><a-icon type="user" /> 个人信息</a-menu-item>
            <a-menu-item key="password"><a-icon type="lock" /> 修改密码</a-menu-item>
            <a-menu-divider />
            <a-menu-item key="logout"><a-icon type="logout" /> 退出登录</a-menu-item>
          </a-menu>
        </a-dropdown>
      </div>
    </header>

    <div class="shell-body">
      <!-- 左侧菜单 -->
      <aside class="shell-sider">
        <div class="sider-title">
          <a-icon :type="activeGroup.icon" />
          <span>{{ activeGroup.title }}</span>
        </div>
        <a-menu
          mode="inline"
          theme="dark"
          class="sider-menu"
          :selected-keys="[currentPath]"
          @click="onMenuClick">
          <a-menu-item v-for="item in activeGroup.children" :key="item.path">
            <a-icon :type="item.icon" />
            <span>{{ item.title }}</span>
          </a-menu-item>
        </a-menu>
        <div class="sider-footer">
          <div>统一登录 / SSO 认证中心</div>
          <div class="text-mono">{{ ssoServerUrl }}</div>
        </div>
      </aside>

      <!-- 内容 -->
      <main class="shell-content">
        <div class="content-breadcrumb">
          <a-icon type="home" />
          <span class="crumb-root">用户中心</span>
          <a-icon type="right" class="crumb-sep" />
          <span>{{ activeGroup.title }}</span>
          <template v-if="currentTitle && currentTitle !== activeGroup.title">
            <a-icon type="right" class="crumb-sep" />
            <span>{{ currentTitle }}</span>
          </template>
        </div>
        <router-view />
      </main>
    </div>

    <!-- 个人信息 -->
    <a-modal v-model="profileVisible" title="个人信息" :width="520" :footer="null">
      <a-descriptions :column="1" bordered size="small">
        <a-descriptions-item label="登录账号">{{ user.username }}</a-descriptions-item>
        <a-descriptions-item label="昵称">{{ user.nickname }}</a-descriptions-item>
        <a-descriptions-item label="邮箱">{{ user.email || '-' }}</a-descriptions-item>
        <a-descriptions-item label="手机号">{{ user.phone || '-' }}</a-descriptions-item>
        <a-descriptions-item label="最后登录">{{ formatTime(user.lastLoginTime) }}</a-descriptions-item>
      </a-descriptions>
    </a-modal>
  </div>
</template>

<script>
import { authApi } from '../api'
import {
  clearLoginState,
  getStoredRoles,
  getStoredUser,
  setStoredRoles,
  setStoredUser
} from '../utils/auth'

export default {
  name: 'BasicLayout',
  data() {
    return {
      collapsed: false,
      profileVisible: false,
      user: getStoredUser() || {},
      roles: getStoredRoles(),
      ssoServerUrl: window.location.origin
    }
  },
  computed: {
    isAdmin() {
      return this.roles.includes('admin')
    },
    menuGroups() {
      const groups = [
        {
          key: 'workbench',
          title: '工作台',
          icon: 'dashboard',
          children: [{ path: '/workbench', title: '工作台', icon: 'dashboard' }]
        }
      ]
      if (this.isAdmin) {
        groups.push({
          key: 'system',
          title: '系统管理',
          icon: 'setting',
          children: [
            { path: '/user', title: '用户管理', icon: 'user' },
            { path: '/org', title: '组织架构', icon: 'apartment' },
            { path: '/region', title: '区划管理', icon: 'environment' },
            { path: '/client', title: '应用管理', icon: 'appstore' },
            { path: '/log', title: '登录日志', icon: 'file-text' },
            { path: '/operlog', title: '操作日志', icon: 'profile' },
            { path: '/config', title: '规则配置', icon: 'setting' }
          ]
        })
      }
      groups.push({
        key: 'account',
        title: '个人中心',
        icon: 'user',
        children: [{ path: '/password', title: '修改密码', icon: 'lock' }]
      })
      return groups
    },
    activeGroupKey() {
      const group = this.menuGroups.find((item) =>
        item.children.some((child) => child.path === this.$route.path)
      )
      return group ? group.key : this.menuGroups[0].key
    },
    activeGroup() {
      return this.menuGroups.find((item) => item.key === this.activeGroupKey) || this.menuGroups[0]
    },
    currentPath() {
      return this.$route.path
    },
    currentTitle() {
      return this.$route.meta.title || ''
    },
    displayName() {
      return this.user.nickname || this.user.username || '未登录'
    },
    avatarText() {
      return (this.displayName || 'U').charAt(0)
    }
  },
  mounted() {
    this.refreshUser()
  },
  methods: {
    async refreshUser() {
      try {
        const res = await authApi.me()
        this.user = res.data || {}
        setStoredUser(this.user)
      } catch (e) {
        // 401 已由拦截器统一处理
      }
      try {
        const rolesRes = await authApi.myRoles()
        this.roles = rolesRes.data || []
        setStoredRoles(this.roles)
      } catch (e) {
        // 沿用本地缓存
      }
    },
    selectGroup(group) {
      if (group.key === this.activeGroupKey) {
        return
      }
      const first = group.children[0]
      if (first) {
        this.$router.push(first.path)
      }
    },
    onMenuClick({ key }) {
      if (key && this.$route.path !== key) {
        this.$router.push(key)
      }
    },
    onUserMenuClick({ key }) {
      if (key === 'profile') {
        this.profileVisible = true
      } else if (key === 'password') {
        this.$router.push('/password')
      } else if (key === 'logout') {
        this.doLogout()
      }
    },
    doLogout() {
      this.$confirm({
        title: '退出登录',
        content: '确定要退出登录吗？退出后将回到统一登录页。',
        okText: '确定退出',
        cancelText: '取消',
        onOk: async () => {
          try {
            await authApi.logout()
          } catch (e) {
            // 忽略退出接口异常
          }
          clearLoginState()
          this.$message.success('已退出登录')
          this.$router.replace('/login')
        }
      })
    },
    formatTime(value) {
      if (!value) return '-'
      return String(value).replace('T', ' ').substring(0, 19)
    }
  }
}
</script>

<style lang="less" scoped>
@import '../styles/tokens.less';

.app-shell {
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: @pd-color-page-bg;
}

.shell-header {
  flex: 0 0 @pd-header-height;
  height: @pd-header-height;
  display: flex;
  align-items: center;
  padding: 0 20px;
  background: @pd-color-white;
  border-bottom: 1px solid @pd-color-border-light;
  box-shadow: @pd-shadow-header;
  z-index: 10;
}

.brand {
  display: flex;
  align-items: center;
  min-width: 280px;
  color: @pd-color-text;

  .brand-logo {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 30px;
    height: 30px;
    margin-right: 8px;
    border-radius: 4px;
    background: @pd-color-menu-bg;
    color: #fff;
    font-size: 18px;
  }

  .brand-name {
    font-size: 20px;
    font-weight: 700;
    letter-spacing: 1px;
    color: @pd-color-primary-dark;
  }

  .brand-divider {
    margin: 0 8px;
    color: @pd-color-border-light;
  }

  .brand-product {
    font-size: 16px;
    font-weight: 600;
    color: @pd-color-text;
  }
}

.top-nav {
  flex: 1;
  display: flex;
  align-items: stretch;
  height: 100%;
  overflow-x: auto;
  overflow-y: hidden;
}

.top-nav-item {
  display: flex;
  align-items: center;
  gap: 6px;
  height: @pd-header-height;
  padding: 0 16px;
  color: @pd-color-text-secondary;
  font-size: 14px;
  white-space: nowrap;
  cursor: pointer;
  border-bottom: 3px solid transparent;

  &:hover {
    color: @pd-color-primary-dark;
  }

  &.active {
    color: @pd-color-selected;
    background: @pd-color-card-tint;
    border-bottom-color: @pd-color-selected;
    font-weight: 600;
  }
}

.header-right {
  margin-left: auto;
  padding-left: 16px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.avatar {
  background: @pd-color-selected;
}

.username {
  max-width: 160px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: @pd-color-text;
}

.shell-body {
  flex: 1;
  display: flex;
  min-height: 0;
}

.shell-sider {
  flex: 0 0 @pd-sider-width;
  width: @pd-sider-width;
  display: flex;
  flex-direction: column;
  background: @pd-color-menu-bg;
  color: #fff;
  overflow: hidden;
}

.sider-title {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 48px;
  padding: 0 18px;
  background: @pd-color-menu-sub-bg;
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.sider-menu {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  border-right: 0;
  background: @pd-color-menu-bg;

  /deep/ .ant-menu-item {
    height: 44px;
    line-height: 44px;
    margin: 0;
    color: rgba(255, 255, 255, 0.75);

    &:hover {
      color: #fff;
      background: rgba(255, 255, 255, 0.08);
    }

    &.ant-menu-item-selected {
      color: #fff;
      background: @pd-color-selected;
    }
  }
}

.sider-footer {
  flex: 0 0 auto;
  padding: 12px 18px;
  font-size: 12px;
  line-height: 18px;
  color: rgba(255, 255, 255, 0.5);
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

.shell-content {
  flex: 1;
  min-width: 0;
  min-height: 0;
  overflow: auto;
  padding: @pd-content-padding;
  background: @pd-color-page-bg;
}

.content-breadcrumb {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: @pd-space-md;
  font-size: 13px;
  color: @pd-color-text-muted;

  .crumb-root {
    color: @pd-color-text-secondary;
  }

  .crumb-sep {
    font-size: 10px;
  }
}
</style>
