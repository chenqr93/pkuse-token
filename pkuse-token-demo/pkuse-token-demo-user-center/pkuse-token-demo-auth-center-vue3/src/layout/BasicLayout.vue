<template>
  <el-container class="layout">
    <!-- 左侧菜单 -->
    <el-aside width="220px" class="aside">
      <div class="logo">
        <el-icon :size="22"><Key /></el-icon>
        <span>Pkuse-Token 授权中心</span>
      </div>
      <el-menu :default-active="activeMenu" router class="menu">
        <el-menu-item index="/dashboard">
          <el-icon><Odometer /></el-icon>
          <span>概览</span>
        </el-menu-item>
        <el-menu-item index="/menu">
          <el-icon><Menu /></el-icon>
          <span>菜单管理</span>
        </el-menu-item>
        <el-menu-item index="/role">
          <el-icon><Avatar /></el-icon>
          <span>角色管理</span>
        </el-menu-item>
        <el-menu-item index="/user">
          <el-icon><User /></el-icon>
          <span>用户授权</span>
        </el-menu-item>
        <el-menu-item index="/log">
          <el-icon><Memo /></el-icon>
          <span>操作日志</span>
        </el-menu-item>
      </el-menu>
      <div class="aside-footer text-muted">
        <div>统一权限（RBAC）管理中心</div>
        <div class="text-mono">SSO 模式：{{ ssoMode || '-' }}</div>
      </div>
    </el-aside>

    <el-container>
      <!-- 顶栏 -->
      <el-header class="header">
        <div class="header-title">{{ currentTitle }}</div>
        <el-dropdown @command="onCommand">
          <span class="user-info">
            <el-avatar :size="28" class="avatar">{{ avatarText }}</el-avatar>
            <span class="username">{{ displayName }}</span>
            <el-tag size="small" type="danger">管理员</el-tag>
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="userCenter">
                <el-icon><HomeFilled /></el-icon>返回用户中心
              </el-dropdown-item>
              <el-dropdown-item divided command="logout">
                <el-icon><SwitchButton /></el-icon>退出登录（单点注销）
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>

      <!-- 内容区 -->
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { getStoredUser, getSsoMode } from '../utils/auth'

const route = useRoute()

const activeMenu = computed(() => route.path)
const currentTitle = computed(() => route.meta.title || '')

const user = computed(() => getStoredUser() || {})
const ssoMode = computed(() => getSsoMode())

const displayName = computed(() => user.value.nickname || user.value.username || '未登录')
const avatarText = computed(() => (displayName.value || '?').slice(0, 1))

/** 单点注销：整页跳后端 /sso/logout（由用户中心完成全端注销），回到用户中心工作台 */
function onCommand(command) {
  if (command === 'logout') {
    const back = 'http://localhost:5173/workbench'
    window.location.href = '/sso/logout?back=' + encodeURIComponent(back)
    return
  }
  if (command === 'userCenter') {
    window.location.href = 'http://localhost:5173/workbench'
  }
}
</script>

<style scoped>
.layout {
  height: 100%;
}

.aside {
  background: #001529;
  color: #fff;
  display: flex;
  flex-direction: column;
}

.logo {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-weight: 600;
  color: #fff;
}

.menu {
  border-right: none;
  background: transparent;
  flex: 1;
}

.aside-footer {
  padding: 12px;
  font-size: 12px;
  text-align: center;
}

.header {
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #e6e8eb;
}

.header-title {
  font-size: 16px;
  font-weight: 600;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.avatar {
  background: #409eff;
}

.username {
  max-width: 160px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.main {
  background: #f5f7fa;
  overflow-y: auto;
}
</style>
