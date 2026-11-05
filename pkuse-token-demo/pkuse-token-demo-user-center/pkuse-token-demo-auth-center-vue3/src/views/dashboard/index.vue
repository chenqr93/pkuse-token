<template>
  <div class="page-container">
    <!-- 统计卡片 -->
    <el-row :gutter="16">
      <el-col :span="6">
        <el-card shadow="hover">
          <el-statistic title="接入应用（启用）" :value="overview.appCount || 0" />
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <el-statistic title="菜单总数" :value="overview.menuCount || 0" />
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <el-statistic title="角色总数" :value="overview.roleCount || 0" />
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <el-statistic title="已授权用户数" :value="overview.userCount || 0" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 应用列表（注册表实时 + 本地授权统计） -->
    <el-card class="table-card" shadow="never">
      <template #header>
        <div class="table-toolbar">
          <div>
            <span style="font-weight: 600">应用列表</span>
            <el-tag size="small" class="margin-left" :type="source === 'live' ? 'success' : 'warning'">
              {{ source === 'live' ? '实时数据' : '缓存数据（用户中心不可用）' }}
            </el-tag>
            <span class="text-muted margin-left" style="font-size: 12px">
              注册表更新于：{{ lastRefreshTime || '-' }}
            </span>
          </div>
          <el-button type="primary" :loading="refreshing" @click="refreshRegistry">
            <el-icon><Refresh /></el-icon>&nbsp;刷新应用
          </el-button>
        </div>
      </template>

      <el-table :data="apps" v-loading="loading" row-key="clientId">
        <el-table-column prop="clientId" label="应用标识" min-width="130" />
        <el-table-column prop="clientName" label="应用名称" min-width="160" show-overflow-tooltip />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="menuCount" label="菜单数" width="80" align="center" />
        <el-table-column prop="roleCount" label="角色数" width="80" align="center" />
        <el-table-column prop="userCount" label="授权用户数" width="100" align="center" />
        <el-table-column prop="entryUrl" label="应用入口" min-width="200" show-overflow-tooltip />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :disabled="row.status !== 1" @click="goMenu(row)">
              菜单管理
            </el-button>
            <el-button link type="primary" :disabled="row.status !== 1" @click="goRole(row)">
              角色管理
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
    <!-- 孤儿数据清理（V2） -->
    <el-card class="table-card" shadow="never">
      <template #header>
        <div class="table-toolbar">
          <div>
            <span style="font-weight: 600">孤儿数据清理</span>
            <span class="text-muted margin-left" style="font-size: 12px">
              应用/用户在用户中心删除后，本地残留的授权数据（菜单/角色/绑定）
            </span>
          </div>
          <div>
            <el-button :loading="scanning" @click="scanCleanup">扫描</el-button>
            <el-button type="danger" :disabled="!hasOrphan" :loading="cleaning" @click="executeCleanup">
              清理孤儿数据
            </el-button>
          </div>
        </div>
      </template>
      <template v-if="cleanupResult">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="孤儿应用（菜单/角色残留）">
            <template v-if="cleanupResult.orphanClients && cleanupResult.orphanClients.length">
              <el-tag v-for="c in cleanupResult.orphanClients" :key="c" type="danger" size="small" class="role-tag">{{ c }}</el-tag>
            </template>
            <span v-else class="text-muted">无</span>
          </el-descriptions-item>
          <el-descriptions-item label="孤儿用户（绑定残留）">
            <template v-if="cleanupResult.orphanUsers && cleanupResult.orphanUsers.length">
              <el-tag v-for="u in cleanupResult.orphanUsers" :key="u" type="danger" size="small" class="role-tag">{{ u }}</el-tag>
            </template>
            <span v-else class="text-muted">无</span>
          </el-descriptions-item>
        </el-descriptions>
        <div v-if="cleanupResult.unknownUsers && cleanupResult.unknownUsers.length" class="text-muted" style="font-size: 12px; margin-top: 8px">
          用户中心不可用，{{ cleanupResult.unknownUsers.length }} 个用户的绑定未探测（不参与清理）
        </div>
      </template>
      <div v-else class="text-muted">点击「扫描」检查本地授权数据中引用了已删除应用/用户的残留记录。</div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { appApi, overviewApi, cleanupApi } from '../../api'

const router = useRouter()

const overview = ref({})
const apps = ref([])
const source = ref('live')
const lastRefreshTime = ref('')
const loading = ref(false)
const refreshing = ref(false)

// 孤儿数据清理（V2）
const cleanupResult = ref(null)
const scanning = ref(false)
const cleaning = ref(false)
const hasOrphan = computed(() =>
  !!cleanupResult.value &&
  ((cleanupResult.value.orphanClients && cleanupResult.value.orphanClients.length > 0) ||
   (cleanupResult.value.orphanUsers && cleanupResult.value.orphanUsers.length > 0)))

async function scanCleanup() {
  scanning.value = true
  try {
    const res = await cleanupApi.scan()
    cleanupResult.value = res.data || {}
    ElMessage.success(hasOrphan.value ? '扫描完成：发现孤儿数据' : '扫描完成：无孤儿数据')
  } finally {
    scanning.value = false
  }
}

async function executeCleanup() {
  try {
    await ElMessageBox.confirm('将删除孤儿应用的菜单/角色（级联绑定）与孤儿用户的绑定，确定执行？', '清理确认', { type: 'warning' })
  } catch (e) {
    return
  }
  cleaning.value = true
  try {
    const res = await cleanupApi.execute()
    ElMessage.success(res.msg || '清理完成')
    cleanupResult.value = null
    await Promise.all([loadOverview(), loadApps()])
  } finally {
    cleaning.value = false
  }
}

async function loadOverview() {
  const res = await overviewApi.overview()
  overview.value = res.data || {}
}

async function loadApps() {
  loading.value = true
  try {
    const res = await appApi.list()
    // 统一信封：应用列表与注册表状态收在 data 内（data={apps, source, ssoMode, lastRefreshTime}）
    apps.value = res.data?.apps || []
    source.value = res.data?.source || 'live'
    lastRefreshTime.value = res.data?.lastRefreshTime || ''
  } finally {
    loading.value = false
  }
}

async function refreshRegistry() {
  refreshing.value = true
  try {
    const res = await appApi.refresh()
    ElMessage.success(res.msg || '注册表已刷新')
    await Promise.all([loadOverview(), loadApps()])
  } finally {
    refreshing.value = false
  }
}

function goMenu(row) {
  router.push({ path: '/menu', query: { clientId: row.clientId } })
}

function goRole(row) {
  router.push({ path: '/role', query: { clientId: row.clientId } })
}

onMounted(() => {
  loadOverview()
  loadApps()
})
</script>

<style scoped>
.margin-left {
  margin-left: 8px;
}
</style>
