<template>
  <div class="pd-page">
    <div class="pd-section-title">操作日志（本地审计，仅记录管理动作）</div>

    <div class="pd-search-card">
      <a-form-model layout="inline" :model="query" @submit.prevent>
        <a-form-model-item label="操作人">
          <a-input
            v-model="query.operUserName"
            placeholder="操作人"
            allow-clear
            style="width: 130px"
            @keyup.enter="loadLogs(1)"
          />
        </a-form-model-item>
        <a-form-model-item label="模块">
          <a-input
            v-model="query.module"
            placeholder="模块"
            allow-clear
            style="width: 150px"
            @keyup.enter="loadLogs(1)"
          />
        </a-form-model-item>
        <a-form-model-item label="结果">
          <a-select v-model="query.success" placeholder="全部" allow-clear style="width: 110px">
            <a-select-option :value="1">成功</a-select-option>
            <a-select-option :value="0">失败</a-select-option>
          </a-select>
        </a-form-model-item>
        <a-form-model-item label="时间">
          <a-range-picker
            v-model="timeRange"
            show-time
            value-format="YYYY-MM-DD HH:mm:ss"
            format="YYYY-MM-DD HH:mm:ss"
            separator="至"
            :placeholder="['开始时间', '结束时间']"
            style="width: 360px"
          />
        </a-form-model-item>
        <a-form-model-item>
          <a-button type="primary" @click="loadLogs(1)">
            <a-icon type="search" /><span>查询</span>
          </a-button>
        </a-form-model-item>
      </a-form-model>
    </div>

    <div class="pd-table-card">
      <a-table
        :columns="columns"
        :data-source="logs"
        :loading="loading"
        row-key="id"
        :pagination="false"
        :scroll="{ x: 1040 }"
      >
        <template slot="method" slot-scope="text, record">
          <span class="text-mono">{{ record.method }}</span>
        </template>
        <template slot="success" slot-scope="text, record">
          <a-tag :class="record.success === 1 ? 'pd-tag-green' : 'pd-tag-red'">
            {{ record.success === 1 ? '成功' : '失败' }}
          </a-tag>
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
        :page-size-options="['10', '20', '50']"
        @change="onPageChange"
        @showSizeChange="onSizeChange"
      />
    </div>

    <a-modal
      :visible="detailVisible"
      title="操作日志详情"
      width="640px"
      :footer="null"
      @cancel="detailVisible = false"
    >
      <a-descriptions :column="1" bordered size="small">
        <a-descriptions-item label="时间">{{ detail.createTime }}</a-descriptions-item>
        <a-descriptions-item label="操作人">
          {{ detail.operUserName }}（{{ detail.operUserId }}）
        </a-descriptions-item>
        <a-descriptions-item label="模块/动作">
          {{ detail.module }} / {{ detail.action }}
        </a-descriptions-item>
        <a-descriptions-item label="请求">
          <span class="text-mono">{{ detail.method }}</span>
        </a-descriptions-item>
        <a-descriptions-item label="结果">
          <a-tag :class="detail.success === 1 ? 'pd-tag-green' : 'pd-tag-red'">
            {{ detail.success === 1 ? '成功' : '失败' }}
          </a-tag>
          <span class="result-message">{{ detail.message }}</span>
        </a-descriptions-item>
        <a-descriptions-item label="耗时">{{ detail.costMs }} ms</a-descriptions-item>
        <a-descriptions-item label="IP">{{ detail.ip }}</a-descriptions-item>
        <a-descriptions-item label="请求参数">
          <pre class="params-pre">{{ prettyParams(detail.params) }}</pre>
        </a-descriptions-item>
        <a-descriptions-item label="修改前数据">
          <pre class="params-pre before-pre">{{ prettyParams(detail.beforeData) }}</pre>
        </a-descriptions-item>
        <a-descriptions-item label="修改后数据">
          <pre class="params-pre after-pre">{{ prettyParams(detail.afterData) }}</pre>
        </a-descriptions-item>
      </a-descriptions>
    </a-modal>
  </div>
