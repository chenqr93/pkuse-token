<template>
  <div class="page-container">
    <!-- 说明 -->
    <el-alert type="info" :closable="false" class="tip-alert">
      <template #title>
        操作日志<b>按月分表</b>存储（表名 <code>uc_oper_log_yyyyMM</code>，KingbaseES 声明式分区，自动建表）；
        记录用户新增/修改/删除、应用维护、授权、在线解锁、解绑设备等管理动作，敏感参数已脱敏；
        新增记录<b>传参 + 修改后数据</b>，更新记录<b>传参 + 修改前数据 + 修改后数据</b>，删除记录<b>传参 + 删除前数据</b>。
      </template>
    </el-alert>

    <!-- 查询条件 -->
    <el-card shadow="never" class="toolbar-card">
      <div class="table-toolbar">
        <el-form :inline="true" :model="query" @submit.prevent>
          <el-form-item label="操作人">
            <el-input v-model.trim="query.username" placeholder="操作人账号" clearable style="width: 140px" @keyup.enter="onSearch" />
          </el-form-item>
          <el-form-item label="模块">
            <el-select v-model="query.module" placeholder="全部" clearable style="width: 150px">
              <el-option v-for="item in moduleOptions" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
          <el-form-item label="动作">
            <el-input v-model.trim="query.action" placeholder="如：新增用户" clearable style="width: 150px" @keyup.enter="onSearch" />
          </el-form-item>
          <el-form-item label="结果">
            <el-select v-model="query.result" placeholder="全部" clearable style="width: 120px">
              <el-option label="成功" value="success" />
              <el-option label="失败" value="fail" />
            </el-select>
          </el-form-item>
          <el-form-item label="时间">
            <el-date-picker
              v-model="query.range"
              type="datetimerange"
              range-separator="至"
              start-placeholder="开始时间"
              end-placeholder="结束时间"
              value-format="YYYY-MM-DD HH:mm:ss"
              style="width: 340px"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="onSearch">
              <el-icon><Search /></el-icon>查询
            </el-button>
            <el-button @click="onReset">重置</el-button>
          </el-form-item>
        </el-form>
        <el-button @click="loadData">
          <el-icon><Refresh /></el-icon>刷新
        </el-button>
      </div>
    </el-card>

    <!-- 日志列表 -->
    <el-card shadow="never" class="table-card">
      <el-table :data="records" v-loading="loading" stripe>
        <el-table-column label="时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column prop="username" label="操作人" min-width="110" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="text-mono">{{ row.username || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="模块" width="110">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ row.module || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="action" label="动作" width="130" show-overflow-tooltip />
        <el-table-column label="结果" width="80">
          <template #default="{ row }">
            <el-tag size="small" :type="row.result === 'success' ? 'success' : 'danger'">
              {{ row.result === 'success' ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="耗时" width="90">
          <template #default="{ row }">
            <span class="text-mono text-muted">{{ row.costTime != null ? row.costTime + ' ms' : '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="ip" label="IP" width="130">
          <template #default="{ row }">
            <span class="text-mono">{{ row.ip || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="设备" width="150">
          <template #default="{ row }">
            <span class="text-mono text-muted">{{ row.os }} / {{ row.browser }}</span>
          </template>
        </el-table-column>
        <el-table-column label="说明" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">
            <span :class="{ 'text-danger': row.result === 'fail' }">{{ row.msg || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="showDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="pagination"
        background
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        :current-page="query.current"
        :page-size="query.size"
        :page-sizes="[10, 20, 50, 100]"
        @current-change="onPageChange"
        @size-change="onSizeChange"
      />
    </el-card>

    <!-- 日志详情 -->
    <el-dialog v-model="detailVisible" title="操作日志详情" width="640px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="操作人">{{ detail.username || '-' }}</el-descriptions-item>
        <el-descriptions-item label="模块">{{ detail.module || '-' }}</el-descriptions-item>
        <el-descriptions-item label="动作">{{ detail.action || '-' }}</el-descriptions-item>
        <el-descriptions-item label="结果">
          <el-tag size="small" :type="detail.result === 'success' ? 'success' : 'danger'">
            {{ detail.result === 'success' ? '成功' : '失败' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="请求方法" :span="2">
          <span class="text-mono">{{ detail.method || '-' }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="IP">{{ detail.ip || '-' }}</el-descriptions-item>
        <el-descriptions-item label="耗时">{{ detail.costTime != null ? detail.costTime + ' ms' : '-' }}</el-descriptions-item>
        <el-descriptions-item label="操作系统">{{ detail.os || '-' }}</el-descriptions-item>
        <el-descriptions-item label="浏览器">{{ detail.browser || '-' }}</el-descriptions-item>
        <el-descriptions-item label="时间" :span="2">{{ formatTime(detail.createTime) }}</el-descriptions-item>
        <el-descriptions-item v-if="detail.msg" label="说明" :span="2">
          <span :class="{ 'text-danger': detail.result === 'fail' }">{{ detail.msg }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="请求参数" :span="2">
          <pre class="params-pre">{{ prettyParams(detail.params) }}</pre>
        </el-descriptions-item>
        <el-descriptions-item v-if="detail.beforeData" label="修改前数据" :span="2">
          <pre class="params-pre before-pre">{{ prettyParams(detail.beforeData) }}</pre>
        </el-descriptions-item>
        <el-descriptions-item v-if="detail.afterData" label="修改后数据" :span="2">
          <pre class="params-pre after-pre">{{ prettyParams(detail.afterData) }}</pre>
        </el-descriptions-item>
        <el-descriptions-item label="User-Agent" :span="2">
          <span class="text-mono text-muted">{{ detail.userAgent || '-' }}</span>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { operLogApi } from '../../api'

const loading = ref(false)
const records = ref([])
const total = ref(0)

const detailVisible = ref(false)
const detail = ref({})

const query = reactive({ current: 1, size: 10, username: '', module: '', action: '', result: '', range: null })

/** 模块字典（与后端 @OperLog(module=...) 保持一致） */
const moduleOptions = ['用户管理', '应用管理', '授权管理', '设备管理', '登录安全']

onMounted(loadData)

async function loadData() {
  loading.value = true
  try {
    const params = {
      current: query.current,
      size: query.size,
      username: query.username || undefined,
      module: query.module || undefined,
      action: query.action || undefined,
      result: query.result || undefined
    }
    if (query.range && query.range.length === 2) {
      params.begin = query.range[0]
      params.end = query.range[1]
    }
    const res = await operLogApi.page(params)
    records.value = res.data || []
    total.value = res.total || 0
  } catch (e) {
    // 错误提示已由 axios 拦截器统一处理
  } finally {
    loading.value = false
  }
}

function onSearch() {
  query.current = 1
  loadData()
}

function onReset() {
  query.username = ''
  query.module = ''
  query.action = ''
  query.result = ''
  query.range = null
  onSearch()
}

function onPageChange(page) {
  query.current = page
  loadData()
}

function onSizeChange(size) {
  query.size = size
  query.current = 1
  loadData()
}

function showDetail(row) {
  detail.value = row
  detailVisible.value = true
}

function prettyParams(params) {
  if (!params) return '-'
  try {
    // 19 位雪花 ID 超出 JS Number 安全整数范围，先转成字符串再格式化，避免展示精度丢失
    const safe = params.replace(/(:\s*)(\d{16,})\b/g, '$1"$2"')
    return JSON.stringify(JSON.parse(safe), null, 2)
  } catch (e) {
    return params
  }
}

function formatTime(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').substring(0, 19)
}
</script>

<style scoped>
.tip-alert {
  margin-bottom: 16px;
}

.toolbar-card {
  padding-bottom: 0;
}

.pagination {
  margin-top: 16px;
  justify-content: flex-end;
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
  background-color: #fdf0f0;
}

.after-pre {
  background-color: #f0f9eb;
}

.text-danger {
  color: var(--el-color-danger);
}

code {
  font-family: Consolas, Monaco, monospace;
}
</style>
