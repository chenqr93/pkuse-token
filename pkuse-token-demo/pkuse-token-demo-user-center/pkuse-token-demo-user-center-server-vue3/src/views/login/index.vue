<template>
  <div class="login-page">
    <div class="login-box">
      <!-- 左侧品牌区 -->
      <div class="brand">
        <div class="brand-logo">
          <el-icon :size="34"><Key /></el-icon>
        </div>
        <h1 class="brand-title">Pkuse-Token 用户中心</h1>
        <p class="brand-subtitle">统一登录 / SSO 认证中心</p>
        <ul class="brand-points">
          <li><el-icon><CircleCheck /></el-icon>统一账号体系，一处登录全域通行</li>
          <li><el-icon><CircleCheck /></el-icon>登录日志、失败锁定、滑块验证码</li>
          <li><el-icon><CircleCheck /></el-icon>设备锁与设备指纹，异常登录可追溯</li>
        </ul>
      </div>

      <!-- 右侧表单区 -->
      <div class="form-panel">
        <h2 class="form-title">账号登录</h2>
        <p v-if="isSsoRedirect && targetClient" class="sso-tip">
          <el-icon><Connection /></el-icon>
          <span>应用 <b>{{ targetClient }}</b> 正在请求统一登录，登录后将自动跳转</span>
        </p>

        <el-form :model="form" size="large" @submit.prevent @keyup.enter="onSubmit">
          <el-form-item>
            <el-input v-model.trim="form.username" placeholder="账号" clearable @keyup.enter="onSubmit">
              <template #prefix><el-icon><User /></el-icon></template>
            </el-input>
          </el-form-item>
          <el-form-item>
            <el-input v-model="form.password" type="password" placeholder="密码" show-password @keyup.enter="onSubmit">
              <template #prefix><el-icon><Lock /></el-icon></template>
            </el-input>
          </el-form-item>

          <!-- 滑块验证码：点击「点击验证」弹出滑块界面（默认每次登录都需要） -->
          <el-form-item v-if="captchaRequired">
            <el-button
              class="verify-btn"
              :class="{ verified: captchaPassed }"
              :type="captchaPassed ? 'success' : 'warning'"
              @click="openCaptchaDialog"
            >
              <el-icon v-if="captchaPassed"><CircleCheck /></el-icon>
              <el-icon v-else><Lock /></el-icon>
              {{ captchaPassed ? '验证成功，请点击登录' : '点击验证' }}
            </el-button>
          </el-form-item>

          <!-- 新设备登录验证（设备锁） -->
          <el-form-item v-if="deviceVerify">
            <div class="device-verify">
              <el-alert
                type="warning"
                :closable="false"
                title="检测到新设备登录"
                :description="`为保障账号安全，需验证绑定手机号（${deviceVerify.phone || '未绑定'}）。验证通过后将解绑原设备，并绑定当前设备。`"
              />
              <div class="device-verify-row">
                <el-input v-model.trim="verifyCode" placeholder="短信验证码" maxlength="6" @keyup.enter="onVerifyDevice">
                  <template #prefix><el-icon><Iphone /></el-icon></template>
                </el-input>
                <el-button :disabled="countdown > 0" @click="onSendCode">
                  {{ countdown > 0 ? countdown + ' s 后重发' : '发送验证码' }}
                </el-button>
              </div>
              <el-alert
                v-if="demoCode"
                type="info"
                :closable="false"
                class="demo-code-tip"
                :title="`演示环境（无短信通道）：验证码为 ${demoCode}`"
              />
              <div class="device-verify-actions">
                <el-button type="primary" :loading="loading" @click="onVerifyDevice">验证并登录</el-button>
                <el-button @click="resetDeviceVerify">返回</el-button>
              </div>
            </div>
          </el-form-item>

          <el-form-item>
            <el-button
              type="primary"
              class="submit-btn"
              :loading="loading"
              @click="onSubmit"
              :disabled="!!deviceVerify || (captchaRequired && !captchaPassed)"
            >
              {{ isSsoRedirect ? '登录并跳转应用' : '登 录' }}
            </el-button>
          </el-form-item>
        </el-form>

        <!-- 滑块验证码弹窗：拖动滑块完成拼图 -->
        <el-dialog
          v-model="captchaDialogVisible"
          title="拖动滑块完成拼图"
          width="380px"
          :close-on-click-modal="false"
          append-to-body
          class="captcha-dialog"
          @closed="onCaptchaDialogClosed"
        >
          <SliderCaptcha ref="captchaRef" :max-width="320" @success="onCaptchaSuccess" />
          <template #footer>
            <el-button link type="primary" @click="reloadCaptcha">
              <el-icon><Refresh /></el-icon>换一张
            </el-button>
            <el-button link @click="captchaDialogVisible = false">关闭</el-button>
          </template>
        </el-dialog>

        <!-- 登录提示（锁定 / 设备锁等） -->
        <el-alert v-if="securityTip" type="error" :closable="false" class="security-tip">
          <template #title>{{ securityTip }}</template>
        </el-alert>

        <el-alert type="info" :closable="false" class="demo-tip">
          <template #title>
            演示账号：<b>admin</b> / 123456（系统管理员）、<b>zhangsan</b> / 123456（普通用户）
          </template>
        </el-alert>

        <div class="fingerprint-tip">
          <el-icon><Monitor /></el-icon>
          <span>设备指纹：<code>{{ fingerprint || '计算中…' }}</code></span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { sm2 } from 'sm-crypto'
