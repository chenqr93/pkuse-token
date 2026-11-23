<template>
  <div class="pd-page">
    <!-- 说明 -->
    <a-alert type="info" :closable="false" show-icon class="tip-alert">
      <template slot="message">
        登录日志<b>按月分表</b>存储（表名 <code>uc_login_log_yyyyMM</code>，KingbaseES 声明式分区，自动建表）；
        此处的查询会自动跨月分区，可按账号、结果、时间范围筛选。
      </template>
    </a-alert>

    <!-- 查询条件 -->
    <div class="pd-search-card">
      <div class="pd-toolbar">
        <a-form-model layout="inline" :model="query" @submit.prevent>
          <a-form-model-item label="账号">
            <a-input v-model.trim="query.username" placeholder="登录账号" allow-clear style="width: 150px" @keyup.enter="onSearch" />
          </a-form-model-item>
          <a-form-model-item label="结果">
            <a-select v-model="query.result" placeholder="全部" allow-clear style="width: 150px">
              <a-select-option v-for="item in resultOptions" :key="item.value" :value="item.value">
                {{ item.label }}
              </a-select-option>
            </a-select>
          </a-form-model-item>
          <a-form-model-item label="时间">
            <a-range-picker
              v-model="query.range"
              show-time
              value-format="YYYY-MM-DD HH:mm:ss"
              format="YYYY-MM-DD HH:mm:ss"
              separator="至"
              :placeholder="['开始时间', '结束时间']"
              style="width: 340px"
            />
          </a-form-model-item>
          <a-form-model-item>
            <a-button type="primary" @click="onSearch">
              <a-icon type="search" /><span>查询</span>
            </a-button>
            <a-button @click="onReset">重置</a-button>
          </a-form-model-item>
        </a-form-model>
        <div class="pd-toolbar-right">
          <a-button @click="loadData">
            <a-icon type="reload" /><span>刷新</span>
          </a-button>
        </div>
      </div>
    </div>

    <!-- 日志列表 -->
    <div class="pd-table-card">
      <a-table
        :columns="columns"
        :data-source="records"
        :loading="loading"
        :row-key="'id'"
        :pagination="false"
        :scroll="{ x: 1170 }"
      >
        <template slot="createTime" slot-scope="text, record">{{ formatTime(record.createTime) }}</template>
        <template slot="username" slot-scope="text, record">
          <span class="text-mono">{{ record.username || '-' }}</span>
        </template>
        <template slot="loginType" slot-scope="text, record">
          <a-tag :class="loginTypeTagClass(record.loginType)">
            {{ record.loginType === 'logout' ? '注销' : '登录' }}
          </a-tag>
        </template>
        <template slot="result" slot-scope="text, record">
          <a-tag :class="resultTagClass(record.result)">{{ resultLabel(record.result) }}</a-tag>
        </template>
        <template slot="ip" slot-scope="text, record">
          <span class="text-mono">{{ record.ip || '-' }}</span>
        </template>
        <template slot="deviceEnv" slot-scope="text, record">
          <span class="text-mono text-muted">{{ record.os }} / {{ record.browser }}</span>
        </template>
        <template slot="deviceId" slot-scope="text, record">
          <span class="text-mono text-muted">{{ maskDevice(record.deviceId) }}</span>
        </template>
      </a-table>

      <a-pagination
        class="pd-pagination"
        :current="query.current"
        :page-size="query.size"
        :total="total"
        :show-total="showTotalText"
        show-size-changer
        show-quick-jumper
        :page-size-options="['10', '20', '50', '100']"
        @change="onPageChange"
        @showSizeChange="onSizeChange"
      />
    </div>
  </div>
</template>

<script>
import { loginLogApi } from '../../api'

