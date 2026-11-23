<template>
  <div class="pd-page">
    <!-- 说明 -->
    <a-alert type="info" :closable="false" show-icon class="tip-alert">
      <template slot="message">
        接入应用对应统一登录（SSO）中的客户端：新增 / 修改 / 启用 / 禁用后<b>立即同步到认证中心</b>，无需重启服务。
        应用跳转 <code>/sso/auth?client=应用标识&amp;redirect=回调地址</code> 即可发起统一登录。
      </template>
    </a-alert>

    <!-- 查询条件 -->
    <div class="pd-search-card">
      <div class="pd-toolbar">
        <a-form-model layout="inline" :model="query" @submit.prevent>
          <a-form-model-item label="应用标识">
            <a-input v-model.trim="query.clientId" placeholder="如 sso-client1" allow-clear style="width: 180px" @keyup.enter="onSearch" />
          </a-form-model-item>
          <a-form-model-item label="应用名称">
            <a-input v-model.trim="query.clientName" placeholder="应用名称" allow-clear style="width: 160px" @keyup.enter="onSearch" />
          </a-form-model-item>
          <a-form-model-item label="状态">
            <a-select v-model="query.status" placeholder="全部" allow-clear style="width: 120px">
              <a-select-option :value="1">启用</a-select-option>
              <a-select-option :value="0">禁用</a-select-option>
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
            <a-icon type="plus" /><span>新增应用</span>
          </a-button>
        </div>
      </div>
    </div>

    <!-- 应用列表 -->
    <div class="pd-table-card">
      <a-table
        :columns="columns"
        :data-source="records"
        :loading="loading"
        :row-key="'id'"
        :pagination="false"
        :scroll="{ x: 1180 }"
      >
        <template slot="clientId" slot-scope="text, record">
          <span class="text-mono">{{ record.clientId }}</span>
        </template>
        <template slot="clientName" slot-scope="text, record">
          <span>{{ record.clientName }}</span>
          <a-tooltip v-if="record.remark" :title="record.remark" placement="top">
            <a-icon type="info-circle" class="remark-icon" />
          </a-tooltip>
        </template>
        <template slot="allowUrl" slot-scope="text, record">
          <span class="text-mono text-muted">{{ record.allowUrl || '-' }}</span>
        </template>
        <template slot="entryUrl" slot-scope="text, record">
          <span v-if="record.entryUrl" class="text-mono text-muted">{{ record.entryUrl }}</span>
          <span v-else class="text-muted">未配置</span>
        </template>
        <template slot="secretKey" slot-scope="text, record">
          <span class="text-mono">{{ maskSecret(record.secretKey) }}</span>
        </template>
        <template slot="authorizedUserCount" slot-scope="text, record">
          {{ record.authorizedUserCount ?? 0 }}
        </template>
        <template slot="status" slot-scope="text, record">
          <a-switch
            :checked="record.status === 1"
            :loading="togglingId === record.id"
            @change="onToggleStatus(record)"
          />
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
        :page-size-options="['10', '20', '50']"
        @change="onPageChange"
        @showSizeChange="onSizeChange"
      />
    </div>

    <!-- 新增 / 编辑弹窗 -->
    <a-modal
      :visible="dialogVisible"
      :title="form.id ? '编辑应用' : '新增应用'"
      width="620px"
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
        <a-form-model-item label="应用标识" prop="clientId">
          <a-input v-model.trim="form.clientId" placeholder="SSO client 参数，如 sso-client1" :disabled="!!form.id" :max-length="64" />
        </a-form-model-item>
        <a-form-model-item label="应用名称" prop="clientName">
          <a-input v-model.trim="form.clientName" placeholder="应用名称" :max-length="128" />
        </a-form-model-item>
        <a-form-model-item label="回调地址" prop="allowUrl">
          <a-textarea
            v-model.trim="form.allowUrl"
            :rows="2"
            placeholder="允许的授权回调地址，多个用英文逗号分隔；* 表示放行全部"
            :max-length="512"
          />
          <div class="form-tip text-muted">
            示例：<span class="text-mono">http://localhost:9001/sso/login</span>　或　<span class="text-mono">*</span>
          </div>
        </a-form-model-item>
        <a-form-model-item label="应用入口" prop="entryUrl">
          <a-input
            v-model.trim="form.entryUrl"
            placeholder="工作台点击进入应用时跳转的目标地址，留空则工作台不可进入"
            :max-length="255"
          />
          <div class="form-tip text-muted">
            通常填应用的登录入口，如 <span class="text-mono">http://localhost:9001/sso/login?back=http://localhost:9001/</span>
          </div>
        </a-form-model-item>
        <a-form-model-item label="接口秘钥" prop="secretKey">
          <div class="secret-row">
            <a-input v-model.trim="form.secretKey" placeholder="SSO 参数签名 / 消息推送校验使用" :max-length="128" />
            <a-button @click="generateSecret">随机生成</a-button>
          </div>
        </a-form-model-item>
        <a-form-model-item label="消息推送">
          <a-switch v-model="isPushEnabled" checked-children="开启" un-checked-children="关闭" />
        </a-form-model-item>
        <a-form-model-item v-if="form.isPush === 1" label="推送地址" prop="pushUrl">
          <a-input v-model.trim="form.pushUrl" placeholder="如 http://localhost:9003/sso/pushC" :max-length="255" />
        </a-form-model-item>
        <a-form-model-item label="单点注销">
          <a-switch v-model="isSloEnabled" checked-children="参与" un-checked-children="不参与" />
        </a-form-model-item>
        <a-form-model-item label="状态">
          <a-switch v-model="statusEnabled" checked-children="启用" un-checked-children="禁用" />
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
import { clientApi } from '../../api'

