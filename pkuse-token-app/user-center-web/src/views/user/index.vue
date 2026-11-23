<template>
  <div class="pd-page">
    <!-- 查询条件 -->
    <div class="pd-search-card">
      <div class="pd-toolbar">
        <a-form-model layout="inline" :model="query" @submit.prevent>
          <a-form-model-item label="账号">
            <a-input v-model.trim="query.username" placeholder="登录账号" allow-clear style="width: 160px" @keyup.enter="onSearch" />
          </a-form-model-item>
          <a-form-model-item label="昵称">
            <a-input v-model.trim="query.nickname" placeholder="昵称" allow-clear style="width: 160px" @keyup.enter="onSearch" />
          </a-form-model-item>
          <a-form-model-item label="状态">
            <a-select v-model="query.status" placeholder="全部" allow-clear style="width: 120px">
              <a-select-option :value="1">启用</a-select-option>
              <a-select-option :value="0">禁用</a-select-option>
            </a-select>
          </a-form-model-item>
          <a-form-model-item label="所属组织">
            <a-tree-select
              v-model="query.orgId"
              :tree-data="orgTree"
              :replace-fields="orgTreeFields"
              allow-clear
              show-search
              tree-node-filter-prop="title"
              placeholder="全部"
              style="width: 200px"
            />
          </a-form-model-item>
          <a-form-model-item>
            <a-button type="primary" @click="onSearch">
              <a-icon type="search" /><span>查询</span>
            </a-button>
            <a-button @click="onReset">重置</a-button>
          </a-form-model-item>
        </a-form-model>
        <div class="pd-toolbar-right">
          <a-button
            type="primary"
            ghost
            :disabled="selectedRows.length === 0"
            @click="openBatchGrant"
          >
            <a-icon type="appstore" /><span>批量授权{{ selectedRows.length ? `（${selectedRows.length}）` : '' }}</span>
          </a-button>
          <a-button type="primary" @click="openCreate">
            <a-icon type="plus" /><span>新增用户</span>
          </a-button>
        </div>
      </div>
    </div>

    <!-- 用户列表 -->
    <div class="pd-table-card">
      <a-table
        :columns="columns"
        :data-source="records"
        :loading="loading"
        :row-key="'id'"
        :row-selection="rowSelection"
        :pagination="false"
        :scroll="{ x: 1410 }"
      >
        <template slot="username" slot-scope="text, record">
          <span class="text-mono">{{ record.username }}</span>
        </template>
        <template slot="orgName" slot-scope="text, record">{{ record.orgName || '-' }}</template>
        <!-- 身份证号列表脱敏展示（保留前六后四）；编辑弹窗回显明文，保存由后端国密 SM4 加密入库 -->
        <template slot="idCard" slot-scope="text, record">
          <span class="text-mono">{{ maskIdCard(record.idCard) }}</span>
        </template>
        <template slot="role" slot-scope="text, record">
          <a-tag :class="record.role === 'admin' ? 'pd-tag-red' : 'pd-tag-gray'">
            {{ record.role === 'admin' ? '管理员' : '普通用户' }}
          </a-tag>
        </template>
        <template slot="status" slot-scope="text, record">
          <a-switch
            :checked="record.status === 1"
            :loading="togglingId === record.id"
            @change="onToggleStatus(record)"
          />
          <a-tooltip
            v-if="isLocked(record)"
            :title="`登录锁定：${record.lockReason || '连续密码错误'}（至 ${formatTime(record.lockedUntil)}）`"
            placement="top"
          >
            <a-tag class="pd-tag-red lock-tag">已锁定</a-tag>
          </a-tooltip>
          <a-tooltip
            v-else-if="record.mustChangePassword === 1"
            title="开启「首次登录强制修改密码」后，该用户下次登录需先修改密码"
            placement="top"
          >
            <a-tag class="pd-tag-orange lock-tag">需改密</a-tag>
          </a-tooltip>
          <a-tooltip
            v-else-if="record.loginFailCount > 0"
            :title="`连续登录失败 ${record.loginFailCount} 次`"
            placement="top"
          >
            <a-tag class="pd-tag-orange lock-tag">失败{{ record.loginFailCount }}</a-tag>
          </a-tooltip>
        </template>
        <template slot="lastLoginTime" slot-scope="text, record">{{ formatTime(record.lastLoginTime) }}</template>
        <template slot="authClientCount" slot-scope="text, record">
          <a-tag v-if="record.authClientCount > 0" class="pd-tag-green">{{ record.authClientCount }} 应用</a-tag>
          <span v-else class="text-muted">未授权</span>
          <a-tag class="pd-tag-gray device-count">{{ record.deviceCount || 0 }} 设备</a-tag>
        </template>
        <template slot="action" slot-scope="text, record">
          <a-button type="link" @click="openEdit(record)">编辑</a-button>
          <a-button type="link" @click="openGrant(record)">授权</a-button>
          <a-button type="link" @click="openDevices(record)">设备</a-button>
          <a-button v-if="isLocked(record)" type="link" class="link-warning" @click="onUnlock(record)">解锁</a-button>
          <a-button type="link" @click="openResetPassword(record)">重置</a-button>
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
      :title="form.id ? '编辑用户' : '新增用户'"
      width="560px"
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
        <a-form-model-item label="登录账号" prop="username">
          <a-input v-model.trim="form.username" placeholder="登录账号（唯一）" :max-length="64" />
        </a-form-model-item>
        <a-form-model-item v-if="!form.id" label="初始密码" prop="password">
          <a-input-password
            v-model="form.password"
            :placeholder="`不填写则使用系统默认密码 ${passwordPolicy.defaultPassword || '123456'}`"
            :max-length="64"
          />
          <div class="field-tip text-muted">
            密码规则：{{ passwordRuleText }}
            <template v-if="passwordPolicy.complexityEnabled">
              ；规则由「规则配置」维护
            </template>
          </div>
        </a-form-model-item>
        <a-form-model-item label="昵称" prop="nickname">
          <a-input v-model.trim="form.nickname" placeholder="昵称" :max-length="64" />
        </a-form-model-item>
        <a-form-model-item label="邮箱" prop="email">
          <a-input v-model.trim="form.email" placeholder="邮箱" :max-length="128" />
        </a-form-model-item>
        <a-form-model-item label="手机号" prop="phone">
          <a-input v-model.trim="form.phone" placeholder="手机号" :max-length="32" />
        </a-form-model-item>
        <a-form-model-item label="身份证号" prop="idCard">
          <a-input v-model.trim="form.idCard" placeholder="身份证号（业务应用按身份证匹配人员）" :max-length="32" />
        </a-form-model-item>
        <a-form-model-item label="角色" prop="role">
          <a-select v-model="form.role" style="width: 100%">
            <a-select-option value="user">普通用户</a-select-option>
            <a-select-option value="admin">管理员</a-select-option>
          </a-select>
        </a-form-model-item>
        <a-form-model-item label="所属组织" prop="orgId">
          <a-tree-select
            v-model="form.orgId"
            :tree-data="orgTree"
            :replace-fields="orgTreeFields"
            allow-clear
            show-search
            tree-node-filter-prop="title"
            placeholder="不选择则未归属组织"
            style="width: 100%"
          />
        </a-form-model-item>
        <a-form-model-item label="状态">
          <a-switch v-model="statusEnabled" checked-children="启用" un-checked-children="禁用" />
        </a-form-model-item>
        <a-form-model-item label="备注">
          <a-textarea v-model="form.remark" :rows="2" :max-length="255" placeholder="备注" />
        </a-form-model-item>
      </a-form-model>
      <div v-if="form.id" class="dialog-meta text-muted">
        创建时间：{{ formatTime(form.createTime) }}
      </div>
      <template slot="footer">
        <a-button @click="dialogVisible = false">取消</a-button>
        <a-button type="primary" :loading="saving" @click="onSave">保存</a-button>
      </template>
    </a-modal>

    <!-- 授权应用弹窗 -->
    <a-modal
      :visible="grantVisible"
      :title="`授权应用 - ${grantUser.nickname || grantUser.username || ''}`"
      width="560px"
      :mask-closable="false"
      @cancel="grantVisible = false"
    >
      <a-alert type="info" :closable="false" show-icon class="grant-tip">
        <template slot="message">
          勾选该用户可访问的应用：普通用户登录后工作台<b>只展示已授权应用</b>，且只能进入已授权应用（系统管理员不受限）。
        </template>
      </a-alert>
      <a-spin :spinning="grantLoading">
        <div class="grant-body">
          <a-checkbox-group v-if="grantApps.length" v-model="grantChecked">
            <a-checkbox v-for="app in grantApps" :key="app.clientId" :value="app.clientId" class="grant-item">
              {{ app.clientName }}
              <span class="text-mono text-muted">（{{ app.clientId }}）</span>
            </a-checkbox>
          </a-checkbox-group>
          <a-empty v-else description="暂无可授权的应用（请先启用应用）" />
        </div>
      </a-spin>
      <template slot="footer">
        <a-button @click="grantChecked = []">清空</a-button>
        <a-button @click="grantChecked = grantApps.map((app) => app.clientId)">全选</a-button>
        <a-button @click="grantVisible = false">取消</a-button>
        <a-button type="primary" :loading="grantSaving" @click="onSaveGrant">保存</a-button>
      </template>
    </a-modal>

    <!-- 批量授权弹窗 -->
    <a-modal
      :visible="batchGrantVisible"
      title="批量授权应用"
      width="580px"
      :mask-closable="false"
      @cancel="batchGrantVisible = false"
    >
      <a-alert type="info" :closable="false" show-icon class="grant-tip">
        <template slot="message">
          已选择 <b>{{ selectedRows.length }}</b> 个用户：
          <span class="text-mono">{{ selectedUserNames }}</span>
        </template>
      </a-alert>

      <a-form-model :label-col="{ span: 5 }" :wrapper-col="{ span: 19 }">
        <a-form-model-item label="授权模式">
          <a-radio-group v-model="batchGrantMode">
            <a-radio value="append">追加授权（保留原有）</a-radio>
            <a-radio value="cover">覆盖授权（重置为勾选集合）</a-radio>
          </a-radio-group>
        </a-form-model-item>
        <a-form-model-item label="应用">
          <a-spin :spinning="batchGrantLoading">
            <div class="batch-apps">
              <a-checkbox-group v-if="grantApps.length" v-model="batchGrantChecked">
                <a-checkbox v-for="app in grantApps" :key="app.clientId" :value="app.clientId" class="grant-item">
                  {{ app.clientName }}
                  <span class="text-mono text-muted">（{{ app.clientId }}）</span>
                </a-checkbox>
              </a-checkbox-group>
              <a-empty v-else description="暂无可授权的应用（请先启用应用）" />
            </div>
          </a-spin>
        </a-form-model-item>
      </a-form-model>

      <template slot="footer">
        <a-button @click="batchGrantChecked = []">清空</a-button>
        <a-button @click="batchGrantChecked = grantApps.map((app) => app.clientId)">全选</a-button>
        <a-button @click="batchGrantVisible = false">取消</a-button>
        <a-button type="primary" :loading="batchGrantSaving" @click="onSaveBatchGrant">保存</a-button>
      </template>
    </a-modal>

    <!-- 设备管理弹窗（设备锁 / 设备指纹） -->
    <a-modal
      :visible="deviceVisible"
      :title="`设备管理 - ${deviceUser.nickname || deviceUser.username || ''}`"
      width="660px"
      @cancel="deviceVisible = false"
    >
      <a-alert type="info" :closable="false" show-icon class="grant-tip">
        <template slot="message">
          开启设备锁后，账号<b>仅在已绑定设备上允许登录</b>；解绑后该设备可重新作为新设备登录
          （登录日志页可查看每次登录的设备指纹）。
        </template>
      </a-alert>
      <a-table
        :columns="deviceColumns"
        :data-source="deviceList"
        :loading="deviceLoading"
        :row-key="'deviceId'"
        :pagination="false"
        :scroll="{ x: 760 }"
      >
        <template slot="deviceId" slot-scope="text, record">
          <span class="text-mono">{{ record.deviceId }}</span>
        </template>
        <template slot="deviceEnv" slot-scope="text, record">
          <span class="text-muted">{{ record.os }} / {{ record.browser }}</span>
        </template>
        <template slot="deviceLastLoginTime" slot-scope="text, record">{{ formatTime(record.lastLoginTime) }}</template>
        <template slot="deviceAction" slot-scope="text, record">
          <a-button type="link" class="pd-link-danger" @click="onUnbindDevice(record)">解绑</a-button>
        </template>
      </a-table>
      <a-empty v-if="!deviceLoading && !deviceList.length" description="暂无已绑定设备" />
      <template slot="footer">
        <a-button @click="deviceVisible = false">关闭</a-button>
      </template>
    </a-modal>

    <!-- 重置密码弹窗（自定义实现，沿用密码规则校验） -->
    <a-modal
      :visible="resetVisible"
      title="重置密码"
      width="480px"
      :mask-closable="false"
      @cancel="resetVisible = false"
    >
      <p class="reset-tip">为「{{ resetUser.nickname || resetUser.username || '' }}」设置新密码</p>
      <p class="reset-rule text-muted">密码规则：{{ passwordRuleText }}</p>
      <a-form-model ref="resetFormRef" :model="resetForm" :rules="resetRules">
        <a-form-model-item prop="password">
          <a-input-password
            v-model="resetForm.password"
            placeholder="请输入新密码"
            autocomplete="new-password"
            @keyup.enter="onResetPasswordSubmit"
          />
        </a-form-model-item>
      </a-form-model>
      <template slot="footer">
        <a-button @click="resetVisible = false">取消</a-button>
        <a-button type="primary" :loading="resetSaving" @click="onResetPasswordSubmit">确定</a-button>
      </template>
    </a-modal>
  </div>
