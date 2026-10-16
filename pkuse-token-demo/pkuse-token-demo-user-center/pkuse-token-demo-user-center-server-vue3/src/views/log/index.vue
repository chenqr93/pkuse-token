<template>
  <div class="page-container">
    <!-- 说明 -->
    <el-alert type="info" :closable="false" class="tip-alert">
      <template #title>
        登录日志<b>按月分表</b>存储（表名 <code>uc_login_log_yyyyMM</code>，KingbaseES 声明式分区，自动建表）；
        此处的查询会自动跨月分区，可按账号、结果、时间范围筛选。
      </template>
    </el-alert>

    <!-- 查询条件 -->
    <el-card shadow="never" class="toolbar-card">
      <div class="table-toolbar">
        <el-form :inline="true" :model="query" @submit.prevent>
          <el-form-item label="账号">
            <el-input v-model.trim="query.username" placeholder="登录账号" clearable style="width: 150px" @keyup.enter="onSearch" />
          </el-form-item>
          <el-form-item label="结果">
            <el-select v-model="query.result" placeholder="全部" clearable style="width: 150px">
              <el-option v-for="item in resultOptions" :key="item.value" :label="item.label" :value="item.value" />
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
        <el-table-column prop="username" label="账号" min-width="110" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="text-mono">{{ row.username || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="80">
          <template #default="{ row }">
            <el-tag size="small" :type="row.loginType === 'logout' ? 'info' : ''" effect="plain">
              {{ row.loginType === 'logout' ? '注销' : '登录' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="结果" width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="resultTagType(row.result)">{{ resultLabel(row.result) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="msg" label="说明" min-width="200" show-overflow-tooltip />
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
        <el-table-column label="设备指纹" width="150" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="text-mono text-muted">{{ maskDevice(row.deviceId) }}</span>
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
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { loginLogApi } from '../../api'

const loading = ref(false)
const records = ref([])
const total = ref(0)

const query = reactive({ current: 1, size: 10, username: '', result: '', range: null })

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

onMounted(loadData)

async function loadData() {
  loading.value = true
  try {
    const params = {
      current: query.current,
      size: query.size,
      username: query.username || undefined,
      result: query.result || undefined
    }
    if (query.range && query.range.length === 2) {
      params.begin = query.range[0]
      params.end = query.range[1]
    }
    const res = await loginLogApi.page(params)
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

function resultLabel(result) {
  const item = resultOptions.find((option) => option.value === result)
  return item ? item.label : result
}

function resultTagType(result) {
  if (result === 'success' || result === 'device_rebind') return 'success'
  if (result === 'fail' || result === 'no_user') return 'warning'
  if (result === 'locked' || result === 'device_lock') return 'danger'
  if (result === 'captcha_fail' || result === 'device_verify') return 'info'
  return 'info'
}

function maskDevice(deviceId) {
  if (!deviceId) return '-'
  return deviceId.length > 16 ? deviceId.substring(0, 12) + '…' : deviceId
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

code {
  font-family: Consolas, Monaco, monospace;
}
</style>
