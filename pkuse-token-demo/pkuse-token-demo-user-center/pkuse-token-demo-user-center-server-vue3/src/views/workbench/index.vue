<template>
  <div class="page-container">
    <!-- 欢迎卡片 -->
    <el-card shadow="never" class="welcome-card">
      <div class="welcome">
        <el-avatar :size="52" class="welcome-avatar">{{ displayName.charAt(0) }}</el-avatar>
        <div class="welcome-text">
          <div class="welcome-title">
            欢迎回来，{{ displayName }}！
            <el-tag v-if="currentUser.role" size="small" :type="isAdmin ? 'danger' : 'info'">
              {{ isAdmin ? '系统管理员' : '普通用户' }}
            </el-tag>
          </div>
          <div class="welcome-sub">
            上次登录：{{ formatTime(currentUser.lastLoginTime) }}
            <template v-if="overview.serverTime">　|　当前时间：{{ overview.serverTime }}</template>
          </div>
        </div>
        <div v-if="isAdmin" class="welcome-actions">
          <el-button type="primary" @click="$router.push('/user')">
            <el-icon><User /></el-icon>用户管理
          </el-button>
          <el-button @click="$router.push('/client')">
            <el-icon><Grid /></el-icon>应用管理
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 我的应用（应用入口） -->
    <el-card shadow="never" class="section-card" v-loading="loading">
      <template #header>
        <div class="card-header">
          <span>
            我的应用
            <span class="text-muted header-tip">
              （{{ isAdmin ? '系统管理员可进入全部应用' : '仅展示已授权应用，点击进入' }}）
            </span>
          </span>
          <el-button link type="primary" @click="loadData">刷新</el-button>
        </div>
      </template>
      <el-empty
        v-if="!myApps.length"
        :description="isAdmin ? '暂无启用中的应用' : '暂未授权任何应用，请联系管理员开通'"
      />
      <div v-else class="app-grid">
        <div
          v-for="app in myApps"
          :key="app.clientId"
          class="app-card"
          :class="{ disabled: !app.entryUrl }"
          @click="enterApp(app)"
        >
          <div class="app-icon">
            <el-icon :size="22"><Grid /></el-icon>
          </div>
          <div class="app-body">
            <div class="app-name">{{ app.clientName }}</div>
            <div class="app-id text-mono text-muted">{{ app.clientId }}</div>
          </div>
          <el-tag size="small" :type="app.entryUrl ? 'success' : 'info'">
            {{ app.entryUrl ? '进入应用' : '未配置入口' }}
          </el-tag>
        </div>
      </div>
    </el-card>

    <!-- 以下仅系统管理员可见 -->
    <template v-if="isAdmin">
      <div class="stat-row">
        <el-card shadow="never" class="stat-card">
          <div class="stat-value online">{{ overview.onlineUserCount ?? '-' }}</div>
          <div class="stat-label">当前在线</div>
        </el-card>
        <el-card shadow="never" class="stat-card">
          <div class="stat-value">{{ overview.userCount ?? '-' }}</div>
          <div class="stat-label">用户总数</div>
        </el-card>
        <el-card shadow="never" class="stat-card">
          <div class="stat-value success">{{ overview.enabledUserCount ?? '-' }}</div>
          <div class="stat-label">启用用户</div>
        </el-card>
        <el-card shadow="never" class="stat-card">
          <div class="stat-value">{{ overview.clientCount ?? '-' }}</div>
          <div class="stat-label">接入应用</div>
        </el-card>
        <el-card shadow="never" class="stat-card">
          <div class="stat-value success">{{ overview.enabledClientCount ?? '-' }}</div>
          <div class="stat-label">启用应用</div>
        </el-card>
      </div>

      <el-row :gutter="16">
        <!-- 已接入应用 -->
        <el-col :span="15">
          <el-card shadow="never">
            <template #header>
              <div class="card-header">
                <span>已接入应用</span>
                <el-button link type="primary" @click="loadData">刷新</el-button>
              </div>
            </template>
            <el-table :data="myApps" size="default">
              <el-table-column prop="clientId" label="应用标识" min-width="130" show-overflow-tooltip>
                <template #default="{ row }">
                  <span class="text-mono">{{ row.clientId }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="clientName" label="应用名称" min-width="130" show-overflow-tooltip />
              <el-table-column label="回调地址" min-width="180" show-overflow-tooltip>
                <template #default="{ row }">
                  <span class="text-mono text-muted">{{ row.allowUrl || '-' }}</span>
                </template>
              </el-table-column>
              <el-table-column label="状态" width="160">
                <template #default="{ row }">
                  <el-tag v-if="registeredClients.includes(row.clientId)" type="success" size="small">
                    已同步到认证中心
                  </el-tag>
                  <el-tag v-else type="warning" size="small">待同步</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="90" align="center">
                <template #default="{ row }">
                  <el-button link type="primary" :disabled="!row.entryUrl" @click="enterApp(row)">进入</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-col>

        <!-- 认证中心信息 -->
        <el-col :span="9">
          <el-card shadow="never">
            <template #header>
              <div class="card-header">
                <span>统一登录认证中心</span>
                <el-tag size="small" type="success">运行中</el-tag>
              </div>
            </template>
            <el-descriptions :column="1" border>
              <el-descriptions-item label="认证中心地址">
                <span class="text-mono">{{ ssoServerUrl }}</span>
              </el-descriptions-item>
              <el-descriptions-item label="授权地址">
                <span class="text-mono">/sso/auth</span>
              </el-descriptions-item>
              <el-descriptions-item label="Ticket 有效期">
                {{ overview.ssoServer ? overview.ssoServer.ticketTimeout : '-' }} 秒
              </el-descriptions-item>
              <el-descriptions-item label="参数签名校验">
                <el-tag size="small" :type="overview.ssoServer && overview.ssoServer.checkSign ? 'success' : 'warning'">
                  {{ overview.ssoServer && overview.ssoServer.checkSign ? '已开启' : '演示环境已跳过' }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="已注册应用">
                <el-tag v-for="id in registeredClients" :key="id" size="small" class="client-tag">{{ id }}</el-tag>
                <span v-if="!registeredClients.length" class="text-muted">暂无</span>
              </el-descriptions-item>
            </el-descriptions>
            <el-alert type="info" :closable="false" class="sso-alert">
              <template #title>
                普通用户仅能进入已授权应用（登录时校验授权）；应用跳转
                <code>/sso/auth?client=应用标识&amp;redirect=回调地址</code>
              </template>
            </el-alert>
          </el-card>
        </el-col>
      </el-row>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { dashboardApi } from '../../api'
import { getStoredUser, isAdmin as isAdminUser, setStoredUser } from '../../utils/auth'

const loading = ref(false)
const overview = ref({})
const myApps = ref([])
const currentUser = ref(getStoredUser() || {})

const isAdmin = computed(() => isAdminUser() || currentUser.value.role === 'admin')
const displayName = computed(() => currentUser.value.nickname || currentUser.value.username || '用户')
const ssoServerUrl = computed(() => `${window.location.protocol}//${window.location.hostname}:8091`)
const registeredClients = computed(() => overview.value.registeredClients || [])

onMounted(loadData)

async function loadData() {
  loading.value = true
  try {
    // 我的应用入口（所有用户）
    const myAppsRes = await dashboardApi.myApps()
    myApps.value = myAppsRes.data || []
    // 总览数据（仅系统管理员；普通用户不请求）
    if (isAdmin.value) {
      const overviewRes = await dashboardApi.overview()
      overview.value = overviewRes.data || {}
      if (overview.value.currentUser) {
        currentUser.value = overview.value.currentUser
        setStoredUser(currentUser.value)
      }
    }
  } catch (e) {
    // 错误提示已由 axios 拦截器统一处理
  } finally {
    loading.value = false
  }
}

/** 进入应用：跳转认证中心授权地址，由认证中心下发 ticket 后回到应用 */
function enterApp(app) {
  if (!app.entryUrl) {
    ElMessage.warning('该应用未配置入口地址，请联系管理员')
    return
  }
  const url = `${ssoServerUrl.value}/sso/auth?client=${encodeURIComponent(app.clientId)}`
    + `&redirect=${encodeURIComponent(app.entryUrl)}&mode=ticket`
  window.location.href = url
}

function formatTime(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').substring(0, 19)
}
</script>

<style scoped>
.welcome-card {
  background: linear-gradient(120deg, #0b2f5c 0%, #1a4f8f 60%, #2a6fc4 100%);
  border: none;
}

.welcome {
  display: flex;
  align-items: center;
  gap: 16px;
}

.welcome-avatar {
  background-color: rgba(255, 255, 255, 0.2);
  color: #fff;
  font-size: 22px;
}

.welcome-text {
  flex: 1;
}

.welcome-title {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #fff;
  font-size: 18px;
  font-weight: 600;
}

.welcome-sub {
  margin-top: 6px;
  color: rgba(255, 255, 255, 0.72);
}

.section-card {
  margin-top: 16px;
}

.header-tip {
  font-size: 12px;
  font-weight: 400;
}

/* 应用入口卡片 */
.app-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 12px;
}

.app-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.app-card:hover {
  border-color: var(--el-color-primary);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.15);
  transform: translateY(-2px);
}

.app-card.disabled {
  cursor: not-allowed;
  opacity: 0.75;
}

.app-card.disabled:hover {
  border-color: #ebeef5;
  box-shadow: none;
  transform: none;
}

.app-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  border-radius: 10px;
  background-color: #ecf5ff;
  color: var(--el-color-primary);
}

.app-body {
  flex: 1;
  min-width: 0;
}

.app-name {
  font-weight: 600;
}

.app-id {
  margin-top: 2px;
  font-size: 12px;
}

.stat-row {
  display: flex;
  gap: 16px;
  margin: 16px 0;
}

.stat-card {
  flex: 1;
  text-align: center;
  /* 统计行内 flex gap 已提供间距；抵消全局 .el-card + .el-card 的 margin-top，避免横向卡片错位 */
  margin-top: 0;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--el-color-primary);
}

.stat-value.online {
  color: var(--el-color-danger);
}

.stat-value.success {
  color: var(--el-color-success);
}

.stat-label {
  margin-top: 6px;
  color: #909399;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
}

.client-tag {
  margin: 2px 6px 2px 0;
}

.sso-alert {
  margin-top: 16px;
}

code {
  font-family: Consolas, Monaco, monospace;
}
</style>
