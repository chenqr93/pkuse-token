<template>
  <div class="page-container">
    <!-- 说明 -->
    <el-alert type="info" :closable="false" class="tip-alert">
      <template #title>
        区划为省 / 市 / 县 / 乡镇街道四级<b>扁平化字典</b>：每行为一条完整路径，编码为 12 位国标统计口径
        （有效位 + 尾部补零，如建北街道 <span class="text-mono">130102001000</span>）。
        缺级列留空（如省级行、直筒子市）；最终区划编码 / 层级由最深层级<b>自动推导</b>且不可变更。
      </template>
    </el-alert>

    <!-- 查询条件 -->
    <el-card shadow="never" class="toolbar-card">
      <div class="table-toolbar">
        <el-form :inline="true" :model="query" @submit.prevent>
          <el-form-item label="关键字">
            <el-input v-model.trim="query.keyword" placeholder="任一层级名称 / 最终编码前缀" clearable style="width: 220px" @keyup.enter="onSearch" />
          </el-form-item>
          <el-form-item label="省份">
            <el-select v-model="query.provinceCode" placeholder="全部" clearable filterable style="width: 160px">
              <el-option v-for="item in provinceOptions" :key="item.code" :label="item.name" :value="item.code" />
            </el-select>
          </el-form-item>
          <el-form-item label="最终层级">
            <el-select v-model="query.finalLevel" placeholder="全部" clearable style="width: 130px">
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
          <el-icon><Plus /></el-icon>新增区划
        </el-button>
      </div>
    </el-card>

    <!-- 区划列表 -->
    <el-card shadow="never" class="table-card">
      <el-table :data="records" v-loading="loading" stripe>
        <el-table-column label="省" min-width="130" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="region-cell">
              <span>{{ row.provinceName }}</span>
              <span class="text-mono text-muted region-code">{{ row.provinceCode }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="市" min-width="130" show-overflow-tooltip>
          <template #default="{ row }">
            <div v-if="row.cityCode" class="region-cell">
              <span>{{ row.cityName }}</span>
              <span class="text-mono text-muted region-code">{{ row.cityCode }}</span>
            </div>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="县（区）" min-width="130" show-overflow-tooltip>
          <template #default="{ row }">
            <div v-if="row.countyCode" class="region-cell">
              <span>{{ row.countyName }}</span>
              <span class="text-mono text-muted region-code">{{ row.countyCode }}</span>
            </div>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="街道（乡镇）" min-width="130" show-overflow-tooltip>
          <template #default="{ row }">
            <div v-if="row.streetCode" class="region-cell">
              <span>{{ row.streetName }}</span>
              <span class="text-mono text-muted region-code">{{ row.streetCode }}</span>
            </div>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="最终区划编码" width="140" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="text-mono">{{ row.finalCode }}</span>
          </template>
        </el-table-column>
        <el-table-column label="最终区划名称" min-width="120" show-overflow-tooltip>
          <template #default="{ row }">{{ row.finalName }}</template>
        </el-table-column>
        <el-table-column label="最终层级" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="levelTagType(row.finalLevel)">{{ levelLabel(row.finalLevel) }}</el-tag>
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
        :page-sizes="[10, 20, 50, 100]"
        @current-change="onPageChange"
        @size-change="onSizeChange"
      />
    </el-card>

    <!-- 新增 / 编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="form.id ? '编辑区划' : '新增区划'"
      width="680px"
      :close-on-click-modal="false"
      @closed="onDialogClosed"
    >
      <el-alert type="warning" :closable="false" class="form-tip-alert">
        <template #title>
          编辑时最终区划编码不可变更；补零规则：省后 10 位为 0、市后 8 位为 0、县后 6 位为 0、街道后 3 位为 0。
        </template>
      </el-alert>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="130px">
        <el-form-item label="省编码 / 名称" required>
          <div class="pair-row">
            <el-input v-model.trim="form.provinceCode" placeholder="130000000000" maxlength="12" class="pair-code" />
            <el-input v-model.trim="form.provinceName" placeholder="省名称" maxlength="64" class="pair-name" />
          </div>
        </el-form-item>
        <el-form-item label="市编码 / 名称">
          <div class="pair-row">
            <el-input v-model.trim="form.cityCode" placeholder="后 8 位为 0，缺级留空" maxlength="12" class="pair-code" />
            <el-input v-model.trim="form.cityName" placeholder="市名称（直辖市为「市辖区」）" maxlength="64" class="pair-name" />
          </div>
        </el-form-item>
        <el-form-item label="县（区）编码 / 名称">
          <div class="pair-row">
            <el-input v-model.trim="form.countyCode" placeholder="后 6 位为 0，缺级留空" maxlength="12" class="pair-code" />
            <el-input v-model.trim="form.countyName" placeholder="县（区）名称" maxlength="64" class="pair-name" />
          </div>
        </el-form-item>
        <el-form-item label="街道（乡镇）编码 / 名称">
          <div class="pair-row">
            <el-input v-model.trim="form.streetCode" placeholder="后 3 位为 0，缺级留空" maxlength="12" class="pair-code" />
            <el-input v-model.trim="form.streetName" placeholder="街道（乡镇）名称" maxlength="64" class="pair-name" />
          </div>
        </el-form-item>
        <el-form-item label="最终区划（自动推导）">
          <div class="final-preview text-mono">
            {{ finalPreview.code || '-' }}　{{ finalPreview.name || '按最深层级自动推导' }}
            <el-tag v-if="finalPreview.level" size="small" :type="levelTagType(finalPreview.level)" class="final-tag">
              {{ levelLabel(finalPreview.level) }}
            </el-tag>
          </div>
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
import { regionApi } from '../../api'

/** 最终层级选项（与后端 UcRegion 常量一致） */
const levelOptions = [
  { label: '省', value: 1 },
  { label: '市', value: 2 },
  { label: '县（区）', value: 3 },
  { label: '乡镇（街道）', value: 4 }
]

const levelTagTypes = { 1: 'danger', 2: 'warning', 3: 'primary', 4: 'info' }

/** 各级编码校验正则（与后端一致：有效位 + 尾部补零） */
const codePatterns = {
  provinceCode: /^\d{2}0{10}$/,
  cityCode: /^\d{4}0{8}$/,
  countyCode: /^\d{6}0{6}$/,
  streetCode: /^\d{9}0{3}$/
}

const loading = ref(false)
const saving = ref(false)
const records = ref([])
const total = ref(0)
const provinceOptions = ref([])

const query = reactive({ current: 1, size: 20, keyword: '', provinceCode: null, finalLevel: null })

const dialogVisible = ref(false)
const formRef = ref(null)
const form = reactive({
  id: null,
  provinceCode: '',
  provinceName: '',
  cityCode: '',
  cityName: '',
  countyCode: '',
  countyName: '',
  streetCode: '',
  streetName: ''
})

/** 逐级前缀校验：编码需与上级有效位前缀一致 */
function validateLevel(codeKey, nameKey, label, parentKey, parentSignificant) {
  return {
    validator: (rule, value, callback) => {
      const code = (form[codeKey] || '').trim()
      const name = (form[nameKey] || '').trim()
      if (!code && !name) {
        callback()
        return
      }
      if (!code) {
        callback(new Error(`${label}名称需与编码同时填写`))
        return
      }
      if (!name) {
        callback(new Error(`${label}编码需与名称同时填写`))
        return
      }
      if (!codePatterns[codeKey].test(code)) {
        callback(new Error(`${label}编码需为 12 位数字且符合层级补零规则`))
        return
      }
      const parent = (form[parentKey] || '').trim()
      if (parent && !code.startsWith(parent.substring(0, parentSignificant))) {
        callback(new Error(`${label}编码需与上级区划编码前缀一致`))
        return
      }
      callback()
    },
    trigger: 'blur'
  }
}

const rules = {
  provinceCode: [
    {
      validator: (rule, value, callback) => {
        const code = (value || '').trim()
        if (!code) {
          callback(new Error('请输入省区划编码'))
          return
        }
        if (!codePatterns.provinceCode.test(code)) {
          callback(new Error('省编码需为 12 位数字且后 10 位为 0（如 130000000000）'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ],
  provinceName: [{ required: true, message: '请输入省区划名称', trigger: 'blur' }],
  cityCode: [validateLevel('cityCode', 'cityName', '市', 'provinceCode', 2)],
  countyCode: [validateLevel('countyCode', 'countyName', '县', 'cityCode', 4)],
  streetCode: [validateLevel('streetCode', 'streetName', '街道', 'countyCode', 6)]
}

/** 最终区划预览（按最深层级自动推导，与后端口径一致） */
const finalPreview = computed(() => {
  if (form.streetCode) {
    return { code: form.streetCode, name: form.streetName, level: 4 }
  }
  if (form.countyCode) {
    return { code: form.countyCode, name: form.countyName, level: 3 }
  }
  if (form.cityCode) {
    return { code: form.cityCode, name: form.cityName, level: 2 }
  }
  if (form.provinceCode) {
    return { code: form.provinceCode, name: form.provinceName, level: 1 }
  }
  return { code: '', name: '', level: null }
})

onMounted(async () => {
  await loadData()
  await loadProvinces()
})

async function loadData() {
  loading.value = true
  try {
    const res = await regionApi.page({
      current: query.current,
      size: query.size,
      keyword: query.keyword || undefined,
      provinceCode: query.provinceCode || undefined,
      finalLevel: query.finalLevel === null || query.finalLevel === '' ? undefined : query.finalLevel
    })
    records.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (e) {
    // 错误提示已由 axios 拦截器统一处理
  } finally {
    loading.value = false
  }
}

/** 省份下拉选项（级联根级） */
async function loadProvinces() {
  try {
    const res = await regionApi.cascade()
    provinceOptions.value = res.data || []
  } catch (e) {
    // 错误提示已由 axios 拦截器统一处理
  }
}

function onSearch() {
  query.current = 1
  loadData()
}

function onReset() {
  query.keyword = ''
  query.provinceCode = null
  query.finalLevel = null
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

function levelLabel(value) {
  const item = levelOptions.find((option) => option.value === value)
  return item ? item.label : '-'
}

function levelTagType(value) {
  return levelTagTypes[value] || 'info'
}

function resetForm() {
  form.id = null
  form.provinceCode = ''
  form.provinceName = ''
  form.cityCode = ''
  form.cityName = ''
  form.countyCode = ''
  form.countyName = ''
  form.streetCode = ''
  form.streetName = ''
}

function openCreate() {
  resetForm()
  // 默认预填河北省省级路径（当前区划库唯一省份），便于快速新增
  form.provinceCode = '130000000000'
  form.provinceName = '河北省'
  dialogVisible.value = true
}

function openEdit(row) {
  resetForm()
  Object.assign(form, {
    id: row.id,
    provinceCode: row.provinceCode || '',
    provinceName: row.provinceName || '',
    cityCode: row.cityCode || '',
    cityName: row.cityName || '',
    countyCode: row.countyCode || '',
    countyName: row.countyName || '',
    streetCode: row.streetCode || '',
    streetName: row.streetName || ''
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
      provinceCode: form.provinceCode.trim(),
      provinceName: form.provinceName.trim(),
      cityCode: form.cityCode.trim() || null,
      cityName: form.cityName.trim() || null,
      countyCode: form.countyCode.trim() || null,
      countyName: form.countyName.trim() || null,
      streetCode: form.streetCode.trim() || null,
      streetName: form.streetName.trim() || null
    }
    if (form.id) {
      await regionApi.update({ ...payload, id: form.id })
      ElMessage.success('修改成功')
    } else {
      await regionApi.create(payload)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadData()
  } catch (e) {
    // 错误提示已由 axios 拦截器统一处理
  } finally {
    saving.value = false
  }
}

async function onDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定要删除区划「${row.finalName}（${row.finalCode}）」吗？删除后不可恢复。`,
      '危险操作',
      { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'error' }
    )
  } catch (e) {
    return
  }
  try {
    await regionApi.remove(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (e) {
    // 错误提示已由 axios 拦截器统一处理
  }
}
</script>

<style scoped>
.tip-alert {
  margin-bottom: 16px;
}

.form-tip-alert {
  margin-bottom: 16px;
}

.toolbar-card {
  padding-bottom: 0;
}

.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}

.region-cell {
  display: flex;
  flex-direction: column;
  line-height: 1.5;
}

.region-code {
  font-size: 12px;
}

.pair-row {
  display: flex;
  gap: 8px;
  width: 100%;
}

.pair-code {
  width: 220px;
  flex: none;
}

.pair-name {
  flex: 1;
}

.final-preview {
  width: 100%;
  padding: 6px 10px;
  background-color: var(--el-fill-color-light);
  border-radius: 4px;
  font-size: 13px;
}

.final-tag {
  margin-left: 8px;
}
</style>
