<template>
  <div class="login-page">
    <div class="login-box">
      <!-- 左侧品牌区 -->
      <div class="brand">
        <div class="brand-logo">
          <a-icon type="key" style="font-size: 34px" />
        </div>
        <h1 class="brand-title">Pkuse-Token 用户中心</h1>
        <p class="brand-subtitle">统一登录 / SSO 认证中心</p>
        <ul class="brand-points">
          <li><a-icon type="check-circle" />统一账号体系，一处登录全域通行</li>
          <li><a-icon type="check-circle" />登录日志、失败锁定、滑块验证码</li>
          <li><a-icon type="check-circle" />设备锁与设备指纹，异常登录可追溯</li>
        </ul>
      </div>

      <!-- 右侧表单区 -->
      <div class="form-panel">
        <h2 class="form-title">账号登录</h2>
        <p v-if="isSsoRedirect && targetClient" class="sso-tip">
          <a-icon type="link" />
          <span>应用 <b>{{ targetClient }}</b> 正在请求统一登录，登录后将自动跳转</span>
        </p>

        <a-form-model
          :model="form"
          class="login-form"
          @submit.native.prevent
          @keyup.enter.native="onSubmit"
        >
          <a-form-model-item>
            <a-input v-model.trim="form.username" size="large" placeholder="账号" allow-clear>
              <a-icon slot="prefix" type="user" />
            </a-input>
          </a-form-model-item>
          <a-form-model-item>
            <a-input-password v-model="form.password" size="large" placeholder="密码">
              <a-icon slot="prefix" type="lock" />
            </a-input-password>
          </a-form-model-item>

          <!-- 滑块验证码：点击「点击验证」弹出滑块界面（默认每次登录都需要） -->
          <a-form-model-item v-if="captchaRequired">
            <a-button
              class="verify-btn"
              :class="{ 'is-verified': captchaPassed }"
              :type="captchaPassed ? 'primary' : 'default'"
              @click="openCaptchaDialog"
            >
              <a-icon :type="captchaPassed ? 'check-circle' : 'lock'" />
              {{ captchaPassed ? '验证成功，请点击登录' : '点击验证' }}
            </a-button>
          </a-form-model-item>

          <!-- 新设备登录验证（设备锁） -->
          <a-form-model-item v-if="deviceVerify">
            <div class="device-verify">
              <a-alert
                type="warning"
                :closable="false"
                message="检测到新设备登录"
                :description="deviceAlertDescription"
              />
              <div class="device-verify-row">
                <a-input v-model.trim="verifyCode" placeholder="短信验证码" maxlength="6" @keyup.enter="onVerifyDevice">
                  <a-icon slot="prefix" type="mobile" />
                </a-input>
                <a-button :disabled="countdown > 0" @click="onSendCode">
                  {{ countdown > 0 ? countdown + ' s 后重发' : '发送验证码' }}
                </a-button>
              </div>
              <a-alert
                v-if="demoCode"
                type="info"
                :closable="false"
                class="demo-code-tip"
                :message="'演示环境（无短信通道）：验证码为 ' + demoCode"
              />
              <div class="device-verify-actions">
                <a-button type="primary" :loading="loading" @click="onVerifyDevice">验证并登录</a-button>
                <a-button @click="resetDeviceVerify">返回</a-button>
              </div>
            </div>
          </a-form-model-item>

          <a-form-model-item>
            <a-button
              type="primary"
              size="large"
              class="submit-btn"
              :loading="loading"
              @click="onSubmit"
              :disabled="!!deviceVerify || (captchaRequired && !captchaPassed)"
            >
              {{ isSsoRedirect ? '登录并跳转应用' : '登 录' }}
            </a-button>
          </a-form-model-item>
        </a-form-model>

        <!-- 滑块验证码弹窗：拖动滑块完成拼图 -->
        <a-modal
          :visible="captchaDialogVisible"
          title="拖动滑块完成拼图"
          :width="380"
          :mask-closable="false"
          wrap-class-name="captcha-dialog"
          @cancel="closeCaptchaDialog"
        >
          <SliderCaptcha ref="captchaRef" :max-width="320" @success="onCaptchaSuccess" />
          <template slot="footer">
            <a-button type="link" @click="reloadCaptcha">
              <a-icon type="reload" />换一张
            </a-button>
            <a-button type="link" @click="closeCaptchaDialog">关闭</a-button>
          </template>
        </a-modal>

        <!-- 登录提示（锁定 / 设备锁等） -->
        <a-alert v-if="securityTip" type="error" :closable="false" class="security-tip" :message="securityTip" />

        <a-alert type="info" :closable="false" class="demo-tip">
          <template slot="message">
            演示账号：<b>admin</b> / 123456（系统管理员）、<b>zhangsan</b> / 123456（普通用户）
          </template>
        </a-alert>

        <div class="fingerprint-tip">
          <a-icon type="desktop" />
          <span>设备指纹：<code>{{ fingerprint || '计算中…' }}</code></span>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { sm2 } from 'sm-crypto'
