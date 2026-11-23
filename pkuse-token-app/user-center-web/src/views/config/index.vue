<template>
  <div class="pd-page">
    <!-- 说明 -->
    <a-alert type="info" :closable="false" class="tip-alert">
      <template slot="message">
        平台规则配置（仅系统管理员）：密码规则 / 登录规则 / 日志规则保存后<b>即时生效</b>，无需重启服务；
        配置存储于数据库表 <code>uc_config</code>，可一键恢复默认值。
      </template>
    </a-alert>

    <a-card :bordered="false" class="config-card">
      <a-spin :spinning="loading">
        <div class="config-toolbar">
          <a-button type="primary" :loading="saving" @click="onSave">
            <a-icon type="check" />保存配置
          </a-button>
          <a-button @click="onReset" :disabled="saving">恢复默认</a-button>
          <a-button @click="loadData" :disabled="saving">
            <a-icon type="reload" />刷新
          </a-button>
          <span v-if="dirtyCount > 0" class="dirty-tip">有 {{ dirtyCount }} 项修改待保存</span>
        </div>

        <div v-for="group in groupNames" :key="group" class="config-group">
          <div class="group-title">{{ group }}</div>
          <a-form-model
            :model="form"
            :label-col="{ span: 6 }"
            :wrapper-col="{ span: 18 }"
            label-align="left"
            class="config-form"
          >
            <a-form-model-item v-for="item in itemsOf(group)" :key="item.key" :label="item.name">
              <!-- 开关（表单值为字符串 'true' / 'false'） -->
              <a-switch
                v-if="item.type === 'bool'"
                :checked="form[item.key] === 'true'"
                @change="onBoolChange(item.key, $event)"
              />
              <!-- 整数 -->
              <a-input-number
                v-else-if="item.type === 'int'"
                v-model="numberValues[item.key]"
                :min="item.min != null ? item.min : 0"
                :max="item.max != null ? item.max : 999999"
                @change="onNumberChange(item.key, $event)"
              />
              <!-- 下拉选项 -->
              <a-select v-else-if="item.type === 'option'" v-model="form[item.key]" style="width: 320px">
                <a-select-option
                  v-for="option in item.options"
                  :key="option.value"
                  :value="option.value"
                >
                  {{ option.label }}
                </a-select-option>
              </a-select>
              <!-- 文本 -->
              <a-input v-else v-model.trim="form[item.key]" style="width: 320px" />

              <div class="item-remark">
                {{ item.remark }}
                <span v-if="item.type === 'bool'" class="text-muted">
                  （{{ form[item.key] === 'true' ? '已开启' : '已关闭' }}）
                </span>
              </div>
            </a-form-model-item>
          </a-form-model>
        </div>

        <a-divider />

        <div class="config-footer">
          <a-button type="primary" :loading="saving" @click="onSave">保存配置</a-button>
          <a-button @click="onReset" :disabled="saving">恢复默认</a-button>
          <div class="text-muted footer-tip">
            提示：密码规则影响「新增用户 / 重置密码 / 用户自助修改密码」；
            「首次登录强制修改密码」开启后，新增用户与被重置密码的用户下次登录需先改密；
            分区频率调整后，后端会立即按新频率续建分区（已有分区保持不变）。
          </div>
        </div>
      </a-spin>
    </a-card>
  </div>
</template>

<script>
import { configApi } from '../../api'

export default {
  name: 'ConfigPage',
  data() {
    return {
      loading: false,
      saving: false,
      groupNames: [],
      items: [],
      // 表单值（统一以字符串保存，与后端存储格式一致）
      form: {},
      // 整数型配置项的数字绑定（a-input-number 需要数字）
      numberValues: {},
      // 加载时的原始值（用于计算改动项）
      originValues: {}
    }
  },
  computed: {
    dirtyCount() {
      return Object.keys(this.changedValues()).length
    }
  },
  mounted() {
    this.loadData()
  },
  methods: {
    async loadData() {
      this.loading = true
      try {
        const res = await configApi.list()
        const groups = res.data || {}
        this.groupNames = Object.keys(groups)
        const all = []
        Object.keys(groups).forEach((group) => {
          ;(groups[group] || []).forEach((item) => all.push(item))
        })
        this.items = all
        all.forEach((item) => {
          // Vue 2：动态新增的属性必须用 $set 才能保持响应式
          this.$set(this.form, item.key, item.value)
          this.$set(this.numberValues, item.key, Number(item.value))
        })
        const snapshot = {}
        all.forEach((item) => {
          snapshot[item.key] = item.value
        })
        this.originValues = snapshot
      } catch (e) {
        // 错误提示已由 axios 拦截器统一处理
      } finally {
        this.loading = false
      }
    },

    itemsOf(group) {
      return this.items.filter((item) => item.group === group)
    },

    /** 与加载值比对，得到待保存的配置项 */
    changedValues() {
      const changed = {}
      Object.keys(this.form).forEach((key) => {
        if (String(this.form[key]) !== String(this.originValues[key])) {
          changed[key] = String(this.form[key])
        }
      })
      return changed
    },

    /** 开关：把布尔值代理回字符串 'true' / 'false' */
    onBoolChange(key, checked) {
      this.$set(this.form, key, checked ? 'true' : 'false')
    },

    /** 整数输入：同步字符串值到表单 */
    onNumberChange(key, value) {
      this.$set(this.form, key, String(value))
    },

    async onSave() {
      const changed = this.changedValues()
      if (Object.keys(changed).length === 0) {
        this.$message.info('配置没有变化')
        return
      }
      this.saving = true
      try {
        const res = await configApi.save(changed)
        this.$message.success(res.msg || '配置已保存并即时生效')
        await this.loadData()
      } catch (e) {
        // 校验失败等提示已由拦截器处理
      } finally {
        this.saving = false
      }
    },

    onReset() {
      this.$confirm({
        title: '提示',
        content: '确定将全部规则配置恢复为默认值吗？（立即生效）',
        okText: '确定恢复',
        cancelText: '取消',
        onOk: () => {
          this.doReset()
        },
        onCancel: () => {}
      })
    },

    async doReset() {
      this.saving = true
      try {
        const res = await configApi.reset()
        this.$message.success(res.msg || '已恢复默认配置')
        await this.loadData()
      } catch (e) {
        // 忽略：提示已由拦截器处理
      } finally {
        this.saving = false
      }
    }
  }
}
</script>

<style lang="less" scoped>
@import '../../styles/tokens.less';

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
  color: @pd-color-warning;
  font-size: 13px;
}

.config-group {
  margin-top: 8px;
}

.group-title {
  margin: 16px 0 8px;
  padding-left: 8px;
  border-left: 3px solid @pd-color-selected;
  font-size: 15px;
  font-weight: 600;
}

.config-form /deep/ .ant-form-item {
  margin-bottom: 18px;
}

.item-remark {
  width: 100%;
  margin-top: 2px;
  color: @pd-color-text-muted;
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
