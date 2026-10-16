<template>
  <div class="page-container">
    <!-- 强制改密提示（规则配置 - 首次登录强制修改密码） -->
    <el-alert v-if="forceMode" type="warning" :closable="false" class="tip-alert">
      <template #title>
        检测到当前账号使用<b>初始密码</b>：为保障账号安全，请先修改密码，修改成功后才能使用系统其他功能。
      </template>
    </el-alert>

    <el-card shadow="never" class="password-card">
      <template #header>
        <div class="card-title">
          <span>修改密码</span>
          <span class="text-muted account-tip">当前账号：{{ username || '-' }}</span>
        </div>
      </template>

      <el-alert type="info" :closable="false" class="rule-tip">
        <template #title>密码规则：{{ rule.text || '加载中…' }}</template>
      </el-alert>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px" class="password-form" @submit.prevent>
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="form.oldPassword" type="password" show-password placeholder="请输入原密码" maxlength="64" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="form.newPassword" type="password" show-password placeholder="请输入新密码" maxlength="64" />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            show-password
            placeholder="请再次输入新密码"
            maxlength="64"
            @keyup.enter="onSubmit"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="onSubmit" :disabled="!form.oldPassword || !form.newPassword">
            确认修改
          </el-button>
          <el-button v-if="!forceMode" @click="onBack">返回</el-button>
        </el-form-item>
      </el-form>

      <div class="tips text-muted">
        <div>1、密码使用国密 SM2 加密后传输，服务端以 SM3 + 随机盐存储；</div>
        <div>2、修改成功后本账号的登录会话保持有效，无需重新登录；</div>
        <div>3、如忘记原密码，可联系系统管理员在「用户管理」中重置密码。</div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { sm2 } from 'sm-crypto'
import { authApi } from '../../api'
import { getStoredUser, setStoredUser } from '../../utils/auth'

const route = useRoute()
const router = useRouter()

const formRef = ref(null)
const saving = ref(false)
const rule = ref({ text: '' })
const publicKey = ref('')
const user = ref(getStoredUser() || {})

/** 强制改密模式（由统一登录页在检测到初始密码时带 ?force=1 进入） */
const forceMode = computed(() => route.query.force === '1')
const username = computed(() => user.value.username || '')

const form = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

/** 前端预校验（服务端仍会按规则配置做权威校验） */
const rules = computed(() => ({
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { validator: validateNewPassword, trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' }
  ]
}))

onMounted(async () => {
  if (!user.value.username) {
    // 兜底：刷新页面后从服务端取当前用户
    try {
      const res = await authApi.me()
      user.value = res.data || {}
      setStoredUser(user.value)
    } catch (e) {
      // 401 已由拦截器处理
    }
  }
  try {
    const res = await authApi.passwordRule()
    rule.value = res.data || {}
  } catch (e) {
    // 忽略：规则提示获取失败不阻塞改密
  }
  try {
    const res = await authApi.publicKey()
    publicKey.value = res.data || ''
  } catch (e) {
    publicKey.value = ''
  }
})

function validateNewPassword(rule_, value, callback) {
  if (!value) {
    callback()
    return
  }
  if (form.oldPassword && value === form.oldPassword) {
    callback(new Error('新密码不能与原密码相同'))
    return
  }
  const current = rule.value
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
}

function validateConfirm(rule_, value, callback) {
  if (value !== form.newPassword) {
    callback(new Error('两次输入的新密码不一致'))
    return
  }
  callback()
}

function encrypt(plain) {
  if (!publicKey.value) {
    return plain
  }
  try {
    return sm2.doEncrypt(plain, publicKey.value, 1)
  } catch (e) {
    return plain
  }
}

async function onSubmit() {
  try {
    await formRef.value.validate()
  } catch (e) {
    return
  }
  saving.value = true
  try {
    const res = await authApi.changePassword({
      oldPassword: encrypt(form.oldPassword),
      newPassword: encrypt(form.newPassword),
      encrypted: !!publicKey.value
    })
    ElMessage.success(res.msg || '密码修改成功')
    form.oldPassword = ''
    form.newPassword = ''
    form.confirmPassword = ''
    // 刷新本地用户信息（清除「需修改密码」标记）
    try {
      const me = await authApi.me()
      user.value = me.data || {}
      setStoredUser(user.value)
    } catch (e) {
      // 忽略：不影响改密结果
    }
    // 强制改密完成后：继续登录时暂存的跳转目标（如 SSO 回跳地址），否则回工作台
    const pending = sessionStorage.getItem('post-change-password-redirect')
    sessionStorage.removeItem('post-change-password-redirect')
    if (pending) {
      if (pending.startsWith('http')) {
        window.location.href = pending
      } else {
        router.replace(pending)
      }
      return
    }
    if (forceMode.value) {
      router.replace('/workbench')
    }
  } catch (e) {
    // 错误提示已由 axios 拦截器统一处理
  } finally {
    saving.value = false
  }
}

function onBack() {
  router.back()
}
</script>

<style scoped>
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
