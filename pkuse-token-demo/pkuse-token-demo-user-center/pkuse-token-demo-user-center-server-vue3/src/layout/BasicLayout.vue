<template>
  <el-container class="layout">
    <!-- 左侧菜单 -->
    <el-aside width="220px" class="aside">
      <div class="logo">
        <el-icon :size="22"><Key /></el-icon>
        <span>Pkuse-Token 用户中心</span>
      </div>
      <el-menu :default-active="activeMenu" router class="menu">
        <el-menu-item index="/workbench">
          <el-icon><Odometer /></el-icon>
          <span>工作台</span>
        </el-menu-item>
        <!-- 用户管理、应用管理仅系统管理员可见 -->
        <el-menu-item v-if="isAdmin" index="/user">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
        <el-menu-item v-if="isAdmin" index="/client">
          <el-icon><Grid /></el-icon>
          <span>应用管理</span>
        </el-menu-item>
        <el-menu-item v-if="isAdmin" index="/log">
          <el-icon><Document /></el-icon>
          <span>登录日志</span>
        </el-menu-item>
        <el-menu-item v-if="isAdmin" index="/operlog">
          <el-icon><Memo /></el-icon>
          <span>操作日志</span>
        </el-menu-item>
        <el-menu-item v-if="isAdmin" index="/config">
          <el-icon><Setting /></el-icon>
          <span>规则配置</span>
        </el-menu-item>
      </el-menu>
      <div class="aside-footer text-muted">
        <div>统一登录 / SSO 认证中心</div>
        <div class="text-mono">{{ ssoServerUrl }}</div>
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
            <el-tag v-if="user.role" size="small" :type="user.role === 'admin' ? 'danger' : 'info'">
              {{ user.role === 'admin' ? '管理员' : '普通用户' }}
            </el-tag>
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">
                <el-icon><InfoFilled /></el-icon>个人信息
              </el-dropdown-item>
              <el-dropdown-item command="password">
                <el-icon><Lock /></el-icon>修改密码
              </el-dropdown-item>
              <el-dropdown-item divided command="logout">
                <el-icon><SwitchButton /></el-icon>退出登录
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

    <!-- 个人信息 -->
    <el-dialog v-model="profileVisible" title="个人信息" width="520px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="登录账号">{{ user.username }}</el-descriptions-item>
        <el-descriptions-item label="昵称">{{ user.nickname }}</el-descriptions-item>
        <el-descriptions-item label="角色">{{ user.role === 'admin' ? '管理员' : '普通用户' }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ user.email || '-' }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ user.phone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="最后登录">{{ formatTime(user.lastLoginTime) }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </el-container>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Key, Odometer, User, Grid, Document, Memo, Setting, Lock,
  ArrowDown, InfoFilled, SwitchButton
} from '@element-plus/icons-vue'
import { authApi } from '../api'
import { clearLoginState, getStoredUser, isAdmin as isAdminUser, setStoredUser } from '../utils/auth'

const route = useRoute()
const router = useRouter()

const user = ref(getStoredUser() || {})
const profileVisible = ref(false)

const isAdmin = computed(() => isAdminUser() || user.value.role === 'admin')

const activeMenu = computed(() => route.path)
const currentTitle = computed(() => route.meta.title || '用户中心')
const displayName = computed(() => user.value.nickname || user.value.username || '未登录')
const avatarText = computed(() => (displayName.value || 'U').charAt(0))
const ssoServerUrl = computed(() => `${window.location.protocol}//${window.location.hostname}:9000`)

onMounted(async () => {
  // 拉取最新的当前用户信息，保持顶栏信息准确
  try {
    const res = await authApi.me()
    user.value = res.data || {}
    setStoredUser(user.value)
  } catch (e) {
    // 401 已由拦截器统一处理
  }
})

function formatTime(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').substring(0, 19)
}

function onCommand(command) {
  if (command === 'profile') {
    profileVisible.value = true
  } else if (command === 'password') {
    router.push('/password')
  } else if (command === 'logout') {
    doLogout()
  }
}

async function doLogout() {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？退出后将回到统一登录页。', '提示', {
      confirmButtonText: '确定退出',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch (e) {
    return
  }
  try {
    await authApi.logout()
  } catch (e) {
    // 忽略退出接口异常，本地登录态照常清除
  }
  clearLoginState()
  ElMessage.success('已退出登录')
  router.replace('/login')
}
</script>

<style scoped>
.layout {
  height: 100%;
}

.aside {
  display: flex;
  flex-direction: column;
  background-color: #001529;
  color: #fff;
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 60px;
  padding: 0 16px;
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  white-space: nowrap;
}

.menu {
  flex: 1;
  border-right: none;
  background-color: transparent;
}

.menu :deep(.el-menu-item) {
  color: rgba(255, 255, 255, 0.72);
}

.menu :deep(.el-menu-item:hover) {
  color: #fff;
  background-color: rgba(255, 255, 255, 0.08);
}

.menu :deep(.el-menu-item.is-active) {
  color: #fff;
  background-color: var(--el-color-primary);
}

.aside-footer {
  padding: 12px 16px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.45);
  line-height: 1.8;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background-color: #fff;
  border-bottom: 1px solid #ebeef5;
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
  outline: none;
}

.user-info .avatar {
  background-color: var(--el-color-primary);
}

.user-info .username {
  font-weight: 500;
}

.main {
  background-color: #f5f7fa;
  padding: 0;
}
</style>
