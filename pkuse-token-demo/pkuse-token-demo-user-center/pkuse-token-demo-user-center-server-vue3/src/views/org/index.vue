<template>
  <div class="page-container">
    <!-- 说明 -->
    <el-alert type="info" :closable="false" class="tip-alert">
      <template #title>
        组织架构为省 / 市 / 县 / 乡镇街道四级树形结构，用户可在「用户管理」中归属到组织节点。
        层级为档案属性（手动选择，允许与树深度不一致）；删除组织前需先删除其下级组织并移除挂载用户。
      </template>
    </el-alert>

    <!-- 查询条件 -->
    <el-card shadow="never" class="toolbar-card">
      <div class="table-toolbar">
        <el-form :inline="true" :model="query" @submit.prevent>
          <el-form-item label="单位名称">
            <el-input v-model.trim="query.orgName" placeholder="单位名称" clearable style="width: 180px" @keyup.enter="onSearch" />
          </el-form-item>
          <el-form-item label="简称">
            <el-input v-model.trim="query.shortName" placeholder="单位简称" clearable style="width: 150px" @keyup.enter="onSearch" />
          </el-form-item>
          <el-form-item label="层级">
            <el-select v-model="query.orgLevel" placeholder="全部" clearable style="width: 130px">
              <el-option v-for="item in levelOptions" :key="item.value" :label="item.label" :value="item.value" />
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
          <el-icon><Plus /></el-icon>新增组织
        </el-button>
      </div>
    </el-card>

    <!-- 组织树 -->
    <el-card shadow="never" class="table-card">
      <el-table
        ref="tableRef"
        v-loading="loading"
        :data="treeData"
        row-key="id"
        :tree-props="{ children: 'children' }"
        default-expand-all
        stripe
      >
        <el-table-column prop="orgName" label="单位名称" min-width="240" show-overflow-tooltip>
          <template #default="{ row }">
            <span>{{ row.orgName }}</span>
            <el-tooltip v-if="row.remark" :content="row.remark" placement="top">
              <el-icon class="remark-icon"><InfoFilled /></el-icon>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column label="层级" width="110" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="levelTagType(row.orgLevel)">{{ levelLabel(row.orgLevel) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="区划" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="region-cell">
              <span>{{ row.regionName || '-' }}</span>
              <span v-if="row.regionCode" class="text-mono text-muted region-code">{{ row.regionCode }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="shortName" label="简称" min-width="130" show-overflow-tooltip>
          <template #default="{ row }">{{ row.shortName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="creditCode" label="统一社会信用代码" min-width="190" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.creditCode" class="text-mono">{{ row.creditCode }}</span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="70" align="center" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openCreateChild(row)">新增下级</el-button>
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="onDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="tree-count text-muted">共 {{ totalCount }} 个组织节点</div>
    </el-card>

    <!-- 新增 / 编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="form.id ? '编辑组织' : '新增组织'"
      width="640px"
      :close-on-click-modal="false"
      @closed="onDialogClosed"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="上级组织" prop="parentId">
          <el-tree-select
            v-model="form.parentId"
            :data="parentOptions"
            :props="{ label: 'orgName', children: 'children' }"
            node-key="id"
            check-strictly
            clearable
            filterable
            placeholder="不选择则为顶级组织"
            style="width: 100%"
          />
          <div class="form-tip text-muted">层级为档案属性，由下方手动选择，不随挂载位置自动推导</div>
        </el-form-item>
        <el-form-item label="层级" prop="orgLevel">
          <el-select v-model="form.orgLevel" placeholder="请选择层级" style="width: 100%">
            <el-option v-for="item in levelOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="单位名称" prop="orgName">
          <el-input v-model.trim="form.orgName" placeholder="单位全称，同一上级下不可重复" maxlength="128" />
        </el-form-item>
        <el-form-item label="简称" prop="shortName">
          <el-input v-model.trim="form.shortName" placeholder="单位简称" maxlength="64" />
        </el-form-item>
        <el-form-item label="统一社会信用代码" prop="creditCode">
          <el-input v-model.trim="form.creditCode" placeholder="18 位，选填" maxlength="18" />
        </el-form-item>
        <el-form-item label="所属区划" prop="regionPath">
          <el-cascader
            ref="regionCascaderRef"
            v-model="form.regionPath"
            :props="cascaderProps"
            clearable
            filterable
            placeholder="请选择省 / 市 / 县 / 街道（可停在任意层级）"
            style="width: 100%"
            @change="onRegionChange"
          />
          <div class="form-tip text-muted">
            数据来自「区划管理」，选中后自动回填区划代码与名称；直辖市市级为「市辖区」
          </div>
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="form.sort" :min="0" :max="9999" controls-position="right" style="width: 160px" />
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
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { orgApi, regionApi } from '../../api'

/** 层级选项（与后端 UcOrgUnit 常量一致：1=省 2=市 3=县 4=乡镇街道） */
const levelOptions = [
  { label: '省', value: 1 },
  { label: '市', value: 2 },
  { label: '县（区）', value: 3 },
  { label: '乡镇（街道）', value: 4 }
]

/** 层级标签颜色 */
const levelTagTypes = { 1: 'danger', 2: 'warning', 3: 'primary', 4: 'info' }

const loading = ref(false)
const saving = ref(false)
const treeData = ref([])
const totalCount = ref(0)

// 全量组织树（仅用于表单里的「上级组织」选择：列表过滤时不影响可选范围）
const fullTree = ref([])

const query = reactive({ orgName: '', shortName: '', orgLevel: null })

const dialogVisible = ref(false)
const formRef = ref(null)
const regionCascaderRef = ref(null)
const form = reactive({
  id: null,
  parentId: null,
  orgLevel: null,
  orgName: '',
  shortName: '',
  creditCode: '',
  regionPath: null,
  regionName: '',
  regionCode: '',
  sort: 0,
  remark: ''
})

/** 区划级联：懒加载 + 可停在任意层级（checkStrictly） */
const cascaderProps = {
  lazy: true,
  checkStrictly: true,
  lazyLoad: loadRegionNode
}

function loadRegionNode(node, resolve) {
  regionApi.cascade(node.level === 0 ? null : node.value)
    .then((res) => {
      const options = (res.data || []).map((item) => ({
        value: item.code,
        label: item.name,
        leaf: item.leaf
      }))
      resolve(options)
    })
    .catch(() => resolve([]))
}

/** 由 12 位区划编码推导级联回显路径（按有效位判断缺级，缺级自动跳过） */
function deriveRegionPath(code) {
  if (!/^\d{12}$/.test(code || '')) {
    return null
  }
  const province = code.substring(0, 2) + '0'.repeat(10)
  if (code.substring(2, 4) === '00') {
    return [province]
  }
  const city = code.substring(0, 4) + '0'.repeat(8)
  if (code.substring(4, 6) === '00') {
    return [province, city]
  }
  const county = code.substring(0, 6) + '0'.repeat(6)
  if (code.substring(6, 9) === '000') {
    return [province, city, county]
  }
  return [province, city, county, code]
}

/** 级联选择变化：回填区划代码与名称（名称为各级名称 / 拼接，超长回退叶名） */
function onRegionChange(value) {
  if (!value || !value.length) {
    form.regionCode = ''
    form.regionName = ''
    return
  }
  form.regionCode = value[value.length - 1]
  const nodes = regionCascaderRef.value?.getCheckedNodes(false) || []
  const labels = nodes.length ? nodes[0].pathNodes.map((node) => node.label) : []
  const joined = labels.join('/')
  form.regionName = joined.length > 60 ? (labels[labels.length - 1] || '') : joined
}

const rules = {
  orgName: [{ required: true, message: '请输入单位名称', trigger: 'blur' }],
  orgLevel: [{ required: true, message: '请选择层级', trigger: 'change' }],
  regionPath: [{ required: true, message: '请选择所属区划', trigger: 'change' }],
  creditCode: [
    {
      validator: (rule, value, callback) => {
        if (!value) {
          callback()
          return
        }
        if (!/^[0-9A-HJ-NPQRTUWXY]{2}\d{6}[0-9A-HJ-NPQRTUWXY]{10}$/.test(value)) {
          callback(new Error('统一社会信用代码格式不正确（18 位）'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ]
}

/** 上级组织可选树：编辑时剔除自身及其子孙（后端也会兜底校验） */
const parentOptions = computed(() => {
  if (!form.id) {
    return fullTree.value
  }
  return filterOutSubtree(fullTree.value, form.id)
})

onMounted(async () => {
  await loadData()
  await loadFullTree()
})

async function loadData() {
  loading.value = true
  try {
    const res = await orgApi.tree({
      orgName: query.orgName || undefined,
      shortName: query.shortName || undefined,
      orgLevel: query.orgLevel === null || query.orgLevel === '' ? undefined : query.orgLevel
    })
    treeData.value = res.data?.list || []
    totalCount.value = res.data?.count || 0
  } catch (e) {
    // 错误提示已由 axios 拦截器统一处理
  } finally {
    loading.value = false
  }
}

/** 加载全量组织树（上级组织选择用，不受列表过滤条件影响） */
async function loadFullTree() {
  try {
    const res = await orgApi.tree()
    fullTree.value = res.data?.list || []
  } catch (e) {
    // 错误提示已由 axios 拦截器统一处理
  }
}

function onSearch() {
  loadData()
}

function onReset() {
  query.orgName = ''
  query.shortName = ''
  query.orgLevel = null
  loadData()
}

function levelLabel(value) {
  const item = levelOptions.find((option) => option.value === value)
  return item ? item.label : '-'
}

function levelTagType(value) {
  return levelTagTypes[value] || 'info'
}

function resetForm() {
  form.id = null
  form.parentId = null
  form.orgLevel = null
  form.orgName = ''
  form.shortName = ''
  form.creditCode = ''
  form.regionPath = null
  form.regionName = ''
  form.regionCode = ''
  form.sort = 0
  form.remark = ''
}

function openCreate() {
  resetForm()
  // 顶级默认按省级建档（可改：层级允许与树深度不一致）
  form.orgLevel = 1
  dialogVisible.value = true
}

/** 新增下级：预挂当前节点，层级默认取父层级 + 1（乡镇街道封顶，可改） */
function openCreateChild(row) {
  resetForm()
  form.parentId = row.id
  form.orgLevel = Math.min((row.orgLevel || 1) + 1, 4)
  dialogVisible.value = true
}

function openEdit(row) {
  resetForm()
  Object.assign(form, {
    id: row.id,
    parentId: row.parentId && row.parentId !== '0' ? row.parentId : null,
    orgLevel: row.orgLevel,
    orgName: row.orgName,
    shortName: row.shortName || '',
    creditCode: row.creditCode || '',
    regionPath: deriveRegionPath(row.regionCode),
    regionName: row.regionName || '',
    regionCode: row.regionCode || '',
    sort: row.sort ?? 0,
    remark: row.remark
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
    const payload = {
      parentId: form.parentId || null,
      orgLevel: form.orgLevel,
      orgName: form.orgName,
      shortName: form.shortName,
      creditCode: form.creditCode,
      regionName: form.regionName,
      regionCode: form.regionCode,
      sort: form.sort,
      remark: form.remark
    }
    if (form.id) {
      await orgApi.update({ ...payload, id: form.id })
      ElMessage.success('保存成功')
    } else {
      await orgApi.create(payload)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadData()
    loadFullTree()
  } catch (e) {
    // 错误提示已由 axios 拦截器统一处理
  } finally {
    saving.value = false
  }
}

async function onDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定要删除组织「${row.orgName}」吗？存在下级组织或仍有用户挂载时将被拦截。`,
      '危险操作',
      { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'error' }
    )
  } catch (e) {
    return
  }
  try {
    await orgApi.remove(row.id)
    ElMessage.success('删除成功')
    loadData()
    loadFullTree()
  } catch (e) {
    // 错误提示已由 axios 拦截器统一处理（如：存在下级 / 有用户挂载）
  }
}

/** 从树中剔除指定节点及其子孙（返回新数组，不改原树） */
function filterOutSubtree(nodes, excludeId) {
  const result = []
  for (const node of nodes) {
    if (node.id === excludeId) {
      continue
    }
    const copy = { ...node }
    if (node.children && node.children.length) {
      copy.children = filterOutSubtree(node.children, excludeId)
    }
    result.push(copy)
  }
  return result
}
</script>

<style scoped>
.tip-alert {
  margin-bottom: 16px;
}

.toolbar-card {
  padding-bottom: 0;
}

.region-cell {
  display: flex;
  flex-direction: column;
  line-height: 1.5;
}

.region-code {
  font-size: 12px;
}

.tree-count {
  margin-top: 12px;
  font-size: 12px;
  text-align: right;
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
