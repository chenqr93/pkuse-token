<template>
  <div class="pd-page">
    <!-- 说明 -->
    <a-alert type="info" :closable="false" show-icon class="tip-alert">
      <template slot="message">
        操作日志<b>按月分表</b>存储（表名 <code>uc_oper_log_yyyyMM</code>，KingbaseES 声明式分区，自动建表）；
        记录用户新增/修改/删除、应用维护、授权、在线解锁、解绑设备等管理动作，敏感参数已脱敏；
        新增记录<b>传参 + 修改后数据</b>，更新记录<b>传参 + 修改前数据 + 修改后数据</b>，删除记录<b>传参 + 删除前数据</b>。
      </template>
    </a-alert>

    <!-- 查询条件 -->
    <div class="pd-search-card">
      <div class="pd-toolbar">
        <a-form-model layout="inline" :model="query" @submit.prevent>
          <a-form-model-item label="操作人">
            <a-input v-model.trim="query.username" placeholder="操作人账号" allow-clear style="width: 140px" @keyup.enter="onSearch" />
          </a-form-model-item>
          <a-form-model-item label="模块">
            <a-select v-model="query.module" placeholder="全部" allow-clear style="width: 150px">
              <a-select-option v-for="item in moduleOptions" :key="item" :value="item">
                {{ item }}
              </a-select-option>
            </a-select>
          </a-form-model-item>
          <a-form-model-item label="动作">
            <a-input v-model.trim="query.action" placeholder="如：新增用户" allow-clear style="width: 150px" @keyup.enter="onSearch" />
          </a-form-model-item>
          <a-form-model-item label="结果">
            <a-select v-model="query.result" placeholder="全部" allow-clear style="width: 120px">
              <a-select-option value="success">成功</a-select-option>
              <a-select-option value="fail">失败</a-select-option>
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
        :scroll="{ x: 1280 }"
      >
        <template slot="createTime" slot-scope="text, record">{{ formatTime(record.createTime) }}</template>
        <template slot="username" slot-scope="text, record">
          <span class="text-mono">{{ record.username || '-' }}</span>
        </template>
        <template slot="module" slot-scope="text, record">
          <a-tag class="pd-tag-blue">{{ record.module || '-' }}</a-tag>
        </template>
        <template slot="result" slot-scope="text, record">
          <a-tag :class="record.result === 'success' ? 'pd-tag-green' : 'pd-tag-red'">
            {{ record.result === 'success' ? '成功' : '失败' }}
          </a-tag>
        </template>
        <template slot="costTime" slot-scope="text, record">
          <span class="text-mono text-muted">{{ record.costTime != null ? record.costTime + ' ms' : '-' }}</span>
        </template>
        <template slot="ip" slot-scope="text, record">
          <span class="text-mono">{{ record.ip || '-' }}</span>
        </template>
        <template slot="deviceEnv" slot-scope="text, record">
          <span class="text-mono text-muted">{{ record.os }} / {{ record.browser }}</span>
        </template>
        <template slot="msg" slot-scope="text, record">
          <span :class="{ 'text-danger': record.result === 'fail' }">{{ record.msg || '-' }}</span>
        </template>
        <template slot="action" slot-scope="text, record">
          <a-button type="link" @click="showDetail(record)">详情</a-button>
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

    <!-- 日志详情 -->
    <a-modal
      :visible="detailVisible"
      title="操作日志详情"
      width="640px"
      :footer="null"
      @cancel="detailVisible = false"
    >
      <a-descriptions :column="2" bordered size="small">
        <a-descriptions-item label="操作人">{{ detail.username || '-' }}</a-descriptions-item>
        <a-descriptions-item label="模块">{{ detail.module || '-' }}</a-descriptions-item>
        <a-descriptions-item label="动作">{{ detail.action || '-' }}</a-descriptions-item>
        <a-descriptions-item label="结果">
          <a-tag :class="detail.result === 'success' ? 'pd-tag-green' : 'pd-tag-red'">
            {{ detail.result === 'success' ? '成功' : '失败' }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="请求方法" :span="2">
          <span class="text-mono">{{ detail.method || '-' }}</span>
        </a-descriptions-item>
        <a-descriptions-item label="IP">{{ detail.ip || '-' }}</a-descriptions-item>
        <a-descriptions-item label="耗时">{{ detail.costTime != null ? detail.costTime + ' ms' : '-' }}</a-descriptions-item>
        <a-descriptions-item label="操作系统">{{ detail.os || '-' }}</a-descriptions-item>
        <a-descriptions-item label="浏览器">{{ detail.browser || '-' }}</a-descriptions-item>
        <a-descriptions-item label="时间" :span="2">{{ formatTime(detail.createTime) }}</a-descriptions-item>
        <a-descriptions-item v-if="detail.msg" label="说明" :span="2">
          <span :class="{ 'text-danger': detail.result === 'fail' }">{{ detail.msg }}</span>
        </a-descriptions-item>
        <a-descriptions-item label="请求参数" :span="2">
          <pre class="params-pre">{{ prettyParams(detail.params) }}</pre>
        </a-descriptions-item>
        <a-descriptions-item v-if="detail.beforeData" label="修改前数据" :span="2">
          <pre class="params-pre before-pre">{{ prettyParams(detail.beforeData) }}</pre>
        </a-descriptions-item>
        <a-descriptions-item v-if="detail.afterData" label="修改后数据" :span="2">
          <pre class="params-pre after-pre">{{ prettyParams(detail.afterData) }}</pre>
        </a-descriptions-item>
        <a-descriptions-item label="User-Agent" :span="2">
          <span class="text-mono text-muted">{{ detail.userAgent || '-' }}</span>
        </a-descriptions-item>
      </a-descriptions>
    </a-modal>
  </div>
</template>

<script>
import { operLogApi } from '../../api'

/** 模块字典（与后端 @OperLog(module=...) 保持一致） */
const moduleOptions = ['用户管理', '应用管理', '授权管理', '设备管理', '登录安全']

export default {
  name: 'OperLog',
  data() {
    return {
      moduleOptions,
      loading: false,
      records: [],
      total: 0,
      detailVisible: false,
      detail: {},
      query: { current: 1, size: 10, username: '', module: '', action: '', result: '', range: null },
      sizeChangeLock: false,
      columns: [
        {
          title: '时间',
          dataIndex: 'createTime',
          width: 170,
          scopedSlots: { customRender: 'createTime' }
        },
        {
          title: '操作人',
          dataIndex: 'username',
          width: 120,
          ellipsis: true,
          scopedSlots: { customRender: 'username' }
        },
        {
          title: '模块',
          dataIndex: 'module',
          width: 110,
          scopedSlots: { customRender: 'module' }
        },
        { title: '动作', dataIndex: 'action', width: 140, ellipsis: true },
        {
          title: '结果',
          dataIndex: 'result',
          width: 90,
          scopedSlots: { customRender: 'result' }
        },
        {
          title: '耗时',
          dataIndex: 'costTime',
          width: 100,
          scopedSlots: { customRender: 'costTime' }
        },
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
          title: '说明',
          dataIndex: 'msg',
          width: 170,
          ellipsis: true,
          scopedSlots: { customRender: 'msg' }
        },
        {
          title: '操作',
          key: 'action',
          width: 90,
          fixed: 'right',
          scopedSlots: { customRender: 'action' }
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
          module: this.query.module || undefined,
          action: this.query.action || undefined,
          result: this.query.result || undefined
        }
        if (this.query.range && this.query.range.length === 2) {
          params.begin = this.query.range[0]
          params.end = this.query.range[1]
        }
        const res = await operLogApi.page(params)
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
      this.query.module = ''
      this.query.action = ''
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
    showDetail(row) {
      this.detail = row
      this.detailVisible = true
    },
    prettyParams(params) {
      if (!params) return '-'
      try {
        // 19 位雪花 ID 超出 JS Number 安全整数范围，先转成字符串再格式化，避免展示精度丢失
        const safe = params.replace(/(:\s*)(\d{16,})\b/g, '$1"$2"')
        return JSON.stringify(JSON.parse(safe), null, 2)
      } catch (e) {
        return params
      }
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

.params-pre {
  margin: 0;
  max-height: 260px;
  overflow: auto;
  font-family: Consolas, Monaco, monospace;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-all;
}

.before-pre {
  background-color: fade(@pd-color-error, 12%);
}

.after-pre {
  background-color: fade(@pd-color-success, 12%);
}

code {
  font-family: Consolas, Monaco, monospace;
}
</style>
