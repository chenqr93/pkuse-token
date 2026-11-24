<template>
  <div class="pd-page">
    <div class="pd-table-card">
      <div class="pd-toolbar">
        <div class="pd-toolbar-left">
          <span class="card-title">角色管理</span>
          <a-select
            v-model="clientId"
            placeholder="选择应用"
            style="width: 240px"
            @change="onClientChange"
          >
            <a-select-option
              v-for="app in apps"
              :key="app.clientId"
              :value="app.clientId"
              :disabled="app.status !== 1"
            >{{ app.clientName }}（{{ app.clientId }}）</a-select-option>
          </a-select>
          <a-input
            v-model="query.roleName"
            placeholder="角色名称/编码"
            allow-clear
            style="width: 180px"
            @keyup.enter="loadRoles(1)"
          />
          <a-button @click="loadRoles(1)">
            <a-icon type="search" /><span>查询</span>
          </a-button>
        </div>
        <div class="pd-toolbar-right">
          <a-button type="primary" :disabled="!clientId" @click="openCreate">
            <a-icon type="plus" /><span>新增角色</span>
          </a-button>
        </div>
      </div>

      <a-table
        :columns="columns"
        :data-source="roles"
        :loading="loading"
        row-key="id"
        :pagination="false"
        :scroll="{ x: 1090 }"
      >
        <template slot="roleCode" slot-scope="text, record">
          <span class="text-mono">{{ record.roleCode }}</span>
        </template>
        <template slot="status" slot-scope="text, record">
          <a-tag :class="record.status === 0 ? 'pd-tag-green' : 'pd-tag-gray'">
            {{ record.status === 0 ? '启用' : '禁用' }}
          </a-tag>
        </template>
        <template slot="action" slot-scope="text, record">
          <div class="table-actions">
            <a-button type="link" @click="openMenus(record)">菜单权限</a-button>
            <a-button type="link" @click="openEdit(record)">编辑</a-button>
            <a-button type="link" @click="toggleStatus(record)">
              {{ record.status === 0 ? '禁用' : '启用' }}
            </a-button>
            <a-popconfirm
              title="确定删除该角色？（将级联清理角色-菜单与用户-角色绑定）"
              ok-text="确定"
              cancel-text="取消"
              @confirm="remove(record)"
            >
              <a-button type="link" class="pd-link-danger">删除</a-button>
            </a-popconfirm>
          </div>
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

    <!-- 新增/编辑角色 -->
    <a-modal
      :visible="dialogVisible"
      :title="form.id ? '编辑角色' : '新增角色'"
      width="480px"
      :mask-closable="false"
      :destroy-on-close="true"
      @cancel="dialogVisible = false"
    >
      <a-form-model :model="form" :label-col="{ span: 5 }" :wrapper-col="{ span: 19 }">
        <a-form-model-item label="角色编码" required>
          <a-input
            v-model="form.roleCode"
            :disabled="!!form.id"
            :max-length="50"
            placeholder="字母数字下划线，应用内唯一"
          />
        </a-form-model-item>
        <a-form-model-item label="角色名称" required>
          <a-input v-model="form.roleName" :max-length="50" />
        </a-form-model-item>
        <a-form-model-item label="备注">
          <a-textarea v-model="form.remark" :rows="2" :max-length="200" />
        </a-form-model-item>
      </a-form-model>
      <template slot="footer">
        <a-button @click="dialogVisible = false">取消</a-button>
        <a-button type="primary" :loading="saving" @click="save">保存</a-button>
      </template>
    </a-modal>

    <!-- 菜单权限勾选 -->
    <a-modal
      :visible="menusVisible"
      :title="'菜单权限：' + (currentRole.roleName || '')"
      width="520px"
      :mask-closable="false"
      :destroy-on-close="true"
      @cancel="menusVisible = false"
    >
      <a-alert
        type="info"
        :closable="false"
        show-icon
        class="menus-tip"
        message="父子节点不联动，请按需勾选目录/页面/按钮；勾选页面时其所需父目录建议一并勾选"
      />
      <a-tree
        checkable
        check-strictly
        default-expand-all
        class="menu-tree"
        :tree-data="menuTree"
        :checked-keys="checkedKeys"
        :replace-fields="treeFields"
        @check="onMenuCheck"
      />
      <template slot="footer">
        <a-button @click="menusVisible = false">取消</a-button>
        <a-button type="primary" :loading="saving" @click="saveMenus">保存</a-button>
      </template>
    </a-modal>
  </div>
</template>

<script>
import { appApi, menuApi, roleApi } from '../../api'

