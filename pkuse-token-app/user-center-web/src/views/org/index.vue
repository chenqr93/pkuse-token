<template>
  <div class="pd-page">
    <!-- 说明 -->
    <a-alert type="info" :closable="false" show-icon class="tip-alert">
      <template slot="message">
        组织架构为省 / 市 / 县 / 乡镇街道四级树形结构，用户可在「用户管理」中归属到组织节点。
        层级为档案属性（手动选择，允许与树深度不一致）；删除组织前需先删除其下级组织并移除挂载用户。
      </template>
    </a-alert>

    <!-- 查询条件 -->
    <div class="pd-search-card">
      <div class="pd-toolbar">
        <a-form-model layout="inline" :model="query" @submit.prevent>
          <a-form-model-item label="单位名称">
            <a-input v-model.trim="query.orgName" placeholder="单位名称" allow-clear style="width: 180px" @keyup.enter="onSearch" />
          </a-form-model-item>
          <a-form-model-item label="简称">
            <a-input v-model.trim="query.shortName" placeholder="单位简称" allow-clear style="width: 150px" @keyup.enter="onSearch" />
          </a-form-model-item>
          <a-form-model-item label="层级">
            <a-select v-model="query.orgLevel" placeholder="全部" allow-clear style="width: 130px">
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
            <a-icon type="plus" /><span>新增组织</span>
          </a-button>
        </div>
      </div>
    </div>

    <!-- 组织树 -->
    <div class="pd-table-card">
      <a-table
        :columns="columns"
        :data-source="treeData"
        :loading="loading"
        :row-key="'id'"
        :pagination="false"
        :expanded-row-keys="expandedRowKeys"
        :scroll="{ x: 1180 }"
        @expandedRowsChange="onExpandedRowsChange"
      >
        <template slot="orgName" slot-scope="text, record">
          <span>{{ record.orgName }}</span>
          <a-tooltip v-if="record.remark" :title="record.remark" placement="top">
            <a-icon type="info-circle" class="remark-icon" />
          </a-tooltip>
        </template>
        <template slot="orgLevel" slot-scope="text, record">
          <a-tag :class="levelTagClass(record.orgLevel)">{{ levelLabel(record.orgLevel) }}</a-tag>
        </template>
        <template slot="region" slot-scope="text, record">
          <div class="region-cell">
            <span>{{ record.regionName || '-' }}</span>
            <span v-if="record.regionCode" class="text-mono text-muted region-code">{{ record.regionCode }}</span>
          </div>
        </template>
        <template slot="shortName" slot-scope="text, record">{{ record.shortName || '-' }}</template>
        <template slot="creditCode" slot-scope="text, record">
          <span v-if="record.creditCode" class="text-mono">{{ record.creditCode }}</span>
          <span v-else class="text-muted">-</span>
        </template>
        <template slot="action" slot-scope="text, record">
          <a-button type="link" @click="openCreateChild(record)">新增下级</a-button>
          <a-button type="link" @click="openEdit(record)">编辑</a-button>
          <a-button type="link" class="pd-link-danger" @click="onDelete(record)">删除</a-button>
        </template>
      </a-table>
      <div class="tree-count text-muted">共 {{ totalCount }} 个组织节点</div>
    </div>

    <!-- 新增 / 编辑弹窗 -->
    <a-modal
      :visible="dialogVisible"
      :title="form.id ? '编辑组织' : '新增组织'"
      width="640px"
      :mask-closable="false"
      @cancel="dialogVisible = false"
    >
      <a-form-model
        ref="formRef"
        :model="form"
        :rules="rules"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 18 }"
      >
        <a-form-model-item label="上级组织" prop="parentId">
          <a-tree-select
            v-model="form.parentId"
            :tree-data="parentOptions"
            :replace-fields="orgTreeFields"
            allow-clear
            show-search
            tree-node-filter-prop="title"
            placeholder="不选择则为顶级组织"
            style="width: 100%"
          />
          <div class="form-tip text-muted">层级为档案属性，由下方手动选择，不随挂载位置自动推导</div>
        </a-form-model-item>
        <a-form-model-item label="层级" prop="orgLevel">
          <a-select v-model="form.orgLevel" placeholder="请选择层级" style="width: 100%">
            <a-select-option v-for="item in levelOptions" :key="item.value" :value="item.value">
              {{ item.label }}
            </a-select-option>
          </a-select>
        </a-form-model-item>
        <a-form-model-item label="单位名称" prop="orgName">
          <a-input v-model.trim="form.orgName" placeholder="单位全称，同一上级下不可重复" :max-length="128" />
        </a-form-model-item>
        <a-form-model-item label="简称" prop="shortName">
          <a-input v-model.trim="form.shortName" placeholder="单位简称" :max-length="64" />
        </a-form-model-item>
        <a-form-model-item label="统一社会信用代码" prop="creditCode">
          <a-input v-model.trim="form.creditCode" placeholder="18 位，选填" :max-length="18" />
        </a-form-model-item>
        <a-form-model-item label="所属区划" prop="regionPath">
          <a-cascader
            v-model="form.regionPath"
            :options="regionOptions"
            :field-names="regionFieldNames"
            :load-data="loadRegionNode"
            change-on-select
            allow-clear
            show-search
            placeholder="请选择省 / 市 / 县 / 街道（可停在任意层级）"
            style="width: 100%"
            @change="onRegionChange"
          />
          <div class="form-tip text-muted">
            数据来自「区划管理」，选中后自动回填区划代码与名称；直辖市市级为「市辖区」
          </div>
        </a-form-model-item>
        <a-form-model-item label="排序" prop="sort">
          <a-input-number v-model="form.sort" :min="0" :max="9999" style="width: 160px" />
        </a-form-model-item>
        <a-form-model-item label="备注">
          <a-textarea v-model="form.remark" :rows="2" :max-length="255" placeholder="备注" />
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
import { orgApi, regionApi } from '../../api'