const resultOptions = [
  { label: '登录成功', value: 'success' },
  { label: '密码错误', value: 'fail' },
  { label: '账号锁定', value: 'locked' },
  { label: '验证码错误', value: 'captcha_fail' },
  { label: '设备锁拦截', value: 'device_lock' },
  { label: '设备验证', value: 'device_verify' },
  { label: '设备换绑', value: 'device_rebind' },
  { label: '账号禁用', value: 'disabled' },
  { label: '账号不存在', value: 'no_user' }
]

export default {
  name: 'LoginLog',
  data() {
    return {
      resultOptions,
      loading: false,
      records: [],
      total: 0,
      query: { current: 1, size: 10, username: '', result: '', range: null },
      sizeChangeLock: false,
      columns: [
        {
          title: '时间',
          dataIndex: 'createTime',
          width: 170,
          scopedSlots: { customRender: 'createTime' }
        },
        {
          title: '账号',
          dataIndex: 'username',
          width: 120,
          ellipsis: true,
          scopedSlots: { customRender: 'username' }
        },
        {
          title: '类型',
          dataIndex: 'loginType',
          width: 90,
          scopedSlots: { customRender: 'loginType' }
        },
        {
          title: '结果',
          dataIndex: 'result',
          width: 120,
          scopedSlots: { customRender: 'result' }
        },
        { title: '说明', dataIndex: 'msg', width: 220, ellipsis: true },
        {
          title: 'IP',
          dataIndex: 'ip',
          width: 140,
          scopedSlots: { customRender: 'ip' }
        },
        {
          title: '设备',
          dataIndex: 'os',
          width: 150,
          scopedSlots: { customRender: 'deviceEnv' }
        },
        {
          title: '设备指纹',
          dataIndex: 'deviceId',
          width: 160,
          ellipsis: true,
          scopedSlots: { customRender: 'deviceId' }
        }
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
        const params = {
          current: this.query.current,
          size: this.query.size,
          username: this.query.username || undefined,
          result: this.query.result || undefined
        }
        if (this.query.range && this.query.range.length === 2) {
          params.begin = this.query.range[0]
          params.end = this.query.range[1]
        }
        const res = await loginLogApi.page(params)
        this.records = res.data?.records || []
        this.total = res.data?.total || 0
      } catch (e) {
        // 错误提示已由 axios 拦截器统一处理
      } finally {
        this.loading = false
      }
    },
    onSearch() {
      this.query.current = 1
      this.loadData()
    },
    onReset() {
      this.query.username = ''
      this.query.result = ''
      this.query.range = null
      this.onSearch()
    },
    showTotalText(total) {
      return `共 ${total} 条`
    },
    onPageChange(page, pageSize) {
      if (this.sizeChangeLock) {
        return
      }
      this.query.current = page
      if (pageSize && pageSize !== this.query.size) {
        this.query.size = pageSize
      }
      this.loadData()
    },
    onSizeChange(current, size) {
      this.sizeChangeLock = true
      const nextSize = Number(size)
      this.query.size = Number.isNaN(nextSize) ? this.query.size : nextSize
      this.query.current = 1
      this.loadData()
      this.$nextTick(() => {
        this.sizeChangeLock = false
      })
    },
    resultLabel(result) {
      const item = resultOptions.find((option) => option.value === result)
      return item ? item.label : result
    },
    resultTagClass(result) {
      if (result === 'success' || result === 'device_rebind') return 'pd-tag-green'
      if (result === 'fail' || result === 'no_user') return 'pd-tag-orange'
      if (result === 'locked' || result === 'device_lock') return 'pd-tag-red'
      if (result === 'captcha_fail' || result === 'device_verify') return 'pd-tag-gray'
      return 'pd-tag-gray'
    },
    loginTypeTagClass(loginType) {
      return loginType === 'logout' ? 'pd-tag-gray' : 'pd-tag-blue'
    },
    maskDevice(deviceId) {
      if (!deviceId) return '-'
      return deviceId.length > 16 ? deviceId.substring(0, 12) + '…' : deviceId
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

.tip-alert {
  margin-bottom: @pd-space-lg;
}

code {
  font-family: Consolas, Monaco, monospace;
}
</style>