export default {
  name: 'RoleManage',
  data() {
    return {
      apps: [],
      clientId: '',
      roles: [],
      total: 0,
      loading: false,
      saving: false,
      dialogVisible: false,
      menusVisible: false,
      menuTree: [],
      checkedKeys: [],
      treeFields: { title: 'menuName', key: 'id', children: 'children' },
      currentRole: {},
      sizeChangeLock: false,
      query: { current: 1, size: 10, roleName: '' },
      form: { id: '', clientId: '', roleCode: '', roleName: '', remark: '' },
      columns: [
        {
          title: '角色编码',
          dataIndex: 'roleCode',
          key: 'roleCode',
          width: 140,
          ellipsis: true,
          scopedSlots: { customRender: 'roleCode' }
        },
        { title: '角色名称', dataIndex: 'roleName', key: 'roleName', width: 160, ellipsis: true },
        {
          title: '状态',
          dataIndex: 'status',
          key: 'status',
          width: 90,
          scopedSlots: { customRender: 'status' }
        },
        { title: '已绑菜单', dataIndex: 'menuCount', key: 'menuCount', width: 100, align: 'center' },
        { title: '授权用户', dataIndex: 'userCount', key: 'userCount', width: 100, align: 'center' },
        { title: '备注', dataIndex: 'remark', key: 'remark', width: 220, ellipsis: true },
        {
          title: '操作',
          key: 'action',
          width: 280,
          fixed: 'right',
          scopedSlots: { customRender: 'action' }
        }
      ]
    }
  },
  mounted() {
    this.loadApps()
  },
  methods: {
    async loadApps() {
      try {
        const res = await appApi.list()
        // 统一信封：应用列表在 data.apps 内
        this.apps = ((res.data && res.data.apps) || []).filter((app) => app.status === 1)
        const fromQuery = this.$route.query.clientId
        if (this.apps.length) {
          this.clientId =
            fromQuery && this.apps.some((a) => a.clientId === fromQuery)
              ? fromQuery
              : this.apps[0].clientId
          await this.loadRoles(1)
        }
      } catch (e) {
        // 错误提示已由 axios 拦截器统一处理
      }
    },
    async loadRoles(page) {
      if (!this.clientId) {
        return
      }
      this.query.current = page || this.query.current
      this.loading = true
      try {
        const res = await roleApi.page({
          clientId: this.clientId,
          roleName: this.query.roleName || undefined,
          current: this.query.current,
          size: this.query.size
        })
        this.roles = (res.data && res.data.records) || []
        this.total = (res.data && res.data.total) || 0
      } catch (e) {
        // 错误提示已由 axios 拦截器统一处理
      } finally {
        this.loading = false
      }
    },
    onClientChange() {
      this.query.roleName = ''
      this.loadRoles(1)
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
      this.loadRoles(page)
    },
    onSizeChange(current, size) {
      this.sizeChangeLock = true
      this.query.size = Number(size)
      this.query.current = 1
      this.loadRoles(1)
      this.$nextTick(() => {
        this.sizeChangeLock = false
      })
    },
    openCreate() {
      Object.assign(this.form, {
        id: '',
        clientId: this.clientId,
        roleCode: '',
        roleName: '',
        remark: ''
      })
      this.dialogVisible = true
    },
    openEdit(row) {
      Object.assign(this.form, {
        id: row.id,
        clientId: row.clientId,
        roleCode: row.roleCode,
        roleName: row.roleName,
        remark: row.remark || ''
      })
      this.dialogVisible = true
    },
    async save() {
      this.saving = true
      try {
        const payload = Object.assign({}, this.form, { clientId: this.clientId })
        const res = this.form.id ? await roleApi.update(payload) : await roleApi.create(payload)
        this.$message.success(res.msg || '保存成功')
        this.dialogVisible = false
        await this.loadRoles()
      } catch (e) {
        // 错误提示已由 axios 拦截器统一处理
      } finally {
        this.saving = false
      }
    },
    async toggleStatus(row) {
      const target = row.status === 0 ? 1 : 0
      try {
        const res = await roleApi.changeStatus(row.id, target)
        this.$message.success(res.msg || '操作成功')
        await this.loadRoles()
      } catch (e) {
        // 错误提示已由 axios 拦截器统一处理
      }
    },
    async remove(row) {
      try {
        const res = await roleApi.remove(row.id)
        this.$message.success(res.msg || '删除成功')
        await this.loadRoles()
      } catch (e) {
        // 错误提示已由 axios 拦截器统一处理
      }
    },
    async openMenus(row) {
      this.currentRole = row
      this.checkedKeys = []
      try {
        const [treeRes, idsRes] = await Promise.all([
          menuApi.tree(this.clientId),
          roleApi.menuIds(row.id)
        ])
        // 树形数据统一收在 data.list（data={list, count}）
        this.menuTree = (treeRes.data && treeRes.data.list) || []
        this.checkedKeys = (idsRes.data || []).slice()
        this.menusVisible = true
      } catch (e) {
        // 错误提示已由 axios 拦截器统一处理
      }
    },
    /**
     * check-strictly 下 AntDV a-tree 回传 { checked, halfChecked }（非数组），
     * 这里统一归一化为受控 checkedKeys 数组，保存时仍为全量覆盖。
     */
    onMenuCheck(checkedKeys) {
      if (checkedKeys && !Array.isArray(checkedKeys)) {
        this.checkedKeys = checkedKeys.checked || []
      } else {
        this.checkedKeys = checkedKeys || []
      }
    },
    async saveMenus() {
      this.saving = true
      try {
        const menuIds = this.checkedKeys.slice()
        const res = await roleApi.bindMenus(this.currentRole.id, menuIds)
        this.$message.success(res.msg || '保存成功')
        this.menusVisible = false
        await this.loadRoles()
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

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: @pd-color-text;
  line-height: 32px;
}

.table-actions {
  display: flex;
  align-items: center;
  gap: @pd-space-md;
}

.menus-tip {
  margin-bottom: @pd-space-md;
}

.menu-tree {
  max-height: 420px;
  overflow: auto;
  padding: @pd-space-xs 0;
}
</style>
