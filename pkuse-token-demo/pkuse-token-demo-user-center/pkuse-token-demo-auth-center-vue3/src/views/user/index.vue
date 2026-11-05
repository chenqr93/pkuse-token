<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>
        <div class="table-toolbar">
          <div class="toolbar-left">
            <span style="font-weight: 600">用户授权</span>
            <span class="text-muted" style="font-size: 12px">
              用户档案实时取自用户中心；此处仅管理「应用内角色」分配
            </span>
          </div>
          <div class="toolbar-left">
            <el-input
              v-model="query.username"
              placeholder="登录账号"
              clearable
              style="width: 140px"
              @keyup.enter="loadUsers(1)"
            />
            <el-input
              v-model="query.nickname"
              placeholder="昵称"
              clearable
              style="width: 140px; margin-left: 8px"
              @keyup.enter="loadUsers(1)"
            />
            <el-select v-model="query.status" placeholder="状态" clearable style="width: 100px; margin-left: 8px">
              <el-option label="启用" :value="1" />
              <el-option label="禁用" :value="0" />
            </el-select>
            <el-button style="margin-left: 8px" @click="loadUsers(1)">查询</el-button>
          </div>
        </div>
      </template>

      <el-table :data="users" v-loading="loading">
        <el-table-column prop="username" label="登录账号" min-width="120" />
        <el-table-column prop="nickname" label="昵称" min-width="120" />
        <el-table-column label="平台角色" width="100">
          <template #default="{ row }">
            <el-tag :type="row.role === 'admin' ? 'danger' : 'info'">
              {{ row.role === 'admin' ? '管理员' : '普通用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="已分配应用角色" min-width="300">
          <template #default="{ row }">
            <template v-if="row.roleSummary && row.roleSummary.length">
              <el-tag
                v-for="(item, index) in row.roleSummary"
                :key="index"
                size="small"
                class="role-tag"
              >{{ item }}</el-tag>
            </template>
            <span v-else class="text-muted">未分配</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openAssign(row)">分配角色</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="pagination"
        layout="total, prev, pager, next, sizes"
        :total="total"
        :current-page="query.current"
        :page-size="query.size"
        @current-change="(page) => loadUsers(page)"
        @size-change="(size) => { query.size = size; loadUsers(1) }"
      />
    </el-card>

    <!-- 分配角色弹窗：按应用分组 -->
    <el-dialog
      v-model="assignVisible"
      :title="`分配角色：${currentUser.nickname || currentUser.username || ''}`"
      width="640px"
    >
      <div v-if="assignLoading" v-loading="true" style="min-height: 120px" />
      <template v-else>
        <el-alert
          v-if="userCenterDown"
          type="warning"
          :closable="false"
          show-icon
          title="用户中心不可用：无法校验应用入口授权状态（保存仍可用）"
          class="assign-tip"
        />
        <div v-for="group in groups" :key="group.clientId" class="assign-group">
          <div class="assign-group-title">
            <span style="font-weight: 600">{{ group.clientName }}</span>
            <span class="text-muted" style="font-size: 12px">（{{ group.clientId }}）</span>
            <el-tag
              v-if="group.entryGranted === false"
              size="small"
              type="warning"
              style="margin-left: 8px"
            >未开通入口（在用户中心-用户管理-授权应用开通）</el-tag>
          </div>
          <el-checkbox-group v-if="group.roles.length" v-model="selectedRoleIds">
            <el-checkbox
              v-for="role in group.roles"
              :key="role.id"
              :value="role.id"
              class="assign-role"
            >{{ role.roleName }}（{{ role.roleCode }}）</el-checkbox>
          </el-checkbox-group>
          <div v-else class="text-muted" style="font-size: 12px; padding: 4px 0">
            该应用暂无启用角色，可先到「角色管理」创建
          </div>
        </div>
      </template>
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveAssign">保存（全量覆盖）</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { userGrantApi } from '../../api'

const users = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ current: 1, size: 10, username: '', nickname: '', status: undefined })

const assignVisible = ref(false)
const assignLoading = ref(false)
const saving = ref(false)
const userCenterDown = ref(false)
const currentUser = ref({})
const groups = ref([])
const selectedRoleIds = ref([])

async function loadUsers(page) {
  query.current = page || query.current
  loading.value = true
  try {
    const res = await userGrantApi.page({
      username: query.username || undefined,
      nickname: query.nickname || undefined,
      status: query.status,
      current: query.current,
      size: query.size
    })
    users.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (e) {
    // 用户中心不可用：保持列表为空并提示（请求拦截器已弹出错误信息）
    users.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

async function openAssign(row) {
  currentUser.value = row
  assignVisible.value = true
  assignLoading.value = true
  userCenterDown.value = false
  try {
    const res = await userGrantApi.roles(row.id)
    const data = res.data || {}
    groups.value = data.groups || []
    selectedRoleIds.value = groups.value.flatMap((group) => group.boundRoleIds || [])
    userCenterDown.value = groups.value.some((group) => group.entryGranted === null)
  } finally {
    assignLoading.value = false
  }
}

async function saveAssign() {
  saving.value = true
  try {
    const res = await userGrantApi.bindRoles(currentUser.value.id, selectedRoleIds.value)
    ElMessage.success(res.msg || '保存成功')
    assignVisible.value = false
    await loadUsers()
  } finally {
    saving.value = false
  }
}

onMounted(loadUsers)
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

.role-tag {
  margin: 2px 6px 2px 0;
}

.assign-tip {
  margin-bottom: 12px;
}

.assign-group {
  border-bottom: 1px dashed #e6e8eb;
  padding: 10px 0;
}

.assign-group-title {
  margin-bottom: 6px;
}

.assign-role {
  margin-right: 16px;
}
</style>
