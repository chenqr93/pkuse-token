// ==================== 各客户端差异配置（复制模板时只需修改本文件） ====================
export default {
  // 客户端名称与模式说明
  clientName: '客户端3（模式三）',
  modeLabel: '模式三：跨域 + 跨 Redis',
  modeDesc: '部署在 127.0.0.1（与认证中心 localhost 跨域）；使用自己的 Redis（192.168.1.75:6379 db6），ticket 通过 HTTP 接口向认证中心校验（is-http=true），单点注销依赖认证中心的消息推送。',

  // 本应用后端地址（前端独立部署，跨域访问后端）
  apiBase: 'http://127.0.0.1:9003',

  // 前端开发服务器端口
  port: 5176,

  // 登录方式：
  //   navigate —— 前端跳转本应用后端 /sso/login，由后端（SDK）完成与认证中心的交互（模式一使用）
  //   ticket   —— 前端取认证中心授权地址，回跳后用 ticket 调后端换取本应用 token（模式二/三/NoSdk 使用）
  loginMode: 'ticket',

  // 认证中心控制台地址（页面上提供返回入口）
  consoleUrl: 'http://localhost:8090'
}