/** 层级选项（与后端 UcOrgUnit 常量一致：1=省 2=市 3=县 4=乡镇街道） */
const levelOptions = [
  { label: '省', value: 1 },
  { label: '市', value: 2 },
  { label: '县（区）', value: 3 },
  { label: '乡镇（街道）', value: 4 }
]

/** 层级标签颜色 */
const levelTagClasses = {
  1: 'pd-tag-red',
  2: 'pd-tag-orange',
  3: 'pd-tag-blue',
  4: 'pd-tag-gray'
}

/** 上级组织 / 表单组织选择：orgName / children 数据映射为 a-tree-select 节点 */
const orgTreeFields = {
  title: 'orgName',
  label: 'orgName',
  value: 'id',
  key: 'id',
  children: 'children'
}

/** 区划级联：regionCode / regionName 数据映射 */
const regionFieldNames = {
  label: 'regionName',
  value: 'regionCode',
  children: 'children'
}

export default {
  name: 'OrgManage',
  data() {
    return {
      levelOptions,
      orgTreeFields,
      regionFieldNames,
      loading: false,
      saving: false,
      treeData: [],
      totalCount: 0,
      // 组织树默认全部展开（异步数据下 default-expand-all-rows 不生效，改用受控 expandedRowKeys）
      expandedRowKeys: [],
      // 全量组织树（仅用于表单里的「上级组织」选择：列表过滤时不影响可选范围）
      fullTree: [],
      query: { orgName: '', shortName: '', orgLevel: null },
      dialogVisible: false,
      // 区划级联选项（根节点预加载，展开时按 parentCode 懒加载下级）
      regionOptions: [],
      form: {
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
      },
      rules: {
        orgName: [{ required: true, message: '请输入单位名称', trigger: 'blur' }],
        orgLevel: [{ required: true, message: '请选择层级', trigger: 'change' }],
        regionPath: [{ required: true, message: '请选择所属区划', trigger: 'change' }],
        creditCode: [{ validator: this.validateCreditCode, trigger: 'blur' }]
      },
      columns: [
        {
          title: '单位名称',
          dataIndex: 'orgName',
          width: 260,
          ellipsis: true,
          scopedSlots: { customRender: 'orgName' }
        },
        {
          title: '层级',
          dataIndex: 'orgLevel',
          width: 110,
          align: 'center',
          scopedSlots: { customRender: 'orgLevel' }
        },
        {
          title: '区划',
          dataIndex: 'regionName',
          width: 180,
          ellipsis: true,
          scopedSlots: { customRender: 'region' }
        },
        {
          title: '简称',
          dataIndex: 'shortName',
          width: 140,
          ellipsis: true,
          scopedSlots: { customRender: 'shortName' }
        },
        {
          title: '统一社会信用代码',
          dataIndex: 'creditCode',
          width: 200,
          ellipsis: true,
          scopedSlots: { customRender: 'creditCode' }
        },
        { title: '排序', dataIndex: 'sort', width: 80, align: 'center' },
        {
          title: '操作',
          key: 'action',
          width: 210,
          fixed: 'right',
          scopedSlots: { customRender: 'action' }
        }
      ]
    }
  },
  computed: {
    /** 上级组织可选树：编辑时剔除自身及其子孙（后端也会兜底校验） */
    parentOptions() {
      if (!this.form.id) {
        return this.fullTree
      }
      return filterOutSubtree(this.fullTree, this.form.id)
    }
  },
  async mounted() {
    await this.loadData()
    await this.loadFullTree()
    await this.loadRegionOptions()
  },
  methods: {
    async loadData() {
      this.loading = true
      try {
        const res = await orgApi.tree({
          orgName: this.query.orgName || undefined,
          shortName: this.query.shortName || undefined,
          orgLevel: this.query.orgLevel === null || this.query.orgLevel === '' ? undefined : this.query.orgLevel
        })
        this.treeData = res.data?.list || []
        this.totalCount = res.data?.count || 0
        this.expandedRowKeys = collectNodeIds(this.treeData)
      } catch (e) {
        // 错误提示已由 axios 拦截器统一处理
      } finally {
        this.loading = false
      }
    },
    /** 加载全量组织树（上级组织选择用，不受列表过滤条件影响） */
    async loadFullTree() {
      try {
        const res = await orgApi.tree()
        this.fullTree = res.data?.list || []
      } catch (e) {
        // 错误提示已由 axios 拦截器统一处理
      }
    },
    /** 加载区划级联根节点（省），下级靠 loadRegionNode 懒加载 */
    async loadRegionOptions() {
      try {
        const res = await regionApi.cascade()
        this.regionOptions = (res.data || []).map((item) => ({
          regionCode: item.code,
          regionName: item.name,
          isLeaf: !!item.leaf
        }))
      } catch (e) {
        // 错误提示已由 axios 拦截器统一处理
      }
    },
    /** a-cascader 懒加载：把下一级挂到当前选中节点上 */
    loadRegionNode(selectedOptions) {
      const target = selectedOptions[selectedOptions.length - 1]
      if (!target) {
        return
      }
      target.loading = true
      regionApi
        .cascade(target.regionCode)
        .then((res) => {
          target.loading = false
          target.children = (res.data || []).map((item) => ({
            regionCode: item.code,
            regionName: item.name,
            isLeaf: !!item.leaf
          }))
          this.regionOptions = this.regionOptions.slice()
        })
        .catch(() => {
          target.loading = false
          this.regionOptions = this.regionOptions.slice()
        })
    },
    /** 由 12 位区划编码推导级联回显路径（按有效位判断缺级，缺级自动跳过） */
    deriveRegionPath(code) {
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
    },
    /** 级联选择变化：回填区划代码与名称（名称为各级名称 / 拼接，超长回退叶名） */
    onRegionChange(value, selectedOptions) {
      if (!value || !value.length) {
        this.form.regionCode = ''
        this.form.regionName = ''
        return
      }
      this.form.regionCode = value[value.length - 1]
      const nodes = selectedOptions || []
      const labels = nodes.map((node) => node.regionName || '')
      const joined = labels.join('/')
      this.form.regionName = joined.length > 60 ? (labels[labels.length - 1] || '') : joined
    },
    validateCreditCode(rule, value, callback) {
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
    onSearch() {
      this.loadData()
    },
    onReset() {
      this.query.orgName = ''
      this.query.shortName = ''
      this.query.orgLevel = null
      this.loadData()
    },
    onExpandedRowsChange(keys) {
      this.expandedRowKeys = keys || []
    },
    levelLabel(value) {
      const item = levelOptions.find((option) => option.value === value)
      return item ? item.label : '-'
    },
    levelTagClass(value) {
      return levelTagClasses[value] || 'pd-tag-gray'
    },
    resetFormFields() {
      this.form.id = null
      this.form.parentId = null
      this.form.orgLevel = null
      this.form.orgName = ''
      this.form.shortName = ''
      this.form.creditCode = ''
      this.form.regionPath = null
      this.form.regionName = ''
      this.form.regionCode = ''
      this.form.sort = 0
      this.form.remark = ''
    },
    clearFormValidate() {
      const formRef = this.$refs.formRef
      if (formRef) {
        formRef.clearValidate()
      }
    },
    openCreate() {
      this.resetFormFields()
      // 顶级默认按省级建档（可改：层级允许与树深度不一致）
      this.form.orgLevel = 1
      this.dialogVisible = true
      this.$nextTick(this.clearFormValidate)
    },
    /** 新增下级：预挂当前节点，层级默认取父层级 + 1（乡镇街道封顶，可改） */
    openCreateChild(row) {
      this.resetFormFields()
      this.form.parentId = row.id
      this.form.orgLevel = Math.min((row.orgLevel || 1) + 1, 4)
      this.dialogVisible = true
      this.$nextTick(this.clearFormValidate)
    },
    openEdit(row) {
      this.resetFormFields()
      Object.assign(this.form, {
        id: row.id,
        parentId: row.parentId && row.parentId !== '0' ? row.parentId : null,
        orgLevel: row.orgLevel,
        orgName: row.orgName,
        shortName: row.shortName || '',
        creditCode: row.creditCode || '',
        regionPath: this.deriveRegionPath(row.regionCode),
        regionName: row.regionName || '',
        regionCode: row.regionCode || '',
        sort: row.sort ?? 0,
        remark: row.remark
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
          parentId: this.form.parentId || null,
          orgLevel: this.form.orgLevel,
          orgName: this.form.orgName,
          shortName: this.form.shortName,
          creditCode: this.form.creditCode,
          regionName: this.form.regionName,
          regionCode: this.form.regionCode,
          sort: this.form.sort,
          remark: this.form.remark
        }
        if (this.form.id) {
          await orgApi.update({ ...payload, id: this.form.id })
          this.$message.success('保存成功')
        } else {
          await orgApi.create(payload)
          this.$message.success('新增成功')
        }
        this.dialogVisible = false
        this.loadData()
        this.loadFullTree()
      } catch (e) {
        // 错误提示已由 axios 拦截器统一处理
      } finally {
        this.saving = false
      }
    },
    onDelete(row) {
      this.$confirm({
        title: '危险操作',
        content: `确定要删除组织「${row.orgName}」吗？存在下级组织或仍有用户挂载时将被拦截。`,
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
        await orgApi.remove(row.id)
        this.$message.success('删除成功')
        this.loadData()
        this.loadFullTree()
      } catch (e) {
        // 错误提示已由 axios 拦截器统一处理（如：存在下级 / 有用户挂载）
      }
    }
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

/** 收集树中所有节点 id（用于受控展开全部节点） */
function collectNodeIds(nodes) {
  const ids = []
  const walk = (list) => {
    ;(list || []).forEach((node) => {
      ids.push(node.id)
      if (node.children && node.children.length) {
        walk(node.children)
      }
    })
  }
  walk(nodes)
  return ids
}
</script>

<style lang="less" scoped>
@import '../../styles/tokens.less';

.tip-alert {
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

.tree-count {
  margin-top: @pd-space-md;
  padding-bottom: @pd-space-md;
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
  color: @pd-color-text-muted;
  vertical-align: -2px;
  cursor: help;
}
</style>
