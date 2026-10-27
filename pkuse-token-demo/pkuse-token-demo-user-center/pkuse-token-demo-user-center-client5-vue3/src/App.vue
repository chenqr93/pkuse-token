<template>
  <div class="page">
    <!-- 登录处理中 -->
    <div v-if="view === 'login'" class="center-box">
      <div class="spinner"></div>
      <p>{{ loginTip }}</p>
      <p v-if="errorMsg" class="error">{{ errorMsg }}</p>
    </div>

    <!-- 应用首页 -->
    <template v-else>
      <header class="header">
        <div class="brand">
          <span class="logo">SSO</span>
          <div>
            <div class="title">{{ config.clientName }}</div>
            <div class="subtitle">{{ config.modeLabel }}</div>
          </div>
        </div>
        <a class="link" :href="config.consoleUrl" target="_blank">用户中心（认证中心）↗</a>
      </header>

      <main class="main">
        <!-- 错误提示 -->
        <div v-if="errorMsg" class="alert-error">
          <span>操作失败：{{ errorMsg }}</span>
          <button class="btn ghost" @click="errorMsg = ''">关闭</button>
        </div>

        <!-- 登录状态 -->
        <section class="card">
          <div class="card-title">当前登录状态</div>
          <div class="status-row">
            <span class="badge" :class="isLogin ? 'badge-ok' : 'badge-off'">
              {{ isLogin ? '已登录' : '未登录' }}
            </span>
            <span v-if="isLogin" class="muted">loginId：{{ loginId }}</span>
          </div>
          <div v-if="token" class="muted mono token-line">
            token：{{ token.substring(0, 8) }}****{{ token.substring(token.length - 4) }}
          </div>
          <div class="muted tip">
            {{ isLogin ? '' : '点击下方「统一登录」跳转认证中心完成登录' }}
          </div>
        </section>

        <!-- 操作按钮 -->
        <section class="card">
          <div class="card-title">操作</div>
          <div class="btn-row">
            <button class="btn primary" @click="onLogin">
              {{ isLogin ? '重新登录' : '统一登录' }}
            </button>
            <button class="btn" :disabled="!isLogin" @click="onMyInfo">获取账号资料</button>
            <button class="btn" :disabled="!isLogin" @click="onLogoutByAlone">单应用注销</button>
            <button class="btn danger" :disabled="!isLogin" @click="onLogoutSlo">单点注销</button>
            <button class="btn ghost" @click="refresh">刷新状态</button>
          </div>
        </section>

        <!-- 账号资料 -->
        <section v-if="userInfo" class="card">
          <div class="card-title">账号资料（来自认证中心 userinfo 消息）</div>
          <pre class="json">{{ JSON.stringify(userInfo, null, 2) }}</pre>
        </section>

        <!-- 开放接口调用方管理（magic-api，需登录） -->
        <section v-if="isLogin" class="card">
          <div class="card-head">
            <div class="card-title">开放接口调用方管理（magic-api）</div>
            <div class="btn-row">
              <button class="btn primary" @click="openCreate">新登记调用方</button>
              <button class="btn" :disabled="openLoading" @click="loadOpenClients">刷新列表</button>
            </div>
          </div>
          <p class="muted tip">
            登记法治中台内部应用与外部第三方应用：调用方以 client/secret 签名调
            <span class="mono">POST /open/token</span> 换取本平台令牌后访问 magic 接口；
            禁用 / 重置密钥 / 删除会即时吊销其全部已签发令牌。
          </p>
          <table v-if="openClients.length" class="table">
            <thead>
              <tr>
                <th>调用方标识</th>
                <th>显示名</th>
                <th>有效期(秒)</th>
                <th>状态</th>
                <th>密钥</th>
                <th>备注</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in openClients" :key="row.id">
                <td class="mono">{{ row.clientId }}</td>
                <td>{{ row.clientName || '—' }}</td>
                <td>{{ row.timeoutSeconds }}</td>
                <td>
                  <span class="badge" :class="row.status === 1 ? 'badge-ok' : 'badge-off'">
                    {{ row.status === 1 ? '启用' : '禁用' }}
                  </span>
                </td>
                <td class="mono">{{ row.secretKey }}</td>
                <td>{{ row.remark || '—' }}</td>
                <td class="op-cell">
                  <button class="btn sm" @click="openEdit(row)">编辑</button>
                  <button class="btn sm" @click="toggleStatus(row)">
                    {{ row.status === 1 ? '禁用' : '启用' }}
                  </button>
                  <button class="btn sm" @click="onResetSecret(row)">重置密钥</button>
                  <button class="btn sm danger" @click="onRemove(row)">删除</button>
                </td>
              </tr>
            </tbody>
          </table>
          <p v-else class="muted tip">{{ openLoading ? '加载中…' : '暂无登记的调用方' }}</p>
        </section>

        <!-- 模式说明 -->
        <section class="card tips">
          <div class="card-title">对接模式说明</div>
          <p>{{ config.modeDesc }}</p>
          <p class="muted mono">
            前端：http://localhost:{{ config.port }}　　后端：{{ config.apiBase }}
          </p>
          <p class="muted mono">
            magic-api 编辑器：{{ config.apiBase }}/api-manage/console
          </p>
        </section>
      </main>
    </template>

    <!-- 新登记 / 编辑调用方弹窗 -->
    <div v-if="formDialog.visible" class="dialog-mask">
      <div class="dialog">
        <div class="card-title">
          {{ formDialog.mode === 'create' ? '新登记调用方' : '编辑调用方：' + formDialog.clientId }}
        </div>
        <div v-if="formDialog.mode === 'create'" class="form-item">
          <label>调用方标识 *</label>
          <input v-model.trim="formDialog.clientId"
                 placeholder="2~64 位字母/数字/下划线/中划线，如 fz-app-demo" />
        </div>
        <div class="form-item">
          <label>显示名</label>
          <input v-model.trim="formDialog.clientName" placeholder="如：法治中台示例应用" />
        </div>
        <div class="form-item">
          <label>令牌有效期（秒）</label>
          <input v-model.number="formDialog.timeoutSeconds" type="number" min="60" placeholder="默认 7200" />
        </div>
        <div class="form-item">
          <label>备注</label>
          <input v-model.trim="formDialog.remark" placeholder="调用方说明（选填）" />
        </div>
        <p v-if="formDialog.mode === 'create'" class="muted tip">
          密钥自动生成，提交后仅显示一次，请及时下发给调用方妥善保管。
        </p>
        <div class="btn-row dialog-foot">
          <button class="btn primary" :disabled="submitting" @click="submitForm">
            {{ submitting ? '提交中…' : '提 交' }}
          </button>
          <button class="btn" @click="formDialog.visible = false">取 消</button>
        </div>
      </div>
    </div>

    <!-- 密钥仅此一次展示弹窗 -->
    <div v-if="secretDialog.visible" class="dialog-mask">
      <div class="dialog">
        <div class="card-title">密钥仅此一次展示：{{ secretDialog.client }}</div>
        <p class="alert-once">请立即复制并下发给调用方妥善保管，关闭后将无法再次查看（忘记可随时重置）。</p>
        <pre class="json">{{ secretDialog.secret }}</pre>
        <div class="btn-row dialog-foot">
          <button class="btn primary" @click="copySecret(secretDialog.secret)">复制密钥</button>
          <button class="btn" @click="secretDialog.visible = false">我已保存，关闭</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import config from './config.js'
