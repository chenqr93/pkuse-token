<template>
  <div class="page-container">
    <!-- 说明 -->
    <el-alert type="info" :closable="false" class="tip-alert">
      <template #title>
        接入应用对应统一登录（SSO）中的客户端：新增 / 修改 / 启用 / 禁用后<b>立即同步到认证中心</b>，无需重启服务。
        应用跳转 <code>/sso/auth?client=应用标识&amp;redirect=回调地址</code> 即可发起统一登录。
      </template>
    </el-alert>

    <!-- 查询条件 -->
    <el-card shadow="never" class="toolbar-card">
      <div class="table-toolbar">
        <el-form :inline="true" :model="query" @submit.prevent>
          <el-form-item label="应用标识">
            <el-input v-model.trim="query.clientId" placeholder="如 sso-client1" clearable style="width: 180px" @keyup.enter="onSearch" />
          </el-form-item>
          <el-form-item label="应用名称">
            <el-input v-model.trim="query.clientName" placeholder="应用名称" clearable style="width: 160px" @keyup.enter="onSearch" />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
              <el-option label="启用" :value="1" />
              <el-option label="禁用" :value="0" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="onSearch">
              <el-icon><Search /></el-icon>查询
            </el-button>
            <el-button @click="onReset">重置</el-button>
          </el-form-item>
        </el-form>
        <el-button type="primary" @click="openCreate">
          <el-icon><Plus /></el-icon>新增应用
        </el-button>
      </div>
    </el-card>

    <!-- 应用列表 -->
    <el-card shadow="never" class="table-card">
      <el-table :data="records" v-loading="loading" stripe>
        <el-table-column prop="clientId" label="应用标识" min-width="130" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="text-mono">{{ row.clientId }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="clientName" label="应用名称" min-width="130" show-overflow-tooltip>
          <template #default="{ row }">
            <span>{{ row.clientName }}</span>
            <el-tooltip v-if="row.remark" :content="row.remark" placement="top">
              <el-icon class="remark-icon"><InfoFilled /></el-icon>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column label="授权回调地址" min-width="170" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="text-mono text-muted">{{ row.allowUrl || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="应用入口地址" min-width="190" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.entryUrl" class="text-mono text-muted">{{ row.entryUrl }}</span>
            <span v-else class="text-muted">未配置</span>
          </template>
        </el-table-column>
        <el-table-column label="接口秘钥" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="text-mono">{{ maskSecret(row.secretKey) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="已授权用户" width="100" align="center">
          <template #default="{ row }">{{ row.authorizedUserCount ?? 0 }}</template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 1"
              :loading="togglingId === row.id"
              @change="onToggleStatus(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="onDelete(row)">删除</el-button>
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
        :page-sizes="[10, 20, 50]"
        @current-change="onPageChange"
        @size-change="onSizeChange"
      />
    </el-card>

    <!-- 新增 / 编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="form.id ? '编辑应用' : '新增应用'"
      width="620px"
      :close-on-click-modal="false"
      @closed="onDialogClosed"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="应用标识" prop="clientId">
          <el-input v-model.trim="form.clientId" placeholder="SSO client 参数，如 sso-client1" :disabled="!!form.id" maxlength="64" />
        </el-form-item>
        <el-form-item label="应用名称" prop="clientName">
          <el-input v-model.trim="form.clientName" placeholder="应用名称" maxlength="128" />
        </el-form-item>
        <el-form-item label="回调地址" prop="allowUrl">
          <el-input
            v-model.trim="form.allowUrl"
            type="textarea"
            :rows="2"
            placeholder="允许的授权回调地址，多个用英文逗号分隔；* 表示放行全部"
            maxlength="512"
          />
          <div class="form-tip text-muted">
            示例：<span class="text-mono">http://localhost:9001/sso/login</span>　或　<span class="text-mono">*</span>
          </div>
        </el-form-item>
        <el-form-item label="应用入口" prop="entryUrl">
          <el-input
            v-model.trim="form.entryUrl"
            placeholder="工作台点击进入应用时跳转的目标地址，留空则工作台不可进入"
            maxlength="255"
          />
          <div class="form-tip text-muted">
            通常填应用的登录入口，如 <span class="text-mono">http://localhost:9001/sso/login?back=http://localhost:9001/</span>
          </div>
        </el-form-item>
        <el-form-item label="接口秘钥" prop="secretKey">
          <div class="secret-row">
            <el-input v-model.trim="form.secretKey" placeholder="SSO 参数签名 / 消息推送校验使用" maxlength="128" />
            <el-button @click="generateSecret">随机生成</el-button>
          </div>
        </el-form-item>
        <el-form-item label="消息推送">
          <el-switch v-model="form.isPush" :active-value="1" :inactive-value="0" active-text="开启" inactive-text="关闭" />
        </el-form-item>
        <el-form-item v-if="form.isPush === 1" label="推送地址" prop="pushUrl">
          <el-input v-model.trim="form.pushUrl" placeholder="如 http://localhost:9003/sso/pushC" maxlength="255" />
        </el-form-item>
        <el-form-item label="单点注销">
          <el-switch v-model="form.isSlo" :active-value="1" :inactive-value="0" active-text="参与" inactive-text="不参与" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="禁用" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="255" placeholder="备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { clientApi } from '../../api'

const loading = ref(false)
const saving = ref(false)
const togglingId = ref(null)
const records = ref([])
const total = ref(0)

const query = reactive({ current: 1, size: 10, clientId: '', clientName: '', status: null })

const dialogVisible = ref(false)
const formRef = ref(null)
const form = reactive({
  id: null,
  clientId: '',
  clientName: '',
  allowUrl: '*',
  entryUrl: '',
  secretKey: '',
  isPush: 0,
  pushUrl: '/sso/pushC',
  isSlo: 1,
  status: 1,
  remark: ''
})

const rules = {
  clientId: [
    { required: true, message: '请输入应用标识', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_-]{2,64}$/, message: '应用标识仅支持字母、数字、下划线、中划线', trigger: 'blur' }
  ],
  clientName: [{ required: true, message: '请输入应用名称', trigger: 'blur' }],
  entryUrl: [
    {
      validator: (rule, value, callback) => {
        if (!value) {
          callback()
          return
        }
        if (!value.startsWith('http://') && !value.startsWith('https://')) {
          callback(new Error('应用入口地址需以 http:// 或 https:// 开头'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ],
  allowUrl: [
    {
      validator: (rule, value, callback) => {
        if (!value) {
          callback()
          return
        }
        const items = value.split(',').map((item) => item.trim()).filter(Boolean)
        for (const url of items) {
          if (url === '*') continue
          if (!url.startsWith('http://') && !url.startsWith('https://')) {
            callback(new Error('回调地址需以 http:// 或 https:// 开头，或填写 *'))
            return
          }
          const starIndex = url.indexOf('*')
          if (starIndex !== -1 && starIndex !== url.length - 1) {
            callback(new Error('回调地址中的 * 只能出现在地址末尾'))
            return
          }
        }
        callback()
      },
      trigger: 'blur'
    }
  ]
}

onMounted(loadData)

async function loadData() {
  loading.value = true
  try {
    const res = await clientApi.page({
      current: query.current,
      size: query.size,
      clientId: query.clientId || undefined,
      clientName: query.clientName || undefined,
      status: query.status === null || query.status === '' ? undefined : query.status
    })
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
  query.clientId = ''
  query.clientName = ''
  query.status = null
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

function resetForm() {
  form.id = null
  form.clientId = ''
  form.clientName = ''
  form.allowUrl = '*'
  form.entryUrl = ''
  form.secretKey = ''
  form.isPush = 0
  form.pushUrl = '/sso/pushC'
  form.isSlo = 1
  form.status = 1
  form.remark = ''
}

function openCreate() {
  resetForm()
  form.secretKey = randomSecret()
  dialogVisible.value = true
}

function openEdit(row) {
  resetForm()
  Object.assign(form, {
    id: row.id,
    clientId: row.clientId,
    clientName: row.clientName,
    allowUrl: row.allowUrl,
    entryUrl: row.entryUrl || '',
    secretKey: row.secretKey,
    isPush: row.isPush === 1 ? 1 : 0,
    pushUrl: row.pushUrl || '/sso/pushC',
    isSlo: row.isSlo === 1 ? 1 : 0,
    status: row.status,
    remark: row.remark
  })
  dialogVisible.value = true
}

function onDialogClosed() {
  formRef.value?.clearValidate()
}

function randomSecret() {
  const chars = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789'
  let text = ''
  for (let i = 0; i < 32; i++) {
    text += chars.charAt(Math.floor(Math.random() * chars.length))
  }
  return text
}

function generateSecret() {
  form.secretKey = randomSecret()
}

async function onSave() {
  try {
    await formRef.value.validate()
  } catch (e) {
    return
  }
  saving.value = true
  try {
    const payload = {
      clientId: form.clientId,
      clientName: form.clientName,
      allowUrl: form.allowUrl,
      entryUrl: form.entryUrl,
      secretKey: form.secretKey,
      isPush: form.isPush,
      pushUrl: form.pushUrl,
      isSlo: form.isSlo,
      status: form.status,
      remark: form.remark
    }
    if (form.id) {
      await clientApi.update({ ...payload, id: form.id })
      ElMessage.success('保存成功，已同步到认证中心')
    } else {
      await clientApi.create(payload)
      ElMessage.success('新增成功，已同步到认证中心')
    }
    dialogVisible.value = false
    loadData()
  } catch (e) {
    // 错误提示已由 axios 拦截器统一处理
  } finally {
    saving.value = false
  }
}

async function onToggleStatus(row) {
  const target = row.status === 1 ? 0 : 1
  const action = target === 1 ? '启用' : '禁用'
  try {
    await ElMessageBox.confirm(
      `${action}后该应用${target === 1 ? '可以' : '将无法'}发起统一登录，确定要${action}「${row.clientName}」吗？`,
      '提示',
      { confirmButtonText: `确定${action}`, cancelButtonText: '取消', type: 'warning' }
    )
  } catch (e) {
    return
  }
  togglingId.value = row.id
  try {
    await clientApi.changeStatus(row.id, target)
    ElMessage.success(`已${action}`)
    loadData()
  } catch (e) {
    // 错误提示已由 axios 拦截器统一处理
  } finally {
    togglingId.value = null
  }
}

async function onDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定要删除应用「${row.clientName}」吗？删除后该应用将无法发起统一登录。`,
      '危险操作',
      { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'error' }
    )
  } catch (e) {
    return
  }
  try {
    await clientApi.remove(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (e) {
    // 错误提示已由 axios 拦截器统一处理
  }
}

function maskSecret(secret) {
  if (!secret) return '-'
  if (secret.length <= 10) return secret
  return secret.substring(0, 6) + '****' + secret.substring(secret.length - 4)
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

.secret-row {
  display: flex;
  gap: 8px;
  width: 100%;
}

.form-tip {
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.6;
}

.remark-icon {
  margin-left: 4px;
  color: var(--el-color-info);
  vertical-align: -2px;
  cursor: help;
}
</style>
