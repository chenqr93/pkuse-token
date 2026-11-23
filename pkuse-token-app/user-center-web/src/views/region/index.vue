<template>
  <div class="pd-page">
    <!-- 说明 -->
    <a-alert type="info" :closable="false" show-icon class="tip-alert">
      <template slot="message">
        区划为省 / 市 / 县 / 乡镇街道四级<b>扁平化字典</b>：每行为一条完整路径，编码为 12 位国标统计口径
        （有效位 + 尾部补零，如建北街道 <span class="text-mono">130102001000</span>）。
        缺级列留空（如省级行、直筒子市）；最终区划编码 / 层级由最深层级<b>自动推导</b>且不可变更。
      </template>
    </a-alert>

    <!-- 查询条件 -->
    <div class="pd-search-card">
      <div class="pd-toolbar">
        <a-form-model layout="inline" :model="query" @submit.prevent>
          <a-form-model-item label="关键字">
            <a-input v-model.trim="query.keyword" placeholder="任一层级名称 / 最终编码前缀" allow-clear style="width: 220px" @keyup.enter="onSearch" />
          </a-form-model-item>
          <a-form-model-item label="省份">
            <a-select v-model="query.provinceCode" placeholder="全部" allow-clear show-search style="width: 160px">
              <a-select-option v-for="item in provinceOptions" :key="item.code" :value="item.code">
                {{ item.name }}
              </a-select-option>
            </a-select>
          </a-form-model-item>
          <a-form-model-item label="最终层级">
            <a-select v-model="query.finalLevel" placeholder="全部" allow-clear style="width: 130px">
              <a-select-option v-for="item in levelOptions" :key="item.value" :value="item.value">
                {{ item.label }}
              </a-select-option>
            </a-select>
          </a-form-model-item>
          <a-form-model-item>
            <a-button type="primary" @click="onSearch">
              <a-icon type="search" /><span>查询</span>
            </a-button>
            <a-button @click="onReset">重置</a-button>
          </a-form-model-item>
        </a-form-model>
        <div class="pd-toolbar-right">
          <a-button type="primary" @click="openCreate">
            <a-icon type="plus" /><span>新增区划</span>
          </a-button>
        </div>
      </div>
    </div>

    <!-- 区划列表 -->
    <div class="pd-table-card">
      <a-table
        :columns="columns"
        :data-source="records"
        :loading="loading"
        :row-key="'id'"
        :pagination="false"
        :scroll="{ x: 1090 }"
      >
        <template slot="province" slot-scope="text, record">
          <div class="region-cell">
            <span>{{ record.provinceName }}</span>
            <span class="text-mono text-muted region-code">{{ record.provinceCode }}</span>
          </div>
        </template>
        <template slot="city" slot-scope="text, record">
          <div v-if="record.cityCode" class="region-cell">
            <span>{{ record.cityName }}</span>
            <span class="text-mono text-muted region-code">{{ record.cityCode }}</span>
          </div>
          <span v-else class="text-muted">-</span>
        </template>
        <template slot="county" slot-scope="text, record">
          <div v-if="record.countyCode" class="region-cell">
            <span>{{ record.countyName }}</span>
            <span class="text-mono text-muted region-code">{{ record.countyCode }}</span>
          </div>
          <span v-else class="text-muted">-</span>
        </template>
        <template slot="street" slot-scope="text, record">
          <div v-if="record.streetCode" class="region-cell">
            <span>{{ record.streetName }}</span>
            <span class="text-mono text-muted region-code">{{ record.streetCode }}</span>
          </div>
          <span v-else class="text-muted">-</span>
        </template>
        <template slot="finalCode" slot-scope="text, record">
          <span class="text-mono">{{ record.finalCode }}</span>
        </template>
        <template slot="finalName" slot-scope="text, record">{{ record.finalName }}</template>
        <template slot="finalLevel" slot-scope="text, record">
          <a-tag :class="levelTagClass(record.finalLevel)">{{ levelLabel(record.finalLevel) }}</a-tag>
        </template>
        <template slot="action" slot-scope="text, record">
          <a-button type="link" @click="openEdit(record)">编辑</a-button>
          <a-button type="link" class="pd-link-danger" @click="onDelete(record)">删除</a-button>
        </template>
      </a-table>

      <a-pagination
        class="pd-pagination"
        :current="query.current"
        :page-size="query.size"
        :total="total"
        :show-total="showTotalText"
        show-size-changer
        show-quick-jumper
        :page-size-options="['10', '20', '50', '100']"
        @change="onPageChange"
        @showSizeChange="onSizeChange"
      />
    </div>

    <!-- 新增 / 编辑弹窗 -->
    <a-modal
      :visible="dialogVisible"
      :title="form.id ? '编辑区划' : '新增区划'"
      width="680px"
      :mask-closable="false"
      @cancel="dialogVisible = false"
    >
      <a-alert type="warning" :closable="false" show-icon class="form-tip-alert">
        <template slot="message">
          编辑时最终区划编码不可变更；补零规则：省后 10 位为 0、市后 8 位为 0、县后 6 位为 0、街道后 3 位为 0。
        </template>
      </a-alert>
      <a-form-model
        ref="formRef"
        :model="form"
        :rules="rules"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 18 }"
      >
        <a-form-model-item label="省编码 / 名称" required>
          <div class="pair-row">
            <a-input v-model.trim="form.provinceCode" placeholder="130000000000" :max-length="12" class="pair-code" />
            <a-input v-model.trim="form.provinceName" placeholder="省名称" :max-length="64" class="pair-name" />
          </div>
        </a-form-model-item>
        <a-form-model-item label="市编码 / 名称">
          <div class="pair-row">
            <a-input v-model.trim="form.cityCode" placeholder="后 8 位为 0，缺级留空" :max-length="12" class="pair-code" />
            <a-input v-model.trim="form.cityName" placeholder="市名称（直辖市为「市辖区」）" :max-length="64" class="pair-name" />
          </div>
        </a-form-model-item>
        <a-form-model-item label="县（区）编码 / 名称">
          <div class="pair-row">
            <a-input v-model.trim="form.countyCode" placeholder="后 6 位为 0，缺级留空" :max-length="12" class="pair-code" />
            <a-input v-model.trim="form.countyName" placeholder="县（区）名称" :max-length="64" class="pair-name" />
          </div>
        </a-form-model-item>
        <a-form-model-item label="街道（乡镇）编码 / 名称">
          <div class="pair-row">
            <a-input v-model.trim="form.streetCode" placeholder="后 3 位为 0，缺级留空" :max-length="12" class="pair-code" />
            <a-input v-model.trim="form.streetName" placeholder="街道（乡镇）名称" :max-length="64" class="pair-name" />
          </div>
        </a-form-model-item>
        <a-form-model-item label="最终区划（自动推导）">
          <div class="final-preview text-mono">
            {{ finalPreview.code || '-' }}　{{ finalPreview.name || '按最深层级自动推导' }}
            <a-tag v-if="finalPreview.level" :class="levelTagClass(finalPreview.level)" class="final-tag">
              {{ levelLabel(finalPreview.level) }}
            </a-tag>
          </div>
        </a-form-model-item>
      </a-form-model>
      <template slot="footer">
        <a-button @click="dialogVisible = false">取消</a-button>
        <a-button type="primary" :loading="saving" @click="onSave">保存</a-button>
      </template>
    </a-modal>
  </div>
