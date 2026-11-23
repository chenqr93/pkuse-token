<template>
  <div class="pd-page">
    <!-- 欢迎卡片 -->
    <a-card :bordered="false" class="welcome-card">
      <div class="welcome">
        <a-avatar :size="52" class="welcome-avatar">{{ displayName.charAt(0) }}</a-avatar>
        <div class="welcome-text">
          <div class="welcome-title">欢迎回来，{{ displayName }}！</div>
          <div class="welcome-sub">
            上次登录：{{ formatTime(currentUser.lastLoginTime) }}
            <template v-if="overview.serverTime">　|　当前时间：{{ overview.serverTime }}</template>
          </div>
        </div>
        <div v-if="isAdmin" class="welcome-actions">
          <a-button type="primary" @click="$router.push('/user')">
            <a-icon type="user" />用户管理
          </a-button>
          <a-button @click="$router.push('/client')">
            <a-icon type="appstore" />应用管理
          </a-button>
        </div>
      </div>
    </a-card>

    <!-- 我的应用（应用入口） -->
    <a-card :bordered="false" class="section-card">
      <template slot="title">
        <div class="card-header">
          <span>
            我的应用
            <span class="text-muted header-tip">
              （{{ isAdmin ? '系统管理员可进入全部应用' : '仅展示已授权应用，点击进入' }}）
            </span>
          </span>
          <a-button type="link" @click="loadData">刷新</a-button>
        </div>
      </template>
      <a-spin :spinning="loading">
        <a-empty
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
              <a-icon type="appstore" style="font-size: 22px" />
            </div>
            <div class="app-body">
              <div class="app-name">{{ app.clientName }}</div>
              <div class="app-id text-mono text-muted">{{ app.clientId }}</div>
            </div>
            <a-tag :class="app.entryUrl ? 'pd-tag-green' : 'pd-tag-gray'">
              {{ app.entryUrl ? '进入应用' : '未配置入口' }}
            </a-tag>
          </div>
        </div>
      </a-spin>
    </a-card>

    <!-- 以下仅系统管理员可见 -->
    <template v-if="isAdmin">
      <div class="stat-row">
        <a-card v-for="stat in statList" :key="stat.label" :bordered="false" class="stat-card">
          <a-statistic :title="stat.label" :value="stat.value" :class="['stat-value', stat.tone]" />
        </a-card>
      </div>

      <a-row :gutter="16">
        <!-- 已接入应用 -->
        <a-col :span="15">
          <a-card :bordered="false">
            <template slot="title">
              <div class="card-header">
                <span>已接入应用</span>
                <a-button type="link" @click="loadData">刷新</a-button>
              </div>
            </template>
            <a-table
              :data-source="myApps"
              :columns="appColumns"
              :pagination="false"
              row-key="clientId"
              size="middle"
            >
              <template slot="clientId" slot-scope="text">
                <span class="text-mono">{{ text }}</span>
              </template>
              <template slot="allowUrl" slot-scope="text">
                <span class="text-mono text-muted">{{ text || '-' }}</span>
              </template>
              <template slot="status" slot-scope="text, record">
                <a-tag v-if="registeredClients.indexOf(record.clientId) >= 0" class="pd-tag-green">
                  已同步到认证中心
                </a-tag>
                <a-tag v-else class="pd-tag-orange">待同步</a-tag>
              </template>
              <template slot="action" slot-scope="text, record">
                <a-button type="link" :disabled="!record.entryUrl" @click="enterApp(record)">进入</a-button>
              </template>
            </a-table>
          </a-card>
        </a-col>

        <!-- 认证中心信息 -->
        <a-col :span="9">
          <a-card :bordered="false">
            <template slot="title">
              <div class="card-header">
                <span>统一登录认证中心</span>
                <a-tag class="pd-tag-green">运行中</a-tag>
              </div>
            </template>
            <a-descriptions :column="1" bordered size="small">
              <a-descriptions-item label="认证中心地址">
                <span class="text-mono">{{ ssoServerUrl }}</span>
              </a-descriptions-item>
              <a-descriptions-item label="授权地址">
                <span class="text-mono">/sso/auth</span>
              </a-descriptions-item>
              <a-descriptions-item label="Ticket 有效期">
                {{ overview.ssoServer ? overview.ssoServer.ticketTimeout : '-' }} 秒
              </a-descriptions-item>
              <a-descriptions-item label="参数签名校验">
                <a-tag :class="overview.ssoServer && overview.ssoServer.checkSign ? 'pd-tag-green' : 'pd-tag-orange'">
                  {{ overview.ssoServer && overview.ssoServer.checkSign ? '已开启' : '演示环境已跳过' }}
                </a-tag>
              </a-descriptions-item>
              <a-descriptions-item label="已注册应用">
                <a-tag v-for="id in registeredClients" :key="id" class="client-tag pd-tag-blue">{{ id }}</a-tag>
                <span v-if="!registeredClients.length" class="text-muted">暂无</span>
              </a-descriptions-item>
            </a-descriptions>
            <a-alert type="info" :closable="false" class="sso-alert">
              <template slot="message">
                普通用户仅能进入已授权应用（登录时校验授权）；应用跳转
                <code>/sso/auth?client=应用标识&amp;redirect=回调地址</code>
              </template>
            </a-alert>
          </a-card>
        </a-col>
      </a-row>
    </template>
  </div>
