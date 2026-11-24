<template>
  <div class="pd-page">
    <!-- 统计卡片 -->
    <div class="stat-row">
      <a-card :bordered="false" class="stat-card">
        <a-statistic title="接入应用（启用）" :value="overview.appCount || 0" class="stat-value" />
      </a-card>
      <a-card :bordered="false" class="stat-card">
        <a-statistic title="菜单总数" :value="overview.menuCount || 0" class="stat-value" />
      </a-card>
      <a-card :bordered="false" class="stat-card">
        <a-statistic title="角色总数" :value="overview.roleCount || 0" class="stat-value" />
      </a-card>
      <a-card :bordered="false" class="stat-card">
        <a-statistic title="已授权用户数" :value="overview.userCount || 0" class="stat-value" />
      </a-card>
    </div>

    <!-- 应用列表（注册表实时 + 本地授权统计） -->
    <div class="pd-table-card section-card">
      <div class="pd-toolbar">
        <div class="pd-toolbar-left">
          <span class="card-title">应用列表</span>
          <a-tag :class="source === 'live' ? 'pd-tag-green' : 'pd-tag-orange'">
            {{ source === 'live' ? '实时数据' : '缓存数据（用户中心不可用）' }}
          </a-tag>
          <span class="text-muted refresh-time">注册表更新于：{{ lastRefreshTime || '-' }}</span>
        </div>
        <div class="pd-toolbar-right">
          <a-button type="primary" :loading="refreshing" @click="refreshRegistry">
            <a-icon type="reload" /><span>刷新应用</span>
          </a-button>
        </div>
      </div>

      <a-table
        :columns="appColumns"
        :data-source="apps"
        :loading="loading"
        row-key="clientId"
        :pagination="false"
        :scroll="{ x: 1120 }"
      >
        <template slot="status" slot-scope="text, record">
          <a-tag :class="record.status === 1 ? 'pd-tag-green' : 'pd-tag-gray'">
            {{ record.status === 1 ? '启用' : '禁用' }}
          </a-tag>
        </template>
        <template slot="entryUrl" slot-scope="text, record">
          <span v-if="record.entryUrl" class="text-mono text-muted">{{ record.entryUrl }}</span>
          <span v-else class="text-muted">-</span>
        </template>
        <template slot="action" slot-scope="text, record">
          <div class="table-actions">
            <a-button type="link" :disabled="record.status !== 1" @click="goMenu(record)">菜单管理</a-button>
            <a-button type="link" :disabled="record.status !== 1" @click="goRole(record)">角色管理</a-button>
          </div>
        </template>
      </a-table>
    </div>

    <!-- 孤儿数据清理（V2） -->
    <div class="pd-page-card section-card">
      <div class="pd-toolbar">
        <div class="pd-toolbar-left">
          <span class="card-title">孤儿数据清理</span>
          <span class="text-muted cleanup-tip">
            应用/用户在用户中心删除后，本地残留的授权数据（菜单/角色/绑定）
          </span>
        </div>
        <div class="pd-toolbar-right">
          <a-button :loading="scanning" @click="scanCleanup">扫描</a-button>
          <a-button type="danger" :disabled="!hasOrphan" :loading="cleaning" @click="executeCleanup">
            清理孤儿数据
          </a-button>
        </div>
      </div>

      <template v-if="cleanupResult">
        <a-descriptions :column="2" bordered size="small">
          <a-descriptions-item label="孤儿应用（菜单/角色残留）">
            <template v-if="cleanupResult.orphanClients && cleanupResult.orphanClients.length">
              <a-tag
                v-for="client in cleanupResult.orphanClients"
                :key="client"
                class="pd-tag-red role-tag"
              >{{ client }}</a-tag>
            </template>
            <span v-else class="text-muted">无</span>
          </a-descriptions-item>
          <a-descriptions-item label="孤儿用户（绑定残留）">
            <template v-if="cleanupResult.orphanUsers && cleanupResult.orphanUsers.length">
              <a-tag
                v-for="user in cleanupResult.orphanUsers"
                :key="user"
                class="pd-tag-red role-tag"
              >{{ user }}</a-tag>
            </template>
            <span v-else class="text-muted">无</span>
          </a-descriptions-item>
        </a-descriptions>
        <div
          v-if="cleanupResult.unknownUsers && cleanupResult.unknownUsers.length"
          class="text-muted unknown-tip"
        >
          用户中心不可用，{{ cleanupResult.unknownUsers.length }} 个用户的绑定未探测（不参与清理）
        </div>
      </template>
      <div v-else class="text-muted cleanup-empty">
        点击「扫描」检查本地授权数据中引用了已删除应用/用户的残留记录。
      </div>
    </div>
  </div>
</template>

<script>
import { appApi, overviewApi, cleanupApi } from '../../api'

