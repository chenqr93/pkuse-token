<template>
  <div class="page-container">
    <el-card shadow="never">
      <div class="table-toolbar">
        <div class="toolbar-left">
          <span style="font-weight: 600">角色管理</span>
          <el-select
            v-model="clientId"
            placeholder="选择应用"
            style="width: 240px; margin-left: 12px"
            @change="onClientChange"
          >
            <el-option
              v-for="app in apps"
              :key="app.clientId"
              :label="`${app.clientName}（${app.clientId}）`"
              :value="app.clientId"
              :disabled="app.status !== 1"
            />
          </el-select>
          <el-input
            v-model="query.roleName"
            placeholder="角色名称/编码"
            clearable
            style="width: 180px; margin-left: 12px"
            @keyup.enter="loadRoles(1)"
          />
          <el-button style="margin-left: 8px" @click="loadRoles(1)">查询</el-button>
        </div>
        <el-button type="primary" :disabled="!clientId" @click="openCreate">
          <el-icon><Plus /></el-icon>&nbsp;新增角色
        </el-button>
      </div>

      <el-table :data="roles" v-loading="loading" class="table-card">
        <el-table-column prop="roleCode" label="角色编码" min-width="120" />
        <el-table-column prop="roleName" label="角色名称" min-width="140" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'success' : 'info'">
              {{ row.status === 0 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="menuCount" label="已绑菜单" width="90" align="center" />
        <el-table-column prop="userCount" label="授权用户" width="90" align="center" />
        <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
        <el-table-column label="操作" width="290" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openMenus(row)">菜单权限</el-button>
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link @click="toggleStatus(row)">{{ row.status === 0 ? '禁用' : '启用' }}</el-button>
            <el-popconfirm
              title="确定删除该角色？（将级联清理角色-菜单与用户-角色绑定）"
              @confirm="remove(row)"
            >
              <template #reference>
                <el-button link type="danger">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="pagination"
        layout="total, prev, pager, next, sizes"
        :total="total"
        :current-page="query.current"
        :page-size="query.size"
        @current-change="(page) => loadRoles(page)"
        @size-change="(size) => { query.size = size; loadRoles(1) }"
      />
    </el-card>

    <!-- 新增/编辑角色 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑角色' : '新增角色'" width="480px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="角色编码" required>
          <el-input v-model="form.roleCode" :disabled="!!form.id" maxlength="50"
                    placeholder="字母数字下划线，应用内唯一" />
        </el-form-item>
        <el-form-item label="角色名称" required>
          <el-input v-model="form.roleName" maxlength="50" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="200" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <!-- 菜单权限勾选 -->
    <el-dialog v-model="menusVisible" :title="`菜单权限：${currentRole.roleName || ''}`" width="520px">
      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="父子节点不联动，请按需勾选目录/页面/按钮；勾选页面时其所需父目录建议一并勾选"
        class="menus-tip"
      />
      <el-tree
        ref="menuTreeRef"
        :data="menuTree"
        node-key="id"
        show-checkbox
        check-strictly
        default-expand-all
        :props="{ label: 'menuName', children: 'children' }"
      />
      <template #footer>
        <el-button @click="menusVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveMenus">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { nextTick, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { appApi, menuApi, roleApi } from '../../api'

const route = useRoute()

const apps = ref([])
const clientId = ref('')
const roles = ref([])
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const menusVisible = ref(false)
const menuTreeRef = ref(null)
const menuTree = ref([])
const currentRole = ref({})

const query = reactive({ current: 1, size: 10, roleName: '' })
const form = reactive({ id: '', clientId: '', roleCode: '', roleName: '', remark: '' })

async function loadApps() {
  const res = await appApi.list()
  apps.value = (res.data || []).filter((app) => app.status === 1)
  const fromQuery = route.query.clientId
  if (apps.value.length) {
    clientId.value =
      fromQuery && apps.value.some((a) => a.clientId === fromQuery)
        ? fromQuery
        : apps.value[0].clientId
    await loadRoles(1)
  }
}

async function loadRoles(page) {
  if (!clientId.value) {
    return
  }
  query.current = page || query.current
  loading.value = true
  try {
    const res = await roleApi.page({
      clientId: clientId.value,
      roleName: query.roleName || undefined,
      current: query.current,
      size: query.size
    })
    roles.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

function onClientChange() {
  query.roleName = ''
  loadRoles(1)
}

function openCreate() {
  Object.assign(form, { id: '', clientId: clientId.value, roleCode: '', roleName: '', remark: '' })
  dialogVisible.value = true
}

function openEdit(row) {
  Object.assign(form, {
    id: row.id,
    clientId: row.clientId,
    roleCode: row.roleCode,
    roleName: row.roleName,
    remark: row.remark || ''
  })
  dialogVisible.value = true
}

async function save() {
  saving.value = true
  try {
    const payload = { ...form, clientId: clientId.value }
    const res = form.id ? await roleApi.update(payload) : await roleApi.create(payload)
    ElMessage.success(res.msg || '保存成功')
    dialogVisible.value = false
    await loadRoles()
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row) {
  const target = row.status === 0 ? 1 : 0
  const res = await roleApi.changeStatus(row.id, target)
  ElMessage.success(res.msg || '操作成功')
  await loadRoles()
}

async function remove(row) {
  const res = await roleApi.remove(row.id)
  ElMessage.success(res.msg || '删除成功')
  await loadRoles()
}

async function openMenus(row) {
  currentRole.value = row
  const [treeRes, idsRes] = await Promise.all([
    menuApi.tree(clientId.value),
    roleApi.menuIds(row.id)
  ])
  menuTree.value = treeRes.data || []
  menusVisible.value = true
  await nextTick()
  menuTreeRef.value.setCheckedKeys(idsRes.data || [], false)
}

async function saveMenus() {
  saving.value = true
  try {
    const menuIds = menuTreeRef.value.getCheckedKeys()
    const res = await roleApi.bindMenus(currentRole.value.id, menuIds)
    ElMessage.success(res.msg || '保存成功')
    menusVisible.value = false
    await loadRoles()
  } finally {
    saving.value = false
  }
}

onMounted(loadApps)
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

.menus-tip {
  margin-bottom: 12px;
}
</style>