import SliderCaptcha from '../../components/SliderCaptcha.vue'
import { authApi } from '../../api'
import { saveLoginState } from '../../utils/auth'
import { getDeviceFingerprint } from '../../utils/fingerprint'

const route = useRoute()
const router = useRouter()

const form = reactive({ username: '', password: '' })
const loading = ref(false)
const fingerprint = ref('')
// 当前是否需要滑块验证码（策略：captcha-after-fail=0 时登录页默认就需要）
const captchaRequired = ref(false)
// 滑块验证弹窗
const captchaDialogVisible = ref(false)
const captchaRef = ref(null)
// 已通过的滑块验证结果：{ captchaId, track }
const captchaSolution = ref(null)
// 滑块是否已通过（通过后由用户手动点「登录」提交，不自动提交）
const captchaPassed = ref(false)
// 安全相关提示（账号锁定等）
const securityTip = ref('')
// SM2 公钥（国密，用于密码传输加密）
const publicKey = ref('')
// 新设备登录验证：{ needVerify, verifyToken, phone }
const deviceVerify = ref(null)
const verifyCode = ref('')
const demoCode = ref('')
const countdown = ref(0)
let countdownTimer = null

/**
 * SSO 回跳地址：
 * 未登录访问认证中心 /sso/auth 时，后端会把原始授权地址通过 ?redirect= 带回本页面，
 * 登录成功后整页跳回该地址，由认证中心下发 ticket 并重定向回目标应用。
 */
const redirect = computed(() => {
  const value = route.query.redirect
  return typeof value === 'string' ? value : ''
})

/** 从回跳地址中解析目标应用标识，用于页面提示 */
const targetClient = computed(() => {
  if (!redirect.value) return ''
  try {
    const queryString = redirect.value.substring(redirect.value.indexOf('?') + 1)
    return new URLSearchParams(queryString).get('client') || ''
  } catch (e) {
    return ''
  }
})

/** 是否为跨系统（SSO）回跳：整页跳转认证中心；否则为站内地址，走前端路由 */
const isSsoRedirect = computed(() => redirect.value.startsWith('http'))

onMounted(async () => {
  // 计算设备指纹（登录时上报，服务端用于设备锁与登录日志）
  fingerprint.value = await getDeviceFingerprint()
  // 获取 SM2 公钥（国密，用于密码传输加密；获取失败时退化为明文传输）
  try {
    const res = await authApi.publicKey()
    publicKey.value = res.data || ''
  } catch (e) {
    publicKey.value = ''
  }
  // 查询当前策略下是否需要滑块验证码（captcha-after-fail=0 时默认就需要）
  try {
    const res = await authApi.needCaptcha(form.username)
    captchaRequired.value = res.data === true
  } catch (e) {
    captchaRequired.value = false
  }
})

onUnmounted(() => {
  if (countdownTimer) {
    clearInterval(countdownTimer)
  }
})

/* ------------------------- 滑块验证码 ------------------------- */

/** 点击「点击验证」：打开滑块验证弹窗（每次都取一张新的验证码） */
async function openCaptchaDialog() {
  captchaDialogVisible.value = true
  await nextTick()
  captchaRef.value?.reload()
}

/** 弹窗内「换一张」 */
async function reloadCaptcha() {
  await nextTick()
  captchaRef.value?.reload()
}

function onCaptchaDialogClosed() {
  // 未通过就关闭弹窗时，保持未验证状态
  if (!captchaSolution.value) {
    captchaPassed.value = false
  }
}

/**
 * 滑块验证通过
 *
 * <p> 仅记录验证结果并关闭弹窗、提示用户点击「登录」提交，不自动提交—— </p>
 * <p> 避免用户在密码尚未修改完成时被自动触发登录（也会白白消耗一张一次性验证码）。 </p>
 */
function onCaptchaSuccess(solution) {
  captchaSolution.value = solution
  captchaPassed.value = true
  captchaDialogVisible.value = false
  ElMessage.success('验证通过，请点击「登 录」')
}