import SliderCaptcha from '../../components/SliderCaptcha.vue'
import { authApi } from '../../api'
import { saveLoginState } from '../../utils/auth'
import { getDeviceFingerprint } from '../../utils/fingerprint'

export default {
  name: 'LoginPage',
  components: { SliderCaptcha },
  data() {
    return {
      form: { username: '', password: '' },
      loading: false,
      fingerprint: '',
      // 当前是否需要滑块验证码（策略：captcha-after-fail=0 时登录页默认就需要）
      captchaRequired: false,
      // 滑块验证弹窗
      captchaDialogVisible: false,
      // 已通过的滑块验证结果：{ captchaId, track }
      captchaSolution: null,
      // 滑块是否已通过（通过后由用户手动点「登录」提交，不自动提交）
      captchaPassed: false,
      // 安全相关提示（账号锁定等）
      securityTip: '',
      // SM2 公钥（国密，用于密码传输加密）
      publicKey: '',
      // 新设备登录验证：{ needVerify, verifyToken, phone }
      deviceVerify: null,
      verifyCode: '',
      demoCode: '',
      countdown: 0,
      countdownTimer: null
    }
  },
  computed: {
    /**
     * SSO 回跳地址：
     * 未登录访问认证中心 /sso/auth 时，后端会把原始授权地址通过 ?redirect= 带回本页面，
     * 登录成功后整页跳回该地址，由认证中心下发 ticket 并重定向回目标应用。
     */
    redirect() {
      const value = this.$route.query.redirect
      return typeof value === 'string' ? value : ''
    },
    /** 从回跳地址中解析目标应用标识，用于页面提示 */
    targetClient() {
      if (!this.redirect) return ''
      try {
        const queryString = this.redirect.substring(this.redirect.indexOf('?') + 1)
        return new URLSearchParams(queryString).get('client') || ''
      } catch (e) {
        return ''
      }
    },
    /** 是否为跨系统（SSO）回跳：整页跳转认证中心；否则为站内地址，走前端路由 */
    isSsoRedirect() {
      return this.redirect.indexOf('http') === 0
    },
    /** 新设备登录验证提示文案 */
    deviceAlertDescription() {
      const phone = this.deviceVerify && this.deviceVerify.phone ? this.deviceVerify.phone : '未绑定'
      return `为保障账号安全，需验证绑定手机号（${phone}）。验证通过后将解绑原设备，并绑定当前设备。`
    }
  },
  mounted() {
    this.init()
  },
  beforeDestroy() {
    if (this.countdownTimer) {
      clearInterval(this.countdownTimer)
      this.countdownTimer = null
    }
  },
  methods: {
    async init() {
      // 计算设备指纹（登录时上报，服务端用于设备锁与登录日志）
      this.fingerprint = await getDeviceFingerprint()
      // 获取 SM2 公钥（国密，用于密码传输加密；获取失败时退化为明文传输）
      try {
        const res = await authApi.publicKey()
        this.publicKey = (res.data && res.data.publicKey) || ''
      } catch (e) {
        this.publicKey = ''
      }
      // 查询当前策略下是否需要滑块验证码（captcha-after-fail=0 时默认就需要）
      try {
        const res = await authApi.needCaptcha(this.form.username)
        this.captchaRequired = res.data === true
      } catch (e) {
        this.captchaRequired = false
      }
    },

    /* ------------------------- 滑块验证码 ------------------------- */

    /** 点击「点击验证」：打开滑块验证弹窗（每次都取一张新的验证码） */
    async openCaptchaDialog() {
      this.captchaDialogVisible = true
      await this.$nextTick()
      if (this.$refs.captchaRef) {
        this.$refs.captchaRef.reload()
      }
    },

    /** 弹窗内「换一张」 */
    async reloadCaptcha() {
      await this.$nextTick()
      if (this.$refs.captchaRef) {
        this.$refs.captchaRef.reload()
      }
    },

    /** 关闭弹窗（未通过且关闭时保持未验证状态） */
    closeCaptchaDialog() {
      this.captchaDialogVisible = false
      this.onCaptchaDialogClosed()
    },

    onCaptchaDialogClosed() {
      // 未通过就关闭弹窗时，保持未验证状态
      if (!this.captchaSolution) {
        this.captchaPassed = false
      }
    },

    /**
     * 滑块验证通过
     *
     * <p> 仅记录验证结果并关闭弹窗、提示用户点击「登录」提交，不自动提交—— </p>
     * <p> 避免用户在密码尚未修改完成时被自动触发登录（也会白白消耗一张一次性验证码）。 </p>
     */
    onCaptchaSuccess(solution) {
      this.captchaSolution = solution
      this.captchaPassed = true
      this.captchaDialogVisible = false
      this.$message.success('验证通过，请点击「登 录」')
    },

    /** 使用 SM2 公钥加密密码（国密；C1C3C2 模式，与后端 BC 一致） */
    encryptPassword(plain) {
      if (!this.publicKey) {
        return plain
      }
      try {
        return sm2.doEncrypt(plain, this.publicKey, 1)
      } catch (e) {
        return plain
      }
    },

    async onSubmit() {
      if (this.loading) {
        return
      }
      if (!this.form.username) {
        this.$message.warning('请输入账号')
        return
      }
      if (!this.form.password) {
        this.$message.warning('请输入密码')
        return
      }
      // 需要滑块验证但尚未通过：打开验证弹窗并提示
      if (this.captchaRequired && !this.captchaPassed) {
        this.$message.warning('请先完成滑块验证')
        await this.openCaptchaDialog()
        return
      }
      // 策略为「连续失败 N 次后才需要」时，提交前用账号再确认一次是否需要验证码
      if (!this.captchaRequired) {
        try {
          const need = await authApi.needCaptcha(this.form.username)
          if (need.data === true) {
            this.captchaRequired = true
            await this.openCaptchaDialog()
            this.$message.warning('连续登录失败，请先完成滑块验证')
            return
          }
        } catch (e) {
          // 查询失败则不拦截，继续走登录（由服务端决定是否返回 428）
        }
      }

      await this.doLogin()
    },

    /**
     * 提交登录
     *
     * <p> 验证码校验数据为一次性：只要本次请求带了验证码，无论成功失败都会被消费， </p>
     * <p> 因此请求后必须清除「已通过」状态（弹窗内的验证码下次打开时会重新获取）。 </p>
     */
    async doLogin() {
      this.loading = true
      this.securityTip = ''
      try {
        const res = await authApi.login({
          username: this.form.username,
          // 密码使用 SM2 公钥加密后传输（国密）
          password: this.encryptPassword(this.form.password),
          encrypted: !!this.publicKey,
          deviceId: this.fingerprint || (await getDeviceFingerprint()),
          captchaId: this.captchaSolution ? this.captchaSolution.captchaId : undefined,
          captchaTrack: this.captchaSolution ? this.captchaSolution.track : undefined
        })
        // 登录成功：清理验证状态并跳转
        this.captchaSolution = null
        this.captchaPassed = false
        this.captchaDialogVisible = false
        this.deviceVerify = null
        // 用户信息与角色拆分：登录后再拉取平台角色（管理端显隐判断用）
        const rolesRes = await authApi.myRoles().catch(() => null)
        saveLoginState(res.data.token, res.data.user, (rolesRes && rolesRes.data) || [])
        this.$message.success('登录成功')
        // 规则配置 - 首次登录强制修改密码：先进入改密页
        if (await this.handleMustChangePassword(res.data)) {
          return
        }
        await this.doRedirectAfterLogin()
      } catch (e) {
        // 验证码已被本次请求消费：清除通过状态（下次打开弹窗会取新的验证码）
        this.captchaSolution = null
        this.captchaPassed = false
        if (e.code === 423) {
          // 账号锁定（可由管理员在线解锁）：收起验证弹窗，展示锁定提示
          this.securityTip = e.message
          this.captchaDialogVisible = false
        } else if (e.code === 428) {
          // 验证码未通过：自动重新打开弹窗并换一张新验证码
          this.$message.warning(e.message || '请完成滑块验证')
          await this.openCaptchaDialog()
        } else if (e.code === 421) {
          // 新设备登录（设备锁）：进入手机验证码验证流程
          this.captchaDialogVisible = false
          this.deviceVerify = e.data || { needVerify: true }
          this.verifyCode = ''
          this.demoCode = ''
          this.$message.warning(e.message || '检测到新设备登录，请验证身份')
        } else {
          // 其他失败（密码错误等）：提示后由用户修正后重新点击「点击验证」
          this.$message.error(e.message || '登录失败')
        }
      } finally {
        this.loading = false
      }
    },

    /* ------------------------- 新设备登录验证（设备锁换绑） ------------------------- */

    /** 发送短信验证码（验证凭证来自登录时的 421 响应） */
    async onSendCode() {
      if (!this.deviceVerify || !this.deviceVerify.verifyToken) {
        this.$message.warning('验证凭证已失效，请重新登录')
        this.resetDeviceVerify()
        return
      }
      try {
        const res = await authApi.sendDeviceCode(this.deviceVerify.verifyToken)
        const data = res.data || {}
        this.deviceVerify = { ...this.deviceVerify, phone: data.phone }
        this.demoCode = data.demoCode || ''
        this.$message.success('验证码已发送至 ' + (data.phone || '绑定手机号'))
        // 倒计时（60 秒后可重发）
        this.countdown = 60
        if (this.countdownTimer) {
          clearInterval(this.countdownTimer)
        }
        this.countdownTimer = setInterval(() => {
          this.countdown -= 1
          if (this.countdown <= 0) {
            clearInterval(this.countdownTimer)
            this.countdownTimer = null
          }
        }, 1000)
      } catch (e) {
        this.$message.error(e.message || '验证码发送失败')
      }
    },

    /** 校验验证码并完成换绑登录 */
    async onVerifyDevice() {
      if (!this.verifyCode) {
        this.$message.warning('请输入短信验证码')
        return
      }
      this.loading = true
      try {
        const res = await authApi.verifyDevice(this.deviceVerify.verifyToken, this.verifyCode)
        // 用户信息与角色拆分：登录后再拉取平台角色（管理端显隐判断用）
        const rolesRes = await authApi.myRoles().catch(() => null)
        saveLoginState(res.data.token, res.data.user, (rolesRes && rolesRes.data) || [])
        this.$message.success('验证通过，已绑定新设备')
        this.deviceVerify = null
        if (await this.handleMustChangePassword(res.data)) {
          return
        }
        await this.doRedirectAfterLogin()
      } catch (e) {
        this.$message.error(e.message || '验证失败')
      } finally {
        this.loading = false
      }
    },

    resetDeviceVerify() {
      this.deviceVerify = null
      this.verifyCode = ''
      this.demoCode = ''
    },

    /** 登录成功后的跳转（SSO 回跳 / 站内地址 / 工作台） */
    async doRedirectAfterLogin() {
      // 强制改密时暂存的原始跳转目标（改密完成后继续跳转）
      const pending = sessionStorage.getItem('post-change-password-redirect')
      if (pending) {
        sessionStorage.removeItem('post-change-password-redirect')
        if (pending.indexOf('http') === 0) {
          window.location.href = pending
        } else {
          this.$router.replace(pending)
        }
        return
      }
      if (this.isSsoRedirect) {
        window.location.href = this.redirect
      } else if (this.redirect) {
        this.$router.replace(this.redirect)
      } else {
        this.$router.replace('/workbench')
      }
    },

    /**
     * 登录成功后处理：开启「首次登录强制修改密码」（规则配置）时，先进入改密页
     *
     * @return true=已跳转改密页（调用方不再执行后续跳转）
     */
    async handleMustChangePassword(data) {
      if (!data || !data.mustChangePassword) {
        return false
      }
      // 记录原始跳转目标（如 SSO 回跳地址），改密完成后继续
      if (this.redirect) {
        sessionStorage.setItem('post-change-password-redirect', this.redirect)
      }
      this.$message.warning('检测到初始密码，请先修改密码后再使用系统')
      this.$router.replace({ path: '/password', query: { force: '1' } })
      return true
    }
  }
}
</script>