</template>

<script>
import { operLogApi } from '../../api'

export default {
  name: 'OperLog',
  data() {
    return {
      logs: [],
      total: 0,
      loading: false,
      sizeChangeLock: false,
      query: { current: 1, size: 10, operUserName: '', module: '', success: undefined },
      timeRange: null,
      detailVisible: false,
      detail: {},
      columns: [
        { title: '时间', dataIndex: 'createTime', key: 'createTime', width: 170 },
        { title: '操作人', dataIndex: 'operUserName', key: 'operUserName', width: 110 },
        { title: '模块', dataIndex: 'module', key: 'module', width: 150, ellipsis: true },
        { title: '动作', dataIndex: 'action', key: 'action', width: 120, ellipsis: true },
        {
          title: '请求',
          dataIndex: 'method',
          key: 'method',
          width: 190,
          ellipsis: true,
          scopedSlots: { customRender: 'method' }
        },
        {
          title: '结果',
          dataIndex: 'success',
          key: 'success',
          width: 90,
          scopedSlots: { customRender: 'success' }
        },
        { title: '耗时(ms)', dataIndex: 'costMs', key: 'costMs', width: 100, align: 'center' },
        {
          title: '操作',
          key: 'action',
          width: 80,
          fixed: 'right',
          scopedSlots: { customRender: 'action' }
        }
      ]
    }
  },
  computed: {
    beginTime() {
      return this.timeRange ? this.timeRange[0] : undefined
    },
    endTime() {
      return this.timeRange ? this.timeRange[1] : undefined
    }
  },
  mounted() {
    this.loadLogs()
  },
  methods: {
    async loadLogs(page) {
      this.query.current = page || this.query.current
      this.loading = true
      try {
        const res = await operLogApi.page({
          operUserName: this.query.operUserName || undefined,
          module: this.query.module || undefined,
          success:
            this.query.success === undefined || this.query.success === null || this.query.success === ''
              ? undefined
              : this.query.success,
          beginTime: this.beginTime,
          endTime: this.endTime,
          current: this.query.current,
          size: this.query.size
        })
        this.logs = (res.data && res.data.records) || []
        this.total = (res.data && res.data.total) || 0
      } catch (e) {
        // 错误提示已由 axios 拦截器统一处理
      } finally {
        this.loading = false
      }
    },
    showTotalText(total) {
      return '共 ' + total + ' 条'
    },
    onPageChange(page, pageSize) {
      if (this.sizeChangeLock) {
        return
      }
      this.query.current = page
      if (pageSize && Number(pageSize) !== this.query.size) {
        this.query.size = Number(pageSize)
      }
      this.loadLogs(page)
    },
    onSizeChange(current, size) {
      this.sizeChangeLock = true
      this.query.size = Number(size)
      this.query.current = 1
      this.loadLogs(1)
      this.$nextTick(() => {
        this.sizeChangeLock = false
      })
    },
    showDetail(row) {
      this.detail = row
      this.detailVisible = true
    },
    prettyParams(params) {
      if (!params) {
        return '-'
      }
      try {
        return JSON.stringify(JSON.parse(params), null, 2)
      } catch (e) {
        return params
      }
    }
  }
}
</script>

<style lang="less" scoped>
@import '../../styles/tokens.less';

.result-message {
  margin-left: @pd-space-sm;
}

.params-pre {
  margin: 0;
  max-height: 260px;
  overflow: auto;
  font-family: Consolas, Monaco, 'Courier New', monospace;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-all;
}

/* 修改前（红底）/ 修改后（绿底）数据快照 */
.before-pre {
  padding: 6px;
  border-radius: @pd-card-radius;
  background: fade(@pd-color-error, 12%);
}

.after-pre {
  padding: 6px;
  border-radius: @pd-card-radius;
  background: fade(@pd-color-success, 12%);
}
</style>