</template>

<script>
import { clientApi, configApi, orgApi, userApi } from '../../api'

export default {
  name: 'UserManage',
  data() {
    return {
      loading: false,
      saving: false,
      togglingId: null,
      records: [],
      total: 0,
      // 组织架构树（筛选与表单的归属组织选择共用，进入页面加载一次）
      orgTree: [],
      orgTreeFields: {
        title: 'orgName',
        label: 'orgName',
        value: 'id',
        key: 'id',
        children: 'children'
      },
      query: { current: 1, size: 10, username: '', nickname: '', status: null, orgId: null },
      // 表格勾选（批量授权）
      selectedRowKeys: [],
      selectedRows: [],
      // 分页大小变化时抑制重复的 change 事件
      sizeChangeLock: false,
      dialogVisible: false,
      // 密码规则（规则配置 - 密码规则：默认密码与复杂度，用于表单提示）
      passwordPolicy: {},
      // 授权应用弹窗
      grantVisible: false,
      grantLoading: false,
      grantSaving: false,
      grantUser: {},
      grantApps: [],
      grantChecked: [],
      // 设备管理弹窗
      deviceVisible: false,
      deviceLoading: false,
      deviceUser: {},
      deviceList: [],
      // 批量授权
      batchGrantVisible: false,
      batchGrantLoading: false,
      batchGrantSaving: false,
      batchGrantMode: 'append',
      batchGrantChecked: [],
      // 重置密码弹窗
      resetVisible: false,
      resetSaving: false,
      resetUser: {},
      resetForm: { password: '' },
      resetRules: {
        password: [{ validator: this.validateResetPasswordRule, trigger: 'blur' }]
      },
      form: {
        id: null,
        username: '',
        password: '',
        nickname: '',
        email: '',
        phone: '',
        idCard: '',
        role: 'user',
        orgId: null,
        status: 1,
        remark: '',
        createTime: null
      },
      rules: {
        username: [
          { required: true, message: '请输入登录账号', trigger: 'blur' },
          { min: 2, max: 64, message: '账号长度 2 ~ 64 位', trigger: 'blur' }
        ],
        email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
        phone: [{ pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }],
        idCard: [{ pattern: /(^\d{15}$)|(^\d{17}(\d|X|x)$)/, message: '身份证号格式不正确', trigger: 'blur' }]
      },
      columns: [
        {
          title: '账号',
          dataIndex: 'username',
          width: 120,
          scopedSlots: { customRender: 'username' }
        },
        { title: '昵称', dataIndex: 'nickname', width: 120, ellipsis: true },
        {
          title: '所属组织',
          dataIndex: 'orgName',
          width: 160,
          ellipsis: true,
          scopedSlots: { customRender: 'orgName' }
        },
        { title: '角色', dataIndex: 'role', width: 90, scopedSlots: { customRender: 'role' } },
        { title: '状态', dataIndex: 'status', width: 130, scopedSlots: { customRender: 'status' } },
        { title: '邮箱', dataIndex: 'email', width: 140, ellipsis: true },
        {
          title: '身份证号',
          dataIndex: 'idCard',
          width: 180,
          ellipsis: true,
          scopedSlots: { customRender: 'idCard' }
        },
        {
          title: '最后登录',
          dataIndex: 'lastLoginTime',
          width: 160,
          scopedSlots: { customRender: 'lastLoginTime' }
        },
        {
          title: '授权应用 / 设备',
          dataIndex: 'authClientCount',
          width: 160,
          align: 'center',
          scopedSlots: { customRender: 'authClientCount' }
        },
        {
          title: '操作',
          key: 'action',
          width: 280,
          fixed: 'right',
          scopedSlots: { customRender: 'action' }
        }
      ],
      deviceColumns: [
        {
          title: '设备指纹',
          dataIndex: 'deviceId',
          width: 200,
          ellipsis: true,
          scopedSlots: { customRender: 'deviceId' }
        },
        {
          title: '环境',
          dataIndex: 'os',
          width: 150,
          scopedSlots: { customRender: 'deviceEnv' }
        },
        { title: '最近 IP', dataIndex: 'lastLoginIp', width: 130, ellipsis: true },
        {
          title: '最近登录',
          dataIndex: 'lastLoginTime',
          width: 170,
          scopedSlots: { customRender: 'deviceLastLoginTime' }
        },
        {
          title: '操作',
          key: 'deviceAction',
          width: 90,
          align: 'center',
          scopedSlots: { customRender: 'deviceAction' }
        }
      ]
    }
  },
  computed: {
    selectedUserNames() {
      return this.selectedRows.map((row) => row.nickname || row.username).join('、')
    },
    rowSelection() {
      return {
        selectedRowKeys: this.selectedRowKeys,
        fixed: 'left',
        columnWidth: 46,
        onChange: this.onSelectionChange
      }
    },
    // 状态开关的 1/0 约定在 a-switch 中用布尔代理保持数据形状
    statusEnabled: {
      get() {
        return this.form.status === 1
      },
      set(value) {
        this.form.status = value ? 1 : 0
      }
    },
    passwordRuleText() {
      return this.passwordPolicy.text || '密码长度不少于 6 位'
    }
  },
  async mounted() {
    await this.loadData()
    // 密码规则（默认密码 / 复杂度）用于新增与重置密码提示
    try {
      const res = await configApi.passwordRule()
      this.passwordPolicy = res.data || {}
    } catch (e) {
      // 忽略：提示信息获取失败不影响用户管理
    }
    // 组织架构树用于筛选与表单选择（失败不影响主流程）
    await this.loadOrgTree()
  },
  methods: {
    /** 加载组织架构树（筛选下拉 / 表单归属选择共用） */
    async loadOrgTree() {
      try {
        const res = await orgApi.tree()
        // 树形数据统一收在 data.list（data={list, count}）
        this.orgTree = res.data?.list || []
      } catch (e) {
        // 错误提示已由 axios 拦截器统一处理
      }
    },
    async loadData() {
      this.loading = true
      try {
        const res = await userApi.page({
          current: this.query.current,
          size: this.query.size,
          username: this.query.username || undefined,
          nickname: this.query.nickname || undefined,
          status: this.query.status === null || this.query.status === '' ? undefined : this.query.status,
          orgId: this.query.orgId || undefined
        })
        this.records = res.data?.records || []
        this.total = res.data?.total || 0
        // 与表格刷新数据后清空勾选的行为保持一致
        this.selectedRowKeys = []
        this.selectedRows = []
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
      this.query.username = ''
      this.query.nickname = ''
      this.query.status = null
      this.query.orgId = null
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
      this.form.username = ''
      this.form.password = ''
      this.form.nickname = ''
      this.form.email = ''
      this.form.phone = ''
      this.form.idCard = ''
      this.form.role = 'user'
      this.form.orgId = null
      this.form.status = 1
      this.form.remark = ''
      this.form.createTime = null
    },
    clearFormValidate() {
      const formRef = this.$refs.formRef
      if (formRef) {
        formRef.clearValidate()
      }
    },
    /** 身份证号脱敏（保留前六后四，中间以 * 占位）；空值显示占位符 */
    maskIdCard(idCard) {
      if (!idCard) return '-'
      if (idCard.length <= 10) return idCard
      return idCard.slice(0, 6) + '********'.slice(0, idCard.length - 10) + idCard.slice(-4)
    },
    openCreate() {
      this.resetFormFields()
      this.dialogVisible = true
      this.$nextTick(this.clearFormValidate)
    },
    openEdit(row) {
      this.resetFormFields()
      Object.assign(this.form, {
        id: row.id,
        username: row.username,
        nickname: row.nickname,
        email: row.email,
        phone: row.phone,
        idCard: row.idCard,
        role: row.role || 'user',
        orgId: row.orgId || null,
        status: row.status,
        remark: row.remark,
        createTime: row.createTime
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
        if (this.form.id) {
          await userApi.update({
            id: this.form.id,
            username: this.form.username,
            nickname: this.form.nickname,
            email: this.form.email,
            phone: this.form.phone,
            idCard: this.form.idCard,
            role: this.form.role,
            orgId: this.form.orgId || null,
            status: this.form.status,
            remark: this.form.remark
          })
          this.$message.success('保存成功')
        } else {
          await userApi.create({
            username: this.form.username,
            password: this.form.password || undefined,
            nickname: this.form.nickname,
            email: this.form.email,
            phone: this.form.phone,
            idCard: this.form.idCard,
            role: this.form.role,
            orgId: this.form.orgId || null,
            status: this.form.status,
            remark: this.form.remark
          })
          this.$message.success(
            this.form.password
              ? '新增成功'
              : `新增成功，初始密码为 ${this.passwordPolicy.defaultPassword || '123456'}`
          )
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
        content: `确定要${action}用户「${row.nickname || row.username}」吗？`,
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
        await userApi.changeStatus(row.id, target)
        this.$message.success(`已${action}`)
        this.loadData()
      } catch (e) {
        // 错误提示已由 axios 拦截器统一处理
      } finally {
        this.togglingId = null
      }
    },
    /* ------------------------- 重置密码 ------------------------- */
    openResetPassword(row) {
      this.resetUser = row
      this.resetForm.password = ''
      this.resetVisible = true
      this.$nextTick(() => {
        const formRef = this.$refs.resetFormRef
        if (formRef) {
          formRef.clearValidate()
        }
      })
    },
    validateResetPasswordRule(rule, value, callback) {
      const message = this.validateByPasswordRule(value)
      if (message === true) {
        callback()
        return
      }
      callback(new Error(message))
    },
    /** 按「规则配置 - 密码规则」做前端预校验（服务端仍会做权威校验） */
    validateByPasswordRule(value) {
      if (!value) {
        return '请输入新密码'
      }
      const rule = this.passwordPolicy
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
    },
    async onResetPasswordSubmit() {
      try {
        await this.$refs.resetFormRef.validate()
      } catch (e) {
        return
      }
      this.resetSaving = true
      try {
        await userApi.resetPassword(this.resetUser.id, this.resetForm.password)
        this.$message.success('密码已重置')
        this.resetVisible = false
      } catch (e) {
        // 错误提示已由 axios 拦截器统一处理
      } finally {
        this.resetSaving = false
      }
    },
    onDelete(row) {
      this.$confirm({
        title: '危险操作',
        content: `确定要删除用户「${row.nickname || row.username}」吗？删除后不可恢复，其应用授权也将一并清除。`,
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
        await userApi.remove(row.id)
        this.$message.success('删除成功')
        this.loadData()
      } catch (e) {
        // 错误提示已由 axios 拦截器统一处理
      }
    },
    /* ------------------------- 授权应用 / 批量授权 ------------------------- */
    /** 表格选中项变化 */
    onSelectionChange(selectedRowKeys, selectedRows) {
      this.selectedRowKeys = selectedRowKeys || []
      this.selectedRows = selectedRows || []
    },
    async openGrant(row) {
      this.grantUser = row
      this.grantChecked = []
      this.grantVisible = true
      this.grantLoading = true
      try {
        // 仅启用的应用可授权
        const [appsRes, checkedRes] = await Promise.all([clientApi.list(), userApi.listClients(row.id)])
        this.grantApps = (appsRes.data || []).filter((app) => app.status === 1)
        this.grantChecked = checkedRes.data || []
      } catch (e) {
        // 错误提示已由 axios 拦截器统一处理
      } finally {
        this.grantLoading = false
      }
    },
    async onSaveGrant() {
      this.grantSaving = true
      try {
        await userApi.bindClients(this.grantUser.id, this.grantChecked)
        this.$message.success('授权已保存')
        this.grantVisible = false
        this.loadData()
      } catch (e) {
        // 错误提示已由 axios 拦截器统一处理
      } finally {
        this.grantSaving = false
      }
    },
    /** 打开批量授权弹窗 */
    async openBatchGrant() {
      if (!this.selectedRows.length) {
        this.$message.warning('请先勾选需要授权的用户')
        return
      }
      this.batchGrantMode = 'append'
      this.batchGrantChecked = []
      this.batchGrantVisible = true
      this.batchGrantLoading = true
      try {
        const appsRes = await clientApi.list()
        this.grantApps = (appsRes.data || []).filter((app) => app.status === 1)
      } catch (e) {
        // 错误提示已由 axios 拦截器统一处理
      } finally {
        this.batchGrantLoading = false
      }
    },
    /** 保存批量授权（覆盖模式且未勾选时先确认清空） */
    onSaveBatchGrant() {
      const userIds = this.selectedRows.map((row) => row.id)
      if (this.batchGrantMode === 'cover' && this.batchGrantChecked.length === 0) {
        this.$confirm({
          title: '提示',
          content: `覆盖模式下未勾选任何应用，将清空这 ${userIds.length} 个用户的全部应用授权，确定继续吗？`,
          okText: '确定清空',
          cancelText: '取消',
          onOk: () => {
            this.doSaveBatchGrant()
          },
          onCancel: () => {}
        })
        return
      }
      this.doSaveBatchGrant()
    },
    async doSaveBatchGrant() {
      const userIds = this.selectedRows.map((row) => row.id)
      this.batchGrantSaving = true
      try {
        const res = await userApi.batchBindClients({
          userIds,
          clientIds: this.batchGrantChecked,
          mode: this.batchGrantMode
        })
        const action = this.batchGrantMode === 'append' ? '追加授权' : '覆盖授权'
        this.$message.success(
          `已为 ${res.data?.userCount ?? userIds.length} 个用户${action} ${this.batchGrantChecked.length} 个应用`
        )
        this.batchGrantVisible = false
        this.loadData()
      } catch (e) {
        // 错误提示已由 axios 拦截器统一处理
      } finally {
        this.batchGrantSaving = false
      }
    },
    /* ------------------------- 登录锁定 / 设备管理 ------------------------- */
    /** 是否处于登录锁定期 */
    isLocked(row) {
      return !!row.lockedUntil && new Date(row.lockedUntil).getTime() > Date.now()
    },
    /** 管理员在线解锁 */
    onUnlock(row) {
      this.$confirm({
        title: '在线解锁',
        content: `确定解锁账号「${row.nickname || row.username}」吗？解锁后将清除失败计数，账号可立即重新登录。`,
        okText: '确定解锁',
        cancelText: '取消',
        onOk: () => {
          this.doUnlock(row)
        },
        onCancel: () => {}
      })
    },
    async doUnlock(row) {
      try {
        await userApi.unlock(row.id)
        this.$message.success('已解锁')
        this.loadData()
      } catch (e) {
        // 错误提示已由 axios 拦截器统一处理
      }
    },
    /** 打开设备管理弹窗 */
    async openDevices(row) {
      this.deviceUser = row
      this.deviceList = []
      this.deviceVisible = true
      this.deviceLoading = true
      try {
        const res = await userApi.listDevices(row.id)
        this.deviceList = res.data || []
      } catch (e) {
        // 错误提示已由 axios 拦截器统一处理
      } finally {
        this.deviceLoading = false
      }
    },
    /** 解绑设备 */
    onUnbindDevice(device) {
      this.$confirm({
        title: '解绑设备',
        content: `确定解绑设备「${device.deviceId}」吗？解绑后该设备可重新作为新设备登录。`,
        okText: '确定解绑',
        cancelText: '取消',
        onOk: () => {
          this.doUnbindDevice(device)
        },
        onCancel: () => {}
      })
    },
    async doUnbindDevice(device) {
      try {
        await userApi.unbindDevice(this.deviceUser.id, device.deviceId)
        this.$message.success('设备已解绑')
        await this.openDevices(this.deviceUser)
        this.loadData()
      } catch (e) {
        // 错误提示已由 axios 拦截器统一处理
      }
    },
    formatTime(value) {
      if (!value) return '-'
      return String(value).replace('T', ' ').substring(0, 19)
    }
  }
}
</script>

<style lang="less" scoped>
@import '../../styles/tokens.less';

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

/* 批量授权：应用勾选列表 */
.batch-apps {
  width: 100%;
  max-height: 240px;
  overflow-y: auto;
}

.link-warning {
  color: @pd-color-warning;

  &:hover,
  &:focus {
    color: @pd-color-warning;
    opacity: 0.8;
  }
}

.reset-tip {
  margin: 0 0 6px;
  color: @pd-color-text;
  font-size: 14px;
}

.reset-rule {
  margin: 0 0 12px;
  font-size: 12px;
  line-height: 1.6;
}
</style>
