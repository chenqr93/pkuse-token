<template>
  <div class="app-shell">
    <header class="shell-header">
      <div class="brand">
        <span class="brand-logo"><a-icon type="safety-certificate" /></span>
        <span class="brand-name">PKUSE</span>
        <span class="brand-divider">|</span>
        <span class="brand-product">授权中心</span>
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
            <a-tag class="pd-tag-red">管理员</a-tag>
            <a-icon type="down" />
          </span>
          <a-menu slot="overlay" @click="onUserMenuClick">
            <a-menu-item key="userCenter"><a-icon type="home" /> 返回用户中心</a-menu-item>
            <a-menu-divider />
            <a-menu-item key="logout"><a-icon type="logout" /> 退出登录（单点注销）</a-menu-item>
          </a-menu>
        </a-dropdown>
      </div>
    </header>

    <div class="shell-body">
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
          <div>统一权限（RBAC）管理中心</div>
          <div class="text-mono">SSO 模式：{{ ssoMode || '-' }}</div>
        </div>
      </aside>

      <main class="shell-content">
        <div class="content-breadcrumb">
          <a-icon type="home" />
          <span class="crumb-root">授权中心</span>
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
  </div>
</template>

<script>
import { getStoredUser, getSsoMode } from '../utils/auth'

const USER_CENTER_HOME = window.location.origin + '/usercenter/workbench'

export default {
  name: 'BasicLayout',
  data() {
    return {
      user: getStoredUser() || {},
      ssoMode: getSsoMode(),
      userCenterHome: USER_CENTER_HOME
    }
  },
  computed: {
    menuGroups() {
      return [
        {
          key: 'overview',
          title: '概览',
          icon: 'dashboard',
          children: [{ path: '/dashboard', title: '概览', icon: 'dashboard' }]
        },
        {
          key: 'authz',
          title: '权限管理',
          icon: 'safety-certificate',
          children: [
            { path: '/menu', title: '菜单管理', icon: 'menu' },
            { path: '/role', title: '角色管理', icon: 'team' },
            { path: '/user', title: '用户授权', icon: 'user' }
          ]
        },
        {
          key: 'audit',
          title: '审计',
          icon: 'audit',
          children: [{ path: '/log', title: '操作日志', icon: 'file-text' }]
        }
      ]
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
      return (this.displayName || '?').slice(0, 1)
    }
  },
  methods: {
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
      if (key === 'logout') {
        window.location.href = (process.env.VUE_APP_UC_BASE || window.location.origin) + '/sso/logout?singleDeviceIdLogout=true&back=' + encodeURIComponent(USER_CENTER_HOME)
      } else if (key === 'userCenter') {
        window.location.href = USER_CENTER_HOME
      }
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
  min-width: 260px;
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
