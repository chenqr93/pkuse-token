<template>
  <div class="pd-page">
    <div class="pd-table-card">
      <div class="pd-toolbar">
        <div class="pd-toolbar-left">
          <span class="card-title">菜单管理</span>
          <a-select
            v-model="clientId"
            placeholder="选择应用"
            style="width: 240px"
            @change="loadTree"
          >
            <a-select-option
              v-for="app in apps"
              :key="app.clientId"
              :value="app.clientId"
              :disabled="app.status !== 1"
            >{{ app.clientName }}（{{ app.clientId }}）</a-select-option>
          </a-select>
          <span class="text-muted toolbar-tip">类型：目录 / 页面 / 按钮；按钮必填权限码</span>
        </div>
        <div class="pd-toolbar-right">
          <a-button type="primary" :disabled="!clientId" @click="openCreate('0')">
            <a-icon type="plus" /><span>新增根菜单</span>
          </a-button>
        </div>
      </div>

      <a-table
        :columns="columns"
        :data-source="tree"
        :loading="loading"
        row-key="id"
        :pagination="false"
        :expanded-row-keys="expandedRowKeys"
        :scroll="{ x: 1300 }"
        @expandedRowsChange="onExpandedRowsChange"
      >
        <template slot="menuName" slot-scope="text, record">
          <span class="menu-name">
            <a-icon :type="menuIcon(record.icon)" class="menu-icon" />
            <span class="menu-name-text">{{ record.menuName }}</span>
          </span>
        </template>
        <template slot="menuType" slot-scope="text, record">
          <a-tag :class="typeTag(record.menuType).cls">{{ typeTag(record.menuType).text }}</a-tag>
        </template>
        <template slot="path" slot-scope="text, record">
          <span v-if="record.path" class="text-mono text-muted">{{ record.path }}</span>
          <span v-else class="text-muted">-</span>
        </template>
        <template slot="component" slot-scope="text, record">
          <span v-if="record.component" class="text-mono text-muted">{{ record.component }}</span>
          <span v-else class="text-muted">-</span>
        </template>
        <template slot="perms" slot-scope="text, record">
          <span v-if="record.perms" class="text-mono text-muted">{{ record.perms }}</span>
          <span v-else class="text-muted">-</span>
        </template>
        <template slot="status" slot-scope="text, record">
          <a-tag :class="record.status === 0 ? 'pd-tag-green' : 'pd-tag-gray'">
            {{ record.status === 0 ? '启用' : '禁用' }}
          </a-tag>
        </template>
        <template slot="visible" slot-scope="text, record">
          <a-tag
            v-if="record.menuType !== 2"
            :class="record.visible === 0 ? 'pd-tag-green' : 'pd-tag-orange'"
          >{{ record.visible === 0 ? '显示' : '隐藏' }}</a-tag>
          <span v-else class="text-muted">-</span>
        </template>
        <template slot="action" slot-scope="text, record">
          <div class="table-actions">
            <a-button
              v-if="record.menuType !== 2"
              type="link"
              :disabled="record.status !== 0"
              @click="openCreate(record.id)"
            >新增子级</a-button>
            <a-button type="link" @click="openEdit(record)">编辑</a-button>
            <a-button type="link" @click="toggleStatus(record)">
              {{ record.status === 0 ? '禁用' : '启用' }}
            </a-button>
            <a-popconfirm
              title="确定删除该菜单？（被角色绑定时将级联清理绑定）"
              ok-text="确定"
              cancel-text="取消"
              @confirm="remove(record)"
            >
              <a-button type="link" class="pd-link-danger">删除</a-button>
            </a-popconfirm>
          </div>
        </template>
      </a-table>
    </div>

    <!-- 新增/编辑弹窗 -->
    <a-modal
      :visible="dialogVisible"
      :title="form.id ? '编辑菜单' : '新增菜单'"
      width="560px"
      :mask-closable="false"
      :destroy-on-close="true"
      @cancel="dialogVisible = false"
    >
      <a-form-model
        :model="form"
        :label-col="{ span: 5 }"
        :wrapper-col="{ span: 19 }"
      >
        <a-form-model-item label="菜单类型" required>
          <a-radio-group v-model="form.menuType" :disabled="!!form.id">
            <a-radio-button :value="0">目录</a-radio-button>
            <a-radio-button :value="1">页面</a-radio-button>
            <a-radio-button :value="2">按钮</a-radio-button>
          </a-radio-group>
        </a-form-model-item>
        <a-form-model-item label="菜单名称" required>
          <a-input v-model="form.menuName" :max-length="50" placeholder="应用内唯一" />
        </a-form-model-item>
        <a-form-model-item label="上级菜单">
          <a-input :value="parentLabel" disabled />
        </a-form-model-item>
        <template v-if="form.menuType !== 2">
          <a-form-model-item label="路由地址" :required="form.menuType === 1">
            <a-input v-model="form.path" placeholder="如 /sys/user" />
          </a-form-model-item>
          <a-form-model-item v-if="form.menuType === 1" label="组件路径">
            <a-input v-model="form.component" placeholder="如 sys/user/index" />
          </a-form-model-item>
        </template>
        <a-form-model-item label="权限码" :required="form.menuType === 2">
          <a-input
            v-model="form.perms"
            :disabled="form.menuType === 0"
            placeholder="如 sys:user:add（按钮必填，应用内唯一）"
          />
        </a-form-model-item>
        <a-form-model-item v-if="form.menuType !== 2" label="图标">
          <a-input v-model="form.icon" placeholder="Ant Design Vue 图标名，如 setting">
            <a-icon slot="prefix" :type="menuIcon(form.icon)" />
          </a-input>
        </a-form-model-item>
        <a-form-model-item label="排序">
          <a-input-number v-model="form.sort" :min="0" :max="9999" style="width: 160px" />
        </a-form-model-item>
        <a-form-model-item v-if="form.menuType !== 2" label="是否显示">
          <a-switch
            :checked="form.visible === 0"
            checked-children="显示"
            un-checked-children="隐藏"
            @change="onVisibleChange"
          />
        </a-form-model-item>
        <a-form-model-item label="备注">
          <a-input v-model="form.remark" :max-length="200" />
        </a-form-model-item>
      </a-form-model>
      <template slot="footer">
        <a-button @click="dialogVisible = false">取消</a-button>
        <a-button type="primary" :loading="saving" @click="save">保存</a-button>
      </template>
    </a-modal>
  </div>