</template>

<script>
import { dashboardApi } from '../../api'
import { getStoredUser, isAdmin as isAdminUser, setStoredUser } from '../../utils/auth'

export default {
  name: 'WorkbenchPage',
  data() {
    return {
      loading: false,
      overview: {},
      myApps: [],
      currentUser: getStoredUser() || {},
      appColumns: [
        {
          title: '应用标识',
          dataIndex: 'clientId',
          key: 'clientId',
          width: 150,
          ellipsis: true,
          scopedSlots: { customRender: 'clientId' }
        },
        {
          title: '应用名称',
          dataIndex: 'clientName',
          key: 'clientName',
          width: 150,
          ellipsis: true
        },
        {
          title: '回调地址',
          dataIndex: 'allowUrl',
          key: 'allowUrl',
          ellipsis: true,
          scopedSlots: { customRender: 'allowUrl' }
        },
        {
          title: '状态',
          key: 'status',
          width: 160,
          scopedSlots: { customRender: 'status' }
        },
        {
          title: '操作',
          key: 'action',
          width: 90,
          align: 'center',
          scopedSlots: { customRender: 'action' }
        }
      ]
    }
  },
  computed: {
    isAdmin() {
      return isAdminUser()
    },
    displayName() {
      return this.currentUser.nickname || this.currentUser.username || '用户'
    },
    ssoServerUrl() {
      return window.location.origin
    },
    registeredClients() {
      return this.overview.registeredClients || []
    },
    statList() {
      const overview = this.overview || {}
      return [
        { label: '当前在线', value: this.statValue(overview.onlineUserCount), tone: 'online' },
        { label: '用户总数', value: this.statValue(overview.userCount), tone: '' },
        { label: '启用用户', value: this.statValue(overview.enabledUserCount), tone: 'success' },
        { label: '接入应用', value: this.statValue(overview.clientCount), tone: '' },
        { label: '启用应用', value: this.statValue(overview.enabledClientCount), tone: 'success' }
      ]
    }
  },
  mounted() {
    this.loadData()
  },
  methods: {
    async loadData() {
      this.loading = true
      try {
        // 我的应用入口（所有用户）
        const myAppsRes = await dashboardApi.myApps()
        this.myApps = myAppsRes.data || []
        // 总览数据（仅系统管理员；普通用户不请求）
        if (this.isAdmin) {
          const overviewRes = await dashboardApi.overview()
          this.overview = overviewRes.data || {}
          if (this.overview.currentUser) {
            this.currentUser = this.overview.currentUser
            setStoredUser(this.currentUser)
          }
        }
      } catch (e) {
        // 错误提示已由 axios 拦截器统一处理
      } finally {
        this.loading = false
      }
    },

    /** 统计数值：无值时展示 '-' */
    statValue(value) {
      return value === null || value === undefined ? '-' : value
    },

    /** 进入应用：跳转认证中心授权地址，由认证中心下发 ticket 后回到应用 */
    enterApp(app) {
      if (!app.entryUrl) {
        this.$message.warning('该应用未配置入口地址，请联系管理员')
        return
      }
      const url = `${this.ssoServerUrl}/sso/auth?client=${encodeURIComponent(app.clientId)}`
        + `&redirect=${encodeURIComponent(app.entryUrl)}&mode=ticket`
      window.location.href = url
    },

    formatTime(value) {
      if (!value) return '-'
      return String(value).replace('T', ' ').substring(0, 19)
    }
  }
}
</script>

<style lang="less" scoped>
@import '../../styles/tokens.less';

.welcome-card {
  background: linear-gradient(120deg, @pd-color-menu-sub-bg 0%, @pd-color-menu-bg 60%, @pd-color-primary-dark 100%);
  border: none;
}

.welcome {
  display: flex;
  align-items: center;
  gap: 16px;
}

.welcome-avatar {
  background-color: fade(@pd-color-white, 20%);
  color: @pd-color-white;
  font-size: 22px;
}

.welcome-text {
  flex: 1;
}

.welcome-title {
  display: flex;
  align-items: center;
  gap: 8px;
  color: @pd-color-white;
  font-size: 18px;
  font-weight: 600;
}

.welcome-sub {
  margin-top: 6px;
  color: fade(@pd-color-white, 72%);
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
  border: 1px solid @pd-color-border-light;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.app-card:hover {
  border-color: @pd-color-selected;
  box-shadow: 0 4px 12px fade(@pd-color-selected, 15%);
  transform: translateY(-2px);
}

.app-card.disabled {
  cursor: not-allowed;
  opacity: 0.75;
}

.app-card.disabled:hover {
  border-color: @pd-color-border-light;
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
  background-color: @pd-color-card-tint;
  color: @pd-color-selected;
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
}

.stat-value /deep/ .ant-statistic-title {
  margin-bottom: 6px;
  color: @pd-color-text-muted;
  font-size: 14px;
}

.stat-value /deep/ .ant-statistic-content {
  font-size: 28px;
  font-weight: 700;
  color: @pd-color-selected;
}

.stat-value.online /deep/ .ant-statistic-content {
  color: @pd-color-danger-deep;
}

.stat-value.success /deep/ .ant-statistic-content {
  color: @pd-color-success;
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