<style lang="less" scoped>
@import '../../styles/tokens.less';

.login-page {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  background: linear-gradient(135deg, @pd-color-menu-sub-bg 0%, @pd-color-menu-bg 45%, @pd-color-primary-dark 100%);
}

.login-box {
  display: flex;
  width: 880px;
  min-height: 460px;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 18px 60px fade(@pd-color-menu-bg, 35%);
  background-color: @pd-color-white;
}

.brand {
  width: 380px;
  padding: 48px 36px;
  color: @pd-color-white;
  background: linear-gradient(160deg, @pd-color-menu-sub-bg 0%, @pd-color-menu-bg 60%, @pd-color-primary-dark 100%);
}

.brand-logo {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 64px;
  height: 64px;
  border-radius: 16px;
  background-color: fade(@pd-color-white, 12%);
  margin-bottom: 20px;
}

.brand-title {
  margin: 0 0 6px;
  color: @pd-color-white;
  font-size: 22px;
  font-weight: 600;
  letter-spacing: 1px;
}

.brand-subtitle {
  margin: 0 0 28px;
  color: fade(@pd-color-white, 65%);
}

.brand-points {
  margin: 0;
  padding: 0;
  list-style: none;
  line-height: 2.2;
  color: fade(@pd-color-white, 85%);
  font-size: 13px;
}

.brand-points .anticon {
  margin-right: 8px;
  color: @pd-color-success;
  vertical-align: -2px;
}

.form-panel {
  flex: 1;
  padding: 40px 36px;
}

.form-title {
  margin: 0 0 20px;
  font-size: 20px;
  color: @pd-color-text;
}

.sso-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 0 0 16px;
  padding: 10px 12px;
  border-radius: 6px;
  background-color: @pd-tag-info-bg;
  color: @pd-tag-info;
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

.verify-btn:not(.is-verified) {
  color: @pd-tag-warning;
  border-color: @pd-tag-warning;
  background-color: @pd-tag-warning-bg;
}

.verify-btn:not(.is-verified):hover,
.verify-btn:not(.is-verified):focus {
  color: @pd-color-white;
  background-color: @pd-color-warning;
  border-color: @pd-color-warning;
}

.verify-btn.is-verified,
.verify-btn.is-verified:hover,
.verify-btn.is-verified:focus {
  color: @pd-color-white;
  background-color: @pd-color-success;
  border-color: @pd-color-success;
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
  color: @pd-color-text-muted;
}

.fingerprint-tip code {
  font-family: Consolas, Monaco, monospace;
}
</style>