</template>

<script>
import { regionApi } from '../../api'

/** 最终层级选项（与后端 UcRegion 常量一致） */
const levelOptions = [
  { label: '省', value: 1 },
  { label: '市', value: 2 },
  { label: '县（区）', value: 3 },
  { label: '乡镇（街道）', value: 4 }
]

const levelTagClasses = {
  1: 'pd-tag-red',
  2: 'pd-tag-orange',
  3: 'pd-tag-blue',
  4: 'pd-tag-gray'
}

/** 各级编码校验正则（与后端一致：有效位 + 尾部补零） */
const codePatterns = {
  provinceCode: /^\d{2}0{10}$/,
  cityCode: /^\d{4}0{8}$/,
  countyCode: /^\d{6}0{6}$/,
  streetCode: /^\d{9}0{3}$/
}

export default {
  name: 'RegionManage',
  data() {
    return {
      levelOptions,
      loading: false,
      saving: false,
      records: [],
      total: 0,
      provinceOptions: [],
      query: { current: 1, size: 20, keyword: '', provinceCode: null, finalLevel: null },
      dialogVisible: false,
      sizeChangeLock: false,
      form: {
        id: null,
        provinceCode: '',
        provinceName: '',
        cityCode: '',
        cityName: '',
        countyCode: '',
        countyName: '',
        streetCode: '',
        streetName: ''
      },
      rules: {
        provinceCode: [{ validator: this.validateProvinceCode, trigger: 'blur' }],
        provinceName: [{ required: true, message: '请输入省区划名称', trigger: 'blur' }],
        cityCode: [this.buildLevelRule('cityCode', 'cityName', '市', 'provinceCode', 2)],
        countyCode: [this.buildLevelRule('countyCode', 'countyName', '县', 'cityCode', 4)],
        streetCode: [this.buildLevelRule('streetCode', 'streetName', '街道', 'countyCode', 6)]
      },
      columns: [
        {
          title: '省',
          dataIndex: 'provinceName',
          width: 140,
          ellipsis: true,
          scopedSlots: { customRender: 'province' }
        },
        {
          title: '市',
          dataIndex: 'cityName',
          width: 140,
          ellipsis: true,
          scopedSlots: { customRender: 'city' }
        },
        {
          title: '县（区）',
          dataIndex: 'countyName',
          width: 140,
          ellipsis: true,
          scopedSlots: { customRender: 'county' }
        },
        {
          title: '街道（乡镇）',
          dataIndex: 'streetName',
          width: 140,
          ellipsis: true,
          scopedSlots: { customRender: 'street' }
        },
        {
          title: '最终区划编码',
          dataIndex: 'finalCode',
          width: 150,
          ellipsis: true,
          scopedSlots: { customRender: 'finalCode' }
        },
        {
          title: '最终区划名称',
          dataIndex: 'finalName',
          width: 130,
          ellipsis: true,
          scopedSlots: { customRender: 'finalName' }
        },
        {
          title: '最终层级',
          dataIndex: 'finalLevel',
          width: 110,
          align: 'center',
          scopedSlots: { customRender: 'finalLevel' }
        },
        {
          title: '操作',
          key: 'action',
          width: 140,
          fixed: 'right',
          scopedSlots: { customRender: 'action' }
        }
      ]
    }
  },
  computed: {
    /** 最终区划预览（按最深层级自动推导，与后端口径一致） */
    finalPreview() {
      if (this.form.streetCode) {
        return { code: this.form.streetCode, name: this.form.streetName, level: 4 }
      }
      if (this.form.countyCode) {
        return { code: this.form.countyCode, name: this.form.countyName, level: 3 }
      }
      if (this.form.cityCode) {
        return { code: this.form.cityCode, name: this.form.cityName, level: 2 }
      }
      if (this.form.provinceCode) {
        return { code: this.form.provinceCode, name: this.form.provinceName, level: 1 }
      }
      return { code: '', name: '', level: null }
    }
  },
  async mounted() {
    await this.loadData()
    await this.loadProvinces()
  },
  methods: {
    async loadData() {
      this.loading = true
      try {
        const res = await regionApi.page({
          current: this.query.current,
          size: this.query.size,
          keyword: this.query.keyword || undefined,
          provinceCode: this.query.provinceCode || undefined,
          finalLevel: this.query.finalLevel === null || this.query.finalLevel === '' ? undefined : this.query.finalLevel
        })
        this.records = res.data?.records || []
        this.total = res.data?.total || 0
      } catch (e) {
        // 错误提示已由 axios 拦截器统一处理
      } finally {
        this.loading = false
      }
    },
    /** 省份下拉选项（级联根级） */
    async loadProvinces() {
      try {
        const res = await regionApi.cascade()
        this.provinceOptions = res.data || []
      } catch (e) {
        // 错误提示已由 axios 拦截器统一处理
      }
    },
    onSearch() {
      this.query.current = 1
      this.loadData()
    },
    onReset() {
      this.query.keyword = ''
      this.query.provinceCode = null
      this.query.finalLevel = null
      this.onSearch()
    },
    showTotalText(total) {
      return `共 ${total} 条`
    },
    onPageChange(page, pageSize) {
      if (this.sizeChangeLock) {
        return
      }
      this.query.current = page
      if (pageSize && pageSize !== this.query.size) {
        this.query.size = pageSize
      }
      this.loadData()
    },
    onSizeChange(current, size) {
      this.sizeChangeLock = true
      const nextSize = Number(size)
      this.query.size = Number.isNaN(nextSize) ? this.query.size : nextSize
      this.query.current = 1
      this.loadData()
      this.$nextTick(() => {
        this.sizeChangeLock = false
      })
    },
    levelLabel(value) {
      const item = levelOptions.find((option) => option.value === value)
      return item ? item.label : '-'
    },
    levelTagClass(value) {
      return levelTagClasses[value] || 'pd-tag-gray'
    },
    /** 逐级前缀校验规则：编码需与上级有效位前缀一致 */
    buildLevelRule(codeKey, nameKey, label, parentKey, parentSignificant) {
      return {
        validator: (rule, value, callback) => {
          const form = this.form
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
    },
    validateProvinceCode(rule, value, callback) {
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
    resetFormFields() {
      this.form.id = null
      this.form.provinceCode = ''
      this.form.provinceName = ''
      this.form.cityCode = ''
      this.form.cityName = ''
      this.form.countyCode = ''
      this.form.countyName = ''
      this.form.streetCode = ''
      this.form.streetName = ''
    },
    clearFormValidate() {
      const formRef = this.$refs.formRef
      if (formRef) {
        formRef.clearValidate()
      }
    },
    openCreate() {
      this.resetFormFields()
      // 默认预填河北省省级路径（当前区划库唯一省份），便于快速新增
      this.form.provinceCode = '130000000000'
      this.form.provinceName = '河北省'
      this.dialogVisible = true
      this.$nextTick(this.clearFormValidate)
    },
    openEdit(row) {
      this.resetFormFields()
      Object.assign(this.form, {
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
      this.dialogVisible = true
      this.$nextTick(this.clearFormValidate)
    },
    async onSave() {
      try {
        await this.$refs.formRef.validate()
      } catch (e) {
        return
      }
      this.saving = true
      try {
        const payload = {
          provinceCode: this.form.provinceCode.trim(),
          provinceName: this.form.provinceName.trim(),
          cityCode: this.form.cityCode.trim() || null,
          cityName: this.form.cityName.trim() || null,
          countyCode: this.form.countyCode.trim() || null,
          countyName: this.form.countyName.trim() || null,
          streetCode: this.form.streetCode.trim() || null,
          streetName: this.form.streetName.trim() || null
        }
        if (this.form.id) {
          await regionApi.update({ ...payload, id: this.form.id })
          this.$message.success('修改成功')
        } else {
          await regionApi.create(payload)
          this.$message.success('新增成功')
        }
        this.dialogVisible = false
        this.loadData()
      } catch (e) {
        // 错误提示已由 axios 拦截器统一处理
      } finally {
        this.saving = false
      }
    },
    onDelete(row) {
      this.$confirm({
        title: '危险操作',
        content: `确定要删除区划「${row.finalName}（${row.finalCode}）」吗？删除后不可恢复。`,
        okText: '确定删除',
        cancelText: '取消',
        okType: 'danger',
        onOk: () => {
          this.doDelete(row)
        },
        onCancel: () => {}
      })
    },
    async doDelete(row) {
      try {
        await regionApi.remove(row.id)
        this.$message.success('删除成功')
        this.loadData()
      } catch (e) {
        // 错误提示已由 axios 拦截器统一处理
      }
    }
  }
}
</script>

<style lang="less" scoped>
@import '../../styles/tokens.less';

.tip-alert {
  margin-bottom: @pd-space-lg;
}

.form-tip-alert {
  margin-bottom: @pd-space-lg;
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
  gap: @pd-space-sm;
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
  background-color: @pd-color-bg;
  border-radius: 4px;
  font-size: 13px;
}

.final-tag {
  margin-left: 8px;
}
</style>
