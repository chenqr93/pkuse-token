<template>
  <div class="pd-page">
    <!-- 强制改密提示（规则配置 - 首次登录强制修改密码） -->
    <a-alert v-if="forceMode" type="warning" :closable="false" class="tip-alert">
      <template slot="message">
        检测到当前账号使用<b>初始密码</b>：为保障账号安全，请先修改密码，修改成功后才能使用系统其他功能。
      </template>
    </a-alert>

    <a-card :bordered="false" class="password-card">
      <template slot="title">
        <div class="card-title">
          <span>修改密码</span>
          <span class="text-muted account-tip">当前账号：{{ username || '-' }}</span>
        </div>
      </template>

      <a-alert type="info" :closable="false" class="rule-tip" :message="'密码规则：' + (rule.text || '加载中…')" />

      <a-form-model
        ref="formRef"
        :model="form"
        :rules="rules"
        :label-col="{ span: 5 }"
        :wrapper-col="{ span: 19 }"
        label-align="left"
        class="password-form"
        @submit.native.prevent
      >
        <a-form-model-item label="原密码" prop="oldPassword">
          <a-input-password v-model="form.oldPassword" placeholder="请输入原密码" maxlength="64" />
        </a-form-model-item>
        <a-form-model-item label="新密码" prop="newPassword">
          <a-input-password v-model="form.newPassword" placeholder="请输入新密码" maxlength="64" />
        </a-form-model-item>
        <a-form-model-item label="确认新密码" prop="confirmPassword">
          <a-input-password
            v-model="form.confirmPassword"
            placeholder="请再次输入新密码"
            maxlength="64"
            @keyup.enter="onSubmit"
          />
        </a-form-model-item>
        <a-form-model-item :wrapper-col="{ span: 19, offset: 5 }">
          <a-button type="primary" :loading="saving" @click="onSubmit" :disabled="!form.oldPassword || !form.newPassword">
            确认修改
          </a-button>
          <a-button v-if="!forceMode" @click="onBack">返回</a-button>
        </a-form-model-item>
      </a-form-model>

      <div class="tips text-muted">
        <div>1、密码使用国密 SM2 加密后传输，服务端以 SM3 + 随机盐存储；</div>
        <div>2、修改成功后本账号的登录会话保持有效，无需重新登录；</div>
        <div>3、如忘记原密码，可联系系统管理员在「用户管理」中重置密码。</div>
      </div>
    </a-card>
  </div>
</template>

<script>
import { sm2 } from 'sm-crypto'
import { authApi } from '../../api'
import { getStoredUser, setStoredUser } from '../../utils/auth'

export default {
  name: 'PasswordPage',
  data() {
    return {
      saving: false,
      rule: { text: '' },
      publicKey: '',
      user: getStoredUser() || {},
      form: { oldPassword: '', newPassword: '', confirmPassword: '' }
    }
  },
  computed: {
    /** 强制改密模式（由统一登录页在检测到初始密码时带 ?force=1 进入） */
    forceMode() {
      return this.$route.query.force === '1'
    },
    username() {
      return this.user.username || ''
    },
    /** 前端预校验（服务端仍会按规则配置做权威校验） */
    rules() {
      return {
        oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
        newPassword: [
          { required: true, message: '请输入新密码', trigger: 'blur' },
          { validator: this.validateNewPassword, trigger: 'blur' }
        ],
        confirmPassword: [
          { required: true, message: '请再次输入新密码', trigger: 'blur' },
          { validator: this.validateConfirm, trigger: 'blur' }
        ]
      }
    }
  },
  mounted() {
    this.init()
  },
  methods: {
    async init() {
      if (!this.user.username) {
        // 兜底：刷新页面后从服务端取当前用户
        try {
          const res = await authApi.me()
          this.user = res.data || {}
          setStoredUser(this.user)
        } catch (e) {
          // 401 已由拦截器处理
        }
      }
      try {
        const res = await authApi.passwordRule()
        this.rule = res.data || {}
      } catch (e) {
        // 忽略：规则提示获取失败不阻塞改密
      }
      try {
        const res = await authApi.publicKey()
        this.publicKey = (res.data && res.data.publicKey) || ''
      } catch (e) {
        this.publicKey = ''
      }
    },

    validateNewPassword(ruleItem, value, callback) {
      if (!value) {
        callback()
        return
      }
      if (this.form.oldPassword && value === this.form.oldPassword) {
        callback(new Error('新密码不能与原密码相同'))
        return
      }
      const current = this.rule
      if (current.minLength && value.length < current.minLength) {
        callback(new Error(`密码长度不能小于 ${current.minLength} 位`))
        return
      }
      if (current.complexityEnabled) {
        if (current.requireUppercase && !/[A-Z]/.test(value)) {
          callback(new Error('密码必须包含大写字母'))
          return
        }
        if (current.requireLowercase && !/[a-z]/.test(value)) {
          callback(new Error('密码必须包含小写字母'))
          return
        }
        if (current.requireDigit && !/[0-9]/.test(value)) {
          callback(new Error('密码必须包含数字'))
          return
        }
        if (current.requireSpecial && !/[^A-Za-z0-9]/.test(value)) {
          callback(new Error('密码必须包含特殊字符'))
          return
        }
      }
      callback()
    },

    validateConfirm(ruleItem, value, callback) {
      if (value !== this.form.newPassword) {
        callback(new Error('两次输入的新密码不一致'))
        return
      }
      callback()
    },

    encrypt(plain) {
      if (!this.publicKey) {
        return plain
      }
      try {
        return sm2.doEncrypt(plain, this.publicKey, 1)
      } catch (e) {
        return plain
      }
    },

    onSubmit() {
      if (!this.$refs.formRef) {
        return
      }
      this.$refs.formRef.validate((valid) => {
        if (!valid) {
          return
        }
        this.doSubmit()
      })
    },

    async doSubmit() {
      this.saving = true
      try {
        const res = await authApi.changePassword({
          oldPassword: this.encrypt(this.form.oldPassword),
          newPassword: this.encrypt(this.form.newPassword),
          encrypted: !!this.publicKey
        })
        this.$message.success(res.msg || '密码修改成功')
        this.form.oldPassword = ''
        this.form.newPassword = ''
        this.form.confirmPassword = ''
        // 刷新本地用户信息（清除「需修改密码」标记）
        try {
          const me = await authApi.me()
          this.user = me.data || {}
          setStoredUser(this.user)
        } catch (e) {
          // 忽略：不影响改密结果
        }
        // 强制改密完成后：继续登录时暂存的跳转目标（如 SSO 回跳地址），否则回工作台
        const pending = sessionStorage.getItem('post-change-password-redirect')
        sessionStorage.removeItem('post-change-password-redirect')
        if (pending) {
          if (pending.indexOf('http') === 0) {
            window.location.href = pending
          } else {
            this.$router.replace(pending)
          }
          return
        }
        if (this.forceMode) {
          this.$router.replace('/workbench')
        }
      } catch (e) {
        // 错误提示已由 axios 拦截器统一处理
      } finally {
        this.saving = false
      }
    },

    onBack() {
      this.$router.back()
    }
  }
}
</script>

<style lang="less" scoped>
@import '../../styles/tokens.less';

.tip-alert {
  margin-bottom: 16px;
}

.password-card {
  max-width: 720px;
}

.card-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
}

.account-tip {
  font-size: 13px;
  font-weight: 400;
}

.rule-tip {
  margin-bottom: 16px;
}

.password-form {
  margin-top: 4px;
}

.tips {
  margin-top: 8px;
  font-size: 12px;
  line-height: 1.9;
}
</style>
