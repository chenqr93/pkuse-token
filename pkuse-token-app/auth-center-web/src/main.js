import Vue from 'vue'
import Antd, { message, Modal } from 'ant-design-vue'
import 'ant-design-vue/dist/antd.less'
import App from './App.vue'
import router, { ensureSsoIdentity } from './router'
import './styles/index.less'

Vue.use(Antd)
Vue.config.productionTip = false

Vue.prototype.$message = message
Vue.prototype.$confirm = Modal.confirm
message.config({ top: '20px', maxCount: 3 })

// 用户中心切换账号后，回到已打开的授权中心标签页时自动纠正本地会话
window.addEventListener('focus', () => {
  ensureSsoIdentity(true).catch(() => {})
})
document.addEventListener('visibilitychange', () => {
  if (!document.hidden) {
    ensureSsoIdentity(true).catch(() => {})
  }
})

new Vue({
  router,
  render: (h) => h(App)
}).$mount('#app')
