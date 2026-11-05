<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>
        <div class="table-toolbar">
          <span style="font-weight: 600">操作日志（本地审计，仅记录管理动作）</span>
          <div class="toolbar-left">
            <el-input v-model="query.operUserName" placeholder="操作人" clearable style="width: 120px"
                      @keyup.enter="loadLogs(1)" />
            <el-input v-model="query.module" placeholder="模块" clearable style="width: 150px; margin-left: 8px"
                      @keyup.enter="loadLogs(1)" />
            <el-select v-model="query.success" placeholder="结果" clearable style="width: 100px; margin-left: 8px">
              <el-option label="成功" :value="1" />
              <el-option label="失败" :value="0" />
            </el-select>
            <el-date-picker
              v-model="timeRange"
              type="datetimerange"
              range-separator="至"
              start-placeholder="开始时间"
              end-placeholder="结束时间"
              value-format="YYYY-MM-DD HH:mm:ss"
              style="margin-left: 8px; width: 360px"
            />
            <el-button style="margin-left: 8px" @click="loadLogs(1)">查询</el-button>
          </div>
        </div>
      </template>

      <el-table :data="logs" v-loading="loading">
        <el-table-column prop="createTime" label="时间" width="170" />
        <el-table-column prop="operUserName" label="操作人" width="100" />
        <el-table-column prop="module" label="模块" min-width="140" />
        <el-table-column prop="action" label="动作" min-width="110" />
        <el-table-column label="请求" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ row.method }}</template>
        </el-table-column>
        <el-table-column label="结果" width="80">
          <template #default="{ row }">
            <el-tag :type="row.success === 1 ? 'success' : 'danger'">
              {{ row.success === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="costMs" label="耗时(ms)" width="90" align="center" />
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="showDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="pagination"
        layout="total, prev, pager, next, sizes"
        :total="total"
        :current-page="query.current"
        :page-size="query.size"
        @current-change="(page) => loadLogs(page)"
        @size-change="(size) => { query.size = size; loadLogs(1) }"
      />
    </el-card>

    <el-dialog v-model="detailVisible" title="操作日志详情" width="640px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="时间">{{ current.createTime }}</el-descriptions-item>
        <el-descriptions-item label="操作人">
          {{ current.operUserName }}（{{ current.operUserId }}）
        </el-descriptions-item>
        <el-descriptions-item label="模块/动作">{{ current.module }} / {{ current.action }}</el-descriptions-item>
        <el-descriptions-item label="请求">{{ current.method }}</el-descriptions-item>
        <el-descriptions-item label="结果">
          <el-tag :type="current.success === 1 ? 'success' : 'danger'">
            {{ current.success === 1 ? '成功' : '失败' }}
          </el-tag>
          <span style="margin-left: 8px">{{ current.message }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="耗时">{{ current.costMs }} ms</el-descriptions-item>
        <el-descriptions-item label="IP">{{ current.ip }}</el-descriptions-item>
        <el-descriptions-item label="请求参数">
          <pre class="params-pre">{{ formatParams(current.params) }}</pre>
        </el-descriptions-item>
        <el-descriptions-item label="修改前数据">
          <pre class="params-pre before-pre">{{ formatParams(current.beforeData) }}</pre>
        </el-descriptions-item>
        <el-descriptions-item label="修改后数据">
          <pre class="params-pre after-pre">{{ formatParams(current.afterData) }}</pre>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { operLogApi } from '../../api'

const logs = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ current: 1, size: 10, operUserName: '', module: '', success: undefined })
const timeRange = ref(null)
const detailVisible = ref(false)
const current = ref({})

const beginTime = computed(() => (timeRange.value ? timeRange.value[0] : undefined))
const endTime = computed(() => (timeRange.value ? timeRange.value[1] : undefined))

async function loadLogs(page) {
  query.current = page || query.current
  loading.value = true
  try {
    const res = await operLogApi.page({
      operUserName: query.operUserName || undefined,
      module: query.module || undefined,
      success: query.success,
      beginTime: beginTime.value,
      endTime: endTime.value,
      current: query.current,
      size: query.size
    })
    logs.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

function showDetail(row) {
  current.value = row
  detailVisible.value = true
}

function formatParams(params) {
  if (!params) {
    return '-'
  }
  try {
    return JSON.stringify(JSON.parse(params), null, 2)
  } catch (e) {
    return params
  }
}

onMounted(loadLogs)
</script>

<style scoped>
.toolbar-left {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
}

.pagination {
  margin-top: 14px;
  justify-content: flex-end;
}

.params-pre {
  margin: 0;
  max-height: 260px;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-all;
  font-size: 12px;
}

/* 修改前（红底）/ 修改后（绿底）数据快照 */
.before-pre {
  background: #fef0f0;
  padding: 6px;
  border-radius: 4px;
}

.after-pre {
  background: #f0f9eb;
  padding: 6px;
  border-radius: 4px;
}
</style>