import { ssoApi, openClientApi, setToken, clearToken, getToken } from './api.js'

const view = ref('home')
const loginTip = ref('正在跳转统一登录…')
const errorMsg = ref('')
const isLogin = ref(false)
const loginId = ref(null)
const token = ref(getToken())
const userInfo = ref(null)

// 开放接口调用方管理状态
const openClients = ref([])
const openLoading = ref(false)
const submitting = ref(false)
const formDialog = reactive({
  visible: false, mode: 'create', id: '',
  clientId: '', clientName: '', timeoutSeconds: 7200, remark: ''
})
const secretDialog = reactive({ visible: false, client: '', secret: '' })

onMounted(async () => {
  // 统一登录回跳页面：/login（可能携带 ticket）
  if (window.location.pathname.startsWith('/login')) {
    await handleLoginPage()
    return
  }
  await refresh()
})

/** 登录页：驱动统一登录流程 */
async function handleLoginPage() {
  view.value = 'login'
  const params = new URLSearchParams(window.location.search)
  const ticket = params.get('ticket')

  try {
    if (config.loginMode === 'navigate') {
      // 模式一：直接跳本应用后端登录入口（同域 Cookie + 共享 Redis，无需 ticket）
      loginTip.value = '正在跳转认证中心统一登录…'
      window.location.href = `${config.apiBase}/sso/login?back=${encodeURIComponent(window.location.origin + '/')}`
      return
    }

    if (ticket) {
      // 从认证中心带 ticket 回跳：换取本应用 token
      loginTip.value = '正在校验 ticket 并登录…'
      const res = await ssoApi.doLoginByTicket(ticket)
      if (res.code === 200) {
        setToken(res.data)
        window.location.replace('/')
        return
      }
      errorMsg.value = res.msg || '登录失败'
      view.value = 'home'
      return
    }

    // 未登录：取认证中心授权地址并跳转
    loginTip.value = '正在跳转认证中心统一登录…'
    const res = await ssoApi.getSsoAuthUrl(window.location.origin + '/login')
    if (res.code === 200 && res.data) {
      window.location.href = res.data
      return
    }
    errorMsg.value = res.msg || '获取认证中心地址失败'
    view.value = 'home'
  } catch (e) {
    errorMsg.value = '请求失败：' + e.message
    view.value = 'home'
  }
}

