import Vue from 'vue'
import Antd, { message, Modal } from 'ant-design-vue'
import 'ant-design-vue/dist/antd.less'
import App from './App.vue'
import router from './router'
import './styles/index.less'

Vue.use(Antd)
Vue.config.productionTip = false

Vue.prototype.$message = message
Vue.prototype.$confirm = Modal.confirm
message.config({ top: '20px', maxCount: 3 })

new Vue({
  router,
  render: (h) => h(App)
}).$mount('#app')
