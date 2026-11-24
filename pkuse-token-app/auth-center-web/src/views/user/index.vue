<template>
  <div class="pd-page">
    <div class="pd-section-title">
      用户授权
      <span class="text-muted section-tip">用户档案实时取自用户中心；此处仅管理「应用内角色」分配</span>
    </div>

    <div class="pd-search-card">
      <a-form-model layout="inline" :model="query" @submit.prevent>
        <a-form-model-item label="登录账号">
          <a-input
            v-model="query.username"
            placeholder="登录账号"
            allow-clear
            style="width: 140px"
            @keyup.enter="loadUsers(1)"
          />
        </a-form-model-item>
        <a-form-model-item label="昵称">
          <a-input
            v-model="query.nickname"
            placeholder="昵称"
            allow-clear
            style="width: 140px"
            @keyup.enter="loadUsers(1)"
          />
        </a-form-model-item>
        <a-form-model-item label="状态">
          <a-select v-model="query.status" placeholder="全部" allow-clear style="width: 100px">
            <a-select-option :value="1">启用</a-select-option>
            <a-select-option :value="0">禁用</a-select-option>
          </a-select>
        </a-form-model-item>
        <a-form-model-item>
          <a-button type="primary" @click="loadUsers(1)">
            <a-icon type="search" /><span>查询</span>
          </a-button>
        </a-form-model-item>
      </a-form-model>
    </div>

    <div class="pd-table-card">
      <a-table
        :columns="columns"
        :data-source="users"
        :loading="loading"
        row-key="id"
        :pagination="false"
        :scroll="{ x: 1000 }"
      >
        <template slot="username" slot-scope="text, record">
          <span class="text-mono">{{ record.username }}</span>
        </template>
        <template slot="role" slot-scope="text, record">
          <a-tag :class="record.role === 'admin' ? 'pd-tag-red' : 'pd-tag-gray'">
            {{ record.role === 'admin' ? '管理员' : '普通用户' }}
          </a-tag>
        </template>
        <template slot="status" slot-scope="text, record">
          <a-tag :class="record.status === 1 ? 'pd-tag-green' : 'pd-tag-gray'">
            {{ record.status === 1 ? '启用' : '禁用' }}
          </a-tag>
        </template>
        <template slot="roleSummary" slot-scope="text, record">
          <template v-if="record.roleSummary && record.roleSummary.length">
            <a-tag
              v-for="(item, index) in record.roleSummary"
              :key="index"
              class="pd-tag-blue role-tag"
            >{{ item }}</a-tag>
          </template>
          <span v-else class="text-muted">未分配</span>
        </template>
        <template slot="action" slot-scope="text, record">
          <a-button type="link" @click="openAssign(record)">分配角色</a-button>
        </template>
      </a-table>

      <a-pagination
        class="pd-pagination"
        :current="query.current"
        :page-size="query.size"
        :total="total"
        :show-total="showTotalText"
        show-size-changer
        :page-size-options="['10', '20', '50']"
        @change="onPageChange"
        @showSizeChange="onSizeChange"
      />
    </div>

    <!-- 分配角色弹窗：按应用分组 -->
    <a-modal
      :visible="assignVisible"
      :title="'分配角色：' + (currentUser.nickname || currentUser.username || '')"
      width="640px"
      :mask-closable="false"
      @cancel="assignVisible = false"
    >
      <a-spin :spinning="assignLoading">
        <div v-if="!assignLoading" class="assign-body">
          <a-alert
            v-if="userCenterDown"
            type="warning"
            :closable="false"
            show-icon
            class="assign-tip"
            message="用户中心不可用：无法校验应用入口授权状态（保存仍可用）"
          />
          <div v-for="group in groups" :key="group.clientId" class="assign-group">
            <div class="assign-group-title">
              <span class="group-name">{{ group.clientName }}</span>
              <span class="text-muted group-id">（{{ group.clientId }}）</span>
              <a-tag
                v-if="group.entryGranted === false"
                class="pd-tag-orange entry-tag"
              >未开通入口（在用户中心-用户管理-授权应用开通）</a-tag>
            </div>
            <a-checkbox-group v-if="group.roles.length" v-model="selectedRoleIds">
              <a-checkbox
                v-for="role in group.roles"
                :key="role.id"
                :value="role.id"
                class="assign-role"
              >{{ role.roleName }}（{{ role.roleCode }}）</a-checkbox>
            </a-checkbox-group>
            <div v-else class="text-muted empty-roles">该应用暂无启用角色，可先到「角色管理」创建</div>
          </div>
        </div>
        <div v-else class="assign-placeholder"></div>
      </a-spin>
      <template slot="footer">
        <a-button @click="assignVisible = false">取消</a-button>
        <a-button type="primary" :loading="saving" @click="saveAssign">保存（全量覆盖）</a-button>
      </template>
    </a-modal>
  </div>
</template>

<script>
import { userGrantApi } from '../../api'