/** 刷新登录状态（已登录时同步加载调用方列表） */
async function refresh() {
  try {
    const res = await ssoApi.isLogin()
    isLogin.value = !!res.isLogin
    loginId.value = res.loginId
    token.value = res.token || getToken()
    if (!isLogin.value) {
      userInfo.value = null
      openClients.value = []
    } else {
      await loadOpenClients()
    }
  } catch (e) {
    errorMsg.value = '请求失败：' + e.message
  }
}

function onLogin() {
  window.location.href = '/login'
}

async function onMyInfo() {
  errorMsg.value = ''
  const res = await ssoApi.myInfo()
  if (res && res.code !== 200) {
    errorMsg.value = res.msg || '获取账号资料失败'
    return
  }
  userInfo.value = res
}

async function onLogoutByAlone() {
  await ssoApi.logoutByAlone()
  clearToken()
  await refresh()
}

function onLogoutSlo() {
  // 单点注销：跳应用后端 /sso/logout（会通知认证中心，注销后回到本页）
  window.location.href = ssoApi.logoutUrl(window.location.origin + '/')
}

// ==================== 开放接口调用方管理 ====================

/** 加载调用方列表 */
async function loadOpenClients() {
  openLoading.value = true
  try {
    const res = await openClientApi.list()
    if (res.code === 200) {
      openClients.value = res.data || []
    } else {
      errorMsg.value = res.msg || '获取调用方列表失败'
    }
  } catch (e) {
    errorMsg.value = '请求失败：' + e.message
  } finally {
    openLoading.value = false
  }
}

function openCreate() {
  Object.assign(formDialog, {
    visible: true, mode: 'create', id: '',
    clientId: '', clientName: '', timeoutSeconds: 7200, remark: ''
  })
}

function openEdit(row) {
  Object.assign(formDialog, {
    visible: true, mode: 'edit', id: row.id,
    clientId: row.clientId, clientName: row.clientName || '',
    timeoutSeconds: row.timeoutSeconds || 7200, remark: row.remark || ''
  })
}

/** 提交登记 / 编辑 */
async function submitForm() {
  errorMsg.value = ''
  if (formDialog.mode === 'create' && !formDialog.clientId) {
    errorMsg.value = '请填写调用方标识'
    return
  }
  submitting.value = true
  const isCreate = formDialog.mode === 'create'
  try {
    const res = isCreate
      ? await openClientApi.create({
          clientId: formDialog.clientId,
          clientName: formDialog.clientName,
          timeoutSeconds: formDialog.timeoutSeconds,
          remark: formDialog.remark
        })
      : await openClientApi.update(formDialog.id, {
          clientName: formDialog.clientName,
          timeoutSeconds: formDialog.timeoutSeconds,
          remark: formDialog.remark
        })
    if (res.code !== 200) {
      errorMsg.value = res.msg || '提交失败'
      return
    }
    formDialog.visible = false
    if (isCreate && res.data && res.data.secretKey) {
      // 新登记：密钥仅此一次展示
      showSecret(res.data.clientId, res.data.secretKey)
    }
    await loadOpenClients()
  } catch (e) {
    errorMsg.value = '请求失败：' + e.message
  } finally {
    submitting.value = false
  }
}

/** 启用 / 禁用（禁用会吊销该调用方全部令牌） */
async function toggleStatus(row) {
  const target = row.status === 1 ? 0 : 1
  if (target === 0 && !confirm(`确认禁用「${row.clientId}」？其全部已签发令牌将立即失效。`)) {
    return
  }
  const res = await openClientApi.updateStatus(row.id, target)
  if (res.code !== 200) {
    errorMsg.value = res.msg || '操作失败'
    return
  }
  await loadOpenClients()
}

/** 重置密钥（旧令牌全部吊销，新密钥仅展示一次） */
async function onResetSecret(row) {
  if (!confirm(`确认重置「${row.clientId}」的密钥？其全部已签发令牌将立即失效，调用方需用新密钥重新获取令牌。`)) {
    return
  }
  const res = await openClientApi.resetSecret(row.id)
  if (res.code !== 200) {
    errorMsg.value = res.msg || '重置失败'
    return
  }
  showSecret(row.clientId, res.data)
  await loadOpenClients()
}

/** 删除调用方（并吊销其全部令牌） */
async function onRemove(row) {
  if (!confirm(`确认删除「${row.clientId}」？其全部已签发令牌将立即失效，且不可恢复。`)) {
    return
  }
  const res = await openClientApi.remove(row.id)
  if (res.code !== 200) {
    errorMsg.value = res.msg || '删除失败'
    return
  }
  await loadOpenClients()
}

/** 弹窗展示一次性密钥 */
function showSecret(client, secret) {
  Object.assign(secretDialog, { visible: true, client, secret })
}

/** 复制密钥到剪贴板 */
async function copySecret(text) {
  try {
    await navigator.clipboard.writeText(text)
    alert('已复制到剪贴板')
  } catch (e) {
    window.prompt('请手动复制：', text)
  }
}
</script>