/** 使用 SM2 公钥加密密码（国密；C1C3C2 模式，与后端 BC 一致） */
function encryptPassword(plain) {
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
  if (loading.value) {
    return
  }
  if (!form.username) {
    ElMessage.warning('请输入账号')
    return
  }
  if (!form.password) {
    ElMessage.warning('请输入密码')
    return
  }
  // 需要滑块验证但尚未通过：打开验证弹窗并提示
  if (captchaRequired.value && !captchaPassed.value) {
    ElMessage.warning('请先完成滑块验证')
    await openCaptchaDialog()
    return
  }
  // 策略为「连续失败 N 次后才需要」时，提交前用账号再确认一次是否需要验证码
  if (!captchaRequired.value) {
    try {
      const need = await authApi.needCaptcha(form.username)
      if (need.data === true) {
        captchaRequired.value = true
        await openCaptchaDialog()
        ElMessage.warning('连续登录失败，请先完成滑块验证')
        return
      }
    } catch (e) {
      // 查询失败则不拦截，继续走登录（由服务端决定是否返回 428）
    }
  }

  await doLogin()
}

/**
 * 提交登录
 *
 * <p> 验证码校验数据为一次性：只要本次请求带了验证码，无论成功失败都会被消费， </p>
 * <p> 因此请求后必须清除「已通过」状态（弹窗内的验证码下次打开时会重新获取）。 </p>
 */
async function doLogin() {
  loading.value = true
  securityTip.value = ''
  try {
    const res = await authApi.login({
      username: form.username,
      // 密码使用 SM2 公钥加密后传输（国密）
      password: encryptPassword(form.password),
      encrypted: !!publicKey.value,
      deviceId: fingerprint.value || (await getDeviceFingerprint()),
      captchaId: captchaSolution.value ? captchaSolution.value.captchaId : undefined,
      captchaTrack: captchaSolution.value ? captchaSolution.value.track : undefined
    })
    // 登录成功：清理验证状态并跳转
    captchaSolution.value = null
    captchaPassed.value = false
    captchaDialogVisible.value = false
    deviceVerify.value = null
    saveLoginState(res.data.token, res.data.user)
    ElMessage.success('登录成功')
    // 规则配置 - 首次登录强制修改密码：先进入改密页
    if (await handleMustChangePassword(res.data)) {
      return
    }
    await doRedirectAfterLogin()
    return
  } catch (e) {
    // 验证码已被本次请求消费：清除通过状态（下次打开弹窗会取新的验证码）
    captchaSolution.value = null
    captchaPassed.value = false
    if (e.code === 423) {
      // 账号锁定（可由管理员在线解锁）：收起验证弹窗，展示锁定提示
      securityTip.value = e.message
      captchaDialogVisible.value = false
    } else if (e.code === 428) {
      // 验证码未通过：自动重新打开弹窗并换一张新验证码
      ElMessage.warning(e.message || '请完成滑块验证')
      await openCaptchaDialog()
    } else if (e.code === 421) {
      // 新设备登录（设备锁）：进入手机验证码验证流程
      captchaDialogVisible.value = false
      deviceVerify.value = e.data || { needVerify: true }
      verifyCode.value = ''
      demoCode.value = ''
      ElMessage.warning(e.message || '检测到新设备登录，请验证身份')
    } else {
      // 其他失败（密码错误等）：提示后由用户修正后重新点击「点击验证」
      ElMessage.error(e.message || '登录失败')
    }
  } finally {
    loading.value = false
  }
}

/* ------------------------- 新设备登录验证（设备锁换绑） ------------------------- */

/** 发送短信验证码（验证凭证来自登录时的 421 响应） */
async function onSendCode() {
  if (!deviceVerify.value || !deviceVerify.value.verifyToken) {
    ElMessage.warning('验证凭证已失效，请重新登录')
    resetDeviceVerify()
    return
  }
  try {
    const res = await authApi.sendDeviceCode(deviceVerify.value.verifyToken)
    const data = res.data || {}
    deviceVerify.value = { ...deviceVerify.value, phone: data.phone }
    demoCode.value = data.demoCode || ''
    ElMessage.success('验证码已发送至 ' + (data.phone || '绑定手机号'))
    // 倒计时（60 秒后可重发）
    countdown.value = 60
    if (countdownTimer) {
      clearInterval(countdownTimer)
    }
    countdownTimer = setInterval(() => {
      countdown.value -= 1
      if (countdown.value <= 0) {
        clearInterval(countdownTimer)
        countdownTimer = null
      }
    }, 1000)
  } catch (e) {
    ElMessage.error(e.message || '验证码发送失败')
  }
}