export default {
  name: 'UserGrant',
  data() {
    return {
      users: [],
      total: 0,
      loading: false,
      sizeChangeLock: false,
      query: { current: 1, size: 10, username: '', nickname: '', status: undefined },
      assignVisible: false,
      assignLoading: false,
      saving: false,
      userCenterDown: false,
      currentUser: {},
      groups: [],
      selectedRoleIds: [],
      columns: [
        {
          title: '登录账号',
          dataIndex: 'username',
          key: 'username',
          width: 140,
          ellipsis: true,
          scopedSlots: { customRender: 'username' }
        },
        { title: '昵称', dataIndex: 'nickname', key: 'nickname', width: 140, ellipsis: true },
        {
          title: '平台角色',
          dataIndex: 'role',
          key: 'role',
          width: 110,
          scopedSlots: { customRender: 'role' }
        },
        {
          title: '状态',
          dataIndex: 'status',
          key: 'status',
          width: 90,
          scopedSlots: { customRender: 'status' }
        },
        {
          title: '已分配应用角色',
          dataIndex: 'roleSummary',
          key: 'roleSummary',
          width: 380,
          scopedSlots: { customRender: 'roleSummary' }
        },
        {
          title: '操作',
          key: 'action',
          width: 120,
          fixed: 'right',
          scopedSlots: { customRender: 'action' }
        }
      ]
    }
  },
  mounted() {
    this.loadUsers()
  },
  methods: {
    async loadUsers(page) {
      this.query.current = page || this.query.current
      this.loading = true
      try {
        const res = await userGrantApi.page({
          username: this.query.username || undefined,
          nickname: this.query.nickname || undefined,
          status:
            this.query.status === undefined || this.query.status === null || this.query.status === ''
              ? undefined
              : this.query.status,
          current: this.query.current,
          size: this.query.size
        })
        this.users = (res.data && res.data.records) || []
        this.total = (res.data && res.data.total) || 0
      } catch (e) {
        // 用户中心不可用：保持列表为空并提示（请求拦截器已弹出错误信息）
        this.users = []
        this.total = 0
      } finally {
        this.loading = false
      }
    },
    showTotalText(total) {
      return '共 ' + total + ' 条'
    },
    onPageChange(page, pageSize) {
      if (this.sizeChangeLock) {
        return
      }
      this.query.current = page
      if (pageSize && Number(pageSize) !== this.query.size) {
        this.query.size = Number(pageSize)
      }
      this.loadUsers(page)
    },
    onSizeChange(current, size) {
      this.sizeChangeLock = true
      this.query.size = Number(size)
      this.query.current = 1
      this.loadUsers(1)
      this.$nextTick(() => {
        this.sizeChangeLock = false
      })
    },
    async openAssign(row) {
      this.currentUser = row
      this.assignVisible = true
      this.assignLoading = true
      this.userCenterDown = false
      try {
        const res = await userGrantApi.roles(row.id)
        const data = res.data || {}
        this.groups = data.groups || []
        // 平铺的已绑定角色 ID（保存时全量覆盖）
        this.selectedRoleIds = this.groups.reduce(
          (acc, group) => acc.concat(group.boundRoleIds || []),
          []
        )
        // entryGranted 三态：false=未开通入口；null=用户中心不可用无法探测；true=已开通
        this.userCenterDown = this.groups.some((group) => group.entryGranted === null)
      } catch (e) {
        // 错误提示已由 axios 拦截器统一处理
      } finally {
        this.assignLoading = false
      }
    },
    async saveAssign() {
      this.saving = true
      try {
        const res = await userGrantApi.bindRoles(
          this.currentUser.id,
          this.selectedRoleIds
        )
        this.$message.success(res.msg || '保存成功')
        this.assignVisible = false
        await this.loadUsers()
      } catch (e) {
        // 错误提示已由 axios 拦截器统一处理
      } finally {
        this.saving = false
      }
    }
  }
}
</script>

<style lang="less" scoped>
@import '../../styles/tokens.less';

.section-tip {
  margin-left: @pd-space-sm;
  font-size: 12px;
  font-weight: 400;
}

.role-tag {
  margin: 2px 6px 2px 0;
}

.assign-tip {
  margin-bottom: @pd-space-md;
}

.assign-body {
  min-height: 120px;
}

.assign-placeholder {
  min-height: 120px;
}

.assign-group {
  padding: 10px 0;
  border-bottom: 1px dashed @pd-color-border-light;

  &:last-child {
    border-bottom: 0;
  }
}

.assign-group-title {
  margin-bottom: 6px;
}

.group-name {
  font-weight: 600;
  color: @pd-color-text;
}

.group-id {
  font-size: 12px;
}

.entry-tag {
  margin-left: @pd-space-sm;
}

.assign-role {
  margin-right: @pd-space-lg;
}

.empty-roles {
  padding: 4px 0;
  font-size: 12px;
}
</style>