export default {
  name: 'ClientManage',
  data() {
    return {
      loading: false,
      saving: false,
      togglingId: null,
      records: [],
      total: 0,
      query: { current: 1, size: 10, clientId: '', clientName: '', status: null },
      dialogVisible: false,
      sizeChangeLock: false,
      form: {
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
      },
      rules: {
        clientId: [
          { required: true, message: '请输入应用标识', trigger: 'blur' },
          { pattern: /^[A-Za-z0-9_-]{2,64}$/, message: '应用标识仅支持字母、数字、下划线、中划线', trigger: 'blur' }
        ],
        clientName: [{ required: true, message: '请输入应用名称', trigger: 'blur' }],
        entryUrl: [{ validator: this.validateEntryUrl, trigger: 'blur' }],
        allowUrl: [{ validator: this.validateAllowUrl, trigger: 'blur' }]
      },
      columns: [
        {
          title: '应用标识',
          dataIndex: 'clientId',
          width: 140,
          scopedSlots: { customRender: 'clientId' }
        },
        {
          title: '应用名称',
          dataIndex: 'clientName',
          width: 150,
          ellipsis: true,
          scopedSlots: { customRender: 'clientName' }
        },
        {
          title: '授权回调地址',
          dataIndex: 'allowUrl',
          width: 190,
          ellipsis: true,
          scopedSlots: { customRender: 'allowUrl' }
        },
        {
          title: '应用入口地址',
          dataIndex: 'entryUrl',
          width: 200,
          ellipsis: true,
          scopedSlots: { customRender: 'entryUrl' }
        },
        {
          title: '接口秘钥',
          dataIndex: 'secretKey',
          width: 160,
          scopedSlots: { customRender: 'secretKey' }
        },
        {
          title: '已授权用户',
          dataIndex: 'authorizedUserCount',
          width: 110,
          align: 'center',
          scopedSlots: { customRender: 'authorizedUserCount' }
        },
        {
          title: '状态',
          dataIndex: 'status',
          width: 90,
          scopedSlots: { customRender: 'status' }
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
    // 状态开关的 1/0 约定在 a-switch 中用布尔代理保持数据形状
    isPushEnabled: {
      get() {
        return this.form.isPush === 1
      },
      set(value) {
        this.form.isPush = value ? 1 : 0
      }
    },
    isSloEnabled: {
      get() {
        return this.form.isSlo === 1
      },
      set(value) {
        this.form.isSlo = value ? 1 : 0
      }
    },
    statusEnabled: {
      get() {
        return this.form.status === 1
      },
      set(value) {
        this.form.status = value ? 1 : 0
      }
    }
  },
  mounted() {
    this.loadData()
  },
  methods: {
    async loadData() {
      this.loading = true
      try {
        const res = await clientApi.page({
          current: this.query.current,
          size: this.query.size,
          clientId: this.query.clientId || undefined,
          clientName: this.query.clientName || undefined,
          status: this.query.status === null || this.query.status === '' ? undefined : this.query.status
        })
        this.records = res.data?.records || []
        this.total = res.data?.total || 0
      } catch (e) {
        // 错误提示已由 axios 拦截器统一处理
      } finally {
        this.loading = false
      }
    },
    onSearch() {
      this.query.current = 1
      this.loadData()
    },
    onReset() {
      this.query.clientId = ''
      this.query.clientName = ''
      this.query.status = null
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
    resetFormFields() {
      this.form.id = null
      this.form.clientId = ''
      this.form.clientName = ''
      this.form.allowUrl = '*'
      this.form.entryUrl = ''
      this.form.secretKey = ''
      this.form.isPush = 0
      this.form.pushUrl = '/sso/pushC'
      this.form.isSlo = 1
      this.form.status = 1
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
      this.form.secretKey = this.randomSecret()
      this.dialogVisible = true
      this.$nextTick(this.clearFormValidate)
    },
    openEdit(row) {
      this.resetFormFields()
      Object.assign(this.form, {
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
      this.dialogVisible = true
      this.$nextTick(this.clearFormValidate)
    },
    randomSecret() {
      const chars = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789'
      let text = ''
      for (let i = 0; i < 32; i++) {
        text += chars.charAt(Math.floor(Math.random() * chars.length))
      }
      return text
    },
    generateSecret() {
      this.form.secretKey = this.randomSecret()
    },
    validateEntryUrl(rule, value, callback) {
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
    validateAllowUrl(rule, value, callback) {
      if (!value) {
        callback()
        return
      }
      const items = value
        .split(',')
        .map((item) => item.trim())
        .filter(Boolean)
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
    async onSave() {
      try {
        await this.$refs.formRef.validate()
      } catch (e) {
        return
      }
      this.saving = true
      try {
        const payload = {
          clientId: this.form.clientId,
          clientName: this.form.clientName,
          allowUrl: this.form.allowUrl,
          entryUrl: this.form.entryUrl,
          secretKey: this.form.secretKey,
          isPush: this.form.isPush,
          pushUrl: this.form.pushUrl,
          isSlo: this.form.isSlo,
          status: this.form.status,
          remark: this.form.remark
        }
        if (this.form.id) {
          await clientApi.update({ ...payload, id: this.form.id })
          this.$message.success('保存成功，已同步到认证中心')
        } else {
          await clientApi.create(payload)
          this.$message.success('新增成功，已同步到认证中心')
        }
        this.dialogVisible = false
        this.loadData()
      } catch (e) {
        // 错误提示已由 axios 拦截器统一处理
      } finally {
        this.saving = false
      }
    },
    onToggleStatus(row) {
      const target = row.status === 1 ? 0 : 1
      const action = target === 1 ? '启用' : '禁用'
      this.$confirm({
        title: '提示',
        content: `${action}后该应用${target === 1 ? '可以' : '将无法'}发起统一登录，确定要${action}「${row.clientName}」吗？`,
        okText: `确定${action}`,
        cancelText: '取消',
        onOk: () => {
          this.doChangeStatus(row, target)
        },
        onCancel: () => {}
      })
    },
    async doChangeStatus(row, target) {
      const action = target === 1 ? '启用' : '禁用'
      this.togglingId = row.id
      try {
        await clientApi.changeStatus(row.id, target)
        this.$message.success(`已${action}`)
        this.loadData()
      } catch (e) {
        // 错误提示已由 axios 拦截器统一处理
      } finally {
        this.togglingId = null
      }
    },
    onDelete(row) {
      this.$confirm({
        title: '危险操作',
        content: `确定要删除应用「${row.clientName}」吗？删除后该应用将无法发起统一登录。`,
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
        await clientApi.remove(row.id)
        this.$message.success('删除成功')
        this.loadData()
      } catch (e) {
        // 错误提示已由 axios 拦截器统一处理
      }
    },
    maskSecret(secret) {
      if (!secret) return '-'
      if (secret.length <= 10) return secret
      return secret.substring(0, 6) + '****' + secret.substring(secret.length - 4)
    }
  }
}
</script>

<style lang="less" scoped>
@import '../../styles/tokens.less';

.tip-alert {
  margin-bottom: @pd-space-lg;
}

.secret-row {
  display: flex;
  gap: @pd-space-sm;
  width: 100%;
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
