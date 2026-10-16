<template>
  <div class="page-container">
    <!-- 说明 -->
    <el-alert type="info" :closable="false" class="tip-alert">
      <template #title>
        平台规则配置（仅系统管理员）：密码规则 / 登录规则 / 日志规则保存后<b>即时生效</b>，无需重启服务；
        配置存储于数据库表 <code>uc_config</code>，可一键恢复默认值。
      </template>
    </el-alert>

    <el-card shadow="never" v-loading="loading" class="config-card">
      <div class="config-toolbar">
        <el-button type="primary" :loading="saving" @click="onSave">
          <el-icon><Check /></el-icon>保存配置
        </el-button>
        <el-button @click="onReset" :disabled="saving">恢复默认</el-button>
        <el-button @click="loadData" :disabled="saving">
          <el-icon><Refresh /></el-icon>刷新
        </el-button>
        <span v-if="dirtyCount > 0" class="dirty-tip">有 {{ dirtyCount }} 项修改待保存</span>
      </div>

      <div v-for="group in groupNames" :key="group" class="config-group">
        <div class="group-title">{{ group }}</div>
        <el-form label-width="240px" label-position="left" class="config-form">
          <el-form-item v-for="item in itemsOf(group)" :key="item.key" :label="item.name">
            <!-- 开关 -->
            <el-switch v-if="item.type === 'bool'" v-model="form[item.key]" active-value="true" inactive-value="false" />
            <!-- 整数 -->
            <el-input-number
              v-else-if="item.type === 'int'"
              v-model="numberValues[item.key]"
              :min="item.min != null ? item.min : 0"
              :max="item.max != null ? item.max : 999999"
              @change="(val) => (form[item.key] = String(val))"
            />
            <!-- 下拉选项 -->
            <el-select v-else-if="item.type === 'option'" v-model="form[item.key]" style="width: 320px">
              <el-option
                v-for="option in item.options"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
            <!-- 文本 -->
            <el-input v-else v-model.trim="form[item.key]" style="width: 320px" />

            <div class="item-remark">
              {{ item.remark }}
              <span v-if="item.type === 'bool'" class="text-muted">
                （{{ form[item.key] === 'true' ? '已开启' : '已关闭' }}）
              </span>
            </div>
          </el-form-item>
        </el-form>
      </div>

      <el-divider />

      <div class="config-footer">
        <el-button type="primary" :loading="saving" @click="onSave">保存配置</el-button>
        <el-button @click="onReset" :disabled="saving">恢复默认</el-button>
        <div class="text-muted footer-tip">
          提示：密码规则影响「新增用户 / 重置密码 / 用户自助修改密码」；
          「首次登录强制修改密码」开启后，新增用户与被重置密码的用户下次登录需先改密；
          分区频率调整后，后端会立即按新频率续建分区（已有分区保持不变）。
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { configApi } from '../../api'

const loading = ref(false)
const saving = ref(false)
const groupNames = ref([])
const items = ref([])
// 表单值（统一以字符串保存，与后端存储格式一致）
const form = reactive({})
// 整数型配置项的数字绑定（el-input-number 需要数字）
const numberValues = reactive({})
// 加载时的原始值（用于计算改动项）
const originValues = ref({})

const dirtyCount = computed(() => changedValues() ? Object.keys(changedValues()).length : 0)

onMounted(loadData)

async function loadData() {
  loading.value = true
  try {
    const res = await configApi.list()
    const groups = res.data || {}
    groupNames.value = Object.keys(groups)
    const all = []
    Object.keys(groups).forEach((group) => {
      ;(groups[group] || []).forEach((item) => all.push(item))
    })
    items.value = all
    all.forEach((item) => {
      form[item.key] = item.value
      numberValues[item.key] = Number(item.value)
    })
    const snapshot = {}
    all.forEach((item) => {
      snapshot[item.key] = item.value
    })
    originValues.value = snapshot
  } catch (e) {
    // 错误提示已由 axios 拦截器统一处理
  } finally {
    loading.value = false
  }
}

function itemsOf(group) {
  return items.value.filter((item) => item.group === group)
}

/** 与加载值比对，得到待保存的配置项 */
function changedValues() {
  const changed = {}
  Object.keys(form).forEach((key) => {
    if (String(form[key]) !== String(originValues.value[key])) {
      changed[key] = String(form[key])
    }
  })
  return changed
}

async function onSave() {
  const changed = changedValues()
  if (Object.keys(changed).length === 0) {
    ElMessage.info('配置没有变化')
    return
  }
  saving.value = true
  try {
    const res = await configApi.save(changed)
    ElMessage.success(res.msg || '配置已保存并即时生效')
    await loadData()
  } catch (e) {
    // 校验失败等提示已由拦截器处理
  } finally {
    saving.value = false
  }
}

async function onReset() {
  try {
    await ElMessageBox.confirm('确定将全部规则配置恢复为默认值吗？（立即生效）', '提示', {
      confirmButtonText: '确定恢复',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch (e) {
    return
  }
  saving.value = true
  try {
    const res = await configApi.reset()
    ElMessage.success(res.msg || '已恢复默认配置')
    await loadData()
  } catch (e) {
    // 忽略：提示已由拦截器处理
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.tip-alert {
  margin-bottom: 16px;
}

.config-card {
  padding-bottom: 8px;
}

.config-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.dirty-tip {
  margin-left: 8px;
  color: var(--el-color-warning);
  font-size: 13px;
}

.config-group {
  margin-top: 8px;
}

.group-title {
  margin: 16px 0 8px;
  padding-left: 8px;
  border-left: 3px solid var(--el-color-primary);
  font-size: 15px;
  font-weight: 600;
}

.config-form :deep(.el-form-item) {
  margin-bottom: 18px;
}

.item-remark {
  width: 100%;
  margin-top: 2px;
  color: #909399;
  font-size: 12px;
  line-height: 1.6;
}

.config-footer {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.footer-tip {
  margin-top: 8px;
  width: 100%;
  font-size: 12px;
  line-height: 1.7;
}

code {
  font-family: Consolas, Monaco, monospace;
}
</style>