/** 校验验证码并完成换绑登录 */
async function onVerifyDevice() {
  if (!verifyCode.value) {
    ElMessage.warning('请输入短信验证码')
    return
  }
  loading.value = true
  try {
    const res = await authApi.verifyDevice(deviceVerify.value.verifyToken, verifyCode.value)
    saveLoginState(res.data.token, res.data.user)
    ElMessage.success('验证通过，已绑定新设备')
    deviceVerify.value = null
    if (await handleMustChangePassword(res.data)) {
      return
    }
    await doRedirectAfterLogin()
  } catch (e) {
    ElMessage.error(e.message || '验证失败')
  } finally {
    loading.value = false
  }
}

function resetDeviceVerify() {
  deviceVerify.value = null
  verifyCode.value = ''
  demoCode.value = ''
}

/** 登录成功后的跳转（SSO 回跳 / 站内地址 / 工作台） */
async function doRedirectAfterLogin() {
  // 强制改密时暂存的原始跳转目标（改密完成后继续跳转）
  const pending = sessionStorage.getItem('post-change-password-redirect')
  if (pending) {
    sessionStorage.removeItem('post-change-password-redirect')
    if (pending.startsWith('http')) {
      window.location.href = pending
    } else {
      router.replace(pending)
    }
    return
  }
  if (isSsoRedirect.value) {
    window.location.href = redirect.value
  } else if (redirect.value) {
    router.replace(redirect.value)
  } else {
    router.replace('/workbench')
  }
}

/**
 * 登录成功后处理：开启「首次登录强制修改密码」（规则配置）时，先进入改密页
 *
 * @return true=已跳转改密页（调用方不再执行后续跳转）
 */
async function handleMustChangePassword(data) {
  if (!data || !data.mustChangePassword) {
    return false
  }
  // 记录原始跳转目标（如 SSO 回跳地址），改密完成后继续
  if (redirect.value) {
    sessionStorage.setItem('post-change-password-redirect', redirect.value)
  }
  ElMessage.warning('检测到初始密码，请先修改密码后再使用系统')
  router.replace({ path: '/password', query: { force: '1' } })
  return true
}
</script>

<style scoped>
.login-page {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  background: linear-gradient(135deg, #1f3b73 0%, #2a5298 45%, #3f7bc4 100%);
}

.login-box {
  display: flex;
  width: 880px;
  min-height: 460px;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 18px 60px rgba(0, 0, 0, 0.35);
  background-color: #fff;
}

.brand {
  width: 380px;
  padding: 48px 36px;
  color: #fff;
  background: linear-gradient(160deg, #001529 0%, #0b2f5c 60%, #124a8b 100%);
}

.brand-logo {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 64px;
  height: 64px;
  border-radius: 16px;
  background-color: rgba(255, 255, 255, 0.12);
  margin-bottom: 20px;
}

.brand-title {
  margin: 0 0 6px;
  font-size: 22px;
  letter-spacing: 1px;
}

.brand-subtitle {
  margin: 0 0 28px;
  color: rgba(255, 255, 255, 0.65);
}

.brand-points {
  margin: 0;
  padding: 0;
  list-style: none;
  line-height: 2.2;
  color: rgba(255, 255, 255, 0.85);
  font-size: 13px;
}

.brand-points .el-icon {
  margin-right: 8px;
  color: #67c23a;
  vertical-align: -2px;
}

.form-panel {
  flex: 1;
  padding: 40px 36px;
}

.form-title {
  margin: 0 0 20px;
  font-size: 20px;
  color: #303133;
}

.sso-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 0 0 16px;
  padding: 10px 12px;
  border-radius: 6px;
  background-color: #ecf5ff;
  color: #409eff;
  font-size: 13px;
}

.submit-btn {
  width: 100%;
}

/* 「点击验证」按钮（参考 tianai 官方样式：整行按钮，验证通过后变绿） */
.verify-btn {
  width: 100%;
  height: 44px;
  font-size: 15px;
  letter-spacing: 2px;
}

.verify-btn.verified {
  --el-button-bg-color: #67c23a;
  --el-button-border-color: #67c23a;
  --el-button-hover-bg-color: #85ce61;
  --el-button-hover-border-color: #85ce61;
}

.security-tip {
  margin-bottom: 10px;
}

/* 新设备登录验证 */
.device-verify {
  width: 100%;
}

.device-verify-row {
  display: flex;
  gap: 10px;
  margin-top: 12px;
}

.device-verify-actions {
  display: flex;
  gap: 10px;
  margin-top: 12px;
}

.demo-code-tip {
  margin-top: 10px;
}

.demo-tip {
  margin-top: 8px;
}

.fingerprint-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 10px;
  font-size: 12px;
  color: #909399;
}

.fingerprint-tip code {
  font-family: Consolas, Monaco, monospace;
}
</style>
