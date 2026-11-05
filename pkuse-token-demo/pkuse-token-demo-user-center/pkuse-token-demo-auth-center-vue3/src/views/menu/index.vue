<template>
  <div class="page-container">
    <el-card shadow="never">
      <div class="table-toolbar">
        <div class="toolbar-left">
          <span style="font-weight: 600">菜单管理</span>
          <el-select
            v-model="clientId"
            placeholder="选择应用"
            style="width: 240px; margin-left: 12px"
            @change="loadTree"
          >
            <el-option
              v-for="app in apps"
              :key="app.clientId"
              :label="`${app.clientName}（${app.clientId}）`"
              :value="app.clientId"
              :disabled="app.status !== 1"
            />
          </el-select>
          <span class="text-muted" style="margin-left: 12px; font-size: 12px">
            类型：目录 / 页面 / 按钮；按钮必填权限码
          </span>
        </div>
        <el-button type="primary" :disabled="!clientId" @click="openCreate('0')">
          <el-icon><Plus /></el-icon>&nbsp;新增根菜单
        </el-button>
      </div>

      <el-table
        :data="tree"
        v-loading="loading"
        row-key="id"
        :tree-props="{ children: 'children' }"
        default-expand-all
        class="table-card"
      >
        <el-table-column prop="menuName" label="菜单名称" min-width="200" />
        <el-table-column label="类型" width="80">
          <template #default="{ row }">
            <el-tag size="small" :type="typeTag(row.menuType).type">
              {{ typeTag(row.menuType).text }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="path" label="路由地址" min-width="140" show-overflow-tooltip />
        <el-table-column prop="component" label="组件路径" min-width="150" show-overflow-tooltip />
        <el-table-column prop="perms" label="权限码" min-width="130" show-overflow-tooltip />
        <el-table-column prop="sort" label="排序" width="70" align="center" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'success' : 'info'">
              {{ row.status === 0 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="显示" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.menuType !== 2" :type="row.visible === 0 ? 'success' : 'warning'">
              {{ row.visible === 0 ? '显示' : '隐藏' }}
            </el-tag>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.menuType !== 2"
              link
              type="primary"
              :disabled="row.status !== 0"
              @click="openCreate(row.id)"
            >新增子级</el-button>
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link @click="toggleStatus(row)">{{ row.status === 0 ? '禁用' : '启用' }}</el-button>
            <el-popconfirm title="确定删除该菜单？（被角色绑定时将级联清理绑定）" @confirm="remove(row)">
              <template #reference>
                <el-button link type="danger">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑菜单' : '新增菜单'" width="560px">
      <el-form :model="form" label-width="92px">
        <el-form-item label="菜单类型" required>
          <el-radio-group v-model="form.menuType" :disabled="!!form.id">
            <el-radio-button :value="0">目录</el-radio-button>
            <el-radio-button :value="1">页面</el-radio-button>
            <el-radio-button :value="2">按钮</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="菜单名称" required>
          <el-input v-model="form.menuName" maxlength="50" placeholder="应用内唯一" />
        </el-form-item>
        <el-form-item label="上级菜单">
          <el-input :value="parentLabel" disabled />
        </el-form-item>
        <template v-if="form.menuType !== 2">
          <el-form-item label="路由地址" :required="form.menuType === 1">
            <el-input v-model="form.path" placeholder="如 /sys/user" />
          </el-form-item>
          <el-form-item v-if="form.menuType === 1" label="组件路径">
            <el-input v-model="form.component" placeholder="如 sys/user/index" />
          </el-form-item>
        </template>
        <el-form-item label="权限码" :required="form.menuType === 2">
          <el-input
            v-model="form.perms"
            :disabled="form.menuType === 0"
            placeholder="如 sys:user:add（按钮必填，应用内唯一）"
          />
        </el-form-item>
        <el-form-item v-if="form.menuType !== 2" label="图标">
          <el-input v-model="form.icon" placeholder="Element Plus 图标名，如 Setting" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :max="9999" />
        </el-form-item>
        <el-form-item v-if="form.menuType !== 2" label="是否显示">
          <el-switch v-model="form.visible" :active-value="0" inactive-value-text="隐藏" :inactive-value="1"
                     active-text="显示" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" maxlength="200" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { appApi, menuApi } from '../../api'

const route = useRoute()

const apps = ref([])
const clientId = ref('')
const tree = ref([])
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)

const form = reactive({
  id: '',
  clientId: '',
  parentId: '0',
  menuName: '',
  menuType: 0,
  path: '',
  component: '',
  perms: '',
  icon: '',
  sort: 0,
  visible: 0,
  remark: ''
})

/** 上级菜单显示名（根为“根节点”） */
const parentLabel = computed(() => {
  if (!form.parentId || form.parentId === '0') {
    return '根节点'
  }
  const node = findNode(tree.value, form.parentId)
  return node ? node.menuName : form.parentId
})

function findNode(nodes, id) {
  for (const node of nodes) {
    if (node.id === id) {
      return node
    }
    const child = findNode(node.children || [], id)
    if (child) {
      return child
    }
  }
  return null
}

function typeTag(type) {
  if (type === 0) {
    return { text: '目录', type: 'warning' }
  }
  if (type === 1) {
    return { text: '页面', type: 'primary' }
  }
  return { text: '按钮', type: 'info' }
}

async function loadApps() {
  const res = await appApi.list()
  // 统一信封：应用列表在 data.apps 内
  apps.value = (res.data?.apps || []).filter((app) => app.status === 1)
  if (!clientId.value && apps.value.length) {
    clientId.value = route.query.clientId && apps.value.some((a) => a.clientId === route.query.clientId)
      ? route.query.clientId
      : apps.value[0].clientId
    await loadTree()
  }
}

async function loadTree() {
  if (!clientId.value) {
    tree.value = []
    return
  }
  loading.value = true
  try {
    const res = await menuApi.tree(clientId.value)
    // 树形数据统一收在 data.list（data={list, count}）
    tree.value = res.data?.list || []
  } finally {
    loading.value = false
  }
}

function openCreate(parentId) {
  Object.assign(form, {
    id: '',
    clientId: clientId.value,
    parentId: parentId || '0',
    menuName: '',
    menuType: parentId === '0' ? 0 : 1,
    path: '',
    component: '',
    perms: '',
    icon: '',
    sort: 0,
    visible: 0,
    remark: ''
  })
  dialogVisible.value = true
}

function openEdit(row) {
  Object.assign(form, {
    id: row.id,
    clientId: clientId.value,
    parentId: row.parentId,
    menuName: row.menuName,
    menuType: row.menuType,
    path: row.path || '',
    component: row.component || '',
    perms: row.perms || '',
    icon: row.icon || '',
    sort: row.sort || 0,
    visible: row.visible == null ? 0 : row.visible,
    remark: ''
  })
  dialogVisible.value = true
}

async function save() {
  saving.value = true
  try {
    const payload = { ...form, clientId: clientId.value }
    const res = form.id ? await menuApi.update(payload) : await menuApi.create(payload)
    ElMessage.success(res.msg || '保存成功')
    dialogVisible.value = false
    await loadTree()
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row) {
  const target = row.status === 0 ? 1 : 0
  const res = await menuApi.changeStatus(row.id, target)
  ElMessage.success(res.msg || '操作成功')
  await loadTree()
}

async function remove(row) {
  const res = await menuApi.remove(row.id)
  ElMessage.success(res.msg || '删除成功')
  await loadTree()
}

onMounted(loadApps)
</script>