export default {
  name: 'DashboardPage',
  data() {
    return {
      overview: {},
      apps: [],
      source: 'live',
      lastRefreshTime: '',
      loading: false,
      refreshing: false,
      // 孤儿数据清理（V2）
      cleanupResult: null,
      scanning: false,
      cleaning: false,
      appColumns: [
        { title: '应用标识', dataIndex: 'clientId', key: 'clientId', width: 140, ellipsis: true },
        { title: '应用名称', dataIndex: 'clientName', key: 'clientName', width: 170, ellipsis: true },
        {
          title: '状态',
          dataIndex: 'status',
          key: 'status',
          width: 90,
          scopedSlots: { customRender: 'status' }
        },
        { title: '菜单数', dataIndex: 'menuCount', key: 'menuCount', width: 80, align: 'center' },
        { title: '角色数', dataIndex: 'roleCount', key: 'roleCount', width: 80, align: 'center' },
        { title: '授权用户数', dataIndex: 'userCount', key: 'userCount', width: 110, align: 'center' },
        {
          title: '应用入口',
          dataIndex: 'entryUrl',
          key: 'entryUrl',
          width: 220,
          ellipsis: true,
          scopedSlots: { customRender: 'entryUrl' }
        },
        {
          title: '操作',
          key: 'action',
          width: 200,
          fixed: 'right',
          scopedSlots: { customRender: 'action' }
        }
      ]
    }
  },
  computed: {
    hasOrphan() {
      return (
        !!this.cleanupResult &&
        (((this.cleanupResult.orphanClients || []).length > 0) ||
          ((this.cleanupResult.orphanUsers || []).length > 0))
      )
    }
  },
  mounted() {
    this.loadOverview()
    this.loadApps()
  },
  methods: {
    async scanCleanup() {
      this.scanning = true
      try {
        const res = await cleanupApi.scan()
        this.cleanupResult = res.data || {}
        this.$message.success(
          this.hasOrphan ? '扫描完成：发现孤儿数据' : '扫描完成：无孤儿数据'
        )
      } catch (e) {
        // 错误提示已由 axios 拦截器统一处理
      } finally {
        this.scanning = false
      }
    },
    executeCleanup() {
      this.$confirm({
        title: '清理确认',
        content: '将删除孤儿应用的菜单/角色（级联绑定）与孤儿用户的绑定，确定执行？',
        okText: '确定',
        cancelText: '取消',
        okType: 'danger',
        onOk: () => {
          this.doExecuteCleanup()
        },
        onCancel: () => {}
      })
    },
    async doExecuteCleanup() {
      this.cleaning = true
      try {
        const res = await cleanupApi.execute()
        this.$message.success(res.msg || '清理完成')
        this.cleanupResult = null
        await Promise.all([this.loadOverview(), this.loadApps()])
      } catch (e) {
        // 错误提示已由 axios 拦截器统一处理
      } finally {
        this.cleaning = false
      }
    },
    async loadOverview() {
      try {
        const res = await overviewApi.overview()
        this.overview = res.data || {}
      } catch (e) {
        // 错误提示已由 axios 拦截器统一处理
      }
    },
    async loadApps() {
      this.loading = true
      try {
        const res = await appApi.list()
        // 统一信封：应用列表与注册表状态收在 data 内（data={apps, source, ssoMode, lastRefreshTime}）
        const data = res.data || {}
        this.apps = data.apps || []
        this.source = data.source || 'live'
        this.lastRefreshTime = data.lastRefreshTime || ''
      } catch (e) {
        // 错误提示已由 axios 拦截器统一处理
      } finally {
        this.loading = false
      }
    },
    async refreshRegistry() {
      this.refreshing = true
      try {
        const res = await appApi.refresh()
        this.$message.success(res.msg || '注册表已刷新')
        await Promise.all([this.loadOverview(), this.loadApps()])
      } catch (e) {
        // 错误提示已由 axios 拦截器统一处理
      } finally {
        this.refreshing = false
      }
    },
    goMenu(row) {
      this.$router.push({ path: '/menu', query: { clientId: row.clientId } })
    },
    goRole(row) {
      this.$router.push({ path: '/role', query: { clientId: row.clientId } })
    }
  }
}
</script>

<style lang="less" scoped>
@import '../../styles/tokens.less';

.stat-row {
  display: flex;
  gap: @pd-space-lg;
  margin-bottom: @pd-section-gap;
}

.stat-card {
  flex: 1;
  min-width: 0;
  background: @pd-color-white;
  border-radius: @pd-card-radius;
}

.stat-value /deep/ .ant-statistic-title {
  margin-bottom: 6px;
  color: @pd-color-text-muted;
  font-size: 14px;
}

.stat-value /deep/ .ant-statistic-content {
  font-size: 28px;
  font-weight: 700;
  color: @pd-btn-primary;
}

.section-card {
  margin-top: @pd-section-gap;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: @pd-color-text;
  line-height: 32px;
}

.refresh-time {
  font-size: 12px;
}

.cleanup-tip {
  font-size: 12px;
}

.table-actions {
  display: flex;
  align-items: center;
  gap: @pd-space-md;
}

.role-tag {
  margin: 2px 6px 2px 0;
}

.unknown-tip {
  margin-top: @pd-space-sm;
  font-size: 12px;
}

.cleanup-empty {
  padding-bottom: @pd-space-xs;
}
</style>
