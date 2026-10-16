// ==================== 各客户端差异配置（复制模板时只需修改本文件） ====================
export default {
  // 客户端名称与模式说明
  clientName: '客户端1（模式一）',
  modeLabel: '模式一：同域 + 同 Redis',
  modeDesc: '与认证中心同为 localhost（同域，浏览器 Cookie 共享），并与认证中心使用同一个 Redis，会话数据直接共享，登录无需 ticket（mode=simple）。',

  // 本应用后端地址（前端独立部署，跨端口访问后端）
  apiBase: 'http://localhost:9001',

  // 前端开发服务器端口
  port: 5174,

  // 登录方式：
  //   navigate —— 前端跳转本应用后端 /sso/login，由后端（SDK）完成与认证中心的交互（模式一使用）
  //   ticket   —— 前端取认证中心授权地址，回跳后用 ticket 调后端换取本应用 token（模式二/三/NoSdk 使用）
  loginMode: 'navigate',

  // 认证中心控制台地址（页面上提供返回入口）
  consoleUrl: 'http://localhost:8090'
}