</template>

<script>
import { appApi, menuApi } from '../../api'

/** 收集树节点 id（异步数据下受控 expandedRowKeys 默认展开全部） */
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

export default {
  name: 'MenuManage',
  data() {
    return {
      apps: [],
      clientId: '',
      tree: [],
      expandedRowKeys: [],
      loading: false,
      saving: false,
      dialogVisible: false,
      form: {
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
      },
      columns: [
        {
          title: '菜单名称',
          dataIndex: 'menuName',
          key: 'menuName',
          width: 230,
          ellipsis: true,
          scopedSlots: { customRender: 'menuName' }
        },
        {
          title: '类型',
          dataIndex: 'menuType',
          key: 'menuType',
          width: 80,
          scopedSlots: { customRender: 'menuType' }
        },
        {
          title: '路由地址',
          dataIndex: 'path',
          key: 'path',
          width: 160,
          ellipsis: true,
          scopedSlots: { customRender: 'path' }
        },
        {
          title: '组件路径',
          dataIndex: 'component',
          key: 'component',
          width: 170,
          ellipsis: true,
          scopedSlots: { customRender: 'component' }
        },
        {
          title: '权限码',
          dataIndex: 'perms',
          key: 'perms',
          width: 150,
          ellipsis: true,
          scopedSlots: { customRender: 'perms' }
        },
        { title: '排序', dataIndex: 'sort', key: 'sort', width: 70, align: 'center' },
        {
          title: '状态',
          dataIndex: 'status',
          key: 'status',
          width: 80,
          scopedSlots: { customRender: 'status' }
        },
        {
          title: '显示',
          dataIndex: 'visible',
          key: 'visible',
          width: 80,
          scopedSlots: { customRender: 'visible' }
        },
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
  computed: {
    /** 上级菜单显示名（根为“根节点”） */
    parentLabel() {
      if (!this.form.parentId || this.form.parentId === '0') {
        return '根节点'
      }
      const node = this.findNode(this.tree, this.form.parentId)
      return node ? node.menuName : this.form.parentId
    }
  },
  mounted() {
    this.loadApps()
  },
  methods: {
    findNode(nodes, id) {
      for (const node of nodes) {
        if (node.id === id) {
          return node
        }
        const child = this.findNode(node.children || [], id)
        if (child) {
          return child
        }
      }
      return null
    },
    typeTag(type) {
      if (type === 0) {
        return { text: '目录', cls: 'pd-tag-orange' }
      }
      if (type === 1) {
        return { text: '页面', cls: 'pd-tag-blue' }
      }
      return { text: '按钮', cls: 'pd-tag-gray' }
    },
    /**
     * 菜单图标：历史数据可能保存 Element Plus 图标名（PascalCase）。
     * 不做名称映射，仅透传 AntDV 的 kebab-case 图标名，无法识别时回退默认图标 appstore。
     */
    menuIcon(icon) {
      if (!icon || icon.indexOf('el-icon') === 0) {
        return 'appstore'
      }
      return /^[a-z][a-z0-9-]*$/.test(icon) ? icon : 'appstore'
    },
    async loadApps() {
      try {
        const res = await appApi.list()
        // 统一信封：应用列表在 data.apps 内
        this.apps = ((res.data && res.data.apps) || []).filter((app) => app.status === 1)
        if (!this.clientId && this.apps.length) {
          const fromQuery = this.$route.query.clientId
          this.clientId =
            fromQuery && this.apps.some((a) => a.clientId === fromQuery)
              ? fromQuery
              : this.apps[0].clientId
          await this.loadTree()
        }
      } catch (e) {
        // 错误提示已由 axios 拦截器统一处理
      }
    },
    async loadTree() {
      if (!this.clientId) {
        this.tree = []
        this.expandedRowKeys = []
        return
      }
      this.loading = true
      try {
        const res = await menuApi.tree(this.clientId)
        // 树形数据统一收在 data.list（data={list, count}）
        this.tree = (res.data && res.data.list) || []
        this.expandedRowKeys = collectNodeIds(this.tree)
      } catch (e) {
        // 错误提示已由 axios 拦截器统一处理
      } finally {
        this.loading = false
      }
    },
    onExpandedRowsChange(keys) {
      this.expandedRowKeys = keys || []
    },
    openCreate(parentId) {
      Object.assign(this.form, {
        id: '',
        clientId: this.clientId,
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
      this.dialogVisible = true
    },
    openEdit(row) {
      Object.assign(this.form, {
        id: row.id,
        clientId: this.clientId,
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
      this.dialogVisible = true
    },
    onVisibleChange(checked) {
      this.form.visible = checked ? 0 : 1
    },
    async save() {
      this.saving = true
      try {
        const payload = Object.assign({}, this.form, { clientId: this.clientId })
        const res = this.form.id ? await menuApi.update(payload) : await menuApi.create(payload)
        this.$message.success(res.msg || '保存成功')
        this.dialogVisible = false
        await this.loadTree()
      } catch (e) {
        // 错误提示已由 axios 拦截器统一处理
      } finally {
        this.saving = false
      }
    },
    async toggleStatus(row) {
      const target = row.status === 0 ? 1 : 0
      try {
        const res = await menuApi.changeStatus(row.id, target)
        this.$message.success(res.msg || '操作成功')
        await this.loadTree()
      } catch (e) {
        // 错误提示已由 axios 拦截器统一处理
      }
    },
    async remove(row) {
      try {
        const res = await menuApi.remove(row.id)
        this.$message.success(res.msg || '删除成功')
        await this.loadTree()
      } catch (e) {
        // 错误提示已由 axios 拦截器统一处理
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

.toolbar-tip {
  font-size: 12px;
}

.menu-name {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  max-width: 100%;
}

.menu-icon {
  color: @pd-color-text-muted;
}

.menu-name-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.table-actions {
  display: flex;
  align-items: center;
  gap: @pd-space-md;
}
</style>
