<template>
  <div class="page-container">
    <!-- 查询条件 -->
    <el-card shadow="never">
      <div class="table-toolbar">
        <el-form :inline="true" :model="query" @submit.prevent>
          <el-form-item label="账号">
            <el-input v-model.trim="query.username" placeholder="登录账号" clearable style="width: 160px" @keyup.enter="onSearch" />
          </el-form-item>
          <el-form-item label="昵称">
            <el-input v-model.trim="query.nickname" placeholder="昵称" clearable style="width: 160px" @keyup.enter="onSearch" />
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
        <div class="toolbar-actions">
          <el-button
            type="primary"
            plain
            :disabled="selectedRows.length === 0"
            @click="openBatchGrant"
          >
            <el-icon><Grid /></el-icon>批量授权{{ selectedRows.length ? `（${selectedRows.length}）` : '' }}
          </el-button>
          <el-button type="primary" @click="openCreate">
            <el-icon><Plus /></el-icon>新增用户
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 用户列表 -->
    <el-card shadow="never" class="table-card">
      <el-table :data="records" v-loading="loading" stripe @selection-change="onSelectionChange">
        <el-table-column type="selection" width="46" fixed="left" />
        <el-table-column prop="username" label="账号" min-width="110" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="text-mono">{{ row.username }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="nickname" label="昵称" min-width="110" show-overflow-tooltip />
        <el-table-column label="角色" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="row.role === 'admin' ? 'danger' : 'info'">
              {{ row.role === 'admin' ? '管理员' : '普通用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 1"
              :loading="togglingId === row.id"
              @change="onToggleStatus(row)"
            />
            <el-tooltip
              v-if="isLocked(row)"
              :content="`登录锁定：${row.lockReason || '连续密码错误'}（至 ${formatTime(row.lockedUntil)}）`"
              placement="top"
            >
              <el-tag size="small" type="danger" class="lock-tag">已锁定</el-tag>
            </el-tooltip>
            <el-tooltip
              v-else-if="row.mustChangePassword === 1"
              content="开启「首次登录强制修改密码」后，该用户下次登录需先修改密码"
              placement="top"
            >
              <el-tag size="small" type="warning" class="lock-tag">需改密</el-tag>
            </el-tooltip>
            <el-tooltip
              v-else-if="row.loginFailCount > 0"
              :content="`连续登录失败 ${row.loginFailCount} 次`"
              placement="top"
            >
              <el-tag size="small" type="warning" class="lock-tag">失败{{ row.loginFailCount }}</el-tag>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column prop="email" label="邮箱" min-width="130" show-overflow-tooltip />
        <el-table-column label="最后登录" width="150">
          <template #default="{ row }">{{ formatTime(row.lastLoginTime) }}</template>
        </el-table-column>
        <el-table-column label="授权应用 / 设备" width="140" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.authClientCount > 0" size="small" type="success">{{ row.authClientCount }} 应用</el-tag>
            <span v-else class="text-muted">未授权</span>
            <el-tag size="small" type="info" class="device-count">{{ row.deviceCount || 0 }} 设备</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="primary" @click="openGrant(row)">授权</el-button>
            <el-button link type="primary" @click="openDevices(row)">设备</el-button>
            <el-button v-if="isLocked(row)" link type="warning" @click="onUnlock(row)">解锁</el-button>
            <el-button link type="primary" @click="onResetPassword(row)">重置</el-button>
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
      :title="form.id ? '编辑用户' : '新增用户'"
      width="560px"
      :close-on-click-modal="false"
      @closed="onDialogClosed"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="登录账号" prop="username">
          <el-input v-model.trim="form.username" placeholder="登录账号（唯一）" maxlength="64" />
        </el-form-item>
        <el-form-item v-if="!form.id" label="初始密码" prop="password">
          <el-input
            v-model="form.password"
            :placeholder="`不填写则使用系统默认密码 ${passwordPolicy.defaultPassword || '123456'}`"
            maxlength="64"
            show-password
          />
          <div class="field-tip text-muted">
            密码规则：{{ passwordPolicy.text || '长度不少于 6 位' }}
            <template v-if="passwordPolicy.complexityEnabled">
              ；规则由「规则配置」维护
            </template>
          </div>
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model.trim="form.nickname" placeholder="昵称" maxlength="64" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model.trim="form.email" placeholder="邮箱" maxlength="128" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model.trim="form.phone" placeholder="手机号" maxlength="32" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="form.role" style="width: 100%">
            <el-option label="普通用户" value="user" />
            <el-option label="管理员" value="admin" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="禁用" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="255" placeholder="备注" />
        </el-form-item>
      </el-form>
      <div v-if="form.id" class="dialog-meta text-muted">
        创建时间：{{ formatTime(form.createTime) }}
      </div>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 授权应用弹窗 -->
    <el-dialog
      v-model="grantVisible"
      :title="`授权应用 - ${grantUser.nickname || grantUser.username || ''}`"
      width="560px"
      :close-on-click-modal="false"
    >
      <el-alert type="info" :closable="false" class="grant-tip">
        <template #title>
          勾选该用户可访问的应用：普通用户登录后工作台<b>只展示已授权应用</b>，且只能进入已授权应用（系统管理员不受限）。
        </template>
      </el-alert>
      <div v-loading="grantLoading" class="grant-body">
        <el-checkbox-group v-if="grantApps.length" v-model="grantChecked">
          <el-checkbox v-for="app in grantApps" :key="app.clientId" :value="app.clientId" class="grant-item">
            {{ app.clientName }}
            <span class="text-mono text-muted">（{{ app.clientId }}）</span>
          </el-checkbox>
        </el-checkbox-group>
        <el-empty v-else description="暂无可授权的应用（请先启用应用）" :image-size="60" />
      </div>
      <template #footer>
        <el-button @click="grantChecked = []">清空</el-button>
        <el-button @click="grantChecked = grantApps.map((app) => app.clientId)">全选</el-button>
        <el-button @click="grantVisible = false">取消</el-button>
        <el-button type="primary" :loading="grantSaving" @click="onSaveGrant">保存</el-button>
      </template>
    </el-dialog>

    <!-- 批量授权弹窗 -->
    <el-dialog
      v-model="batchGrantVisible"
      title="批量授权应用"
      width="580px"
      :close-on-click-modal="false"
    >
      <el-alert type="info" :closable="false" class="grant-tip">
        <template #title>
          已选择 <b>{{ selectedRows.length }}</b> 个用户：
          <span class="text-mono">{{ selectedUserNames }}</span>
        </template>
      </el-alert>

      <el-form label-width="90px">
        <el-form-item label="授权模式">
          <el-radio-group v-model="batchGrantMode">
            <el-radio value="append">追加授权（保留原有）</el-radio>
            <el-radio value="cover">覆盖授权（重置为勾选集合）</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="应用">
          <div class="batch-apps" v-loading="batchGrantLoading">
            <el-checkbox-group v-if="grantApps.length" v-model="batchGrantChecked">
              <el-checkbox v-for="app in grantApps" :key="app.clientId" :value="app.clientId" class="grant-item">
                {{ app.clientName }}
                <span class="text-mono text-muted">（{{ app.clientId }}）</span>
              </el-checkbox>
            </el-checkbox-group>
            <el-empty v-else description="暂无可授权的应用（请先启用应用）" :image-size="60" />
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="batchGrantChecked = []">清空</el-button>
        <el-button @click="batchGrantChecked = grantApps.map((app) => app.clientId)">全选</el-button>
        <el-button @click="batchGrantVisible = false">取消</el-button>
        <el-button type="primary" :loading="batchGrantSaving" @click="onSaveBatchGrant">保存</el-button>
      </template>
    </el-dialog>

    <!-- 设备管理弹窗（设备锁 / 设备指纹） -->
    <el-dialog
      v-model="deviceVisible"
      :title="`设备管理 - ${deviceUser.nickname || deviceUser.username || ''}`"
      width="660px"
    >
      <el-alert type="info" :closable="false" class="grant-tip">
        <template #title>
          开启设备锁后，账号<b>仅在已绑定设备上允许登录</b>；解绑后该设备可重新作为新设备登录
          （登录日志页可查看每次登录的设备指纹）。
        </template>
      </el-alert>
      <el-table :data="deviceList" v-loading="deviceLoading" size="default">
        <el-table-column label="设备指纹" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="text-mono">{{ row.deviceId }}</span>
          </template>
        </el-table-column>
        <el-table-column label="环境" width="140">
          <template #default="{ row }">
            <span class="text-muted">{{ row.os }} / {{ row.browser }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="lastLoginIp" label="最近 IP" width="125" show-overflow-tooltip />
        <el-table-column label="最近登录" width="160">
          <template #default="{ row }">{{ formatTime(row.lastLoginTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="80" align="center">
          <template #default="{ row }">
            <el-button link type="danger" @click="onUnbindDevice(row)">解绑</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!deviceLoading && !deviceList.length" description="暂无已绑定设备" :image-size="60" />
      <template #footer>
        <el-button @click="deviceVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { clientApi, configApi, userApi } from '../../api'

const loading = ref(false)
const saving = ref(false)
const togglingId = ref(null)
const records = ref([])
const total = ref(0)

const query = reactive({ current: 1, size: 10, username: '', nickname: '', status: null })

const dialogVisible = ref(false)
const formRef = ref(null)

// 密码规则（规则配置 - 密码规则：默认密码与复杂度，用于表单提示）
const passwordPolicy = ref({})

// 授权应用弹窗
const grantVisible = ref(false)
const grantLoading = ref(false)
const grantSaving = ref(false)
const grantUser = ref({})
const grantApps = ref([])
const grantChecked = ref([])

// 设备管理弹窗
const deviceVisible = ref(false)
const deviceLoading = ref(false)
const deviceUser = ref({})
const deviceList = ref([])

// 批量授权
const selectedRows = ref([])
const batchGrantVisible = ref(false)
const batchGrantLoading = ref(false)
const batchGrantSaving = ref(false)
const batchGrantMode = ref('append')
const batchGrantChecked = ref([])
const selectedUserNames = computed(() =>
  selectedRows.value.map((row) => row.nickname || row.username).join('、')
)

const form = reactive({
  id: null,
  username: '',
  password: '',
  nickname: '',
  email: '',
  phone: '',
  role: 'user',
  status: 1,
  remark: '',
  createTime: null
})

const rules = {
  username: [
    { required: true, message: '请输入登录账号', trigger: 'blur' },
    { min: 2, max: 64, message: '账号长度 2 ~ 64 位', trigger: 'blur' }
  ],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }]
}

onMounted(async () => {
  await loadData()
  // 密码规则（默认密码 / 复杂度）用于新增与重置密码提示
  try {
    const res = await configApi.passwordRule()
    passwordPolicy.value = res.data || {}
  } catch (e) {
    // 忽略：提示信息获取失败不影响用户管理
  }
})

async function loadData() {
  loading.value = true
  try {
    const res = await userApi.page({
      current: query.current,
      size: query.size,
      username: query.username || undefined,
      nickname: query.nickname || undefined,
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
  query.username = ''
  query.nickname = ''
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
  form.username = ''
  form.password = ''
  form.nickname = ''
  form.email = ''
  form.phone = ''
  form.role = 'user'
  form.status = 1
  form.remark = ''
  form.createTime = null
}

function openCreate() {
  resetForm()
  dialogVisible.value = true
}

function openEdit(row) {
  resetForm()
  Object.assign(form, {
    id: row.id,
    username: row.username,
    nickname: row.nickname,
    email: row.email,
    phone: row.phone,
    role: row.role || 'user',
    status: row.status,
    remark: row.remark,
    createTime: row.createTime
  })
  dialogVisible.value = true
}

function onDialogClosed() {
  formRef.value?.clearValidate()
}

async function onSave() {
  try {
    await formRef.value.validate()
  } catch (e) {
    return
  }
  saving.value = true
  try {
    if (form.id) {
      await userApi.update({
        id: form.id,
        username: form.username,
        nickname: form.nickname,
        email: form.email,
        phone: form.phone,
        role: form.role,
        status: form.status,
        remark: form.remark
      })
      ElMessage.success('保存成功')
    } else {
      await userApi.create({
        username: form.username,
        password: form.password || undefined,
        nickname: form.nickname,
        email: form.email,
        phone: form.phone,
        role: form.role,
        status: form.status,
        remark: form.remark
      })
      ElMessage.success(form.password
        ? '新增成功'
        : `新增成功，初始密码为 ${passwordPolicy.defaultPassword || '123456'}`)
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
    await ElMessageBox.confirm(`确定要${action}用户「${row.nickname || row.username}」吗？`, '提示', {
      confirmButtonText: `确定${action}`,
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch (e) {
    return
  }
  togglingId.value = row.id
  try {
    await userApi.changeStatus(row.id, target)
    ElMessage.success(`已${action}`)
    loadData()
  } catch (e) {
    // 错误提示已由 axios 拦截器统一处理
  } finally {
    togglingId.value = null
  }
}

async function onResetPassword(row) {
  let password = ''
  const ruleText = passwordPolicy.value.text || '密码长度不少于 6 位'
  try {
    const { value } = await ElMessageBox.prompt(
      `为「${row.nickname || row.username}」设置新密码（${ruleText}）`,
      '重置密码',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputType: 'password',
        inputValidator: (value) => validateByPasswordRule(value, ruleText),
        inputErrorMessage: ruleText
      }
    )
    password = value
  } catch (e) {
    return
  }
  try {
    await userApi.resetPassword(row.id, password)
    ElMessage.success('密码已重置')
  } catch (e) {
    // 错误提示已由 axios 拦截器统一处理
  }
}

/** 按「规则配置 - 密码规则」做前端预校验（服务端仍会做权威校验） */
function validateByPasswordRule(value, ruleText) {
  if (!value) {
    return '请输入新密码'
  }
  const rule = passwordPolicy.value
  const minLength = rule.minLength || 6
  if (value.length < minLength) {
    return `密码长度不能小于 ${minLength} 位`
  }
  if (rule.complexityEnabled) {
    if (rule.requireUppercase && !/[A-Z]/.test(value)) {
      return '密码必须包含大写字母'
    }
    if (rule.requireLowercase && !/[a-z]/.test(value)) {
      return '密码必须包含小写字母'
    }
    if (rule.requireDigit && !/[0-9]/.test(value)) {
      return '密码必须包含数字'
    }
    if (rule.requireSpecial && !/[^A-Za-z0-9]/.test(value)) {
      return '密码必须包含特殊字符'
    }
  }
  return true
}

async function onDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定要删除用户「${row.nickname || row.username}」吗？删除后不可恢复，其应用授权也将一并清除。`,
      '危险操作',
      { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'error' }
    )
  } catch (e) {
    return
  }
  try {
    await userApi.remove(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (e) {
    // 错误提示已由 axios 拦截器统一处理
  }
}

/* ------------------------- 授权应用 / 批量授权 ------------------------- */

/** 表格选中项变化 */
function onSelectionChange(rows) {
  selectedRows.value = rows || []
}

async function openGrant(row) {
  grantUser.value = row
  grantChecked.value = []
  grantVisible.value = true
  grantLoading.value = true
  try {
    // 仅启用的应用可授权
    const [appsRes, checkedRes] = await Promise.all([clientApi.list(), userApi.listClients(row.id)])
    grantApps.value = (appsRes.data || []).filter((app) => app.status === 1)
    grantChecked.value = checkedRes.data || []
  } catch (e) {
    // 错误提示已由 axios 拦截器统一处理
  } finally {
    grantLoading.value = false
  }
}

async function onSaveGrant() {
  grantSaving.value = true
  try {
    await userApi.bindClients(grantUser.value.id, grantChecked.value)
    ElMessage.success('授权已保存')
    grantVisible.value = false
    loadData()
  } catch (e) {
    // 错误提示已由 axios 拦截器统一处理
  } finally {
    grantSaving.value = false
  }
}

/** 打开批量授权弹窗 */
async function openBatchGrant() {
  if (!selectedRows.value.length) {
    ElMessage.warning('请先勾选需要授权的用户')
    return
  }
  batchGrantMode.value = 'append'
  batchGrantChecked.value = []
  batchGrantVisible.value = true
  batchGrantLoading.value = true
  try {
    const appsRes = await clientApi.list()
    grantApps.value = (appsRes.data || []).filter((app) => app.status === 1)
  } catch (e) {
    // 错误提示已由 axios 拦截器统一处理
  } finally {
    batchGrantLoading.value = false
  }
}

/** 保存批量授权 */
async function onSaveBatchGrant() {
  const userIds = selectedRows.value.map((row) => row.id)
  if (batchGrantMode.value === 'cover' && batchGrantChecked.value.length === 0) {
    try {
      await ElMessageBox.confirm(
        `覆盖模式下未勾选任何应用，将清空这 ${userIds.length} 个用户的全部应用授权，确定继续吗？`,
        '提示',
        { confirmButtonText: '确定清空', cancelButtonText: '取消', type: 'warning' }
      )
    } catch (e) {
      return
    }
  }
  batchGrantSaving.value = true
  try {
    const res = await userApi.batchBindClients({
      userIds,
      clientIds: batchGrantChecked.value,
      mode: batchGrantMode.value
    })
    const action = batchGrantMode.value === 'append' ? '追加授权' : '覆盖授权'
    ElMessage.success(
      `已为 ${res.userCount ?? userIds.length} 个用户${action} ${batchGrantChecked.value.length} 个应用`
    )
    batchGrantVisible.value = false
    loadData()
  } catch (e) {
    // 错误提示已由 axios 拦截器统一处理
  } finally {
    batchGrantSaving.value = false
  }
}

/* ------------------------- 登录锁定 / 设备管理 ------------------------- */

/** 是否处于登录锁定期 */
function isLocked(row) {
  return !!row.lockedUntil && new Date(row.lockedUntil).getTime() > Date.now()
}

/** 管理员在线解锁 */
async function onUnlock(row) {
  try {
    await ElMessageBox.confirm(
      `确定解锁账号「${row.nickname || row.username}」吗？解锁后将清除失败计数，账号可立即重新登录。`,
      '在线解锁',
      { confirmButtonText: '确定解锁', cancelButtonText: '取消', type: 'warning' }
    )
  } catch (e) {
    return
  }
  try {
    await userApi.unlock(row.id)
    ElMessage.success('已解锁')
    loadData()
  } catch (e) {
    // 错误提示已由 axios 拦截器统一处理
  }
}

/** 打开设备管理弹窗 */
async function openDevices(row) {
  deviceUser.value = row
  deviceList.value = []
  deviceVisible.value = true
  deviceLoading.value = true
  try {
    const res = await userApi.listDevices(row.id)
    deviceList.value = res.data || []
  } catch (e) {
    // 错误提示已由 axios 拦截器统一处理
  } finally {
    deviceLoading.value = false
  }
}

/** 解绑设备 */
async function onUnbindDevice(device) {
  try {
    await ElMessageBox.confirm(
      `确定解绑设备「${device.deviceId}」吗？解绑后该设备可重新作为新设备登录。`,
      '解绑设备',
      { confirmButtonText: '确定解绑', cancelButtonText: '取消', type: 'warning' }
    )
  } catch (e) {
    return
  }
  try {
    await userApi.unbindDevice(deviceUser.value.id, device.deviceId)
    ElMessage.success('设备已解绑')
    await openDevices(deviceUser.value)
    loadData()
  } catch (e) {
    // 错误提示已由 axios 拦截器统一处理
  }
}

function formatTime(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').substring(0, 19)
}
</script>

<style scoped>
.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}

.dialog-meta {
  margin-top: 4px;
  text-align: right;
  font-size: 12px;
}

.field-tip {
  width: 100%;
  margin-top: 2px;
  font-size: 12px;
  line-height: 1.6;
}

.lock-tag {
  margin-left: 6px;
  vertical-align: -2px;
}

.device-count {
  margin-left: 6px;
}

.grant-tip {
  margin-bottom: 14px;
}

.grant-body {
  min-height: 60px;
  max-height: 320px;
  overflow-y: auto;
}

.grant-item {
  display: flex;
  margin: 0 0 10px 0;
}

/* 工具栏右侧按钮组 */
.toolbar-actions {
  display: flex;
  gap: 10px;
}

/* 批量授权：应用勾选列表 */
.batch-apps {
  width: 100%;
  max-height: 240px;
  overflow-y: auto;
}
</style>
